package com.soundhelix.component.sequenceengine.impl;

/**
 * Implements an abstract SequenceEngine with some basic functionality.
 *
 * @see SequenceEngine
 *
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

import org.apache.log4j.Logger;

import com.soundhelix.component.sequenceengine.SequenceEngine;

public abstract class AbstractSequenceEngine implements SequenceEngine {
    protected final Logger logger;

    protected long randomSeed;

    public AbstractSequenceEngine() {
        logger = Logger.getLogger(this.getClass());
    }

    /**
     * Returns the required number of ActivityVectors. For most implementations, this will be 1, but certain implementations, like for
     * multi-instrument sequences, more than 1 might be required. Subclasses should override this.
     *
     * @return the number of ActivityVectors
     */

    public int getActivityVectorCount() {
        return 1;
    }

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public long getRandomSeed() {
        return randomSeed;
    }
}
