package com.soundhelix.player;

import org.apache.log4j.Logger;

import com.soundhelix.misc.Arrangement;

/**
 * Represents an abstract real-time player for Arrangements.
 * 
 * @see Arrangement
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

// TODO: check if more methods should be available, for example,
// getting the current tick, setting the current tick

public abstract class AbstractPlayer implements Player {
	protected final Logger logger;

	protected long randomSeed;
	
    public AbstractPlayer() {
    	logger = Logger.getLogger(this.getClass());
    }

    public void setRandomSeed(long randomSeed) {
    	this.randomSeed = randomSeed;
    }

    public long getRandomSeed() {
    	return randomSeed;
    }
}
