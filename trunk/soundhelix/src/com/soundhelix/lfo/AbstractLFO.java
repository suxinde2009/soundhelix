package com.soundhelix.lfo;


/**
 * This class implements the basic methods for setting the mode and speed of the LFO as well
 * as the minimum and maximum amplitude and value to return. In addition, the LFO's starting phase can be set.
 * The type of LFO (e.g., sine, triangle, sawtooth or rectangle wave) must be implemented by
 * subclasses. Subclasses can implement any type of waveform (for example, Bezier-spline interpolated
 * random waveforms), but must return their results so that they depend on the selected speed in a
 * natural way and that they are consistent (same method parameters must return same results for the
 * same configuration of the same instance).
 *
 * @author Thomas Schürger
 */

public abstract class AbstractLFO implements LFO {
	private static final double TWO_PI = 2.0d*Math.PI;

	/** The minimum amplitude value. */
	private int returnMinimum = Integer.MIN_VALUE;
	
	/** The maximum amplitude value. */
	private int returnMaximum = Integer.MAX_VALUE;

	/** The minimum amplitude value. */
	private int amplitudeMinimum;
	
	/** The maximum amplitude value. */
	private int amplitudeMaximum;
	
	/** The number of microrotations per tick. */
	private long microRotationsPerTick;

	/** The start phase in microrotations. */
	private long microRotationShift;
	
	/** True if one of the set...Speed() methods has been called. */
	private boolean isConfigured;
	
	/**
     * Returns the LFO's value of the given angle as a double.
     * The returned value must be between 0 and 1 (both inclusive).
     * 
     * @param angle the angle in radians (non-negative)
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
        if (!isConfigured) {
            throw(new RuntimeException("LFO speed not set yet"));           
        }
        
        double angle = (TWO_PI / 1000000d) * ((double) tick * microRotationsPerTick + microRotationShift);
        
        int value = amplitudeMinimum + (int)(0.5d + amplitudeMaximum * getValue(angle));
        
        if (value > returnMaximum) {
        	return returnMaximum;
        } else if (value < returnMinimum) {
        	return returnMinimum;
        } else {
        	return value;
        }
    }

	public void setBeatSpeed(int milliRotationsPerBeat,int ticksPerBeat,int milliBPM) {
		this.microRotationsPerTick = milliRotationsPerBeat*1000L/ticksPerBeat; 
		isConfigured = true;
	}

	public void setSongSpeed(int milliRotationsPerSong,int ticksPerSong,int milliBPM) {
		this.microRotationsPerTick = milliRotationsPerSong*1000L/ticksPerSong; 
		isConfigured = true;
	}

	public void setActivitySpeed(int milliRotationsPerActivity,int startTick,int endTick,int milliBPM) {
		this.microRotationsPerTick = milliRotationsPerActivity * 1000L / (endTick - startTick);		
		this.microRotationShift -= microRotationsPerTick * startTick;
		isConfigured = true;
	}

	public void setTimeSpeed(int milliRotationsPerSecond,int ticksPerBeat,int milliBPM) {
		this.microRotationsPerTick = milliRotationsPerSecond * 60000000L / milliBPM / ticksPerBeat;
		isConfigured = true;
	}
	
	public void setPhase(int microRotations) {
		this.microRotationShift = microRotations;
	}

	/**
	 * Sets the minimum value to return.
	 * 
	 * @param minimum the minimum value
	 */
	
	public void setValueMinimum(int minimum) {
		this.returnMinimum = minimum;
	}

	/**
	 * Sets the maximum value to return.
	 * 
	 * @param maximum the maximum value
	 */
	
	public void setValueMaximum(int maximum) {
		this.returnMaximum = maximum;
	}
	/**
	 * Sets the amplitude minimum.
	 * 
	 * @param minimum the amplitude minimum
	 */
	
	public void setAmplitudeMinimum(int minimum) {
		this.amplitudeMinimum = minimum;
	}

	/**
	 * Sets the amplitude maximum.
	 * 
	 * @param maximum the amplitude maximum
	 */
	
	public void setAmplitudeMaximum(int maximum) {
		this.amplitudeMaximum = maximum;
	}
}
