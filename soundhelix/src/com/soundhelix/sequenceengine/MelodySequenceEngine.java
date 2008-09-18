package com.soundhelix.sequenceengine;

import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

import com.soundhelix.harmonyengine.HarmonyEngine;
import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Track.TrackType;

/**
 * Implements a sequence engine that repeats user-specified patterns.
 */

public class MelodySequenceEngine extends SequenceEngine {
	
	private static final int PAUSE = Integer.MIN_VALUE;
	private static final int TRANSITION = Integer.MIN_VALUE+1;
	private static final int FREE = Integer.MIN_VALUE+2;
	// all values at or below this values are special
	private static final int SPECIAL = FREE;	
	
	private static final int[] majorTable = new int[] {0,4,7};
	private static final int[] minorTable = new int[] {0,3,7};
	private static final boolean[] scaleTable = new boolean[] {true,false,true,false,true,true,false,true,false,true,false,true};

	private static boolean obeyChordSubtype = false;
	private static String defaultPatternString = "0,-,-,0,-,-,0,-,1,-,-,0,-,-,0,-,0,-,-,0,-,-,0,-,0,-,-1,-,0,-,-,-,0,-,-,0,-,-,0,-,1,-,-,0,-,-,0,-,0,-,-,0,-,-,0,-,0,-,-1,-,0,-,4,1";
	private String patternString;
	private int[] pattern;
	private int patternLength;
	
	private int[] pitchPattern;
	
	private static Random random = new Random();
	
	private MelodySequenceEngine() {
		super();
		this.patternString = defaultPatternString;
		this.pattern = parsePatternString(patternString);
		this.patternLength = pattern.length;
	}

	public MelodySequenceEngine(String patternString) {
		super();
		this.patternString = patternString;
		this.pattern = parsePatternString(patternString);
		this.patternLength = pattern.length;
	}

	public Track render(ActivityVector... activityVectors) {
		ActivityVector activityVector = activityVectors[0];

		Sequence seq = new Sequence();
        HarmonyEngine ce = structure.getHarmonyEngine();
        
        int tick = 0;
        
        int ticks = structure.getTicks();
        
        int pitch = Integer.MIN_VALUE;
        
        pitchPattern = new int[ce.getChordSectionTicks(0)];
        
		while(tick < ticks) {
        	Chord chord = ce.getChord(tick);
        	int len = ce.getChordTicks(tick);
        
        	for(int i=0;i<len;i++) {
        		if(activityVector.isActive(tick)) {
        			int value = pattern[(tick+i)%patternLength];
        			
        			if(value == PAUSE) {
            			// add pause
            			seq.addPause(1);
        				continue;
        			} else if(value == FREE) {
        				if(tick >= pitchPattern.length) {
            				pitch = pitchPattern[tick % pitchPattern.length];
        				} else {
        					pitch = getRandomPitch(pitch == Integer.MIN_VALUE ? chord.getPitch() : pitch);
        					System.out.println("Using pitch "+pitch);
        					pitchPattern[tick % pitchPattern.length] = pitch;
        				}
        				seq.addNote(pitch,1);
        			} else {
        				pitch = chord.getPitch();
        				seq.addNote(pitch,1);
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
	
	private static int[] parsePatternString(String s) {
		String[] p = s.split(",");
		int len = p.length;
		
		int[] array = new int[len];
		
		for(int i=0;i<len;i++) {
			if(p[i].equals("-")) {
				array[i] = PAUSE;
			} else if(p[i].equals("+")) {
				array[i] = FREE;
			} else {
				array[i] = Integer.parseInt(p[i]);
			}
		}
		
		return array;
	}
	
	/**
	 * Returns a transition pitch between the chord and the next chord,
	 * which is based on the base pitches of the two chords.
	 * If the next chord is null, the pitch of the first chord is used.
	 * If the pitch difference of the two chords is 2, the halftone in
	 * between is returned. If the pitch difference of the two chords
	 * is one or zero, the first pitch is returned. Otherwise, a pitch
	 * between the two pitches which is on the C/Am scale is returned.
	 * 
	 * @param chord the current chord
	 * @param nextChord the next chord (or null)
	 * 
	 * @return a transition pitch
	 */
	
	
    private static int getTransitionPitch(Chord chord,Chord nextChord) {
    	if(nextChord == null) {
    		// next chord is undefined, just return the current pitch
    		return chord.getPitch();
    	}
    	
    	int pitch1 = chord.getPitch();
    	int pitch2 = nextChord.getPitch();
    	
    	int diff = pitch2-pitch1;
    	int absdiff = Math.abs(diff);
    	
    	if(diff == 0) {
    		// chords are the same
    		return pitch1;
    	} else if(absdiff == 2) {
    		// pitch difference is one tone,
    		// use the halftone in between
    		return((pitch1+pitch2)/2);
       	} else if(absdiff == 1) {
    		// pitch difference is one halftone
    		// use the current pitch
    		return(pitch1);
    	} else if(diff > 0) {
    		pitch1 += Math.min(0,absdiff/2-1);
    		do {
    			pitch1++;
    		} while(!isOnScale(pitch1));
    	   	return pitch1;
    	} else {
    		pitch1 -= Math.min(0,absdiff/2-1);
    		do {
    			pitch1--;
    		} while(!isOnScale(pitch1));
    		return pitch1;
    	}
    }
    
    // checks if the given pitch is on the C/Am scale
    
    private static boolean isOnScale(int pitch) {
    	pitch = ((pitch%12)+12)%12;
    	return scaleTable[pitch];
    }
    
    private static int getRandomPitch(int pitch) {
    	int r = random.nextInt(3);
    	
    	if(r == 0) {
    		pitch += random.nextInt(2);
    		do {
    		  pitch++;
    		} while(!isOnScale(pitch));    		
    	} else if(r==1) {
    		pitch -= random.nextInt(2);
    		do {
      		  pitch--;
      		} while(!isOnScale(pitch));    		
    	}
    	
    	return pitch;
    }
    
    public void configure(Node node,XPath xpath) throws XPathException {
    }
}
