package com.soundhelix.lfo;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

/**
 * Implements a low frequency oscillator (LFO) using a sine wave. A full LFO rotation
 * corresponds to an angle of 2*Pi radians (360 degrees).
 * 
 * @author Thomas Schürger
 */

public class SineLFO extends AbstractLFO {
	public double getValue(double angle) {
	    return 0.5d + Math.sin(angle) * 0.5d;
	}
	
    public final void configure(Node node, XPath xpath) throws XPathException {
    }
}