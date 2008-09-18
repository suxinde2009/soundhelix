package com.soundhelix.sequenceengine;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
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
 * Implements a sequence engine that plays chords using user-specified patterns.
 */

public class ChordSequenceEngine extends SequenceEngine {
	
	private static final int[] majorTable = new int[] {0,4,7,12,16,19};
	private static final int[] minorTable = new int[] {0,3,7,12,15,19};

	private static boolean obeyChordSubtype = true;
	private int[] pattern;
	private int patternLength;
	
	public ChordSequenceEngine() {
		super();
	}

	public void setPattern(String patternString) {
		this.pattern = parsePatternString(patternString);
		this.patternLength = pattern.length;
	}

	public Track render(ActivityVector... activityVectors) {
		ActivityVector activityVector = activityVectors[0];
		
        Sequence seq[] = new Sequence[3];
        HarmonyEngine ce = structure.getHarmonyEngine();
        
        int tick = 0;
        
        for(int i=0;i<3;i++) {
        	seq[i] = new Sequence();
        }
        
        while(tick < structure.getTicks()) {
        	Chord chord = ce.getChord(tick);
        	int len = ce.getChordTicks(tick);
        
        	for(int i=0;i<len;i++) {
        		if(activityVector.isActive(tick)) {
        			// add next note for major/minor chord
        			
        			int value = pattern[(tick+i)%patternLength];
        			
        			if(value == Integer.MIN_VALUE) {
            			// add pause
            			seq[0].addPause(1);
            			seq[1].addPause(1);
            			seq[2].addPause(1);
        				continue;
        			}
        			
        			if(obeyChordSubtype) {
        				if(chord.getSubtype() == ChordSubtype.BASE_4) {
        					value++;
        				} else if(chord.getSubtype() == ChordSubtype.BASE_6) {
        					value--;
        				}
        			}
        			
        			// value can be -1 or >= 0 here
        			
        			// split value into octave and offset
        			// we add 3 to avoid modulo and division issues with
        			// negative values
        			
        			int octave = (value >= 0 ? value/3 : (value-2)/3);
        			int offset = (value >= 0 ? value%3 : (value%3)+3);
        			
        	 	    if(chord.isMajor()) {
        			    seq[0].addNote(octave*12+majorTable[offset]+chord.getPitch(),1);
        			    seq[1].addNote(octave*12+majorTable[offset+1]+chord.getPitch(),1);
        			    seq[2].addNote(octave*12+majorTable[offset+2]+chord.getPitch(),1);
        		    } else {
           			    seq[0].addNote(octave*12+minorTable[offset]+chord.getPitch(),1);
           			    seq[1].addNote(octave*12+minorTable[offset+1]+chord.getPitch(),1);
           			    seq[2].addNote(octave*12+minorTable[offset+2]+chord.getPitch(),1);
        		    }
        		} else {
        			// add pause
        			seq[0].addPause(1);
        			seq[1].addPause(1);
        			seq[2].addPause(1);
        		}
         	}
        	
        	tick += len;
        }
        
		Track track = new Track(TrackType.MELODY);
        track.add(seq[0]);
        track.add(seq[1]);
        track.add(seq[2]);
        return track;
	}
	
	private static int[] parsePatternString(String s) {
		String[] p = s.split(",");
		int len = p.length;
		
		int[] array = new int[len];
		
		for(int i=0;i<len;i++) {
			if(p[i].equals("-")) {
				array[i] = Integer.MIN_VALUE;
			} else {
				array[i] = Integer.parseInt(p[i]);
			}
		}
		
		return array;
	}
	
    public void configure(Node node,XPath xpath) throws XPathException {
    	setPattern(XMLUtils.parseString((Node)xpath.evaluate("pattern",node,XPathConstants.NODE),xpath));	
    }
}
