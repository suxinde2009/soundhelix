package com.soundhelix.component.sequenceengine.impl;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.component.lfo.LFO;
import com.soundhelix.component.patternengine.PatternEngine;
import com.soundhelix.misc.ActivityMatrix;
import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Harmony;
import com.soundhelix.misc.LFOSequence;
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
 * Conditional LFOs allow the creation of LFO sequences certain conditions are met, e.g., for generating fill-ins, crescendos, etc. when monitored
 * ActivityVectors become active or inactive.
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

    /** The conditional pattern entries. */
    private ConditionalPatternDrumEntry[] conditionalPatternEntries;

    /** The conditional LFO entries. */
    private ConditionalLFODrumEntry[] conditionalLFOEntries;

    /** The random generator. */
    private Random random;

    public DrumSequenceEngine() {
        super();
    }

    public void setDrumEntries(DrumEntry[] drumEntries) {
        this.drumEntries = drumEntries;
    }

    public void setConditionalPatternEntries(ConditionalPatternDrumEntry[] conditionalPatternEntries) {
        this.conditionalPatternEntries = conditionalPatternEntries;
    }

    public void setConditionalLFOEntries(ConditionalLFODrumEntry[] conditionalLFOEntries) {
        this.conditionalLFOEntries = conditionalLFOEntries;
    }

    @Override
    public Track render(SongContext songContext, ActivityVector[] activityVectors) {
        int drumEntryCount = drumEntries.length;

        Track track = new Track(TrackType.RHYTHMIC);
        Sequence[] seqs = new Sequence[drumEntryCount];

        for (int i = 0; i < drumEntryCount; i++) {
            seqs[i] = new Sequence(songContext);
            track.add(seqs[i]);
        }

        processPatterns(songContext, activityVectors, seqs);
        processConditionalPatterns(songContext, activityVectors, seqs);
        processConditionalLFOs(songContext, activityVectors, track);

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
            int restartTick = 0;

            while (tick < ticks) {
                if (tick >= restartTick) {
                    pos = 0;
                    restartTick = getNextPatternRestartTick(songContext, tick);
                }

                Pattern.PatternEntry entry = pattern.get(pos % patternLength);
                int pitch = entry.getPitch();
                int len = Math.min(entry.getTicks(), restartTick - tick);

                if (activityVector.isActive(tick)) {
                    int vel = entry.getVelocity();

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
        int conditionalPatternEntryCount = conditionalPatternEntries.length;

        Map<String, Integer> indexMap = songContext.getActivityMatrix().getIndexMap();

        for (ConditionalPatternDrumEntry entry : conditionalPatternEntries) {
            if (entry.preCondition == null) {
                entry.preCondition = getConditionPattern(indexMap, entry.preConditionString);
                entry.postCondition = getConditionPattern(indexMap, entry.postConditionString);
            }
        }

        // the tick where each pattern was applied last (last tick of the pattern + 1), initially 0
        // (used to avoid overlapping application of patterns)
        int[] lastMatchedTick = new int[conditionalPatternEntryCount];

        // get all chord section activities

        String[] chordSectionActivity = new String[chordSections];
        int tick = 0;
        int x = 0;

        while (tick < ticks) {
            chordSectionActivity[x++] = getActivityString(tick, songContext.getActivityMatrix());
            tick += harmony.getChordSectionTicks(tick);
        }

        // start with the second chord section
        tick = harmony.getChordSectionTicks(0);
        int chordSection = 1;

        while (tick < ticks) {
            String activity = chordSectionActivity[chordSection];

            for (int i = 0; i < conditionalPatternEntryCount; i++) {
                ConditionalPatternDrumEntry conditionalEntry = conditionalPatternEntries[i];
                Pattern pattern = conditionalEntry.pattern;
                int patternTicks = pattern.getTicks();
                double probability = conditionalEntry.probability;
                java.util.regex.Pattern preCondition = conditionalEntry.preCondition;
                java.util.regex.Pattern postCondition = conditionalEntry.postCondition;

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

                            int[] targets = conditionalEntry.targets;
                            Mode mode = conditionalEntry.mode;

                            logger.debug("Applying conditional pattern " + i + " with length " + patternTicks + " for targets " + Arrays.toString(
                                    targets) + " at ticks " + tick + "-" + (tick + patternTicks - 1));

                            int len = pattern.size();

                            for (int k = 0; k < len; k++) {
                                PatternEntry entry = pattern.get(k);

                                for (int j = 0; j < targets.length; j++) {
                                    Sequence seq = seqs[targets[j]];
                                    int basePitch = drumEntries[targets[j]].pitch;

                                    if (entry.isNote()) {
                                        seq.replaceEntry(tick, new Sequence.SequenceEntry(basePitch + entry.getPitch(), entry.getVelocity(), entry
                                                .getTicks(), entry.isLegato()));
                                    } else if (mode == Mode.REPLACE) {
                                        seq.replaceEntry(tick, new Sequence.SequenceEntry(Integer.MIN_VALUE, -1, entry.getTicks(), entry.isLegato()));
                                    }
                                }

                                tick += entry.getTicks();
                            }

                            lastMatchedTick[i] = tick;

                            i += conditionalEntry.skipWhenApplied;
                        } else {
                            // either the pattern would have overlapped or the random generator didn't agree to apply
                            // the pattern

                            i += conditionalEntry.skipWhenNotApplied;
                        }
                    }
                }
            }

            tick += harmony.getChordSectionTicks(tick);
            chordSection++;
        }
    }

    /**
     * Process all conditional LFOs.
     * 
     * @param songContext the song context
     * @param activityVectors the activity vectors
     * @param track the track
     */

    private void processConditionalLFOs(SongContext songContext, ActivityVector[] activityVectors, Track track) {
        Structure structure = songContext.getStructure();
        Harmony harmony = songContext.getHarmony();
        int ticks = structure.getTicks();
        int chordSections = HarmonyUtils.getChordSectionCount(songContext);
        int conditionalLFOEntryCount = conditionalLFOEntries.length;

        Map<String, Integer> indexMap = songContext.getActivityMatrix().getIndexMap();

        for (ConditionalLFODrumEntry entry : conditionalLFOEntries) {
            entry.preCondition = getConditionPattern(indexMap, entry.preConditionString);
            entry.postCondition = getConditionPattern(indexMap, entry.postConditionString);

            // generate LFO sequence and attach to track (will be filled during processing)
            LFOSequence seq = new LFOSequence(songContext);
            entry.lfoSequence = seq;
            track.add(seq, entry.lfoName);
        }

        // the tick where each conditional LFO was applied last (last tick of the pattern + 1), initially 0
        // (used to avoid overlapping application of LFOs)
        int[] lastMatchedTick = new int[conditionalLFOEntryCount];

        // get all chord section activities

        String[] chordSectionActivity = new String[chordSections];
        int tick = 0;
        int x = 0;

        while (tick < ticks) {
            chordSectionActivity[x++] = getActivityString(tick, songContext.getActivityMatrix());
            tick += harmony.getChordSectionTicks(tick);
        }

        // start with the second chord section
        tick = harmony.getChordSectionTicks(0);
        int chordSection = 1;

        while (tick < ticks) {
            String activity = chordSectionActivity[chordSection];

            for (int i = 0; i < conditionalLFOEntryCount; i++) {
                ConditionalLFODrumEntry conditionalEntry = conditionalLFOEntries[i];
                int patternTicks = conditionalEntry.ticks;
                double probability = conditionalEntry.probability;
                java.util.regex.Pattern preCondition = conditionalEntry.preCondition;
                java.util.regex.Pattern postCondition = conditionalEntry.postCondition;

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

                            logger.debug("Applying conditional LFO \"" + conditionalEntry.lfoName + "\" with length " + patternTicks + " for ticks "
                                    + tick + "-" + (tick + patternTicks - 1));

                            LFOSequence seq = conditionalEntry.lfoSequence;
                            // fill up the LFO sequence with the default value from the current size of the LFO sequence to the current tick
                            conditionalEntry.lfoSequence.addValue(conditionalEntry.defaultValue, tick - conditionalEntry.lfoSequence.getTicks());

                            LFO lfo = conditionalEntry.lfo;

                            if (conditionalEntry.rotationUnit.equals("beat")) {
                                lfo.setPhase(conditionalEntry.phase);
                                lfo.setBeatSpeed(conditionalEntry.speed, structure.getTicksPerBeat());
                            } else if (conditionalEntry.rotationUnit.equals("range")) {
                                lfo.setPhase(conditionalEntry.phase);
                                lfo.setActivitySpeed(conditionalEntry.speed, tick, tick + patternTicks);
                            } else {
                                throw new RuntimeException("Invalid rotation unit \"" + conditionalEntry.rotationUnit + "\"");
                            }

                            for (int k = 0; k < patternTicks; k++) {
                                seq.addValue(lfo.getRawTickValue(tick++));
                            }

                            lastMatchedTick[i] = tick;

                            i += conditionalEntry.skipWhenApplied;
                        } else {
                            // either the pattern would have overlapped or the random generator didn't agree to apply
                            // the pattern

                            i += conditionalEntry.skipWhenNotApplied;
                        }
                    }
                }
            }

            tick += harmony.getChordSectionTicks(tick);
            chordSection++;
        }

        // fill up all LFO sequences with the default value from the current size of the LFO sequence to the end of the song

        for (int i = 0; i < conditionalLFOEntryCount; i++) {
            ConditionalLFODrumEntry conditionalEntry = conditionalLFOEntries[i];
            LFOSequence seq = conditionalEntry.lfoSequence;
            seq.addValue(conditionalEntry.defaultValue, ticks - seq.getTicks());
        }
    }

    /**
     * Returns the activity string of the given tick. The activity string is a concatenation of '0' and '1' characters specifying whether the
     * ActivityVectors from the list are active or not. If the tick is negative, the returned string consists of only '0' characters.
     * 
     * @param tick the tick to check
     * @param activityMatrix the activity matrix
     * 
     * @return the activity string
     */

    private String getActivityString(int tick, ActivityMatrix activityMatrix) {
        StringBuilder sb = new StringBuilder(activityMatrix.size());

        for (ActivityVector av : activityMatrix) {
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

        ConditionalPatternDrumEntry[] conditionalPatternEntries = new ConditionalPatternDrumEntry[patterns];

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
            } catch (Exception e) {}

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
            } catch (Exception e) {}

            if (i + 1 + skipWhenApplied > patterns || i + 1 + skipWhenApplied < 0) {
                throw new RuntimeException("Skip value \"" + skipWhenApplied + "\" would skip out of conditional pattern range");
            }

            int skipWhenNotApplied = 0;

            try {
                skipWhenNotApplied = XMLUtils.parseInteger(random, "skipWhenNotApplied", nodeList.item(i));
            } catch (Exception e) {}

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
                conditionalPatternEntries[i] = new ConditionalPatternDrumEntry(pattern, preCondition, postCondition, mode, targets, probability,
                        skipWhenApplied, skipWhenNotApplied);
            } else {
                conditionalPatternEntries[i] = new ConditionalPatternDrumEntry(pattern, preConditionString, postConditionString, mode, targets,
                        probability, skipWhenApplied, skipWhenNotApplied);
            }
        }

        setConditionalPatternEntries(conditionalPatternEntries);

        nodeList = XMLUtils.getNodeList("conditionalLFO", node);
        patterns = nodeList.getLength();

        ConditionalLFODrumEntry[] conditionalLFOEntries = new ConditionalLFODrumEntry[patterns];

        for (int i = 0; i < patterns; i++) {
            String lfoName = XMLUtils.parseString(random, "name", nodeList.item(i));
            int ticks = XMLUtils.parseInteger(random, XMLUtils.getNode("ticks", nodeList.item(i)));

            double speed = XMLUtils.parseDouble(random, XMLUtils.getNode("speed", nodeList.item(i)));

            String rotationUnit = XMLUtils.parseString(random, "rotationUnit", nodeList.item(i));

            double phase = 0.0d;

            try {
                phase = XMLUtils.parseDouble(random, XMLUtils.getNode("phase", nodeList.item(i)));
            } catch (Exception e) {}

            int defaultValue = XMLUtils.parseInteger(random, XMLUtils.getNode("defaultValue", nodeList.item(i)));

            String preConditionString = XMLUtils.parseString(random, "precondition", nodeList.item(i));
            String postConditionString = XMLUtils.parseString(random, "postcondition", nodeList.item(i));

            double probability = XMLUtils.parseDouble(random, "probability", nodeList.item(i)) / 100.0d;

            int skipWhenApplied = 0;

            try {
                skipWhenApplied = XMLUtils.parseInteger(random, "skipWhenApplied", nodeList.item(i));
            } catch (Exception e) {}

            if (i + 1 + skipWhenApplied > patterns || i + 1 + skipWhenApplied < 0) {
                throw new RuntimeException("Skip value \"" + skipWhenApplied + "\" would skip out of conditional pattern range");
            }

            int skipWhenNotApplied = 0;

            try {
                skipWhenNotApplied = XMLUtils.parseInteger(random, "skipWhenNotApplied", nodeList.item(i));
            } catch (Exception e) {}

            if (i + 1 + skipWhenNotApplied > patterns || i + 1 + skipWhenNotApplied < 0) {
                throw new RuntimeException("Skip value \"" + skipWhenNotApplied + "\" would skip out of conditonal pattern range");
            }

            Node lfoNode = XMLUtils.getNode("lfo", nodeList.item(i));

            LFO lfo;

            try {
                lfo = XMLUtils.getInstance(songContext, LFO.class, lfoNode, randomSeed, i);
            } catch (Exception e) {
                throw new RuntimeException("Could not instantiate LFO", e);
            }

            lfo.setSongContext(songContext);

            conditionalLFOEntries[i] = new ConditionalLFODrumEntry(lfo, lfoName, ticks, speed, rotationUnit, phase, defaultValue, preConditionString,
                    postConditionString, probability, skipWhenApplied, skipWhenNotApplied);
        }

        setConditionalLFOEntries(conditionalLFOEntries);

        configurePatternRestartMode(random, node);
    }

    /**
     * Parses the given condition string and returns a pattern representing the condition. The condition string is given as a comma-separated list of
     * ActivityVector names, which are each prefixed by either "+" or "-".
     * 
     * @param indexMap the map that maps ActivityVector names to iterator index
     * @param string the condition string
     * 
     * @return the condition pattern
     */

    private static java.util.regex.Pattern getConditionPattern(Map<String, Integer> indexMap, String string) {
        int vectors = indexMap.size();

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

                Integer index = indexMap.get(name);

                if (index == null) {
                    throw new IllegalArgumentException("Unknown ActitvityVector \"" + name + "\" referenced in condition pattern");
                }

                sb.setCharAt(index, c);
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
     * Represents a conditional pattern drum entry.
     */

    private static final class ConditionalPatternDrumEntry {
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

        private ConditionalPatternDrumEntry(Pattern pattern, java.util.regex.Pattern preCondition, java.util.regex.Pattern postCondition, Mode mode,
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

        private ConditionalPatternDrumEntry(Pattern pattern, String preConditionString, String postConditionString, Mode mode, int[] targets,
                double probability, int skipWhenApplied, int skipWhenNotApplied) {
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

    /**
     * Represents a conditional pattern drum entry.
     */

    private static final class ConditionalLFODrumEntry {
        /** The LFO. */
        private LFO lfo;

        /** The LFO name. */
        private String lfoName;

        /** The number of ticks. */
        private int ticks;

        /** The LFO speed in radians. */
        private double speed;

        /** The LFO rotation unit. */
        private String rotationUnit;

        /** The LFO phase in radians. */
        private double phase;

        /** The LFO default value. */
        private int defaultValue;

        /** The precondition as a regular expression. */
        private java.util.regex.Pattern preCondition;

        /** The postcondition as a regular expression. */
        private java.util.regex.Pattern postCondition;

        /** The precondition as a string. */
        private final String preConditionString;

        /** The postcondition as a string. */
        private final String postConditionString;

        /** The probability for being applied if it could be applied. */
        private final double probability;

        /** The number of entries to skip after applying. */
        private final int skipWhenApplied;

        /** The number of entries to skip after not applying. */
        private final int skipWhenNotApplied;

        /** The LFO sequence. */
        private LFOSequence lfoSequence;

        /**
         * Constructor.
         * 
         * @param lfo the LFO
         * @param lfoName the LFO name
         * @param ticks the number of ticks
         * @param speed the speed
         * @param rotationUnit the rotation unit
         * @param phase the phase
         * @param defaultValue the default value
         * @param preConditionString the precondition
         * @param postConditionString the postcondition
         * @param probability the probability
         * @param skipWhenApplied the number of items to skip after applying
         * @param skipWhenNotApplied the number of items to skip after not applying
         */

        private ConditionalLFODrumEntry(LFO lfo, String lfoName, int ticks, double speed, String rotationUnit, double phase, int defaultValue,
                String preConditionString, String postConditionString, double probability, int skipWhenApplied, int skipWhenNotApplied) {
            this.lfo = lfo;
            this.lfoName = lfoName;
            this.ticks = ticks;
            this.speed = speed;
            this.rotationUnit = rotationUnit;
            this.phase = phase;
            this.defaultValue = defaultValue;
            this.preConditionString = preConditionString;
            this.postConditionString = postConditionString;
            this.probability = probability;
            this.skipWhenApplied = skipWhenApplied;
            this.skipWhenNotApplied = skipWhenNotApplied;
        }
    }
}
