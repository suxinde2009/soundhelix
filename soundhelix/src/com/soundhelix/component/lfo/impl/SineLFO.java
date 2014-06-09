package com.soundhelix.component.lfo.impl;

import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

import com.soundhelix.misc.SongContext;

/**
 * Implements a low frequency oscillator (LFO) using a sine wave. A full LFO rotation corresponds to an angle of 2*Pi radians (360 degrees).
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class SineLFO extends AbstractLFO {
    @Override
    public double getValue(double angle) {
        return 0.5d + Math.sin(angle) * 0.5d;
    }

    @Override
    public final void configure(SongContext songContext, Node node) throws XPathException {
    }
}