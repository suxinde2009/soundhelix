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
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class CrescendoPatternEngine extends StringPatternEngine {

    /** The random generator. */
    private Random random;

    /** The number of ticks of the resulting pattern. */
    private int patternTicks;

    /** The minimum velocity. */
    private double minVelocity;
    
    /** The maximum velocity. */
    private double maxVelocity;
    
    /** The velocity exponent. */
    private double velocityExponent;

    /** The prefix pattern string. */
    private String prefixPatternString;

    /** The pattern string. */
    private String patternString;

    /** The suffix pattern string. */
    private String suffixPatternString;

    public void configure(Node node, XPath xpath) throws XPathException {
        random = new Random(randomSeed);
        
        try {
            setPatternTicks(XMLUtils.parseInteger(random, "patternTicks", node, xpath));
        } catch (Exception e) {
        }

        try {
            setMinVelocity(XMLUtils.parseInteger(random, "minVelocity", node, xpath));
        } catch (Exception e) {
        }

        try {
            setMaxVelocity(XMLUtils.parseInteger(random, "maxVelocity", node, xpath));
        } catch (Exception e) {
        }

        try {
            setVelocityExponent(XMLUtils.parseDouble(random, "velocityExponent", node, xpath));
        } catch (Exception e) {
        }

        try {
            setPrefixPatternString(XMLUtils.parseString(random, "prefixPattern", node, xpath));
        } catch (Exception e) {
        }

        try {
            setPatternString(XMLUtils.parseString(random, "pattern", node, xpath));
        } catch (Exception e) {
        }

        try {
            setSuffixPatternString(XMLUtils.parseString(random, "suffixPattern", node, xpath));
        } catch (Exception e) {
        }

        
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

        Pattern prefixPattern = Pattern.parseString(prefixPatternString);
        Pattern pattern = Pattern.parseString(patternString);
        Pattern suffixPattern = Pattern.parseString(suffixPatternString);

        int prefixPatternTicks = prefixPattern != null ? prefixPattern.getTicks() : 0;
        int patternTicks = pattern != null ? pattern.getTicks() : 0;
        int suffixPatternTicks = suffixPattern != null ? suffixPattern.getTicks() : 0;
        
        if (prefixPatternTicks + suffixPatternTicks > this.patternTicks) {
            throw new RuntimeException("Prefix pattern and suffix pattern are longer than patternTicks");
        }

        int repetitions = (this.patternTicks - prefixPatternTicks - suffixPatternTicks) / patternTicks;
        
        logger.debug("Repetitions: " + repetitions);
        
        int totalTicks = prefixPatternTicks + repetitions * patternTicks + suffixPatternTicks;

        if (totalTicks < 2) {
            throw new RuntimeException("Concatenated pattern (prefix pattern + n * pattern + suffix pattern) must "
                    + "contain at least 2 ticks");
        }
        
        int tick = appendPattern(sb, prefixPattern, 0, totalTicks);
        
        for (int i = 0; i < repetitions; i++) {
            tick = appendPattern(sb, pattern, tick, totalTicks);
        }

        appendPattern(sb, suffixPattern, tick, totalTicks);

        logger.debug("Pattern: " + sb);
        
        return sb.toString();
    }
    
    /**
     * Appends the given pattern to the given StringBuilder.
     *
     * @param sb the StringBuilder
     * @param pattern the pattern
     * @param tick the current tick
     * @param totalTicks the total number of ticks
     *
     * @return the new number of ticks
     */

    private int appendPattern(StringBuilder sb, Pattern pattern, int tick, int totalTicks) {
        if (pattern == null || pattern.getTicks() == 0) {
            // nothing to do
            return tick;
        }
        
        for (Iterator<PatternEntry> it = pattern.iterator(); it.hasNext();) {
            PatternEntry entry = it.next();
 
            if (sb.length() > 0) {
                sb.append(',');
            }

            if (entry.isPause()) {
                sb.append("-/").append(entry.getTicks());
            } else {
                double v = (double) tick / (totalTicks - 1d);
                int velocity = (int) (RandomUtils.getPowerDouble(v, minVelocity, maxVelocity, velocityExponent)
                        * entry.getVelocity() / Short.MAX_VALUE);

                if (velocity < 1 && minVelocity >= 1) {
                    // this will make sure that a note is not converted to a pause
                    velocity = 1;
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
            }
                
            tick += entry.getTicks();
        }
        
        return tick;
    }
    
    public void setPatternString(String patternString) {
        this.patternString = patternString;
    }

    public void setVelocityExponent(double velocityExponent) {
        this.velocityExponent = velocityExponent;
    }

    public void setMinVelocity(double minVelocity) {
        this.minVelocity = minVelocity;
    }

    public void setMaxVelocity(double maxVelocity) {
        this.maxVelocity = maxVelocity;
    }

    public void setPrefixPatternString(String prefixPatternString) {
        this.prefixPatternString = prefixPatternString;
    }

    public void setSuffixPatternString(String suffixPatternString) {
        this.suffixPatternString = suffixPatternString;
    }

    public void setPatternTicks(int patternTicks) {
        this.patternTicks = patternTicks;
    }
}
