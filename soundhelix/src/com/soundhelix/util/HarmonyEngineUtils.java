package com.soundhelix.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.soundhelix.harmonyengine.HarmonyEngine;
import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Structure;

/**
 * Implements some static methods for HarmonyEngine stuff.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public final class HarmonyEngineUtils {
    private HarmonyEngineUtils() {
    }
    
    /**
     * Returns the total number of chord sections.
     * 
     * @param structure the structure
     * 
     * @return the total number of chord sections
     */
    
    public static int getChordSectionCount(Structure structure) {
        HarmonyEngine harmonyEngine = structure.getHarmonyEngine();
        int ticks = structure.getTicks();
        
        // skip through the chord sections
        // and count how many are available

        int sections = 0;

        for (int tick = 0; tick < ticks; tick += harmonyEngine.getChordSectionTicks(tick)) {
            sections++;
        }

        return sections;
    }
    
    /**
     * Returns a string specifying the chord section that starts
     * at the specified tick, which is a comma-separated list of
     * chords and tick lengths. This is done by listing all the
     * chords and their lengths until the next chord section starts
     * or the song ends.
     * 
     * @param structure the structure
     * @param tick the starting tick
     * 
     * @return a chord section string (or null if the tick parameter is invalid)
     */
    
    public static String getChordSectionString(Structure structure, int tick) {
        if (tick < 0 || tick >= structure.getTicks()) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        HarmonyEngine harmonyEngine = structure.getHarmonyEngine();
        
        int tickEnd = tick + harmonyEngine.getChordSectionTicks(tick);

        while (tick < tickEnd) {
            Chord chord = harmonyEngine.getChord(tick);
            int len = harmonyEngine.getChordTicks(tick);
            
            if (sb.length() > 0) {
                sb.append(',');
            }
            
            sb.append(chord.toString());
            sb.append('/');
            
            // the chord section might end before a chord change occurs
            // therefore we need to use Math.min()
            sb.append(Math.min(tickEnd - tick, len));

            tick += len;
        }
        
        return sb.toString();
    }
    
    /**
     * Returns the number of the chord section of the given tick (counted from 0) or -1 if the tick is negative or
     * at or beyond the end of the song.
     * 
     * @param structure the structure
     * @param tick the tick
     *
     * @return the chord section number (or -1)
     */
        
    public static int getChordSectionNumber(Structure structure, int tick) {
        if (tick < 0 || tick >= structure.getTicks()) {
            return -1;
        }
        
        if (tick == 0) {
            // quick win
            return 0;
        }
        
        HarmonyEngine harmonyEngine = structure.getHarmonyEngine();
        
        int count = -1;
        int t = 0;
        
        do {
            t += harmonyEngine.getChordSectionTicks(t);
            count++;
        } while (t <= tick);
        
        return count;
    }
    
    /**
     * Returns the first tick of the given chord section number (starting from 0) or -1 if the chord section number
     * is negative or at or beyond the end of the song.
     * 
     * @param structure the structure
     * @param chordSection the number of the chord section
     *
     * @return the tick (or -1)
     */
        
    public static int getChordSectionTick(Structure structure, int chordSection) {
        if (chordSection < 0 || structure.getTicks() == 0) {
            return -1;
        }
        
        if (chordSection == 0) {
            // quick win
            return 0;
        }
        
        HarmonyEngine harmonyEngine = structure.getHarmonyEngine();

        int ticks = structure.getTicks();
        int tick = 0;
        int count = 0;
        
        do {
            tick += harmonyEngine.getChordSectionTicks(tick);
            count++;
        } while (count < chordSection && tick < ticks);
        
        if (tick >= ticks) {
            // at or beyond the end of the song
            return -1;
        } else {
            return tick;
        }
    }
    
    /**
     * Returns the number of distinct chord sections.
     * 
     * @param structure the structure
     * 
     * @return the number of distinct chord sections
     */
    
    public static int getDistinctChordSectionCount(Structure structure) {
        HarmonyEngine harmonyEngine = structure.getHarmonyEngine();
        int ticks = structure.getTicks();
        
        Map<String, Boolean> ht = new HashMap<String, Boolean>();

        for (int tick = 0; tick < ticks; tick += harmonyEngine.getChordSectionTicks(tick)) {
            ht.put(getChordSectionString(structure, tick), true);
        }

        return ht.size();
    }
    
    /**
     * Returns a list of start ticks for all chord sections, in ascending order.
     * The first chord section will always start at tick 0. The number of list
     * entries always equals getChordSectionCount(structure).
     * 
     * @param structure the structure
     * 
     * @return a list of start ticks for all chord sections
     */
        
    public static List<Integer> getChordSectionStartTicks(Structure structure) {
        HarmonyEngine harmonyEngine = structure.getHarmonyEngine();
        List<Integer> list = new ArrayList<Integer>();

        int ticks = structure.getTicks();
        
        for (int tick = 0; tick < ticks; tick += harmonyEngine.getChordSectionTicks(tick)) {
            list.add(tick);
        }
        
        return list;
    }
    
    /**
     * Returns the minimum and the maximum length of all activity segments of the given ActivityVector as well as the
     * minimum and maximum length of all pauses between activity segments, all counted in chord sections. Returns null
     * if the ActivityVector never becomes active. If the ActivityVector only has one activity segment, then there is
     * no pause, and therefore the minimum and maximum pause length will be 0.
     * 
     * @param structure the structure
     * @param av the ActivityVector
     *
     * @return a 4-element int array containing the minimum and the maximum segment length and the minimum and maximum pause length (in this order) or null
     */
    
    public static int[] getMinMaxSegmentLengths(Structure structure, ActivityVector av) {
        HarmonyEngine harmonyEngine = structure.getHarmonyEngine();
        int ticks = structure.getTicks();
        boolean isActive = false;
        
        int segmentLength = 0;
        int minSegmentLength = Integer.MAX_VALUE;
        int maxSegmentLength = 0;

        int pauseLength = 0;
        int minPauseLength = Integer.MAX_VALUE;
        int maxPauseLength = 0;

        for (int tick = 0; tick < ticks; tick += harmonyEngine.getChordSectionTicks(tick)) {
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
            return new int [] {minSegmentLength, maxSegmentLength, minPauseLength < Integer.MAX_VALUE ? minPauseLength : 0, maxPauseLength};
        }
    }
}
