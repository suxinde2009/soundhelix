package com.soundhelix.lfo;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;


/**
 * Implements a low frequency oscillator (LFO) using a triangle wave. A fullLFO rotation
 * corresponds to an angle of 2*Pi radians (360 degrees).
 * 
 * @author Thomas Schürger
 */

public class TriangleLFO extends LFO {
	public double getValue(double angle) {
	    angle = (angle%(2.0d*Math.PI));
	    
	    if(angle <Math.PI) {
	        return angle/Math.PI;
	    } else {
	        return (2.0*Math.PI-angle)/Math.PI;
	    }	    	    
	}
}