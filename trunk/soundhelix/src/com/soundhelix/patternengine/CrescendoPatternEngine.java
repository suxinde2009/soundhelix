package com.soundhelix.patternengine;

import java.util.Iterator;
import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.Pattern.PatternEntry;
import com.soundhelix.util.RandomUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements pattern engine that generates a pattern by repeating a given pattern while changing the velocity of each
 * pattern entry.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class CrescendoPatternEngine extends StringPatternEngine {

    /** The random generator. */
    private Random random;

    /** The number of pattern repetitions. */
    private int repetitions;

    /** The minimum velocity. */
    private double minVelocity;
    
    /** The maximum velocity. */
    private double maxVelocity;
    
    /** The velocity exponent. */
    private double velocityExponent;
    
    /** The pattern string. */
    private String patternString;
    
    public void configure(Node node, XPath xpath) throws XPathException {
        random = new Random(randomSeed);
        
        setRepetitions(XMLUtils.parseInteger(random, "repetitions", node, xpath));
        setMinVelocity(XMLUtils.parseInteger(random, "minVelocity", node, xpath));
        setMaxVelocity(XMLUtils.parseInteger(random, "maxVelocity", node, xpath));
        setVelocityExponent(XMLUtils.parseDouble(random, "velocityExponent", node, xpath));
        setPatternString(XMLUtils.parseString(random, "pattern", node, xpath));
        
        super.setPatternString(generatePattern(patternString));
    }
    
    /**
     * Generates a pattern that is based on the given pattern of patterns. Each element of the pattern pattern
     * represents a variation of the base pattern specified by the first character each.
     * 
     * @param patternPattern the string of pattern characters (comma-separated)
     * 
     * @return the generated pattern
     */
    
    private String generatePattern(String patternPattern) {
        StringBuilder sb = new StringBuilder();

        Pattern p = Pattern.parseString(patternString);
        
        if (p.size() == 0) {
            throw new RuntimeException("Pattern must contain at least 1 entry");
        }
        
        int totalSize = repetitions * p.size();
        int pos = 0;
        
        for (int i = 0; i < repetitions; i++) {
            for (Iterator<PatternEntry> it = p.iterator(); it.hasNext();) {
                PatternEntry entry = it.next();
 
                if (sb.length() > 0) {
                    sb.append(',');
                }

                double v = (double) pos / (totalSize - 1d);
                int velocity;

                if (velocityExponent >= 0.0d) {
                    velocity = (int) RandomUtils.getPowerDouble(v, minVelocity, maxVelocity, velocityExponent);
                } else {
                    velocity = (int) RandomUtils.getPowerDouble(v, maxVelocity, minVelocity, -velocityExponent);
                }

                if (entry.isLegato()) {
                    if (entry.isWildcard()) {
                        sb.append(entry.getWildcardCharacter()).append("~/");
                    } else {
                        sb.append(entry.getPitch()).append("~/");
                    }
                } else {
                    if (entry.isWildcard()) {
                        sb.append(entry.getWildcardCharacter()).append('/');
                    } else {
                        sb.append(entry.getPitch()).append('/');
                    }
                }
                
                if (velocity != Short.MAX_VALUE) {
                    sb.append(entry.getTicks()).append(':').append(velocity); 
                } else {
                    sb.append(entry.getTicks());
                }
                                
                
                pos++;
            }
        }
        
        logger.debug("Pattern: " + sb);
        
        return sb.toString();
    }
    
    public void setPatternString(String patternString) {
        this.patternString = patternString;
    }

    public void setVelocityExponent(double velocityExponent) {
        this.velocityExponent = velocityExponent;
    }

    public void setRepetitions(int repetitions) {
        this.repetitions = repetitions;
    }

    public void setMinVelocity(double minVelocity) {
        this.minVelocity = minVelocity;
    }

    public void setMaxVelocity(double maxVelocity) {
        this.maxVelocity = maxVelocity;
    }
}
