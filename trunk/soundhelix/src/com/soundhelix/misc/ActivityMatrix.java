package com.soundhelix.misc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

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
    private Map<String, ActivityVector> map = new TreeMap<String, ActivityVector>();

    /**
     * Adds the given ActivityVector. If an ActivityVector with the same name already exists, it will be replaced in its current position. Otherwise
     * it will be appended to the list of ActivityVectors.
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
     *
     * @return the ActivityVector (or null)
     */

    public ActivityVector get(String name) {
        if (name == null) {
            return null;
        }

        return map.get(name);
    }

    /**
     * Provides an iterator that iterates over all ActivityVectors of this ActivityMatrix in the order they have been added.
     * 
     * @return the iterator
     */

    @Override
    public Iterator<ActivityVector> iterator() {
        return map.values().iterator();
    }

    /**
     * Returns the number of ActivityVectors.
     * 
     * @return the number of ActivityVectors
     */

    public int size() {
        return map.size();
    }

    /**
     * Returns a map that maps from ActivityVector name to its iterator index.
     *
     * @return the map
     */

    public Map<String, Integer> getIndexMap() {
        Map<String, Integer> map = new HashMap<String, Integer>(size());

        int i = 0;

        for (ActivityVector vector : this) {
            map.put(vector.getName(), Integer.valueOf(i++));
        }

        return map;
    }

    /**
     * Dumps the activity vectors as an activity matrix using the given log level.
     * 
     * @param songContext the song context
     * @param title the title
     * @param priority the log level to use
     */

    public void dump(SongContext songContext, String title, Priority priority) {
        if (!LOGGER.isEnabledFor(priority)) {
            return;
        }

        Structure structure = songContext.getStructure();

        StringBuilder sb = new StringBuilder(title);
        sb.append(":\n");

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
                sb.append(n / div % 10);
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

        LOGGER.info(sb.toString());
    }
}
