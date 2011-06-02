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
import com.soundhelix.misc.Structure;
import com.soundhelix.misc.Pattern.PatternEntry;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.patternengine.PatternEngine;
import com.soundhelix.util.RandomUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a sequence engine for drum machines. Drum machines normally play a certain sample (e.g., a base drum or
 * a snare) when a certain pitch is played. This class supports an arbitrary number of combinations of patterns,
 * pitches and activity vectors. Each pattern acts as a voice for a certain pitch.
 * 
 * Conditional patterns allow the modification of the generated sequences when certain conditions are met, e.g., for
 * generating fill-ins, crescendos, etc. when monitored ActivityVectors become active or inactive.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class DrumSequenceEngine extends AbstractSequenceEngine {

    /** Conditional pattern add mode. */
    private static final int MODE_ADD = 0;
    
    /** Conditional pattern replace mode. */
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
        int drumEntryCount = drumEntries.length;

        Track track = new Track(TrackType.RHYTHM);
        Sequence[] seqs = new Sequence[drumEntryCount];


        for (int i = 0; i < drumEntryCount; i++) {
            seqs[i] = new Sequence();
            track.add(seqs[i]);
        }

        processPatterns(activityVectors, seqs, structure);
        processConditionalPatterns(activityVectors, seqs, structure);       
        
        return track;
    }

    /**
     * Process all non-conditional patterns.
     * 
     * @param activityVectors the array of activity vectors
     * @param seqs the array of sequences
     * @param structure the structure
     */
    
    private void processPatterns(ActivityVector[] activityVectors, Sequence[] seqs, Structure structure) {
        int ticks = structure.getTicks();
        int drumEntryCount = drumEntries.length;
        
        for (int i = 0; i < drumEntryCount; i++) {
            ActivityVector activityVector = activityVectors[i];
            Sequence seq = seqs[i];
            Pattern pattern = drumEntries[i].pattern;

            // the base pitch is constant
            int basePitch = drumEntries[i].pitch;

            int patternLength = pattern.size();
            int pos = 0;
            int tick = 0;

            while (tick < ticks) {
                Pattern.PatternEntry entry = pattern.get(pos % patternLength);
                int pitch = entry.getPitch();
                int len = entry.getTicks();

                if (activityVector.isActive(tick)) {
                    short vel = entry.getVelocity();

                    if (entry.isPause()) {
                        // add pause
                        seq.addPause(len);
                    } else {
                        boolean useLegato = entry.isLegato() ? pattern.isLegatoLegal(activityVector, tick + len,
                                pos + 1) : false;
                        seq.addNote(basePitch + pitch, len, vel, useLegato);
                    }
                } else {
                    // add pause
                    seq.addPause(len);
                }

                tick += len;
                pos++;
            }
        }
    }
    
    /**
     * Process all conditional patterns.
     * 
     * @param activityVectors the array of activity vectors
     * @param seqs the array of sequences
     * @param structure the structure
     */

    private void processConditionalPatterns(ActivityVector[] activityVectors, Sequence[] seqs, Structure structure) {
        HarmonyEngine harmonyEngine = structure.getHarmonyEngine();
        int ticks = structure.getTicks();
        int conditionalEntryCount = conditionalEntries.length;

        // the tick where each pattern was applied last (last tick of the pattern + 1), initially 0
        // (used to avoid overlapping application of patterns)
        int[] lastMatchedTick = new int[conditionalEntryCount];

        // start with the second chord section
        int tick = harmonyEngine.getChordSectionTicks(0);

        // get the activity string of the first chord section
        String previousActivity = getActivityString(0, activityVectors);

        while (tick < ticks) {
            String activity = getActivityString(tick, activityVectors);
            String totalActivity = previousActivity + activity; 

            for (int i = 0; i < conditionalEntryCount; i++) {
                Pattern pattern = conditionalEntries[i].pattern;
                int patternTicks = pattern.getTicks();
                double probability = conditionalEntries[i].probability;
                java.util.regex.Pattern condition = conditionalEntries[i].condition;

                if (tick - patternTicks >= lastMatchedTick[i] && RandomUtils.getBoolean(random, probability)
                        && condition.matcher(totalActivity).matches()) {
                    // all conditions met, apply pattern
                    
                    // jump back to where the pattern will start
                    tick -= patternTicks;
                    
                    int[] targets = conditionalEntries[i].targets;
                    int mode = conditionalEntries[i].mode;

                    logger.debug("Applying conditional pattern " + i + " with length " + patternTicks
                            + " for targets " + Arrays.toString(targets) + " at ticks " + tick + "-"
                            + (tick + patternTicks - 1));

                    int len = pattern.size();

                    for (int k = 0; k < len; k++) {
                        PatternEntry entry = pattern.get(k);

                        for (int j = 0; j < targets.length; j++) {
                            Sequence seq = seqs[targets[j]];
                            int basePitch = drumEntries[targets[j]].pitch;

                            if (entry.isNote()) {
                                seq.replaceEntry(tick, new Sequence.SequenceEntry(basePitch + entry.getPitch(),
                                        entry.getVelocity(), entry.getTicks(), entry.isLegato()));
                            } else if (mode == MODE_REPLACE) {
                                seq.replaceEntry(tick, new Sequence.SequenceEntry(Integer.MIN_VALUE, (short) -1,
                                        entry.getTicks(), entry.isLegato()));
                            }
                        }
                        
                        tick += entry.getTicks();
                    }

                    lastMatchedTick[i] = tick;
                }
            }

            tick += harmonyEngine.getChordSectionTicks(tick);
            previousActivity = activity;
        }
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
                        xpath, randomSeed, i);
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
                        xpath, randomSeed, -i - 1);
            } catch (Exception e) {
                throw new RuntimeException("Error instantiating PatternEngine", e);
            }

            Pattern pattern = patternEngine.render("");
            
            conditionalEntries[i] = new ConditionalEntry(pattern, condition, mode, targets, probability);
        }
        
        setConditionalEntries(conditionalEntries);
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
