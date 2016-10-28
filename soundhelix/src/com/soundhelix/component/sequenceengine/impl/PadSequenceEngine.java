package com.soundhelix.component.sequenceengine.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Harmony;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.SongContext;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a pad sequence engine using 1 or more voices, playing at a configurable velocity. This engine simply plays each chord for its whole
 * length. The chord tones are selected by specifying a list of chord offsets. For example, offsets 0, 1 and 2 form a normal 3-tone chord. The chord
 * can be "stretched" by using a wider offset range, e.g., 0,1,2,3,4 which would add base and middle tone with increased octave to the normal chord
 * tones.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class PadSequenceEngine extends AbstractSequenceEngine {
    /** The random generator. */
    private Random random;

    /** Flag indicating whether chords should be normalized. */
    private boolean isNormalizeChords;

    /** Flag indicating whether chord sections should be obeyed. */
    private boolean obeyChordSections;

    /** Flag indicating whether all pitches should be retriggered for every new chord. */
    private boolean retriggerPitches = true;

    /** The number of voices. */
    private int voiceCount = -1;

    /** The offsets to use. */
    private int[] offsets;

    /** The velocity to use. */
    private int velocity;

    /*
     * Constructor.
     */

    public PadSequenceEngine() {
        super();
    }

    public void setOffsets(int[] offsets) {
        if (offsets.length == 0) {
            throw new RuntimeException("Array of offsets must not be empty");
        }

        // check uniqueness of offsets

        Set<Integer> set = new HashSet<Integer>(offsets.length);

        for (int offset : offsets) {
            if (set.contains(offset)) {
                throw new RuntimeException("Offsets must be unique");
            }

            set.add(offset);
        }

        this.offsets = offsets;
        this.voiceCount = offsets.length;
    }

    public void setVelocity(int velocity) {
        this.velocity = velocity;
    }

    @Override
    public Track render(SongContext songContext, ActivityVector[] activityVectors) {
        Harmony harmony = songContext.getHarmony();

        ActivityVector activityVector = activityVectors[0];

        Track track = new Track(TrackType.MELODIC);

        for (int i = 0; i < voiceCount; i++) {
            track.add(new Sequence(songContext));
        }

        int tick = 0;
        int ticks = songContext.getStructure().getTicks();

        while (tick < ticks) {
            Chord chord = harmony.getChord(tick);

            if (isNormalizeChords) {
                chord = chord.normalize();
            }

            int len = Math.min(harmony.getChordTicks(tick), activityVector.getIntervalLength(tick));

            if (obeyChordSections) {
                len = Math.min(len, harmony.getChordSectionTicks(tick));
            }

            if (activityVector.isActive(tick)) {
                // check the first sequence (either all voices are active or all are inactive)
                Sequence firstSeq = track.get(0);
                int size = firstSeq.size();

                if (retriggerPitches || size == 0 || firstSeq.get(size - 1).isPause()) {
                    for (int i = 0; i < voiceCount; i++) {
                        int pitch = chord.getPitch(offsets[i]);
                        track.get(i).addNote(pitch, len, velocity);
                    }
                } else {
                    // extend all sequences' notes where the previous and the current pitches match and add new notes
                    // to the other ones

                    // maps pitches of the previous chord to their sequence number
                    Map<Integer, Integer> map = new HashMap<Integer, Integer>(voiceCount);

                    // contains all sequence numbers where the pitch must change
                    Set<Integer> set = new HashSet<Integer>(voiceCount);

                    // iterate over the pitches of the previous chord
                    for (int i = 0; i < voiceCount; i++) {
                        Sequence seq = track.get(i);
                        int pitch = seq.get(seq.size() - 1).getPitch();
                        map.put(pitch, i);
                        set.add(i);
                    }

                    // iterate over all current pitches; extend the ones that match the previous pitch and remove
                    // those from the set of pitches that must change

                    for (int i = 0; i < voiceCount; i++) {
                        int pitch = chord.getPitch(offsets[i]);
                        Integer k = map.get(pitch);

                        if (k != null) {
                            // pitch exists in previous chord, extend that note and remove from set
                            track.get(k).extendNote(len);
                            set.remove(k);
                        }
                    }

                    // from the n sequences, m have been extended, the remaining (n-m) pitches must be assigned
                    // to the (n-m) non-extended sequences

                    Iterator<Integer> it = set.iterator();

                    for (int i = 0; i < voiceCount; i++) {
                        int pitch = chord.getPitch(offsets[i]);

                        if (!map.containsKey(pitch)) {
                            // the pitch was not extended
                            // get the next sequence that was not extended and add a new note to it
                            track.get(it.next()).addNote(pitch, len, velocity);
                        }
                    }
                }
            } else {
                for (int i = 0; i < voiceCount; i++) {
                    track.get(i).addPause(len);
                }
            }
            tick += len;
        }

        return track;
    }

    @Override
    public void configure(SongContext songContext, Node node) throws XPathException {
        random = new Random();

        String offsetString = XMLUtils.parseString(random, "offsets", node);

        if (offsetString == null || offsetString.equals("")) {
            offsetString = "0,1,2";
        }

        String[] offsetList = offsetString.split(",");

        int[] offsets = new int[offsetList.length];

        for (int i = 0; i < offsetList.length; i++) {
            offsets[i] = Integer.parseInt(offsetList[i]);
        }

        setOffsets(offsets);

        try {
            setNormalizeChords(XMLUtils.parseBoolean(random, "normalizeChords", node));
        } catch (Exception e) {}

        try {
            setObeyChordSections(XMLUtils.parseBoolean(random, "obeyChordSections", node));
        } catch (Exception e) {}

        try {
            setRetriggerPitches(XMLUtils.parseBoolean(random, "retriggerPitches", node));
        } catch (Exception e) {}

        try {
            setVelocity(XMLUtils.parseInteger(random, "velocity", node));
        } catch (Exception e) {
            setVelocity(songContext.getStructure().getMaxVelocity());
        }
    }

    public void setObeyChordSections(boolean obeyChordSections) {
        this.obeyChordSections = obeyChordSections;
    }

    public void setNormalizeChords(boolean isNormalizeChords) {
        this.isNormalizeChords = isNormalizeChords;
    }

    public void setRetriggerPitches(boolean retriggerPitches) {
        this.retriggerPitches = retriggerPitches;
    }
}
