package com.soundhelix.sequenceengine;

import java.util.Hashtable;
import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.harmonyengine.HarmonyEngine;
import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.util.NoteUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a sequence engine that uses a randomly generated melody, played
 * with a given rhythmic pattern. For each distinct chord section, a melody
 * is generated and used for each occurrence of the chord section.
 * <br><br>
 * <b>XML-Configuration</b>
 * <table border=1>
 * <tr><th>Tag</th> <th>#</th> <th>Example</th> <th>Description</th> <th>Required</th>
 * <tr><td><code>pattern</code></td> <td>+</td> <td><code>0,-,-,-,1,-,2,-</code></td> <td>Sets the patterns to use. One of the patterns is selected at random.</td> <td>no</td>
 * </table>
 * @author Thomas Schürger (thomas@schuerger.com)
 */

// TODO: work on this (not really usable yet)

public class MelodySequenceEngine extends SequenceEngine {
	
	private static final int PAUSE = Integer.MIN_VALUE;
	private static final int TRANSITION = Integer.MIN_VALUE+1;
	private static final int FREE = Integer.MIN_VALUE+2;
	// all values at or below this values are special
	private static final int SPECIAL = FREE;	
	
	private static boolean obeyChordSubtype = false;
	private static String defaultPatternString = "0,-,-,+,-,-,+,-,0,-,-,+,-,-,+,-,0,-,-,+,-,-,+,-,0,-,+,-,+,-,-,-,0,-,-,+,-,-,+,-,0,-,-,+,-,-,+,-,0,-,-,+,-,-,+,-,0,-,+,-,+,-,+,+";
	//private static String defaultPatternString = "0,-,-,-";
	private String patternString;
	private int[] pattern;
	private int patternLength;
	
	private static Random random = new Random();
	
	public MelodySequenceEngine() {
		this(defaultPatternString);
	}

	public MelodySequenceEngine(String patternString) {
		super();
		setPattern(patternString);
	}

	public Track render(ActivityVector[] activityVectors) {
		ActivityVector activityVector = activityVectors[0];

		Sequence seq = new Sequence();
        HarmonyEngine ce = structure.getHarmonyEngine();
        
        int tick = 0;
        
        int ticks = structure.getTicks();
        
        Hashtable<String,int[]> melodyHashtable = createMelodies();
        
		while(tick < ticks) {
        	int len = ce.getChordSectionTicks(tick);
        	int[] pitchPattern = melodyHashtable.get(ce.getChordSectionString(tick));
        	       	
        	for(int i=0;i<len;i++) {
        		if(activityVector.isActive(tick)) {
        			int value = pitchPattern[i];
        			
        			if(value == PAUSE) {
            			// add pause
            			seq.addPause(1);
        			} else {
        				seq.addNote(value,1);
        			}
        		} else {
        			// add pause
        			seq.addPause(1);
        		}
        		
        		tick++;
         	}
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
     * Returns a random pitch which is near the given pitch and on the
     * C/Am scale.
     * 
     * @param the starting pitch
     * 
     * @return the random pitch
     */
    
    private static int getRandomPitch(int pitch,int maxDistanceDown,int maxDistanceUp) {
    	int r = random.nextInt(3);
    	
    	if(r == 0 || pitch < -10) {
    		pitch += random.nextInt(maxDistanceUp);
    		do {
    		  pitch++;
    		} while(!NoteUtils.isOnScale(pitch));    		
    	} else if(r==1 || pitch > 10) {
    		pitch -= random.nextInt(maxDistanceDown);
    		do {
      		  pitch--;
      		} while(!NoteUtils.isOnScale(pitch));    		
    	}
    	
    	return pitch;
    }

    /**
     * Returns a random pitch which is near the given pitch and
     * is one of the given chords notes.
     * 
     * @param the starting pitch
     * 
     * @return the random pitch
     */
    
    private static int getRandomPitch(Chord chord,int pitch,int maxDistanceDown,int maxDistanceUp) {
    	int r = random.nextInt(3);
    	
    	if(r == 0 || pitch < -10) {
    		pitch += random.nextInt(maxDistanceUp);
    		do {
    		  pitch++;
    		} while(!chord.containsPitch(pitch));    		
    	} else if(r==1 || pitch > 10) {
    		pitch -= random.nextInt(maxDistanceDown);
    		do {
      		  pitch--;
      		} while(!chord.containsPitch(pitch));    		
    	}
    	
    	return pitch;
    }

    /**
     * Creates a melody for each distinct chord section and
     * returns a hashtable mapping chord section strings to
     * melody arrays.
     * 
     * @return a hashtable mapping chord section strings to melody arrays
     */
    
    private Hashtable<String,int[]> createMelodies() {
    	HarmonyEngine he = structure.getHarmonyEngine();
    	
    	Hashtable<String,int[]> ht = new Hashtable<String,int[]>();
    	
    	int tick = 0;
    	
    	while(tick < structure.getTicks()) {
    		String section = he.getChordSectionString(tick);
            int len = he.getChordSectionTicks(tick);
    		
    		if(!ht.containsKey(section)) {
    			// no melody created yet; create one
    			    			
    			int[] pitchList = new int[len];
    			int pitch = Integer.MIN_VALUE;
    			
    			for(int i=0;i<len;i++) {
    				int value = pattern[i%patternLength];
        			Chord chord = he.getChord(tick+i);
        			
        			if(value == PAUSE) {
            			pitchList[i] = PAUSE;
        			} else if(value == FREE) {
        				pitch = getRandomPitch(pitch == Integer.MIN_VALUE ? chord.getPitch() : pitch,2,2);
        				pitchList[i] = pitch;
        			} else {
        				pitch = getRandomPitch(chord,pitch == Integer.MIN_VALUE ? chord.getPitch() : pitch,2,2);
    					pitchList[i] = pitch;
        			}
    			}
    			
    			ht.put(section,pitchList);
    		} else {
    			// melody already created, skip chord section
    		}
    		
    		tick += len;
    	}
    	
    	return ht;
    }
    
    public void configure(Node node,XPath xpath) throws XPathException {
		NodeList nodeList = (NodeList)xpath.evaluate("pattern",node,XPathConstants.NODESET);

		if(nodeList.getLength() == 0) {
			return; // Use default pattern
		}
		
		setPattern(XMLUtils.parseString(nodeList.item(new Random().nextInt(nodeList.getLength())),xpath));
    }
    
	public void setPattern(String patternString) {
		this.patternString = patternString;
		this.pattern = parsePatternString(patternString);
		this.patternLength = pattern.length;
	}
}
