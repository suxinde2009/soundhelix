package com.soundhelix.util;

import java.util.HashMap;
import java.util.Map;

import com.soundhelix.misc.Chord;

/**
 * Implements some static methods for converting notes to pitches and vice versa. Note names can be provided either using sharp or flat notation (e.g.
 * "G#" and "Ab" are equal).
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public final class NoteUtils {
    /** Array of notes that are on the C/Am scale, starting with C. */
    private static final boolean[] SCALE_TABLE = new boolean[] {true, false, true, false, true, true, false, true, false, true, false, true};

    /** The list of all note names (using sharp), starting with C. */
    private static String[] sharpNoteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    /** The list of all note names (using flat), starting with C. */
    private static String[] flatNoteNames = {"C", "Db", "D", "Eb", "E", "F", "Ab", "G", "Gb", "A", "Bb", "B"};

    /** Maps note names to normalized pitches. */
    private static Map<String, Integer> noteMap = new HashMap<String, Integer>();

    static {
        // build a reverse lookup table for sharpNoteNames and flatNoteNames

        for (int i = 0; i < 12; i++) {
            noteMap.put(sharpNoteNames[i], i);
        }

        for (int i = 0; i < 12; i++) {
            noteMap.put(flatNoteNames[i], i);
        }
    }

    /**
     * Private constructor.
     */

    private NoteUtils() {
    }

    /**
     * Returns the sharp note name of the given pitch. The pitch is normalized first (between 0 and 11).
     * 
     * @param pitch the pitch
     * 
     * @return the pitch name
     */

    public static String getSharpNoteName(int pitch) {
        return sharpNoteNames[(pitch % 12 + 12) % 12];
    }

    /**
     * Returns the flat note name of the given pitch. The pitch is normalized first (between 0 and 11).
     * 
     * @param pitch the pitch
     * 
     * @return the pitch name
     */

    public static String getFlatNoteName(int pitch) {
        return flatNoteNames[(pitch % 12 + 12) % 12];
    }

    /**
     * Returns the note pitch of the given note (between 0 and 11). If the note is invalid, Integer.MIN_VALUE is returned.
     * 
     * @param name the note name
     * 
     * @return the note pitch or Integer.MIN_VALUE
     */

    public static int getNotePitch(String name) {
        if (name == null) {
            return Integer.MIN_VALUE;
        }

        Integer pitch = noteMap.get(name);

        if (pitch == null) {
            return Integer.MIN_VALUE;
        } else {
            return pitch;
        }
    }

    /**
     * Returns true iff the given pitch is on the C/Am scale (i.e., a white key on the piano keyboard).
     * 
     * @param pitch the pitch to check
     * 
     * @return true or false
     */

    public static boolean isOnScale(int pitch) {
        return SCALE_TABLE[(pitch % 12 + 12) % 12];
    }

    /**
     * Returns a transition pitch between the chord and the next chord, which is based on the base pitches of the two chords. If the next chord is
     * null, the pitch of the first chord is used. If the pitch difference of the two chords is 2, the halftone in between is returned. If the pitch
     * difference of the two chords is one or zero, the first pitch is returned. Otherwise, a pitch between the two pitches which is on the C/Am scale
     * is returned.
     * 
     * @param chord the current chord
     * @param nextChord the next chord (or null)
     * 
     * @return a transition pitch
     */

    public static int getTransitionPitch(Chord chord, Chord nextChord) {
        if (nextChord == null) {
            // next chord is undefined, just return the current pitch
            return chord.getLowPitch();
        }

        int pitch1 = chord.getLowPitch();
        int pitch2 = nextChord.getLowPitch();

        int diff = pitch2 - pitch1;
        int absdiff = Math.abs(diff);

        if (diff == 0) {
            // chords are the same
            return pitch1;
        } else if (absdiff == 2) {
            // pitch difference is one tone,
            // use the halftone in between
            return (pitch1 + pitch2) / 2;
        } else if (absdiff == 1) {
            // pitch difference is one halftone
            // use the current pitch
            return pitch1;
        } else if (diff > 0) {
            // we have a pitch difference of at least 3 halftones up
            pitch1 += Math.min(0, absdiff / 2 - 1);
            do {
                pitch1++;
            } while (!isOnScale(pitch1));
            return pitch1;
        } else {
            // we have a pitch difference of at least 3 halftones down
            pitch1 -= Math.min(0, absdiff / 2 - 1);
            do {
                pitch1--;
            } while (!isOnScale(pitch1));
            return pitch1;
        }
    }
}
