package com.soundhelix.sequenceengine;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
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
import com.soundhelix.misc.Pattern.PatternEntry;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.patternengine.PatternEngine;
import com.soundhelix.util.HarmonyEngineUtils;
import com.soundhelix.util.NoteUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a sequence engine that uses a randomly generated melody, played
 * with a given rhythmic pattern. For each distinct chord section, a melody
 * is generated and used for each occurrence of the chord section.
 *
 * @author Thomas Schürger (thomas@schuerger.com)
 */

// TODO: add proper configurability

public class MelodySequenceEngine extends AbstractSequenceEngine {
    /** Wildcard for free pitch. */
	private static final char FREE = '+';
	
	/** Wildcard for repeated pitch. */
	private static final char REPEAT = '*';
	
	private Pattern pattern;
	private int patternLength;
	
	private Random random;
	
	public MelodySequenceEngine() {
		super();
	}

	public Track render(ActivityVector[] activityVectors) {
		ActivityVector activityVector = activityVectors[0];

		Sequence seq = new Sequence();
        HarmonyEngine harmonyEngine = structure.getHarmonyEngine();
        
        int tick = 0;
        int ticks = structure.getTicks();
        
        Map<String, Pattern> melodyHashMap = createMelodies();
        
		while (tick < ticks) {
        	int len = harmonyEngine.getChordSectionTicks(tick);
        	Pattern p = melodyHashMap.get(HarmonyEngineUtils.getChordSectionString(structure, tick));

        	int pos = 0;
        	int tickEnd = tick + len;
        	
        	while (tick < tickEnd) {
    			PatternEntry entry = p.get(pos % p.size());
    			int l = entry.getTicks();
    			
    			if (tick + l > tickEnd) {
    			    l = tickEnd - tick;
    			}

    			if (activityVector.isActive(tick)) {	
        			if (entry.isPause()) {
            			seq.addPause(l);
        			} else {
        				seq.addNote(entry.getPitch(), l);
        			}
        		} else {
        			seq.addPause(l);
        		}
        		
    			pos++;
    			tick += l;
         	}
        }
        
		Track track = new Track(TrackType.MELODY);
        track.add(seq);
        return track;
	}
    
    /**
     * Returns a random pitch which is near the given pitch and on the
     * C/Am scale.
     * 
     * @param pitch the starting pitch
     * @param maxDistanceDown the maximum distance below the pitch
     * @param maxDistanceUp the maximum distance above the pitch
     * 
     * @return the random pitch
     */
    
    private int getRandomPitch(int pitch, int maxDistanceDown, int maxDistanceUp) {
    	int p = pitch;
    	boolean again;
    	
    	do {
    		again = false;
    		int r = random.nextInt(3);
    		
    		if (r == 2 && random.nextFloat() > 0.4f) {
    			r = random.nextInt(2);
    		}
    		
    		if (r == 0 || p < -12) {
    			// move up
    			p += random.nextInt(maxDistanceUp);
    			do {
    				p++;
    			} while (!NoteUtils.isOnScale(p));    		
    		} else if (r == 1 || p > 12) {
    			// move down
    			p -= random.nextInt(maxDistanceDown);
    			do {
    				p--;
    			} while (!NoteUtils.isOnScale(p));    		
    		} else {
    			// don't move, but check
    			if (!NoteUtils.isOnScale(p)) {
    				// pitch has to be changed, because it is invalid
    				// we must go up or down, so we'll retry    			
    				again = true;
    				continue;
    			}
    		}
    	} while (again || p < -12 || p > 12);

    	return p;
    }

    /**
     * Returns a random pitch which is near the given pitch and
     * is one of the given chords notes.
     * 
     * @param chord the chord
     * @param pitch the starting pitch
     * @param maxDistanceDown the maximum distance below the pitch
     * @param maxDistanceUp the maximum distance above the pitch
     * 
     * @return the random pitch
     */
    
