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
 * Implements a sequence engine that repeats a set of user-specified patterns in a voice each. A pattern is a string containing any number of
 * comma-separated integers, minus and plus signs. Integers play the corresponding note of the chord (0 is the base note, 1 the middle note and so on;
 * the numbers may also be negative). A minus sign is a pause. A plus sign plays a transition note between the current chord and the chord of the next
 * non-transition tone that will be played. The pitch of the transition note is based on the base notes of the two chords. This can be used for funky
 * base lines.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class MultiPatternSequenceEngine extends AbstractMultiPatternSequenceEngine {

    public MultiPatternSequenceEngine() {
        super();
    }

    @Override
    public void configure(SongContext songContext, Node node) throws XPathException {
        random = new Random(randomSeed);

        NodeList patternsList = XMLUtils.getNodeList("patternEngines", node);

        if (patternsList.getLength() == 0) {
            throw new RuntimeException("Need at least 1 list of patternEngines");
        }

        Node patternsNode = patternsList.item(random.nextInt(patternsList.getLength()));

        NodeList patternList = XMLUtils.getNodeList("patternEngine", patternsNode);

        if (patternList.getLength() == 0) {
            throw new RuntimeException("Need at least 1 pattern");
        }

        Pattern[] patterns = new Pattern[patternList.getLength()];

        for (int i = 0; i < patternList.getLength(); i++) {
            PatternEngine patternEngine;

            try {
                patternEngine = XMLUtils.getInstance(songContext, PatternEngine.class, patternList.item(i), randomSeed, i);
            } catch (Exception e) {
                throw new RuntimeException("Error instantiating PatternEngine", e);
            }

            patterns[i] = patternEngine.render(songContext, "" + TRANSITION);
        }

        setPatterns(patterns);

        try {
            setNormalizeChords(XMLUtils.parseBoolean(random, "normalizeChords", node));
        } catch (Exception e) {}

        configurePatternRestartMode(random, node);
    }
}
