package com.soundhelix.sequenceengine;

import java.util.Hashtable;

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
 * Implements a sequence engine that repeats user-specified patterns. For each
 * power of 2, a different pattern can be specified. The length of the current chord
 * determines which of these patterns will be used.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class ArpeggioSequenceEngine extends SequenceEngine {
	
	private static final int[] majorTable = new int[] {0,4,7};
	private static final int[] minorTable = new int[] {0,3,7};

	private static final boolean obeyChordSubtype = true;
	
	private static final Hashtable<Integer,int[]> arpeggioTable = new Hashtable<Integer,int[]>();
	
	static {
		arpeggioTable.put(1,new int[] {0});
		arpeggioTable.put(2,new int[] {0,1});
		arpeggioTable.put(4,new int[] {0,1,2,1});
		arpeggioTable.put(8,new int[] {0,1,2,1,2,3,2,1});
		arpeggioTable.put(16,new int[] {0,1,2,1,2,3,2,3,4,3,4,5,4,3,2,1});
		arpeggioTable.put(32,new int[] {0,1,2,1,2,3,2,3,4,3,4,5,4,5,6,5,6,7,6,7,8,7,8,9,8,7,6,5,4,3,2,1});
		arpeggioTable.put(64,new int[] {0,1,2,1,2,3,2,3,4,3,4,5,4,5,6,5,6,7,6,7,8,7,8,9,8,9,10,9,10,11,10,11,12,11,12,13,12,13,14,13,14,15,14,15,16,15,16,17,16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1});

		/*
		arpeggioTable.put(1,new int[] {0});
		arpeggioTable.put(2,new int[] {0,2});
		arpeggioTable.put(4,new int[] {0,2,1,3});
		arpeggioTable.put(8,new int[] {0,2,1,3,0,2,1,3});
		arpeggioTable.put(16,new int[] {0,2,1,3,0,2,1,3,0,2,1,3,0,2,1,3});
		arpeggioTable.put(32,new int[] {0,2,1,3,0,2,1,3,0,2,1,3,0,2,1,3,0,2,1,3,0,2,1,3,0,2,1,3,0,2,1,3});
		arpeggioTable.put(64,new int[] {0,2,1,3,0,2,1,3,0,2,1,3,0,2,1,3,0,2,1,3,0,2,1,3,0,2,1,3,0,2,1,3,0,2,1,3,0,2,1,3,0,2,1,3,0,2,1,3,0,2,1,3,0,2,1,3,0,2,1,3,0,2,1,3});
        */
}

	public ArpeggioSequenceEngine() {
		super();
	}
	
	public Track render(ActivityVector... activityVectors) {
		ActivityVector activityVector = activityVectors[0];
		
        Sequence seq = new Sequence();
        HarmonyEngine ce = structure.getHarmonyEngine();
        
        int tick = 0;
        
        while(tick < structure.getTicks()) {
        	Chord chord = ce.getChord(tick);
        	int len = ce.getChordTicks(tick);
        	int seqlen = Integer.highestOneBit(Math.min(32,len));
        	
        	for(int i=0;i<len;i++) {
        		if(activityVector.isActive(tick)) {
        			// add next note for major/minor chord
        			
        			int[] table = arpeggioTable.get(seqlen);
        			int value = table[i%seqlen];
        			
        			if(value == -1) {
            			// add pause
            			seq.addPause(1);
        				continue;
        			}
        			
        			if(obeyChordSubtype) {
        				if(chord.getSubtype() == ChordSubtype.BASE_4) {
        					value++;
        				} else if(chord.getSubtype() == ChordSubtype.BASE_6) {
        					value--;
        				}
        			}
        			
        			int octave = (value >= 0 ? value/3 : (value-2)/3);
        			int offset = (value >= 0 ? value%3 : (value%3)+3);
        			
        	 	    if(chord.isMajor()) {
        			    seq.addNote(octave*12+majorTable[offset]+chord.getPitch(),1);
        		    } else {
           			    seq.addNote(octave*12+minorTable[offset]+chord.getPitch(),1);
        		    }
        		} else {
        			// add pause
        			seq.addPause(1);
        		}
         	}
        	
        	tick += len;
        }
        
		Track track = new Track(TrackType.MELODY);
        track.add(seq);
        return track;
	}
	
    public void configure(Node node,XPath xpath) throws XPathException {
    }
}
