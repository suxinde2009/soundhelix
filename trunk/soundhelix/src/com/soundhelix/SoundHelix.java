package com.soundhelix;

import java.io.File;
import java.io.FileInputStream;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.soundhelix.constants.BuildConstants;
import com.soundhelix.player.MidiPlayer;
import com.soundhelix.player.Player;
import com.soundhelix.remotecontrol.ConsoleRemoteControl;
import com.soundhelix.remotecontrol.RemoteControl;
import com.soundhelix.util.SongUtils;

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

	/** The logger. */
    private static Logger logger;

    /** Flag indicating whether a new song should be generated. */
	private static boolean generateNew = false;
	
	/** The queue for generated songs. */
	private BlockingQueue<Player> songQueue = new LinkedBlockingQueue<Player>();

	/** The XML configuration file. */
	private String filename;

	/** The random seed. */
	private long randomSeed;

	private String songName = null;
	
	public SoundHelix(String filename,long randomSeed) {
		this.filename = filename;
		this.randomSeed = randomSeed;
		this.songName = null;
	}
	
	public SoundHelix(String filename,String songName) {
	    this.filename = filename;
	    this.randomSeed = 0;
	    this.songName = songName;
	}

	public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("-h") || args.length > 2) {
            System.out.println("java SoundHelix [XML-File [Songtitle]] ");
            System.exit(0);
        }

        // initialize log4j
        PropertyConfigurator.configureAndWatch("log4j.properties",60 * 1000);

        logger = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());
		logger.info("SoundHelix " + BuildConstants.VERSION + " (r" + BuildConstants.REVISION + "), built on "
						   + BuildConstants.BUILD_DATE);
		
		String filename = (args.length >= 1 ? args[0] : "SoundHelix.xml");
		
		if (!new File(filename).exists()) {
			throw(new RuntimeException("Configuration file \"" + filename + "\" doesn't exist"));
		}

		String songName = (args.length == 2 ? args[1] : null);

		long randomSeed = 0;
		
		if (songName != null && !songName.equals("")) {
			if (songName.startsWith("seed:")) {
				randomSeed = Long.parseLong(songName.substring(5));
			}
		} else {
			randomSeed = new Random().nextLong();
		}
		
		try {
			// instantiate this class so we can launch a thread
		    SoundHelix soundHelix;
		    
		    if (songName != null && !songName.equals("")) {
		        soundHelix = new SoundHelix(filename,songName);
		    } else {
                soundHelix = new SoundHelix(filename,randomSeed);		        
		    }
			// launch song generation thread with low priority
			Thread t = new Thread(soundHelix,"Generator");
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();

			RemoteControl remoteControl = new ConsoleRemoteControl();		
			
			Thread consoleThread = new Thread(remoteControl,"Console");
			consoleThread.setPriority(Thread.MIN_PRIORITY);
			consoleThread.start();

            // increase priority of the current thread for playback
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

			generateNew = true;

			while (true) {
				// this call will block until a song has been generated by the thread			
				Player player = soundHelix.getNextSongFromQueue();
				generateNew = false;

				// create shutdown hook
				Thread shutdownHook = new Thread(soundHelix.new ShutdownRunnable(player));
				Runtime.getRuntime().addShutdownHook(shutdownHook);

				try {
					player.open();
					remoteControl.setPlayer(player);
					player.play();
					remoteControl.setPlayer(null);
					player.close();
				} catch (Exception e) {
					logger.warn("Exception during playback",e);
				}
				
				generateNew = true;
				
                // remove shutdown hook
				Runtime.getRuntime().removeShutdownHook(shutdownHook);
			}
		} catch (Exception e) {
			logger.warn("Exception detected",e);
			throw(e);
		}
    }
	
	/**
	 * Implements the functionality of the thread that generates songs. The method runs forever and generates a new
	 * song as soon as it detects the queue to be empty.
	 */
	
	public void run() {
        long randomSeed = this.randomSeed;
        Random random;
        
        if (songName != null) {
            random = new Random();
        } else {
            random = new Random(randomSeed);
        }
        
        while (true) {
			try {
				if (songQueue.size() < 1 && generateNew) {
					// the queue is empty; render a new song
				    
				    if (songName != null) {
				        songQueue.add(SongUtils.generateSong(new File(filename),songName));
				    } else {
				        songQueue.add(SongUtils.generateSong(new File(filename),randomSeed));
				    }
				    
				    songName = null;
					randomSeed = random.nextLong();
				}
			} catch (Exception e) {
				logger.warn("Exception occurred", e);
			}
			
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
			}
		}
	}
	
	/**
	 * Removes and returns the next SongQueueEntry from the queue. This method will
	 * block until an entry is available.
	 * 
	 * @return the next SongQueueEntry
	 */
	
	public Player getNextSongFromQueue() throws InterruptedException {
		return songQueue.take();
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
		/** The player. */
		private Player player;
		
		public ShutdownRunnable(Player player) {
			this.player = player;
		}
		
		public void run() {
			try {
				// FIXME: this is a quick and dirty solution
				
				// the preferred solution would be to call
				// player.close(). However, calling close()
				// can cause the player to throw exceptions because
				// the player thread doesn't seem to be already
				// terminated when the shutdown hook is called, and
				// so the player may be using already closed resources.
				
				if (player instanceof MidiPlayer) {
					((MidiPlayer)player).muteAllChannels();
				}
			} catch (Exception e) {
			}
		}
	}
}
