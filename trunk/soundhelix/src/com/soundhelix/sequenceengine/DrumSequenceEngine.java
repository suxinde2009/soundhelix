package com.soundhelix.sequenceengine;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a sequence engine for drum machines. Drum machines normally play a certain
 * sample (e.g., a base drum or a snare) when a certain pitch is played. This class supports an
 * arbitrary number of combinations of patterns, pitches and activity groups.
 * Each pattern acts as a voice for a certain pitch. The activity group can be used to
 * group the voices together so that they are all active or all silent at the same time.
 * For example, you might group three hi-hat patterns together so that all 3 are active
 * or silent. The activity groups must be numbered starting from 0, and the used groups must be
 * "dense" (i.e., without gaps).
 *
 * <h3>XML configuration</h3>
 * <table border=1>
 * <tr><th>Tag</th> <th>#</th> <th>Attributes</th> <th>Description</th> <th>Required</th>
 * <tr><td><code>pattern</code></td> <td>+</td> <td><code>pitch</code>, <code>activityGroup</code></td> <td>Defines the pattern to use with the given pitch. The pattern is put into the given activity group.</td> <td>yes</td>
 * </table>
 *
 * <h3>Configuration example</h3>
 * 
 * The following example uses 6 patterns with 4 activity groups:
 * <br>
 * <pre>&lt;sequenceEngine class="DrumSequenceEngine"&gt;
 *   &lt;!-- base drum --&gt;
 *   &lt;pattern pitch="36" activityGroup="0"&gt;10001000100010001000100010001001100010001000100010001000100010101000100010001000100010001000100110001000100010001000100011101011&lt;/pattern&gt;
 *   &lt;!-- clap --&gt;
 *   &lt;pattern pitch="37" activityGroup="1"&gt;00001000000010000000100000001000000010000000100000001000000110100000100000001000000010000000100000001000000010000000100001001000&lt;/pattern&gt;
 *   &lt;!-- closed hi-hat --&gt;
 *   &lt;pattern pitch="40" activityGroup="2"&gt;10001000100010001100100010001000&lt;/pattern&gt;
 *   &lt;!-- open hi-hat --&gt;
 *   &lt;pattern pitch="44" activityGroup="2"&gt;0010001000100010001000100010001000100010001000100010001000100101&lt;/pattern&gt;
 *   &lt;!-- other hi-hat --&gt;
 *   &lt;pattern pitch="39" activityGroup="2"&gt;001&lt;/pattern&gt;
 *   &lt;!-- snare --&gt;
 *   &lt;pattern pitch="42" activityGroup="3"&gt;00001000010010000000100001001010000010000100100000001000010010110000100001001000000010010100101000001000010010000000100101001011&lt;/pattern&gt;
 * &lt;/sequenceEngine&gt;
 * </pre>
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

// TODO: consider adding velocity support to the patterns (do we need this?)

public class DrumSequenceEngine extends SequenceEngine {

	private DrumEntry[] drumEntries;
	private int activityVectors;
	
	public DrumSequenceEngine() {
		super();
	}
	
	public void setDrumEntries(DrumEntry[] drumEntries) {
		int activityVectors = -1;
		
		for(int i=0;i<drumEntries.length;i++) {
			activityVectors = Math.max(activityVectors,drumEntries[i].activityGroup);
		}

		activityVectors++;

		this.activityVectors = activityVectors;
		
		this.drumEntries = drumEntries;
	}
	
	public Track render(ActivityVector[] activityVector) {
		Track track = new Track(TrackType.RHYTHM);

		int stretchFactor;
		
		int ticksPerBeat = structure.getTicksPerBeat();
		
		switch(ticksPerBeat) {
		case 16:
		case 12:
		case 8:
		case 6:
		case 4:
		case 3:
		        stretchFactor = 1;
		        break;

		case 2: 
        		stretchFactor = 2;
        		break;

		case 1: 
        		stretchFactor = 4;
        		break;
        		
        default:
        		throw(new RuntimeException("Invalid ticks per beat"));
		}
	
		for(int i=0;i<drumEntries.length;i++) {
			DrumEntry drumEntry = drumEntries[i];
			
        	Sequence seq = new Sequence();
      	
        	for(int tick=0,patternTick=0;tick<structure.getTicks();tick++,patternTick++) {
        		if(activityVector[drumEntry.activityGroup].isActive(tick) && drumEntry.pattern.charAt((patternTick*stretchFactor)%drumEntry.pattern.length()) == '1') {
        			seq.addNote(drumEntry.pitch,1);
        		} else {
        			seq.addPause(1);
        		}
        		
        		switch(ticksPerBeat) {
        		case 6:
        		case 8: seq.addPause(1);
        				tick++;
        		        break;
        		        
        		case 12:
        		case 16: seq.addPause(3);
        		         tick += 3;
        		         break;
        		}
        	}
      
        	track.add(seq);
		}    
		
        return track;
	}
	
	public int getActivityVectorCount() {
		return activityVectors;
	}
	
    public void configure(Node node,XPath xpath) throws XPathException {
		NodeList nodeList = (NodeList)xpath.evaluate("pattern",node,XPathConstants.NODESET);

		int patterns = nodeList.getLength();
		
		DrumEntry[] drumEntries = new DrumEntry[patterns];
		
		if(nodeList.getLength() == 0) {
			throw(new RuntimeException("Need at least 1 pattern"));
		}
		
		for(int i=0;i<patterns;i++) {
			String pattern = XMLUtils.parseString(nodeList.item(i),xpath);
			int pitch = Integer.parseInt((String)xpath.evaluate("attribute::pitch",nodeList.item(i),XPathConstants.STRING));
			int activityGroup = Integer.parseInt((String)xpath.evaluate("attribute::activityGroup",nodeList.item(i),XPathConstants.STRING));

			if(activityGroup < 0) {
				throw(new RuntimeException("activityGroup must not be negative"));
			}
			
			drumEntries[i] = new DrumEntry(pattern,pitch,activityGroup);
		}
		
		setDrumEntries(drumEntries);
    }
    
    private class DrumEntry {
    	private final String pattern;
    	private final int pitch;
    	private final int activityGroup;
    	
    	private DrumEntry(String pattern,int pitch,int activityGroup) {
    		this.pattern = pattern;
    		this.pitch = pitch;
    		this.activityGroup = activityGroup;
    	}
    }
}
