package com.soundhelix.util;

import java.util.Random;

/**
 * Implements a consistent random generator. Each method of this class will
 * return the same random value consistently, given the same class instance and
 * the same parameter combination. Internally, some seeds are generated upon
 * instantiation, and these seeds are used in combination with the given seed
 * and other parameters to instantiate and use a Random instance. For different
 * parameters the Random class will be seeded differently with a very high
 * probability.
 */

public class ConsistentRandom {
	// main seed
	private long constantSeed = System.nanoTime();
    
	// pertubation seed for booleans
	// these nanoTime() and currentTimeMillis() are correlated,
	// but this should be fine for our purposes
	private long millis = System.currentTimeMillis();
    
    public ConsistentRandom()
    {
    }
    
    /**
     * Returns a random integer between min and max (inclusive).
     * 
     * @param min the minimum value
     * @param max the maximum value (inclusive)
     * @param seed the seed to use
     *
     * @return
     */
    
    public int getInteger(int min,int max,long seed) {
    	Random r = new Random((long)min*93+(long)max*107+constantSeed+seed);
    	return min+r.nextInt(max-min);
    }

    public int getInteger(int min,int max,Object seedObject) {
    	return getInteger(min,max,seedObject.hashCode());
    }

    public boolean getBoolean(long seed) {
    	Random r = new Random(millis+constantSeed+seed);
    	return r.nextBoolean();
    }

    public boolean getBoolean(Object seedObject) {
       	return getBoolean(seedObject.hashCode());
    }
}
