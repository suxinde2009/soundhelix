package com.soundhelix.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.lfo.LFO;
import com.soundhelix.misc.Arrangement;
import com.soundhelix.misc.Arrangement.ArrangementEntry;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Sequence.SequenceEntry;
import com.soundhelix.misc.Structure;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.util.StringUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a MIDI player, which can distribute instrument playback to an
 * arbitrary number of MIDI devices in parallel. Each instrument used must be
 * mapped to a combination of MIDI device and MIDI channel. For each channel
 * the MIDI program to use can be defined individually. If no program is
 * specified for a channel, the program is not modified, i.e., the currently selected program is used.
 * All specified MIDI devices are opened for playback, even if they are not used by any
 * instrument. If clock synchronization is enabled for at least one device, the devices
 * are synchronized to the player by sending out TIMING_CLOCK MIDI events to
 * each synchronized device 24 times per beat. For the synchronization to work,
 * each device will be sent a START event before playing and a STOP event after
 * playing. MIDI synchronization works independent of the selected groove. Timing ticks are sent out
 * using a fixed frequency according to the BPM, even though sending out note MIDI messages can vary depending on the
 * selected groove. Clock synchronization should be used for devices using synchronized
 * effects (for example, synchronized echo) in order to communicate the BPM
 * speed to use. As clock synchronization requires some additional overhead,
 * e.g., sending out MIDI messages 24 times per beat instead of the number of
 * ticks per beat, it should only be used if really required.
 * 
 * Timing the ticks (or clock synchronization ticks) is done by using a
 * feedback algorithm based on Thread.sleep() calls with nanosecond resolution. As mentioned, sending out
 * timing ticks is not groove-dependent, whereas sending out note MIDI messages is groove-dependent. 
 * 
 * This player supports LFOs, whose frequency can be based on a second, a beat, the whole song or on the activity (first
 * activity until last activity) of an instrument. The granularity of an LFO is always a tick. With every tick, each
 * LFO will send out a MIDI message with the new value for the target controller, but only if the LFO value is the
 * first one sent or if it has changed since the last value sent.
 * 
 * Instances of this class are not thread-safe. They must not be used in multiple threads without external
 * synchronization.
 *
 * @author Thomas Schürger (thomas@schuerger.com)
 */

// TODO: add possibility to map a virtual channel to several MIDI channels (do we need this)?
// TODO: mute all MIDI channels when player is aborted by ctrl+c (how?)
// TODO: on each tick, send all note-offs before sending note-ons (this is currently done per track, but should be done globally)

public class MidiPlayer extends AbstractPlayer {
    /**
     * The number of MIDI clock synchronization ticks per beat.
     * 24 is the standard MIDI synchronization, 480 is the
     * professional MIDI synchronization.
     */
    private static final int CLOCK_SYNCHRONIZATION_TICKS_PER_BEAT = 24;

    /** The random generator. */
    private Random random;
    
    /** The array of MIDI devices. */
    private Device[] devices;
    
    /** The playback speed in milli-BPM. */
    private int milliBPM;
    
    /** The transposition pitch. */
    private int transposition;
    
    /** The array of groove integers. */
    private int[] groove;
    
    /** The number of ticks to wait before playing. */
    private int beforePlayWaitTicks;
    
    /** The number of ticks to wait after playing. */
    private int afterPlayWaitTicks;
    
    /** The map that maps from channel names to device channels. */
    private Map<String, DeviceChannel> channelMap;
    
    /** The map that maps from device name to MIDI device. */
    private Map<String, Device> deviceMap;
    
    /** The array of controller LFOs. */
    private ControllerLFO[] controllerLFOs;
    
    /** True if open() has been called, false otherwise. */
    private boolean opened;
    
    /** True if at least one MIDI device requires clock synchronization. */
    private boolean useClockSynchronization;

    /** True if playing has been aborted. */
    private boolean isAborted;    

    public MidiPlayer() {
        super();
    }
    
    /**
     * Opens all MIDI devices.
     */
    
