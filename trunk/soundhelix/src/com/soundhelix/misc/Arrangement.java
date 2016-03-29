package com.soundhelix.misc;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Represents an arrangement. An arrangement consists of pairs of Tracks and instruments.
 * 
 * @see com.soundhelix.misc.Track
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class Arrangement implements Iterable<Arrangement.ArrangementEntry> {
    /** The list of arrangement entries. */
    private final List<ArrangementEntry> entryList = new ArrayList<ArrangementEntry>();

    /** The map that maps instruments to arrangement entries. */
    private final Map<String, ArrangementEntry> entryMap = new HashMap<String, ArrangementEntry>();

    /**
     * Constructor.
     */

    public Arrangement() {
    }

    /**
     * Adds the given track and assigns the given instrument to the track.
     * 
     * @param track the track
     * @param instrument the instrument
     */

    public void add(Track track, String instrument) {
        if (entryMap.containsKey(instrument)) {
            throw new IllegalArgumentException("Instrument \"" + instrument + "\" already in arrangement");
        }

        ArrangementEntry entry = new ArrangementEntry(track, instrument);
        entryList.add(entry);
        entryMap.put(instrument, entry);
    }

    /**
     * Provides an iterator that iterates over all ArrangementEntries of this Arrangement in the order the corresponding (Track, Instrument) pairs
     * have been added.
     * 
     * @return the iterator
     */

    @Override
    public Iterator<ArrangementEntry> iterator() {
        return entryList.iterator();
    }

    /**
     * Returns the number of arrangement entries.
     * 
     * @return the number of entries
     */

    public int size() {
        return entryList.size();
    }

    /**
     * Returns the arrangement entry with the given index.
     * 
     * @param index the index (starting with 0)
     * 
     * @return the arrangement entry
     */

    public ArrangementEntry get(int index) {
        return entryList.get(index);
    }

    /**
     * Returns the arrangement entry whose instrument has the given name. If such an entry doesn't exist, null is returned.
     * 
     * @param instrument the name of the instrument
     * 
     * @return the arrangement entry or null
     */

    public ArrangementEntry get(String instrument) {
        return entryMap.get(instrument);
    }

    /**
     * Loads a gzip-serialized arrangement from the given file and returns it.
     * 
     * @param filename the filename of the arrangement to load
     * 
     * @throws IOException in case of an I/O problem
     * @throws ClassNotFoundException in case of a deserialization problem
     * 
     * @return the read arrangement
     */

    public static Arrangement loadArrangement(String filename) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(filename);
        GZIPInputStream gis = new GZIPInputStream(fis);
        ObjectInputStream ois = new ObjectInputStream(gis);
        Arrangement arrangement = (Arrangement) ois.readObject();
        ois.close();
        gis.close();
        fis.close();

        return arrangement;
    }

    /**
     * Serializes this arrangement and writes it in gziped format to the given file.
     * 
     * @param arrangement the arrangement to save
     * @param filename the filename to save the arrangement to
     * 
     * @throws IOException in case of an I/O problem
     */

    public static void saveArrangement(Arrangement arrangement, String filename) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        GZIPOutputStream gos = new GZIPOutputStream(fos, 1 << 18);
        ObjectOutputStream oos = new ObjectOutputStream(gos);
        oos.writeObject(arrangement);
        oos.close();
        gos.close();
        fos.close();
    }

    /**
     * Immutable container for an arrangement entry.
     */

    public static final class ArrangementEntry {
        /** The track. */
        private final Track track;

        /** The instrument. */
        private final String instrument;

        /**
         * Constructor.
         * 
         * @param track the track
         * @param instrument the instrument
         */

        private ArrangementEntry(Track track, String instrument) {
            this.track = track;
            this.instrument = instrument;
        }

        public Track getTrack() {
            return track;
        }

        public String getInstrument() {
            return instrument;
        }
    }
}
