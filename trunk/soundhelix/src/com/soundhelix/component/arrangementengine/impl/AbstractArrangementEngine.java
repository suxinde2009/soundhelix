package com.soundhelix.component.arrangementengine.impl;

import org.apache.log4j.Logger;

import com.soundhelix.component.arrangementengine.ArrangementEngine;

/**
 * Abstract implementation of an ArrangementEngine, which provides some basic functionality.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public abstract class AbstractArrangementEngine implements ArrangementEngine {
    /** The logger. */
    protected final Logger logger;

    /** The random seed. */
    protected long randomSeed;

    /**
     * Constructor.
     */

    public AbstractArrangementEngine() {
        logger = Logger.getLogger(getClass());
    }

    /**
     * Sets the random seed.
     * 
     * @param randomSeed the random seed
     */

    @Override
    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    /**
     * Returns the random seed.
     * 
     * @return the random seed
     */

    @Override
    public long getRandomSeed() {
        return randomSeed;
    }
}