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
 * Implements a flexible HarmonyEngine based on user-specified patterns. One of the given
 * patterns is chosen at random. The patterns contain references to chord random tables, which
 * are used to randomly select a chord for the pattern position. Chord patterns can also
 * contain back references (denoted as "$position", e.g., "$0") which reproduces a chord that
 * was generated at an earlier position.
 *
 * <br><br>
 * <b>XML-Configuration</b>
 * <table border=1>
 * <tr><th>Tag</th> <th>#</th> <th>Example</th> <th>Description</th> <th>Required</th>
 * <tr><td><code>chordPattern</code></td> <td>+</td> <td><code>0/4,1/4,1/4,2/4</code></td> <td>Sets the patterns to use. The pattern is a comma-separated list consisting of random table numbers and lengths in beats. One of the patterns is selected at random.</td> <td>yes</td>
 * <tr><td><code>chordRandomTable</code></td> <td>*</td> <td><code>C,Am,G,F,Em,Dm</code></td> <td>Adds a random table. Random tables can be accessed from patterns. The tables are numbered from 0 in the order of appearance.</td> <td>yes</td>
 * </table>
 *
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class PatternHarmonyEngine extends HarmonyEngine {
	private Chord[] chord;
	private int[] ticks;
	private int[] sectionTicks;
	
	private String[] chordPatterns;
	private String[][] chordRandomTables;
	
	public PatternHarmonyEngine() {
		super();
	}
	
	public Chord getChord(int tick) {
		if(chord == null) {
			parsePattern();
		}
		
		if(tick < structure.getTicks()) {
			return chord[tick];
		} else {
			return null;
		}
	}

	public int getChordTicks(int tick) {
		if(chord == null) {
			parsePattern();
		}

		if(tick < structure.getTicks()) {
			return ticks[tick];
		} else {
			return 0;
		}
	}

	public int getChordSectionTicks(int tick) {
		if(chord == null) {
			parsePattern();
		}

		if(tick < structure.getTicks()) {
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
		
		if(!pat.startsWith("+")) {
			pat = "+"+pat;
		}
		
		float factor;
		float r = (float)Math.random();
		
		if(r > 1) {
			factor = 2.0f;
		} else if(r > 0.01) {
			factor = 1.0f;
		} else {
			factor = 0.5f;
			pat = pat+","+pat.substring(1);
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
			
			int len = (int)(Integer.parseInt(p[1])*structure.getTicksPerBeat()*factor);
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
				if(pitch > 0)
					pitch -= 12;
				ch = new Chord(pitch,ChordType.MINOR,Chord.ChordSubtype.BASE_0);
			} else {
				pitch = NoteUtils.getNotePitch(cho);
				if(pitch > 0)
					pitch -= 12;
				ch = new Chord(pitch,ChordType.MAJOR,Chord.ChordSubtype.BASE_0);;
			}

			if(firstChord == null) {
				firstChord = ch;
			}
			
			if(newSection) {
				ch = firstChord.findClosestChord(ch);
			} else {
				// previousChord is always non-null here
				ch = previousChord.findClosestChord(ch);
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
		
		System.out.println("Sections: "+sectionVector.size());
		
		
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
		Random random = new Random();
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
				int table = Integer.parseInt(spec[0]);

				int num;

				do {
					num = random.nextInt(chordRandomTables[table].length);
					chord = chordRandomTables[table][num];
				} while(chord.equals(prevChord) || i == count-1 && chord.equals(firstChord));
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
		NodeList nodeList = (NodeList)xpath.evaluate("chordPattern",node,XPathConstants.NODESET);
		String[] chordPatterns = new String[nodeList.getLength()];
		
		for(int i=0;i<nodeList.getLength();i++) {
			chordPatterns[i] = XMLUtils.parseString(nodeList.item(i),xpath);
		}

		setChordPatterns(chordPatterns);

		nodeList = (NodeList)xpath.evaluate("chordRandomTable",node,XPathConstants.NODESET);
		String[][] chordRandomTables = new String[nodeList.getLength()][];
		
		for(int i=0;i<nodeList.getLength();i++) {
			String table = XMLUtils.parseString(nodeList.item(i),xpath);
			chordRandomTables[i] = table.split(",");
		}
		
		setChordRandomTables(chordRandomTables);
	}
}
