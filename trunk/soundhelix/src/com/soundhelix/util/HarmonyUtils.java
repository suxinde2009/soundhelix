package com.soundhelix.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Harmony;
import com.soundhelix.misc.SongContext;
import com.soundhelix.misc.Structure;

/**
 * Implements some static methods for Harmony stuff.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public final class HarmonyUtils {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());

    /**
     * Private constructor.
     */

    private HarmonyUtils() {
    }

    /**
     * Returns the total number of chord sections.
     * 
     * @param songContext the song context
     * 
     * @return the total number of chord sections
     */

    public static int getChordSectionCount(SongContext songContext) {
        Harmony harmony = songContext.getHarmony();
        int ticks = songContext.getStructure().getTicks();

        // skip through the chord sections
        // and count how many are available

        int sections = 0;

        for (int tick = 0; tick < ticks; tick += harmony.getChordSectionTicks(tick)) {
            sections++;
        }

        return sections;
    }

    /**
     * Returns a string specifying the chord section that starts at the specified tick, which is a comma-separated list of chords and tick lengths.
     * This is done by listing all the chords and their lengths until the next chord section starts or the song ends.
     * 
     * @param songContext the song context
     * @param tick the starting tick
     * 
     * @return a chord section string (or null if the tick parameter is invalid)
     */

    public static String getChordSectionString(SongContext songContext, int tick) {
        Structure structure = songContext.getStructure();
        Harmony harmony = songContext.getHarmony();

        if (tick < 0 || tick >= structure.getTicks()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        int tickEnd = tick + harmony.getChordSectionTicks(tick);

        while (tick < tickEnd) {
            Chord chord = harmony.getChord(tick);
            int len = harmony.getChordTicks(tick);

            if (sb.length() > 0) {
                sb.append(',');
            }

            sb.append(chord.toString());
            sb.append('/');

            // the chord section might end before a chord change occurs
            // therefore we need to use Math.min()
            sb.append(NumberUtils.toString((double) Math.min(tickEnd - tick, len) / structure.getTicksPerBeat()));

            tick += len;
        }

        return sb.toString();
    }

    /**
     * Returns the number of the chord section of the given tick (counted from 0) or -1 if the tick is negative or at or beyond the end of the song.
     * 
     * @param songContext the song context
     * @param tick the tick
     * 
     * @return the chord section number (or -1)
     */

    public static int getChordSectionNumber(SongContext songContext, int tick) {
        Structure structure = songContext.getStructure();
        Harmony harmony = songContext.getHarmony();

        if (tick < 0 || tick >= structure.getTicks()) {
            return -1;
        }

        if (tick == 0) {
            // quick win
            return 0;
        }

        int count = -1;
        int t = 0;

        do {
            t += harmony.getChordSectionTicks(t);
            count++;
        } while (t <= tick);

        return count;
    }

    /**
     * Returns the first tick of the given chord section number (starting from 0) or -1 if the chord section number is negative or beyond the end of
     * the song. If
     * 
     * @param songContext the song context
     * @param chordSection the number of the chord section
     * 
     * @return the tick (or -1)
     */

    public static int getChordSectionTick(SongContext songContext, int chordSection) {
        Structure structure = songContext.getStructure();
        Harmony harmony = songContext.getHarmony();

        if (chordSection < 0 || structure.getTicks() == 0) {
            return -1;
        }

        if (chordSection == 0) {
            // quick win
            return 0;
        }

        int ticks = structure.getTicks();
        int tick = 0;
        int count = 0;

        do {
            tick += harmony.getChordSectionTicks(tick);
            count++;
        } while (count < chordSection && tick < ticks);

        if (tick > ticks) {
            // beyond the end of the song
            return -1;
        } else {
            return tick;
        }
    }

    /**
     * Returns the number of distinct chord sections.
     * 
     * @param songContext the song context
     * 
     * @return the number of distinct chord sections
     */

    public static int getDistinctChordSectionCount(SongContext songContext) {
        Structure structure = songContext.getStructure();
        Harmony harmony = songContext.getHarmony();

        int ticks = structure.getTicks();

        Map<String, Boolean> ht = new HashMap<String, Boolean>();

        for (int tick = 0; tick < ticks; tick += harmony.getChordSectionTicks(tick)) {
            ht.put(getChordSectionString(songContext, tick), true);
        }

        return ht.size();
    }

    /**
     * Returns a list of start ticks for all chord sections, in ascending order. The first chord section will always start at tick 0. The number of
     * list entries always equals getChordSectionCount(structure).
     * 
     * @param songContext the song context
     * 
     * @return a list of start ticks for all chord sections
     */

    public static List<Integer> getChordSectionStartTicks(SongContext songContext) {
        Structure structure = songContext.getStructure();
        Harmony harmony = songContext.getHarmony();

        List<Integer> list = new ArrayList<Integer>();
        int ticks = structure.getTicks();

        for (int tick = 0; tick < ticks; tick += harmony.getChordSectionTicks(tick)) {
            list.add(tick);
        }

        return list;
    }

    /**
     * Returns the minimum and the maximum length of all activity segments of the given ActivityVector as well as the minimum and maximum length of
     * all pauses between activity segments, all counted in chord sections. Returns null if the ActivityVector never becomes active. If the
     * ActivityVector only has one activity segment, then there is no pause, and therefore the minimum and maximum pause length will be 0.
     * 
     * @param songContext the song context
     * @param av the ActivityVector
     * 
     * @return a 4-element int array containing the minimum and the maximum segment length and the minimum and maximum pause length (in this order) or
     * null
     */

    public static int[] getMinMaxSegmentLengths(SongContext songContext, ActivityVector av) {
        Structure structure = songContext.getStructure();
        Harmony harmony = songContext.getHarmony();

        int ticks = structure.getTicks();
        boolean isActive = false;

        int segmentLength = 0;
        int minSegmentLength = Integer.MAX_VALUE;
        int maxSegmentLength = 0;

        int pauseLength = 0;
        int minPauseLength = Integer.MAX_VALUE;
        int maxPauseLength = 0;

        for (int tick = 0; tick < ticks; tick += harmony.getChordSectionTicks(tick)) {
            if (!isActive) {
                if (av.isActive(tick)) {
                    isActive = true;

                    if (pauseLength > 0 && pauseLength < minPauseLength) {
                        minPauseLength = pauseLength;
                    }

                    if (pauseLength > maxPauseLength) {
                        maxPauseLength = pauseLength;
                    }

                    segmentLength = 1;
                } else if (segmentLength > 0) {
                    pauseLength++;
                }
            } else {
                if (!av.isActive(tick)) {
                    isActive = false;

                    if (segmentLength < minSegmentLength) {
                        minSegmentLength = segmentLength;
                    }

                    if (segmentLength > maxSegmentLength) {
                        maxSegmentLength = segmentLength;
                    }

                    pauseLength = 1;
                } else {
                    segmentLength++;
                }
            }
        }

        if (isActive) {
            if (segmentLength < minSegmentLength) {
                minSegmentLength = segmentLength;
            }

            if (segmentLength > maxSegmentLength) {
                maxSegmentLength = segmentLength;
            }
        }

        if (maxSegmentLength == 0) {
            return null;
        } else {
            return new int[] {minSegmentLength, maxSegmentLength, minPauseLength < Integer.MAX_VALUE ? minPauseLength : 0, maxPauseLength};
        }
    }

    /**
     * Dumps all chords and their lengths in ticks.
     * 
     * @param songContext the song context
     */

    public static void dumpChords(SongContext songContext) {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }

        Structure structure = songContext.getStructure();
        Harmony harmony = songContext.getHarmony();

        StringBuilder sb = new StringBuilder();

        int tick = 0;
        int ticks = structure.getTicks();

        while (tick < ticks) {
            Chord chord = harmony.getChord(tick);
            int len = harmony.getChordTicks(tick);

            if (tick > 0) {
                sb.append(',').append(chord).append('/').append(len);
            } else {
                sb.append(chord).append('/').append(len);
            }

            tick += len;
        }

        LOGGER.debug(sb.toString());
    }

    /**
     * Checks the sanity of the song's harmony. An exception with details will be thrown if the sanity check has failed.
     * 
     * @param songContext the song context
     */

    public static void checkSanity(SongContext songContext) {
        Structure structure = songContext.getStructure();
        Harmony harmony = songContext.getHarmony();

        Chord lastChord = null;
        int lastChordTicks = 1;
        int lastChordSectionTicks = 1;

        int ticks = structure.getTicks();

        for (int tick = 0; tick < ticks; tick++) {
            Chord chord = harmony.getChord(tick);

            if (chord == null) {
                throw new RuntimeException("Null chord returned at tick " + tick);
            }

            int chordTicks = harmony.getChordTicks(tick);

            if (chordTicks <= 0) {
                throw new RuntimeException("Chord ticks <= 0 at tick " + tick);
            }

            if (lastChordTicks > 1 && chordTicks != lastChordTicks - 1) {
                throw new RuntimeException("Chord tick not decremented at " + tick);
            }

            int chordSectionTicks = harmony.getChordSectionTicks(tick);

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

}
