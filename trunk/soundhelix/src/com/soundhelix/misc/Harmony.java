package com.soundhelix.misc;

/**
 * Represents a harmony. A harmony defines the chord sections and the chords to use for every tick. Instances of this class must consistently return
 * their results.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public interface Harmony {
    /**
     * Returns the chord to use at the specified point in time. Within the valid tick interval this must be non-null (each tick must define a chord).
     *
     * @param tick the tick
     *
     * @return the Chord
     */

    Chord getChord(int tick);

    /**
     * Returns the number of ticks the current chord will be played from the given tick position before the chord will change or the song will end
     * (whichever happens first). This requirement is strict, i.e., the chord must not change before the returned number of ticks and it must change
     * directly afterwards or the song must end. For a valid tick parameter, the return value must always be positive.
     *
     * @param tick the tick
     *
     * @return the number of ticks before the next chord change
     */

    int getChordTicks(int tick);

    /**
     * Returns the number of ticks the current chord section will be played from the given tick position before the next chord section will begin or
     * the song will end. This method can be used to check when special processing (like adding rhythm fill-ins) can be done. For standard chord
     * sections, the total length of the chord section should be used. For a valid tick parameter, the return value must always be positive.
     *
     * @param tick the tick number
     *
     * @return the number of ticks before the next chord section begins or the song will end
     */

    int getChordSectionTicks(int tick);
}
