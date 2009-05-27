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

public abstract class AbstractLFO implements LFO {
	protected int minimum;
	protected int maximum;
	
	private long microRotationsPerTick;
	private long microRotationShift;
	
	boolean speedSet;
	
	/**
     * Returns the LFO's value of the given angle as a double.
     * The returned value must be between 0 and 1 (both inclusive).
     * 
     * @param the angle in radians (non-negative)
     * 
     * @return the LFO's value (between 0 and 1, both inclusive)
     */
    
    protected abstract double getValue(double angle);

	/**
	 * Returns the LFO's value of the given tick.
	 * 
	 * @param tick the tick (non-negative)
	 * 
	 * @return the LFO's value
	 */

    public int getTickValue(int tick) {
        if(!speedSet) {
            throw(new RuntimeException("LFO speed not set yet"));           
        }
        
        double angle = 2.0d*Math.PI*((double)tick*(double)microRotationsPerTick/1000000.0d + (double)microRotationShift/1000000d);
        
        return minimum+(int)(0.5d+(double)maximum*(getValue(angle)));
    }

	public void setBeatSpeed(int milliRotationsPerBeat,int ticksPerBeat,int milliBPM) {
		this.microRotationsPerTick = milliRotationsPerBeat*1000L/ticksPerBeat; 
		speedSet = true;
	}

	public void setSongSpeed(int milliRotationsPerSong,int ticksPerSong,int milliBPM) {
		this.microRotationsPerTick = milliRotationsPerSong*1000L/ticksPerSong; 
		speedSet = true;
	}

	public void setActivitySpeed(int milliRotationsPerActivity,int startTick,int endTick,int milliBPM) {
		this.microRotationsPerTick = milliRotationsPerActivity*1000L/(endTick-startTick);		
		this.microRotationShift -= microRotationsPerTick*startTick;
		speedSet = true;
	}

	public void setTimeSpeed(int milliRotationsPerSecond,int ticksPerBeat,int milliBPM) {
		this.microRotationsPerTick = milliRotationsPerSecond*60000000L/milliBPM/ticksPerBeat;
		speedSet = true;
	}
	
	public void setPhase(int microRotations) {
		this.microRotationShift = microRotations;
	}

	/**
	 * Sets the minimum value to return.
	 * 
	 * @param minimum the minimum value
	 */
	
	public void setMinimum(int minimum) {
		this.minimum = minimum;
	}

	/**
	 * Sets the maximum value to return.
	 * 
	 * @param maximum the maximum value
	 */
	
	public void setMaximum(int maximum) {
		this.maximum = maximum;
	}
}
