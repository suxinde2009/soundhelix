package com.soundhelix.harmonyengine;

import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Chord.ChordType;
import com.soundhelix.util.NoteUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a flexible HarmonyEngine based on user-specified patterns. The patterns consist
 * of chord patterns and random table patterns. One of the given
 * chord patterns is chosen at random. A chord pattern consists of comma-separated combinations of chords
 * and lengths in beats (separated by a slash). A chord is either a simple chord name ("C" for C major, "Am" for A minor, etc.),
 * a random table number (starting from 0), a random table number (starting from 0) with a negative backreference (e.g.,
 * "0!0") to an already generated chord at an earlier position or it can be a positive backreference to an already
 * generated chord at an earlier position ("$0" for first chord, "$1" for second chord, etc.). If a random table number
 * is given, a chord is randomly chosen from that table. Chord random tables a comma-separated lists of chord names
 * (e.g., "Am,G,F,Em,Dm"), they are numbered starting from 0. For example, the chord pattern "Am/4,0/4,0!1/4,$1/4"
 * means "A minor for 4 beats, a random chord from random table 0 for 4 beats, a random chord from random table 0 but
 * not the same as the one from position 1 for 4 beats and the second chord again for 4 beats" and could result in
 * the chord sequence "Am/4,F/4,G/4,F/4" (given suitable random tables). Normally, each chord pattern is an individual
 * chord section. A pattern can be split into two or more chord sections by using "+" signs directly before a
 * chord/length combination (e.g., "Am/4,F/4,G/4,C/4,+Am/4,F/4,G/4,Em/4").
 *
 * <br><br>
 * <b>XML-Configuration</b>
 * <table border=1>
 * <tr><th>Tag</th> <th>#</th> <th>Example</th> <th>Description</th> <th>Required</th>
 * <tr><td><code>chordPattern</code></td> <td>+</td> <td><code>Am/4,1/4,1/4,2/4</code></td> <td>Sets the chord patterns to use. One of the patterns is chosen at random.</td> <td>yes</td>
 * <tr><td><code>chordRandomTable</code></td> <td>*</td> <td><code>C,Am,G,F,Em,Dm</code></td> <td>Adds a random table. Random tables can be accessed from patterns. The tables are numbered from 0 in the order of appearance.</td> <td>no</td>
 * </table>
 *
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class PatternHarmonyEngine extends AbstractHarmonyEngine {
	private Chord[] chord;
	private int[] ticks;
	private int[] sectionTicks;
	
	private String[] chordPatterns;
	private String[][] chordRandomTables;
	
	private Random random;
	
	public PatternHarmonyEngine() {
		super();
	}
	
	public Chord getChord(int tick) {
		if(chord == null) {
			parsePattern();
		}
		
		if(tick >= 0 && tick < structure.getTicks()) {
			return chord[tick];
		} else {
			return null;
		}
	}

	public int getChordTicks(int tick) {
		if(chord == null) {
			parsePattern();
		}

		if(tick >= 0 && tick < structure.getTicks()) {
			return ticks[tick];
		} else {
			return 0;
		}
	}

	public int getChordSectionTicks(int tick) {
		if(chord == null) {
			parsePattern();
		}

		if(tick >= 0 && tick < structure.getTicks()) {
			return sectionTicks[tick];
		} else {
			return 0;
		}		
	}
	
	private void parsePattern() {
		chord = new Chord[structure.getTicks()];
		ticks = new int[structure.getTicks()];
		sectionTicks = new int[structure.getTicks()];
		
		String pat = createPattern();
		
		// prepend a '+' sign, if not already present
		
		if(!pat.startsWith("+")) {
			pat = "+"+pat;
		}
		
		logger.debug("Using harmony pattern "+pat);

		String[] c = pat.split(",");
		
		int tick = 0;
		int pos = 0;
		int sTicks = 0;
		
		Vector<Integer> sectionVector = new Vector<Integer>();
		
		Chord firstChord = null;
		Chord previousChord = null;
		
		while(tick < structure.getTicks()) {
			String[] p = c[pos % c.length].split("/");

			boolean newSection = false;
			
			int len = (int)(Integer.parseInt(p[1])*structure.getTicksPerBeat());
			
			String cho = p[0];
			
			if(cho.startsWith("+")) {
				newSection = true;
				if(sTicks > 0) {
					sectionVector.add(sTicks);
				}
				cho = cho.substring(1);
				sTicks = 0;
			} else {
				newSection = false;
			}
			
			int pitch;
			Chord ch;

			// check whether we have a major or minor chord
			
			if(cho.endsWith("m")) {
				pitch = NoteUtils.getNotePitch(cho.substring(0,cho.length()-1));
				
				if(pitch > 0) {
					pitch -= 12;
				}
				
				ch = new Chord(pitch,ChordType.MINOR,Chord.ChordSubtype.BASE_0);
			} else {
				pitch = NoteUtils.getNotePitch(cho);
				
				if(pitch > 0) {
					pitch -= 12;
				}
				
				ch = new Chord(pitch,ChordType.MAJOR,Chord.ChordSubtype.BASE_0);
			}

			if(firstChord == null) {
				firstChord = ch;
			}
			
			if(newSection) {
				ch = firstChord.findClosestChord(ch);
			} else {
				// previousChord is always non-null here
				ch = firstChord.findClosestChord(ch);
			}
			
			int songTicks = structure.getTicks();
			
			for(int j=0;j<len && tick < songTicks;j++) {
				ticks[tick] = (tick + len-j >= songTicks ? songTicks - tick : len-j);
				chord[tick] = ch;
				tick++;
				sTicks++;
			}
			
			previousChord = ch;
			
			pos++;
		}
		
		sectionVector.add(sTicks);
		
		logger.debug("Chord sections: "+sectionVector.size());
		
		tick = 0;
		for(int section=0;section<sectionVector.size();section++) {
			int len = sectionVector.get(section);
			
			for(int i=0;i<len;i++) {
				sectionTicks[tick] = len-i;
				tick++;
			}
		}
		
		checkSanity();
	}

	public void setChordPatterns(String[] chordPatterns) {
		if(chordPatterns == null || chordPatterns.length == 0) {
			throw(new IllegalArgumentException("Need at least 1 chord pattern"));
		}
		
		this.chordPatterns = chordPatterns;
	}
	
	public void setChordRandomTables(String[][] chordRandomTables) {
		if(chordRandomTables == null) {
			throw(new IllegalArgumentException("chordRandomTables must not be null"));
		}

		this.chordRandomTables = chordRandomTables;
	}
	
	/**
	 * Creates a pattern that can be parsed using parsePattern().
	 * 
	 * @return a pattern
	 */
	
	private String createPattern() {
		StringBuilder sb = new StringBuilder(80);
		
		// choose a chord pattern at random
		String[] chords = chordPatterns[random.nextInt(chordPatterns.length)].split(",");
		
		int count = chords.length;
		
		String firstChord = null;
		String prevChord = null;

		ArrayList<String> chordList = new ArrayList<String>();
		
		for(int i=0;i<count;i++) {
			if(sb.length() > 0) {sb.append(',');}
			
			String[] spec = chords[i].split("/");
			
			boolean nextSection;
			
			if(spec[0].startsWith("+")) {
				nextSection = true;
				spec[0] = spec[0].substring(1);
			} else {
				nextSection = false;
			}
			
			String chord;
			int length = Integer.parseInt(spec[1]);
			
			if(spec[0].startsWith("$")) {
				int refnum = Integer.parseInt(spec[0].substring(1));
				
				if(refnum < 0 || refnum >= chordList.size()) {
					throw(new RuntimeException("Invalid back reference $"+refnum));
				}
				
				chord = chordList.get(refnum);
			} else {
				// check if we have a note or a random table number
				
				int pitch = NoteUtils.getNotePitch(spec[0].endsWith("m") ? spec[0].substring(0,spec[0].length()-1) : spec[0]);
				
				if(pitch == Integer.MIN_VALUE) {
					// we have a random chord table number

					int pos = spec[0].indexOf('!');
					
					int table;
					int notrefnum;
					
					if (pos > 0) {
						table = Integer.parseInt(spec[0].substring(0,pos));
						notrefnum = Integer.parseInt(spec[0].substring(pos+1));
					} else {
						table = Integer.parseInt(spec[0]);
						notrefnum = -1;
					}

					int num;
					int it = 0;
					
					do {
						num = random.nextInt(chordRandomTables[table].length);
						chord = chordRandomTables[table][num];
						it++;
						if (it > 1000) {
							// try again
							return createPattern();
						}
					} while(chord.equals(prevChord) || i == count-1 && chord.equals(firstChord) || notrefnum >= 0 && chord.equals(chordList.get(notrefnum)));
				} else {
					// we have a note, take the note (include 'm' suffix, if present)
					chord = spec[0];				
				}
			}
				
			sb.append((nextSection ? "+" : "")+chord+"/"+length);
			prevChord = chord;

			if(i == 0) {
				firstChord = chord;
			}

			chordList.add(chord);			
		}
		
		return sb.toString();	
	}
	
	public void configure(Node node,XPath xpath) throws XPathException {		
		random = new Random(randomSeed);

		NodeList nodeList = (NodeList)xpath.evaluate("chordPattern",node,XPathConstants.NODESET);
		String[] chordPatterns = new String[nodeList.getLength()];
		
		for(int i=0;i<nodeList.getLength();i++) {
			chordPatterns[i] = XMLUtils.parseString(random,nodeList.item(i),xpath);
		}

		setChordPatterns(chordPatterns);

		nodeList = (NodeList)xpath.evaluate("chordRandomTable",node,XPathConstants.NODESET);
		String[][] chordRandomTables = new String[nodeList.getLength()][];
		
		for(int i=0;i<nodeList.getLength();i++) {
			String table = XMLUtils.parseString(random,nodeList.item(i),xpath);
			chordRandomTables[i] = table.split(",");
		}
		
		setChordRandomTables(chordRandomTables);
	}
}
