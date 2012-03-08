package com.soundhelix.lfo;

import com.soundhelix.misc.RandomSeedable;
import com.soundhelix.misc.XMLConfigurable;

/**
 * Represents a low frequency oscillator (LFO). The oscillator can run in one of four modes: synchronized to time (e.g, a full rotation every 5
 * seconds), synchronized to beat (e.g., a full rotation every 16 beats), synchronized to activity (e.g., a full rotation from activity start to
 * activity end of an instrument) or synchronized to the song length (e.g., 2 full rotations over the whole song). An LFO allows random access to its
 * value given a tick. LFOs can be used for slowly changing certain settings, for example, for changing the filter cutoff frequency of a MIDI device.
 * 
 * LFO implementation must be able to handle small values for their parameters and must return values correctly (i.e., without returning jumpy/glitchy
 * values).
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public interface LFO extends XMLConfigurable, RandomSeedable {
    /**
     * Returns the LFO's value of the given tick.
     * 
     * @param tick the tick (non-negative)
     * 
     * @return the LFO's value
     */
    int getTickValue(int tick);

    /**
     * Makes this LFO synchronized to beats and sets the parameters.
     * 
     * @param milliRotationsPerBeat the number of millirotations per beat
     * @param ticksPerBeat the ticks per beat
     * @param milliBPM the milli-BPM
     */

    void setBeatSpeed(int milliRotationsPerBeat, int ticksPerBeat, int milliBPM);

    /**
     * Makes this LFO synchronized to the song length and sets the parameters.
     * 
     * @param milliRotationsPerSong the number of millirotations for the whole song
     * @param ticksPerSong the ticks of the song
     * @param milliBPM the milli-BPM
     */

    void setSongSpeed(int milliRotationsPerSong, int ticksPerSong, int milliBPM);

    /**
     * Makes this LFO synchronized to a tick range and sets the parameters.
     * 
     * @param milliRotationsPerActivity the number of millirotations for the tick range
     * @param startTick the start tick
     * @param endTick the end tick
     * @param milliBPM the milli-BPM
     */

    void setActivitySpeed(int milliRotationsPerActivity, int startTick, int endTick, int milliBPM);

    /**
     * Makes this LFO synchronized to time and sets the parameters.
     * 
     * @param milliRotationsPerSecond the number of millirotations per second
     * @param ticksPerBeat the ticks per beat
     * @param milliBPM the milli-BPM
     */

    void setTimeSpeed(int milliRotationsPerSecond, int ticksPerBeat, int milliBPM);

    /**
     * Set the starting phase (the phase to use for tick 0).
     * 
     * @param microRotations the microrotations
     */

    void setPhase(int microRotations);

    /**
     * Sets the minimum value to return. If this value is greater than the amplitude minimum, this results in a cut-off at the given value.
     * 
     * @param minValue the value minimum
     */

    void setMinValue(int minValue);

    /**
     * Sets the maximum value to return. If this value is smaller than the amplitude maximum, this results in a cut-off at the given value.
     * 
     * @param maxValue the value maximum
     */

    void setMaxValue(int maxValue);

    /**
     * Sets the minimum value of the amplitude.
     * 
     * @param minAmplitude the minimum amplitude value
     */

    void setMinAmplitude(int minAmplitude);

    /**
     * Sets the maximum value of the amplitude.
     * 
     * @param maxAmplitude the maximum amplitude value
     */

    void setMaxAmplitude(int maxAmplitude);
}