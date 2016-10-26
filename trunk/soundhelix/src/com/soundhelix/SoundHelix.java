package com.soundhelix;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.soundhelix.component.player.Player;
import com.soundhelix.misc.SongContext;
import com.soundhelix.remotecontrol.ConsoleRemoteControl;
import com.soundhelix.remotecontrol.RemoteControl;
import com.soundhelix.util.SongUtils;
import com.soundhelix.util.VersionUtils;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

/**
 * Implements the main class. The main() method determines the configuration file and then waits for the next generated song and plays it. The
 * configuration parsing and generation of songs is done in a separate thread to guarantee seamless playing. The thread priority for the song
 * generator is set to a low value, the priority of the playing thread is set to a high value.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class SoundHelix implements Runnable {

    /** The logger. */
    private static Logger logger;

    /** Flag indicating whether a new song should be generated. */
    private static boolean generateNew;

    /** The queue for generated songs. */
    private BlockingQueue<SongContext> songQueue = new LinkedBlockingQueue<SongContext>();

    /** The XML document URL. */
    private URL url;

    /** The random seed. */
    private long randomSeed;

    /** The song name. */
    private String songName;

    /**
     * Constructor.
     * 
     * @param url the SoundHelix XML URL
     * @param randomSeed the random seed
     */

    public SoundHelix(URL url, long randomSeed) {
        this.url = url;
        this.randomSeed = randomSeed;
        this.songName = null;
    }

    /**
     * Constructor.
     * 
     * @param url the SoundHelix XML URL
     * @param songName the song name
     */

    public SoundHelix(URL url, String songName) {
        this.url = url;
        this.randomSeed = 0;
        this.songName = songName;
    }

    /**
     * The main method.
     * 
     * @param args the arguments
     * 
     * @throws Exception in case of any problem
     */

    public static void main(String[] args) throws Exception {
        Getopt g = parseParameters(args);

        String songName = null;
        String filename = null;
        boolean showHelp = false;
        boolean showVersion = false;
        boolean showMidiDevices = false;

        int c;
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'h':
                    showHelp = true;
                    break;
                case 'v':
                    showVersion = true;
                    break;
                case 'm':
                    showMidiDevices = true;
                    break;
                case 's':
                    songName = g.getOptarg();
                    break;
                case 1:
                    if (filename == null) {
                        filename = g.getOptarg();
                    } else {
                        System.out.println("XML-Filename specified more than once");
                        System.exit(1);
                    }
                    break;
                case '?':
                    System.exit(1);
                    break;
                default:
            }
        }

        int count = 0;

        if (showHelp) {
            count++;
        }

        if (showVersion) {
            count++;
        }

        if (showMidiDevices) {
            count++;
        }

        if (count > 1) {
            System.out.println("Use one of \"--help\", \"--version\" and \"--show-midi-devices\"");
            System.exit(1);
        }

        if (showHelp) {
            System.out.println("Usage: java -jar SoundHelix.jar [options] xml-filename");
            System.out.println("       java -jar SoundHelix.jar [options] xml-url");
            System.out.println();
            System.out.println("Options:");
            System.out.println();
            System.out.println("   -h");
            System.out.println("   --help                 Show this help");
            System.out.println();
            System.out.println("   -v");
            System.out.println("   --version              Show the application version");
            System.out.println();
            System.out.println("   -s songname");
            System.out.println("   --song-name songname   Set the song name for seeding the random generator");
            System.out.println();
            System.out.println("   -m");
            System.out.println("   --show-midi-devices    Show available MIDI devices with MIDI IN/OUT port");
            System.out.println();
            System.out.println("See http://www.soundhelix.com/doc/running for more information.");
            System.exit(0);
        }

        if (showVersion) {
            System.out.println(VersionUtils.getVersion());
            System.exit(0);
        }

        if (showMidiDevices) {
            showMidiDevices();
            System.exit(0);
        }

        if (filename == null) {
            System.out.println("No XML filename provided");
            System.exit(1);
        }

        // initialize log4j
        PropertyConfigurator.configureAndWatch("log4j.properties", 60 * 1000);

        logger = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());
        VersionUtils.logVersion();

        long randomSeed = 0;

        if (songName != null && !songName.equals("")) {
            if (songName.startsWith("seed:")) {
                randomSeed = Long.parseLong(songName.substring(5));
                songName = null;
            }
        } else {
            randomSeed = new Random().nextLong();
        }

        try {
            // instantiate this class so we can launch a thread
            SoundHelix soundHelix;
            URL url;

            if (filename.startsWith("http://") || filename.startsWith("https://") || filename.startsWith("ftp://") || filename.startsWith("file:/")) {
                url = new URL(filename);
            } else {
                url = new File(filename).toURI().toURL();
            }

            if (songName != null && !songName.equals("")) {
                soundHelix = new SoundHelix(url, songName);
            } else {
                soundHelix = new SoundHelix(url, randomSeed);
            }

            // launch song generation thread with low priority
            Thread t = new Thread(soundHelix, "Generator");
            t.setPriority(Thread.MIN_PRIORITY);
            generateNew = true;
            t.start();

            RemoteControl remoteControl = new ConsoleRemoteControl();

            Thread consoleThread = new Thread(remoteControl, "Console");
            consoleThread.setPriority(Thread.MIN_PRIORITY);
            consoleThread.start();

            // increase priority of the current thread for playback
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

            while (true) {
                // this call will block until a song has been generated by the thread
                SongContext songContext = soundHelix.getNextSongFromQueue();
                Player player = songContext.getPlayer();
                generateNew = false;

                // create shutdown hook
                Thread shutdownHook = new Thread(new ShutdownRunnable(player));
                Runtime.getRuntime().addShutdownHook(shutdownHook);

                try {
                    remoteControl.setSongContext(songContext);
                    player.play(songContext);
                    remoteControl.setSongContext(null);
                } catch (Exception e) {
                    logger.warn("Exception during playback", e);
                }

                generateNew = true;

                // remove shutdown hook
                try {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                } catch (IllegalStateException e) {
                    // ignore, happens if the shutdown hook is already running
                }
            }
        } catch (Exception e) {
            logger.warn("Exception detected", e);
            throw e;
        }
    }

    /**
     * Implements the functionality of the thread that generates songs. The method runs forever and generates a new song as soon as it detects the
     * queue to be empty.
     */

    @Override
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
                        songQueue.add(SongUtils.generateSong(url, songName));
                    } else {
                        songQueue.add(SongUtils.generateSong(url, randomSeed));
                    }
                }
            } catch (Exception e) {
                logger.warn("Exception occurred", e);
            }

            songName = null;
            randomSeed = random.nextLong();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * Removes and returns the next SongContext from the queue. This method will block until an entry is available.
     * 
     * @return the next SongContext
     * 
     * @throws InterruptedException if interrupted
     */

    public SongContext getNextSongFromQueue() throws InterruptedException {
        return songQueue.take();
    }

    /**
     * Parse command-line parameters and return a Getopt instance.
     * 
     * @param args the command-line parameters
     * 
     * @return the Getopt instance
     */

    private static Getopt parseParameters(String[] args) {
        LongOpt[] longopts = new LongOpt[] {new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'), new LongOpt("version", LongOpt.NO_ARGUMENT, null,
                'v'), new LongOpt("song-name", LongOpt.REQUIRED_ARGUMENT, null, 's'), new LongOpt("show-midi-devices", LongOpt.NO_ARGUMENT, null,
                        'm')};

        return new Getopt("SoundHelix", args, "-hvs:m", longopts);
    }

    /**
     * Prints all available MIDI devices.
     */

    private static void showMidiDevices() {
        System.out.println("\nAvailable MIDI devices with MIDI IN (can be used for playback):");
        System.out.println();

        String[] array = getMidiDevices(true);

        if (array.length > 0) {
            int num = 0;
            for (String device : array) {
                System.out.println("Device " + (++num) + ": \"" + device + "\"");
            }
        } else {
            System.out.println("None");
        }

        System.out.println("\nAvailable MIDI devices with MIDI OUT (can be used for remote-controlling SoundHelix):");
        System.out.println();
        array = getMidiDevices(false);

        if (array.length > 0) {
            int num = 0;
            for (String device : array) {
                System.out.println("Device " + (++num) + ": \"" + device + "\"");
            }
        } else {
            System.out.println("None");
        }

    }

    /**
     * Gets the available MIDI device names. If midiIn is true, devices with MIDI IN are returned, otherwise devices with MIDI OUT are returned. The
     * device names are returned as a sorted string array.
     * 
     * @param midiIn if true, MIDI in is returned, otherwise MIDI OUT is returned
     * 
     * @return the list of MIDI devices
     */

    private static String[] getMidiDevices(boolean midiIn) {
        List<String> list = new ArrayList<String>();
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        for (MidiDevice.Info info : infos) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);

                if (device != null) {
                    if (midiIn && device.getReceiver() != null || !midiIn && device.getTransmitter() != null) {
                        list.add(info.getName());
                    }
                }
            } catch (Exception e) {}
        }

        String[] array = new String[list.size()];
        Arrays.sort(list.toArray(array));
        return array;
    }

    /**
     * Implements a simple shutdown hook that can be run when the JVM exits. The hook currently mutes all channels if the current player is a MIDI
     * player. Note that shutdown hooks are only run when the JVM exits normally, e.g., by pressing CTRL+C, calls to System.exit() or uncaught
     * exceptions. If the JVM is killed however, (e.g., using SIGKILL), shutdown hooks are not run.
     */

    private static class ShutdownRunnable implements Runnable {
        /** The player. */
        private Player player;

        /**
         * Constructor.
         * 
         * @param player the player
         */

        ShutdownRunnable(Player player) {
            this.player = player;
        }

        @Override
        public void run() {
            logger.trace("Starting shutdown hook");

            try {
                if (player != null) {
                    logger.debug("Aborting playback");
                    player.abortPlay();
                }
            } catch (Exception e) {
                logger.error("Exception during shutdown hook", e);
            }

            logger.trace("Finished shutdown hook");
        }
    }
}
