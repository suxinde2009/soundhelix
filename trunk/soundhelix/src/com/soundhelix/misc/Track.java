package com.soundhelix.misc;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a track. A track consists of an arbitrary number of sequences and a type.
 * The type can either be MELODY, which means that the sequences contain melodic notes
 * which are subject for transposition, or it can be RHYTHM, which means the sequences
 * contain rhythm notes which must never be transposed. For example, for drum machines,
 * RHYTHM must be used, because each pitch of a sequence selects a drum sample to play
 * (c' might be the base drum, c#' might be the snare and so on) rather than a frequency
 * of a tone to play.
 * 
 * Only a whole track can be assigned to a channel, so all contained sequences use the
 * same channel for playback. If different channels are needed, the sequences must each
 * be put into a track individually.
 *
 * @author Thomas Sch�rger (thomas@schuerger.com)
 */

public class Track {
	// the sequence list
	List<Sequence> seqList = new ArrayList<Sequence>();
	
	public enum TrackType {
		MELODY,
		RHYTHM
	}
	
	// the type
	private final TrackType type;
	
	public Track(TrackType type) {
		this.type = type;
	}

	/**
	 * Adds the given sequence to this track.
	 * 
	 * @param sequence the sequence to add
	 */
	
	public void add(Sequence sequence) {
		seqList.add(sequence);
	}
	
	/**
	 * Returns the number of sequences this track contains.
	 * 
	 * @return the number of sequences
	 */
	
	public int size() {
		return seqList.size();
	}
	
	/**
	 * Returns the track's type.
	 * 
	 * @return the type
	 */
	
	public TrackType getType() {
		return type;
	}
	
	/**
	 * Returns the sequence with the given index.
	 * 
	 * @param index the index
	 * 
	 * @return the sequence
	 */
	
	public Sequence get(int index) {
		return seqList.get(index);
	}
}