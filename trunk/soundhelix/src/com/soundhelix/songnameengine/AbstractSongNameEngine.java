package com.soundhelix.songnameengine;

import com.soundhelix.misc.RandomSeedable;
import com.soundhelix.misc.XMLConfigurable;

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
