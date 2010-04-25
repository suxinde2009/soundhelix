package com.soundhelix.sequenceengine;

import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.util.XMLUtils;

/**
 * Implements a sequence engine that repeats a set of user-specified patterns in a voice each.
 * A pattern is a string containing any number of comma-separated integers, minus and
 * plus signs. Integers play the corresponding note of the chord (0 is the base
 * note, 1 the middle note and so on; the numbers may also be negative). A minus
 * sign is a pause. A plus sign plays a transition note between the current
 * chord and the chord of the next non-transition tone that will be played. The
 * pitch of the transition note is based on the base notes of the two chords.
 * This can be used for funky base lines.
 *
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class MultiPatternSequenceEngine extends AbstractMultiPatternSequenceEngine {
	
	public MultiPatternSequenceEngine() {
		super();
	}

	public void configure(Node node,XPath xpath) throws XPathException {
    	random = new Random(randomSeed);

		NodeList patternsList = (NodeList)xpath.evaluate("patterns",node,XPathConstants.NODESET);

		if(patternsList.getLength() == 0) {
			throw(new RuntimeException("Need at least 1 list of patterns"));
		}

		Node patternsNode = patternsList.item(random.nextInt(patternsList.getLength()));
		
		NodeList patternList = (NodeList) xpath.evaluate("pattern", patternsNode,XPathConstants.NODESET);

		if(patternList.getLength() == 0) {
			throw(new RuntimeException("Need at least 1 pattern"));
		}

		String[] patterns = new String[patternList.getLength()];
		
		for(int i=0;i<patternList.getLength();i++) {
			patterns[i] = XMLUtils.parseString(random,patternList.item(i),xpath);
		}
		
		setPatterns(patterns);

		try {
			setObeyChordSubtype(XMLUtils.parseBoolean(random,"obeyChordSubtype",node,xpath));
		} catch(Exception e) {}
    }
}