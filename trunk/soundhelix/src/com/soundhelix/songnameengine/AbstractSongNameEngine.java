package com.soundhelix.songnameengine;

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
