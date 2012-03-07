package com.soundhelix.lfo;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

/**
 * Implements a low frequency oscillator (LFO) using a rectangle wave, starting with 0. A full LFO rotation corresponds to an angle of 2*Pi radians
 * (360 degrees).
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class RectangleLFO extends AbstractLFO {
    @Override
    public double getValue(double angle) {
        // normalize angle into the range [0,2*Pi[
        angle = ((angle % TWO_PI) + TWO_PI) % TWO_PI;

        return angle < Math.PI ? 0d : 1d;
    }

    @Override
    public final void configure(Node node, XPath xpath) throws XPathException {
    }
}