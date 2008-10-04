package com.soundhelix.util;

import java.util.Hashtable;

/**
 * Implements some static methods for converting notes to pitches and vice versa.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class NoteUtils {
	// list of notes which are on the C/Am scale, i.e., which
	// form the white keys on the piano keyboard, starting with C
	private static final boolean[] scaleTable = new boolean[] {true,false,true,false,true,true,false,true,false,true,false,true};

	private static String noteNames[] = {
		"c","c#","d","d#","e","f","f#","g","g#","a","a#","b"
	};
	
	// maps note names to pitches
	private static Hashtable<String,Integer> h = new Hashtable<String,Integer>();
	
	static {
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
    	return scaleTable[((pitch%12)+12)%12];
    }
}
