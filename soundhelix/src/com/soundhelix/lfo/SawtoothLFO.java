package com.soundhelix.lfo;

/**
 * Implements a low frequency oscillator (LFO) using a sawtooth wave, starting from 0 if
 * up is true, starting down from 1 otherwise. A full LFO rotation corresponds to an angle
 * of 2*Pi radians (360 degrees).
 * 
 * @author Thomas Schürger
 */

public class SawtoothLFO extends LFO {
    private boolean up;
    
    public SawtoothLFO() {
    	this(true);
    }
    
    public SawtoothLFO(boolean up) {
        this.up = up;
    }
    
	public double getValue(double angle) {
	    angle = (angle%(2.0d*Math.PI));
	    
	    if(up) {
	        return 1.0d-angle/2.0d/Math.PI;
	    } else {
	        return angle/2.0d/Math.PI;
	    }	    	    
	}
}