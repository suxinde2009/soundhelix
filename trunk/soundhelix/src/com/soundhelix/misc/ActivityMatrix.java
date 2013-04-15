package com.soundhelix.misc;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.soundhelix.util.HarmonyUtils;

/**
 * Container for ActivityVectors.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class ActivityMatrix implements Iterable<ActivityVector> {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());
 
    /** The map that maps from names to ActivityVectors. */
    private Map<String, ActivityVector> map = new LinkedHashMap<String, ActivityVector>();

    /**
     * Adds the given ActivityVector. If an ActivityVector with the same name already exists, it will be replaced in its current position.
     * Otherwise it will be appended to the list of ActivityVectors.
     * 
     * @param activityVector the ActivityVector
     */    
    public void add(ActivityVector activityVector) {
        map.put(activityVector.getName(), activityVector);
    }

    /**
     * Looks up and returns the ActivityVector with the given name. If the name is not found, null is returned.
     *
     * @param name the name
     * @return the ActivityVector (or null)
     */
    
    public ActivityVector get(String name) {
        return map.get(name);
    }

    @Override
    public Iterator<ActivityVector> iterator() {
        return map.values().iterator();
    }
    
    /**
     * Dumps the activity vectors as an activity matrix to the log with level DEBUG.
     *
     * @param songContext the song context
     */

    public void dump(SongContext songContext) {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }

        Structure structure = songContext.getStructure();
        
        StringBuilder sb = new StringBuilder("Song's activity matrix:\n");

        int chordSections = HarmonyUtils.getChordSectionCount(songContext);

        int digits = String.valueOf(chordSections - 1).length();
        int div = 1;

        for (int i = 1; i < digits; i++) {
            div *= 10;
        }

        int ticks = structure.getTicks();
        int maxLen = 0;

        for (ActivityVector av : map.values()) {
            maxLen = Math.max(maxLen, av.getName().length());
        }

        maxLen = Math.max(maxLen, "Section #".length());

        for (int d = 0; d < digits; d++) {
            if (d == 0) {
                sb.append(String.format("%" + maxLen + "s: ", "Section #"));
            } else {
                sb.append(String.format("%" + maxLen + "s  ", ""));
            }

            int n = 0;
            for (int tick = 0; tick < ticks; tick += songContext.getHarmony().getChordSectionTicks(tick)) {
                sb.append((n / div) % 10);
                n++;
            }
            sb.append('\n');
            div /= 10;
        }

        for (int i = 0; i < maxLen + chordSections + 9; i++) {
            sb.append('=');
        }

        sb.append('\n');

        for (ActivityVector av : map.values()) {
            sb.append(String.format("%" + maxLen + "s: ", av.getName()));

            for (int tick = 0; tick < ticks; tick += songContext.getHarmony().getChordSectionTicks(tick)) {
                sb.append(av.isActive(tick) ? '*' : '-');
            }

            int activeTicks = av.getActiveTicks();
            sb.append(activeTicks > 0 ? String.format(" %5.1f%%%n", 100.0d * activeTicks / ticks) : "\n");
        }

        for (int i = 0; i < maxLen + chordSections + 9; i++) {
            sb.append('=');
        }

        sb.append('\n');

        sb.append(String.format("%" + maxLen + "s: ", "# active"));

        for (int tick = 0; tick < ticks; tick += songContext.getHarmony().getChordSectionTicks(tick)) {
            int c = 0;

            for (ActivityVector av : map.values()) {
                if (av.isActive(tick)) {
                    c++;
                }
            }

            // output number in base-62 format (0-9, a-z, A-Z)

            if (c >= 36) {
                sb.append((char) ('A' + c - 36));
            } else if (c >= 10) {
                sb.append((char) ('a' + c - 10));
            } else {
                sb.append((char) ('0' + c));
            }
        }

        LOGGER.debug(sb.toString());
    }    
}
