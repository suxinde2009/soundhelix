package com.soundhelix.harmonyengine;

import org.apache.log4j.Logger;

import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Structure;

/**
 * Implements an abstract HarmonyEngine with some basic functionality.
 *
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public abstract class AbstractHarmonyEngine implements HarmonyEngine {
    /** The serial version UID. */

    /** The logger. */
    protected final Logger logger;

    /** The structure. */
    protected Structure structure;

    /** The random seed. */
    protected long randomSeed;

    /**
     * Constructor.
     */

    public AbstractHarmonyEngine() {
        logger = Logger.getLogger(getClass());
    }

    public void setSongStructure(Structure structure) {
        this.structure = structure;
    }

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
     * Checks if the 3 abstract methods return consistent and correct results. In case of a detected problem, a RuntimeException will be thrown.
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
                throw new RuntimeException("Chord changes unexpectedly from " + lastChord + " to " + chord + " at tick " + tick);
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
