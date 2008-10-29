package com.soundhelix.lfo;


/**
 * Represents an abstract low frequency oscillator (LFO). The oscillator
 * speed can either be relative (depending on the BPM, e.g., a full rotation
 * every 16 beats) or absolute (depending on time, e.g., a full rotation every
 * 15 seconds). An LFO allows random access to its value given a tick or a millisecond.
 * LFOs can be used for slowly changing certain settings, for example, for changing the
 * filter cutoff frequency of a MIDI device.
 * 
 * This class implements the basic methods for setting the relative or absolute speed
 * of the LFO as well as the minimum and maximum value to return. The type of LFO
 * (e.g., sine, triangle, sawtooth or rectangle wave) must be implemented by subclasses.
 * Subclasses can implement any type of waveform (for example, Bezier-spline interpolated
 * random waveforms), but must return their results in a way so that they depend on the selected
 * speed in a natural way and that they are consistent (same method parameters must
 * return same results for the same configuration of the same instance).
 * 
 * LFO implementation must be able to handle small values for milli-RPM and milli-RPB
 * correctly (i.e., without returning jumpy/glitchy values). This could mean using
 * double arithmetic internally.
 * 
 * @author Thomas Schürger
 */

// TODO: add support for specifying the initial phase

public abstract class LFO {
	protected int minimum;
	protected int maximum;
	
	protected int milliRPM;
	protected int milliRPB;
	protected int milliBPM;
	protected double beatsPerTick;
	
	protected boolean relative;
	
	boolean speedSet;
	
	/**
     * Returns the LFO's value of the given angle as a double.
     * The returned value must be between 0 and 1 (both inclusive).
     * 
     * @param the angle in radians
     * 
     * @return the LFO's value (between 0 and 1, both inclusive)
     */
    
    protected abstract double getValue(double angle);

	/**
	 * Returns the LFO's value of the given tick.
	 * 
	 * @param tick the tick
	 * 
	 * @return the LFO's value
	 */

    public int getTickValue(int tick) {
        if(!speedSet) {
            throw(new RuntimeException("LFO speed not set yet"));           
        }
        
        double angle;
        
        if(relative) {
            angle = Math.PI*(double)tick*beatsPerTick*(double)milliRPB*0.002d;
        } else {
            angle = 2.0d*Math.PI*(double)tick*beatsPerTick/(double)milliBPM*(double)milliRPM;            
        }

        return minimum+(int)(0.5d+(double)maximum*(getValue(angle)));
    }

	/**
	 * Returns the LFO's value of the given millisecond.
	 * 
	 * @param milliSecond the millisecond
	 * 
	 * @return the LFO's value
	 */
	
    public int getMilliSecondValue(int milliSecond) {
        if(!speedSet) {
            throw(new RuntimeException("LFO speed not set yet"));           
        }

        double angle;
        
        if(relative) {
            angle = Math.PI*(double)milliBPM*(double)milliSecond*(double)milliRPB/30000000000d;
        } else {
            angle = Math.PI*(double)milliBPM*(double)milliSecond/(double)milliBPM*(double)milliRPM/30000000d;            
        }

        return minimum+(int)(0.5d+(double)maximum*(getValue(angle)));
    }
    
	/**
	 * Sets the absolute speed of this LFO. Calling this method replaces
	 * the previous absolute or relative speed.
	 * 
	 * @param milliRotationsPerBeat the number of millirotations per beat
	 * @param milliBPM the number of milli-BPM
	 * @param ticksPerBeat the number of ticks per beat
	 */
	
	public void setAbsoluteSpeed(int milliRotationsPerMinute,int milliBPM,int ticksPerBeat) {
		this.beatsPerTick = 1.0d/(double)ticksPerBeat;
		this.milliRPM = milliRotationsPerMinute;
		this.milliBPM = milliBPM;
		this.relative = false;
		this.speedSet = true;
	}
	
	/**
	 * Sets the relative speed of this LFO. Calling this method replaces
	 * the previous absolute or relative speed.
	 * 
	 * @param milliRotationsPerBeat the millirotations per beat
     * @param milliBPM the number of milli-BPM
	 * @param ticksPerBeat the number of ticks per beat
	 */
	
	public void setRelativeSpeed(int milliRotationsPerBeat,int milliBPM,int ticksPerBeat) {	
		this.milliRPB = milliRotationsPerBeat;
		this.milliBPM = milliBPM;
		this.beatsPerTick = 1.0d/(double)ticksPerBeat;
		this.relative = true;		
		this.speedSet = true;
	}

	public void setMinimum(int minimum) {
		this.minimum = minimum;
	}

	public void setMaximum(int maximum) {
		this.maximum = maximum;
	}
}