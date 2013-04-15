package com.soundhelix.component.sequenceengine.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.component.patternengine.PatternEngine;
import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Harmony;
import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.Pattern.PatternEntry;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.SongContext;
import com.soundhelix.misc.Structure;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.util.HarmonyUtils;
import com.soundhelix.util.RandomUtils;
import com.soundhelix.util.StringUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a sequence engine for drum machines. Drum machines normally play a certain sample (e.g., a base drum or a snare) when a certain pitch is
 * played. This class supports an arbitrary number of combinations of patterns, pitches and activity vectors. Each pattern acts as a voice for a
 * certain pitch.
 *
 * Conditional patterns allow the modification of the generated sequences when certain conditions are met, e.g., for generating fill-ins, crescendos,
 * etc. when monitored ActivityVectors become active or inactive.
 *
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class DrumSequenceEngine extends AbstractSequenceEngine {

    /** The conditional modes. */
    private enum Mode {
        /** The add mode. */
        ADD,
        /** The replace mode. */
        REPLACE
    }

    /** The drum entries. */
    private DrumEntry[] drumEntries;

    /** The conditional entries. */
    private ConditionalDrumEntry[] conditionalEntries;

    /** The random generator. */
    private Random random;

    public DrumSequenceEngine() {
        super();
    }

    public void setDrumEntries(DrumEntry[] drumEntries) {
        this.drumEntries = drumEntries;
    }

    public void setConditionalEntries(ConditionalDrumEntry[] conditionalEntries) {
        this.conditionalEntries = conditionalEntries;
    }

    @Override
    public Track render(SongContext songContext, ActivityVector[] activityVectors) {
        int drumEntryCount = drumEntries.length;

        Track track = new Track(TrackType.RHYTHMIC);
        Sequence[] seqs = new Sequence[drumEntryCount];

        for (int i = 0; i < drumEntryCount; i++) {
            seqs[i] = new Sequence();
            track.add(seqs[i]);
        }

        processPatterns(songContext, activityVectors, seqs);
        processConditionalPatterns(songContext, activityVectors, seqs);

        return track;
    }

    /**
     * Process all non-conditional patterns.
     *
     * @param songContext the song context
     * @param activityVectors the array of activity vectors
     * @param seqs the array of sequences
     */

    private void processPatterns(SongContext songContext, ActivityVector[] activityVectors, Sequence[] seqs) {
        int ticks = songContext.getStructure().getTicks();
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
                        boolean useLegato = entry.isLegato() ? pattern.isLegatoLegal(activityVector, tick + len, pos + 1) : false;
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
     * @param songContext the song context
     * @param activityVectors the array of activity vectors
     * @param seqs the array of sequences
     */

    private void processConditionalPatterns(SongContext songContext, ActivityVector[] activityVectors, Sequence[] seqs) {
        Structure structure = songContext.getStructure();
        Harmony harmony = songContext.getHarmony();
        int ticks = structure.getTicks();
        int chordSections = HarmonyUtils.getChordSectionCount(songContext);
        int conditionalEntryCount = conditionalEntries.length;

        Map<String, List<Integer>> map = new HashMap<String, List<Integer>>();

        for (int i = 0; i < activityVectors.length; i++) {
            ActivityVector av = activityVectors[i];
            List<Integer> list = map.get(av.getName());
            if (list == null) {
                list = new ArrayList<Integer>();
                map.put(av.getName(), list);
            }

            list.add(i);
        }

        for (ConditionalDrumEntry entry : conditionalEntries) {
            if (entry.preCondition == null) {
                entry.preCondition = getConditionPattern(map, drumEntries.length, entry.preConditionString);
                entry.postCondition = getConditionPattern(map, drumEntries.length, entry.postConditionString);
            }
        }

        // the tick where each pattern was applied last (last tick of the pattern + 1), initially 0
        // (used to avoid overlapping application of patterns)
        int[] lastMatchedTick = new int[conditionalEntryCount];

        // get all chord section activities

        String[] chordSectionActivity = new String[chordSections];
        int tick = 0;
        int x = 0;

        while (tick < ticks) {
            chordSectionActivity[x++] = getActivityString(tick, activityVectors);
            tick += harmony.getChordSectionTicks(tick);
        }

        // start with the second chord section
        tick = harmony.getChordSectionTicks(0);
        int chordSection = 1;

        while (tick < ticks) {
            String activity = chordSectionActivity[chordSection];

            for (int i = 0; i < conditionalEntryCount; i++) {
                Pattern pattern = conditionalEntries[i].pattern;
                int patternTicks = pattern.getTicks();
                double probability = conditionalEntries[i].probability;
                java.util.regex.Pattern preCondition = conditionalEntries[i].preCondition;
                java.util.regex.Pattern postCondition = conditionalEntries[i].postCondition;

                if (tick - patternTicks >= 0 && postCondition.matcher(activity).matches()) {
                    // check if all chord section boundaries crossed by the pattern (including the current one)
                    // fulfill the precondition

                    int mtick = tick - patternTicks;
                    int cs = HarmonyUtils.getChordSectionNumber(songContext, mtick);
                    boolean preConditionMatched = true;

                    while (mtick < tick && preConditionMatched) {
                        mtick += harmony.getChordSectionTicks(mtick);
                        if (!preCondition.matcher(chordSectionActivity[cs]).matches()) {
                            preConditionMatched = false;
                            break;
                        }
                        cs++;
                    }

                    if (preConditionMatched) {
                        if (tick - patternTicks >= lastMatchedTick[i] && RandomUtils.getBoolean(random, probability)) {
                            // all conditions met, apply pattern

                            // jump back to where the pattern will start
                            tick -= patternTicks;

                            int[] targets = conditionalEntries[i].targets;
                            Mode mode = conditionalEntries[i].mode;

                            logger.debug("Applying conditional pattern " + i + " with length " + patternTicks + " for targets "
                                    + Arrays.toString(targets) + " at ticks " + tick + "-" + (tick + patternTicks - 1));

                            int len = pattern.size();

                            for (int k = 0; k < len; k++) {
                                PatternEntry entry = pattern.get(k);

                                for (int j = 0; j < targets.length; j++) {
                                    Sequence seq = seqs[targets[j]];
                                    int basePitch = drumEntries[targets[j]].pitch;

                                    if (entry.isNote()) {
                                        seq.replaceEntry(
                                                tick,
                                                new Sequence.SequenceEntry(basePitch + entry.getPitch(), entry.getVelocity(), entry.getTicks(), entry
                                                        .isLegato()));
                                    } else if (mode == Mode.REPLACE) {
                                        seq.replaceEntry(tick,
                                                new Sequence.SequenceEntry(Integer.MIN_VALUE, (short) -1, entry.getTicks(), entry.isLegato()));
                                    }
                                }

                                tick += entry.getTicks();
                            }

                            lastMatchedTick[i] = tick;

                            i += conditionalEntries[i].skipWhenApplied;
                        } else {
                            // either the pattern would have overlapped or the random generator didn't agree to apply
                            // the pattern

                            i += conditionalEntries[i].skipWhenNotApplied;
                        }
                    }
                }
            }

            tick += harmony.getChordSectionTicks(tick);
            chordSection++;
        }
    }

    /**
     * Returns the activity string of the given tick. The activity string is a concatenation of '0' and '1' characters specifying whether the
     * ActivityVectors from the list are active or not. If the tick is negative, the returned string consists of only '0' characters.
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

    @Override
    public int getActivityVectorCount() {
        return drumEntries.length;
    }

    @Override
    public void configure(SongContext songContext, Node node) throws XPathException {
        random = new Random(randomSeed);

        NodeList nodeList = XMLUtils.getNodeList("pattern", node);
        int patterns = nodeList.getLength();

        DrumEntry[] drumEntries = new DrumEntry[patterns];

        if (patterns == 0) {
            throw new RuntimeException("Need at least 1 pattern");
        }

        for (int i = 0; i < patterns; i++) {
            int pitch = XMLUtils.parseInteger(random, "pitch", nodeList.item(i));

            Node patternEngineNode = XMLUtils.getNode("patternEngine", nodeList.item(i));

            PatternEngine patternEngine;

            try {
                patternEngine = XMLUtils.getInstance(songContext, PatternEngine.class, patternEngineNode, randomSeed, i);
            } catch (Exception e) {
                throw new RuntimeException("Error instantiating PatternEngine", e);
            }

            Pattern pattern = patternEngine.render(songContext, "");
            drumEntries[i] = new DrumEntry(pattern, pitch);
        }

        setDrumEntries(drumEntries);

        nodeList = XMLUtils.getNodeList("conditionalPattern", node);
        patterns = nodeList.getLength();

        ConditionalDrumEntry[] conditionalEntries = new ConditionalDrumEntry[patterns];

        for (int i = 0; i < patterns; i++) {
            String targetString = XMLUtils.parseString(random, "target", nodeList.item(i));
            String[] targetStrings = targetString.split(",");
            int[] targets = new int[targetStrings.length];

            for (int k = 0; k < targetStrings.length; k++) {
                targets[k] = Integer.parseInt(targetStrings[k]);
            }

            String conditionString = null;

            try {
                conditionString = XMLUtils.parseString(random, "condition", nodeList.item(i));
            } catch (Exception e) {
            }

            java.util.regex.Pattern preCondition = null;
            java.util.regex.Pattern postCondition = null;
            String preConditionString = null;
            String postConditionString = null;

            if (conditionString != null && !conditionString.equals("")) {
                String[] conditions = conditionString.split(">");
                conditions[0] = conditions[0].replaceAll(",", "|").replaceAll("-", ".");
                conditions[1] = conditions[1].replaceAll(",", "|").replaceAll("-", ".");
                preCondition = java.util.regex.Pattern.compile(conditions[0]);
                postCondition = java.util.regex.Pattern.compile(conditions[1]);

                // TODO remove support
                logger.warn("The tag \"condition\" is deprecated. Use the tags \"precondition\" and \"postcondition\" instead.");
            } else {
                // we cannot evaluate the patterns yet, because we don't have the list of ActivityVectors yet during the configure pass

                preConditionString = XMLUtils.parseString(random, "precondition", nodeList.item(i));
                postConditionString = XMLUtils.parseString(random, "postcondition", nodeList.item(i));
            }

            String modeString = XMLUtils.parseString(random, "mode", nodeList.item(i));
            Mode mode;

            if (modeString.equals("add")) {
                mode = Mode.ADD;
            } else if (modeString.equals("replace")) {
                mode = Mode.REPLACE;
            } else {
                throw new RuntimeException("Unknown mode \"" + modeString + "\"");
            }

            double probability = XMLUtils.parseDouble(random, "probability", nodeList.item(i)) / 100.0d;

            int skipWhenApplied = 0;

            try {
                skipWhenApplied = XMLUtils.parseInteger(random, "skipWhenApplied", nodeList.item(i));
            } catch (Exception e) {
            }

            if (i + 1 + skipWhenApplied > patterns || i + 1 + skipWhenApplied < 0) {
                throw new RuntimeException("Skip value \"" + skipWhenApplied + "\" would skip out of conditional pattern range");
            }

            int skipWhenNotApplied = 0;

            try {
                skipWhenNotApplied = XMLUtils.parseInteger(random, "skipWhenNotApplied", nodeList.item(i));
            } catch (Exception e) {
            }

            if (i + 1 + skipWhenNotApplied > patterns || i + 1 + skipWhenNotApplied < 0) {
                throw new RuntimeException("Skip value \"" + skipWhenNotApplied + "\" would skip out of conditonal pattern range");
            }

            Node patternEngineNode = XMLUtils.getNode("patternEngine", nodeList.item(i));

            PatternEngine patternEngine;

            try {
                patternEngine = XMLUtils.getInstance(songContext, PatternEngine.class, patternEngineNode, randomSeed, -i - 1);
            } catch (Exception e) {
                throw new RuntimeException("Error instantiating PatternEngine", e);
            }

            Pattern pattern = patternEngine.render(songContext, "");

            if (preCondition != null) {
                conditionalEntries[i] = new ConditionalDrumEntry(pattern, preCondition, postCondition, mode, targets, probability, skipWhenApplied,
                        skipWhenNotApplied);
            } else {
                conditionalEntries[i] = new ConditionalDrumEntry(pattern, preConditionString, postConditionString, mode, targets, probability,
                        skipWhenApplied, skipWhenNotApplied);
            }
        }

        setConditionalEntries(conditionalEntries);
    }

    /**
     * Parses the given condition string and returns a pattern representing the condition. The condition string is given as a comma-separated list of
     * ActivityVector names, which are each prefixed by either "+" or "-".
     *
     * @param map the map that maps ActivityVector names to lists of offsets
     * @param vectors the total number of ActivityVectors
     * @param string the condition string
     *
     * @return the condition pattern
     */

    private static java.util.regex.Pattern getConditionPattern(Map<String, List<Integer>> map, int vectors, String string) {
        // start with a regexp string consisting of only dots

        StringBuilder sb = new StringBuilder(vectors);

        for (int i = 0; i < vectors; i++) {
            sb.append('.');
        }

        if (string != null && !string.equals("")) {
            String[] stringParts = StringUtils.split(string, ',');

            for (String stringPart : stringParts) {
                char c = stringPart.charAt(0);
                String name = stringPart.substring(1);

                if (c == '+') {
                    c = '1';
                } else if (c == '-') {
                    c = '0';
                } else {
                    throw new IllegalArgumentException("Condition string part \"" + stringPart + "\" is invalid");
                }

                // set all characters at the offsets given by ActivityVector name to either "1" or "0"

                List<Integer> indexes = map.get(name);

                if (indexes == null) {
                    throw new IllegalArgumentException("Unknown ActitvityVector \"" + name + "\" referenced in condition pattern");
                }

                for (int index : indexes) {
                    sb.setCharAt(index, c);
                }
            }
        }

        return java.util.regex.Pattern.compile(sb.toString());
    }

    /**
     * Represents a normal drum entry.
     */
    
    private static final class DrumEntry {
        /** The pattern. */
        private final Pattern pattern;
        
        /** The pitch. */
        private final int pitch;

        /**
         * Constructor.
         * 
         * @param pattern the pattern
         * @param pitch the pitch
         */
        
        private DrumEntry(Pattern pattern, int pitch) {
            this.pattern = pattern;
            this.pitch = pitch;
        }
    }

     /**
      * Represents a conditional drum entry.
      */
     
    private static final class ConditionalDrumEntry {
        /** The pattern. */
        private final Pattern pattern;
        
        /** The precondition as a regular expression. */
        private java.util.regex.Pattern preCondition;
        
        /** The postcondition as a regular expression. */
        private java.util.regex.Pattern postCondition;
        
        /** The precondition as a string. */
        private final String preConditionString;
        
        /** The postcondition as a string. */
        private final String postConditionString;
        
        /** The mode. */
        private final Mode mode;
        
        /** The array of targets. */
        private final int[] targets;
        
        /** The probability for being applied if it could be applied. */
        private final double probability;
        
        /** The number of entries to skip after applying. */
        private final int skipWhenApplied;
        
        /** The number of entries to skip after not applying. */
        private final int skipWhenNotApplied;

        /**
         * Constructor.
         * 
         * @param pattern the pattern
         * @param preCondition the precondition
         * @param postCondition the postcondition
         * @param mode the mode
         * @param targets the targets
         * @param probability the probability
         * @param skipWhenApplied the number of items to skip after applying
         * @param skipWhenNotApplied the number of items to skip after not applying
         */
        
        private ConditionalDrumEntry(Pattern pattern, java.util.regex.Pattern preCondition, java.util.regex.Pattern postCondition, Mode mode,
                int[] targets, double probability, int skipWhenApplied, int skipWhenNotApplied) {
            this.pattern = pattern;
            this.preConditionString = null;
            this.postConditionString = null;
            this.preCondition = preCondition;
            this.postCondition = postCondition;
            this.mode = mode;
            this.targets = targets;
            this.probability = probability;
            this.skipWhenApplied = skipWhenApplied;
            this.skipWhenNotApplied = skipWhenNotApplied;
        }

        /**
         * Constructor.
         * 
         * @param pattern the pattern
         * @param preConditionString the precondition string
         * @param postConditionString the postcondition string
         * @param mode the mode
         * @param targets the targets
         * @param probability the probability
         * @param skipWhenApplied the number of items to skip after applying
         * @param skipWhenNotApplied the number of items to skip after not applying
         */
        
        private ConditionalDrumEntry(Pattern pattern, String preConditionString, String postConditionString, Mode mode,
                int[] targets, double probability, int skipWhenApplied, int skipWhenNotApplied) {
            this.pattern = pattern;
            this.preConditionString = preConditionString;
            this.postConditionString = postConditionString;
            this.mode = mode;
            this.targets = targets;
            this.probability = probability;
            this.skipWhenApplied = skipWhenApplied;
            this.skipWhenNotApplied = skipWhenNotApplied;
        }
    }
}
