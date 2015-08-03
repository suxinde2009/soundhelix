package com.soundhelix.component.lfo;

import com.soundhelix.component.Component;
import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.SongContext;

/**
 * Represents a low frequency oscillator (LFO). The oscillator can run in one of five synchronization modes: synchronized to time (e.g, a full
 * rotation every 5 seconds), synchronized to beat (e.g., a full rotation every 16 beats), synchronized to activity (e.g., a full rotation from
 * activity start to activity end of an instrument), synchronized to the song length (e.g., 2 full rotations over the whole song) or synchronized to a
 * segment pair (e.g., the first half a rotation in the active segment and the second half in the inactive segment). An LFO allows random access to
 * its value given a tick. LFOs can be used for slowly changing certain settings, for example, for changing the filter cutoff frequency of a MIDI
 * device.
 * 
 * LFO implementation must be able to handle small values for their parameters and must return values correctly (i.e., without returning jumpy/glitchy
 * values).
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public interface LFO extends Component {
    /**
     * Returns the LFO's value at the given tick as an integer, taking scaling (via minAmplitude and maxAmplitude) and cut-off (via minValue and
     * maxValue) into account.
     * 
     * @param tick the tick (non-negative)
     * 
     * @return the LFO's value
     */
    int getTickValue(int tick);

    /**
     * Returns the LFO's raw value (between 0 and 1) at the given tick, without any scaling or cut-off.
     * 
     * @param tick the tick (non-negative)
     * 
     * @return the LFO's value
     */
    double getRawTickValue(int tick);

    /**
     * Makes this LFO synchronized to beats and sets the parameters.
     * 
     * @param rotationsPerBeat the number of rotations per beat
     * @param ticksPerBeat the number of ticks per beat
     */

    void setBeatSpeed(double rotationsPerBeat, int ticksPerBeat);

    /**
     * Makes this LFO synchronized to the song length and sets the parameters.
     * 
     * @param rotationsPerSong the number of rotations for the whole song
     * @param ticksPerSong the ticks of the song
     */

    void setSongSpeed(double rotationsPerSong, int ticksPerSong);

    /**
     * Makes this LFO synchronized to a tick range and sets the parameters.
     * 
     * @param rotationsPerActivity the number of rotations for the tick range
     * @param startTick the start tick
     * @param endTick the end tick
     */

    void setActivitySpeed(double rotationsPerActivity, int startTick, int endTick);

    /**
     * Makes this LFO synchronized to an ActivityVector's activity and pause segments.
     * 
     * @param rotationsPerSegmentPair the number of rotations per segment pair
     * @param activityVector the activity vector
     */

    void setSegmentPairSpeed(double rotationsPerSegmentPair, ActivityVector activityVector);

    /**
     * Makes this LFO synchronized to time and sets the parameters.
     * 
     * @param rotationsPerSecond the number of rotations per second
     * @param ticksPerBeat the ticks per beat
     * @param bpm the BPM
     */

    void setTimeSpeed(double rotationsPerSecond, int ticksPerBeat, double bpm);

    /**
     * Set the starting phase (the phase to use for tick 0). If a phase should be set, it must be set before calling any of the set*Speed() methods.
     * 
     * @param phase the number of rotations for the initial phase
     */

    void setPhase(double phase);

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

    /**
     * Sets the song context.
     * 
     * @param songContext the song context
     */

    void setSongContext(SongContext songContext);
}