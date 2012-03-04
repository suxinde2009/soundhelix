package com.soundhelix.misc;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.soundhelix.util.NoteUtils;

/**
 * Defines a chord. A chord is immutable and consists of 3 different pitches, which cannot span more than 11 halftones.
 * A chord may be one of the common type major, minor, diminished or augmented (all of them in all three inversion
 * flavors) or can have any other pitch combination. All chords can be rotated up or down. Rotating a chord up means
 * replacing the chord's low pitch with the same pitch transposed one octave up. Rotating a chord down means replacing
 * the chord's high pitch with the same pitch transposed one octave down. All common type chords (except for augmented
 * chords) can be normalized, which means that the chords are rotated up or down so that their low pitch equals the
 * root pitch of the chord (e.g., "C4" and "C6" are normalized to "C"), if that is not already the case. Augmented
 * chords cannot be normalized, because they don't have a unique root pitch (e.g., the chords "Caug", "Eaug6" and
 * "G#aug4" are not distinguishable if you look at their pitches).
 * 
 * The low and the high pitch must not be farther apart than 11 halftones.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class Chord {
    /** The number of chord flavors. */
    private static final int CHORD_FLAVORS = 12;
    
    /** Code for the major chord. */
    private static final int MAJOR = 407;
    
    /** Code for the major6 chord. */
    private static final int MAJOR6 = 509;

    /** Code for the major4 chord. */
    private static final int MAJOR4 = 308;
    
    /** Code for the minor chord. */
    private static final int MINOR = 307;

    /** Code for the minor6 chord. */
    private static final int MINOR6 = 508;
    
    /** Code for the minor4 chord. */
    private static final int MINOR4 = 409;
    
    /** Code for the diminished chord. */
    private static final int DIM = 306;
    
    /** Code for the diminished6 chord. */
    private static final int DIM6 = 609;
    
    /** Code for the diminished4 chord. */
    private static final int DIM4 = 309;
    
    /** Code for the augmented chord. */
    private static final int AUG = 408;
    
    /** Code for the augmented 6 chord (same as augmented). */
    private static final int AUG6 = 408;
    
    /** Code for the augmented 4 chord (same as augmented). */
    private static final int AUG4 = 408;

    /** Pattern for arbitrary chords ("pitch1:pitch2:pitch3"). */
    private static final Pattern GENERIC_CHORD_PATTERN = Pattern.compile("^(-?\\d+):(-?\\d+):(-?\\d+)$");
    
    /** Maps from chord names to chord codes. */
    private static final Map<String, Integer> NAME_TO_CODE_MAP = new LinkedHashMap<String, Integer>(12 * CHORD_FLAVORS);
    
    /** Maps from chord codes to chord names. */
    private static final Map<Integer, String> CODE_TO_NAME_MAP = new HashMap<Integer, String>(12 * CHORD_FLAVORS);

    /** The low pitch of the chord. */
    private final int lowPitch;

    /** The middle pitch of the chord. */
    private final int middlePitch;

    /** The high pitch of the chord. */
    private final int highPitch;

    /**
     * The flavor of the chord (encoded as the difference of the middle pitch and the low pitch times hundred plus the
     * difference of the high pitch and the low pitch). The flavor is independent of the low pitch.
     */
    private final int flavor;

    /**
     * The integer that uniquely identifies the chords, including the chord's low pitch, but the octave is normalized.
     */
    private final int code;

    static {
        for (int i = 0; i <= 12; i++) {
            NAME_TO_CODE_MAP.put(NoteUtils.getNoteName(i).toUpperCase(), i * 10000 + MAJOR);
            NAME_TO_CODE_MAP.put(NoteUtils.getNoteName(i + 5).toUpperCase() + "6", i * 10000 + MAJOR6);
            NAME_TO_CODE_MAP.put(NoteUtils.getNoteName(i - 4).toUpperCase() + "4", i * 10000 + MAJOR4);
            NAME_TO_CODE_MAP.put(NoteUtils.getNoteName(i).toUpperCase() + "m", i * 10000 + MINOR);
            NAME_TO_CODE_MAP.put(NoteUtils.getNoteName(i + 5).toUpperCase() + "m6", i * 10000 + MINOR6);
            NAME_TO_CODE_MAP.put(NoteUtils.getNoteName(i - 3).toUpperCase() + "m4", i * 10000 + MINOR4);
            NAME_TO_CODE_MAP.put(NoteUtils.getNoteName(i).toUpperCase() + "dim", i * 10000 + DIM);
            NAME_TO_CODE_MAP.put(NoteUtils.getNoteName(i + 6).toUpperCase() + "dim6", i * 10000 + DIM6);
            NAME_TO_CODE_MAP.put(NoteUtils.getNoteName(i - 4).toUpperCase() + "dim4", i * 10000 + DIM4);
            
            // use "aug" last for the reverse mapping; we want 10000 + AUG to point to ...aug, rather than
            // to ...aug4 or ...aug6; we are using a LinkedHashMap, so order matters
            NAME_TO_CODE_MAP.put(NoteUtils.getNoteName(i + 4).toUpperCase() + "aug6", i * 10000 + AUG6);
            NAME_TO_CODE_MAP.put(NoteUtils.getNoteName(i - 4).toUpperCase() + "aug4", i * 10000 + AUG4);
            NAME_TO_CODE_MAP.put(NoteUtils.getNoteName(i).toUpperCase() + "aug", i * 10000 + AUG);
        }
        
        // create the reverse version of the upper map
        
        for (Map.Entry<String, Integer> entry : NAME_TO_CODE_MAP.entrySet()) {
            CODE_TO_NAME_MAP.put(entry.getValue(), entry.getKey());
        }
    }
    
    /**
     * Instantiates a chord. Pitches are sorted by their value and are used as the low, middle and high pitch,
     * respectively.
     * 
     * @param pitch1 the first pitch
     * @param pitch2 the second pitch
     * @param pitch3 the third pitch
     */
    
    public Chord(int pitch1, int pitch2, int pitch3) {
        // sort the three pitches by their value
        
        int min = Math.min(pitch1, Math.min(pitch2, pitch3));
        int max = Math.max(pitch1, Math.max(pitch2, pitch3));

        lowPitch = min;
        middlePitch = min == pitch1 ? (max == pitch2 ? pitch3 : pitch2)
                      : (max == pitch1 ? (min == pitch2 ? pitch3 : pitch2) : pitch1);
        highPitch = max;
        
        if (lowPitch == middlePitch || middlePitch == highPitch) {
            throw new RuntimeException("Duplicate pitches in chord " + this);
        }

        if (highPitch - lowPitch >= 12) {
            throw new RuntimeException("High and low pitch are more than 11 halftones apart in chord " + this);
        }
        
        flavor = (middlePitch - lowPitch) * 100 + (highPitch - lowPitch);        
        code = (((lowPitch % 12) + 12) % 12) * 10000 + flavor;
    }
    
    /**
     * Returns true iff this chord is a major chord.
     * 
     * @return true iff the chord is a major chord
     */
    
    public boolean isMajor() {
        return flavor == MAJOR || flavor == MAJOR6 || flavor == MAJOR4;
    }

    /**
     * Returns true iff this chord is a minor chord.
     * 
     * @return true iff the chord is a minor chord
     */
    

    public boolean isMinor() {
        return flavor == MINOR || flavor == MINOR6 || flavor == MINOR4;
    }
    
    /**
     * Returns true iff this chord is a diminished chord.
     *
     * @return true iff this chord is a diminished chord.
     */
    
    public boolean isDiminished() {
        return flavor  == DIM || flavor == DIM6 || flavor == DIM4;
    }

    /**
     * Returns true iff this chord is an augmented chord.
     *
     * @return true iff this chord is an augmented chord.
     */

    public boolean isAugmented() {
        // all types (no inversion, first inversion and second inversion) are equal
        return flavor == AUG;
    }
    
    /**
     * Implements an equality check. Two chords are equivalent iff they are based on the same low pitch and have
     * the same flavor (which is equivalent to having the same 3 pitches).
     * 
     * @param other the other chord to compare this chord to
     * 
     * @return true if the two objects are equal, false otherwise
     */
    
    public boolean equals(Object other) {
        if (other == null || !(other instanceof Chord)) {
            return false;
        }
        
        Chord otherChord = (Chord) other;
        return this == otherChord || this.lowPitch == otherChord.lowPitch && this.flavor == otherChord.flavor;
    }
    
    /**
     * Implements an equality check. Two chords are equivalent if they are equivalent when both are normalized.
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
     * Returns a string representation of this chord. If the chord has a canonical name, its canonical name is
     * returned, otherwise a generic string representation is returned.
     * 
     * @return the string represenation
     */
    
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
     * Returns the pitch of the given chord offset. 0 will return the low pitch, 1 the middle pitch, 2 the high pitch,
     * 3 the low pitch transposed up by 1 octave, etc. Negative offsets are also supported.
     *
     * @param offset the chord offset
     *
     * @return the pitch
     */
    
    public int getPitch(int offset) {
        int p = offset % 3;
        int octaveOffset = 12 * (offset / 3);
        
        if (p == 0) {
            return octaveOffset + lowPitch;
        } else if (p == 1) {
            return octaveOffset + middlePitch;
        } else {
            return octaveOffset + highPitch;
        }
    }

    /**
     * Normalizes the chord. Any major, minor or diminished chord with first or second inversion will be converted to
     * its counterpart without inversion. For all other chords the original chord will be returned. Augmented chords
     * cannot be normalized, because they don't have a unique root pitch.
     * 
     * @return the chord
     */    
    
    public Chord normalize() {
        switch (flavor) {
            case MAJOR6:
                return new Chord(middlePitch, middlePitch + 4, middlePitch + 7);
            case MAJOR4: 
                return new Chord(highPitch - 12, highPitch - 8, highPitch - 5);
            case MINOR6: 
                return new Chord(middlePitch, middlePitch + 3, middlePitch + 7);
            case MINOR4:
                return new Chord(highPitch - 12, highPitch - 9, highPitch - 5);
            case DIM6:   
                return new Chord(middlePitch, middlePitch + 3, middlePitch + 6);
            case DIM4:   
                return new Chord(highPitch - 12, highPitch - 9, highPitch - 6);
            default:
                // already normalized or not normalizable
                return this;
        }
    }
    
    /**
     * Rotates the chord up by one chord offset. Effectively, this method creates a copy of this chord where the low
     * note is transposed one octave up.
     * 
     * @return the chord rotated up by one chord offset
     */
    
    public Chord rotateUp() {
        return new Chord(getLowPitch() + 12, getMiddlePitch(), getHighPitch());
    }
    
    /**
     * Rotates the chord down by one chord offset. Effectively, this method creates a copy of this chord where the high
     * note is transposed one octave down.
     * 
     * @return the chord rotated down by one chord offset
     */
    
    public Chord rotateDown() {
        return new Chord(getLowPitch(), getMiddlePitch(), getHighPitch() - 12);
    }
    
    /**
     * Returns true if the given pitch is a note that is contained in the chord, false otherwise. All involved pitches
     * are normalized, i.e., octave does not matter.
     * 
     * @param pitch the pitch
     * 
     * @return true if the pitch belongs to the chord, false otherwise
     */
    
    public boolean containsPitch(int pitch) {
        return ((((pitch - getLowPitch()) % 12) + 12) % 12) == 0
            || ((((pitch - getMiddlePitch()) % 12) + 12) % 12) == 0
            || ((((pitch - getHighPitch()) % 12) + 12) % 12) == 0;
    }
    
    /**
     * Returns a chord that is a rotated version of this chord whose middle pitch is as close to the middle pitch of
     * the given other chord as possible. For example, the closest version of chord "C" to chord "Am" is chord "C6".
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
     * Parses the given chord string and returns a Chord instance that represents the chord string.
     * 
     * @param chordString the chord as a string 
     *
     * @return the Chord
     */
        
    public static Chord parseChord(String chordString) {
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
            chord = getChordFromName(chordString);
            
            if (chord == null) {
                throw new RuntimeException("Invalid chord name " + chordString);
            }
        }
        
        return chord;        
    }

    /**
     * Looks up the chord with the given name. If the chord cannot be found, null is returned, otherwise the chord
     * is returned.
     * 
     * @param name the chord name
     *
     * @return the chord or null if the chord cannot be found
     */
    
    private static Chord getChordFromName(String name) {
        Integer codeInteger = NAME_TO_CODE_MAP.get(name);
        
        if (codeInteger != null) {
            int code = codeInteger;
            
            int diff1 = (code / 100) % 100;
            int diff2 = code % 100;
            int pitch = code / 10000;
 
            if (pitch > 2) {
                pitch -= 12;
            }
            
            return new Chord(pitch, pitch + diff1, pitch + diff2);
        } else {
            return null;
        }
    }
}
