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
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Structure;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Arrangement.ArrangementEntry;
import com.soundhelix.misc.Sequence.SequenceEntry;
import com.soundhelix.misc.Track.TrackType;
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
 * LFO will send out a MIDI message with the new value for the target controller, but only if the LFO value has
 * changed.
 * 
 * Instances of this class are not thread-safe. They must not be used in multiple threads without external
 * synchronization.
 * 
 * <h3>XML configuration</h3>
 * <table border=1>
 * <tr><th>Tag</th> <th>#</th> <th>Attributes</th> <th>Description</th> <th>Required</th>
 * <tr><td><code>device</code></td> <td>1</td> <td><code>name</code>, <code>clockSynchronization</code></td> <td>Specifies the MIDI device to make available using the given name.</td> <td>yes</td>
 * <tr><td><code>bpm</code></td> <td>1</td> <td></td> <td>Specifies the beats per minute to use.</td> <td>yes</td>
 * <tr><td><code>transposition</code></td> <td>1</td> <td></td> <td>Specifies the transposition in halftones to use. Pitches are generated at around 0, so for MIDI the transposition must be something around 60.</td> <td>yes</td>
 * <tr><td><code>groove</code></td> <td>1</td> <td></td> <td>Specifies the groove to use. See method setGroove().</td> <td>yes</td>
 * <tr><td><code>map</code></td> <td>*</td> <td><code>instrument</code>, <code>device</code>, <code>channel</code>, <code>program</code> (optional)</td> <td>Maps the instrument specified by <i>instrument</i> to MIDI device <i>device</i> and channel <i>channel</i>.</td> <td>no</td>
 * </table>
 *
 * <h3>Configuration example</h3>
 *
 * <pre>
 * &lt;player class="MidiPlayer"&gt;
 *   &lt;device name="device1" clockSynchronization="true"&gt;Out To MIDI Yoke:  1&lt;/device&gt;
 *   &lt;device name="device2" clockSynchronization="false"&gt;Out To MIDI Yoke:  2&lt;/device&gt;
 *   &lt;bpm&gt;&lt;random min="130" max="150" type="normal" mean="140" variance="6"/&gt;&lt;/bpm&gt;
 *   &lt;transposition&gt;&lt;random min="64" max="70"/&gt;&lt;/transposition&gt;
 *   &lt;groove&gt;&lt;random list="100,100|110,90|115,85|120,80|115,85,120,80"/&gt;&lt;/groove&gt;
 *   &lt;map instrument="0" device="device1" channel="8" program="13"/&gt;
 *   &lt;map instrument="1" device="device2" channel="7"/&gt;
 * &lt;/player&gt;
 * </pre>
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

// TODO: add possibility to map a virtual channel to several MIDI channels (do we need this)?
// TODO: mute all MIDI channels when player is aborted by ctrl+c (how?)
// TODO: allow setting BPM in a fine-grained fashion (with at least milli-BPM resolution)
// TODO: on each tick, send all note-offs before sending note-ons (this is currently done per track, but should be done globally)

public class MidiPlayer extends AbstractPlayer {
	/**
	 * The number of MIDI clock synchronization ticks per beat.
	 * 24 is the standard MIDI synchronization, 480 is the
	 * professional MIDI synchronization.
	 */
	private static final int CLOCK_SYNCHRONIZATION_TICKS_PER_BEAT = 24;

	private Random random;
	
	private Device[] devices;
	
	private int milliBPM;
	private int transposition;
	private int[] groove;
	
	private int beforePlayWaitTicks;
	private int afterPlayWaitTicks;
	
	private Map<Integer,DeviceChannel> channelMap;
	private Map<String,Device> deviceMap;
	
	private ControllerLFO[] controllerLFOs;
	
	// has open() been called?
	boolean opened = false;
	
	// true if at least one MIDI device requires clock synchronization
	boolean useClockSynchronization = false;

    private boolean isAborted;    

    public MidiPlayer() {
    	super();
    }
	
    /**
     * Opens all MIDI devices.
     */
    