    private int getRandomPitch(Chord chord, int pitch, int maxDistanceDown, int maxDistanceUp) {
    	int p;
    	boolean again;
    	
    	do {
    		again = false;
    		p = pitch;
    		int r = random.nextInt(3);

    		if (r == 2 && random.nextFloat() > 0.4f) {
    			r = random.nextInt(2);
    		}

    		if (r == 0 || p < -12) {
    			// move up
    			p += random.nextInt(maxDistanceUp);
    			do {
    				p++;
    			} while (!chord.containsPitch(p));    		
    		} else if (r == 1 || p > 12) {
    			// move down
    			p -= random.nextInt(maxDistanceDown);
    			do {
    				p--;
    			} while (!chord.containsPitch(p));    		
    		} else {
    			// don't move, but check
    			if (!chord.containsPitch(p)) {
    				// pitch has to be changed, because it is invalid
    				// we must go up or down, so we'll retry    			
    				again = true;
    			}
    		}
    	} while (again || p < -12 || p > 12);

    	return p;
    }

    /**
     * Creates a melody for each distinct chord section and returns a map mapping chord section strings to
     * melody patterns.
     * 
     * @return a map mapping chord section strings to melody patterns
     */
    
    private Map<String, Pattern> createMelodies() {
    	HarmonyEngine he = structure.getHarmonyEngine();
    	
    	Map<String, Pattern> ht = new HashMap<String, Pattern>();
    	
    	int ticks = structure.getTicks();
    	int tick = 0;
    	int pos = 0;
    	
    	while (tick < ticks) {
    		String section = HarmonyEngineUtils.getChordSectionString(structure, tick);
            int len = he.getChordSectionTicks(tick);
    		
    		if (!ht.containsKey(section)) {
    			// no melody created yet; create one
    			List<PatternEntry> list = new ArrayList<PatternEntry>();    			
    			
    			int pitch = Integer.MIN_VALUE;
    			 			
    			for (int i = 0; i < len;) {
    				PatternEntry entry = pattern.get(pos % patternLength);
        			Chord chord = he.getChord(tick + i);
        			int t = entry.getTicks();
        			
        			if (entry.isPause()) {
        				list.add(new PatternEntry(t));
        			} else if (entry.isWildcard() && entry.getWildcardCharacter() == FREE) {
        				pitch = getRandomPitch(pitch == Integer.MIN_VALUE ? 0 : pitch, 2, 2);
        				list.add(new PatternEntry(pitch, entry.getVelocity(), t, entry.isLegato()));
        			} else if (entry.isWildcard() && entry.getWildcardCharacter() == REPEAT
        					   && pitch != Integer.MIN_VALUE && chord.containsPitch(pitch)) {
        				// reuse the previous pitch
        				list.add(new PatternEntry(pitch, entry.getVelocity(), t, entry.isLegato()));
        			} else {
        				pitch = getRandomPitch(chord, pitch == Integer.MIN_VALUE ? 0 : pitch, 2, 2);
        				list.add(new PatternEntry(pitch, entry.getVelocity(), t, entry.isLegato()));
        			}
        			
        			pos++;
        			i += t;
    			}
   
    			ht.put(section, new Pattern(list.toArray(new PatternEntry[list.size()])));
    		}
    		
    		tick += len;
    	}
    	
    	return ht;
    }
    
    public void configure(Node node, XPath xpath) throws XPathException {
    	random = new Random(randomSeed);
    	
		NodeList nodeList = (NodeList) xpath.evaluate("patternEngine", node, XPathConstants.NODESET);

		if (nodeList.getLength() == 0) {
		    // use default pattern
			return;
		}
		
		PatternEngine patternEngine;
		
		try {
			int i = random.nextInt(nodeList.getLength());
			patternEngine = XMLUtils.getInstance(PatternEngine.class, nodeList.item(i),
					xpath, randomSeed ^ 47351842858L, i);
		} catch (Exception e) {
			throw new RuntimeException("Error instantiating PatternEngine", e);
		}
		
		Pattern pattern = patternEngine.render("" + FREE + REPEAT);
		setPattern(pattern);
    }
    
	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
		this.patternLength = pattern.size();
	}
}
