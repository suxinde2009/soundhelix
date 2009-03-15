package com.soundhelix;

import java.io.File;
import java.util.Random;
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
import com.soundhelix.misc.Arrangement;
import com.soundhelix.misc.Structure;
import com.soundhelix.player.MidiPlayer;
import com.soundhelix.player.Player;
import com.soundhelix.util.XMLUtils;

/**
 * Implements the main class. The main() method determines the configuration
 * file and then waits for the next generated song and plays it. The
 * configuration parsing and generation of songs is done in a separate
 * thread to guarantee seamless playing. The thread priority for the
 * song generator is set to a low value, the priority of the playing
 * thread is set to a high value.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

// TODO: provide a DTD or XML Schema for the configuration file

public class SoundHelix implements Runnable {

	private static Logger logger;

	// the queue for generated Arrangements
	private BlockingQueue<SongQueueEntry> songQueue = new LinkedBlockingQueue<SongQueueEntry>();

	// the XML configuration file
	private String filename;

	// the random seed
	private long randomSeed;

	public SoundHelix(String filename,long randomSeed) {
		this.filename = filename;
		this.randomSeed = randomSeed;
	}
	
	public static void main(String[] args) throws Exception {		
		if(args.length == 1 && args[0].equals("-h") || args.length > 2) {
			System.out.println("java SoundHelix [XML-File [Songtitle]] ");
			System.exit(0);
		}

		String filename = (args.length >= 1 ? args[0] : "SoundHelix.xml");
		
		if(!new File(filename).exists()) {
			throw(new RuntimeException("Configuration file \""+filename+"\" doesn't exist"));
		}

		String songtitle = (args.length == 2 ? args[1] : null);

		// initialize log4j
		PropertyConfigurator.configureAndWatch("log4j.properties",60*1000);

		logger = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());
		logger.debug("Starting");

		long randomSeed = (songtitle != null ? songtitle.toLowerCase().hashCode() : System.nanoTime());
		
		logger.debug("Main random seed: "+randomSeed);
		
		try {
			// instantiate this class so we can launch a thread
			SoundHelix soundHelix = new SoundHelix(filename,randomSeed);
			
			// launch song generation thread with low priority
			Thread t = new Thread(soundHelix);
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();

			// increase priority of the current thread
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			
			while(true) {
				// this call will block until a SongQueueEntry has been generated by the thread			
				SongQueueEntry entry = soundHelix.getNextSongQueueEntry();

				Arrangement arrangement = entry.arrangement;
				Player player = entry.player;
	
				// create shutdown hook
				Thread shutdownHook = new Thread(soundHelix.new ShutdownRunnable(player));
				Runtime.getRuntime().addShutdownHook(shutdownHook);

				try {
					player.open();
					player.play(arrangement);
					player.close();
				} catch(Exception e) {
					logger.warn("Exception during playback",e);
				}
				
                // remove shutdown hook
				Runtime.getRuntime().removeShutdownHook(shutdownHook);
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
			Random random = new Random(randomSeed);
			
			try {
				if(songQueue.size() < 1) {
					// the queue is empty; render a new song

					System.out.println("Rendering new song");

					File file = new File(filename);

					logger.debug("Reading and parsing XML file");
					
					DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					Document doc = builder.parse(file);
					
					XPath xpath = XPathFactory.newInstance().newXPath();

					// preprocess document tree by expanding include tags, if present
					XMLUtils.expandIncludeTags(random,doc,xpath);
					
					// get the root element of the file (we don't care what it's called)
					Node mainNode = (Node)xpath.evaluate("/*",doc,XPathConstants.NODE);

					Node structureNode = (Node)xpath.evaluate("structure",mainNode,XPathConstants.NODE);
					Node harmonyEngineNode = (Node)xpath.evaluate("harmonyEngine",mainNode,XPathConstants.NODE);
					Node arrangementEngineNode = (Node)xpath.evaluate("arrangementEngine",mainNode,XPathConstants.NODE);
					Node playerNode = (Node)xpath.evaluate("player",mainNode,XPathConstants.NODE);

					Structure structure = parseStructure(random,structureNode,xpath);
					HarmonyEngine harmonyEngine = XMLUtils.getInstance(HarmonyEngine.class,harmonyEngineNode,xpath,randomSeed^47357892832l);
					structure.setHarmonyEngine(harmonyEngine);	

					long startTime = System.nanoTime();
					ArrangementEngine arrangementEngine = XMLUtils.getInstance(ArrangementEngine.class,arrangementEngineNode,xpath,randomSeed^123454893l);
					arrangementEngine.setStructure(structure);
					Arrangement arrangement = arrangementEngine.render();
					long time = System.nanoTime()-startTime;

					System.out.println("Rendering took "+(time/1000000)+" ms");
					Player player = XMLUtils.getInstance(Player.class,playerNode,xpath,randomSeed^5915925127l);					
					songQueue.add(new SongQueueEntry(arrangement,player));
				}
			} catch(Exception e) {e.printStackTrace();}
			
			randomSeed ^= random.nextLong();

			try {
				Thread.sleep(5000);
			} catch(Exception e) {}
		}
	}
	
	/**
	 * Removes and returns the next SongQueueEntry from the queue. This method will
	 * block until an entry is available.
	 * 
	 * @return the next SongQueueEntry
	 */
	
	public SongQueueEntry getNextSongQueueEntry() throws InterruptedException {
		return songQueue.take();
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
	
	public static Structure parseStructure(Random random,Node node,XPath xpath) throws XPathException {
		int bars = XMLUtils.parseInteger(random,"bars",node,xpath);
		int beatsPerBar = XMLUtils.parseInteger(random,"beatsPerBar",node,xpath);
		int ticksPerBeat = XMLUtils.parseInteger(random,"ticksPerBeat",node,xpath);

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
	
	private class SongQueueEntry
	{
		Arrangement arrangement;
		Player player;
		
		public SongQueueEntry(Arrangement arrangement,Player player) {
			this.arrangement = arrangement;
			this.player = player;
		}
	}
	
	/**
	 * Implements a simple shutdown hook that can be run when the
	 * JVM exits. The hook currently mutes all channels if the current
	 * player is a MIDI player. Note that shutdown hooks are only run when the
	 * JVM exits normally, e.g., by pressing CTRL+C, calls to System.exit()
	 * or uncaught exceptions. If the JVM is killed however, (e.g., using
	 * SIGTERM), shutdown hooks are not run.
	 */
	
	private class ShutdownRunnable implements Runnable {
		private Player player;
		
		public ShutdownRunnable(Player player) {
			this.player = player;
		}
		
		public void run() {
			try {
				// FIXME: this is a quick and dirty solution
				
				// the preferred solution would be to call
				// player.close().  However, calling close()
				// can cause the player to throw exceptions because
				// the player thread doesn't seem to be already
				// terminated when the shutdown hook is called, and
				// so the player may be using already closed resources.
				
				if(player instanceof MidiPlayer) {
					((MidiPlayer)player).muteAllChannels();
				}
			} catch(Exception e) {}
		}
	}
}
