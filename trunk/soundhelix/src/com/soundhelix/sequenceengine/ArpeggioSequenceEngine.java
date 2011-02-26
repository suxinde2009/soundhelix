package com.soundhelix.sequenceengine;

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
import com.soundhelix.misc.Chord.ChordSubtype;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a sequence engine that repeats user-specified patterns. Patterns are comma-separated integers (notes
 * from chords) or minus signs (pauses). For each chord the pattern with the best-matching length is used. The
 * best-matching pattern is the one with the smallest length that is equal to or larger than the required length or
 * the pattern with the largest length if the former doesn't exist. At least some patterns with lengths of powers
 * of two should be provided.
 *
 * @author Thomas Schürger (thomas@schuerger.com)
 */

// TODO: allow specifying velocities in arpeggio patterns (like in the PatternSequenceEngine)

public class ArpeggioSequenceEngine extends AbstractSequenceEngine {
	private static final int[] MAJOR_TABLE = new int[] {0, 4, 7};
	private static final int[] MINOR_TABLE = new int[] {0, 3, 7};

	private static boolean obeyChordSubtype = true;

	private int[][] patterns;

	public ArpeggioSequenceEngine() {
		super();
	}
	
	public Track render(ActivityVector[] activityVectors) {
		ActivityVector activityVector = activityVectors[0];
		
        Sequence seq = new Sequence();
        HarmonyEngine ce = structure.getHarmonyEngine();
        
        int tick = 0;
        
        while (tick < structure.getTicks()) {
        	Chord chord = ce.getChord(tick);
        	int len = ce.getChordTicks(tick);
        	
        	// get arpeggio table for current chord section length
			int[] pattern = getArpeggioPattern(len);
        	int patternLen = pattern.length;
			
        	for (int i = 0; i < len; i++) {
        		if (activityVector.isActive(tick)) {
        			// add next note for major/minor chord
        			
        			int value = pattern[i % patternLen];
        			
        			if (value == Integer.MIN_VALUE) {
            			// add pause
            			seq.addPause(1);
        				continue;
        			}
        			
        			if (obeyChordSubtype) {
        				if (chord.getSubtype() == ChordSubtype.BASE_4) {
        					value++;
        				} else if (chord.getSubtype() == ChordSubtype.BASE_6) {
        					value--;
        				}
        			}
        			
        			int octave = value >= 0 ? value / 3 : (value - 2) / 3;
        			int offset = ((value % 3) + 3) % 3;
        			
        	 	    if (chord.isMajor()) {
        			    seq.addNote(octave * 12 + MAJOR_TABLE[offset] + chord.getPitch(), 1);
        		    } else {
           			    seq.addNote(octave * 12 + MINOR_TABLE[offset] + chord.getPitch(), 1);
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

	/**
	 * Returns an optimal arpeggio pattern for the given length,
	 * based on a best-fit selection. The method returns the shortest
	 * pattern that has a length of len or more. If such a pattern
	 * doesn't exist, returns the longest pattern shorter than len,
	 * which is the longest pattern available.
	 * 
	 * @param len the length
	 * 
	 * @return the arpeggio pattern
	 */
	
	private int[] getArpeggioPattern(int len) {
		// slow implementation, but this method is only called
		// once per chord and we normally don't have a whole lot of patterns
		
		// might use binary search or caching later
		
		int bestIndex = -1;
		int bestIndexLen = Integer.MAX_VALUE;
		int maxIndex = -1;
		int maxIndexLen = -1;
		
		for (int i = 0; i < patterns.length; i++) {
			int l = patterns[i].length;
			
			if (l >= len && l < bestIndexLen) {
				bestIndex = i;
				bestIndexLen = l;
			} else if (l >= maxIndexLen) {
				maxIndex = i;
				maxIndexLen = l;
			}		
		}
		
		if (bestIndex != -1) {
			return patterns[bestIndex];
		} else {
			// we haven't found an optimal pattern
			// use the longest one we've found
			return patterns[maxIndex];
		}
	}
	
	public void setPatterns(int[][] patterns) {
		this.patterns = patterns;
	}
	
    public void configure(Node node, XPath xpath) throws XPathException {
    	Random random = new Random(randomSeed);
    	
		NodeList nodeList = (NodeList) xpath.evaluate("pattern", node, XPathConstants.NODESET);

		int patterns = nodeList.getLength();
		
		if (nodeList.getLength() == 0) {
			throw new RuntimeException("Need at least 1 pattern");
		}
		
		int[][] array = new int[patterns][];
		
		for (int i = 0; i < patterns; i++) {
			String pattern = XMLUtils.parseString(random, nodeList.item(i), xpath);

			String[] p = pattern.split(",");
			
			int[] l = new int[p.length];
			
			for (int k = 0; k < p.length; k++) {
				if (p[k].equals("-")) {
					l[k] = Integer.MIN_VALUE;
				} else {
				    l[k] = Integer.parseInt(p[k]);
				}
			}
			
			array[i] = l;
		}
		
		setPatterns(array);   
    }
}
