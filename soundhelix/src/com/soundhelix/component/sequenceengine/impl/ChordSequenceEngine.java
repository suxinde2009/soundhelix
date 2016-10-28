package com.soundhelix.component.sequenceengine.impl;

import java.util.Random;

import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.component.patternengine.PatternEngine;
import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.SongContext;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a sequence engine that plays chords using user-specified patterns.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class ChordSequenceEngine extends MultiPatternSequenceEngine {

    /**
     * Constructor.
     */

    public ChordSequenceEngine() {
        super();
    }

    @Override
    public void configure(SongContext songContext, Node node) throws XPathException {
        random = new Random(randomSeed);

        String offsetString = XMLUtils.parseString(random, "offsets", node);

        if (offsetString == null || offsetString.equals("")) {
            offsetString = "0,1,2";
        }

        String[] offsetList = offsetString.split(",");

        int[] offsets = new int[offsetList.length];

        for (int i = 0; i < offsetList.length; i++) {
            offsets[i] = Integer.parseInt(offsetList[i]);
        }

        try {
            setNormalizeChords(XMLUtils.parseBoolean(random, "normalizeChords", node));
        } catch (Exception e) {}

        NodeList nodeList = XMLUtils.getNodeList("patternEngine", node);

        if (nodeList.getLength() == 0) {
            throw new RuntimeException("Need at least 1 pattern");
        }

        PatternEngine patternEngine;

        try {
            int i = random.nextInt(nodeList.getLength());
            patternEngine = XMLUtils.getInstance(songContext, PatternEngine.class, nodeList.item(i), randomSeed, i);
        } catch (Exception e) {
            throw new RuntimeException("Error instantiating PatternEngine", e);
        }

        // render base pattern
        Pattern pattern = patternEngine.render(songContext, "");

        // transpose base pattern once for each offset

        Pattern[] patterns = new Pattern[offsets.length];

        for (int i = 0; i < offsets.length; i++) {
            patterns[i] = pattern.transpose(offsets[i]);
        }

        setPatterns(patterns);
    }
}
