package com.soundhelix.patternengine;

public abstract class AbstractPatternEngine implements PatternEngine {
	protected long randomSeed;
	
	public long getRandomSeed() {
		return randomSeed;
	}

	public void setRandomSeed(long seed) {
		this.randomSeed = randomSeed;
	}

}
