package com.soundhelix.misc;

/**
 * Simple interface that adds random-seedability to a class. A class that
 * implements this interface, but does not want to use it needs to supply at least
 * a dummy implementation that adheres to the contract described in getRandomSeed().
 *
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public interface RandomSeedable {
	/**
	 * Sets the random seed to use. A class instance given the same configuration and
	 * random seed should produce the same method results when the same methods are
	 * called with the same parameters and in the same order. If this method is not
	 * called on a class instance, the random seed should be set to an arbitrary
	 * value (preferable to something non-constant, e.g., System.nanoTime()).
	 * 
	 * @param seed the random seed
	 */
	
	public void setRandomSeed(long seed);

	/**
	 * Returns the current random seed, which is the last seed set by setRandomSeed()
	 * or the initial random seed if setRandomSeed() hasn't been called before.
	 * 
	 * @return the random seed
	 */
		
	public long getRandomSeed();
}
