package com.soundhelix.component.harmonyengine.impl;

import org.apache.log4j.Logger;

import com.soundhelix.component.harmonyengine.HarmonyEngine;

/**
 * Implements an abstract HarmonyEngine with some basic functionality.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public abstract class AbstractHarmonyEngine implements HarmonyEngine {
    /** The logger. */
    protected final Logger logger;

    /** The random seed. */
    protected long randomSeed;

    /**
     * Constructor.
     */

    public AbstractHarmonyEngine() {
        logger = Logger.getLogger(getClass());
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
