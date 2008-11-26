package com.soundhelix.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

import com.soundhelix.misc.Arrangement;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Structure;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Arrangement.ArrangementEntry;
import com.soundhelix.misc.Sequence.SequenceEntry;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a MIDI player, which can distribute instrument playback to an arbitrary
 * number of MIDI devices in parallel. Each instrument used must be mapped to a combination
 * of MIDI device and MIDI channel. For each channel the MIDI program to use can be defined
 * individually. If no program is specified for a channel, the program is not modified.
 * All specified MIDI devices are opened for playback, even if they are not used by any
 * instrument. If clock synchronization is enabled for a device, the devices are synchronized
 * to the player by sending out TIMING_CLOCK MIDI events to each synchronized device 24 times
 * per beat. For the synchronization to work, each device will be sent a START event before
 * playing and a STOP event after playing. Note that MIDI synchronization is highly incompatible
 * with setting grooves other than the standard groove (no groove), at least on most MIDI devices.
 * Clock synchronization should be used for devices using synchronized effects (for example, synchronized
 * echo) in order to communicate the BPM speed to use. As clock synchronization requires some additional
 * overhead, e.g., sending out MIDI messages 24 times per beat instead of the number of ticks per
 * beat, it should only be used if really required.
 * 
 * Timing the ticks (or clock synchronization ticks) is done by using a feedback algorithm based on
 * Thread.sleep() calls with nanosecond resolution. 
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

public class MidiPlayer extends Player {
	private Device[] devices;
	
	private int bpm;
	private int transposition;
	private int groove[];
	
	private Map<Integer,DeviceChannel> channelMap;
	private Map<String,Device> deviceMap;
	
	// has open() been called?
	boolean opened = false;
	
	// true if at least one MIDI device requires clock synchronization
	boolean useClockSynchronization = false;
	
    public MidiPlayer() {
    	super();
    }
	
    /**
     * Opens all MIDI devices.
     */
    
    public void open() {
    	if(opened) {
    		throw(new RuntimeException("open() already called"));
    	}
    	
    	try {
    		for(int i=0;i<devices.length;i++) {
    			devices[i].open();
    		}
    	} catch(Exception e) {
    		throw(new RuntimeException("Could not open MIDI devices",e));
    	}
    	
    	opened = true;
    }
    
    /**
     * Returns a string containing all MIDI devices that can receive MIDI
     * messages.
     * 
     * @return the string of MIDI devices
     */

    private String getMidiDevices() {
    	StringBuilder sb = new StringBuilder();

    	MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();

    	int num = 1;

    	for(int i=0;i<info.length;i++) {
    		try {
    			MidiDevice device = MidiSystem.getMidiDevice(info[i]);

    			if(device != null && device.getReceiver() != null) {   		
    				if(sb.length() > 0) {
    					sb.append('\n');
    				}
    				sb.append("MIDI device "+(num++)+": \""+info[i].getName()+"\"");
    			}
    		} catch(Exception e) {}
    	}

    	return sb.toString();
    }
    
