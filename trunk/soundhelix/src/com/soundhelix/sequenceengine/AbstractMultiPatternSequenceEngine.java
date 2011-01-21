package com.soundhelix.sequenceengine;

import java.util.Random;

import com.soundhelix.harmonyengine.HarmonyEngine;
import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Chord.ChordSubtype;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.util.NoteUtils;

/**
 * Implements a sequence engine that repeats a set of user-specified patterns in a voice each.
 * A pattern is a string containing any number of comma-separated integers, minus and
 * plus signs. Integers play the corresponding note of the chord (0 is the base
 * note, 1 the middle note and so on; the numbers may also be negative). A minus
 * sign is a pause. A plus sign plays a transition note between the current
 * chord and the chord of the next non-transition tone that will be played. The
 * pitch of the transition note is based on the base notes of the two chords.
 * This can be used for funky base lines.
 *
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public abstract class AbstractMultiPatternSequenceEngine extends AbstractSequenceEngine {
	
	protected static final char TRANSITION = '+';
	
	protected static final int[] MAJOR_TABLE = new int[] {0, 4, 7};
	protected static final int[] MINOR_TABLE = new int[] {0, 3, 7};

	protected Random random;
	
	protected boolean obeyChordSubtype;
	
	private Pattern[] patterns;
	
	public AbstractMultiPatternSequenceEngine() {
		super();
	}

	public void setPatterns(Pattern[] patterns) {
		this.patterns = patterns;
	}
	
	public void setObeyChordSubtype(boolean obeyChordSubtype) {
		this.obeyChordSubtype = obeyChordSubtype;
	}

	public Track render(ActivityVector[] activityVectors) {
		ActivityVector activityVector = activityVectors[0];

        HarmonyEngine harmonyEngine = structure.getHarmonyEngine();        
        
        int ticks = structure.getTicks();
        Chord firstChord = harmonyEngine.getChord(0);

        int patternCount = patterns.length;

		Sequence[] seqs = new Sequence[patternCount];
		
		for (int i = 0; i < patternCount; i++) {
			seqs[i] = new Sequence();
		}

		Track track = new Track(TrackType.MELODY);
		
       	for (int i = 0; i < patterns.length; i++) {
    		Sequence seq = seqs[i];
    		Pattern pattern = patterns[i];
    		int patternLength = pattern.size();
    		int pos = 0;
    		int tick = 0;

			while (tick < ticks) {
    			Chord chord = harmonyEngine.getChord(tick);

    			if (obeyChordSubtype) {
    				chord = firstChord.findClosestChord(chord);
    			}

				Pattern.PatternEntry entry = pattern.get(pos % patternLength);
        		int len = entry.getTicks();

        		if (activityVector.isActive(tick)) {
        			short vel = entry.getVelocity();

        			if (entry.isPause()) {
        				// add pause
        				seq.addPause(len);
        			} else if (entry.isWildcard() && entry.getWildcardCharacter() == TRANSITION) {
        				// find the tick of the next note that will
        				// be played

        				int p = pos + 1;
        				int t = tick + len;

        				while (t < ticks && (!pattern.get(p % patternLength).isNote())) {
        					t += pattern.get(p % patternLength).getTicks();
        					p++;
        				}

        				Chord nextChord;

        				if (t < ticks && activityVector.isActive(t)) {
        					nextChord = harmonyEngine.getChord(t);
        				} else {
        					// the next chord would either fall into
        					// an inactivity interval or be at the end
        					// of the song
        					nextChord = null;
        				}

        				int pitch = NoteUtils.getTransitionPitch(chord, nextChord);

                        boolean useLegato = entry.isLegato()
                                            ? pattern.isLegatoLegal(activityVector, tick + len, pos + 1) : false;
        				seq.addNote(pitch, len, vel, useLegato);
        			} else {
        				// normal note
        				int value = entry.getPitch();

        				if (obeyChordSubtype) {
        					if (chord.getSubtype() == ChordSubtype.BASE_4) {
        						value++;
        					} else if (chord.getSubtype() == ChordSubtype.BASE_6) {
        						value--;
        					}
        				}

        				// split value into octave and offset
        				// we add 3 to avoid modulo and division issues with
        				// negative values

        				int octave = value >= 0 ? value / 3 : (value - 2) / 3;
        				int offset = ((value % 3) + 3) % 3;

        				boolean useLegato = entry.isLegato()
                                            ? pattern.isLegatoLegal(activityVector, tick + len, pos + 1) : false;

        				if (chord.isMajor()) {
                            seq.addNote(octave * 12 + MAJOR_TABLE[offset] + chord.getPitch(), len, vel, useLegato);
        				} else {
                            seq.addNote(octave * 12 + MINOR_TABLE[offset] + chord.getPitch(), len, vel, useLegato);
        				}
        			}
        		} else {
        			// add pause
        			seq.addPause(len);
        		}

        		tick += len;
        		pos++;
        	}
    		track.add(seq);
        }
        
        return track;
	}
}
