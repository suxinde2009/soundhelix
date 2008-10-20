package com.soundhelix.misc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


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
