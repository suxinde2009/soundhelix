package com.soundhelix.lfo;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

/**
 * Represents an abstract low frequency oscillator (LFO). The oscillator
 * speed can either be relative (depending on the BPM) or absolute (depending
 * on time). An LFO allows random access to its value given a tick or a
 * millisecond.
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