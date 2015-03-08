package com.soundhelix.component.patternengine.impl;

import java.util.Random;

import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.SongContext;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a PatternEngine that reads the pattern directly from a string.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class StringPatternEngine extends AbstractPatternEngine {
    /** The ticks per beat of the pattern. */
    private int patternTicksPerBeat = -1;

    /** The pattern string. */
    private String patternString;

    @Override
    public Pattern render(SongContext songContext, String wildcardString) {
        return Pattern.parseString(songContext, patternString, wildcardString, patternTicksPerBeat);
    }

    @Override
    public void configure(SongContext songContext, Node node) throws XPathException {
        Random random = new Random(randomSeed);

        NodeList nodeList = XMLUtils.getNodeList("string", node);

        if (nodeList.getLength() == 0) {
            throw new RuntimeException("Need at least 1 pattern string");
        }

        Node patternNode = nodeList.item(random.nextInt(nodeList.getLength()));

        try {
            setPatternTicksPerBeat(XMLUtils.parseInteger(random, "@ticksPerBeat", patternNode));
        } catch (Exception e) {}

        setPatternString(XMLUtils.parseString(random, patternNode));
    }

    public String getPatternString() {
        return patternString;
    }

    public void setPatternString(String patternString) {
        this.patternString = patternString;
    }

    public void setPatternTicksPerBeat(int patternTicksPerBeat) {
        this.patternTicksPerBeat = patternTicksPerBeat;
    }
}
