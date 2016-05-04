package com.soundhelix.component.sequenceengine.impl;

import java.util.Random;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import com.soundhelix.component.sequenceengine.SequenceEngine;
import com.soundhelix.misc.SongContext;
import com.soundhelix.util.XMLUtils;

/**
 * Implements an abstract SequenceEngine with some basic functionality.
 * 
 * @see SequenceEngine
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public abstract class AbstractSequenceEngine implements SequenceEngine {

    protected enum PatternRestartMode {
        /** Never restart the pattern. */
        SONG,

        /** Restart pattern on every new chord section. */
        CHORD_SECTION,

        /** Restart pattern on every new chord. */
        CHORD
    };

    /** The logger. */
    protected final Logger logger;

    /** The random seed. */
    protected long randomSeed;

    /** The pattern restart mode, including its default. */
    protected PatternRestartMode patternRestartMode = PatternRestartMode.SONG;

    /**
     * Constructor.
     */

    public AbstractSequenceEngine() {
        logger = Logger.getLogger(this.getClass());
    }

    /**
     * Returns the required number of ActivityVectors. For most implementations, this will be 1, but certain implementations, like for
     * multi-instrument sequences, more than 1 might be required. Subclasses should override this.
     * 
     * @return the number of ActivityVectors
     */

    @Override
    public int getActivityVectorCount() {
        return 1;
    }

    @Override
    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    @Override
    public long getRandomSeed() {
        return randomSeed;
    }

    /**
     * Returns the tick number of the next required pattern restart after the given tick.
     * 
     * @param songContext the song context
     * @param tick the tick
     * @return the pattern restart tick
     */

    protected int getNextPatternRestartTick(SongContext songContext, int tick) {
        switch (patternRestartMode) {
            case SONG:
                return songContext.getStructure().getTicks();

            case CHORD_SECTION:
                return tick + songContext.getHarmony().getChordSectionTicks(tick);

            case CHORD:

                return tick + songContext.getHarmony().getChordTicks(tick);

            default:
                throw new IllegalStateException("Unsupported PatternRestartMode " + patternRestartMode);
        }
    }

    public void setPatternRestartMode(PatternRestartMode patternRestartMode) {
        if (patternRestartMode == null) {
            throw new IllegalArgumentException("patternRestartMode must be non-null");
        }

        this.patternRestartMode = patternRestartMode;
    }

    protected void configurePatternRestartMode(Random random, Node node) {
        String patternRestartModeString = XMLUtils.parseString(random, "patternRestartMode", node);

        if (patternRestartModeString != null) {
            PatternRestartMode patternRestartMode;

            switch (patternRestartModeString) {
                case "song":
                    patternRestartMode = PatternRestartMode.SONG;
                    break;

                case "chordSection":
                    patternRestartMode = PatternRestartMode.CHORD_SECTION;
                    break;

                case "chord":
                    patternRestartMode = PatternRestartMode.CHORD;
                    break;

                default:
                    throw new RuntimeException("Invalid patternRestartMode \"" + patternRestartModeString + "\"");
            }

            setPatternRestartMode(patternRestartMode);
        }
    }
}
