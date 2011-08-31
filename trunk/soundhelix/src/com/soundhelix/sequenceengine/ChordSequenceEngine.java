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
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class ChordSequenceEngine extends MultiPatternSequenceEngine {
    
    public ChordSequenceEngine() {
        super();
    }

    public void configure(Node node, XPath xpath) throws XPathException {
        random = new Random(randomSeed);

        String offsetString = XMLUtils.parseString(random, "offsets", node, xpath);
        
        if (offsetString == null || offsetString.equals("")) {
            offsetString = "0,1,2";
        }
        
        String[] offsetList = offsetString.split(",");
        
        int[] offsets = new int[offsetList.length];
        
        for (int i = 0; i < offsetList.length; i++) {
            offsets[i] = Integer.parseInt(offsetList[i]);
        }
        
        try {
            setNormalizeChords(!XMLUtils.parseBoolean(random, "obeyChordSubtype", node, xpath));
            logger.warn("The tag \"obeyChordSubtype\" has been deprecated. Use \"normalizeChords\" with inverted value instead.");
        } catch (Exception e) {}

        try {
            setNormalizeChords(XMLUtils.parseBoolean(random, "normalizeChords", node, xpath));
        } catch (Exception e) {}

        NodeList nodeList = (NodeList) xpath.evaluate("patternEngine", node, XPathConstants.NODESET);

        if (nodeList.getLength() == 0) {
            throw new RuntimeException("Need at least 1 pattern");
        }
        
        PatternEngine patternEngine;
        
        try {
            int i = random.nextInt(nodeList.getLength());
            patternEngine = XMLUtils.getInstance(PatternEngine.class, nodeList.item(i),
                    xpath, randomSeed, i);
        } catch (Exception e) {
            throw new RuntimeException("Error instantiating PatternEngine", e);
        }
        
        // render base pattern
        Pattern pattern = patternEngine.render("");

        // transpose base pattern once for each offset
        
        Pattern[] patterns = new Pattern[offsets.length];
        
        for (int i = 0; i < offsets.length; i++) {
            patterns[i] = pattern.transpose(offsets[i]);
        }
        
        setPatterns(patterns);
    }
}
