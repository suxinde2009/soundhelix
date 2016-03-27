package com.soundhelix.component.lfo.impl;

import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

import com.soundhelix.misc.LFOSequence;
import com.soundhelix.misc.SongContext;

/**
 * Implements an LFO that is based on a precomputed LFO sequence rather than on a function. This LFO is used internally only (for conditional LFOs).
 * Even though it implements the Component interface, it cannot be used as a component.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class LFOSequenceLFO extends AbstractLFO {

    /** The LFO sequence. */
    private LFOSequence lfoSequence;

    /**
     * Constructor.
     */

    @SuppressWarnings("unused")
    private LFOSequenceLFO() {
    }

    /**
     * Constructor.
     * 
     * @param lfoSequence the LFO sequence
     */

    public LFOSequenceLFO(LFOSequence lfoSequence) {
        this.lfoSequence = lfoSequence;

        // no need to set speed, mode, etc., as all of that is already part of the LFO sequence
        isConfigured = true;
    }

    @Override
    protected double getValue(double angle) {
        // not needed, won't be called, because the overriden getRawTickValue() doesn't use getValue()
        return 0;
    }

    @Override
    public double getRawTickValue(int tick) {
        return lfoSequence.getValue(tick);
    }

    @Override
    public void configure(SongContext songContext, Node node) throws XPathException {
        // this is an internal LFO, it cannot be XML-configured
        throw new UnsupportedOperationException();
    }
}
