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

/**
 * Implements a pad sequence engine using 3 or more voices. This engine
 * simply plays chords.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class PadSequenceEngine extends SequenceEngine {
	
	private static final int[] majorTable = new int[] {0,4,7};
	private static final int[] minorTable = new int[] {0,3,7};
	
	private static boolean obeyChordSubtypes = true;
	
	private static int voiceCount = 3;
	private static int postPauseTicks = 1;
	
	public PadSequenceEngine() {
		super();
	}
	
	public Track render(ActivityVector... activityVectors) {
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
        	
        	int pos = 0;

        	if(obeyChordSubtypes) {
        		if(chord.getSubtype() == ChordSubtype.BASE_4) {
        			pos = 1;
        		} else if(chord.getSubtype() == ChordSubtype.BASE_6) {
        			pos = -1;
        		}
        	}
        	      	
        	for(int i=0;i<voiceCount;i++) {
        		Sequence seq = track.get(i);

        		int octave = (pos >= 0 ? pos/3 : (pos-2)/3);
        		int offset = (pos >= 0 ? pos%3 : (pos%3) + 3);
        		
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
    }
}
