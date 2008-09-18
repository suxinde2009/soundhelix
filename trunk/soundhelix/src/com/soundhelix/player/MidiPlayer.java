package com.soundhelix.player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

import com.soundhelix.misc.Arrangement;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Structure;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Arrangement.ArrangementEntry;
import com.soundhelix.misc.Sequence.SequenceEntry;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a MIDI player. Track's channels are mapped to MIDI channels
 * in a 1:1 fashion.
 * 
 * @todo Add MIDI channel mapping
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class MidiPlayer extends Player {
	private String deviceName;
	private MidiDevice device;
	private Receiver receiver;
	private int bpm;
	private int transposition;
	private int groove[];
	
    public MidiPlayer() {
    	super();
	}
	
    public void open() {
       	try {
       		device = MidiPlayer.findMidiDevice(deviceName);

    		if(device == null) {
    			throw(new RuntimeException("Could not find MIDI device"));
    		}

    		device.open();

    		receiver = device.getReceiver();

    		if(receiver == null) {
    			throw(new RuntimeException("MIDI device does not have a Receiver"));
    		}
    	} catch(Exception e) {
    		throw(new RuntimeException("Error opening MIDI device \""+deviceName+"\"",e));
    	}
    }

    public void close() {
    	if(device != null) {
    		device.close();
    		device = null;
    		receiver = null;
    	}
    }
    
    private void setDevice(String deviceName) {
    	this.deviceName = deviceName;
    }
    
    private void setBPM(int bpm) {
    	if(bpm <= 0) {
    		throw(new IllegalArgumentException("BPM must be > 0"));
    	}
    	
    	this.bpm = bpm;
    }

    private void setTransposition(int transposition) {
    	if(transposition <= 0) {
    		throw(new IllegalArgumentException("transposition must be >= 0"));
    	}
    	
    	this.transposition = transposition;
    }

    /**
     * Sets the groove for playback. A groove list a comma-separated
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
    	if(receiver == null) {
    		throw(new RuntimeException("Must call open() first"));
    	}
    	
    	try {
    		Structure song = arrangement.getStructure();

    		List<int[]> tickList = new ArrayList<int[]>();
    		List<int[]> posList = new ArrayList<int[]>();

    		System.out.println("Song length: "+(song.getTicks()*60/(song.getTicksPerBeat()*bpm))+" seconds");

    		Iterator<ArrangementEntry> i = arrangement.iterator();

    		ShortMessage sm = new ShortMessage();

    		while(i.hasNext()) {
    			ArrangementEntry ae = i.next();
    			int size = ae.getTrack().size();
    			tickList.add(new int[size]);
    			posList.add(new int[size]);

    			//sm.setMessage(ShortMessage.STOP,ae.getChannel(),0,0);

    		}

    		int tick = 0;

    		long nanos = System.nanoTime();
    		long lastWantedNanos = nanos;

    		while(tick < song.getTicks()) {
    			i = arrangement.iterator();

    			if((tick % 16) == 0) {
    				System.out.printf("Tick: %4d   Seconds: %3d\n",tick,tick*60/(song.getTicksPerBeat()*bpm));
    			}
    			for(int k=0;i.hasNext();k++) {
    				ArrangementEntry e = i.next();
    				Track track = e.getTrack();
    				int channel = e.getChannel();

    				int[] t = tickList.get(k);
    				int[] p = posList.get(k);

    				for(int j=0;j<t.length;j++) {

    					if(--t[j] <= 0) {
    						Sequence s = track.get(j);

    						if(p[j] > 0) {
    							SequenceEntry prevse = s.get(p[j]-1);
    							if(prevse.isNote()) {
    								sm.setMessage(ShortMessage.NOTE_OFF,channel,(track.getType() == TrackType.MELODY ? transposition : 0)+prevse.getPitch(),0);
    								receiver.send(sm,-1);
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
    								sm.setMessage(ShortMessage.NOTE_ON,channel,(track.getType() == TrackType.MELODY ? transposition : 0)+se.getPitch(),getMidiVelocity(se.getVelocity()));
    								receiver.send(sm,-1);
    							}

    							p[j]++;
    							t[j] = se.getTicks();
    						} catch(Exception x) {throw new RuntimeException("Error at k="+k+"  j="+j+"  p[j]="+p[j],x);}
    					}
    				}
    			}

    			// sleep the desired time
    			// this is done by using a simple feedback algorithm that
    			// tries hard to keep the player exactly in sync with the system clock

    			// desired length of the current tick in nanoseconds
    			long length = 1000000000l*60l*(long)groove[tick%groove.length]/1000l/(long)(song.getTicksPerBeat()*bpm);

    			long wantedNanos = lastWantedNanos+length;

    			long wait = Math.max(0,wantedNanos-System.nanoTime());

    			Thread.sleep((int)(wait/1000000l),(int)(wait%1000000l));
    			tick++;

    			lastWantedNanos = wantedNanos;

    			/*sm.setMessage(ShortMessage.TIMING_CLOCK);
    		receiver.send(sm,-1);
    		sm.setMessage(ShortMessage.TIMING_CLOCK);
    		receiver.send(sm,-1);
    		sm.setMessage(ShortMessage.TIMING_CLOCK);
    		receiver.send(sm,-1);

    	    MetaMessage mm = new MetaMessage();
    		long us = 60000000/(bpm+10);
    		mm.setMessage(0x51,new byte[] {0x03,(byte)((us >>> 0)&0xff),(byte)((us >>> 8)&0xff),(byte)((us >>> 16)&0xff)},4);
    	    receiver.send(mm,-1);*/


    		}

    		// playing finished
    		
    		// send a NOTE_OFF for all current notes
    		
    		i = arrangement.iterator();

    		for(int k=0;i.hasNext();k++) {
    			ArrangementEntry e = i.next();
    			Track track = e.getTrack();
    			int channel = e.getChannel();

    			int[] p = posList.get(k);

    			for(int j=0;j<p.length;j++) {
    				Sequence s = track.get(j);					
    				SequenceEntry prevse = s.get(p[j]-1);
    				if(prevse.isNote()) {
    					sm.setMessage(ShortMessage.NOTE_OFF,channel,(track.getType() == TrackType.MELODY ? transposition : 0)+prevse.getPitch(),0);
    					receiver.send(sm,-1);
    				}
    			}
    		}
    	} catch(Exception e) {
    		throw(new RuntimeException("Playback error",e));
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
    	setDevice(XMLUtils.parseString((Node)xpath.evaluate("device",node,XPathConstants.NODE),xpath));	
    	setBPM(XMLUtils.parseInteger((Node)xpath.evaluate("bpm",node,XPathConstants.NODE),xpath));
    	setTransposition(XMLUtils.parseInteger((Node)xpath.evaluate("transposition",node,XPathConstants.NODE),xpath));
    	setGroove(XMLUtils.parseString((Node)xpath.evaluate("groove",node,XPathConstants.NODE),xpath));
    }
}
