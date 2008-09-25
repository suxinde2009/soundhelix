package com.soundhelix.sequenceengine;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

import com.soundhelix.harmonyengine.HarmonyEngine;
import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Chord.ChordSubtype;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a pad sequence engine using 1 or more voices, playing at
 * full velocity. This engine simply plays each chord for its whole
 * length. The chord tones are selected by specifying a list of
 * chord offsets. For example, offsets 0, 1 and 2 form a normal
 * 3-tone chord. The chord can be "stretched" by using a wider offset range, e.g.,
 * 0,1,2,3,4 which would add base and middle tone with increased octave to the
 * normal chord tones.
 * <br><br>
 * <b>XML-Configuration</b>
 * <table border=1>
 * <tr><th>Tag</th> <th>#</th> <th>Example</th> <th>Description</th> <th>Required</th>
 * <tr><td><code>offsets</code></td> <td></td> <td><code>0,1,2</code></td> <td>The list of offsets to use for playing chords.</td> <td>yes</td>
 * </table>

 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class PadSequenceEngine extends SequenceEngine {
	
	private static final int[] majorTable = new int[] {0,4,7};
	private static final int[] minorTable = new int[] {0,3,7};
	
	private static boolean obeyChordSubtypes = true;
	
	private static int postPauseTicks = 1;
	
	private int voiceCount = -1;
	private int[] offsets;
	
	public PadSequenceEngine() {
		super();
	}
	
	public void setOffsets(int[] offsets) {
		if(offsets.length == 0) {
			throw(new RuntimeException("Array of offsets must not be empty"));
		}
		
		this.offsets = offsets;
    	this.voiceCount = offsets.length;
	}
	
	public Track render(ActivityVector[] activityVectors) {
		ActivityVector activityVector = activityVectors[0];

		Track track = new Track(TrackType.MELODY);
		
        for(int i=0;i<voiceCount;i++) {
        	track.add(new Sequence());
        }

        HarmonyEngine ce = structure.getHarmonyEngine();
        
        //Chord firstChord = ce.getChord(0);
        
        int tick = 0;
        
        while(tick < structure.getTicks()) {
        	//Chord chord = firstChord.findClosestChord(ce.getChord(tick));
           	Chord chord = ce.getChord(tick);
           	int len = ce.getChordTicks(tick);
           	
        	if(activityVector.isActive(tick)) {
        	
        	int activityLen = activityVector.getIntervalLength(tick);
        	
        	int shift = 0;

        	if(obeyChordSubtypes) {
        		if(chord.getSubtype() == ChordSubtype.BASE_4) {
        			shift = 1;
        		} else if(chord.getSubtype() == ChordSubtype.BASE_6) {
        			shift = -1;
        		}
        	}
	
        	for(int i=0;i<voiceCount;i++) {
        		Sequence seq = track.get(i);

        		int pos = offsets[i]+shift;
        		
        		int octave = (pos >= 0 ? pos/3 : (pos-2)/3);
        		int offset = ((pos%3)+3)%3;
        		
        		if(chord.isMajor()) {
        			seq.addNote(octave*12+majorTable[offset]+chord.getPitch(),Math.min(activityLen,len)-postPauseTicks);
        			seq.addPause(len-Math.min(activityLen,len));
        		} else {
        			seq.addNote(octave*12+minorTable[offset]+chord.getPitch(),Math.min(activityLen,len)-postPauseTicks);        			
        			seq.addPause(len-Math.min(activityLen,len));
        		}
        		
        		if(postPauseTicks > 0) {
        			seq.addPause(postPauseTicks);
        		}
        		     		
        		pos++;
        	}
        	} else {
        		for(int i=0;i<voiceCount;i++) {
        		  track.get(i).addPause(len);
        		}
        	}
        	tick += len;
        }
        
        return track;
	}
	
    public void configure(Node node,XPath xpath) throws XPathException {
    	String offsetString = XMLUtils.parseString("offsets",node,xpath);
    	
    	if(offsetString == null || offsetString.equals("")) {
    		offsetString = "0,1,2";
    	}

    	String[] offsetList = offsetString.split(",");
    	
    	int[] offsets = new int[offsetList.length];
    	
    	for(int i=0;i<offsetList.length;i++) {
    		offsets[i] = Integer.parseInt(offsetList[i]);
    	}
    	
    	setOffsets(offsets);
    }
}
