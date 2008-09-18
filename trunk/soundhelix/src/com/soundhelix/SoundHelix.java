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
	private final Structure structure;
	private XPath xpath;
	private Node arrangementEngineNode;

	// the queue for generated Arrangements
	private static BlockingQueue<Arrangement> arrangementQueue;
	
	public SoundHelix(Structure structure,Node arrangementEngineNode,XPath xpath) {
		this.structure = structure;
		this.arrangementEngineNode = arrangementEngineNode;
		this.xpath = xpath;
	}
	
	public static void main(String[] args) throws Exception {		
		String filename;
		
		if(args.length ==1  && args[0].equals("-h") || args.length > 1) {
			System.out.println("java SoundHelix [XML-File]");
			System.exit(0);
		}
	
		if(args.length == 1) {
			filename = args[0];
		} else {
			filename = "SoundHelix.xml";
		}
		
		PropertyConfigurator.configureAndWatch( "log4j.properties", 60*1000 );

		logger = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());
		logger.debug("Starting");
		
		try {
			File file = new File(filename);

			logger.debug("Reading and parsing XML file");
			
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(file);

			XPath xpath = XPathFactory.newInstance().newXPath();

			Node mainNode = (Node)xpath.evaluate("/*",doc,XPathConstants.NODE);

			Node structureNode = (Node)xpath.evaluate("structure",mainNode,XPathConstants.NODE);
			Node harmonyEngineNode = (Node)xpath.evaluate("harmonyEngine",mainNode,XPathConstants.NODE);
			Node arrangementEngineNode = (Node)xpath.evaluate("arrangementEngine",mainNode,XPathConstants.NODE);
			Node playerNode = (Node)xpath.evaluate("player",mainNode,XPathConstants.NODE);

			Structure structure = parseStructure(structureNode,xpath);
			HarmonyEngine harmonyEngine = XMLUtils.getInstance(HarmonyEngine.class,harmonyEngineNode,xpath);
			structure.setHarmonyEngine(harmonyEngine);	

			arrangementQueue = new LinkedBlockingQueue<Arrangement>();

			SoundHelix electro = new SoundHelix(structure,arrangementEngineNode,xpath);
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
	
	public void run() {
		try {
			while(true) {
				if(arrangementQueue.size() < 1) {
					System.out.println("Rendering new song");

					long startTime = System.nanoTime();
					ArrangementEngine arrangementEngine = XMLUtils.getInstance(ArrangementEngine.class,arrangementEngineNode,xpath);
					arrangementEngine.setStructure(structure);
					Arrangement arrangement = arrangementEngine.render();
					long time = System.nanoTime()-startTime;

					System.out.println("Rendering took "+(time/1000000)+" ms");
					arrangementQueue.add(arrangement);
				}

				try {
					Thread.sleep(10000);
				} catch(Exception e) {}
			}
		} catch(Exception e) {e.printStackTrace();}
	}
	
	public static Structure parseStructure(Node node,XPath xpath) throws XPathException {
		int bars = XMLUtils.parseInteger("bars",node,xpath);
		int beatsPerBar = XMLUtils.parseInteger("beatsPerBar",node,xpath);
		int ticksPerBeat = XMLUtils.parseInteger("ticksPerBeat",node,xpath);

		Structure structure = new Structure(bars,beatsPerBar,ticksPerBeat);
		structure.setHarmonyEngine(new PatternHarmonyEngine());

		return structure;
	}
}