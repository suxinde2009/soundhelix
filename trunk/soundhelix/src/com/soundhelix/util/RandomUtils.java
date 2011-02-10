package com.soundhelix.util;

import java.util.Random;

/**
 * Implements some static methods for random numbers. All methods need a random generator as
 * a parameter.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public final class RandomUtils {
	private RandomUtils() {}
	
	/**
	 * Returns a random integer uniformly distributed within min
	 * and max (both inclusive), using a granularity of step.
	 * The granularity is based on min, i.e., only values
	 * of the form min+k*step (k = 0, 1, ..., upto a suitable
	 * maximum) can be returned.
	 * 
	 * @param random the random generator
	 * @param min the minimum value (inclusive)
	 * @param max the maximum value (inclusive)
	 * @param step the step (must be >= 0)
     *
	 * @return a uniformly distributed integer
	 */
	
	public static int getUniformInteger(Random random, int min, int max, int step) {
		return min + step * random.nextInt((max - min) / step + 1);
	}

	/**
	 * Returns a random double uniformly distributed within min
	 * (inclusive) and max (exclusive). max cannot be returned by
	 * this method, but even if it could, the probability for the
	 * random value being exactly max would be negligible (in the
	 * order of 2^-53).
	 * 
	 * @param random the random generator
	 * @param min the minimum value (inclusive)
	 * @param max the maximum value (exclusive)
     *
	 * @return a uniformly distributed integer
	 */
	
	public static double getUniformDouble(Random random, double min, double max) {
		return min + (max - min) * random.nextDouble();
	}

	/**
	 * Returns a random double uniformly distributed within min
	 * (inclusive) and max (exclusive), using a granularity of step.
	 * The granularity is based on min, i.e., only values
	 * of the form min+k*step (k = 0, 1, ..., upto a suitable
	 * maximum) can be returned.
	 * 
	 * @param random the random generator
	 * @param min the minimum value (inclusive)
	 * @param max the maximum value (exclusive)
	 * @param step the step (must be >= 0)
     *
	 * @return a uniformly distributed double
	 */
	
	public static double getUniformDouble(Random random, double min, double max, double step) {
		return min + step * Math.floor((max - min) * random.nextDouble() / step);
	}

	/**
	 * Returns a normally distributed random integer within min and max
	 * (both inclusive), having the specified mean and the
	 * specified variance. If the calculated random Gaussian does not
	 * fall into the specified interval between min and max,
	 * the process is repeated until it does.
	 * 
	 * @param random the random generator
	 * @param min the minimum value (inclusive)
	 * @param max the maximum value (inclusive)
	 * @param mean the mean value
	 * @param variance the variance (square of the standard deviation)
	 * 
	 * @return a normally distributed integer 
	 */
	
	public static int getNormalInteger(Random random, int min, int max, double mean, double variance) {
		int value;
		
		do {
			value = (int) (mean + variance * random.nextGaussian());
		} while (value < min || value > max);

		return value;
	}
	
	/**
	 * Returns a normally distributed random double within min and max
	 * (both inclusive), having the specified mean and the
	 * specified variance. If the calculated random Gaussian does not
	 * fall into the specified interval between min and max,
	 * the process is repeated until it does.
	 * 
	 * @param random the random generator
	 * @param min the minimum value (inclusive)
	 * @param max the maximum value (inclusive)
	 * @param mean the mean value
	 * @param variance the variance (square of the standard deviation)
	 * 
	 * @return a normally distributed double
	 */
	
	public static double getNormalDouble(Random random, double min, double max, double mean, double variance) {
		double value;
		
		do {
			value = mean + variance * random.nextGaussian();
		} while (value < min || value > max);

		return value;
	}
	
	/**
     * Returns a random double with a power distribution of the given order between min (inclusive) and max (exclusive).
	 * 
	 * @param random the random generator
	 * @param min the minimum value (inclusive)
	 * @param max the maximum value (exclusive)
	 * @param order the order (1 = linear, 2 = quadratic, 3 = cubic, etc.)
	 * 
	 * @return the random double
	 */
	
	public static double getPowerDouble(Random random, double min, double max, double order) {
		return min + (max - min) * Math.pow(random.nextDouble(), order);
	}

	/**
	 * Converts the given random value (between 0 and 1) into a double with a power distribution
	 * of the given order between min (inclusive) and max (exclusive).
	 * 
	 * @param value the random value (between 0 and 1)
	 * @param min the minimum value (inclusive)
	 * @param max the maximum value (exclusive)
	 * @param order the order (1 = linear, 2 = quadratic, 3 = cubic, etc.)
	 * 
	 * @return the random double
	 */
	
	public static double getPowerDouble(double value, double min, double max, double order) {
		return min + (max - min) * Math.pow(value, order);
	}

	/**
	 * Returns a boolean being true with the given probability.
	 * 
	 * @param random the random generator
	 * @param probability the probability (between 0 and 1)
	 * 
	 * @return a boolean being true with the given probability
	 */
	
	public static boolean getBoolean(Random random, double probability) {
	    if (probability == 0.5d) {
	    	return random.nextBoolean();
	    } else if (probability >= 1.0d) {
			return true;
		} else if (probability <= 0.0d) {
			return false;
		} else {
			return random.nextDouble() < probability;
		}
	}
	
	public long getRandomSeed(Object object) {
	    return (((long) object.getClass().hashCode()) << 32) + (long) object.hashCode();
	}
}
