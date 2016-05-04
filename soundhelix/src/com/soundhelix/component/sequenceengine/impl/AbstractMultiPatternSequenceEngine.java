package com.soundhelix.component.sequenceengine.impl;

import java.util.Random;

import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Harmony;
import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.SongContext;
import com.soundhelix.misc.Structure;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.util.NoteUtils;

/**
 * Implements a sequence engine that repeats a set of user-specified patterns in a voice each. A pattern is a string containing any number of
 * comma-separated integers, minus and plus signs. Integers play the corresponding note of the chord (0 is the base note, 1 the middle note and so on;
 * the numbers may also be negative). A minus sign is a pause. A plus sign plays a transition note between the current chord and the chord of the next
 * non-transition tone that will be played. The pitch of the transition note is based on the base notes of the two chords. This can be used for funky
 * base lines.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public abstract class AbstractMultiPatternSequenceEngine extends AbstractSequenceEngine {

    /** The transition character. */
    protected static final char TRANSITION = '+';

    /** The random generator. */
    protected Random random;

    /** The boolean indicating whether chords should be normalized. */
    protected boolean isNormalizeChords = true;

    /** The array of patterns. */
    private Pattern[] patterns;

    /**
     * Constructor.
     */

    public AbstractMultiPatternSequenceEngine() {
        super();
    }

    public void setPatterns(Pattern[] patterns) {
        this.patterns = patterns;
    }

    @Override
    public Track render(SongContext songContext, ActivityVector[] activityVectors) {
        Structure structure = songContext.getStructure();
        Harmony harmony = songContext.getHarmony();

        ActivityVector activityVector = activityVectors[0];

        int ticks = structure.getTicks();
        int patternCount = patterns.length;

        Sequence[] seqs = new Sequence[patternCount];

        for (int i = 0; i < patternCount; i++) {
            seqs[i] = new Sequence(songContext);
        }

        Track track = new Track(TrackType.MELODIC);

        for (int i = 0; i < patterns.length; i++) {
            Sequence seq = seqs[i];
            Pattern pattern = patterns[i];
            int patternLength = pattern.size();
            int pos = 0;
            int tick = 0;
            int restartTick = 0;

            while (tick < ticks) {
                if (tick >= restartTick) {
                    pos = 0;
                    restartTick = getNextPatternRestartTick(songContext, tick);
                }

                Chord chord = harmony.getChord(tick);

                if (isNormalizeChords) {
                    chord = chord.normalize();
                }

                Pattern.PatternEntry entry = pattern.get(pos % patternLength);
                int len = Math.min(entry.getTicks(), restartTick - tick);

                if (activityVector.isActive(tick)) {
                    int vel = entry.getVelocity();

                    if (entry.isPause()) {
                        // add pause
                        seq.addPause(len);
                    } else if (entry.isWildcard() && entry.getWildcardCharacter() == TRANSITION) {
                        // find the tick of the next note that will
                        // be played

                        int p = pos + 1;
                        int t = tick + len;

                        while (t < ticks && !pattern.get(p % patternLength).isNote()) {
                            t += pattern.get(p % patternLength).getTicks();
                            p++;
                        }

                        Chord nextChord;

                        if (t < ticks && activityVector.isActive(t)) {
                            nextChord = harmony.getChord(t);
                        } else {
                            // the next chord would either fall into
                            // an inactivity interval or be at the end
                            // of the song
                            nextChord = null;
                        }

                        int pitch;

                        if (isNormalizeChords) {
                            pitch = NoteUtils.getTransitionPitch(chord.normalize(), nextChord != null ? nextChord.normalize() : null);
                        } else {
                            pitch = NoteUtils.getTransitionPitch(chord, nextChord);
                        }

                        boolean useLegato = entry.isLegato() ? pattern.isLegatoLegal(activityVector, tick + len, pos + 1) : false;
                        seq.addNote(pitch, len, vel, useLegato);
                    } else {
                        // normal note
                        int value = entry.getPitch();

                        boolean useLegato = entry.isLegato() ? pattern.isLegatoLegal(activityVector, tick + len, pos + 1) : false;

                        seq.addNote(chord.getPitch(value), len, vel, useLegato);
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

    public void setNormalizeChords(boolean isNormalizeChords) {
        this.isNormalizeChords = isNormalizeChords;
    }
}
