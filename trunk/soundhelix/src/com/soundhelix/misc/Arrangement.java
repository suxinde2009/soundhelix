package com.soundhelix.misc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Represents an arrangement. An arrangement consists of pairs of Tracks and
 * channels. A channel is a virtual instrument number. Tracks that are mapped
 * to the same channel will use the same instrument for playback.
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
    
    public void add(Track track,int channel) {
    	entryList.add(new ArrangementEntry(track,channel));
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
    	Track track;
    	int channel;
    	
    	private ArrangementEntry(Track track,int channel) {
    		this.track = track;
    		this.channel = channel;
    	}
    	
    	public Track getTrack() {
    		return track;
    	}
    	
    	public int getChannel() {
    		return channel;
    	}
    }
}