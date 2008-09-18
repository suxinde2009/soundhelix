package com.soundhelix.util;

import java.util.Random;

/**
 * Implements some static random methods.
 * 
 * @author Thomas Schürger
 *
 */

public class RandomUtils {
	private static final Random random = new Random();

	/**
	 * Returns a random integer uniformly distributed within min
	 * and max (both inclusive), using a quantization of step.
	 * The quantization is based on min, i.e., only values
	 * of the form min+k*step (k = 0, 1, ..., upto a suitable
	 * maximum) can be returned.
	 * 
	 * @param min the minimum value
	 * @param max the maximum value
	 * @param step the step (normally 1)
     *
	 * @return a uniformly distributed integer
	 */
	
	public static int getUniformInteger(int min,int max,int step) {
		return min+step*random.nextInt((max-min)/step+1);
	}

	/**
	 * Returns a normally distributed integer within min and max
	 * (both inclusive), having the specified mean and the
	 * specified variance. If the calculated random Gaussian does not
	 * fall into the specified interval between min and max,
	 * the process is repeated until it does.
	 * 
	 * @param min the minimum value
	 * @param max the maximum value
	 * @param mean the mean value
	 * @param variance the variance (square of the standard diviation)
	 * 
	 * @return a normally distributed integer 
	 */
	
	public static int getNormalInteger(int min,int max,double mean,double variance) {
		int value;
		
		do {
			value = (int)(mean+variance*random.nextGaussian());
		} while(value < min || value > max);

		return value;
	}
}
