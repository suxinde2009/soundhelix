package com.soundhelix;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.soundhelix.player.MidiPlayer;
import com.soundhelix.player.Player;
import com.soundhelix.remotecontrol.ConsoleRemoteControl;
import com.soundhelix.remotecontrol.RemoteControl;
import com.soundhelix.util.SongUtils;
import com.soundhelix.util.VersionUtils;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;

/**
 * Implements the main class. The main() method determines the configuration file and then waits for the next generated song and plays it. The
 * configuration parsing and generation of songs is done in a separate thread to guarantee seamless playing. The thread priority for the song
 * generator is set to a low value, the priority of the playing thread is set to a high value.
 *
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

// TODO: provide a DTD or XML Schema for the configuration file

public class SoundHelix implements Runnable {

    /** The logger. */
    private static Logger logger;

    /** Flag indicating whether a new song should be generated. */
    private static boolean generateNew;

    /** The queue for generated songs. */
    private BlockingQueue<Player> songQueue = new LinkedBlockingQueue<Player>();

    /** The XML document URL. */
    private URL url;

    /** The random seed. */
    private long randomSeed;

    /** The song name. */
    private String songName;

    public SoundHelix(URL url, long randomSeed) {
        this.url = url;
        this.randomSeed = randomSeed;
        this.songName = null;
    }

    public SoundHelix(URL url, String songName) {
        this.url = url;
        this.randomSeed = 0;
        this.songName = songName;
    }

    public static void main(String[] args) throws Exception {
        LongOpt[] longopts = new LongOpt[] {
            new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
            new LongOpt("song-name", LongOpt.REQUIRED_ARGUMENT, null, 's'),
            new LongOpt("show-midi-devices", LongOpt.NO_ARGUMENT, null, 'm'),
        };

        Getopt g = new Getopt("SoundHelix", args, "-hs:m", longopts);

        String songName = null;
        String filename = null;
        boolean showHelp = false;
        boolean showMidiDevices = false;

        int c;
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'h':
                    showHelp = true;
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
                    }
                    break;
                case '?':
                    return;
                default:
            }
        }


        int count = 0;

        if (showHelp) {
            count++;
        }

        if (showMidiDevices) {
            count++;
        }

        if (count > 1) {
            System.out.println("Use one of \"--help\" and \"--show-midi-devices\"");
            return;
        }

        if (showHelp) {
            System.out.println("Usage: java -jar SoundHelix.jar [options] xml-filename");
            System.out.println();
            System.out.println("Options:");
            System.out.println();
            System.out.println("   -h");
            System.out.println("   --help                 Show this help");
            System.out.println();
            System.out.println("   -s songname");
            System.out.println("   --song-name songname   Set the song name for seeding the random generator");
            System.out.println();
            System.out.println("   -m");
            System.out.println("   --show-midi-devices    Show available MIDI devices with MIDI IN port");
            return;
        }

        if (showMidiDevices) {
            showMidiDevices();
            return;
        }

        if (filename == null) {
            System.out.println("No XML filename provided");
            return;
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
                Player player = soundHelix.getNextSongFromQueue();
                generateNew = false;

                // create shutdown hook
                Thread shutdownHook = new Thread(new ShutdownRunnable(player));
                Runtime.getRuntime().addShutdownHook(shutdownHook);

                try {
                    player.open();
                    remoteControl.setPlayer(player);
                    player.play();
                    remoteControl.setPlayer(null);
                    player.close();
                } catch (Exception e) {
                    logger.warn("Exception during playback", e);
                }

                generateNew = true;

                // remove shutdown hook
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
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
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Removes and returns the next SongQueueEntry from the queue. This method will block until an entry is available.
     *
     * @return the next SongQueueEntry
     *
     * @throws InterruptedException if interrupted
     */

    public Player getNextSongFromQueue() throws InterruptedException {
        return songQueue.take();
    }

    /**
     * Prints all available MIDI devices.
     */

    private static void showMidiDevices() {
        System.out.println("Available MIDI devices with MIDI IN:");
        System.out.println();

        List<String> list = new ArrayList<String>();
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        for (MidiDevice.Info info : infos) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);

                if (device != null && device.getReceiver() != null) {
                    list.add(info.getName());
                }
            } catch (Exception e) {}
        }

        String[] array = new String[list.size()];
        Arrays.sort(list.toArray(array));

        int num = 0;
        for (String device : array) {
            System.out.println("Device " + (++num) + ": \"" + device + "\"");
        }
    }

    /**
     * Implements a simple shutdown hook that can be run when the JVM exits. The hook currently mutes all channels if the current player is a MIDI
     * player. Note that shutdown hooks are only run when the JVM exits normally, e.g., by pressing CTRL+C, calls to System.exit() or uncaught
     * exceptions. If the JVM is killed however, (e.g., using SIGTERM), shutdown hooks are not run.
     */

    private static class ShutdownRunnable implements Runnable {
        /** The player. */
        private Player player;

        public ShutdownRunnable(Player player) {
            this.player = player;
        }

        public void run() {
            logger.trace("Starting shutdown hook");

            try {
                // FIXME: this is a quick and dirty solution

                // the preferred solution would be to call player.close(). However, calling close() can cause the player to throw exceptions because
                // the player thread doesn't seem to be already terminated when the shutdown hook is called, and so the player may be using already
                // closed resources.

                if (player instanceof MidiPlayer) {
                    logger.trace("Muting all MIDI channels");
                    ((MidiPlayer) player).muteAllChannels();
                }
            } catch (Throwable e) {
                logger.error("Exception during shutdoown hook", e);
            }

            logger.trace("Finished shutdown hook");
        }
    }
}
