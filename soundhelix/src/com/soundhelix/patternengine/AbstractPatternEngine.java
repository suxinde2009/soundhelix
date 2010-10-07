package com.soundhelix.patternengine;

import org.apache.log4j.Logger;

import com.soundhelix.misc.RandomSeedable;
import com.soundhelix.misc.XMLConfigurable;

public abstract class AbstractPatternEngine implements PatternEngine,RandomSeedable,XMLConfigurable {
	protected long randomSeed;
	
	protected final Logger logger;
	
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
