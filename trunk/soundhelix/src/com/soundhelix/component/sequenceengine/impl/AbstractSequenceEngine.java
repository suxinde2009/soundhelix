package com.soundhelix.component.sequenceengine.impl;

import org.apache.log4j.Logger;

import com.soundhelix.component.sequenceengine.SequenceEngine;

/**
 * Implements an abstract SequenceEngine with some basic functionality.
 *
 * @see SequenceEngine
 *
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public abstract class AbstractSequenceEngine implements SequenceEngine {
    /** The logger. */
    protected final Logger logger;

    /** The random seed. */
    protected long randomSeed;

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
}
