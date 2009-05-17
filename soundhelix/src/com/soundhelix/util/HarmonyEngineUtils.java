package com.soundhelix.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.soundhelix.harmonyengine.HarmonyEngine;
import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Structure;

/**
 * Implements some static methods for random numbers.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class HarmonyEngineUtils {
	private HarmonyEngineUtils() {}
	
	/**
	 * Returns the total number of chord sections. The return
	 * value, once calculated, is cached for further method
	 * calls.
	 * 
	 * @return the total number of chord sections
	 */
	
	public static int getChordSectionCount(Structure structure) {
		HarmonyEngine harmonyEngine = structure.getHarmonyEngine();
		
		// skip through the chord sections
		// and count how many are available

		int sections = 0;
		int tick = 0;

		while(tick < structure.getTicks()) {
			tick += harmonyEngine.getChordSectionTicks(tick);
			sections++;
		}

		return sections;
	}
	
	/**
	 * Returns a string specifying the chord section that starts
	 * at the specified tick, which is a comma-separated list of
	 * chords and tick lengths. This is done by listing all the
	 * chords and their lengths until the next chord section starts
	 * or the song ends.
	 * 
	 * @param tick
	 * 
	 * @return a chord section string (or null if the tick parameter is invalid)
	 */
	
	public static String getChordSectionString(Structure structure,int tick) {
		StringBuilder sb = new StringBuilder();
		HarmonyEngine harmonyEngine = structure.getHarmonyEngine();
		
		if(tick < 0 || tick >= structure.getTicks()) {
			return null;
		}
		
		int tickEnd = tick+harmonyEngine.getChordSectionTicks(tick);

		while(tick < tickEnd) {
			Chord chord = harmonyEngine.getChord(tick);
			int len = harmonyEngine.getChordTicks(tick);
			
			if(sb.length() > 0) {
				sb.append(',');
			}
			
			sb.append(chord.getShortName());
			sb.append('/');
			
			// the chord section might end before a chord change occurs
			// therefore we need to use Math.min()
			sb.append(Math.min(tickEnd-tick,len));

			tick += len;
		}
		
		return sb.toString();
	}
	
	/**
	 * Returns the number of distinct chord sections. The return
	 * value, once calculated, is cached for further method
	 * calls.
	 * 
	 * @return the number of distinct chord sections
	 */
	
	public static int getDistinctChordSectionCount(Structure structure) {
		HarmonyEngine harmonyEngine = structure.getHarmonyEngine();
		
		Hashtable<String,Boolean> ht = new Hashtable<String,Boolean>();
		int tick = 0;

		while(tick < structure.getTicks()) {
			ht.put(getChordSectionString(structure,tick),true);
			tick += harmonyEngine.getChordSectionTicks(tick);
		}

		return ht.size();
	}
	
	/**
	 * Returns a list of start ticks for all chord sections, in ascending order.
	 * The first chord section will always start at tick 0. The number of list
	 * entries always equals getChordSectionCount().
	 * 
	 * @return a list of start ticks for all chord sections
	 */
		
	public static List<Integer> getChordSectionStartTicks(Structure structure) {
		HarmonyEngine harmonyEngine = structure.getHarmonyEngine();
		ArrayList<Integer> list = new ArrayList<Integer>(20);

	    int ticks = structure.getTicks();
	    
	    for(int tick=0;tick<ticks;tick += harmonyEngine.getChordSectionTicks(tick)) {
	        list.add(tick);
	    }
	    
	    return list;
	}
}
