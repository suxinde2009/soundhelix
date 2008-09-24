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
	
	private static Hashtable<String,Integer> h = new Hashtable<String,Integer>();
	
	static {
		for(int i=0;i<12;i++) {
			h.put(noteNames[i],i);
		}
	}
	
	private NoteUtils() {}

	public static String getNoteName(int pitch) {
		int offset = (pitch >= 0 ? pitch%12 : ((pitch%12)+12)%12);
		return noteNames[offset];
	}
	
	public static int getNotePitch(String name) {
		return h.get(name.toLowerCase());
	}
	
    /**
     * Returns true iff the given pitch is on the C/Am scale
     * (i.e., a "white key" on the piano keyboard).
     * 
     * @param pitch the pitch to check
     * 
     * @return true or false
     */
    
    public static boolean isOnScale(int pitch) {
    	return scaleTable[((pitch%12)+12)%12];
    }
}
