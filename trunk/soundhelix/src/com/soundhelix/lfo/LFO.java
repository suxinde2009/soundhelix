package com.soundhelix.lfo;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

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
 * speed and that they are consistent (same method parameters must return same results for
 * the same configuration of the same instance).
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
	
	/**
	 * Returns the LFO's value of the given tick.
	 * 
	 * @param tick the tick
	 * 
	 * @return the LFO's value
	 */
	
	public abstract int getTickValue(int tick);

	/**
	 * Returns the LFO's value of the given millisecond.
	 * 
	 * @param milliSecond the millisecond
	 * 
	 * @return the LFO's value
	 */
	
	public abstract int getMilliSecondValue(int milliSecond);

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
	}
	
	/**
	 * Sets the relative speed of this LFO. Calling this method replaces
	 * the previous absolute or relative speed.
	 * 
	 * @param milliBPM the milli-BPM
	 * @param ticksPerBeat the number of ticks per beat
	 */
	
	public void setRelativeSpeed(int milliRotationsPerBeat,int ticksPerBeat) {	
		this.milliRPB = milliRotationsPerBeat;
		this.beatsPerTick = 1.0d/(double)ticksPerBeat;
		this.relative = true;
	}

	public void setMinimum(int minimum) {
		this.minimum = minimum;
	}

	public void setMaximum(int maximum) {
		this.maximum = maximum;
	}
}