package com.soundhelix.patternengine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.misc.Pattern;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a generic sequence engine that generates a random pattern, with random
 * pitches, random note and pause lengths, random legato and random velocity, as well as the ability to repeat
 * subpatterns.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

// FIXME: endless loops are possible in certain cases

public class RandomFragmentPatternEngine extends StringPatternEngine {

    /** The random generator. */
    private Random random;
    
    /**
     * The length of the base pattern. The total length of the pattern is this value times the length of the pattern
     * string.
     */
    private int patternTicks = 16;

    /**
     * The comma-separated pattern used to generate the final pattern. Equal character pairs refer to equal patterns,
     * different character pairs to different patterns. The first character defines the base pattern, the second
     * character the variation of the base pattern. For example, the string "A1,A2,A1,B1" will will generate 2 base
     * patterns (one each for A and B, called "A1" and "B1") and a variation of base pattern A called "A2".
     */
    private String patternString = "A1,A2,A1,B1";

    private Map<Character, String[]> patternStringMap;
    
    public void configure(Node node, XPath xpath) throws XPathException {
        random = new Random(randomSeed);
        
        setPatternTicks(XMLUtils.parseInteger(random, "patternTicks", node, xpath));
        setPatternString(XMLUtils.parseString(random, "patternString", node, xpath));

        NodeList nodeList = (NodeList) xpath.evaluate("pattern", node, XPathConstants.NODESET);
        int patterns = nodeList.getLength();

        patternStringMap = new HashMap<Character, String[]>(patterns);
        
        for (int i = 0; i < patterns; i++) {
            String group = XMLUtils.parseString(random, "attribute::group", nodeList.item(i), xpath);
            
            if (group == null || group.length() != 1) {
                throw new RuntimeException("Need exactly one character, got \"" + group + "\"");
            }        
            
            char character = group.charAt(0);
            
            if (patternStringMap.containsKey(character)) {
                throw new RuntimeException("Pattern string for group \"" + character + "\" defined more than once");
            }
            
            String[] patternArray = XMLUtils.parseStringList(random, nodeList.item(i), '|', xpath);
            
            if (patternArray == null || patternArray.length == 0) {
                throw new RuntimeException("Pattern string for group \"" + character + "\" is empty");
            }
            
            patternStringMap.put(character, patternArray);
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
        // maps base pattern characters (e.g., 'A') to patterns
        Map<Character, String> basePatternMap = new HashMap<Character, String>();
        
        // maps pattern strings (e.g., 'A1') to patterns 
        Map<String, String> patternMap = new HashMap<String, String>();

        // contains all patterns generated so far
        Set<String> patternSet = new HashSet<String>();
        StringBuilder totalPattern = new StringBuilder();

        
        String[] patterns = patternPattern.split(",");

        for (String patternString : patterns) {
            if (patternString.length() != 2) {
                throw new RuntimeException("Pattern part \"" + patternString + "\" is invalid (2 characters required)");
            }
            
            String p = patternMap.get(patternString);
            
            if (p == null) {
                // the pattern string is unknown, check if there already is a base pattern
                
                char basePatternCharacter = patternString.charAt(0);            
                String basePattern = basePatternMap.get(basePatternCharacter);
                
                if (basePattern == null) {
                    // no base pattern is found, create one and use it
                    p = generateBasePattern(basePatternCharacter);
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

                        p = generateBasePattern(basePatternCharacter);                        
                    } while(patternSet.contains(basePatternCharacter + p));
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("Pattern " + patternString + ": " + p);                
                }
                
                patternMap.put(patternString, p);
                patternSet.add(basePatternCharacter + p);
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
     * @return the random pattern
     */
    
    private String generateBasePattern(char character) {
        String[] patterns = patternStringMap.get(character);
        
        if (patterns == null || patterns.length == 0) {
            throw new RuntimeException("Patterns for character \"" + character + "\" not found");
        }

        int patternCount = patterns.length;
        
        StringBuilder sb = new StringBuilder();
        int activeTicks = 0;
        
        while (activeTicks < patternTicks) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            
            String patternString = patterns[random.nextInt(patternCount)];
            int stringTicks = Pattern.getStringTicks(patternString);
            
            sb.append(patternString);
            activeTicks += stringTicks;
            
            if (activeTicks > patternTicks) {
                throw new RuntimeException("Generated pattern is longer than " + patternTicks + " ticks");
            }            
        }

        return sb.toString();
    }
    
    /**
     * Finds the maximum integer from the given list of ints.
     * If the list is empty, Integer.MIN_VALUE is returned.
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
     * Finds the minimum integer from the given list of ints.
     * If the list is empty, Integer.MAX_VALUE is returned.
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
    
    public void setPatternString(String patternString) {
        this.patternString = patternString;
    }

    private static PatternEntry[] parsePatternEntryListString(Random random, String path, Node parentNode, XPath xpath) {
        String string = XMLUtils.parseString(random, path, parentNode, xpath);

        if (string == null || string.equals("")) {
            return null;
        }

        String[] stringArray = string.split(",");
        int length = stringArray.length;

        PatternEntry[] array = new PatternEntry[length];

        for (int i = 0; i < length; i++) {
            if (Character.isDigit(stringArray[i].charAt(0))) {
                array[i] = new PatternEntry(Integer.parseInt(stringArray[i]));
            } else {
                array[i] = new PatternEntry(stringArray[i].charAt(0));
            }
        }

        return array;
    }

    private static final class PatternEntry {
        /** The offset. */
        private int offset;
        
        /** The wildcard character. */
        private char wildcard;
        
        /** The wildcard flag. */
        private boolean isWildcard;
        
        private PatternEntry(int offset) {
            this.offset = offset;
        }
        
        private PatternEntry(char wildcard) {
            this.wildcard = wildcard;
            this.isWildcard = true;
        }        
    }
}