    public void open() {
        if (opened) {
            throw new IllegalStateException("open() already called");
        }
        
        try {
            for (Device device : devices) {
                device.open();
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not open MIDI devices", e);
        }
        
        opened = true;
        isAborted = false;
    }
    
    /**
     * Returns a string containing all available MIDI devices in the system that can receive MIDI
     * messages.
     * 
     * @return the string of MIDI devices
     */

    private String getMidiDevices() {
        StringBuilder sb = new StringBuilder();

        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        int num = 0;

        for (MidiDevice.Info info : infos) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);

                if (device != null && device.getReceiver() != null) {           
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    sb.append("MIDI device " + (++num) + ": \"" + info.getName() + "\"");
                }
            } catch (Exception e) {}
        }

        return sb.toString();
    }
    
    /**
     * Closes all MIDI devices.
     */
    
    public final void close() {
        if (devices != null && opened) {
            try {
                muteAllChannels();
            } catch (Exception e) {}
            
            try {
                for (Device device : devices) {
                    device.close();
                }
            } catch (Exception e) {
                throw new RuntimeException("Could not close MIDI devices");
            }
            
            devices = null;
            opened = false;
        }
    }
    
    /**
     * Sets the MIDI devices.
     * 
     * @param devices the MIDI devices
     */
    
    private void setDevices(Device[] devices) {
        deviceMap = new HashMap<String, Device>();
        
        boolean useClockSynchronization = false;
        
        for (Device device : devices) {
            if (deviceMap.containsKey(device.name)) {
                throw new RuntimeException("Device name \"" + device.name + "\" used more than once");
            }
            
            deviceMap.put(device.name, device);
            useClockSynchronization |= device.useClockSynchronization;
        }
        
        this.devices = devices;
        this.useClockSynchronization = useClockSynchronization;
    }

    /**
     * Gets the number of beats per minute for playback.
     * 
     * @return the number of millibeats per minute
     */
    
    public int getMilliBPM() {
        return milliBPM;
    }

    /**
     * Sets the number of beats per minute for playback.
     * 
     * @param milliBPM the number of millibeats per minute
     */
    
    public void setMilliBPM(int milliBPM) {
        if (milliBPM <= 0) {
            throw new IllegalArgumentException("BPM must be > 0");
        }
        
        logger.debug("Setting BPM to " + (milliBPM / 1000f));
        
        this.milliBPM = milliBPM;
    }

    public void setTransposition(int transposition) {
        if (transposition <= 0) {
            throw new IllegalArgumentException("transposition must be >= 0");
        }
        
        this.transposition = transposition;
    }

    /**
     * Sets the groove for playback. A groove is a comma-separated list of
     * integers acting as relative weights for tick lengths. The player cycles
     * through this list while playing and uses the list for timing ticks. For
     * example, the string "5,3" results in a ratio of 5:3, namely 5/8 of the
     * total tick length on every even tick and 3/8 of the tick length for
     * every odd tick. If even and odd ticks originally had a length of 100 ms
     * each, then they would be 125 ms and 75 ms, respectively. The default
     * groove (i.e., no groove) is "1", resulting in equally timed ticks.
     * Note that even though the groove is handled correctly by the player, it
     * might not be handled as expected on the MIDI device used for playback.
     * For example, if some time-synchronized echo is used on the MIDI device,
     * it might sound strange if grooved input is used for a non-grooved echo.
     * 
     * @param grooveString the groove string
     */

    public final void setGroove(String grooveString) {
        if (grooveString == null || grooveString.equals("")) {
            grooveString = "1";
        }
        
        String[] grooveList = grooveString.split(",");
        int len = grooveList.length;

        int sum = 0;
        
        for (String s : grooveList) {
            sum += Integer.parseInt(s);
        }
        
        groove = new int[len];
        int totalGroove = 0;
        
        for (int i = 0; i < len; i++) {
            groove[i] = 1000 * len * Integer.parseInt(grooveList[i]) / sum;
            totalGroove += groove[i];
        }
        
        // we want a total groove of len*1000
        // totalGroove might be a little off due to rounding
        // errors
        
        // correct last groove entry, if necessary, to have the
        // correct total groove
        
        groove[len - 1] -= totalGroove - len * 1000;
    }
    
    /**
     * Sets the channel map, which maps instruments to MIDI
     * devices and channels. All used instruments must be mapped.
     * 
     * @param channelMap the channel map
     */
    
    public void setChannelMap(Map<String, DeviceChannel> channelMap) {
        this.channelMap = channelMap;
    }
    
    /**
     * Takes a comma-separated list of MIDI devices and instantiates the first one that is available in the system.
     * 
     * @param namesString comma-separated list of MIDI device names
     * 
     * @return a first instantiated MIDI device or null if none of the devices are available
     */
    
    private MidiDevice findMidiDevice(String namesString) {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        Map<String, MidiDevice.Info> map = new HashMap<String, MidiDevice.Info>();
        
        for (MidiDevice.Info info : infos) {
            map.put(info.getName(), info);
        } 
            
        String[] names = StringUtils.split(namesString, ',');
 
        for (String name : names) {
            MidiDevice.Info info = map.get(name);
            
            if (info != null) {
                // device was found, try to create an instance
                
                try {
                    MidiDevice midiDevice = MidiSystem.getMidiDevice(info);
                    logger.debug("Successfully opened MIDI device \"" + name + "\"");
                    return midiDevice;
                } catch (Exception e) {
                    logger.debug("MIDI device \"" + name + "\" could not be instantiated", e);
                }
            } else {
                logger.debug("MIDI device \"" + name + "\" was not found");
            }
        }
        
        // none of the devices were found or were instantiable
        return null;
    }
      
    public void play() {
        Arrangement arrangement = this.arrangement;
        
        if (!opened) {
            throw new IllegalStateException("Must call open() first");
        }

        try {
            initializeControllerLFOs(arrangement);
            muteAllChannels();
            setChannelPrograms();            

            Structure structure = arrangement.getStructure();
            int ticksPerBeat = structure.getTicksPerBeat();
            int ticks = structure.getTicks();

            // when clock synchronization is used, we must make sure that
            // the ticks per beat divide CLOCK_SYNCHRONIZATION_TICKS_PER_BEAT
            
            if (useClockSynchronization && (CLOCK_SYNCHRONIZATION_TICKS_PER_BEAT % structure.getTicksPerBeat()) != 0) {
                throw new RuntimeException("Ticks per beat (" + structure.getTicksPerBeat() 
                        + ") must be a divider of " + CLOCK_SYNCHRONIZATION_TICKS_PER_BEAT
                        + " for MIDI clock synchronization");
            }
            
            int clockTimingsPerTick =  useClockSynchronization
                    ? CLOCK_SYNCHRONIZATION_TICKS_PER_BEAT / structure.getTicksPerBeat() : 1;

            if (logger.isDebugEnabled()) {
                logger.debug("Song length: " + ticks + " ticks ("
                             + (ticks * 60 * 1000 / (structure.getTicksPerBeat() * milliBPM)) + " seconds @ "
                             + ((double) milliBPM / 1000) + " BPM)");
            }
            
            if (useClockSynchronization) {
                sendMidiMessageToClockSynchronized(ShortMessage.START);
            }

            long referenceTime = System.nanoTime();
            
            if (beforePlayWaitTicks > 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Waiting " + beforePlayWaitTicks + " ticks before playing");
                }
                
                // wait specified number of ticks before starting playing, sending timing ticks if configured
                referenceTime = waitTicks(referenceTime, beforePlayWaitTicks,
                            clockTimingsPerTick, structure.getTicksPerBeat());
            }
        
            /** Contains the remaining ticks of each note/pause currently played by a voice of an arrangement entry. */
            List<int[]> tickList = new ArrayList<int[]>();
            /** Contains the pattern position currently played by a voice of an arrangement entry. */
            List<int[]> posList = new ArrayList<int[]>();
            /**
             * Contains the pitch used when the last note was played by a voice of an arrangement entry.
             * This is used to be able to change the transposition while playing and still being able to send the
             * correct NOTE_OFF pitches.
             */ 
            List<int[]> pitchList = new ArrayList<int[]>();

            for (ArrangementEntry entry : arrangement) {
                int size = entry.getTrack().size();
                tickList.add(new int[size]);
                posList.add(new int[size]);
                pitchList.add(new int[size]);
            }

            int currentTick = 0;
            
            long tickReferenceTime = referenceTime;
            long timingTickReferenceTime = useClockSynchronization ? referenceTime : Long.MAX_VALUE;
            
            // note that we use <= here; this is to make sure that the very last tick is completely processed
            // (including timing ticks); otherwise (with <) the loop would end as soon as the last tick has been
            // played, but the remaining timing ticks for the last tick still need to be processed
            
            while (currentTick <= ticks && !isAborted) {
                int tick = currentTick;
                
                // wait until the next event
                referenceTime = waitNanos(tickReferenceTime, timingTickReferenceTime);

                // in each iteration, at least one of the following two conditions should be true
                
                if (referenceTime >= timingTickReferenceTime) {
                       if (useClockSynchronization) {
                           sendMidiMessageToClockSynchronized(ShortMessage.TIMING_CLOCK);
                       }
                    timingTickReferenceTime += getTimingTickNanos(clockTimingsPerTick, ticksPerBeat);
                }

                if (referenceTime >= tickReferenceTime) {
                    if (tick == ticks) {
                        break;
                    }
                    playTick(arrangement, tick, tickList, posList, pitchList);
                    tickReferenceTime += getTickNanos(tick, ticksPerBeat);
                    currentTick++;
                }
            }
            
            // playing finished, send a NOTE_OFF for all current notes
            
            muteActiveChannels(arrangement, posList, pitchList);
        
            if (afterPlayWaitTicks > 0) {
                   if (logger.isDebugEnabled()) {
                    logger.debug("Waiting " + afterPlayWaitTicks + " ticks after playing");
                }

                   waitTicks(referenceTime, afterPlayWaitTicks, clockTimingsPerTick, structure.getTicksPerBeat());
            }
            
            if (useClockSynchronization) {
                sendMidiMessageToClockSynchronized(ShortMessage.STOP);
            }
        } catch (Exception e) {
            throw new RuntimeException("Playback error", e);
        }
    }

    /**
     * Mutes all active channels.
     * 
     * @param arrangement the arrangement
     * @param posList the list of positions
     * @param pitchList the list of pitches
     * 
     * @throws InvalidMidiDataException in case of invalid MIDI data
     */
    
    private void muteActiveChannels(Arrangement arrangement, List<int[]> posList, List<int[]> pitchList)
        throws InvalidMidiDataException {
        int k = 0;
        
        for (ArrangementEntry entry : arrangement) {
            Track track = entry.getTrack();
            String instrument = entry.getInstrument();

            DeviceChannel channel = channelMap.get(instrument);
            
            if (channel == null) {
                throw new RuntimeException("Instrument " + instrument
                        + " not mapped to MIDI device/channel combination");
            }

            int[] p = posList.get(k);
            int[] pitch = pitchList.get(k);

            for (int j = 0; j < p.length; j++) {
                Sequence s = track.get(j);                    
                SequenceEntry prevse = s.get(p[j] - 1);

                if (prevse.isNote()) {
                    sendMidiMessage(channel, ShortMessage.NOTE_OFF, pitch[j], 0);
                }
            }
            
            k++;
        }
    }

    /**
     * Plays a tick, sending NOTE_OFF messages for notes that should be muted and NOTE_ON messages for notes that
     * should be started.
     * 
     * @param arrangement the arrangement
     * @param tick the tick
     * @param tickList the tick list
     * @param posList the position list
     * @param pitchList the pitch list
     * 
     * @throws InvalidMidiDataException in case of invalid MIDI data
     */
    
    private void playTick(Arrangement arrangement, int tick, List<int[]> tickList, List<int[]> posList,
            List<int[]> pitchList) throws InvalidMidiDataException {
        Structure structure = arrangement.getStructure();
        int ticksPerBar = structure.getTicksPerBar();

        if ((tick % (4 * ticksPerBar)) == 0) {
            logger.debug(String.format("Tick: %5d   Seconds: %4d   Progress: %5.1f %%", tick,
                    tick * 60 * 1000 / (structure.getTicksPerBeat() * milliBPM),
                    (double) tick * 100 / structure.getTicks()));
        }

        sendControllerLFOMessages(tick);

        // contains a list of all current pitches where a NOTE_OFF must be sent after the next NOTE_ON (usually tiny)
        List<Integer> legatoList = new ArrayList<Integer>();
        
        int k = 0;
        
        int transposition = this.transposition;
        
        for (ArrangementEntry entry : arrangement) {
            Track track = entry.getTrack();
            String instrument = entry.getInstrument();

            DeviceChannel channel = channelMap.get(instrument);
            
            if (channel == null) {
                throw new RuntimeException("Instrument " + instrument
                                           + " not mapped to MIDI device/channel combination");
            }

            int[] t = tickList.get(k);
            int[] p = posList.get(k);
            int[] pitches = pitchList.get(k);
            
            legatoList.clear();
            
            for (int j = 0; j < t.length; j++) {

                if (--t[j] <= 0) {
                    Sequence s = track.get(j);

                    if (p[j] > 0) {
                        SequenceEntry prevse = s.get(p[j] - 1);
                        if (prevse.isNote()) {
                            int pitch = pitches[j];
                            
                            if (!prevse.isLegato()) {
                                sendMidiMessage(channel, ShortMessage.NOTE_OFF, pitch, 0);
                            } else  {
                                // remember pitch for NOTE_OFF after the next NOTE_ON
                                legatoList.add(pitch);
                            }
                        }
                    }
                }
            }

            for (int j = 0; j < t.length; j++) {
                if (t[j] <= 0) {
                    try {
                        Sequence s = track.get(j);
                        SequenceEntry se = s.get(p[j]);

                        if (se.isNote()) {
                            int pitch = (track.getType() == TrackType.MELODY ? transposition : 0) + se.getPitch();
                            sendMidiMessage(channel, ShortMessage.NOTE_ON, pitch, getMidiVelocity(se.getVelocity()));

                            // remove pitch if it is on the legato list (rare case)
                            // (Integer) must be used to call the remove(Object), and not remove(int) method
                            legatoList.remove((Integer) pitch);

                            pitches[j] = pitch;
                        }

                        p[j]++;
                        t[j] = se.getTicks();
                    } catch (Exception e) {
                        throw new RuntimeException("Error at k=" + k + "  j=" + j + "  p[j]=" + p[j], e);
                    }
                }
            }

            // send NOTE_OFFs for all pitches on the legato list
            
            for (int pitch : legatoList) {
                sendMidiMessage(channel, ShortMessage.NOTE_OFF, pitch, 0);
            }
            
            k++;
        }
    }

    /**
     * Initializes all controller LFOs of the arrangement.
     * 
     * @param arrangement the arrangement
     */
    
    private void initializeControllerLFOs(Arrangement arrangement) {
        Structure structure = arrangement.getStructure();
        
        for (ControllerLFO clfo : controllerLFOs) {
            if (clfo.rotationUnit.equals("song")) {
                clfo.lfo.setPhase((int) (1000000d * clfo.phase));
                clfo.lfo.setSongSpeed((int) (1000f * clfo.speed), structure.getTicks(), milliBPM);
                
            } else if (clfo.rotationUnit.equals("activity")) {
                // if the instrument is inactive or not part of the song, we
                // use the whole song as the length (this LFO is then a no-op)
                
                int[] ticks = getInstrumentActivity(arrangement, clfo.instrument);
                
                int startTick = 0;
                int endTick = structure.getTicks();
                
                if (ticks != null) {
                    startTick = ticks[0];
                    endTick = ticks[1];

                    if (startTick >= endTick) {
                        // track belonging to instrument is silent all the time
                        startTick = 0;
                        endTick = structure.getTicks();
                    }
                }
                
                clfo.lfo.setPhase((int) (1000000d * clfo.phase));
                clfo.lfo.setActivitySpeed((int) (1000f * clfo.speed), startTick, endTick, milliBPM);
            } else if (clfo.rotationUnit.equals("beat")) {
                clfo.lfo.setPhase((int) (1000000d * clfo.phase));
                clfo.lfo.setBeatSpeed((int) (1000f * clfo.speed), structure.getTicksPerBeat(), milliBPM);
            } else if (clfo.rotationUnit.equals("second")) {
                clfo.lfo.setPhase((int) (1000000d * clfo.phase));
                clfo.lfo.setTimeSpeed((int) (1000f * clfo.speed), structure.getTicksPerBeat(), milliBPM);
            } else {
                throw new RuntimeException("Invalid rotation unit \"" + clfo.rotationUnit + "\"");
            }
        }
    }

    /**
     * Sends messages to all configured controllers based on the LFOs.
     * A message is only send to a controller if its LFO value has changed or if tick is 0.
     *
     * @param tick the tick
     * 
     * @throws InvalidMidiDataException in case of invalid MIDI data
     */
    
    private void sendControllerLFOMessages(int tick) throws InvalidMidiDataException {
        for (ControllerLFO clfo : controllerLFOs) {
            int value = clfo.lfo.getTickValue(tick);
            
            if (tick == 0 || value != clfo.lastSentValue) {
                // value has changed or is the first value, send message
                
                String controller = clfo.controller;                
                Device device = deviceMap.get(clfo.deviceName);
                
                if (controller.equals("pitchBend")) {
                    sendMidiMessage(device, clfo.channel, ShortMessage.PITCH_BEND, value % 128, value / 128);
                } else if (controller.equals("modulationWheel")) {
                    sendMidiMessage(device, clfo.channel, ShortMessage.CONTROL_CHANGE, 1, value);
                } else if (controller.equals("breath")) {
                    sendMidiMessage(device, clfo.channel, ShortMessage.CONTROL_CHANGE, 2, value);
                } else if (controller.equals("footPedal")) {
                    sendMidiMessage(device, clfo.channel, ShortMessage.CONTROL_CHANGE, 4, value);
                } else if (controller.equals("volume")) {
                    sendMidiMessage(device, clfo.channel, ShortMessage.CONTROL_CHANGE, 7, value);
                } else if (controller.equals("balance")) {
                    sendMidiMessage(device, clfo.channel, ShortMessage.CONTROL_CHANGE, 8, value);
                } else if (controller.equals("pan")) {
                    sendMidiMessage(device, clfo.channel, ShortMessage.CONTROL_CHANGE, 10, value);
                } else if (controller.equals("expression")) {
                    sendMidiMessage(device, clfo.channel, ShortMessage.CONTROL_CHANGE, 11, value);
                } else if (controller.equals("effect1")) {
                    sendMidiMessage(device, clfo.channel, ShortMessage.CONTROL_CHANGE, 12, value);
                } else if (controller.equals("effect2")) {
                    sendMidiMessage(device, clfo.channel, ShortMessage.CONTROL_CHANGE, 13, value);
                } else if (controller.equals("milliBPM")) {
                    setMilliBPM(value);
                } else {
                    throw new RuntimeException("Invalid LFO controller \"" + controller + "\"");
                }
                
                clfo.lastSentValue = value;
            }
        }
    }

    /**
     * Waits the given number of ticks, sending out TIMING_CLOCK events to
     * the MIDI devices, if necessary. Waiting is done by using a simple feedback
     * algorithm that tries hard to keep the player exactly in sync with the
     * system clock.
     * 
     * @param referenceTime the reference time (from System.nanoTime())
     * @param ticks the number of ticks to wait
     * @param clockTimingsPerTick the number of clock timings per tick
     * @param ticksPerBeat the number of ticks per beat
     *
     * @return the new reference time
     * 
     * @throws InvalidMidiDataException in case of invalid MIDI data
     * @throws InterruptedException in case of sleep interruption
     */
    
    private long waitTicks(long referenceTime, int ticks, int clockTimingsPerTick, int ticksPerBeat)
        throws InvalidMidiDataException, InterruptedException {
        long lastWantedNanos = referenceTime;
        
        for (int t = 0; t < ticks && !isAborted; t++) {
            for (int s = 0; s < clockTimingsPerTick; s++) {                    
            
                long length = getTimingTickNanos(clockTimingsPerTick, ticksPerBeat);
                
                long wantedNanos = lastWantedNanos + length;
                long wait = Math.max(0, wantedNanos - System.nanoTime());

                if (wait > 0) {
                    Thread.sleep((int) (wait / 1000000L), (int) (wait % 1000000L));
                }

                if (useClockSynchronization) {
                    sendMidiMessageToClockSynchronized(ShortMessage.TIMING_CLOCK);
                }

                lastWantedNanos = wantedNanos;
            }
        }
 
        return lastWantedNanos;
    }

    /**
     * Waits until either time1 or time2 (both are given in nano seconds) is reached, whichever comes first. Both
     * times are based on System.nanoTime(). If time1 or time2 is in the past, this method returns immediately.
     * In all cases, the time waited on is returned (either time1 or time2).
     * 
     * @param time1 the first point in time
     * @param time2 the second point in time
     * 
     * @return the point in time waited on (minimum of time1 and time2)
     *
     * @throws InterruptedException in case of sleep interruption
     */
    
    private long waitNanos(long time1, long time2) throws InterruptedException {
        long wantedNanos = Math.min(time1, time2);
        long wait = Math.max(0, wantedNanos - System.nanoTime());

        if (wait > 0) {
            Thread.sleep((int) (wait / 1000000L), (int) (wait % 1000000L));
        }
        
        return wantedNanos;
    }
     
    /**
     * Returns the number of nanos of the given tick, taking the current groove
     * into account.
     * 
     * @param tick the tick
     * @param ticksPerBeat the number of ticks per beat
     * 
     * @return the number of nanos
     */
    
    private long getTickNanos(int tick, int ticksPerBeat) {
        return 60000000000L * groove[tick % groove.length] / ((long) ticksPerBeat * milliBPM);
    }

    /**
     * Returns the number of nanos for a timing tick.
     * 
     * @param ticksPerBeat the ticks per beat
     * @param clockTimingsPerTick the clock timings per tick
     * 
     * @return the number of nanos for a timing tick
     */
    
    private long getTimingTickNanos(int ticksPerBeat, int clockTimingsPerTick) {
        return 60000000000000L / ((long) ticksPerBeat * milliBPM * clockTimingsPerTick);
    }
    
    /**
     * Sets the channel programs of all DeviceChannels used. This
     * method does not set the program of a DeviceChannel more than
     * once. Channels whose program is set to -1 are ignored, so that
     * the currently selected program remains active.
     * 
     * @throws InvalidMidiDataException in case of invalid MIDI data
     */
    
    private void setChannelPrograms() throws InvalidMidiDataException {
        // we use a Map to track whether a program has been set already

        Map<DeviceChannel, Boolean> map = new HashMap<DeviceChannel, Boolean>();
        Iterator<DeviceChannel> i = channelMap.values().iterator();

        while (i.hasNext()) {
            DeviceChannel dc = i.next();

            if (dc.program != -1 && !map.containsKey(dc)) {
                sendMidiMessage(dc, ShortMessage.PROGRAM_CHANGE, dc.program, 0);
                map.put(dc, true);
            }
        }
    }

    /**
     * Sends the given single-byte message to all devices that are using clock synchronization.
     * 
     * @param status the message
     * 
     * @throws InvalidMidiDataException in case of invalid MIDI data
     */
    
    private void sendMidiMessageToClockSynchronized(int status) throws InvalidMidiDataException {
        Iterator<Device> iter = deviceMap.values().iterator();
        
        while (iter.hasNext()) {
            Device device = iter.next();
            
            if (device.useClockSynchronization) {
                sendMidiMessage(device, status);
            }
        }
    }

    /**
     * Mutes all channels of all devices. This is done by sending an ALL
     * SOUND OFF message to all channels. In addition to that (because this
     * does not include sending NOTE OFF) a NOTE_OFF is sent for each of the
     * 128 possible pitches to each channel.
     * 
     * @throws InvalidMidiDataException in case of invalid MIDI data
     */
    
    public final void muteAllChannels() throws InvalidMidiDataException {
        Iterator<DeviceChannel> iter = channelMap.values().iterator();
        
        while (iter.hasNext()) {
            DeviceChannel dc = iter.next();
            
            // send ALL SOUND OFF message
            sendMidiMessage(dc, ShortMessage.CONTROL_CHANGE, 120, 0);

            // send ALL NOTES OFF message (doesn't work on all MIDI devices)
            sendMidiMessage(dc, ShortMessage.CONTROL_CHANGE, 123, 0);

            for (int i = 0; i < 128; i++) {
                sendMidiMessage(dc, ShortMessage.NOTE_OFF, i, 0);
            }
            
            try {
                Thread.sleep(460);
            } catch (Exception e) {}
        }
    }

    /**
     * Converts our internal velocity (between 0 and Short.MAX_VALUE) to
     * a MIDI velocity (between 0 and 127).
     * 
     * @param velocity the velocity to convert
     * 
     * @return the MIDI velocity
     */
    
    private static int getMidiVelocity(short velocity) {
        if (velocity == 0) {
            return 0;
        }

        return 1 + (velocity - 1) * 126 / (Short.MAX_VALUE - 126);      
    }
    
    public final void setControllerLFOs(ControllerLFO[] controllerLFOs) {
        this.controllerLFOs = controllerLFOs;
    }
    
    /**
     * Checks if the given instrument is part of the arrangement and if so, determines the
     * tick of the first note and the tick of the end of the last note plus 1. The start and
     * end ticks are returned as a two-dimensional int array. If the instrument is not found or
     * the instrument's track contains no note, null is returned.
     * 
     * @param arrangement the arrangement
     * @param instrument the number of the instrument
     * 
     * @return a two-dimensional int array containing start and end tick (or null)
     */
    
    private static int[] getInstrumentActivity(Arrangement arrangement, String instrument) {
        for (ArrangementEntry entry : arrangement) {
            if (entry.getInstrument().equals(instrument)) {
                // instrument found, check for first and last tick
            
                Track track = entry.getTrack();
                
                int startTick = Integer.MAX_VALUE;
                int endTick = Integer.MIN_VALUE;
                
                for (int k = 0; k < track.size(); k++) {
                    Sequence seq = track.get(k);
                    int ticks = seq.getTicks();
                    
                    int tick = 0;
                    int j = 0;
                    
                    while (tick < ticks) {
                        SequenceEntry se = seq.get(j++);
                        
                        if (se.isNote()) {
                            if (tick < startTick) {
                                startTick = tick;
                            }
                            if (tick + se.getTicks() > endTick) {
                                endTick = tick + se.getTicks();
                            }
                        }
                        
                        tick += se.getTicks();
                    }
                }
                
                if (startTick == Integer.MAX_VALUE) {
                    // instrument was present but completely silent
                    return null;
                } else {
                    // both startTick and endTick contain a proper value
                    return new int[] {startTick, endTick};
                }
            }
        }
        
        // instrument was not found
        return null;
    }
    
    public final void configure(Node node, XPath xpath) throws XPathException {
        random = new Random(randomSeed);
        
        NodeList nodeList = (NodeList) xpath.evaluate("device", node, XPathConstants.NODESET);
        int entries = nodeList.getLength();
        Device[] devices = new Device[entries];
        
        for (int i = 0; i < entries; i++) {
            String name = (String) xpath.evaluate("attribute::name", nodeList.item(i), XPathConstants.STRING);
            String midiName = XMLUtils.parseString(random, nodeList.item(i), xpath);            
            boolean useClockSynchronization = XMLUtils.parseBoolean(random, "attribute::clockSynchronization",
                    nodeList.item(i), xpath);
            devices[i] = new Device(name, midiName, useClockSynchronization);
        }
        
        setDevices(devices);    
        setMilliBPM((int) (1000
                * XMLUtils.parseInteger(random, (Node) xpath.evaluate("bpm", node, XPathConstants.NODE), xpath)));
        setTransposition(XMLUtils.parseInteger(random,
                (Node) xpath.evaluate("transposition", node, XPathConstants.NODE), xpath));
        setGroove(XMLUtils.parseString(random,
                (Node) xpath.evaluate("groove", node, XPathConstants.NODE), xpath));
        setBeforePlayWaitTicks(XMLUtils.parseInteger(random,
                (Node) xpath.evaluate("beforePlayWaitTicks", node, XPathConstants.NODE), xpath));
        setAfterPlayWaitTicks(XMLUtils.parseInteger(random,
                (Node) xpath.evaluate("afterPlayWaitTicks", node, XPathConstants.NODE), xpath));
        
        nodeList = (NodeList) xpath.evaluate("map", node, XPathConstants.NODESET);
        entries = nodeList.getLength();
        
        Map<String, DeviceChannel> channelMap = new HashMap<String, DeviceChannel>();
        
        for (int i = 0; i < entries; i++) {
            String instrument = (String) xpath.evaluate("attribute::instrument",
                    nodeList.item(i), XPathConstants.STRING);
            String device = (String) xpath.evaluate("attribute::device", nodeList.item(i), XPathConstants.STRING);
            int channel = Integer.parseInt((String) xpath.evaluate("attribute::channel",
                    nodeList.item(i), XPathConstants.STRING)) - 1;

            if (channelMap.containsKey(instrument)) {
                throw new RuntimeException("Instrument " + instrument + " must not be re-mapped");
            }
            
            if (!deviceMap.containsKey(device)) {
                throw new RuntimeException("Device \"" + device + "\" unknown");
            }

            int program = -1;
            
            try {
                program = Integer.parseInt((String) xpath.evaluate("attribute::program",
                        nodeList.item(i), XPathConstants.STRING)) - 1;
            } catch (Exception e) {
            }
            
            DeviceChannel ch = new DeviceChannel(deviceMap.get(device), channel, program);            
            channelMap.put(instrument, ch);
        }
    
        setChannelMap(channelMap);
        
        nodeList = (NodeList) xpath.evaluate("controllerLFO", node, XPathConstants.NODESET);
        entries = nodeList.getLength();
        ControllerLFO[] controllerLFOs = new ControllerLFO[entries];
        
        for (int i = 0; i < entries; i++) {
            int minimum = XMLUtils.parseInteger(random,
                    (Node) xpath.evaluate("minimum", nodeList.item(i), XPathConstants.NODE), xpath);
            int maximum = XMLUtils.parseInteger(random,
                    (Node) xpath.evaluate("maximum", nodeList.item(i), XPathConstants.NODE), xpath);
            double speed = XMLUtils.parseDouble(random,
                    (Node) xpath.evaluate("speed", nodeList.item(i), XPathConstants.NODE), xpath);
            
            String controller = XMLUtils.parseString(random,
                    (Node) xpath.evaluate("controller", nodeList.item(i), XPathConstants.NODE), xpath);

            String device = null;
            int channel = -1;
            
            if (!controller.equals("milliBPM")) {
                device = XMLUtils.parseString(random,
                    (Node) xpath.evaluate("device", nodeList.item(i), XPathConstants.NODE), xpath);
                channel = XMLUtils.parseInteger(random,
                    (Node) xpath.evaluate("channel", nodeList.item(i), XPathConstants.NODE), xpath) - 1;
            }
            
            String rotationUnit = XMLUtils.parseString(random,
                    (Node) xpath.evaluate("rotationUnit", nodeList.item(i), XPathConstants.NODE), xpath);
            
            double phase = 0.0d;
            String instrument = null;
            
            try {
                phase = XMLUtils.parseDouble(random,
                        (Node) xpath.evaluate("phase", nodeList.item(i), XPathConstants.NODE), xpath);
            } catch (Exception e) {
            }
            
            try {
                instrument = XMLUtils.parseString(random,
                        (Node) xpath.evaluate("instrument", nodeList.item(i), XPathConstants.NODE), xpath);
            } catch (Exception e) {
            }

            if (rotationUnit.equals("activity") && (instrument == null || instrument.equals(""))) {
                throw new RuntimeException("Rotation unit \"activity\" requires an instrument");
            }
            
            Node lfoNode = (Node) xpath.evaluate("lfo",  nodeList.item(i), XPathConstants.NODE);
            
            LFO lfo;
            
            try {
                lfo = XMLUtils.getInstance(LFO.class, lfoNode, xpath, randomSeed, i);
            } catch (Exception e) {
                throw new RuntimeException("Could not instantiate LFO", e);
            }

            lfo.setAmplitudeMinimum(minimum);
            lfo.setAmplitudeMaximum(maximum);

            controllerLFOs[i] = new ControllerLFO(lfo, device, channel, controller, instrument,
                                                      speed, rotationUnit, phase);
        }
        
        setControllerLFOs(controllerLFOs);
    }

    /**
     * Sends a MIDI message with the given status, data1 and data2 to the given device channel.
     * 
     * @param deviceChannel the device channel
     * @param status the MIDI status
     * @param data1 the first MIDI data
     * @param data2 the second MIDI data
     *
     * @throws InvalidMidiDataException in case of invalid MIDI data
     */
        
    private void sendMidiMessage(DeviceChannel deviceChannel, int status, int data1, int data2)
        throws InvalidMidiDataException {
        ShortMessage sm = new ShortMessage();        
        sm.setMessage(status, deviceChannel.channel, data1, data2);
        deviceChannel.device.receiver.send(sm, -1);
    }

    /**
     * Sends a MIDI message with the given status, data1 and data2 to the given channel on the given device.
     * 
     * @param device the device
     * @param channel the channel number (0-15)
     * @param status the MIDI status
     * @param data1 the first MIDI data
     * @param data2 the second MIDI data
     *
     * @throws InvalidMidiDataException in case of invalid MIDI data
     */

    private void sendMidiMessage(MidiPlayer.Device device, int channel, int status, int data1, int data2)
        throws InvalidMidiDataException {
        ShortMessage sm = new ShortMessage();        
        sm.setMessage(status, channel, data1, data2);
        device.receiver.send(sm, -1);
    }

    /**
     * Sends a MIDI message with the given status to the given device.
     * 
     * @param device the device
     * @param status the MIDI status
     *
     * @throws InvalidMidiDataException in case of invalid MIDI data
     */

    private void sendMidiMessage(Device device, int status) throws InvalidMidiDataException {
        ShortMessage sm = new ShortMessage();        
        sm.setMessage(status);
        device.receiver.send(sm, -1);
    }

    
    public void abortPlay() {
        this.isAborted = true;
    }

    public void setBeforePlayWaitTicks(int preWaitTicks) {
        this.beforePlayWaitTicks = preWaitTicks;
    }

    public void setAfterPlayWaitTicks(int postWaitTicks) {
        this.afterPlayWaitTicks = postWaitTicks;
    }
    
    private final class Device {
        private final String name;
        private final String midiName;
        private MidiDevice midiDevice;
        private Receiver receiver;
        private boolean useClockSynchronization;
        
        public Device(String name, String midiName, boolean useClockSynchronization) {
            if (name == null || name.equals("")) {
                throw new IllegalArgumentException("Name must not be null or empty");
            }

            if (midiName == null || midiName.equals("")) {
                throw new IllegalArgumentException("MIDI device name must not be null or empty");
            }

            this.name = name;
            this.midiName = midiName;
            this.useClockSynchronization = useClockSynchronization;
        }
        
        public void open() {
               try {
                   midiDevice = findMidiDevice(midiName);

                if (midiDevice == null) {
                       throw new RuntimeException("Could not find MIDI device \"" + midiName
                               + "\". Available devices with MIDI IN:\n" + getMidiDevices());
                }

                midiDevice.open();

                receiver = midiDevice.getReceiver();

                if (receiver == null) {
                    throw new RuntimeException("MIDI device \"" + midiName
                                               + "\" does not have a Receiver. Available devices with MIDI IN:\n"
                                               + getMidiDevices());
                }
            } catch (Exception e) {
                throw new RuntimeException("Error opening MIDI device \"" + midiName + "\"", e);
            }
        }
        
        public void close() {
            if (midiDevice != null) {
                midiDevice.close();
                midiDevice = null;
            }
        }
    }
    
    public static class DeviceChannel {
        private final Device device;
        private final int channel;
        private final int program;
         
        public DeviceChannel(Device device, int channel, int program) {
            this.device = device;
            this.channel = channel;
            this.program = program;
        }

        public final boolean equals(Object object) {
            if (!(object instanceof DeviceChannel)) {
                return false;
            } else if (this == object) {
                return true;
            }
            
            DeviceChannel other = (DeviceChannel) object;            
            return device.equals(other.device) && channel == other.channel && program == other.program;
        }

        public final int hashCode() {
            return device.hashCode() * 16273 + channel * 997 + program;
        }        
    }
    
    private static class ControllerLFO {
        private LFO lfo;
        private final String deviceName;
        private int channel;
        private String controller;
        private String instrument;
        private double speed;
        private String rotationUnit;
        private double phase;
        private int lastSentValue;

        public ControllerLFO(LFO lfo, String deviceName, int channel, String controller, String instrument,
                double speed, String rotationUnit, double phase) {
            this.lfo = lfo;
            this.deviceName = deviceName;
            this.channel = channel;
            this.controller = controller;
            this.instrument = instrument;
            this.speed = speed;
            this.rotationUnit = rotationUnit;
            this.phase = phase;
        }
    }
}
