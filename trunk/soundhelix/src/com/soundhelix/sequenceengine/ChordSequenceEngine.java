package com.soundhelix.sequenceengine;

import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.misc.Pattern;
import com.soundhelix.patternengine.PatternEngine;
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

public class ChordSequenceEngine extends MultiPatternSequenceEngine {
	
	private int[] offsets;

	public ChordSequenceEngine() {
		super();
	}

	public void setOffsets(int[] offsets) {
		if (offsets.length == 0) {
			throw(new RuntimeException("Array of offsets must not be empty"));
		}
		
		this.offsets = offsets;
	}
	
	public void setObeyChordSubtype(boolean obeyChordSubtype) {
		this.obeyChordSubtype = obeyChordSubtype;
	}

    public void configure(Node node,XPath xpath) throws XPathException {
    	random = new Random(randomSeed);

    	String offsetString = XMLUtils.parseString(random,"offsets",node,xpath);
    	
    	if (offsetString == null || offsetString.equals("")) {
    		offsetString = "0,1,2";
    	}
    	
    	String[] offsetList = offsetString.split(",");
    	
    	int[] offsets = new int[offsetList.length];
    	
    	for (int i = 0; i < offsetList.length; i++) {
    		offsets[i] = Integer.parseInt(offsetList[i]);
    	}
    	
    	setOffsets(offsets);
    	
		try {
			setObeyChordSubtype(XMLUtils.parseBoolean(random,"obeyChordSubtype",node,xpath));
		} catch (Exception e) {}

		NodeList nodeList = (NodeList)xpath.evaluate("patternEngine",node,XPathConstants.NODESET);

		if (nodeList.getLength() == 0) {
			throw(new RuntimeException("Need at least 1 pattern"));
		}
		
		PatternEngine patternEngine;
		
		try {
			int i = random.nextInt(nodeList.getLength());
			patternEngine = XMLUtils.getInstance(PatternEngine.class,nodeList.item(i),
					xpath,randomSeed ^ 47351842858l);
		} catch (Exception e) {
			throw(new RuntimeException("Error instantiating PatternEngine",e));
		}
		
		// render base pattern
		Pattern pattern = patternEngine.render("");

		// transpose base pattern once for each offset
		
		Pattern[] patterns = new Pattern[offsets.length];
		
		for (int i = 0; i < offsets.length;i++) {
			patterns[i] = pattern.transpose(offsets[i]);
		}
		
		setPatterns(patterns);
    }
}
