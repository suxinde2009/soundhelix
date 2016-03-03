package com.soundhelix.misc;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an LFO sequence, which defines a raw LFO value (between 0 and 1) per tick.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class LFOSequence {
    /** The list of raw LFO values (between 0 and 1). */
    private List<Double> sequence;

    /** The song context. */
    private SongContext songContext;

    /**
     * Constructor.
     */

    @SuppressWarnings("unused")
    private LFOSequence() {
    }

    /**
     * Constructor.
     * 
     * @param songContext the song context
     */

    public LFOSequence(SongContext songContext) {
        if (songContext == null) {
            throw new IllegalArgumentException("songContext must not be null");
        }

        this.songContext = songContext;
        sequence = new ArrayList<Double>(songContext.getStructure().getTicks());
    }

    /**
     * Adds the value to the sequence with a duration of 1 tick.
     * 
     * @param value the LFO value
     */

    public void addValue(double value) {
        if (sequence.size() >= songContext.getStructure().getTicks()) {
            throw new RuntimeException("LFO sequence too long");
        }

        sequence.add(value);
    }

    /**
     * Adds the value to the sequence with the given duration of ticks.
     * 
     * @param value the LFO value
     * @param ticks the number of ticks
     */

    public void addValue(double value, int ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException("Ticks must be >= 0");
        }

        if (sequence.size() >= songContext.getStructure().getTicks() - ticks + 1) {
            throw new RuntimeException("LFO sequence too long");
        }

        for (int i = 0; i < ticks; i++) {
            sequence.add(value);
        }
    }

    /**
     * Returns the LFO value at the given tick.
     * 
     * @param tick the tick
     * 
     * @return the LFO value
     */

    public double getValue(int tick) {
        return sequence.get(tick);
    }

    /**
     * Returns the total number of ticks this sequence spans.
     * 
     * @return the number of ticks
     */

    public int getTicks() {
        return sequence.size();
    }
}
