package com.soundhelix.component.lfo.impl;

import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

import com.soundhelix.misc.SongContext;

/**
 * Implements a low frequency oscillator (LFO) using a triangle wave. A full LFO rotation corresponds to an angle of 2*Pi radians (360 degrees).
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class TriangleLFO extends AbstractLFO {
    @Override
    public double getValue(double angle) {
        angle = ((angle % TWO_PI) + TWO_PI) % TWO_PI;

        if (angle < Math.PI) {
            return angle / Math.PI;
        } else {
            return (TWO_PI - angle) / Math.PI;
        }
    }

    @Override
    public final void configure(SongContext songContext, Node node) throws XPathException {
    }
}