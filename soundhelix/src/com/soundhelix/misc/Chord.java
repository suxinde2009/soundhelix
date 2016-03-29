package com.soundhelix.misc;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.soundhelix.util.NoteUtils;

/**
 * Defines a chord. A chord is immutable and consists of 3 different pitches, which cannot span more than 11 halftones. Each chord is available in 3
 * inversions (no inversion, first or second inversion). All chords can be rotated up or down. Rotating a chord up means replacing the chord's low
 * pitch with the same pitch transposed one octave up. Rotating a chord down means replacing the chord's high pitch with the same pitch transposed one
 * octave down. All common type chords (except for augmented chords) can be normalized, which means that the chords are rotated up or down so that
 * their low pitch equals the root pitch of the chord (e.g., "C4" and "C6" are normalized to "C"), if that is not already the case. Augmented chords
 * cannot be normalized, because they don't have a unique root pitch (e.g., the chords "Caug", "Eaug6" and "G#aug4" are not distinguishable if you
 * look at their pitches).
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class Chord {

    /** The chord inversion type. */
    public enum InversionType {
        /** No inversion. */
        NONE,
        /** First inversion. */
        FIRST,
        /** Second inversion. */
        SECOND
    };

    /** Pattern for arbitrary chords ("pitch1:pitch2:pitch3"). */
    private static final Pattern GENERIC_CHORD_PATTERN = Pattern.compile("^(-?\\d+):(-?\\d+):(-?\\d+)$");

    /** Maps from chord names to chord codes. */
    private static final Map<String, Integer> NAME_TO_CODE_MAP = new LinkedHashMap<String, Integer>();

    /** Maps from chord codes to chord names. */
    private static final Map<Integer, String> CODE_TO_NAME_MAP = new HashMap<Integer, String>();

    /** Maps from flavor to inversion type. */
    private static final Map<Integer, InversionType> INVERSION_TYPE_MAP = new HashMap<Integer, InversionType>();

    /** The chord templates. */
    private static ChordTemplate[] chordTemplates = {new ChordTemplate("", 4, 7), new ChordTemplate("m", 3, 7), new ChordTemplate("7", 4, 10),
            new ChordTemplate("m7", 3, 10), new ChordTemplate("aug", 4, 8), new ChordTemplate("dim", 3, 6), new ChordTemplate("sus4", 5, 7),
            new ChordTemplate("sus2", 2, 7)};

    /** The low pitch of the chord. */
    private final int lowPitch;

    /** The middle pitch of the chord. */
    private final int middlePitch;

    /** The high pitch of the chord. */
    private final int highPitch;

    /**
     * The flavor of the chord (encoded as the difference of the middle pitch and the low pitch times hundred plus the difference of the high pitch
     * and the low pitch). The flavor is independent of the low pitch.
     */
    private final int flavor;

    /**
     * The integer that uniquely identifies the chords, including the chord's low pitch, but the octave is normalized.
     */
    private final int code;

    static {
        for (int i = 0; i < 12; i++) {
            for (ChordTemplate chordTemplate : chordTemplates) {
                final int diff1 = chordTemplate.diff1;
                final int diff2 = chordTemplate.diff2;

                int flavor = getFlavor(diff1, diff2);
                int flavor6 = getFlavor(12 - diff2, diff1 + 12 - diff2);
                int flavor4 = getFlavor(diff2 - diff1, 12 - diff1);

                NAME_TO_CODE_MAP.put(NoteUtils.getSharpNoteName(i) + chordTemplate.name, getCode(i, flavor));
                NAME_TO_CODE_MAP.put(NoteUtils.getFlatNoteName(i) + chordTemplate.name, getCode(i, flavor));
                NAME_TO_CODE_MAP.put(NoteUtils.getSharpNoteName(i) + chordTemplate.name + "6", getCode((i + diff2) % 12, flavor6));
                NAME_TO_CODE_MAP.put(NoteUtils.getFlatNoteName(i) + chordTemplate.name + "6", getCode((i + diff2) % 12, flavor6));
                NAME_TO_CODE_MAP.put(NoteUtils.getSharpNoteName(i) + chordTemplate.name + "4", getCode((i + diff1) % 12, flavor4));
                NAME_TO_CODE_MAP.put(NoteUtils.getFlatNoteName(i) + chordTemplate.name + "4", getCode((i + diff1) % 12, flavor4));

                // order is important here; for "aug" chords, all flavors are equal, and the inversion type should be NONE

                INVERSION_TYPE_MAP.put(flavor6, InversionType.SECOND);
                INVERSION_TYPE_MAP.put(flavor4, InversionType.FIRST);
                INVERSION_TYPE_MAP.put(flavor, InversionType.NONE);
            }
        }

        // create the reverse version of the upper map

        for (Map.Entry<String, Integer> entry : NAME_TO_CODE_MAP.entrySet()) {
            CODE_TO_NAME_MAP.put(entry.getValue(), entry.getKey());
        }
    }

    /**
     * Instantiates a chord. Pitches are sorted automatically by their value and are used as the low, middle and high pitch, respectively.
     * 
     * @param pitch1 the first pitch
     * @param pitch2 the second pitch
     * @param pitch3 the third pitch
     */

    public Chord(int pitch1, int pitch2, int pitch3) {
        // sort the three pitches by their value

        int min = Math.min(pitch1, Math.min(pitch2, pitch3));
        int max = Math.max(pitch1, Math.max(pitch2, pitch3));

        int lowPitch = min;
        int middlePitch = min == pitch1 ? max == pitch2 ? pitch3 : pitch2 : max == pitch1 ? min == pitch2 ? pitch3 : pitch2 : pitch1;
        int highPitch = max;

        if (lowPitch == middlePitch || middlePitch == highPitch) {
            throw new IllegalArgumentException("Duplicate pitches in chord " + this);
        }

        if (highPitch - lowPitch >= 12) {
            throw new IllegalArgumentException("High and low pitch are more than 11 halftones apart in chord " + this);
        }

        this.lowPitch = lowPitch;
        this.middlePitch = middlePitch;
        this.highPitch = highPitch;

        flavor = getFlavor(middlePitch - lowPitch, highPitch - lowPitch);
        code = getCode((lowPitch % 12 + 12) % 12, flavor);
    }

    /**
     * Implements an equality check. Two chords are equivalent iff they are based on the same low pitch and have the same flavor (which is equivalent
     * to having the same 3 pitches).
     * 
     * @param other the other chord to compare this chord to
     * 
     * @return true if the two objects are equal, false otherwise
     */

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof Chord)) {
            return false;
        }

        Chord otherChord = (Chord) other;
        return this == otherChord || this.lowPitch == otherChord.lowPitch && this.flavor == otherChord.flavor;
    }

    /**
     * Implements an equality check. Two chords are considered equivalent if they are equivalent when both are normalized.
     * 
     * @param other the other chord to compare this chord to
     * 
     * @return true if the two objects are equal when they are normalized, false otherwise
     */

    public boolean equalsNormalized(Object other) {
        if (other == null || !(other instanceof Chord)) {
            return false;
        }

        // check equality without normalization first

        Chord otherChord = (Chord) other;

        if (this == otherChord || this.lowPitch == otherChord.lowPitch && this.flavor == otherChord.flavor) {
            return true;
        }

        // normalize and check

        Chord chord1 = this.normalize();
        Chord chord2 = otherChord.normalize();

        return chord1.lowPitch == chord2.lowPitch && chord1.flavor == chord2.flavor;
    }

    /**
     * Returns a string representation of this chord. If the chord has a canonical name, its canonical name is returned, otherwise a generic string
     * representation is returned.
     * 
     * @return the string representation
     */

    @Override
    public String toString() {
        String name = CODE_TO_NAME_MAP.get(code);

        if (name != null) {
            return name;
        } else {
            return lowPitch + ":" + middlePitch + ":" + highPitch;
        }
    }

    /**
     * Returns the pitch of the low note of the chord.
     * 
     * @return the pitch of the low note
     */

    public int getLowPitch() {
        return lowPitch;
    }

    /**
     * Returns the pitch of the middle note of the chord.
     * 
     * @return the pitch of the middle note
     */

    public int getMiddlePitch() {
        return middlePitch;
    }

    /**
     * Returns the pitch of the high note of the chord.
     * 
     * @return the pitch of the high note
     */

    public int getHighPitch() {
        return highPitch;
    }

    /**
     * Returns the pitch of the given chord offset. 0 will return the low pitch, 1 the middle pitch, 2 the high pitch, 3 the low pitch transposed up
     * by 1 octave, etc. Negative offsets are also supported.
     * 
     * @param offset the chord offset
     * 
     * @return the pitch
     */

    public int getPitch(int offset) {
        int p = (offset % 3 + 3) % 3;
        int octaveOffset = 12 * ((offset < 0 ? offset - 2 : offset) / 3);

        if (p == 0) {
            return octaveOffset + lowPitch;
        } else if (p == 1) {
            return octaveOffset + middlePitch;
        } else {
            return octaveOffset + highPitch;
        }
    }

    /**
     * Returns a normalized version of the chord. Any chord with first or second inversion will be converted to its counterpart without inversion. For
     * all other chords the original chord will be returned. Augmented chords cannot be normalized, because they don't have a unique root pitch.
     * 
     * @return the normalized chord
     */

    public Chord normalize() {
        InversionType type = INVERSION_TYPE_MAP.get(flavor);

        if (type == null || type == InversionType.NONE) {
            // not normalizable or already normalized
            return this;
        }

        if (type == InversionType.SECOND) {
            return rotateUp();
        } else {
            return rotateDown();
        }
    }

    /**
     * Rotates the chord up by one chord offset. This method returns a chord where the low note is transposed one octave up.
     * 
     * @return the chord rotated up by one chord offset
     */

    public Chord rotateUp() {
        return new Chord(lowPitch + 12, middlePitch, highPitch);
    }

    /**
     * Rotates the chord down by one chord offset. This method returns a chord where the high note is transposed one octave down.
     * 
     * @return the chord rotated down by one chord offset
     */

    public Chord rotateDown() {
        return new Chord(lowPitch, middlePitch, highPitch - 12);
    }

    /**
     * Returns true if the given pitch is a note that is contained in the chord, false otherwise. All involved pitches are normalized, i.e., octave
     * does not matter.
     * 
     * @param pitch the pitch
     * 
     * @return true if the pitch belongs to the chord, false otherwise
     */

    public boolean containsPitch(int pitch) {
        return ((pitch - lowPitch) % 12 + 12) % 12 == 0 || ((pitch - middlePitch) % 12 + 12) % 12 == 0 || ((pitch - highPitch) % 12 + 12) % 12 == 0;
    }

    /**
     * Returns a chord that is a rotated version of this chord whose middle pitch is as close to the middle pitch of the given other chord as
     * possible. For example, the closest version of chord "C" to chord "Am" is chord "C6".
     * 
     * @param otherChord the other chord
     * 
     * @return the closest chord
     */

    public Chord findChordClosestTo(Chord otherChord) {
        int targetPitch = otherChord.middlePitch;
        int diff = targetPitch - middlePitch;

        Chord chord1;
        int bestDiff;

        if (diff == 0) {
            // the middle pitches are already equal, so this chord is the closest chord
            return this;
        } else if (diff < 0) {
            // rotate down until we find the closest chord
            chord1 = this;
            bestDiff = -diff;
            Chord bestChord = this;

            do {
                chord1 = chord1.rotateDown();

                diff = Math.abs(targetPitch - chord1.middlePitch);

                if (diff < bestDiff) {
                    bestDiff = diff;
                    bestChord = chord1;
                }
            } while (chord1.middlePitch > targetPitch);

            return bestChord;
        } else {
            // rotate up until we find the closest chord
            chord1 = this;
            bestDiff = diff;
            Chord bestChord = this;

            do {
                chord1 = chord1.rotateUp();

                diff = Math.abs(targetPitch - chord1.middlePitch);

                if (diff < bestDiff) {
                    bestDiff = diff;
                    bestChord = chord1;
                }
            } while (chord1.middlePitch < targetPitch);

            return bestChord;
        }
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Returns the chord flavor for the given two pitch differences.
     * 
     * @param diff1 the first pitch difference (must be smaller than diff2)
     * @param diff2 the second pitch difference (must be larger than diff1)
     *
     * @return the flavor
     */

    private static int getFlavor(int diff1, int diff2) {
        return diff1 * 100 + diff2;
    }

    /**
     * Returns the chord code for the given base pitch and flavor.
     * 
     * @param basePitch the base pitch
     * @param flavor the flavor
     * @return the code
     */

    private static int getCode(int basePitch, int flavor) {
        return basePitch * 10000 + flavor;
    }

    /**
     * Parses the given chord string and returns a Chord instance that represents the chord string.
     * 
     * @param chordString the chord as a string
     * @param crossoverPitch the crossover pitch (between 1 and 12)
     * 
     * @return the Chord
     */

    public static Chord parseChord(String chordString, int crossoverPitch) {
        if (chordString == null) {
            return null;
        }

        Chord chord;

        Matcher m = Chord.GENERIC_CHORD_PATTERN.matcher(chordString);

        if (m.matches()) {
            int p1 = Integer.parseInt(m.group(1));
            int p2 = Integer.parseInt(m.group(2));
            int p3 = Integer.parseInt(m.group(3));

            chord = new Chord(p1, p2, p3);
        } else {
            chord = getChordFromName(chordString, crossoverPitch);

            if (chord == null) {
                throw new RuntimeException("Invalid chord name " + chordString);
            }
        }

        return chord;
    }

    /**
     * Looks up the chord with the given name. If the chord cannot be found, null is returned, otherwise the chord is returned.
     * 
     * @param name the chord name
     * @param crossoverPitch the crossover pitch (between 1 and 12)
     * 
     * @return the chord or null if the chord cannot be found
     */

    private static Chord getChordFromName(String name, int crossoverPitch) {
        Integer codeInteger = NAME_TO_CODE_MAP.get(name);

        if (codeInteger != null) {
            int code = codeInteger;

            int diff1 = code / 100 % 100;
            int diff2 = code % 100;
            int basePitch = code / 10000;

            if (basePitch >= crossoverPitch) {
                basePitch -= 12;
            }

            return new Chord(basePitch, basePitch + diff1, basePitch + diff2);
        } else {
            return null;
        }
    }

    /**
     * Chord template. Consists of a name fragment (e.g. "m" for minor, "" for major) and 2 pitch differences from the root pitch.
     */

    private static final class ChordTemplate {
        /** The name. */
        private String name;

        /** The first pitch difference. */
        private int diff1;

        /** The second pitch difference. */
        private int diff2;

        /**
         * Constructor.
         * 
         * @param name the name
         * @param diff1 the first pitch difference
         * @param diff2 the second pitch difference
         */

        private ChordTemplate(String name, int diff1, int diff2) {
            this.name = name;
            this.diff1 = diff1;
            this.diff2 = diff2;
        }
    }
}
