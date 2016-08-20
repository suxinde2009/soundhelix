package com.soundhelix.component.patternengine.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

import com.soundhelix.misc.SongContext;
import com.soundhelix.util.RandomUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a generic sequence engine that generates a random pattern, with random pitches, random note and pause lengths, random legato and random
 * velocity, as well as the ability to repeat subpatterns.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

// FIXME: endless loops are possible in certain cases

public class RandomPatternEngine extends StringPatternEngine {

    /** The random generator. */
    private Random random;

    /**
     * The length of the base pattern. The total length of the pattern is this value times the length of the pattern string.
     */
    private int patternTicks = 16;

    /**
     * The comma-separated pattern used to generate the final pattern. Equal character pairs refer to equal patterns, different character pairs to
     * different patterns. The first character defines the base pattern, the second character the variation of the base pattern. For example, the
     * string "A1,A2,A1,B1" will will generate 2 base patterns (one each for A and B, called "A1" and "B1") and a variation of base pattern A called
     * "A2".
     */
    private String patternString = "A1,A2,A1,B1";

    /** The probability to use a note rather than a pause. */
    private double noteProbability = 0.8;
    /** The probability to use legato if a note is generated and the next pattern component is also a note. */
    private double legatoProbability = 0.25;

    /** The minimum velocity to use. */
    private double minVelocity = 1d;
    /** The maximum velocity to use. */
    private double maxVelocity = 20000d;

    /** The list of offsets to choose from. The list may contain values more than once for extra weight. */
    private PatternEntry[] offsets = {};
    /** The list of note lengths to choose from. The list may contain values more than once for extra weight. */
    private int[] noteLengths = {1, 2, 3, 2, 1, 1};
    /** The list of pause lengths to choose from. The list may contain values more than once for extra weight. */
    private int[] pauseLengths = {1, 2, 3, 2, 1, 1};
    /** The minimum number of active ticks. */
    private int minActiveTicks = 1;
    /** The maximum number of active ticks. */
    private int maxActiveTicks = 16;
    /** True if all pattern parts must be unique, false otherwise. */
    private boolean uniquePatternParts = true;

    /**
     * The correlation between pitch and velocity. If 1, the velocity depends only on the pitch and not on the random value; if 0, the velocity only
     * depends on the random value.
     */
    private double pitchVelocityCorrelation = 0.75;

    /**
     * The exponent for the power distribution used for velocity. 1 distributes linearly between minimum and maximum velocity, 2 quadratically, 0.5
     * like a square-root, etc. The same happens for a negative exponent, but reverses the meaning of minimum and maximum velocity.
     */
    private double velocityExponent = 3.0d;

