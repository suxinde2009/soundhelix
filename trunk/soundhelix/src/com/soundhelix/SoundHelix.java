package com.soundhelix;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.soundhelix.arrangementengine.ArrangementEngine;
import com.soundhelix.harmonyengine.HarmonyEngine;
import com.soundhelix.harmonyengine.PatternHarmonyEngine;
import com.soundhelix.misc.Arrangement;
import com.soundhelix.misc.Structure;
import com.soundhelix.player.Player;
import com.soundhelix.util.XMLUtils;

/**
 * Implements the main class. The main() method reads and parses an
 * XML configuration file and then generates and plays songs. The
 * generation of songs is done in a separate thread to guarantee
 * seamless playing.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class SoundHelix implements Runnable {

	private static Logger logger;
	private XPath xpath;
	private Node structureNode;
	private Node harmonyEngineNode;
	private Node arrangementEngineNode;

	// the queue for generated Arrangements
	private static BlockingQueue<Arrangement> arrangementQueue;
	
	public SoundHelix(Node structureNode,Node harmonyEngineNode,Node arrangementEngineNode,XPath xpath) {
		this.structureNode = structureNode;
		this.harmonyEngineNode = harmonyEngineNode;
		this.arrangementEngineNode = arrangementEngineNode;
		this.xpath = xpath;
	}
	
	public static void main(String[] args) throws Exception {		
		String filename;
		
		if(args.length == 1 && args[0].equals("-h") || args.length > 1) {
			System.out.println("java SoundHelix [XML-File]");
			System.exit(0);
		}
	
		if(args.length == 1) {
			filename = args[0];
		} else {
			filename = "SoundHelix.xml";
		}
		
		// initialize log4j
		PropertyConfigurator.configureAndWatch( "log4j.properties", 60*1000 );

		logger = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());
		logger.debug("Starting");
		
		try {
			File file = new File(filename);

			logger.debug("Reading and parsing XML file");
			
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(file);
			
			XPath xpath = XPathFactory.newInstance().newXPath();

			// preprocess document tree by expanding include tags, if present
			
			// note that if the include tags use random selection, this random
			// selection is done only once per start of the application, not once
			// per generated song; this is in contrast to the random selection
			// applied in configure() method calls
			
			XMLUtils.expandIncludeTags(doc,doc.getFirstChild(),xpath);
			
			// get the root element of the file (we don't care what it's called)
			Node mainNode = (Node)xpath.evaluate("/*",doc,XPathConstants.NODE);

			Node structureNode = (Node)xpath.evaluate("structure",mainNode,XPathConstants.NODE);
			Node harmonyEngineNode = (Node)xpath.evaluate("harmonyEngine",mainNode,XPathConstants.NODE);
			Node arrangementEngineNode = (Node)xpath.evaluate("arrangementEngine",mainNode,XPathConstants.NODE);
			Node playerNode = (Node)xpath.evaluate("player",mainNode,XPathConstants.NODE);

			arrangementQueue = new LinkedBlockingQueue<Arrangement>();

			// instantiate this class so than we can launch a thread
			
			SoundHelix electro = new SoundHelix(structureNode,harmonyEngineNode,arrangementEngineNode,xpath);
			Thread t = new Thread(electro);
			t.start();

			while(true) {
				Arrangement arrangement = arrangementQueue.take();
				Player player = XMLUtils.getInstance(Player.class,playerNode,xpath);
				player.open();
				player.play(arrangement);
				player.close();
			}
		} catch(Exception e) {
			logger.warn("Exception detected",e);
			throw(e);
		}
    }
	
	/**
	 * Implements the functionality of the thread that generates songs. The method
	 * runs forever and generates a new song as soon as it detects the queue to be
	 * empty.
	 */
	
	public void run() {
		while(true) {
			try {
				if(arrangementQueue.size() < 1) {
					// the queue is empty; render a new song

					System.out.println("Rendering new song");

					Structure structure = parseStructure(structureNode,xpath);
					HarmonyEngine harmonyEngine = XMLUtils.getInstance(HarmonyEngine.class,harmonyEngineNode,xpath);
					structure.setHarmonyEngine(harmonyEngine);	

					long startTime = System.nanoTime();
					ArrangementEngine arrangementEngine = XMLUtils.getInstance(ArrangementEngine.class,arrangementEngineNode,xpath);
					arrangementEngine.setStructure(structure);
					Arrangement arrangement = arrangementEngine.render();
					long time = System.nanoTime()-startTime;

					System.out.println("Rendering took "+(time/1000000)+" ms");
					arrangementQueue.add(arrangement);
				}
			} catch(Exception e) {e.printStackTrace();}
			
			try {
				Thread.sleep(10000);
			} catch(Exception e) {}
		}
	}
	
	/**
	 * Parses the structure tag and creates a Structure instance. Note that
	 * no HarmonyEngine is set yet.
	 * 
	 * @param node the node of the tag
	 * @param xpath an XPath instance
	 * 
	 * @return a Structure
	 * 
	 * @throws XPathException
	 */
	
	public static Structure parseStructure(Node node,XPath xpath) throws XPathException {
		int bars = XMLUtils.parseInteger("bars",node,xpath);
		int beatsPerBar = XMLUtils.parseInteger("beatsPerBar",node,xpath);
		int ticksPerBeat = XMLUtils.parseInteger("ticksPerBeat",node,xpath);

		if(bars <= 0) {
			throw(new RuntimeException("Number of bars must be > 0"));
		}
		
		if(beatsPerBar <= 0) {
			throw(new RuntimeException("Number of beats per bar must be > 0"));
		}
		
		if(ticksPerBeat <= 0) {
			throw(new RuntimeException("Number of ticks per beat must be > 0"));
		}
		
		Structure structure = new Structure(bars,beatsPerBar,ticksPerBeat);

		return structure;
	}
}