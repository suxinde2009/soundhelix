package com.soundhelix.harmonyengine;

import java.util.Hashtable;

import org.apache.log4j.Logger;

import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Structure;
import com.soundhelix.misc.XMLConfigurable;

/**
 * Represents an abstract generator for song harmonies. Normally, song harmonies are a sequence
 * of chords (often with the same length) with a certain pattern.
 * The song's complete chord sequence can be divided into repeating sections.
 * For example, a chord sequence might consist of a chord section for
 * a verse and a (possibly longer or shorter) chord section for a refrain,
 * which could each be repeated a couple of times. These chord sections
 * splits the chord sequence into a number of logical parts.
 * 
 * For each tick, this generator must return the current chord, the remaining
 * number of ticks this chord will be played before a chord change occurs (or the
 * song ends) and the number of ticks before a new chord section begins (or the
 * song ends).
 * 
 * The methods must always return consistent (i.e., non-contradictory) results.
 * In addition, the results must be persistent, i.e., a given instance must always
 * return the same results for each method parameter.
 * 
 * Consider a simple chord section of "Am F F Am" with 16 ticks each. If this
 * section is used twice ("Am F F Am Am F F Am"), the two consecutive F and Am
 * chords must be merged together, even though a new chord section begins between
 * the two consecutive Am chords. If it is important for the caller to see chord
 * changes as well as chord section changes, simply use Math.min(getChordTicks(tick),
 * getChordSectionTicks(tick)).
 * 
 * This should result in the following method behavior:
 * 
 * tick   getChord()  getChordTicks()  getChordSequenceTicks()
 * 
 *   0            Am               16                       64
 *  16             F               32                       48
 *  32             F               16                       32
 *  48            Am               32                       16
 *  64            Am               16                       64
 *  80             F               32                       48
 *  96             F               16                       32
 * 112            Am               16                       16
 * 128     undefined        undefined                undefined
 * 
 * Note that each tick (not only multiples of 16 as shown here) must
 * return correct results. The undefined result is due to the fact that
 * an invalid tick is used (128 is the end of the song).
 * 
 * It is very important to get the method behavior correct. The HarmonyEngine
 * can be sanity-checked using checkSanitiy().
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public abstract class HarmonyEngine implements XMLConfigurable {
	protected final Logger logger;

	protected Structure structure;	
	private int chordSections = -1;
	private int distinctChordSections = -1;
	
	public HarmonyEngine() {
		logger = Logger.getLogger(getClass());
	}
	
	public void setSongStructure(Structure structure) {
		this.structure = structure;
	}
	
    /**
     * Returns the chord to use at the specified point in time.
     * Within the valid tick interval this must be non-null (each tick must define a chord).
     * 
     * @param tick the tick
     * 
     * @return the Chord
     */
	
	public abstract Chord getChord(int tick);
	
	/**
	 * Returns the number of ticks the current chord will
	 * be played from the given tick position before the chord will
	 * change or the song will end (whichever happens first).
	 * This requirement is strict, i.e., the chord must not change
	 * before the returned number of ticks and it must change directly
	 * afterwards or the song must end. For a valid tick parameter,
	 * the return value must always be positive.
	 * 
	 * @param tick the tick
	 * 
	 * @return the number of ticks before the next chord change
	 */
	
	public abstract int getChordTicks(int tick);

	/**
	 * Returns the number of ticks the current chord section will be played from
	 * the given tick position before the next chord section will begin or the
	 * song will end. This method can be used to check when special processing
	 * (like adding rhythm fill-ins) can be done. For standard chord sections,
	 * the total length of the chord section should be used. For a valid tick
	 * parameter, the return value must always be positive.
	 * 
	 * @param tick the tick number
	 * 
	 * @return the number of ticks before the next chord section begins or the song will end
	 */
	
	public abstract int getChordSectionTicks(int tick);
	
	/**
	 * Dumps all chords and their lengths in ticks.
	 */
	
	public void dumpChords() {
		int tick = 0;
         
        while(tick < structure.getTicks()) {
        	Chord chord = getChord(tick);
        	int len = getChordTicks(tick);

        	if(tick > 0) {
        		System.out.print(","+chord+"/"+len);
        	} else {
                System.out.print(chord+"/"+len);
        	}
        	
         	tick += len;
        }
        
        System.out.println();
	}
	
	/**
	 * Returns the total number of chord sections. The return
	 * value, once calculated, is cached for further method
	 * calls.
	 * 
	 * @return the total number of chord sections
	 */
	
	public int getChordSectionCount() {
		if(chordSections >= 0) {
			return chordSections;
		} else {
			// skip through the chord sections
			// and count how many are available
			
			int sections = 0;
			int tick = 0;

			while(tick < structure.getTicks()) {
				tick += getChordSectionTicks(tick);
				sections++;
			}

			this.chordSections = sections;

			return sections;
		}
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
	
	public String getChordSectionString(int tick) {
		StringBuilder sb = new StringBuilder();
		
		if(tick < 0 || tick >= structure.getTicks()) {
			return null;
		}
		
		int tickEnd = tick+getChordSectionTicks(tick);

		while(tick < tickEnd) {
			Chord chord = getChord(tick);
			int len = getChordTicks(tick);
			
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
	
	public int getDistinctChordSectionCount() {
		if(distinctChordSections >= 0) {
			return distinctChordSections;
		} else {
			Hashtable<String,Boolean> ht = new Hashtable<String,Boolean>();
			int tick = 0;

			while(tick < structure.getTicks()) {
				ht.put(getChordSectionString(tick),true);
				tick += getChordSectionTicks(tick);
			}

			distinctChordSections = ht.size();
			
			return distinctChordSections;
		}
	}
	
	/**
	 * Checks if the 3 abstract methods return consistent and correct
	 * results. In case of a detected problem, a RuntimeException will
	 * be thrown.
	 * 
	 * @throws RuntimeException in case of an error
	 */
	
    public void checkSanity() {
         
        Chord lastChord = null;
        int lastChordTicks = 1;
        int lastChordSectionTicks = 1;
 
        int ticks = structure.getTicks();
        
        for(int tick=0;tick<ticks;tick++) {
        	Chord chord = getChord(tick);
        	
        	if(chord == null) {
        		throw(new RuntimeException("Null chord returned at tick "+tick));
        	}

        	int chordTicks = getChordTicks(tick);
        	
        	if(chordTicks <= 0) {
        		throw(new RuntimeException("Chord ticks <= 0 at tick "+tick));
        	}

        	if(lastChordTicks > 1 && chordTicks != lastChordTicks-1) {
        		throw(new RuntimeException("Chord tick not decremented at "+tick));        		
        	}
        	
        	int chordSectionTicks = getChordSectionTicks(tick);

        	if(chordSectionTicks <= 0) {
        		throw(new RuntimeException("Chord section ticks <= 0 at tick "+tick));
        	}

        	if(lastChordSectionTicks > 1 && chordSectionTicks != lastChordSectionTicks-1) {
        		throw(new RuntimeException("Chord section tick not decremented at "+tick));        		
        	}

        	if(!chord.equals(lastChord) && lastChordTicks != 1) {
        		throw(new RuntimeException("Chord changes unexpectedly from "+lastChord+" to "+chord+" at tick "+tick));
        	}
        	
        	lastChord = chord;
        	lastChordTicks = chordTicks;
        	lastChordSectionTicks = chordSectionTicks;
        }
        
        if(lastChordTicks != 1) {
        	throw(new RuntimeException("Chord ticks is not 1 at last tick"));
        }
        
        if(lastChordSectionTicks != 1) {
        	throw(new RuntimeException("Chord section ticks is not 1 at last tick"));
        }
    }
}
