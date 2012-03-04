package com.soundhelix.harmonyengine;

import com.soundhelix.misc.Chord;
import com.soundhelix.misc.RandomSeedable;
import com.soundhelix.misc.Structure;
import com.soundhelix.misc.XMLConfigurable;

/**
 * Interface for song harmony generators. Normally, song harmonies are a sequence
 * of chords (often with the same length) with a certain pattern.
 * The song's complete chord sequence can be divided into repeating sections.
 * For example, a chord sequence might consist of a chord section for
 * a verse and a (possibly longer or shorter) chord section for a refrain,
 * which could each be repeated a couple of times. These chord sections
 * split the chord sequence into a number of logical parts.
 * 
 * For each tick, this generator must return the current chord, the remaining
 * number of ticks this chord will be played before a chord change occurs (or the
 * song ends) and the number of ticks before a new chord section begins (or the
 * song ends).
 * 
 * The methods must always return consistent (i.e., non-contradictory) results.
 * In addition, the results must be persistent, i.e., a given instance must always
 * return the same results for each method parameter.
 * 
 * Consider a simple chord section of "Am F F Am" with 16 ticks each. If this
 * section is used twice ("Am F F Am Am F F Am"), the two consecutive F and Am
 * chords must be merged together, even though a new chord section begins between
 * the two consecutive Am chords. If it is important for the caller to see chord
 * changes as well as chord section changes, simply use Math.min(getChordTicks(tick),
 * getChordSectionTicks(tick)).
 * 
 * This should result in the following method behavior:
 * 
 * tick   getChord()  getChordTicks()  getChordSequenceTicks()
 * 
 *   0            Am               16                       64
 *  16             F               32                       48
 *  32             F               16                       32
 *  48            Am               32                       16
 *  64            Am               16                       64
 *  80             F               32                       48
 *  96             F               16                       32
 * 112            Am               16                       16
 * 128     undefined        undefined                undefined
 * 
 * Note that each tick (not only multiples of 16 as shown here) must
 * return correct results. The undefined result is due to the fact that
 * an invalid tick is used (128 is the end of the song).
 * 
 * It is very important to get the method behavior correct. The HarmonyEngine
 * can be sanity-checked using checkSanitiy().
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public interface HarmonyEngine extends XMLConfigurable, RandomSeedable {
    
    /**
     * Sets the song structure.
     * 
     * @param structure the structure
     */
    
    void setSongStructure(Structure structure);
    
    /**
     * Returns the chord to use at the specified point in time.
     * Within the valid tick interval this must be non-null (each tick must define a chord).
     * 
     * @param tick the tick
     * 
     * @return the Chord
     */
    
    Chord getChord(int tick);
    
    /**
     * Returns the number of ticks the current chord will
     * be played from the given tick position before the chord will
     * change or the song will end (whichever happens first).
     * This requirement is strict, i.e., the chord must not change
     * before the returned number of ticks and it must change directly
     * afterwards or the song must end. For a valid tick parameter,
     * the return value must always be positive.
     * 
     * @param tick the tick
     * 
     * @return the number of ticks before the next chord change
     */
    
    int getChordTicks(int tick);

    /**
     * Returns the number of ticks the current chord section will be played from
     * the given tick position before the next chord section will begin or the
     * song will end. This method can be used to check when special processing
     * (like adding rhythm fill-ins) can be done. For standard chord sections,
     * the total length of the chord section should be used. For a valid tick
     * parameter, the return value must always be positive.
     * 
     * @param tick the tick number
     * 
     * @return the number of ticks before the next chord section begins or the song will end
     */
    
    int getChordSectionTicks(int tick);
}
