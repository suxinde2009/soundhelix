package com.soundhelix.component.patternengine.impl;

import org.apache.log4j.Logger;

import com.soundhelix.component.patternengine.PatternEngine;

/**
 * Implements abstract PatternEngine functionality.
 *
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public abstract class AbstractPatternEngine implements PatternEngine {
    /** The logger. */
    protected final Logger logger;

    /** The random seed. */
    protected long randomSeed;

    public AbstractPatternEngine() {
        logger = Logger.getLogger(this.getClass());
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }
}
