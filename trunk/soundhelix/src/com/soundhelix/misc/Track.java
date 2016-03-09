package com.soundhelix.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Represents a track. A track consists of an arbitrary number of sequences and a type. The type can either be MELODIC, which means that the sequences
 * contain melodic notes which are subject for transposition, or it can be RHYTHMIC, which means the sequences contain rhythm notes which must never
 * be transposed. For example, for drum machines, RHYTHMIC must be used, because each pitch of a sequence selects a drum sample to play (c' might be
 * the base drum, c#' might be the snare and so on) rather than a frequency of a tone to play.
 * 
 * Only a whole track can be assigned to an instrument, so all contained sequences use the same instrument for playback. If different instruments are
 * needed, the sequences must each be put into a track individually. The assignment of a whole track to an instrument is also the reason why it
 * doesn't make sense to individually assign the types to sequences. If you have an instrument where part of the keys should use RHYTHMIC, the others
 * should use MELODIC, you should split these off into two different tracks and assign them to the same MIDI channel for playback.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class Track {
    /** The list of sequences. */
    private List<Sequence> sequences = new ArrayList<Sequence>();

    /** The possible track types. */
    public enum TrackType {
        /** The track contains melodic sequences, subject to transposition. */
        MELODIC,
        /** The track contains fixed-pitch sequences, which must not be transposed. */
        RHYTHMIC
    }

    /** The track type. */
    private final TrackType type;

    /** The map that maps LFO sequence names to LFO sequences. */
    private Map<String, LFOSequence> lfoSequenceMap = new HashMap<String, LFOSequence>();

    /**
     * Constructor.
     * 
     * @param type the type
     */

    public Track(TrackType type) {
        this.type = type;
    }

    /**
     * Adds the given sequence to this track.
     * 
     * @param sequence the sequence to add
     */

    public void add(Sequence sequence) {
        sequences.add(sequence);
    }

    /**
     * Adds the given sequence to this track.
     * 
     * @param sequence the sequence to add
     * @param name the LFO name
     */

    public void add(LFOSequence sequence, String name) {
        lfoSequenceMap.put(name, sequence);
    }

    /**
     * Returns the LFO sequence with the given name. IF no such LFO sequence exists, null is returned.
     * 
     * @param name the LFO sequence name.
     * @return the LFO sequence or null
     */

    public LFOSequence getLFOSequence(String name) {
        return lfoSequenceMap.get(name);
    }

    /**
     * Returns the number of sequences this track contains.
     * 
     * @return the number of sequences
     */

    public int size() {
        return sequences.size();
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
        return sequences.get(index);
    }

    /**
     * Provides an iterator that iterates over all Sequences of this Track in the order they have been added.
     * 
     * @return the iterator
     */

    public Iterator<Sequence> iterator() {
        return sequences.iterator();
    }

    /**
     * Transposes all sequences of this track up by the given number of halftones. If the number of halftones is not zero, the track type must not be
     * RHYTHMIC.
     * 
     * @param halftones the number of halftones (positive or negative)
     */

    public void transpose(int halftones) {
        if (halftones == 0) {
            // nothing to do
            return;
        }

        if (type == TrackType.RHYTHMIC) {
            // non-zero transposition is forbidden for this type
            throw new IllegalArgumentException("Tracks of type RHYTHMIC must not be transposed");
        }

        // transpose all the sequences of this track up by the number of halftones

        for (Sequence seq : sequences) {
            seq.transpose(halftones);
        }
    }

    /**
     * Scales the volume of the track by a factor of velocity/maxVelocity.
     * 
     * @param velocity the velocity
     */

    public void scaleVelocity(int velocity) {
        for (Sequence seq : sequences) {
            seq.scaleVelocity(velocity);
        }
    }
}
