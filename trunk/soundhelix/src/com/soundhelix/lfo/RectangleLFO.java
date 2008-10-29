package com.soundhelix.lfo;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;


/**
 * Implements a low frequency oscillator (LFO) using a rectangle wave, starting with 0.
 * A full LFO rotation corresponds to an angle of 2*Pi radians (360 degrees).
 * 
 * @author Thomas Schürger
 */

public class RectangleLFO extends LFO {
	public double getValue(double angle) {
	    angle = (angle%(2.0d*Math.PI));
	    return angle < Math.PI ? 0d : 1d;
	}
}