    @Override
    public void configure(SongContext songContext, Node node) throws XPathException {
        random = new Random(randomSeed);

        setPatternTicks(XMLUtils.parseInteger(random, "patternTicks", node));
        setNoteProbability(XMLUtils.parseDouble(random, "noteProbability", node) / 100.0);
        setLegatoProbability(XMLUtils.parseDouble(random, "legatoProbability", node) / 100.0);
        setMinVelocity(XMLUtils.parseInteger(random, "minVelocity", node));
        setMaxVelocity(XMLUtils.parseInteger(random, "maxVelocity", node));
        setMinActiveTicks(XMLUtils.parseInteger(random, "minActiveTicks", node));
        setMaxActiveTicks(XMLUtils.parseInteger(random, "maxActiveTicks", node));
        setOffsets(parsePatternEntryListString(random, "offsets", node));
        setNoteLengths(XMLUtils.parseIntegerListString(random, "noteLengths", node));
        setPauseLengths(XMLUtils.parseIntegerListString(random, "pauseLengths", node));
        setPitchVelocityCorrelation(XMLUtils.parseDouble(random, "pitchVelocityCorrelation", node) / 100.0d);
        setVelocityExponent(XMLUtils.parseDouble(random, "velocityExponent", node));
        setPatternString(XMLUtils.parseString(random, "patternString", node));

        try {
            setUniquePatternParts(XMLUtils.parseBoolean(random, "uniquePatternParts", node));
        } catch (Exception e) {}

        try {
            setPatternTicksPerBeat(XMLUtils.parseInteger(random, "ticksPerBeat", node));
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
        // maps base pattern characters (e.g., 'A') to patterns
        Map<Character, String> basePatternMap = new HashMap<Character, String>();

        // maps pattern strings (e.g., 'A1') to patterns
        Map<String, String> patternMap = new HashMap<String, String>();

        // contains all patterns generated so far
        Set<String> patternSet = new HashSet<String>();

        StringBuilder totalPattern = new StringBuilder();

        String[] patterns = patternPattern.split(",");

        for (String patternString : patterns) {
            String p;

            if (patternString.equals("-")) {
                p = "-/" + patternTicks;
            } else if (patternString.length() == 2) {
                p = patternMap.get(patternString);

                if (p == null) {
                    // the pattern string is unknown, check if there already is a base pattern

                    char basePatternCharacter = patternString.charAt(0);
                    String basePattern = basePatternMap.get(basePatternCharacter);

                    if (basePattern == null) {
                        // no base pattern is found, create one and use it
                        p = generateBasePattern(songContext);
                        basePatternMap.put(basePatternCharacter, p);
                    } else {
                        // we have a base pattern
                        // generate a modified pattern until we find one that we haven't used so far

                        int tries = 0;

                        do {
                            tries++;

                            if (tries > 10000) {
                                throw new RuntimeException("Could not create non-used variation of pattern");
                            }

                            p = modifyPattern(basePattern, 2);
                        } while (uniquePatternParts && patternSet.contains(p));
                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug("Pattern " + patternString + ": " + p);
                    }

                    patternMap.put(patternString, p);
                    patternSet.add(p);
                }
            } else {
                throw new RuntimeException("Pattern part \"" + patternString + "\" is invalid (2 characters required)");
            }

            if (totalPattern.length() > 0) {
                totalPattern.append(',');
            }

            totalPattern.append(p);
        }

        return totalPattern.toString();
    }

    /**
     * Creates and returns a random pattern.
     * 
     * @param songContext the song context
     *
     * @return the random pattern
     */

    private String generateBasePattern(SongContext songContext) {
        StringBuilder sb;
        int activeTicks = 0;

        do {
            sb = new StringBuilder();
            activeTicks = 0;

            final int minPitch = findMinimum(offsets);
            final int maxPitch = findMaximum(offsets);
            final int pitchDiff = maxPitch - minPitch;

            boolean currentIsNote;
            boolean nextIsNote = random.nextDouble() < noteProbability;

            int previousPitch = 0;

            int i = 0;
            while (i < patternTicks) {
                currentIsNote = nextIsNote;
                nextIsNote = random.nextDouble() < noteProbability;

                int length;

                if (currentIsNote) {
                    length = noteLengths[random.nextInt(noteLengths.length)];
                } else {
                    length = pauseLengths[random.nextInt(pauseLengths.length)];
                }

                if (i + length > patternTicks) {
                    length = patternTicks - i;
                }

                if (currentIsNote) {
                    PatternEntry entry = offsets[random.nextInt(offsets.length)];
                    boolean isWildcard = entry.isWildcard;

                    double v = (1.0d - pitchVelocityCorrelation) * random.nextDouble() + (pitchDiff == 0 ? 0
                            : pitchVelocityCorrelation * ((isWildcard ? previousPitch : entry.offset) - minPitch) / pitchDiff);

                    int velocity = (int) RandomUtils.getPowerDouble(v, minVelocity, maxVelocity, velocityExponent);

                    boolean isLegato = nextIsNote && i + length < patternTicks && random.nextDouble() < legatoProbability;

                    if (isLegato) {
                        if (isWildcard) {
                            sb.append(entry.wildcard).append("~/");
                        } else {
                            sb.append(entry.offset).append("~/");
                        }
                    } else {
                        if (isWildcard) {
                            sb.append(entry.wildcard).append('/');
                        } else {
                            sb.append(entry.offset).append('/');
                        }
                    }

                    if (velocity != songContext.getStructure().getMaxVelocity()) {
                        sb.append(length).append(':').append(velocity);
                    } else {
                        sb.append(length);
                    }

                    activeTicks += length;

                    if (!isWildcard) {
                        previousPitch = entry.offset;
                    }
                } else {
                    sb.append("-/").append(length);
                }

                i += length;

                if (i < patternTicks) {
                    sb.append(',');
                }
            }
        } while (activeTicks < minActiveTicks || activeTicks > maxActiveTicks);

        return sb.toString();
    }

    /**
     * Takes the given pattern and returns a modified version of it by doing the given number of random swaps. It is possible for the pattern to be
     * identical to the original pattern.
     * 
     * @param pattern the original pattern
     * @param modifications the number of modifications
     * 
     * @return the modified pattern
     */

    private String modifyPattern(String pattern, int modifications) {
        String[] values = pattern.split(",");
        int length = values.length;

        for (int i = 0; i < modifications; i++) {
            int x;
            int y;

            do {
                x = random.nextInt(length);
                y = random.nextInt(length);
            } while (x == y && length > 1);

            String z = values[x];
            values[x] = values[y];
            values[y] = z;
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(values[i]);

            if (i < length - 1) {
                sb.append(',');
            }
        }

        return sb.toString();
    }

    /**
     * Finds the maximum integer from the given list of ints. If the list is empty, Integer.MIN_VALUE is returned.
     * 
     * @param list the list of ints
     * 
     * @return the maximum integer from the list
     */

    private int findMaximum(PatternEntry[] list) {
        int maximum = Integer.MIN_VALUE;
        int num = list.length;

        for (int i = 0; i < num; i++) {
            if (!list[i].isWildcard) {
                maximum = Math.max(list[i].offset, maximum);
            }
        }

        return maximum;
    }

    /**
     * Finds the minimum integer from the given list of ints. If the list is empty, Integer.MAX_VALUE is returned.
     * 
     * @param list the list of ints
     * 
     * @return the minimum integer from the list
     */

    private int findMinimum(PatternEntry[] list) {
        int minimum = Integer.MAX_VALUE;
        int num = list.length;

        for (int i = 0; i < num; i++) {
            if (!list[i].isWildcard) {
                minimum = Math.min(list[i].offset, minimum);
            }
        }

        return minimum;
    }

    public void setPatternTicks(int patternTicks) {
        this.patternTicks = patternTicks;
    }

    public void setNoteProbability(double noteProbability) {
        this.noteProbability = noteProbability;
    }

    public void setLegatoProbability(double legatoProbability) {
        this.legatoProbability = legatoProbability;
    }

    public void setOffsets(PatternEntry[] offsets) {
        this.offsets = offsets;
    }

    public void setMinVelocity(double minVelocity) {
        this.minVelocity = minVelocity;
    }

    public void setMaxVelocity(double maxVelocity) {
        this.maxVelocity = maxVelocity;
    }

    @Override
    public void setPatternString(String patternString) {
        this.patternString = patternString;
    }

    public void setNoteLengths(int[] noteLengths) {
        this.noteLengths = noteLengths;
    }

    public void setPauseLengths(int[] pauseLengths) {
        this.pauseLengths = pauseLengths;
    }

    public void setPitchVelocityCorrelation(double pitchVelocityCorrelation) {
        this.pitchVelocityCorrelation = pitchVelocityCorrelation;
    }

    public void setVelocityExponent(double velocityExponent) {
        this.velocityExponent = velocityExponent;
    }

    public void setMinActiveTicks(int minActiveTicks) {
        this.minActiveTicks = minActiveTicks;
    }

    public void setMaxActiveTicks(int maxActiveTicks) {
        this.maxActiveTicks = maxActiveTicks;
    }

    /**
     * Parses an XML node consisting of pattern entries.
     * 
     * @param random the random generator
     * @param path the XML path
     * @param parentNode the parent node
     * @return the array of pattern entries
     */

    private static PatternEntry[] parsePatternEntryListString(Random random, String path, Node parentNode) {
        String string = XMLUtils.parseString(random, path, parentNode);

        if (string == null || string.equals("")) {
            return null;
        }

        String[] stringArray = string.split(",");
        int length = stringArray.length;

        PatternEntry[] array = new PatternEntry[length];

        for (int i = 0; i < length; i++) {
            char c = stringArray[i].charAt(0);
            if (c == '-' || Character.isDigit(c)) {
                array[i] = new PatternEntry(Integer.parseInt(stringArray[i]));
            } else {
                array[i] = new PatternEntry(stringArray[i].charAt(0));
            }
        }

        return array;
    }

    public void setUniquePatternParts(boolean uniquePatternParts) {
        this.uniquePatternParts = uniquePatternParts;
    }

    /**
     * Immutable container for pattern entries.
     */

    private static final class PatternEntry {
        /** The offset. */
        private int offset;

        /** The wildcard character. */
        private char wildcard;

        /** The wildcard flag. */
        private boolean isWildcard;

        /**
         * Constructor.
         * 
         * @param offset the offset
         */

        private PatternEntry(int offset) {
            this.offset = offset;
        }

        /**
         * Constructor.
         * 
         * @param wildcard the wildcard character
         */

        private PatternEntry(char wildcard) {
            this.wildcard = wildcard;
            this.isWildcard = true;
        }
    }
}
