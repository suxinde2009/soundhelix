package com.soundhelix.misc;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

public class Arrangement {
	private final Structure structure;
    private final List<ArrangementEntry> entryList = new ArrayList<ArrangementEntry>();
    
    public Arrangement(Structure structure) {
    	this.structure = structure;
    }
    
    public void add(Track track,int instrument) {
    	entryList.add(new ArrangementEntry(track,instrument));
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

    public static Arrangement loadArrangement(String filename) throws IOException,ClassNotFoundException {
		FileInputStream fis = new FileInputStream(filename);
		GZIPInputStream gis = new GZIPInputStream(fis);
		ObjectInputStream ois = new ObjectInputStream(gis);
		Arrangement arrangement = (Arrangement)ois.readObject();
		ois.close();
		gis.close();
		fis.close();
		
		return arrangement;
    }

    public static void saveArrangement(Arrangement arrangement,String filename) throws IOException {
    	FileOutputStream fos = new FileOutputStream(filename);
    	GZIPOutputStream gos = new GZIPOutputStream(fos,1<<18);
		ObjectOutputStream oos = new ObjectOutputStream(gos);
		oos.writeObject(arrangement);
		oos.close();
		gos.close();
		fos.close();
    }
    
    public class ArrangementEntry {
    	private final Track track;
    	private final int instrument;
    	
    	private ArrangementEntry(Track track,int instrument) {
    		this.track = track;
    		this.instrument = instrument;
    	}
    	
    	public Track getTrack() {
    		return track;
    	}
    	
    	public int getInstrument() {
    		return instrument;
    	}
    }
}
