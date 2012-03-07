package com.soundhelix.songnameengine;

/**
 * Implements an abstract SongNameEngine with some basic functionality.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public abstract class AbstractSongNameEngine implements SongNameEngine {
    /** The random seed. */
    protected long randomSeed;

    public long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }
}
