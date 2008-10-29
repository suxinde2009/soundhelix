package com.soundhelix.lfo;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;


/**
 * Implements a low frequency oscillator (LFO) using a sawtooth wave, starting from 0 if
 * up is true, starting down from 1 otherwise. A fullL FO rotation corresponds to an angle
 * of 2*Pi radians (360 degrees).
 * 
 * @author Thomas Schürger
 */

public class SawtoothLFO extends LFO {
    boolean up;
    
    private SawtoothLFO() {}
    
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