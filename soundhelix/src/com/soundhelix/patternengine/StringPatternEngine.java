package com.soundhelix.patternengine;

import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.Pattern.PatternEntry;
import com.soundhelix.util.XMLUtils;

public class StringPatternEngine extends AbstractPatternEngine {
    /** The pattern string. */
    private String patternString;
	
	public Pattern render(String wildcardString) {
		return parseString(patternString, wildcardString);
	}

	public void configure(Node node, XPath xpath) throws XPathException {
		Random random = new Random(randomSeed);
		
		NodeList nodeList = (NodeList) xpath.evaluate("string", node, XPathConstants.NODESET);

		if (nodeList.getLength() == 0) {
			throw new RuntimeException("Need at least 1 pattern string");
		}
		
        setPatternString(XMLUtils.parseString(random, nodeList.item(random.nextInt(nodeList.getLength())), xpath));
	}
	
	private static Pattern parseString(String patternString, String wildcardString) {
		if (wildcardString == null) {
			wildcardString = "";
		}
		
		PatternEntry[] pattern;
		
		String[] p = patternString.split(",");
		int len = p.length;

		pattern = new PatternEntry[len];
		
		// format: offset/ticks:velocity or offset~/ticks:velocity
		
		for (int i = 0; i < len; i++) {
			String[] a = p[i].split(":");
			short v = a.length > 1 ? Short.parseShort(a[1]) : Short.MAX_VALUE;
			String[] b = a[0].split("/");
			int t = b.length > 1 ? Integer.parseInt(b[1]) : 1;
			
			boolean legato = b[0].endsWith("~");
			
			if (legato) {
				// cut off legato character
				b[0] = b[0].substring(0, b[0].length() - 1);
			}

			if (b[0].equals("-")) {
				pattern[i] = new Pattern.PatternEntry(t);
			} else if (b[0].length() == 1 && wildcardString.indexOf(b[0]) >= 0) {
				pattern[i] = new PatternEntry(b[0].charAt(0), v, t, legato);
			} else {
				pattern[i] = new PatternEntry(Integer.parseInt(b[0]), v, t, legato);
			}
		}

		return new Pattern(pattern);
	}

	public String getPatternString() {
		return patternString;
	}

	public void setPatternString(String patternString) {
		this.patternString = patternString;
	}
}
