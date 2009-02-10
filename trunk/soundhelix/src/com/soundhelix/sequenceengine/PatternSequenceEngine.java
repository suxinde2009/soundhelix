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
import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Chord.ChordSubtype;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.util.NoteUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a sequence engine that repeats user-specified patterns in a single
 * voice. A pattern is a string containing any number of comma-separated integers, minus and
 * plus signs. Integers play the corresponding note of the chord (0 is the base
 * note, 1 the middle note and so on; the numbers may also be negative). A minus
 * sign is a pause. A plus sign plays a transition note between the current
 * chord and the chord of the next non-transition tone that will be played. The
 * pitch of the transition note is based on the base notes of the two chords.
 * This can be used for funky base lines.
 * <br><br>
 * <b>XML-Configuration</b>
 * <table border=1>
 * <tr><th>Tag</th> <th>#</th> <th>Example</th> <th>Description</th> <th>Required</th>
 * <tr><td><code>pattern</code></td> <td>+</td> <td><code>0,-,-,-,1,-,2,-</code></td> <td>Sets the patterns to use. One of the patterns is selected at random.</td> <td>yes</td>
 * <tr><td><code>obeyChordSubtype</code></td> <td>?</td> <td><code>yes</code></td> <td>Specifies whether to obey chord subtypes (defaults to no).</td> <td>no</td>
 * </table>
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class PatternSequenceEngine extends SequenceEngine {
	
	private static final char TRANSITION = '+';
	
	private static final int[] majorTable = new int[] {0,4,7};
	private static final int[] minorTable = new int[] {0,3,7};

	private Random random;
	
	private boolean obeyChordSubtype = false;
	private int patternLength;
	
	Pattern pattern;
	
	public PatternSequenceEngine() {
		super();
	}

	public void setPattern(String patternString) {
		pattern = Pattern.parseString(patternString,""+TRANSITION);
		this.patternLength = pattern.size();	
	}
	
	public void setObeyChordSubtype(boolean obeyChordSubtype) {
		this.obeyChordSubtype = obeyChordSubtype;
	}

	public Track render(ActivityVector[] activityVectors) {
		ActivityVector activityVector = activityVectors[0];

		Sequence seq = new Sequence();
        HarmonyEngine harmonyEngine = structure.getHarmonyEngine();        
        
        int ticks = structure.getTicks();
        Chord firstChord = harmonyEngine.getChord(0);

        int tick = 0;
        int pos = 0;
        
		while(tick < ticks) {
        	Chord chord = harmonyEngine.getChord(tick);
        	
        	if(obeyChordSubtype) {
        		chord = firstChord.findClosestChord(chord);
        	}

        	Pattern.PatternEntry entry = pattern.get(pos%patternLength);
			int len = entry.getTicks();

       		if(activityVector.isActive(tick)) {
       			short vel = entry.getVelocity();
       			
       			if(entry.isPause()) {
       				// add pause
       				seq.addPause(len);
       			} else if(entry.isWildcard() && entry.getWildcardCharacter() == TRANSITION) {
       				// find the tick of the next note that will
       				// be played

       				int p = pos+1;
       				int t = tick+len;

       				while(t < ticks && (!pattern.get(p%patternLength).isNote())) {
       					t += pattern.get(p%patternLength).getTicks();
       					p++;
       				}

       				Chord nextChord;

       				if(t < ticks && activityVector.isActive(t)) {
       					nextChord = harmonyEngine.getChord(t);
       				} else {
       					// the next chord would either fall into
       					// an inactivity interval or be at the end
       					// of the song
       					nextChord = null;
       				}

       				int pitch = getTransitionPitch(chord,nextChord);

       				seq.addNote(pitch,len,vel);
       			} else {
       				// normal note
       				int value = entry.getPitch();
       				
       				if(obeyChordSubtype) {
       					if(chord.getSubtype() == ChordSubtype.BASE_4) {
       						value++;
       					} else if(chord.getSubtype() == ChordSubtype.BASE_6) {
       						value--;
       					}
       				}

       				// split value into octave and offset
       				// we add 3 to avoid modulo and division issues with
       				// negative values

       				int octave = (value >= 0 ? value/3 : (value-2)/3);
       				int offset = ((value%3)+3)%3;

       				if(chord.isMajor()) {
       					seq.addNote(octave*12+majorTable[offset]+chord.getPitch(),len,vel);
       				} else {
       					seq.addNote(octave*12+minorTable[offset]+chord.getPitch(),len,vel);
       				}
       			}
       		} else {
       			// add pause
       			seq.addPause(len);
       		}
        	
        	tick += len;
        	pos++;

        }
        
		Track track = new Track(TrackType.MELODY);
        track.add(seq);
        return track;
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
    		// we have a pitch difference of at least 3 halftones up
    		pitch1 += Math.min(0,absdiff/2-1);
    		do {
    			pitch1++;
    		} while(!NoteUtils.isOnScale(pitch1));
    	   	return pitch1;
    	} else {
    		// we have a pitch difference of at least 3 halftones down
    		pitch1 -= Math.min(0,absdiff/2-1);
    		do {
    			pitch1--;
    		} while(!NoteUtils.isOnScale(pitch1));
    		return pitch1;
    	}
    }
    	
    public void configure(Node node,XPath xpath) throws XPathException {
    	random = new Random(randomSeed);
    	
		NodeList nodeList = (NodeList)xpath.evaluate("pattern",node,XPathConstants.NODESET);

		if(nodeList.getLength() == 0) {
			throw(new RuntimeException("Need at least 1 pattern"));
		}
		
		try {
			setObeyChordSubtype(XMLUtils.parseBoolean(random,"obeyChordSubtype",node,xpath));
		} catch(Exception e) {}
		
		setPattern(XMLUtils.parseString(random,nodeList.item(random.nextInt(nodeList.getLength())),xpath));
    }
}
