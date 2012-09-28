package com.soundhelix.component.patternengine.impl;

import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.misc.Pattern;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a PatternEngine that reads the pattern directly from a string.
 *
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class StringPatternEngine extends AbstractPatternEngine {
    /** The pattern string. */
    private String patternString;

    public Pattern render(String wildcardString) {
        return Pattern.parseString(patternString, wildcardString);
    }

    public void configure(Node node, XPath xpath) throws XPathException {
        Random random = new Random(randomSeed);

        NodeList nodeList = XMLUtils.getNodeList("string", node, xpath);

        if (nodeList.getLength() == 0) {
            throw new RuntimeException("Need at least 1 pattern string");
        }

        setPatternString(XMLUtils.parseString(random, nodeList.item(random.nextInt(nodeList.getLength())), xpath));
    }

    public String getPatternString() {
        return patternString;
    }

    public void setPatternString(String patternString) {
        this.patternString = patternString;
    }
}
