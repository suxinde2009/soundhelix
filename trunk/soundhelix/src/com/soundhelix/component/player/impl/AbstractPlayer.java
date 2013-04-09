package com.soundhelix.component.player.impl;

import org.apache.log4j.Logger;

import com.soundhelix.component.player.Player;
import com.soundhelix.misc.Arrangement;

/**
 * Represents an abstract real-time player for Arrangements.
 * 
 * @see Arrangement
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public abstract class AbstractPlayer implements Player {
    /** The logger. */
    protected final Logger logger;

    /** The random seed. */
    protected long randomSeed;

    public AbstractPlayer() {
        logger = Logger.getLogger(this.getClass());
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
