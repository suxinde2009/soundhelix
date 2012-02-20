package com.soundhelix.harmonyengine;

import org.apache.log4j.Logger;

import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Structure;

/**
 * Represents an abstract generator for song harmonies. Normally, song harmonies are a sequence
 * of chords (often with the same length) with a certain pattern.
 * The song's complete chord sequence can be divided into repeating sections.
 * For example, a chord sequence might consist of a chord section for
 * a verse and a (possibly longer or shorter) chord section for a refrain,
 * which could each be repeated a couple of times. These chord sections
 * splits the chord sequence into a number of logical parts.
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
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public abstract class AbstractHarmonyEngine implements HarmonyEngine {
    /** The serial version UID. */

    /** The logger. */
    protected final Logger logger;

    /** The structure. */
    protected Structure structure;
    
    /** The random seed. */
    protected long randomSeed;

    public AbstractHarmonyEngine() {
        logger = Logger.getLogger(getClass());
    }
    
    public void setSongStructure(Structure structure) {
        this.structure = structure;
    }

    @Override
    public abstract Chord getChord(int tick);
    
    @Override
    public abstract int getChordTicks(int tick);

    @Override
    public abstract int getChordSectionTicks(int tick);
    
    /**
     * Dumps all chords and their lengths in ticks.
     */
    
    public void dumpChords() {
        int tick = 0;
         
        StringBuilder sb = new StringBuilder();

        while (tick < structure.getTicks()) {
            Chord chord = getChord(tick);
            int len = getChordTicks(tick);

            if (tick > 0) {
                sb.append(',').append(chord).append('/').append(len);
            } else {
                sb.append(chord).append('/').append(len);
            }

            tick += len;
        }

        logger.debug(sb.toString());
    }
    
    /**
     * Checks if the 3 abstract methods return consistent and correct
     * results. In case of a detected problem, a RuntimeException will
     * be thrown.
     */
    
    public void checkSanity() {
        Chord lastChord = null;
        int lastChordTicks = 1;
        int lastChordSectionTicks = 1;
 
        int ticks = structure.getTicks();
        
        for (int tick = 0; tick < ticks; tick++) {
            Chord chord = getChord(tick);
            
            if (chord == null) {
                throw new RuntimeException("Null chord returned at tick " + tick);
            }

            int chordTicks = getChordTicks(tick);
            
            if (chordTicks <= 0) {
                throw new RuntimeException("Chord ticks <= 0 at tick " + tick);
            }

            if (lastChordTicks > 1 && chordTicks != lastChordTicks - 1) {
                throw new RuntimeException("Chord tick not decremented at " + tick);                
            }
            
            int chordSectionTicks = getChordSectionTicks(tick);

            if (chordSectionTicks <= 0) {
                throw new RuntimeException("Chord section ticks <= 0 at tick " + tick);
            }

            if (lastChordSectionTicks > 1 && chordSectionTicks != lastChordSectionTicks - 1) {
                throw new RuntimeException("Chord section tick not decremented at " + tick);                
            }

            if (!chord.equals(lastChord) && lastChordTicks != 1) {
                throw new RuntimeException("Chord changes unexpectedly from " + lastChord + " to " + chord
                                           + " at tick " + tick);
            }
            
            if (chord.equals(lastChord) && lastChordTicks == 1) {
                throw new RuntimeException("Chord was not changed at tick " + tick);
            }

            lastChord = chord;
            lastChordTicks = chordTicks;
            lastChordSectionTicks = chordSectionTicks;
        }
        
        if (lastChordTicks != 1) {
            throw new RuntimeException("Chord ticks is not 1 at last tick");
        }
        
        if (lastChordSectionTicks != 1) {
            throw new RuntimeException("Chord section ticks is not 1 at last tick");
        }
    }
    
    @Override
    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    @Override
    public long getRandomSeed() {
        return randomSeed;
    }
}
