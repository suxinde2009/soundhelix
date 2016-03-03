package com.soundhelix.misc;

/**
 * Defines the immutable logical structure of a song. The logical structure consists of the song's signature details (internal note quantization, song
 * length, etc.).
 * 
 * The main unit is a beat. Beats are divided into ticks (usually 4 ticks form a beat), and beats are grouped into bars (usually 4 beats form a bar),
 * and an integer number of bars forms the song. Ticks are the smallest units of a song.
 * 
 * The song signature is defined by specifying the total number of bars, the number of beats per bar and the number of ticks per beat.
 * 
 * Each point in time within the song is addressed by its tick number (counted from 0). Beats, bars, ticks within a beat and beats within a bar can be
 * derived by using the ticks per beat and the beats per bar, if necessary.
 * 
 * Each component must be ready to handle at least the following ticks per beat: 1, 2, 3, 4, 6, 8, 12, 16. For most music types, 3 (3/4 bars) or 4
 * (4/4 bars) ticks per beat will be fine. In order to mix 3/4 with 4/4 notes, one might consider using 12 ticks per beat, so that quarter notes (3
 * ticks) and third notes (4 ticks) can be used at the same time.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class Structure {
    /** The number of bars. */
    private int bars;

    /** The number of beats per bar. */
    private int beatsPerBar;

    /** The number of ticks per beat. */
    private int ticksPerBeat;

    /** The number of ticks per bar (derived). */
    private int ticksPerBar;

    /** The number of ticks (derived). */
    private int ticks;

    /** The maximum velocity (must be > 0). */
    private int maxVelocity;

    /**
     * Constructor.
     * 
     * @param bars the number of bars
     * @param beatsPerBar the number of beats per bar
     * @param ticksPerBeat the number of ticks per beat
     * @param maxVelocity the maximum velocity
     */

    public Structure(int bars, int beatsPerBar, int ticksPerBeat, int maxVelocity) {
        if (bars <= 0) {
            throw new IllegalArgumentException("bars must be positive");
        }

        if (beatsPerBar <= 0) {
            throw new IllegalArgumentException("beatsPerBar must be positive");
        }

        if (ticksPerBeat <= 0) {
            throw new IllegalArgumentException("ticksPerBeat must be positive");
        }

        if (maxVelocity <= 0) {
            throw new IllegalArgumentException("maxVelocity must be positive");
        }

        this.bars = bars;
        this.beatsPerBar = beatsPerBar;
        this.ticksPerBeat = ticksPerBeat;
        this.maxVelocity = maxVelocity;

        this.ticksPerBar = beatsPerBar * ticksPerBeat;
        this.ticks = bars * ticksPerBar;
    }

    public int getBeatsPerBar() {
        return beatsPerBar;
    }

    public int getTicksPerBeat() {
        return ticksPerBeat;
    }

    public int getTicks() {
        return ticks;
    }

    public int getTicksPerBar() {
        return ticksPerBar;
    }

    public int getBars() {
        return bars;
    }

    public int getMaxVelocity() {
        return maxVelocity;
    }
}
