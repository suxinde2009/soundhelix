package com.soundhelix.lfo;

/**
 * Implements a low frequency oscillator (LFO) using a rectangle wave, starting with 0.
 * A full LFO rotation corresponds to an angle of 2*Pi radians (360 degrees).
 * 
 * @author Thomas Schürger
 */

public class RectangleLFO extends AbstractLFO {
	private static final double TWO_PI = 2.0d*Math.PI;

	public double getValue(double angle) {
		angle = ((angle % TWO_PI) + TWO_PI) % TWO_PI;
	    return angle < Math.PI ? 0d : 1d;
	}
}