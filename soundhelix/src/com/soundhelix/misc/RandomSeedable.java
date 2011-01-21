package com.soundhelix.misc;

/**
 * Simple interface that adds random-seedability to a class. A class that implements this interface, but does not
 * want to use it needs to supply at least a dummy implementation that adheres to the contract described in
 * getRandomSeed(). How the random seed is used is up to the classes that implement this interface. The seed is often
 * used to instantiate a random generator. Note that the class java.util.Random only uses the lower 48 bits of the
 * random seed, but other random generators might make use of the full 64 bits of the seed.
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
	
	void setRandomSeed(long seed);

	/**
	 * Returns the current random seed, which is the last seed set by setRandomSeed()
	 * or the initial random seed if setRandomSeed() hasn't been called before.
	 * 
	 * @return the random seed
	 */
		
	long getRandomSeed();
}
