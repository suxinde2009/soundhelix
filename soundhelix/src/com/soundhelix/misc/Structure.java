package com.soundhelix.misc;

import com.soundhelix.harmonyengine.HarmonyEngine;

/**
 * Defines the logical structure of a song. The logical structure consists of the song's
 * signature (internal note quantization) and the HarmonyEngine.
 * 
 * The main unit is a beat. Beats are divided into ticks (usually 4 ticks form a beat),
 * and beats are grouped into bars (usually 4 beats form a bar), and an integer number of bars
 * forms the song. Ticks are the smallest units of a song.
 * 
 * The song signature is defined by specifying the total number of bars, the number of beats
 * per bar and the number of ticks per beat.
 * 
 * Each point in time within the song is addressed by its tick number (counted from 0). Beats, bars,
 * ticks within a beat and beats within a bar can be derived by using the ticks per beat and
 * the beats per bar, if necessary.
 *
 * Each component must be ready to handle at least the following ticks per beat: 1, 2, 3, 4, 6, 8, 12, 16.
 * For most music types, 3 (3/4 bars) or 4 (4/4 bars) ticks per beat will be fine. In order to mix 3/4 with 4/4
 * notes, one might consider using 12 ticks per beat, so that quarter notes (3 ticks) and third notes
 * (4 ticks) can be used at the same time. 
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class Structure {
    /** The number of bars. */
    private int bars;
    
    /** The number of beats per bar. */
    private int beatsPerBar;
    
    /** The number of ticks per beat. */
    private int ticksPerBeat;
    
    /** The HarmonyEngine. */
    private HarmonyEngine harmonyEngine;
    
    /** The song name. */
    private String songName;
    
    /** The song's random seed. */
    private long randomSeed;
    
    /** The number of ticks per bar (derived). */
    private int ticksPerBar;
    
    /** The number of ticks (derived). */
    private int ticks;
    
    public Structure(int bars, int beatsPerBar, int ticksPerBeat, String songName) {
        this.bars = bars;
        this.beatsPerBar = beatsPerBar;
        this.ticksPerBeat = ticksPerBeat;       

        this.ticksPerBar = beatsPerBar * ticksPerBeat;
        this.ticks = bars * ticksPerBar;

        this.songName = songName;
    }

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
    
    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public HarmonyEngine getHarmonyEngine() {
        return harmonyEngine;
    }
    
    public void setHarmonyEngine(HarmonyEngine harmonyEngine) {
        if (harmonyEngine == null) {
            throw new RuntimeException("HarmonyEngine already set");
        }
        
        this.harmonyEngine = harmonyEngine;
        harmonyEngine.setSongStructure(this);
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }    
}
