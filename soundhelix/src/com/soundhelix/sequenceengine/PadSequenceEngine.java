package com.soundhelix.sequenceengine;

import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

import com.soundhelix.harmonyengine.HarmonyEngine;
import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Chord.ChordSubtype;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a pad sequence engine using 1 or more voices, playing at
 * full velocity. This engine simply plays each chord for its whole
 * length. The chord tones are selected by specifying a list of
 * chord offsets. For example, offsets 0, 1 and 2 form a normal
 * 3-tone chord. The chord can be "stretched" by using a wider offset range, e.g.,
 * 0,1,2,3,4 which would add base and middle tone with increased octave to the
 * normal chord tones.
 *
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class PadSequenceEngine extends AbstractSequenceEngine {
    /** The major table. */
    private static final int[] MAJOR_TABLE = new int[] {0, 4, 7};
    
    /** The minor table. */
    private static final int[] MINOR_TABLE = new int[] {0, 3, 7};
    
    /** The random generator. */
    private Random random;

    /** Flag indicating whether chord subtypes should be used. */
    private boolean obeyChordSubtype = true;
    
    /** Flag indicating whether chord sections should be obeyed. */
    protected boolean obeyChordSections;

    /** The number of voices. */
    private int voiceCount = -1;
    
    /** The offsets to use. */
    private int[] offsets;
    
    /** The velocity to use. */
    private short velocity = Short.MAX_VALUE;
    
    public PadSequenceEngine() {
        super();
    }
    
    public void setOffsets(int[] offsets) {
        if (offsets.length == 0) {
            throw new RuntimeException("Array of offsets must not be empty");
        }
        
        this.offsets = offsets;
        this.voiceCount = offsets.length;
    }

    public void setVelocity(short velocity) {
        this.velocity = velocity;
    }

    public void setObeyChordSubtype(boolean obeyChordSubtype) {
        this.obeyChordSubtype = obeyChordSubtype;
    }

    public Track render(ActivityVector[] activityVectors) {
        ActivityVector activityVector = activityVectors[0];

        Track track = new Track(TrackType.MELODY);
        
        for (int i = 0; i < voiceCount; i++) {
            track.add(new Sequence());
        }

        HarmonyEngine ce = structure.getHarmonyEngine();
        
        Chord firstChord = ce.getChord(0);
        
        int tick = 0;
        
        while (tick < structure.getTicks()) {
            Chord chord = firstChord.findClosestChord(ce.getChord(tick));
            
            int len = Math.min(ce.getChordTicks(tick), activityVector.getIntervalLength(tick));

            if (obeyChordSections) {
                len = Math.min(len, ce.getChordSectionTicks(tick));
            }
            
            if (activityVector.isActive(tick)) {
                int shift = 0;

                if (obeyChordSubtype) {
                    if (chord.getSubtype() == ChordSubtype.BASE_4) {
                        shift = 1;
                    } else if (chord.getSubtype() == ChordSubtype.BASE_6) {
                        shift = -1;
                    }
                }

                for (int i = 0; i < voiceCount; i++) {
                    Sequence seq = track.get(i);

                    int pos = offsets[i] + shift;

                    int octave = pos >= 0 ? pos / 3 : (pos - 2) / 3;
                    int offset = ((pos % 3) + 3) % 3;

                    if (chord.isMajor()) {
                        seq.addNote(octave * 12 + MAJOR_TABLE[offset] + chord.getPitch(),
                                len, velocity);
                    } else {
                        seq.addNote(octave * 12 + MINOR_TABLE[offset] + chord.getPitch(),
                                len, velocity);
                    }

                    pos++;
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
    
    public void configure(Node node, XPath xpath) throws XPathException {
        random = new Random();
        
        String offsetString = XMLUtils.parseString(random, "offsets", node, xpath);
        
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
            setObeyChordSubtype(XMLUtils.parseBoolean(random, "obeyChordSubtype", node, xpath));
        } catch (Exception e) {}

        try {
            setObeyChordSections(XMLUtils.parseBoolean(random, "obeyChordSections", node, xpath));
        } catch (Exception e) {}

        try {
            setVelocity((short) XMLUtils.parseInteger(random, "velocity", node, xpath));
        } catch (Exception e) {}
    }

    public void setObeyChordSections(boolean obeyChordSections) {
        this.obeyChordSections = obeyChordSections;
    }
}
