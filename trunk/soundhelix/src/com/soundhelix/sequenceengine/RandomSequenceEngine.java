package com.soundhelix.sequenceengine;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.harmonyengine.HarmonyEngine;
import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Chord.ChordSubtype;
import com.soundhelix.misc.Pattern.PatternEntry;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.util.NoteUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a sequence engine that plays random notes of the current chord
 * from a list of possible offsets with a given rhythmic pattern. For each
 * distinct chord section, a set of random notes is generated and used for
 * each occurrence of the chord section.
 * <br><br>
 * <b>XML-Configuration</b>
 * <table border=1>
 * <tr><th>Tag</th> <th>#</th> <th>Example</th> <th>Description</th> <th>Required</th>
 * <tr><td><code>pattern</code></td> <td>+</td> <td><code>0,-,-,-,0,-,0,-</code></td> <td>Sets the patterns to use. One of the patterns is selected at random.</td> <td>no</td>
 * <tr><td><code>offsets</code></td> <td>+</td> <td><code>0,1,2,3,4,5</code></td> <td>The list of chord offsets to choose from.</td> <td>no</td>
 * </table>
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class RandomSequenceEngine extends SequenceEngine {	
	private static String defaultPatternString = "0";
	private Pattern pattern;
	private int patternLength;
	private int[] offsets;

	private static final int[] majorTable = new int[] {0,4,7};
	private static final int[] minorTable = new int[] {0,3,7};

	private static Random random = new Random();
	
	public RandomSequenceEngine() {
		this(defaultPatternString);
	}

	public RandomSequenceEngine(String patternString) {
		super();
		setPattern(patternString);
	}

	public Track render(ActivityVector[] activityVectors) {
		ActivityVector activityVector = activityVectors[0];

		Sequence seq = new Sequence();
        HarmonyEngine ce = structure.getHarmonyEngine();
        
        int tick = 0;
        int ticks = structure.getTicks();
        
        Hashtable<String,Pattern> melodyHashtable = createMelodies();
        
		while(tick < ticks) {
        	int len = ce.getChordSectionTicks(tick);
        	Pattern p = melodyHashtable.get(ce.getChordSectionString(tick));
        	int pos = 0;
        	
        	for(int i=0;i<len;) {
    			PatternEntry entry = p.get(pos);
    			int l = entry.getTicks();

    			if(activityVector.isActive(tick)) {	
        			if(entry.isPause()) {
            			// add pause
            			seq.addPause(l);
        			} else {
        				seq.addNote(entry.getPitch(),l);
        			}
        		} else {
        			// add pause
        			seq.addPause(l);
        		}
        		
    			pos++;
    			i += l;
         	}
        	
        	tick += len;
        }
        
		Track track = new Track(TrackType.MELODY);
        track.add(seq);
        return track;
	}

    /**
     * Creates a pitch pattern for each distinct chord section and
     * returns a hashtable mapping chord section strings to
     * pitch patterns.
     * 
     * @return a hashtable mapping chord section strings to pitch patterns
     */
    
    private Hashtable<String,Pattern> createMelodies() {
    	HarmonyEngine he = structure.getHarmonyEngine();
    	
    	Hashtable<String,Pattern> ht = new Hashtable<String,Pattern>();
    	
    	int tick = 0;
    	
    	while(tick < structure.getTicks()) {
    		String section = he.getChordSectionString(tick);
            int len = he.getChordSectionTicks(tick);
    		
    		if(!ht.containsKey(section)) {
    			// no pattern created yet; create one
    			List<PatternEntry> list = new ArrayList<PatternEntry>();    			
    			
    			int lastValue = Integer.MIN_VALUE;
    			int pos = 0;
    			
    			for(int i=0;i<len;) {
    				PatternEntry entry = pattern.get(pos%patternLength);
        			Chord chord = he.getChord(tick+i);
        			int t = entry.getTicks();
        			
        			if(entry.isPause()) {
        				list.add(new PatternEntry(t));
        			} else {
        				int value;
        				
        				do {
        					value = offsets[random.nextInt(offsets.length)];
        				} while(value == lastValue);

        				if(true) {
            				if(chord.getSubtype() == ChordSubtype.BASE_4) {
            					value++;
            				} else if(chord.getSubtype() == ChordSubtype.BASE_6) {
            					value--;
            				}
            			}
        				
            			int octave = (value >= 0 ? value/3 : (value-2)/3);
            			int offset = ((value%3)+3)%3;
            			
            	 	    if(chord.isMajor()) {
            			    list.add(new PatternEntry(octave*12+majorTable[offset]+chord.getPitch(),entry.getVelocity(),t));
            		    } else {
            			    list.add(new PatternEntry(octave*12+minorTable[offset]+chord.getPitch(),entry.getVelocity(),t));
            		    }
        				
        				lastValue = value;
        			}
        			
        			pos++;
        			i += t;
    			}
   
    			ht.put(section,new Pattern(list.toArray(new PatternEntry[list.size()])));
    		} else {
    			// melody already created, skip chord section
    		}
    		
    		tick += len;
    	}
    	
    	return ht;
    }
    
    public void configure(Node node,XPath xpath) throws XPathException {
		NodeList nodeList = (NodeList)xpath.evaluate("pattern",node,XPathConstants.NODESET);
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
    
	public void setPattern(String patternString) {
		this.pattern = Pattern.parseString(patternString,"");
		this.patternLength = pattern.size();
	}
	
	public void setOffsets(int[] offsets) {
		this.offsets = offsets;
	}
}
