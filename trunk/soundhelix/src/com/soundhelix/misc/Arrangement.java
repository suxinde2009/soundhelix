package com.soundhelix.misc;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * Represents an arrangement. An arrangement consists of pairs of Tracks and
 * instruments.
 * 
 * @see Track
 *
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class Arrangement implements Iterable<Arrangement.ArrangementEntry>, Serializable {
	/** The structure. */
	private final Structure structure;
	
	/** The list of arrangement entries. */
    private final List<ArrangementEntry> entryList = new ArrayList<ArrangementEntry>();
    
    public Arrangement(Structure structure) {
    	this.structure = structure;
    }
    
    public void add(Track track, String instrument) {
    	entryList.add(new ArrangementEntry(track, instrument));
    }
    
    public Iterator<ArrangementEntry> iterator() {
    	return entryList.iterator();
    }
    
    public int size() {
    	return entryList.size();
    }
    
    public ArrangementEntry get(int index) {
    	return entryList.get(index);
    }

    public Structure getStructure() {
    	return structure;
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
    
    public static final class ArrangementEntry {
    	/** The track. */
    	private final Track track;
    	
    	/** The instrument. */
    	private final String instrument;
    	
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
