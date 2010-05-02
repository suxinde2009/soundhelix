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
 * Only a whole track can be assigned to an instrument, so all contained sequences use the
 * same instrument for playback. If different instruments are needed, the sequences must each
 * be put into a track individually.
 *
 * @author Thomas Schürger (thomas@schuerger.com)
 */

// TODO: consider moving the TrackType to the Sequence class (as SequenceType)
// this way, different types of sequences could be put into a track, which is currently not possible

public class Track {
	// the sequence list
	List<Sequence> seqList = new ArrayList<Sequence>();
	
	/** The possible track types. */
	public static enum TrackType {
		// the track contains melodic sequences, subject to transposition
		MELODY,
		// the track contains fixed-pitch sequences, which must not be transposed
		RHYTHM
	}
	
	/** The track type. */
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
	
	/**
	 * Transposes all sequences of this track up by the given
	 * number of halftones. If the number of halftones is not zero,
     * the track type must not be RHYTHM.
	 * 
	 * @param halftones the number of halftones (positive or negative)
	 */
	
	public void transpose(int halftones) {
		if (halftones == 0) {
			// nothing to do
			return;
		}
		
		if (type == TrackType.RHYTHM) {
			// transposing is forbidden
			throw(new RuntimeException("Tracks of type RHYTHM must not be transposed"));
		}
		
		// transpose all the sequences of this track
		
		for (Sequence seq : seqList) {
			seq.transpose(halftones);
		}
	}
}
