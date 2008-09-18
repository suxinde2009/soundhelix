package com.soundhelix.misc;

import com.soundhelix.harmonyengine.HarmonyEngine;

/**
 * Defines the logical structure of a song. The logical structure consists of the song's
 * signature (internal note quantization) and the HarmonyEngine.
 * 
 * The main quantization is a beat. Beats are divided into ticks (usually 4 ticks form a beat),
 * and beats are grouped into bars (usually 4 beats form a bar), and an integer number of bars
 * forms the song.
 * 
 * The song signature is defined by specifying the total number of bars, the number of beats
 * per bar and the number of ticks per beat.
 * 
 * Each point in time within the song is addressed by its tick number (counted from 0). Beats, bars,
 * ticks within a beat and beats within a bar can be derived by using the ticks per beat and
 * the beats per bar.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class Structure {
	private int bars;
	private int beatsPerBar;
	private int ticksPerBeat;
	private HarmonyEngine harmonyEngine;
	
	// derived upon instantiation
	private int ticksPerBar;
	private int ticks;
	
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
	
	public HarmonyEngine getHarmonyEngine() {
		return harmonyEngine;
	}
	
	public void setHarmonyEngine(HarmonyEngine harmonyEngine) {
		if(harmonyEngine == null) {
			throw(new RuntimeException("HarmonyEngine already set"));
		}
		
		this.harmonyEngine = harmonyEngine;
		harmonyEngine.setSongStructure(this);
	}
	
	public Structure(int bars,int beatsPerBar,int ticksPerBeat) {
		this.bars = bars;
		this.beatsPerBar = beatsPerBar;
		this.ticksPerBeat = ticksPerBeat;		

		this.ticksPerBar = beatsPerBar*ticksPerBeat;
		this.ticks = bars*ticksPerBar;		
	}
}
