package com.soundhelix.lfo;

/**
 * Implements a low frequency oscillator (LFO) using a sawtooth wave, starting from 0 if
 * up is true, starting down from 1 otherwise. A full LFO rotation corresponds to an angle
 * of 2*Pi radians (360 degrees).
 * 
 * @author Thomas Schürger
 */

public class SawtoothLFO extends AbstractLFO {
	/** The value of two times Pi. */
	private static final double TWO_PI = 2.0d * Math.PI;

	/** Flag indicating whether the LFO is an ascending or a descending sawtooth. */
	private boolean up;
	
    public SawtoothLFO() {
    	this(true);
    }
    
    public SawtoothLFO(boolean up) {
        this.up = up;
    }
    
	public double getValue(double angle) {
		// normalize angle into the range [0,2*Pi[
		angle = ((angle % TWO_PI) + TWO_PI) % TWO_PI;
	    
	    if (up) {
	        return 1.0d - angle / TWO_PI;
	    } else {
	        return angle / TWO_PI;
	    }	    	    
	}
}