    public void close() {
    	if(devices != null && opened) {
    		try {
    			for(int i=0;i<devices.length;i++) {
    				devices[i].close();
    			}
    		} catch(Exception e) {
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
    	
    	for(int i=0;i<devices.length;i++) {
    		if(deviceMap.containsKey(devices[i].name)) {
    			throw(new RuntimeException("Device name \""+devices[i].name+"\" used more than once"));
    		}
    		
    		deviceMap.put(devices[i].name,devices[i]);
    		useClockSynchronization |= devices[i].useClockSynchronization;
    	}
    	
    	this.devices = devices;
    	this.useClockSynchronization = useClockSynchronization;
    }
    
    /**
     * Sets the number of beats per minute for playback.
     * 
     * @param bpm the number of beats per minute
     */
    
    private void setBPM(int bpm) {
    	if(bpm <= 0) {
    		throw(new IllegalArgumentException("BPM must be > 0"));
    	}
    	
    	System.out.println("Setting BPM to "+bpm);
    	
    	this.bpm = bpm;
    }

    private void setTransposition(int transposition) {
    	if(transposition <= 0) {
    		throw(new IllegalArgumentException("transposition must be >= 0"));
    	}
    	
    	this.transposition = transposition;
    }

    /**
     * Sets the groove for playback. A groove is a comma-separated
     * list of integers acting as relative weights for tick lengths.
     * The player cycles through this list while playing and uses the
     * list for timing ticks.
     * For example, the string "5,3" results in a ratio of 5:3, namely
     * 5/8 of the total tick length on every even tick and 3/8 of the tick
     * length for every odd tick. If even and odd ticks originally had a
     * length of 100 ms each, then they would be 125 ms and 75 ms, respectively.
     * The default groove (i.e., no groove) is "1", resulting in equally
     * timed ticks.	Note that even though the groove is handled correctly
     * by the player, it might not be handled as expected on the MIDI
     * device used for playback. For example, if some time-synchronized echo
     * is used on the MIDI device, it might sound strange if grooved input
     * is used for a non-grooved echo.
     * 
     * @param grooveString the groove string
     */

    public void setGroove(String grooveString) {
    	if(grooveString == null || grooveString.equals("")) {
    		grooveString = "1";
    	}
    	
    	System.out.println("Setting groove to "+grooveString);
    	
    	String[] grooveList = grooveString.split(",");
    	int len = grooveList.length;

    	int sum = 0;
    	
		for(int i=0;i<len;i++) {
    		sum += Integer.parseInt(grooveList[i]);
    	}
    	
    	groove = new int[len];
		int totalGroove = 0;
    	
    	for(int i=0;i<len;i++) {
    		groove[i] = 1000*len*Integer.parseInt(grooveList[i])/sum;
    		totalGroove += groove[i];
    	}
    	
    	// we want a total groove of len*1000
    	// totalGroove might be a little off due to rounding
    	// errors
    	
    	// correct last groove entry, if necessary, to have the
    	// correct total groove
    	
    	groove[len-1] -= totalGroove-len*1000;
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
    	MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();

    	for(int i=0;i<info.length;i++) {
    		if(info[i].getName().equals(name)) {
    			try {
    				return MidiSystem.getMidiDevice(info[i]);
    			} catch(Exception e) {return null;}
    		}
    	}
    	
    	return null;
    }
      
    public void play(Arrangement arrangement) {
    	if(!opened) {
    		throw(new RuntimeException("Must call open() first"));
    	}
    	
    	try {
    		Structure structure = arrangement.getStructure();

        	int clockTimingsPerTick = (useClockSynchronization ? 24/structure.getTicksPerBeat() : 1);

    		List<int[]> tickList = new ArrayList<int[]>();
    		List<int[]> posList = new ArrayList<int[]>();

    		System.out.println("Song length: "+(structure.getTicks()*60/(structure.getTicksPerBeat()*bpm))+" seconds");

			muteAllChannels();
            setChannelPrograms();            

            if(useClockSynchronization) {
                sendShortMessageToClockSynchronized(ShortMessage.START);
            }

    		ShortMessage sm = new ShortMessage();

    		Iterator<ArrangementEntry> i = arrangement.iterator();

    		// initialize internal sequence pointers
    		
    		while(i.hasNext()) {
    			ArrangementEntry ae = i.next();
    			int size = ae.getTrack().size();
    			tickList.add(new int[size]);
    			posList.add(new int[size]);
    			//sm.setMessage(ShortMessage.STOP,ae.getChannel(),0,0);
    		}

    		int tick = 0;

    		int ticksPerBar = structure.getTicksPerBar();
    		
    		long nanos = System.nanoTime();
    		long lastWantedNanos = nanos;

    		while(tick < structure.getTicks()) {
    			i = arrangement.iterator();

    			if((tick % ticksPerBar) == 0) {
    				System.out.printf("Tick: %4d   Seconds: %3d  %5.1f %%\n",tick,tick*60/(structure.getTicksPerBeat()*bpm),(double)tick*100d/(double)structure.getTicks());
    			}
    			
    			for(int k=0;i.hasNext();k++) {
    				ArrangementEntry e = i.next();
    				Track track = e.getTrack();
    				int instrument = e.getInstrument();

    				DeviceChannel channel = channelMap.get(instrument);
        			
    				if(channel == null) {
    					throw(new RuntimeException("Instrument "+instrument+" not mapped to MIDI device/channel combination"));
    				}

    				int[] t = tickList.get(k);
    				int[] p = posList.get(k);

    				for(int j=0;j<t.length;j++) {

    					if(--t[j] <= 0) {
    						Sequence s = track.get(j);

    						if(p[j] > 0) {
    							SequenceEntry prevse = s.get(p[j]-1);
    							if(prevse.isNote()) {
    								sm.setMessage(ShortMessage.NOTE_OFF,channel.channel,(track.getType() == TrackType.MELODY ? transposition : 0)+prevse.getPitch(),0);
    								channel.device.receiver.send(sm,-1);
    							}
    						}
    					}
    				}

    				for(int j=0;j<t.length;j++) {
    					if(t[j] <= 0) {
    						try {
    							Sequence s = track.get(j);
    							SequenceEntry se = s.get(p[j]);

    							if(se.isNote()) {
    								sm.setMessage(ShortMessage.NOTE_ON,channel.channel,(track.getType() == TrackType.MELODY ? transposition : 0)+se.getPitch(),getMidiVelocity(se.getVelocity()));
    								channel.device.receiver.send(sm,-1);
    							}

    							p[j]++;
    							t[j] = se.getTicks();
    						} catch(Exception x) {throw new RuntimeException("Error at k="+k+"  j="+j+"  p[j]="+p[j],x);}
    					}
    				}
    			}

    			for(int s=0;s<clockTimingsPerTick;s++) {    				
    	    		if(useClockSynchronization) {
    	    			sendShortMessageToClockSynchronized(ShortMessage.TIMING_CLOCK);
    	    		}
    	    		
    				// sleep the desired time
    				// this is done by using a simple feedback algorithm that
    				// tries hard to keep the player exactly in sync with the system clock

    				// desired length of the current tick in nanoseconds
    				long length = 1000000000l*60l*(long)groove[tick%groove.length]/1000l/(long)(structure.getTicksPerBeat()*bpm*clockTimingsPerTick);

    				long wantedNanos = lastWantedNanos+length;

    				long wait = Math.max(0,wantedNanos-System.nanoTime());

    				Thread.sleep((int)(wait/1000000l),(int)(wait%1000000l));
    				lastWantedNanos = wantedNanos;
    			}

    			tick++;
    		}
    		
    		// playing finished
    		
    		// send a NOTE_OFF for all current notes
    		
    		i = arrangement.iterator();

    		for(int k=0;i.hasNext();k++) {
    			ArrangementEntry e = i.next();
    			Track track = e.getTrack();
    			int instrument = e.getInstrument();

    			DeviceChannel channel = channelMap.get(instrument);
    			
				if(channel == null) {
					throw(new RuntimeException("Instrument "+instrument+" not mapped to MIDI device/channel combination"));
				}

    			int[] p = posList.get(k);

    			for(int j=0;j<p.length;j++) {
    				Sequence s = track.get(j);					
    				SequenceEntry prevse = s.get(p[j]-1);
    				if(prevse.isNote()) {
    					sm.setMessage(ShortMessage.NOTE_OFF,instrument,(track.getType() == TrackType.MELODY ? transposition : 0)+prevse.getPitch(),0);
    					channel.device.receiver.send(sm,-1);
    				}
    			}
    		}
    	
    		if(useClockSynchronization) {
    		    sendShortMessageToClockSynchronized(ShortMessage.STOP);
    		}
    	} catch(Exception e) {
    		throw(new RuntimeException("Playback error",e));
    	}
    }

    /**
     * Sets the channel programs of all DeviceChannels used. This
     * method does not set the program of a DeviceChannel more than
     * once.
     * 
     * @throws InvalidMidiDataException
     */
    
	private void setChannelPrograms() throws InvalidMidiDataException {
		ShortMessage sm = new ShortMessage();

		// we use a Map to track whether a program has been set already

		Map <DeviceChannel,Boolean> map = new HashMap<DeviceChannel,Boolean>();
		Iterator<DeviceChannel> i = channelMap.values().iterator();

		while(i.hasNext()) {
			DeviceChannel dc = i.next();

			if(dc.program != -1 && !map.containsKey(dc)) {
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
        
        while(iter.hasNext()) {
            Device device = iter.next();
            
            if(device.useClockSynchronization) {
                sm.setMessage(message);
                device.receiver.send(sm,-1);
            }
        }
    }

	/**
	 * Mutes all channels of all devices. This is done by sending an ALL
	 * SOUND OFF message to all channels. In addition to that (because this
	 * is not supported by all devices) a NOTE_OFF is sent for each of the
	 * 128 possible pitches to each channel.
	 * 
	 * @throws InvalidMidiDataException
	 */
	
	private void muteAllChannels() throws InvalidMidiDataException {
		ShortMessage sm = new ShortMessage();
		Iterator<DeviceChannel> iter = channelMap.values().iterator();
		
		while(iter.hasNext()) {
			DeviceChannel dc = iter.next();
			
			// send ALL SOUND OFF message
			sm.setMessage(ShortMessage.CONTROL_CHANGE,dc.channel,120,0);
			dc.device.receiver.send(sm,-1);

			for(int i=0;i<128;i++) {
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
    	if(velocity == 0) return 0;
    	
    	return(1+(velocity-1)*126/(Short.MAX_VALUE-126));  	
    }
    
    public void configure(Node node,XPath xpath) throws XPathException {
		NodeList nodeList = (NodeList)xpath.evaluate("device",node,XPathConstants.NODESET);
		int entries = nodeList.getLength();
		Device[] devices = new Device[entries];
		
		for(int i=0;i<entries;i++) {
			String name = (String)xpath.evaluate("attribute::name",nodeList.item(i),XPathConstants.STRING);
			String midiName = XMLUtils.parseString(nodeList.item(i),xpath);			
            boolean useClockSynchronization = XMLUtils.parseBoolean("attribute::clockSynchronization",nodeList.item(i),xpath);
			devices[i] = new Device(name,midiName,useClockSynchronization);
		}
		
    	setDevices(devices);	
    	setBPM(XMLUtils.parseInteger((Node)xpath.evaluate("bpm",node,XPathConstants.NODE),xpath));
    	setTransposition(XMLUtils.parseInteger((Node)xpath.evaluate("transposition",node,XPathConstants.NODE),xpath));
    	setGroove(XMLUtils.parseString((Node)xpath.evaluate("groove",node,XPathConstants.NODE),xpath));
    	
		nodeList = (NodeList)xpath.evaluate("map",node,XPathConstants.NODESET);
		entries = nodeList.getLength();
		
		Map<Integer,DeviceChannel> channelMap = new HashMap<Integer,DeviceChannel>();
		
		for(int i=0;i<entries;i++) {
			int instrument = Integer.parseInt((String)xpath.evaluate("attribute::instrument",nodeList.item(i),XPathConstants.STRING));
			String device = ((String)xpath.evaluate("attribute::device",nodeList.item(i),XPathConstants.STRING));
			int channel = Integer.parseInt((String)xpath.evaluate("attribute::channel",nodeList.item(i),XPathConstants.STRING));

			if(channelMap.containsKey(instrument)) {
				throw(new RuntimeException("Instrument "+instrument+" must not be re-mapped"));
			}
			
			if(!deviceMap.containsKey(device)) {
				throw(new RuntimeException("Device \""+device+"\" unknown"));
			}

			int program = -1;
			
			try {
				program = Integer.parseInt((String)xpath.evaluate("attribute::program",nodeList.item(i),XPathConstants.STRING));
			} catch(Exception e) {}
			
			DeviceChannel ch = new DeviceChannel(deviceMap.get(device),channel,program);			
			channelMap.put(instrument,ch);
		}
		
		setChannelMap(channelMap);
    }
    
    public class Device
    {
    	private final String name;
    	private final String midiName;
    	private MidiDevice midiDevice;
    	private Receiver receiver;
    	private boolean useClockSynchronization;
    	
    	public Device(String name,String midiName,boolean useClockSynchronization) {
    		if(name == null || name.equals("")) {
    			throw(new IllegalArgumentException("Name must not be null or empty"));
    		}

    		if(midiName == null || midiName.equals("")) {
    			throw(new IllegalArgumentException("MIDI device name must not be null or empty"));
    		}

    		this.name = name;
    		this.midiName = midiName;
    		this.useClockSynchronization = useClockSynchronization;
    	}
    	
    	public void open() {
           	try {
           		midiDevice = MidiPlayer.findMidiDevice(midiName);

        		if(midiDevice == null) {
           			throw(new RuntimeException("Could not find MIDI device \""+midiName+"\". Available devices with MIDI IN:\n"+getMidiDevices()));
        		}

        		midiDevice.open();

        		receiver = midiDevice.getReceiver();

        		if(receiver == null) {
        			throw(new RuntimeException("MIDI device \""+midiName+"\" does not have a Receiver. Available devices with MIDI IN:\n"+getMidiDevices()));
        		}
        	} catch(Exception e) {
        		throw(new RuntimeException("Error opening MIDI device \""+midiName+"\"",e));
        	}
    	}
    	
    	public void close() {
    		if(midiDevice != null) {
    			midiDevice.close();
    			midiDevice = null;
    		}
    	}
    }
    
    public class DeviceChannel
    {
    	private final Device device;
    	private final int channel;
    	private final int program;
     	
    	public DeviceChannel(Device device,int channel,int program) {
    		this.device = device;
    		this.channel = channel;
    		this.program = program;
    	}

    	public boolean equals(Object object) {
    		if(object == null || !(object instanceof DeviceChannel))
    			return false;
    		
    		DeviceChannel other = (DeviceChannel)object;    		
    		return this == other || device.equals(other.device) && channel == other.channel && program == other.program;
    	}

    	public int hashCode() {
    		return device.hashCode() * 4711 + channel*513 + program;
    	}    	
    }
}
