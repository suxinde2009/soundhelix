package com.soundhelix.lfo;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;


/**
 * Implements a low frequency oscillator (LFO) using a sine wave.
 * 
 * @author Thomas Schürger
 */

public class SineLFO extends LFO {
	/**
	 * Returns the LFO's value of the given tick.
	 * 
	 * @param tick the tick
	 * 
	 * @return the LFO's value
	 */
	
	public int getTickValue(int tick) {
		double beat = (double)tick*beatsPerTick;

		if(relative) {
			return minimum+(int)(0.5d+(double)maximum*(Math.sin(Math.PI*beat*(double)milliRPB*0.002d)*0.5f+0.5f));
		} else {
			double minute = (double)beat/(double)milliBPM*1000d;
			return minimum+(int)(0.5d+(double)maximum*(Math.sin(Math.PI*minute*(double)milliRPM*0.002d)*0.5f+0.5f));			
		}
	}

	/**
	 * Returns the LFO's value of the given millisecond.
	 * 
	 * @param milliSecond the millisecond
	 * 
	 * @return the LFO's value
	 */
	
	public int getMilliSecondValue(int milliSecond) {
		double beat = (double)milliBPM*(double)milliSecond/60000000d;

		if(relative) {
			return minimum+(int)(0.5d+(double)maximum*(Math.sin(Math.PI*beat*(double)milliRPB*0.002d)*0.5f+0.5f));
		} else {
			double minute = (double)beat/(double)milliBPM*1000d;
			return minimum+(int)(0.5d+(double)maximum*(Math.sin(Math.PI*minute*(double)milliRPM*0.002d)*0.5f+0.5f));			
		}
	}
}