    public void open() {
    	if (opened) {
    		throw(new IllegalStateException("open() already called"));
    	}
    	
    	try {
    		for (Device device : devices) {
    			device.open();
    		}
    	} catch (Exception e) {
    		throw(new RuntimeException("Could not open MIDI devices",e));
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
    			throw(new RuntimeException("Could not close MIDI devices"));
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
    	deviceMap = new HashMap<String,Device>();
    	
    	boolean useClockSynchronization = false;
    	
    	for (Device device : devices) {
    		if (deviceMap.containsKey(device.name)) {
    			throw(new RuntimeException("Device name \"" + device.name + "\" used more than once"));
    		}
    		
    		deviceMap.put(device.name,device);
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
    		throw(new IllegalArgumentException("BPM must be > 0"));
    	}
    	
    	System.out.println("Setting BPM to " + (milliBPM / 1000f));
    	
    	this.milliBPM = milliBPM;
    }

    private void setTransposition(int transposition) {
    	if (transposition <= 0) {
    		throw(new IllegalArgumentException("transposition must be >= 0"));
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
    
    public void setChannelMap(Map<Integer,DeviceChannel> channelMap) {
    	this.channelMap = channelMap;
    }
    
	private static MidiDevice findMidiDevice(String name) {
    	MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

    	for (MidiDevice.Info info : infos) {
    		if (info.getName().equals(name)) {
    			try {
    				return MidiSystem.getMidiDevice(info);
    			} catch (Exception e) {
    				return null;
    			}
    		}
    	}
    	
    	return null;
    }
      
    public void play(Arrangement arrangement) {
    	if (!opened) {
    		throw(new RuntimeException("Must call open() first"));
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
    			throw new RuntimeException("Ticks per beat (" + structure.getTicksPerBeat() + 
    					") must be a divider of " + CLOCK_SYNCHRONIZATION_TICKS_PER_BEAT +
    					" for MIDI clock synchronization");
    		}
    		
        	int clockTimingsPerTick = (useClockSynchronization ?
        			CLOCK_SYNCHRONIZATION_TICKS_PER_BEAT / structure.getTicksPerBeat() : 1);

    		List<int[]> tickList = new ArrayList<int[]>();
    		List<int[]> posList = new ArrayList<int[]>();

    		if (logger.isDebugEnabled()) {
    			logger.debug("Song length: "+ticks+" ticks ("+(ticks*60*1000/(structure.getTicksPerBeat()*milliBPM))+" seconds)");
    		}
    		
            if (useClockSynchronization) {
                sendShortMessageToClockSynchronized(ShortMessage.START);
            }

    		long referenceTime = System.nanoTime();
    		
    		// wait specified number of ticks before starting playing, sending timing ticks if configured
            referenceTime = waitTicks(referenceTime,beforePlayWaitTicks,
            				clockTimingsPerTick,structure.getTicksPerBeat());
 
            for (ArrangementEntry entry : arrangement) {
    			int size = entry.getTrack().size();
    			tickList.add(new int[size]);
    			posList.add(new int[size]);
            }

    		List<Integer> legatoList = new ArrayList<Integer>();

    		int currentTick = 0;
    		
    		long tickReferenceTime = referenceTime;
    		long timingTickReferenceTime = useClockSynchronization ? referenceTime : Long.MAX_VALUE;
    		
    		// note that we use <= here; this is to make sure that the very last tick is completely processed
    		// (including timing ticks); otherwise (with <) the loop would end as soon as the last tick has been
    		// played, but the remaining timing ticks for the last tick still need to be processed
    		
    		while (currentTick <= ticks && !isAborted) {
    			int tick = currentTick;
    			
    			// wait until the next event
    			referenceTime = waitNanos(tickReferenceTime,timingTickReferenceTime);

    			// in each iteration, at least one of the following two conditions should be true
    			
    			if (referenceTime >= timingTickReferenceTime) {
       				if (useClockSynchronization) {
       					sendShortMessageToClockSynchronized(ShortMessage.TIMING_CLOCK);
       				}
    				timingTickReferenceTime += getTimingTickNanos(clockTimingsPerTick, ticksPerBeat);
    			}

    			if (referenceTime >= tickReferenceTime) {
    				if (tick == ticks) {
    					break;
    				}
    				playTick(arrangement, tick, tickList, posList, legatoList);
    				tickReferenceTime += getTickNanos(tick, ticksPerBeat);
    				currentTick++;
    			}
    		}
    		
    		// playing finished	
    		// send a NOTE_OFF for all current notes
    		
    		muteActiveChannels(arrangement, posList);
    	
            waitTicks(referenceTime,afterPlayWaitTicks,clockTimingsPerTick,structure.getTicksPerBeat());

    		if (useClockSynchronization) {
    		    sendShortMessageToClockSynchronized(ShortMessage.STOP);
    		}
    	} catch (Exception e) {
    		throw(new RuntimeException("Playback error",e));
    	}
    }

	private void muteActiveChannels(Arrangement arrangement, List<int[]> posList) throws InvalidMidiDataException {
		ShortMessage sm = new ShortMessage();

		int k = 0;
		
		for (ArrangementEntry entry : arrangement) {
			Track track = entry.getTrack();
			int instrument = entry.getInstrument();

			DeviceChannel channel = channelMap.get(instrument);
			
			if (channel == null) {
				throw(new RuntimeException("Instrument " + instrument +
						" not mapped to MIDI device/channel combination"));
			}

			int[] p = posList.get(k);

			for (int j = 0; j < p.length; j++) {
				Sequence s = track.get(j);					
				SequenceEntry prevse = s.get(p[j] - 1);

				if (prevse.isNote()) {
					sm.setMessage(ShortMessage.NOTE_OFF,channel.channel,
							(track.getType() == TrackType.MELODY ? transposition : 0) + prevse.getPitch(),0);
					channel.device.receiver.send(sm,-1);
				}
			}
			
			k++;
		}
	}

	private void playTick(Arrangement arrangement, int tick, List<int[]> tickList, List<int[]> posList,
			List<Integer> legatoList)
			throws InvalidMidiDataException {
		final ShortMessage sm = new ShortMessage();
		
		Structure structure = arrangement.getStructure();
		int ticksPerBar = structure.getTicksPerBar();

		if ((tick % (4 * ticksPerBar)) == 0) {
			System.out.printf("Tick: %4d   Seconds: %3d  %5.1f %%\n",tick,
					tick * 60 * 1000 / (structure.getTicksPerBeat() * milliBPM),
					(double)tick * 100 / structure.getTicks());
		}

		sendControllerLFOMessages(tick);

		int k = 0;
		
		for (ArrangementEntry entry : arrangement) {
			Track track = entry.getTrack();
			int instrument = entry.getInstrument();

			DeviceChannel channel = channelMap.get(instrument);
			
			if (channel == null) {
				throw(new RuntimeException("Instrument " + instrument +
						" not mapped to MIDI device/channel combination"));
			}

			int[] t = tickList.get(k);
			int[] p = posList.get(k);

			legatoList.clear();
			
			for (int j = 0; j < t.length; j++) {

				if (--t[j] <= 0) {
					Sequence s = track.get(j);

					if (p[j] > 0) {
						SequenceEntry prevse = s.get(p[j] - 1);
						if (prevse.isNote()) {
							int pitch = (track.getType() == TrackType.MELODY ? transposition : 0)+prevse.getPitch();
							
							if (!prevse.isLegato()) {
								sm.setMessage(ShortMessage.NOTE_OFF,channel.channel,pitch,0);
								channel.device.receiver.send(sm,-1);
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
							int pitch = ((track.getType() == TrackType.MELODY ? transposition : 0)+se.getPitch());
							sm.setMessage(ShortMessage.NOTE_ON,channel.channel,pitch,getMidiVelocity(se.getVelocity()));
							channel.device.receiver.send(sm,-1);
							
							// remove pitch if it is on the legato list
							legatoList.remove((Integer)pitch);
						}

						p[j]++;
						t[j] = se.getTicks();
					} catch (Exception x) {
						throw new RuntimeException("Error at k=" + k + "  j=" + j + "  p[j]=" + p[j],x);
						}
				}
			}

			// send NOTE_OFFs for all pitches on the legato list
			
			for (int pitch : legatoList) {
				sm.setMessage(ShortMessage.NOTE_OFF,channel.channel,pitch,0);
				channel.device.receiver.send(sm,-1);
			}
			
			k++;
		}
	}

	private void initializeControllerLFOs(Arrangement arrangement) {
		Structure structure = arrangement.getStructure();
		
		for (ControllerLFO clfo : controllerLFOs) {
			if (clfo.rotationUnit.equals("song")) {
				clfo.lfo.setPhase((int)(1000000d * clfo.phase));
				clfo.lfo.setSongSpeed((int)(1000f * clfo.speed),structure.getTicks(),milliBPM);
				
			} else if (clfo.rotationUnit.equals("activity")) {
				
				// if the instrument is inactive or not part of the song, we
				// use the whole song as the length (this LFO is then a no-op)
				
				int[] ticks = getInstrumentActivity(arrangement,clfo.instrument);
				
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
				
				clfo.lfo.setPhase((int)(1000000d * clfo.phase));
				clfo.lfo.setActivitySpeed((int)(1000f * clfo.speed),startTick,endTick,milliBPM);    				
			} else if (clfo.rotationUnit.equals("beat")) {
				clfo.lfo.setPhase((int)(1000000d * clfo.phase));
				clfo.lfo.setBeatSpeed((int)(1000f * clfo.speed),structure.getTicksPerBeat(),milliBPM);
			} else if (clfo.rotationUnit.equals("second")) {
				clfo.lfo.setPhase((int)(1000000d * clfo.phase));
				clfo.lfo.setTimeSpeed((int)(1000f * clfo.speed),structure.getTicksPerBeat(),milliBPM);
			} else {
				throw(new RuntimeException("Invalid rotation unit \"" + clfo.rotationUnit + "\""));
			}
		}
	}

    /**
     * Sends messages to all configured controllers based on the LFOs.
     * A message is only send to a controller if its LFO value has changed or if tick is 0.
     *
     * @param tick the tick
     * 
     * @throws InvalidMidiDataException
     */
    
	private void sendControllerLFOMessages(int tick) throws InvalidMidiDataException {
		ShortMessage sm = new ShortMessage();
		
		for (ControllerLFO clfo : controllerLFOs) {
			int value = clfo.lfo.getTickValue(tick);
			
			if (tick == 0 || value != clfo.lastSentValue) {
				// value has changed or is the first value, send message
				
				String controller = clfo.controller;				
				
				if (controller.equals("pitchBend")) {
					sm.setMessage(ShortMessage.PITCH_BEND,clfo.channel,value % 128,value / 128);
				} else if (controller.equals("modulationWheel")) {
					sm.setMessage(ShortMessage.CONTROL_CHANGE,clfo.channel,1,value);
				} else if (controller.equals("breath")) {
					sm.setMessage(ShortMessage.CONTROL_CHANGE,clfo.channel,2,value);
				} else if (controller.equals("footPedal")) {
					sm.setMessage(ShortMessage.CONTROL_CHANGE,clfo.channel,4,value);
				} else if (controller.equals("volume")) {
					sm.setMessage(ShortMessage.CONTROL_CHANGE,clfo.channel,7,value);
				} else if (controller.equals("balance")) {
					sm.setMessage(ShortMessage.CONTROL_CHANGE,clfo.channel,8,value);
				} else if (controller.equals("pan")) {
					sm.setMessage(ShortMessage.CONTROL_CHANGE,clfo.channel,10,value);
				} else if (controller.equals("expression")) {
					sm.setMessage(ShortMessage.CONTROL_CHANGE,clfo.channel,11,value);
				} else if (controller.equals("effect1")) {
					sm.setMessage(ShortMessage.CONTROL_CHANGE,clfo.channel,12,value);
				} else if (controller.equals("effect2")) {
					sm.setMessage(ShortMessage.CONTROL_CHANGE,clfo.channel,13,value);
				} else {
					throw(new RuntimeException("Invalid LFO controller \"" + controller + "\""));
				}

				Device device = deviceMap.get(clfo.deviceName);
				device.receiver.send(sm,-1);
				
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
    
    private long waitTicks(long referenceTime,int ticks,int clockTimingsPerTick,int ticksPerBeat)
    		throws InvalidMidiDataException,InterruptedException {
    	long lastWantedNanos = referenceTime;
    	
    	for (int t = 0; t < ticks && !isAborted;t++) {
    	    for (int s = 0; s < clockTimingsPerTick;s++) {    				
    		
    	    	long length = getTimingTickNanos(clockTimingsPerTick, ticksPerBeat);
    	    	
				long wantedNanos = lastWantedNanos + length;
				long wait = Math.max(0,wantedNanos - System.nanoTime());

				if (wait > 0) {
				    Thread.sleep((int)(wait / 1000000l),(int)(wait % 1000000l));
				}

    	    	if (useClockSynchronization) {
    				sendShortMessageToClockSynchronized(ShortMessage.TIMING_CLOCK);
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
    
    private long waitNanos(long time1,long time2) throws InterruptedException {
    	long wantedNanos = Math.min(time1,time2);
    	long wait = Math.max(0,wantedNanos - System.nanoTime());

		if (wait > 0) {
			Thread.sleep((int)(wait / 1000000l),(int)(wait % 1000000l));
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
    
	private long getTickNanos(int tick,int ticksPerBeat) {
		return 60000000000l * groove[tick % groove.length] / (ticksPerBeat * milliBPM);
	}

	/**
	 * Returns the number of nanos for a timing tick.
	 * 
	 * @param ticksPerBeat the ticks per beat
	 * @param clockTimingsPerTick the clock timings per tick
	 * 
	 * @return the number of nanos for a timing tick
	 */
	
	private long getTimingTickNanos(int ticksPerBeat,int clockTimingsPerTick) {
		return 60000000000000l / (ticksPerBeat * milliBPM * clockTimingsPerTick);
	}
    
    /**
     * Sets the channel programs of all DeviceChannels used. This
     * method does not set the program of a DeviceChannel more than
     * once. Channels whose program is set to -1 are ignored, so that
     * the currently selected program remains active.
     * 
     * @throws InvalidMidiDataException
     */
    
	private void setChannelPrograms() throws InvalidMidiDataException {
		ShortMessage sm = new ShortMessage();

		// we use a Map to track whether a program has been set already

		Map <DeviceChannel,Boolean> map = new HashMap<DeviceChannel,Boolean>();
		Iterator<DeviceChannel> i = channelMap.values().iterator();

		while (i.hasNext()) {
			DeviceChannel dc = i.next();

			if (dc.program != -1 && !map.containsKey(dc)) {
				sm.setMessage(ShortMessage.PROGRAM_CHANGE,dc.channel,dc.program,0);
				dc.device.receiver.send(sm,-1);
				map.put(dc,true);
			}
		}
	}

    /**
     * Sends the given single-byte message to all devices that are using
     * clock synchronization.
     * 
     * @param message the message
     * 
     * @throws InvalidMidiDataException
     */
    
    private void sendShortMessageToClockSynchronized(int message) throws InvalidMidiDataException {
        ShortMessage sm = new ShortMessage();
        Iterator<Device> iter = deviceMap.values().iterator();
        
        while (iter.hasNext()) {
            Device device = iter.next();
            
            if (device.useClockSynchronization) {
                sm.setMessage(message);
                device.receiver.send(sm,-1);
            }
        }
    }

	/**
	 * Mutes all channels of all devices. This is done by sending an ALL
	 * SOUND OFF message to all channels. In addition to that (because this
	 * does not include sending NOTE OFF) a NOTE_OFF is sent for each of the
	 * 128 possible pitches to each channel.
	 * 
	 * @throws InvalidMidiDataException
	 */
	
	public final void muteAllChannels() throws InvalidMidiDataException {
		ShortMessage sm = new ShortMessage();
		Iterator<DeviceChannel> iter = channelMap.values().iterator();
		
		while (iter.hasNext()) {
			DeviceChannel dc = iter.next();
			
			// send ALL SOUND OFF message
			sm.setMessage(ShortMessage.CONTROL_CHANGE,dc.channel,120,0);
			dc.device.receiver.send(sm,-1);

			// send ALL NOTES OFF message (doesn't work on all MIDI devices)
			//sm.setMessage(ShortMessage.CONTROL_CHANGE,dc.channel,123,0);
			//dc.device.receiver.send(sm,-1);

			for (int i = 0; i < 128; i++) {
				sm.setMessage(ShortMessage.NOTE_OFF,dc.channel,i,0);
				dc.device.receiver.send(sm,-1);
			}
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

    	return(1 + (velocity - 1) * 126 / (Short.MAX_VALUE - 126));  	
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
    
    private static int[] getInstrumentActivity(Arrangement arrangement,int instrument) {
    	for (ArrangementEntry entry : arrangement) {
    		if (entry.getInstrument() == instrument) {
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
    				return new int[] {startTick,endTick};
    			}
    		}
    	}
    	
    	// instrument was not found
    	return null;
    }
    
    public final void configure(Node node,XPath xpath) throws XPathException {
    	random = new Random(randomSeed);
    	
		NodeList nodeList = (NodeList)xpath.evaluate("device",node,XPathConstants.NODESET);
		int entries = nodeList.getLength();
		Device[] devices = new Device[entries];
		
		for (int i = 0; i < entries; i++) {
			String name = (String)xpath.evaluate("attribute::name",nodeList.item(i),XPathConstants.STRING);
			String midiName = XMLUtils.parseString(random,nodeList.item(i),xpath);			
            boolean useClockSynchronization = XMLUtils.parseBoolean(random,"attribute::clockSynchronization",
            		nodeList.item(i),xpath);
			devices[i] = new Device(name,midiName,useClockSynchronization);
		}
		
    	setDevices(devices);	
    	setMilliBPM((int)(1000 *
    			XMLUtils.parseInteger(random,(Node)xpath.evaluate("bpm",node,XPathConstants.NODE),xpath)));
    	setTransposition(XMLUtils.parseInteger(random,
    			(Node)xpath.evaluate("transposition",node,XPathConstants.NODE),xpath));
    	setGroove(XMLUtils.parseString(random,
    			(Node)xpath.evaluate("groove",node,XPathConstants.NODE),xpath));
    	setBeforePlayWaitTicks(XMLUtils.parseInteger(random,
    			(Node)xpath.evaluate("beforePlayWaitTicks",node,XPathConstants.NODE),xpath));
    	setAfterPlayWaitTicks(XMLUtils.parseInteger(random,
    			(Node)xpath.evaluate("afterPlayWaitTicks",node,XPathConstants.NODE),xpath));
    	
		nodeList = (NodeList)xpath.evaluate("map",node,XPathConstants.NODESET);
		entries = nodeList.getLength();
		
		Map<Integer,DeviceChannel> channelMap = new HashMap<Integer,DeviceChannel>();
		
		for (int i = 0; i < entries; i++) {
			int instrument = Integer.parseInt((String)xpath.evaluate("attribute::instrument",
					nodeList.item(i),XPathConstants.STRING));
			String device = ((String)xpath.evaluate("attribute::device",nodeList.item(i),XPathConstants.STRING));
			int channel = Integer.parseInt((String)xpath.evaluate("attribute::channel",
					nodeList.item(i),XPathConstants.STRING));

			if (channelMap.containsKey(instrument)) {
				throw(new RuntimeException("Instrument " + instrument + " must not be re-mapped"));
			}
			
			if (!deviceMap.containsKey(device)) {
				throw(new RuntimeException("Device \"" + device + "\" unknown"));
			}

			int program = -1;
			
			try {
				program = Integer.parseInt((String)xpath.evaluate("attribute::program",
						nodeList.item(i),XPathConstants.STRING));
			} catch (Exception e) {
			}
			
			DeviceChannel ch = new DeviceChannel(deviceMap.get(device),channel,program);			
			channelMap.put(instrument,ch);
		}
	
		setChannelMap(channelMap);
		
		nodeList = (NodeList)xpath.evaluate("lfo",node,XPathConstants.NODESET);
		entries = nodeList.getLength();
		ControllerLFO[] controllerLFOs = new ControllerLFO[entries];
		
		for (int i = 0; i < entries; i++) {
			int minimum = XMLUtils.parseInteger(random,
					(Node)xpath.evaluate("minimum",nodeList.item(i),XPathConstants.NODE),xpath);
			int maximum = XMLUtils.parseInteger(random,
					(Node)xpath.evaluate("maximum",nodeList.item(i),XPathConstants.NODE),xpath);
			double speed = XMLUtils.parseDouble(random,
					(Node)xpath.evaluate("speed",nodeList.item(i),XPathConstants.NODE),xpath);
			
			String device = XMLUtils.parseString(random,
					(Node)xpath.evaluate("device",nodeList.item(i),XPathConstants.NODE),xpath);
			int channel = XMLUtils.parseInteger(random,
					(Node)xpath.evaluate("channel",nodeList.item(i),XPathConstants.NODE),xpath);
			String controller = XMLUtils.parseString(random,
					(Node)xpath.evaluate("controller",nodeList.item(i),XPathConstants.NODE),xpath);
			String rotationUnit = XMLUtils.parseString(random,
					(Node)xpath.evaluate("rotationUnit",nodeList.item(i),XPathConstants.NODE),xpath);
			
			double phase = 0.0d;
			int instrument = -1;
			
			try {
				phase = XMLUtils.parseDouble(random,
						(Node)xpath.evaluate("phase",nodeList.item(i),XPathConstants.NODE),xpath);
			} catch(Exception e) {
			}
			
			try {
				instrument = XMLUtils.parseInteger(random,
						(Node)xpath.evaluate("instrument",nodeList.item(i),XPathConstants.NODE),xpath);
			} catch (Exception e) {
			}
			
			String className = (String)xpath.evaluate("attribute::class",nodeList.item(i),XPathConstants.STRING);

			if (className.indexOf('.') < 0) {
				// prefix the class name with the package name of the superclass
				className = LFO.class.getName().substring(0,LFO.class.getName().lastIndexOf('.') + 1) + className;
			}
			
			try {
				Class<LFO> cl = (Class<LFO>)Class.forName(className);
				LFO lfo = cl.newInstance();

				lfo.setAmplitudeMinimum(minimum);
				lfo.setAmplitudeMaximum(maximum);

				controllerLFOs[i] = new ControllerLFO(lfo,device,channel,controller,
												instrument,speed,rotationUnit,phase);
			} catch (Exception e) {
				throw(new RuntimeException("Could not instantiate LFO",e));
			}
		}
		
		setControllerLFOs(controllerLFOs);
    }
    
    private final class Device {
    	private final String name;
    	private final String midiName;
    	private MidiDevice midiDevice;
    	private Receiver receiver;
    	private boolean useClockSynchronization;
    	
    	public Device(String name,String midiName,boolean useClockSynchronization) {
    		if (name == null || name.equals("")) {
    			throw(new IllegalArgumentException("Name must not be null or empty"));
    		}

    		if (midiName == null || midiName.equals("")) {
    			throw(new IllegalArgumentException("MIDI device name must not be null or empty"));
    		}

    		this.name = name;
    		this.midiName = midiName;
    		this.useClockSynchronization = useClockSynchronization;
    	}
    	
    	public void open() {
           	try {
           		midiDevice = findMidiDevice(midiName);

        		if (midiDevice == null) {
           			throw new RuntimeException("Could not find MIDI device \"" + midiName +
           					"\". Available devices with MIDI IN:\n" + getMidiDevices());
        		}

        		midiDevice.open();

        		receiver = midiDevice.getReceiver();

        		if (receiver == null) {
        			throw new RuntimeException("MIDI device \"" + midiName +
        					"\" does not have a Receiver. Available devices with MIDI IN:\n" + getMidiDevices());
        		}
        	} catch (Exception e) {
        		throw(new RuntimeException("Error opening MIDI device \"" + midiName + "\"",e));
        	}
    	}
    	
    	public void close() {
    		if (midiDevice != null) {
    			midiDevice.close();
    			midiDevice = null;
    		}
    	}
    }
    
    public class DeviceChannel {
    	private final Device device;
    	private final int channel;
    	private final int program;
     	
    	public DeviceChannel(Device device,int channel,int program) {
    		this.device = device;
    		this.channel = channel;
    		this.program = program;
    	}

    	public final boolean equals(Object object) {
    		if (object == null || !(object instanceof DeviceChannel)) {
    			return false;
    		}
    		
    		DeviceChannel other = (DeviceChannel)object;    		
    		return this == other || device.equals(other.device) && channel == other.channel && program == other.program;
    	}

    	public final int hashCode() {
    		return device.hashCode() * 4711 + channel * 513 + program;
    	}    	
    }
    
    public class ControllerLFO {
    	private LFO lfo;
    	private final String deviceName;
    	private int channel;
    	private String controller;
    	private int instrument;
    	private double speed;
    	private String rotationUnit;
    	private double phase;
    	private int lastSentValue;
    	
    	public ControllerLFO(LFO lfo,String deviceName,int channel,String controller,int instrument,double speed,String rotationUnit,double phase) {
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
	
	public void abortPlay() {
		this.isAborted = true;
	}

	public void setBeforePlayWaitTicks(int preWaitTicks) {
		this.beforePlayWaitTicks = preWaitTicks;
	}

	public void setAfterPlayWaitTicks(int postWaitTicks) {
		this.afterPlayWaitTicks = postWaitTicks;
	}
}
