package com.soundhelix.util;

import java.util.Hashtable;

import com.soundhelix.misc.Chord;

/**
 * Implements some static methods for converting notes to pitches and vice versa.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public final class NoteUtils {
	// list of notes which are on the C/Am scale, i.e., which
	// form the white keys on the piano keyboard, starting with c
	// this equals all even indexes < 5 and all odd indexes >= 5
	private static final boolean[] SCALE_TABLE = new boolean[] {
		true,false,true,false,true,true,false,true,false,true,false,true};

	/** The list of all note names, starting with c. */
	private static String[] noteNames = {
		"c","c#","d","d#","e","f","f#","g","g#","a","a#","b"
	};
	
	// maps note names to normalized pitches
	private static Hashtable<String,Integer> h = new Hashtable<String,Integer>();
	
	static {
		// build a reverse lookup table for noteNames
		for(int i=0;i<12;i++) {
			h.put(noteNames[i],i);
		}
	}
	
	private NoteUtils() {}

	/**
	 * Returns the note name of the given pitch in lower-case. The pitch
	 * is normalized first (between 0 and 11).
	 *
	 * @param pitch the pitch
	 * 
	 * @return the pitch name
	 */
	
	public static String getNoteName(int pitch) {
		return noteNames[((pitch%12)+12)%12];
	}
	
	/**
	 * Returns the note pitch of the given note (between
	 * 0 and 11), ignoring case. If the note is invalid,
     * Integer.MIN_VALUE is returned.
	 * 
	 * @param name the note name
	 *
	 * @return the note pitch or Integer.MIN_VALUE
	 */
	
	public static int getNotePitch(String name) {
		if(name == null) {
			return Integer.MIN_VALUE;
		}
	
		Integer pitch = h.get(name.toLowerCase());
		
		if(pitch == null) {
			return Integer.MIN_VALUE;
		} else {
			return pitch;
		}
	}
	
    /**
     * Returns true iff the given pitch is on the C/Am scale
     * (i.e., a white key on the piano keyboard).
     * 
     * @param pitch the pitch to check
     * 
     * @return true or false
     */
    
    public static boolean isOnScale(int pitch) {
    	return SCALE_TABLE[((pitch%12)+12)%12];
    }

	/**
	 * Returns a transition pitch between the chord and the next chord,
	 * which is based on the base pitches of the two chords.
	 * If the next chord is null, the pitch of the first chord is used.
	 * If the pitch difference of the two chords is 2, the halftone in
	 * between is returned. If the pitch difference of the two chords
	 * is one or zero, the first pitch is returned. Otherwise, a pitch
	 * between the two pitches which is on the C/Am scale is returned.
	 * 
	 * @param chord the current chord
	 * @param nextChord the next chord (or null)
	 * 
	 * @return a transition pitch
	 */
	
	public static int getTransitionPitch(Chord chord,Chord nextChord) {
		if(nextChord == null) {
			// next chord is undefined, just return the current pitch
			return chord.getPitch();
		}
		
		int pitch1 = chord.getPitch();
		int pitch2 = nextChord.getPitch();
		
		int diff = pitch2-pitch1;
		int absdiff = Math.abs(diff);
		
		if(diff == 0) {
			// chords are the same
			return pitch1;
		} else if(absdiff == 2) {
			// pitch difference is one tone,
			// use the halftone in between
			return((pitch1+pitch2)/2);
	   	} else if(absdiff == 1) {
			// pitch difference is one halftone
			// use the current pitch
			return(pitch1);
		} else if(diff > 0) {
			// we have a pitch difference of at least 3 halftones up
			pitch1 += Math.min(0,absdiff/2-1);
			do {
				pitch1++;
			} while(!isOnScale(pitch1));
		   	return pitch1;
		} else {
			// we have a pitch difference of at least 3 halftones down
			pitch1 -= Math.min(0,absdiff/2-1);
			do {
				pitch1--;
			} while(!isOnScale(pitch1));
			return pitch1;
		}
	}
}
