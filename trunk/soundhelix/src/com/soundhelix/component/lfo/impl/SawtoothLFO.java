package com.soundhelix.component.lfo.impl;

import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

import com.soundhelix.misc.SongContext;

/**
 * Implements a low frequency oscillator (LFO) using a sawtooth wave, starting from 0 if up is true, starting down from 1 otherwise. A full LFO
 * rotation corresponds to an angle of 2*Pi radians (360 degrees).
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class SawtoothLFO extends AbstractLFO {
    /** Flag indicating whether the LFO is an ascending or a descending sawtooth. */
    private boolean up;

    /**
     * Constructor.
     */

    public SawtoothLFO() {
        this(true);
    }

    /**
     * Constructor.
     * 
     * @param up if true, uses ascending mode, otherwise descending mode
     */

    public SawtoothLFO(boolean up) {
        this.up = up;
    }

    @Override
    public double getValue(double angle) {
        // normalize angle into the range [0,2*Pi[
        angle = ((angle % TWO_PI) + TWO_PI) % TWO_PI;

        if (up) {
            return angle / TWO_PI;
        } else {
            return 1.0d - angle / TWO_PI;
        }
    }

    @Override
    public final void configure(SongContext songContext, Node node) throws XPathException {
    }
}