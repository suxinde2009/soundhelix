package com.soundhelix.sequenceengine;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.harmonyengine.HarmonyEngine;
import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Pattern.PatternEntry;
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
	private static final char FREE = '+';
	private static final char REPEAT = '*';
	
	private static String defaultPatternString = "0,-,-,+,-,-,+,-,0,-,-,+,-,-,+,-,0,-,-,+,-,-,+,-,0,-,+,-,+,-,-,-,0,-,-,+,-,-,+,-,0,-,-,+,-,-,+,-,0,-,-,+,-,-,+,-,0,-,+,-,+,-,+,+";
	private Pattern pattern;
	private int patternLength;
	
	private Random random;
	
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
        
        Hashtable<String,Pattern> melodyHashtable = createMelodies();
        
		while(tick < ticks) {
        	int len = ce.getChordSectionTicks(tick);
        	Pattern p = melodyHashtable.get(ce.getChordSectionString(tick));
        	int pos = 0;
        	
        	for(int i=0;i<len;) {
    			PatternEntry entry = p.get(pos);
    			int l = entry.getTicks();

    			if(activityVector.isActive(tick)) {	
        			if(entry.isPause()) {
            			// add pause
            			seq.addPause(l);
        			} else {
        				seq.addNote(entry.getPitch(),l);
        			}
        		} else {
        			// add pause
        			seq.addPause(l);
        		}
        		
    			pos++;
    			i += l;
         	}
        	
        	tick += len;
        }
        
		Track track = new Track(TrackType.MELODY);
        track.add(seq);
        return track;
	}
    
    /**
     * Returns a random pitch which is near the given pitch and on the
     * C/Am scale.
     * 
     * @param the starting pitch
     * 
     * @return the random pitch
     */
    
    private int getRandomPitch(int pitch,int maxDistanceDown,int maxDistanceUp) {
    	int p = pitch;
    	boolean again;
    	
    	do {
    		again = false;
    		int r = random.nextInt(3);

    		if(r == 0 || p < -12) {
    			// move up
    			p += random.nextInt(maxDistanceUp);
    			do {
    				p++;
    			} while(!NoteUtils.isOnScale(p));    		
    		} else if(r==1 || p > 12) {
    			// move down
    			p -= random.nextInt(maxDistanceDown);
    			do {
    				p--;
    			} while(!NoteUtils.isOnScale(p));    		
    		} else {
    			// don't move, but check
    			if(!NoteUtils.isOnScale(p)) {
    				// pitch has to be changed, because it is invalid
    				// we must go up or down, so we'll retry    			
    				again = true;
    				continue;
    			}
    		}
    	} while(again || p < -12 || p > 12);

    	return p;
    }

    /**
     * Returns a random pitch which is near the given pitch and
     * is one of the given chords notes.
     * 
     * @param the starting pitch
     * 
     * @return the random pitch
     */
    
    private int getRandomPitch(Chord chord,int pitch,int maxDistanceDown,int maxDistanceUp) {
    	int p;
    	boolean again;
    	
    	do {
    		again = false;
    		p = pitch;
    		int r = random.nextInt(3);

    		if(r == 0 || p < -12) {
    			// move up
    			p += random.nextInt(maxDistanceUp);
    			do {
    				p++;
    			} while(!chord.containsPitch(p));    		
    		} else if(r==1 || p > 12) {
    			// move down
    			p -= random.nextInt(maxDistanceDown);
    			do {
    				p--;
    			} while(!chord.containsPitch(p));    		
    		} else {
    			// don't move, but check
    			if(!chord.containsPitch(p)) {
    				// pitch has to be changed, because it is invalid
    				// we must go up or down, so we'll retry    			
    				again = true;
    			}
    		}
    	} while(again || p < -12 || p > 12);

    	return p;
    }

    /**
     * Creates a melody for each distinct chord section and
     * returns a hashtable mapping chord section strings to
     * melody patterns.
     * 
     * @return a hashtable mapping chord section strings to melody arrays
     */
    
    private Hashtable<String,Pattern> createMelodies() {
    	HarmonyEngine he = structure.getHarmonyEngine();
    	
    	Hashtable<String,Pattern> ht = new Hashtable<String,Pattern>();
    	
    	int tick = 0;
    	
    	while(tick < structure.getTicks()) {
    		String section = he.getChordSectionString(tick);
            int len = he.getChordSectionTicks(tick);
    		
    		if(!ht.containsKey(section)) {
    			// no melody created yet; create one
    			List<PatternEntry> list = new ArrayList<PatternEntry>();    			
    			
    			int pitch = Integer.MIN_VALUE;
    			int pos = 0;
    			
    			for(int i=0;i<len;) {
    				PatternEntry entry = pattern.get(pos%patternLength);
        			Chord chord = he.getChord(tick+i);
        			int t = entry.getTicks();
        			
        			if(entry.isPause()) {
        				list.add(new PatternEntry(t));
        			} else if(entry.isWildcard() && entry.getWildcardCharacter() == FREE) {
        				pitch = getRandomPitch(pitch == Integer.MIN_VALUE ? 0 : pitch,2,2);
        				list.add(new PatternEntry(pitch,entry.getVelocity(),t));
        			} else if(entry.isWildcard() && entry.getWildcardCharacter() == REPEAT && pitch != Integer.MIN_VALUE && chord.containsPitch(pitch)) {
        				// reuse the previous pitch
        				list.add(new PatternEntry(pitch,entry.getVelocity(),t));
        			} else {
        				pitch = getRandomPitch(chord,pitch == Integer.MIN_VALUE ? 0 : pitch,3,3);
        				list.add(new PatternEntry(pitch,entry.getVelocity(),t));
        			}
        			
        			pos++;
        			i += t;
    			}
   
    			ht.put(section,new Pattern(list.toArray(new PatternEntry[list.size()])));
    		} else {
    			// melody already created, skip chord section
    		}
    		
    		tick += len;
    	}
    	
    	return ht;
    }
    
    public void configure(Node node,XPath xpath) throws XPathException {
    	random = new Random(randomSeed);
    	
		NodeList nodeList = (NodeList)xpath.evaluate("pattern",node,XPathConstants.NODESET);

		if(nodeList.getLength() == 0) {
			return; // Use default pattern
		}
		
		setPattern(XMLUtils.parseString(random,nodeList.item(random.nextInt(nodeList.getLength())),xpath));
    }
    
	public void setPattern(String patternString) {
		this.pattern = Pattern.parseString(patternString,""+FREE+REPEAT);
		this.patternLength = pattern.size();
	}
}
