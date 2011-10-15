package com.soundhelix.sequenceengine;

import java.util.Random;

import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Track.TrackType;

/**
 * Implements a sequence engine that repeats a set of user-specified patterns in a voice each.
 * A pattern is a string containing any number of comma-separated integers and minus
 * signs. Integers play the corresponding semitone (0 is C, 1 is C#, etc.; the numbers may also be
 * negative). A minus sign is a pause.
 *
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public abstract class AbstractFreeMultiPatternSequenceEngine extends AbstractSequenceEngine {
    
    /** The random generator. */
    protected Random random;
    
    /** The Pattern array. */
    private Pattern[] patterns;
    
    public AbstractFreeMultiPatternSequenceEngine() {
        super();
    }

    public void setPatterns(Pattern[] patterns) {
        this.patterns = patterns;
    }
    
    public Track render(ActivityVector[] activityVectors) {
        ActivityVector activityVector = activityVectors[0];

        int ticks = structure.getTicks();
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
                Pattern.PatternEntry entry = pattern.get(pos % patternLength);
                int len = entry.getTicks();

                if (activityVector.isActive(tick)) {
                    short vel = entry.getVelocity();

                    if (entry.isPause()) {
                        // add pause
                        seq.addPause(len);
                    } else {
                        // normal note
                        int pitch = entry.getPitch();

                        boolean useLegato = entry.isLegato() 
                                            ? pattern.isLegatoLegal(activityVector, tick + len, pos + 1)
                                            : false;

                        seq.addNote(pitch, len, vel, useLegato);
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
