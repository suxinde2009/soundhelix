package com.soundhelix.lfo;



/**
 * Represents an abstract low frequency oscillator (LFO). The oscillator can run in one of four
 * modes: synchronized to time (e.g, a full rotation every 5 seconds), synchronized to beat
 * (e.g., a full rotation every 16 beats), synchronized to activity (e.g., a full rotation from
 * activity start to activity end of an instrument) or synchronized to the song length (e.g., 2
 * full rotations over the whole song). An LFO allows random access to its value given a tick.
 * LFOs can be used for slowly changing certain settings, for example, for changing the
 * filter cutoff frequency of a MIDI device.
 * 
 * This class implements the basic methods for setting the mode and speed of the LFO as well
 * as the minimum and maximum value to return. In addition, the LFO's starting phase can be set.
 * The type of LFO (e.g., sine, triangle, sawtooth or rectangle wave) must be implemented by
 * subclasses. Subclasses can implement any type of waveform (for example, Bezier-spline interpolated
 * random waveforms), but must return their results so that they depend on the selected speed in a
 * natural way and that they are consistent (same method parameters must return same results for the
 * same configuration of the same instance).
 * 
 * LFO implementation must be able to handle small values for their parameters and must return values
 * correctly (i.e., without returning jumpy/glitchy values).
 *
 * @author Thomas Schürger
 */

// TODO: make this interface and implementations XML-configurable

public interface LFO {
	/**
	 * Returns the LFO's value of the given tick.
	 * 
	 * @param tick the tick (non-negative)
	 * 
	 * @return the LFO's value
	 */

    public int getTickValue(int tick);

	public void setBeatSpeed(int milliRotationsPerBeat,int ticksPerBeat,int milliBPM);
	public void setSongSpeed(int milliRotationsPerSong,int ticksPerSong,int milliBPM);
	public void setActivitySpeed(int milliRotationsPerActivity,int startTick,int endTick,int milliBPM);
	public void setTimeSpeed(int milliRotationsPerSecond,int ticksPerBeat,int milliBPM);
	public void setPhase(int microRotations);	
	public void setMinimum(int minimum);
	public void setMaximum(int maximum);
}