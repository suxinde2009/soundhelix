package com.soundhelix.component.harmonyengine;

import com.soundhelix.component.Component;
import com.soundhelix.misc.Harmony;
import com.soundhelix.misc.SongContext;

/**
 * Interface for song harmony generators. Normally, song harmonies are a sequence of chords (often with the same length) with a certain pattern. The
 * song's complete chord sequence can be divided into repeating sections. For example, a chord sequence might consist of a chord section for a verse
 * and a (possibly longer or shorter) chord section for a refrain, which could each be repeated a couple of times. These chord sections split the
 * chord sequence into a number of logical parts.
 * 
 * For each tick, this generator must return the current chord, the remaining number of ticks this chord will be played before a chord change occurs
 * (or the song ends) and the number of ticks before a new chord section begins (or the song ends).
 * 
 * The methods must always return consistent (i.e., non-contradictory) results. In addition, the results must be persistent, i.e., a given instance
 * must always return the same results for each method parameter.
 * 
 * Consider a simple chord section of "Am F F Am" with 16 ticks each. If this section is used twice ("Am F F Am Am F F Am"), the two consecutive F and
 * Am chords must be merged together, even though a new chord section begins between the two consecutive Am chords. If it is important for the caller
 * to see chord changes as well as chord section changes, simply use Math.min(getChordTicks(tick), getChordSectionTicks(tick)).
 * 
 * This should result in the following method behavior:
 * 
 * tick getChord() getChordTicks() getChordSequenceTicks()
 * 
 * 0 Am 16 64 16 F 32 48 32 F 16 32 48 Am 32 16 64 Am 16 64 80 F 32 48 96 F 16 32 112 Am 16 16 128 undefined undefined undefined
 * 
 * Note that each tick (not only multiples of 16 as shown here) must return correct results. The undefined result is due to the fact that an invalid
 * tick is used (128 is the end of the song).
 * 
 * It is very important to get the method behavior correct. The HarmonyEngine can be sanity-checked using checkSanitiy().
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public interface HarmonyEngine extends Component {

    /**
     * Renders a harmony.
     * 
     * @param songContext the song context
     * 
     * @return the harmony
     */

    Harmony render(SongContext songContext);
}
