package com.soundhelix.lfo;

/**
 * Implements a low frequency oscillator (LFO) using a triangle wave. A full LFO rotation
 * corresponds to an angle of 2*Pi radians (360 degrees).
 * 
 * @author Thomas Schürger
 */

public class TriangleLFO extends AbstractLFO {
	/** The value of two times Pi. */
	private static final double TWO_PI = 2.0d * Math.PI;
	
	public double getValue(double angle) {
		angle = ((angle % TWO_PI) + TWO_PI) % TWO_PI;
	    
	    if (angle < Math.PI) {
	        return angle / Math.PI;
	    } else {
	        return (TWO_PI - angle) / Math.PI;
	    }	    	    
	}
}