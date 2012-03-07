package com.soundhelix.patternengine;

import org.apache.log4j.Logger;

import com.soundhelix.misc.RandomSeedable;
import com.soundhelix.misc.XMLConfigurable;

/**
 * Implements abstract PatternEngine functionality.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public abstract class AbstractPatternEngine implements PatternEngine, RandomSeedable, XMLConfigurable {
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
