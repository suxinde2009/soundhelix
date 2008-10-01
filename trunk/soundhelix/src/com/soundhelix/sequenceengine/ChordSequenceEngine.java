package com.soundhelix.sequenceengine;

import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.harmonyengine.HarmonyEngine;
import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Chord.ChordSubtype;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a sequence engine that plays chords using user-specified patterns.
 *
 * <h3>XML configuration</h3>
 * <table border=1>
 * <tr><th>Tag</th> <th>#</th> <th>Attributes</th> <th>Description</th> <th>Required</th>
 * <tr><td><code>pattern</code></td> <td>+</td> <td></td> <td>Defines the pattern to use.</td> <td>yes</td>
 * <tr><td><code>offsets</code></td> <td>1</td> <td></td> <td>Defines the chord offsets to use (defaults to "0,1,2").</td> <td>no</td>
 * </table>
 *
 * <h3>Configuration example</h3>
 * 
 * <pre>
 * &lt;sequenceEngine class="ChordSequenceEngine"&gt;
 *   &lt;offsets&gt;-1,0,1,2&lt;/offsets&gt;
 *   &lt;pattern&gt;0,-,-,-,0,-,-,0,-,-,0,-,-,0,-,-,0,-,-,-,0,-,-,0,-,-,0,-,3,-,1,-,0,-,-,-,0,-,-,0,-,-,0,-,-,0,-,-,0,-,-,-,0,-,-,0,-,-,0,-,3,1,4,2&lt;/pattern&gt;
 *   &lt;pattern&gt;-,-,0,-&lt;/pattern&gt;
 * &lt;/sequenceEngine&gt;
 * </pre>

 * @author Thomas Schürger (thomas@schuerger.com)
 */

// TODO: allow specifying velocities in patterns (like in the PatternSequenceEngine)

public class ChordSequenceEngine extends SequenceEngine {
	
	private static final int[] majorTable = new int[] {0,4,7};
	private static final int[] minorTable = new int[] {0,3,7};

	private static boolean obeyChordSubtype = true;
	private int[] pattern;
	private int patternLength;
	
	private int voiceCount = 3;
	private int[] offsets;

	public ChordSequenceEngine() {
		super();
	}

	public void setPattern(String patternString) {
		this.pattern = parsePatternString(patternString);
		this.patternLength = pattern.length;
	}

	public void setOffsets(int[] offsets) {
		if(offsets.length == 0) {
			throw(new RuntimeException("Array of offsets must not be empty"));
		}
		
		this.offsets = offsets;
    	this.voiceCount = offsets.length;
	}

	public Track render(ActivityVector[] activityVectors) {
		ActivityVector activityVector = activityVectors[0];
		
        Sequence seq[] = new Sequence[voiceCount];
        HarmonyEngine ce = structure.getHarmonyEngine();
        
        int tick = 0;
        
        for(int i=0;i<voiceCount;i++) {
        	seq[i] = new Sequence();
        }
        
        while(tick < structure.getTicks()) {
        	Chord chord = ce.getChord(tick);
        	int len = ce.getChordTicks(tick);
        
        	for(int i=0;i<len;i++) {
        		if(activityVector.isActive(tick)) {
        			// add next note for major/minor chord

        			for(int k=0;k<voiceCount;k++) {
        				int value = pattern[(tick+i)%patternLength];

        				if(value == Integer.MIN_VALUE) {
        					seq[k].addPause(1);        						
        					continue;
        				}

        				value += offsets[k];

        				if(obeyChordSubtype) {
        					if(chord.getSubtype() == ChordSubtype.BASE_4) {
        						value++;
        					} else if(chord.getSubtype() == ChordSubtype.BASE_6) {
        						value--;
        					}
        				}
        			
        				// split value into octave and offset

        				int octave = (value >= 0 ? value/3 : (value-2)/3);
        				int offset = ((value%3)+3)%3;

        				if(chord.isMajor()) {
        					seq[k].addNote(octave*12+majorTable[offset]+chord.getPitch(),1);
        				} else {
        					seq[k].addNote(octave*12+minorTable[offset]+chord.getPitch(),1);
        				}
        			}
        		} else {
        			for(int k=0;k<voiceCount;k++) {
        				seq[k].addPause(1);
        			}
        		}
        	}
        	
        	tick += len;
        }
        
		Track track = new Track(TrackType.MELODY);

		for(int i=0;i<voiceCount;i++) {
			track.add(seq[i]);		
		}

		return track;
	}
	
	private static int[] parsePatternString(String s) {
		String[] p = s.split(",");
		int len = p.length;
		
		int[] array = new int[len];
		
		for(int i=0;i<len;i++) {
			if(p[i].equals("-")) {
				array[i] = Integer.MIN_VALUE;
			} else {
				array[i] = Integer.parseInt(p[i]);
			}
		}
		
		return array;
	}
	
    public void configure(Node node,XPath xpath) throws XPathException {
		NodeList nodeList = (NodeList)xpath.evaluate("pattern",node,XPathConstants.NODESET);

		if(nodeList.getLength() == 0) {
			throw(new RuntimeException("Need at least 1 pattern"));
		}
		
		setPattern(XMLUtils.parseString(nodeList.item(new Random().nextInt(nodeList.getLength())),xpath));

    	String offsetString = XMLUtils.parseString("offsets",node,xpath);
    	
    	if(offsetString == null || offsetString.equals("")) {
    		offsetString = "0,1,2";
    	}
    	
    	String[] offsetList = offsetString.split(",");
    	
    	int[] offsets = new int[offsetList.length];
    	
    	for(int i=0;i<offsetList.length;i++) {
    		offsets[i] = Integer.parseInt(offsetList[i]);
    	}
    	
    	setOffsets(offsets);
    }
}
