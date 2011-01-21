package com.soundhelix.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import com.soundhelix.harmonyengine.HarmonyEngine;
import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Structure;

/**
 * Implements some static methods for HarmonyEngine stuff.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public final class HarmonyEngineUtils {
	private HarmonyEngineUtils() {
	}
	
	/**
	 * Returns the total number of chord sections.
	 * 
	 * @param structure the structure
	 * 
	 * @return the total number of chord sections
	 */
	
	public static int getChordSectionCount(Structure structure) {
		HarmonyEngine harmonyEngine = structure.getHarmonyEngine();
		int ticks = structure.getTicks();
		
		// skip through the chord sections
		// and count how many are available

		int sections = 0;

	    for (int tick = 0; tick < ticks; tick += harmonyEngine.getChordSectionTicks(tick)) {
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
	 * @param structure the structure
	 * @param tick the starting tick
	 * 
	 * @return a chord section string (or null if the tick parameter is invalid)
	 */
	
	public static String getChordSectionString(Structure structure, int tick) {
		if (tick < 0 || tick >= structure.getTicks()) {
			return null;
		}
		
		StringBuilder sb = new StringBuilder();
		HarmonyEngine harmonyEngine = structure.getHarmonyEngine();
		
		int tickEnd = tick + harmonyEngine.getChordSectionTicks(tick);

		while (tick < tickEnd) {
			Chord chord = harmonyEngine.getChord(tick);
			int len = harmonyEngine.getChordTicks(tick);
			
			if (sb.length() > 0) {
				sb.append(',');
			}
			
			sb.append(chord.getShortName());
			sb.append('/');
			
			// the chord section might end before a chord change occurs
			// therefore we need to use Math.min()
			sb.append(Math.min(tickEnd - tick, len));

			tick += len;
		}
		
		return sb.toString();
	}
	
	/**
	 * Returns the number of distinct chord sections.
	 * 
	 * @param structure the structure
	 * 
	 * @return the number of distinct chord sections
	 */
	
	public static int getDistinctChordSectionCount(Structure structure) {
		HarmonyEngine harmonyEngine = structure.getHarmonyEngine();
		int ticks = structure.getTicks();
		
		Map<String, Boolean> ht = new HashMap<String, Boolean>();

	    for (int tick = 0; tick < ticks; tick += harmonyEngine.getChordSectionTicks(tick)) {
			ht.put(getChordSectionString(structure, tick), true);
		}

		return ht.size();
	}
	
	/**
	 * Returns a list of start ticks for all chord sections, in ascending order.
	 * The first chord section will always start at tick 0. The number of list
	 * entries always equals getChordSectionCount(structure).
	 * 
	 * @param structure the structure
	 * 
	 * @return a list of start ticks for all chord sections
	 */
		
	public static List<Integer> getChordSectionStartTicks(Structure structure) {
		HarmonyEngine harmonyEngine = structure.getHarmonyEngine();
		List<Integer> list = new ArrayList<Integer>();

	    int ticks = structure.getTicks();
	    
	    for (int tick = 0; tick < ticks; tick += harmonyEngine.getChordSectionTicks(tick)) {
	        list.add(tick);
	    }
	    
	    return list;
	}
}
