package com.soundhelix.component.patternengine.impl;

import java.util.Random;

import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.Pattern.PatternEntry;
import com.soundhelix.misc.SongContext;
import com.soundhelix.util.RandomUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements pattern engine that generates a pattern by repeating a given pattern while changing the velocity of each pattern entry.
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

    /** The ticks per beat for the prefix pattern. */
    private int prefixPatternTicksPerBeat = -1;

    /** The main pattern string. */
    private String patternString;

    /** The ticks per beat for the main pattern. */
    private int patternTicksPerBeat = -1;

    /** The suffix pattern string. */
    private String suffixPatternString;

    /** The ticks per beat for the suffix pattern. */
    private int suffixPatternTicksPerBeat = -1;

    @Override
    public void configure(SongContext songContext, Node node) throws XPathException {
        random = new Random(randomSeed);

        try {
            setPatternTicks(XMLUtils.parseInteger(random, "patternTicks", node));
        } catch (Exception e) {}

        try {
            setMinVelocity(XMLUtils.parseInteger(random, "minVelocity", node));
        } catch (Exception e) {}

        try {
            setMaxVelocity(XMLUtils.parseInteger(random, "maxVelocity", node));
        } catch (Exception e) {}

        try {
            setVelocityExponent(XMLUtils.parseDouble(random, "velocityExponent", node));
        } catch (Exception e) {}

        try {
            setPrefixPatternTicksPerBeat(XMLUtils.parseInteger(random, "prefixPattern/@ticksPerBeat", node));
        } catch (Exception e) {}

        try {
            setPrefixPatternString(XMLUtils.parseString(random, "prefixPattern", node));
        } catch (Exception e) {}

        try {
            setPatternTicksPerBeat(XMLUtils.parseInteger(random, "pattern/@ticksPerBeat", node));
        } catch (Exception e) {}

        try {
            setPatternString(XMLUtils.parseString(random, "pattern", node));
        } catch (Exception e) {}

        try {
            setSuffixPatternTicksPerBeat(XMLUtils.parseInteger(random, "suffixPattern/@ticksPerBeat", node));
        } catch (Exception e) {}

        try {
            setSuffixPatternString(XMLUtils.parseString(random, "suffixPattern", node));
        } catch (Exception e) {}

        super.setPatternString(generatePattern(songContext, patternString));
    }

    /**
     * Generates a pattern that is based on the given pattern of patterns. Each element of the pattern pattern represents a variation of the base
     * pattern specified by the first character each.
     * 
     * @param songContext the song context
     * @param patternPattern the string of pattern characters (comma-separated)
     * 
     * @return the generated pattern
     */

    private String generatePattern(SongContext songContext, String patternPattern) {
        StringBuilder sb = new StringBuilder();

        Pattern prefixPattern = Pattern.parseString(songContext, prefixPatternString, prefixPatternTicksPerBeat);
        Pattern pattern = Pattern.parseString(songContext, patternString, patternTicksPerBeat);
        Pattern suffixPattern = Pattern.parseString(songContext, suffixPatternString, suffixPatternTicksPerBeat);

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
            throw new RuntimeException("Concatenated pattern (prefix pattern + n * pattern + suffix pattern) must " + "contain at least 2 ticks");
        }

        int tick = appendPattern(songContext, sb, prefixPattern, 0, totalTicks);

        for (int i = 0; i < repetitions; i++) {
            tick = appendPattern(songContext, sb, pattern, tick, totalTicks);
        }

        appendPattern(songContext, sb, suffixPattern, tick, totalTicks);

        logger.debug("Pattern: " + sb);

        return sb.toString();
    }

    /**
     * Appends the given pattern to the given StringBuilder.
     * 
     * @param songContext the song context
     * @param sb the StringBuilder
     * @param pattern the pattern
     * @param tick the current tick
     * @param totalTicks the total number of ticks
     * 
     * @return the new number of ticks
     */

    private int appendPattern(SongContext songContext, StringBuilder sb, Pattern pattern, int tick, int totalTicks) {
        if (pattern == null || pattern.getTicks() == 0) {
            // nothing to do
            return tick;
        }

        for (PatternEntry entry : pattern) {
            if (sb.length() > 0) {
                sb.append(',');
            }

            if (entry.isPause()) {
                sb.append("-/").append(entry.getTicks());
            } else {
                double v = tick / (totalTicks - 1d);
                int velocity = (int) (RandomUtils.getPowerDouble(v, minVelocity, maxVelocity, velocityExponent) * entry.getVelocity() / songContext
                        .getStructure().getMaxVelocity());

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

                if (velocity != songContext.getStructure().getMaxVelocity()) {
                    sb.append(entry.getTicks()).append(':').append(velocity);
                } else {
                    sb.append(entry.getTicks());
                }
            }

            tick += entry.getTicks();
        }

        return tick;
    }

    @Override
    public void setPatternTicksPerBeat(int patternTicksPerBeat) {
        this.patternTicksPerBeat = patternTicksPerBeat;
    }

    @Override
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

    public void setPrefixPatternTicksPerBeat(int prefixPatternTicksPerBeat) {
        this.prefixPatternTicksPerBeat = prefixPatternTicksPerBeat;
    }

    public void setPrefixPatternString(String prefixPatternString) {
        this.prefixPatternString = prefixPatternString;
    }

    public void setSuffixPatternTicksPerBeat(int suffixPatternTicksPerBeat) {
        this.suffixPatternTicksPerBeat = suffixPatternTicksPerBeat;
    }

    public void setSuffixPatternString(String suffixPatternString) {
        this.suffixPatternString = suffixPatternString;
    }

    public void setPatternTicks(int patternTicks) {
        this.patternTicks = patternTicks;
    }
}
