package com.soundhelix.sequenceengine;

import java.util.Arrays;
import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.harmonyengine.HarmonyEngine;
import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.Pattern.PatternEntry;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.patternengine.PatternEngine;
import com.soundhelix.util.RandomUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a sequence engine for drum machines. Drum machines normally play a certain
 * sample (e.g., a base drum or a snare) when a certain pitch is played. This class supports an
 * arbitrary number of combinations of patterns, pitches and activity groups.
 * Each pattern acts as a voice for a certain pitch. The activity group can be used to
 * group the voices together so that they are all active or all silent at the same time.
 * For example, you might group three hi-hat patterns together so that all 3 are active
 * or silent. The activity groups must be numbered starting from 0, and the used groups must be
 * "dense" (i.e., without gaps).
 *
 * <h3>XML configuration</h3>
 * <table border=1>
 * <tr><th>Tag</th> <th>#</th> <th>Attributes</th> <th>Description</th> <th>Required</th>
 * <tr><td><code>pattern</code></td> <td>+</td> <td><code>pitch</code>, <code>activityGroup</code></td> <td>Defines the pattern to use with the given pitch. The pattern is put into the given activity group.</td> <td>yes</td>
 * </table>
 *
 * <h3>Configuration example</h3>
 * 
 * The following example uses 6 patterns with 4 activity groups:
 * <br>
 * <pre>
 * {@literal
 * <sequenceEngine class="DrumSequenceEngine">
 *   <!-- base drum -->
 *   <pattern pitch="36" activityGroup="0">10001000100010001000100010001001100010001000100010001000100010101000100010001000100010001000100110001000100010001000100011101011</pattern>
 *   <!-- clap -->
 *   <pattern pitch="37" activityGroup="1">00001000000010000000100000001000000010000000100000001000000110100000100000001000000010000000100000001000000010000000100001001000</pattern>
 *   <!-- closed hi-hat -->
 *   <pattern pitch="40" activityGroup="2">10001000100010001100100010001000</pattern>
 *   <!-- open hi-hat -->
 *   <pattern pitch="44" activityGroup="2">0010001000100010001000100010001000100010001000100010001000100101</pattern>
 *   <!-- other hi-hat -->
 *   <pattern pitch="39" activityGroup="2">001</pattern>
 *   <!-- snare -->
 *   <pattern pitch="42" activityGroup="3">00001000010010000000100001001010000010000100100000001000010010110000100001001000000010010100101000001000010010000000100101001011</pattern>
 * </sequenceEngine>
 * }
 * </pre>
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class DrumSequenceEngine extends AbstractSequenceEngine {

    private static final int MODE_ADD = 0;
    private static final int MODE_REPLACE = 1;

    private DrumEntry[] drumEntries;
    private ConditionalEntry[] conditionalEntries;
    
    private Random random;
    
    public DrumSequenceEngine() {
        super();
    }
        
    public void setDrumEntries(DrumEntry[] drumEntries) {
        this.drumEntries = drumEntries;
    }

    public void setConditionalEntries(ConditionalEntry[] conditionalEntries) {
        this.conditionalEntries = conditionalEntries;
    }

    public Track render(ActivityVector[] activityVectors) {
        HarmonyEngine harmonyEngine = structure.getHarmonyEngine();        
        int ticks = structure.getTicks();
        int drumEntryCount = drumEntries.length;

        Sequence[] seqs = new Sequence[drumEntryCount];

        for (int i = 0; i < drumEntryCount; i++) {
            seqs[i] = new Sequence();
        }

        Track track = new Track(TrackType.RHYTHM);

        for (int i = 0; i < drumEntryCount; i++) {
            ActivityVector activityVector = activityVectors[i];
            Sequence seq = seqs[i];
            Pattern pattern = drumEntries[i].pattern;

            // the pitch is constant
            int pitch = drumEntries[i].pitch;

            int patternLength = pattern.size();
            int pos = 0;
            int tick = 0;

            while (tick < ticks) {
                Pattern.PatternEntry entry = pattern.get(pos % patternLength);
                int len = entry.getTicks();

                if (activityVector.isActive(tick)) {
                    short vel = entry.getVelocity();

                    if (entry.isPause()) {
                        // add pause
                        seq.addPause(len);
                    } else {
                        boolean useLegato = entry.isLegato() ? pattern.isLegatoLegal(activityVector, tick + len,
                                pos + 1) : false;
                        seq.addNote(pitch, len, vel, useLegato);
                    }
                } else {
                    // add pause
                    seq.addPause(len);
                }

                tick += len;
                pos++;
            }
            track.add(seq);
        }

        int conditionalEntryCount = conditionalEntries.length;

    next:
            
        for (int i = 0; i < conditionalEntryCount; i++) {
            int[] targets = conditionalEntries[i].targets;
            java.util.regex.Pattern condition = conditionalEntries[i].condition;
            int mode = conditionalEntries[i].mode;
            double probability = conditionalEntries[i].probability;
            Pattern pattern = conditionalEntries[i].pattern;
            int patternTicks = pattern.getTicks();

            // start with the second chord section
            int tick = harmonyEngine.getChordSectionTicks(0);

            String previousActivity = getActivityString(tick - 1, activityVectors);

            // the tick where the pattern was applied last (last tick of the pattern + 1)
            int lastMatchedTick = Integer.MIN_VALUE;

            while (true) {
                // jump to the next position where the condition is true and the pattern does not overlap
                // with the pattern's previous application

                while (tick < ticks) {
                    String activity = getActivityString(tick, activityVectors);
                    String totalActivity = previousActivity + activity; 

                    previousActivity = activity;

                    if (tick - patternTicks >= lastMatchedTick) {
                        if (condition.matcher(totalActivity).matches()) {
                            break;
                        }
                    }

                    tick += harmonyEngine.getChordSectionTicks(tick);
                }

                if (tick >= ticks) {
                    continue next;
                }

                if (tick - patternTicks >= 0) {
                    if (RandomUtils.getBoolean(random, probability)) {                        
                        // jump back to where the pattern will start
                        tick -= patternTicks;

                        logger.debug("Applying conditional pattern " + i + " with length " + patternTicks
                                + " for targets " + Arrays.toString(targets) + " at ticks " + tick + "-"
                                + (tick + patternTicks - 1));

                        int len = pattern.size();

                        for (int k = 0; k < len; k++) {
                            PatternEntry entry = pattern.get(k);

                            for (int j = 0; j < targets.length; j++) {
                                Sequence seq = seqs[targets[j]];
                                int pitch = drumEntries[targets[j]].pitch;

                                if (entry.isNote()) {
                                    seq.replaceEntry(tick, new Sequence.SequenceEntry(pitch, entry.getVelocity(),
                                            entry.getTicks(), entry.isLegato()));
                                } else if (mode == MODE_REPLACE) {
                                    seq.replaceEntry(tick, new Sequence.SequenceEntry(Integer.MIN_VALUE, (short) -1,
                                            entry.getTicks(), entry.isLegato()));
                                }
                            }
                            tick += entry.getTicks();
                        }

                        lastMatchedTick = tick;
                    }
                }

                tick += harmonyEngine.getChordSectionTicks(tick);
            }
        }       

        return track;
    }
    
    /**
     * Returns the activity string of the given tick. The activity string is a concatenation of '0' and '1'
     * characters specifying whether the ActivityVectors from the list are active or not. If the tick is negative,
     * the returned string consists of only '0' characters.
     * 
     * @param tick the tick to check
     * @param activityVectors the array of ActivityVectors
     * 
     * @return the activity string
     */
    
    private String getActivityString(int tick, ActivityVector[] activityVectors) {
        StringBuilder sb = new StringBuilder(activityVectors.length);
        
        for (ActivityVector av : activityVectors) {
            if (tick >= 0 && av.isActive(tick)) {
                sb.append('1');
            } else {
                sb.append('0');
            }
        }
        
        return sb.toString();
    }
    
    public int getActivityVectorCount() {
        return drumEntries.length;
    }
    
    public void configure(Node node, XPath xpath) throws XPathException {
        random = new Random(randomSeed);
        
        NodeList nodeList = (NodeList) xpath.evaluate("pattern", node, XPathConstants.NODESET);
        int patterns = nodeList.getLength();
        
        DrumEntry[] drumEntries = new DrumEntry[patterns];
        
        if (patterns == 0) {
            throw new RuntimeException("Need at least 1 pattern");
        }
        
        for (int i = 0; i < patterns; i++) {
            int pitch = XMLUtils.parseInteger(random, "pitch", nodeList.item(i), xpath);
            
            Node patternEngineNode = (Node) xpath.evaluate("patternEngine", nodeList.item(i), XPathConstants.NODE);
        
            PatternEngine patternEngine;
            
            try {
                patternEngine = XMLUtils.getInstance(PatternEngine.class, patternEngineNode,
                        xpath, randomSeed ^ 47351842858L);
            } catch (Exception e) {
                throw new RuntimeException("Error instantiating PatternEngine", e);
            }
            
            Pattern pattern = patternEngine.render("");
            drumEntries[i] = new DrumEntry(pattern, pitch);
        }

        setDrumEntries(drumEntries);

        nodeList = (NodeList) xpath.evaluate("conditionalPattern", node, XPathConstants.NODESET);
        patterns = nodeList.getLength();
        
        ConditionalEntry[] conditionalEntries = new ConditionalEntry[patterns];
        
        for (int i = 0; i < patterns; i++) {
            String targetString = XMLUtils.parseString(random, "target", nodeList.item(i), xpath);
            String[] targetStrings = targetString.split(",");
            int[] targets = new int[targetStrings.length];
            
            for (int k = 0; k < targetStrings.length; k++) {
                targets[k] = Integer.parseInt(targetStrings[k]);
            }

            String conditionString = XMLUtils.parseString(random, "condition", nodeList.item(i), xpath);
            conditionString = conditionString.replaceAll(">", "").replaceAll(",", "|").replaceAll("-", ".");
            java.util.regex.Pattern condition = java.util.regex.Pattern.compile(conditionString);
            
            String modeString = XMLUtils.parseString(random, "mode", nodeList.item(i), xpath);
            int mode;
            
            if (modeString.equals("add")) {
                mode = MODE_ADD;
            } else if (modeString.equals("replace")) {
                mode = MODE_REPLACE;
            } else {
                throw new RuntimeException("Unknown mode \"" + modeString + "\"");
            }
            
            double probability = XMLUtils.parseDouble(random, "probability", nodeList.item(i), xpath) / 100.0d;

            Node patternEngineNode = (Node) xpath.evaluate("patternEngine", nodeList.item(i), XPathConstants.NODE);
        
            PatternEngine patternEngine;
            
            try {
                patternEngine = XMLUtils.getInstance(PatternEngine.class, patternEngineNode,
                        xpath, randomSeed ^ 47351842858L);
            } catch (Exception e) {
                throw new RuntimeException("Error instantiating PatternEngine", e);
            }

            Pattern pattern = patternEngine.render("");
            
            conditionalEntries[i] = new ConditionalEntry(pattern, condition, mode, targets, probability);
        }
        
        setConditionalEntries(conditionalEntries);
    }
    
    /**
     * Matches a string representing the current activity state with the given pattern.
     * The activity state string must consist only of the digits 0 and 1, the pattern must consist of
     * the same digits plus a minus character.
     * 
     * @param value the value string
     * @param pattern the pattern string
     * 
     * @return true if the value matches the pattern, false otherwise
     */
    
    private boolean matchesPattern(String value, String pattern) {
        if (value == null || pattern == null) {
            return false;
        }
        
        int len = pattern.length();
        
        if (len != value.length()) {
            throw new RuntimeException("The activity value \"" + value + "\" is not compatible with the pattern \""
                                       + pattern + "\"");
        }
        
        for (int i = 0; i < len; i++) {
            char c = pattern.charAt(i);
            switch (c) {
                case '0':
                case '1':
                    if (value.charAt(i) != c) {
                        return false;
                    }
                    break;
                case '-':
                    // acts as a wildcard
                    break;
                default:
                    throw new RuntimeException("Invalid pattern character \"" + c + "\" in pattern \"" + pattern + "\"");
            }
        }
        
        return true;
    }
    
    private static final class DrumEntry {
        private final Pattern pattern;
        private final int pitch;
        
        private DrumEntry(Pattern pattern, int pitch) {
            this.pattern = pattern;
            this.pitch = pitch;
        }
    }
    
    private static final class ConditionalEntry {
        private final Pattern pattern;
        private final java.util.regex.Pattern condition;
        private final int mode;
        private final int[] targets;
        private final double probability;
        
        private ConditionalEntry(Pattern pattern, java.util.regex.Pattern condition, int mode, int[] targets,
                                 double probability) {
            this.pattern = pattern;
            this.condition = condition;
            this.mode = mode;
            this.targets = targets;
            this.probability = probability;
        }
    }
}
