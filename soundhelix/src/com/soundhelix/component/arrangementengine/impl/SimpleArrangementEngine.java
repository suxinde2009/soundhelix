package com.soundhelix.component.arrangementengine.impl;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Priority;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.component.sequenceengine.SequenceEngine;
import com.soundhelix.misc.ActivityMatrix;
import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Arrangement;
import com.soundhelix.misc.Harmony;
import com.soundhelix.misc.SongContext;
import com.soundhelix.misc.Structure;
import com.soundhelix.misc.Track;
import com.soundhelix.util.HarmonyUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a simple ArrangementEngine. The song starts with a configurable "fade-in" of the number of active ActivityVectors and ends with a
 * configurable "fade-out" of the number of active ActivityVectors. For each chord section in between, the number of active AVs is varied in the
 * configured range.
 * 
 * ActivityVector activity can be constrained individually to enforce certain properties. All AVs are filled at the same time because there is always
 * the above-mentioned overall song constraint that defines the number of active AVs for each chord section. For each chord section a choice is made
 * whether to activate or deactivate AVs or to leave the number of active AVs unchanged, according to the AVs that should be active. The list of
 * generated AVs are called a song activity matrix.
 * 
 * The following constraints are supported for ActivityVectors:
 * 
 * - minActive(n): the AV must be active for at least n% of the song; granularity is at chord section level; it is also possible define that the AV
 * may be inactive for the whole song or be active with at least n% of the song - maxActive(n): the AV must be active for at most n% of the song;
 * granularity is at chord section level - startBeforeSection(n): the AV must start before section n - startAfterSection(n): the AV must start after
 * section n - stopBeforeSection(n): the AV must stop before section n, counted from the end (0 is the last chord section) - stopAfterSection(n): the
 * AV must stop after section n, counted from the end (0 is the last chord section) - minSegmentCount(n): the AV must be active for at least n chord
 * section segments - maxSegmentCount(n): the AV must be active for at most n chord section segments - minSegmentLength(n): the minimum AV segment
 * length must be n chord sections - maxSegmentLength(n): the maximum AV segment length must be n chord sections - minPauseLength(n): the minimum AV
 * pause length must be n chord sections - maxPauseLength(n): the maximum AV pause length must be n chord sections
 * 
 * A randomized backtracking algorithm is used for finding a song activity matrix that fulfills all constraints of all ActivityVectors at the same
 * time. It works roughly as follows: The song activity matrix is built from left to right (chord section by chord section) by making random
 * selections about which AVs to activate or deactivate. If a constraint violation is detected, a number of different random choices at the same
 * section is tried by reverting the previous choice and choosing another one; if all these fail, the algorithm backtracks to the previous section. To
 * avoid the overhead of recursion, the algorithm is implemented iteratively.
 * 
 * The algorithm is able to find a valid song activity matrix pretty quickly (if one exists), much faster than the simpler algorithm used in version
 * 0.1 of SoundHelix and before.
 * 
 * The algorithm scales pretty well with increasing numbers of constraints, but keep in mind that every additional constraint increases the complexity
 * level of finding a valid song activity matrix.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class SimpleArrangementEngine extends AbstractArrangementEngine {
    /** The constraint mode for ActivityMatrix generation. */
    public enum ConstraintMode {
        /** Exact mode using backtracking. */
        EXACT,
        /** Greedy mode using an approximation. */
        GREEDY
    }

    /**
     * The ActivityVector modification operators.
     */

    private enum ActivityVectorModificationOperator {
        /** Set bits (0 operands, 1 target). */
        SET(0),
        /** Clear bits (0 operands, 1 target). */
        CLEAR(0),
        /** Flip bits (0 operands, 1 target). */
        FLIP(0),
        /** Logical NOT (1 operand, 1 target). */
        NOT(1),
        /** Logical XOR (2 operands, 1 target). */
        AND(2),
        /** Logical AND NOT (2 operands, 1 target). */
        ANDNOT(2),
        /** Logical OR (2 operands, 1 target). */
        OR(2),
        /** Logical AND (2 operands, 1 target). */
        XOR(2);

        /** The number of operands. */
        private int operands;

        /**
         * Constructor.
         * 
         * @param operands the number of operands
         */

        ActivityVectorModificationOperator(int operands) {
            this.operands = operands;
        }
    }

    /** The random generator. */
    private Random random;

    /** The maximum delta that activity can change in each chord section. */
    private int maxActivityChangeCount = 2;

    /** The minimum number of active ActivityVectors. */
    private int minActivityCount = 2;

    /** The maximum number of active ActivityVectors (determined dynamically if <= 0). */
    private int maxActivityCount;

    /** The constraintMode. */
    private ConstraintMode constraintMode;

    /** The array of start activity counts. */
    private int[] startActivityCounts;

    /** The array of stop activity counts. */
    private int[] stopActivityCounts;

    /** The arrangement entries. */
    private ArrangementEntry[] arrangementEntries;

    /** The map that maps from activity vector names to activity vector configurations. */
    private Map<String, ActivityVectorConfiguration> activityVectorConfigurationHashMap;

    /** The maximum number of iterations before failing. */
    private int maxIterations;

    /** The activity vevtor modifications. */
    private ActivityVectorModification[] activityVectorModifications;

    /**
     * Constructor.
     */

    public SimpleArrangementEngine() {
        super();
    }

    @Override
    public Arrangement render(SongContext songContext) {
        Map<String, ActivityVectorConfiguration> neededActivityVectors = getNeededActivityVectors();

        ActivityVectorConfiguration[] vectors = neededActivityVectors.values().toArray(new ActivityVectorConfiguration[neededActivityVectors.size()]);

        int tracks = arrangementEntries.length;

        if (logger.isDebugEnabled()) {
            logger.debug("Creating " + vectors.length + " ActivityVectors for " + tracks + " track" + (tracks == 1 ? "" : "s"));
        }

        ActivityMatrix activityMatrix;

        if (constraintMode == ConstraintMode.EXACT) {
            activityMatrix = createExactConstrainedActivityVectors(songContext, vectors);
        } else if (constraintMode == ConstraintMode.GREEDY) {
            activityMatrix = createGreedyConstrainedActivityVectors(songContext, vectors);
        } else {
            throw new RuntimeException("Unknown constraint mode \"" + constraintMode + "\"");
        }

        songContext.setActivityMatrix(activityMatrix);

        if (activityVectorModifications.length > 0) {
            activityMatrix.dump(songContext, "Song's activity matrix before applying ActivityVector modifications", Priority.DEBUG);
            processActivityVectorModifications(songContext);
        }

        activityMatrix.dump(songContext, "Song's activity matrix", Priority.INFO);
        shiftIntervalBoundaries(neededActivityVectors);

        return createArrangement(songContext, neededActivityVectors);
    }

    /**
     * Processes the activity vector modifications.
     * 
     * @param songContext the song context
     */

    private void processActivityVectorModifications(SongContext songContext) {
        if (logger.isDebugEnabled()) {
            logger.debug("Applying ActivityVector modifications");
        }

        ActivityMatrix activityMatrix = songContext.getActivityMatrix();

        for (ActivityVectorModification modification : activityVectorModifications) {
            ActivityVectorModificationOperator operator = modification.operator;
            String fromString = modification.from;
            String toString = modification.to;
            ActivityVector target = activityMatrix.get(modification.target);
            ActivityVector operand1 = activityMatrix.get(modification.operand1);
            ActivityVector operand2 = activityMatrix.get(modification.operand2);

            int from = getChordSectionNumber(songContext, fromString);
            int to = getChordSectionNumber(songContext, toString);

            if (from > to) {
                // swap from and to
                from = from ^ to ^ (to = from);
            }

            int fromTick = HarmonyUtils.getChordSectionTick(songContext, from);
            int tillTick = HarmonyUtils.getChordSectionTick(songContext, to + 1);
            switch (operator) {
                case SET:
                    target.setActivityState(fromTick, tillTick, true);
                    break;

                case CLEAR:
                    target.setActivityState(fromTick, tillTick, false);
                    break;

                case FLIP:
                    target.flipActivityState(fromTick, tillTick);
                    break;

                case NOT:
                    target.applyLogicalNot(operand1, fromTick, tillTick);
                    break;

                case AND:
                    target.applyLogicalAnd(operand1, operand2, fromTick, tillTick);
                    break;

                case ANDNOT:
                    target.applyLogicalAndNot(operand1, operand2, fromTick, tillTick);
                    break;

                case OR:
                    target.applyLogicalOr(operand1, operand2, fromTick, tillTick);
                    break;

                case XOR:
                    target.applyLogicalXor(operand1, operand2, fromTick, tillTick);
                    break;

                default:
                    throw new RuntimeException("Unsupported operator \"" + operator + "\"");
            }
        }
    }

    /**
     * Returns the chord section number for the given string, which is either an integer (zero, positive or negative) or a positive double with a "%"
     * behind it.
     * 
     * @param songContext the song context
     * @param str the string
     * @return the chord section number
     */
    private int getChordSectionNumber(SongContext songContext, String str) {
        if (str.endsWith("%")) {
            double percentage = Double.parseDouble(str.substring(0, str.length() - 1));

            if (percentage < 0d || percentage > 100d) {
                throw new IllegalArgumentException("The percentage \"" + str + "\" must be between 0 and 100");
            }

            int tick = (int) (songContext.getStructure().getTicks() * percentage / 100d);

            int number = HarmonyUtils.getChordSectionNumber(songContext, tick);
            if (number == -1) {
                return HarmonyUtils.getChordSectionCount(songContext) - 1;
            } else {
                return number;
            }
        } else {
            int number = Integer.parseInt(str);

            if (number < 0) {
                return HarmonyUtils.getChordSectionCount(songContext) + number;
            } else {
                return number;
            }
        }
    }

    /**
     * Returns the needed activity vectors as a map that maps from the activity vector name to its configuration. Rather than returning all activity
     * vectors, this method only returns the activity vectors that are used within at least one SequenceEngine.
     * 
     * @return the map of activity vectors
     */

    private Map<String, ActivityVectorConfiguration> getNeededActivityVectors() {
        Map<String, ActivityVectorConfiguration> neededActivityVector = new LinkedHashMap<String, ActivityVectorConfiguration>();

        for (ArrangementEntry entry : arrangementEntries) {
            String[] names = entry.activityVectorNames;

            for (String name : names) {
                ActivityVectorConfiguration avc = activityVectorConfigurationHashMap.get(name);

                if (avc == null) {
                    throw new RuntimeException("Unknown ActivityVector \"" + name + "\"");
                }

                neededActivityVector.put(name, avc);
            }
        }

        return neededActivityVector;
    }

    /**
     * Creates an arrangement based on the given map of needed activity vectors. This is done by rendering a track for each SequenceEngine based on
     * the already generated activity vectors.
     * 
     * @param songContext the song context
     * @param neededActivityVector the map of needed activity vectors
     * 
     * @return the created arrangement
     */

    private Arrangement createArrangement(SongContext songContext, Map<String, ActivityVectorConfiguration> neededActivityVector) {
        // use each SequenceEngine to render a track
        // each SequenceEngine is given the number of ActivityVectors it
        // requires

        Arrangement arrangement = new Arrangement();

        for (ArrangementEntry entry : arrangementEntries) {
            SequenceEngine sequenceEngine = entry.sequenceEngine;
            int neededActivityVectors = sequenceEngine.getActivityVectorCount();

            String[] names = entry.activityVectorNames;

            if (names.length != neededActivityVectors) {
                throw new RuntimeException("Need " + neededActivityVectors + " ActivityVector" + (neededActivityVectors == 1 ? "" : "s")
                        + " for instrument " + entry.instrument + ", found " + names.length);
            }

            ActivityVector[] list = new ActivityVector[neededActivityVectors];

            for (int k = 0; k < neededActivityVectors; k++) {
                list[k] = neededActivityVector.get(names[k]).activityVector;
            }

            Track track = sequenceEngine.render(songContext, list);
            track.transpose(entry.transposition);
            track.scaleVelocity(entry.velocity);
            arrangement.add(track, entry.instrument);
        }

        return arrangement;
    }

    /**
     * Shifts the interval boundaries of all activity vectors where a start or stop shift has been configured.
     * 
     * @param neededActivityVector the needed activity vectors
     */

    private void shiftIntervalBoundaries(Map<String, ActivityVectorConfiguration> neededActivityVector) {
        for (ActivityVectorConfiguration avc : neededActivityVector.values()) {
            avc.activityVector.shiftIntervalBoundaries(avc.startShift, avc.stopShift);
        }
    }

    /**
     * Creates an ActivityMatrix that fulfills all constraints. The algorithm uses randomized backtracking for finding such an ActivityMatrix.
     * 
     * @param songContext the song context
     * @param activityVectorConfigurations the ActivityVector configurations
     *
     * @return the ActivityMatrix
     * 
     * @throws RuntimeException in case the constraints could not be fulfilled
     */

    private ActivityMatrix createExactConstrainedActivityVectors(SongContext songContext,
            ActivityVectorConfiguration[] activityVectorConfigurations) {
        int sections = HarmonyUtils.getChordSectionCount(songContext);
        int vectors = activityVectorConfigurations.length;

        // convert minActive and maxActive percentages into chord section counts

        for (ActivityVectorConfiguration avc : activityVectorConfigurations) {
            // minActive has to be rounded up to an integer number of sections, maxActive has to be rounded down
            avc.minActiveSectionCount = (int) (sections * avc.minActive / 100d + 0.999999);
            avc.maxActiveSectionCount = (int) (sections * avc.maxActive / 100d);
        }

        int maxActivityVectors = Math.min(maxActivityCount, vectors);

        if (maxActivityVectors <= 0) {
            maxActivityVectors = getActivityVectorMaximum(vectors, 0.40, 0.2);
        }

        int[] startActivityCounts = processActivityCounts(this.startActivityCounts, vectors);
        int[] stopActivityCounts = processActivityCounts(this.stopActivityCounts, vectors);

        BitSet[] bitSets = new BitSet[sections];
        int[] tries = new int[sections];

        // the out-degree (branch level) to use at each chord section
        int[] sectionIterations = new int[sections];

        ActivityVectorState[][] allStates = new ActivityVectorState[sections][vectors];

        for (int i = 0; i < sections; i++) {
            bitSets[i] = new BitSet(vectors);
            if (i == 0) {
                // we use a large value here; the termination condition is the maximum number of total iterations
                // anyway
                sectionIterations[0] = 1000000000;
            } else {
                // the algorithm terminates most quickly with a value of 2, larger values will reduce the backtracking
                // rate and the speed until a valid solution is found

                // with the value 1, the whole algorithm degrades to restarting the whole activity matrix from scratch
                // every time a violation is detected (it always backtracks to section 0), which undermines the whole
                // backtracking approach
                sectionIterations[i] = 2;
            }

            for (int j = 0; j < vectors; j++) {
                allStates[i][j] = new ActivityVectorState();
            }
        }

        BitSet emptyBitSet = new BitSet(vectors);

        int section = 0;
        int remainingSections = sections - 1;
        int iterations = 0;
        int backtracks = 0;
        int violations = 0;

        long startTime = System.nanoTime();

        nextSection: while (section < sections) {
            BitSet previousBitSet;

            if (section == 0) {
                previousBitSet = emptyBitSet;
            } else {
                previousBitSet = bitSets[section - 1];
            }

            int lastWantedCount = previousBitSet.cardinality();

            // note that the wanted AV acount can be different when backtracking and returning to a section where
            // have been before; this is a wanted behavior of the algorithm in order to increase the exploration
            // space
            int wantedCount = getWantedActivityVectorCount(section, sections, maxActivityVectors, lastWantedCount, startActivityCounts,
                    stopActivityCounts);

            int diff = wantedCount - lastWantedCount;

            while (tries[section] < sectionIterations[section]) {
                tries[section]++;

                copyStateFromPreviousSection(section, allStates);
                ActivityVectorState[] states = allStates[section];
                BitSet bitSet = (BitSet) previousBitSet.clone();
                bitSets[section] = bitSet;

                boolean isSuccess = true;
                int d = diff;

                if (d > 0) {
                    do {
                        int p = setRandomBit(bitSet, vectors);

                        if (p >= 0) {
                            if (states[p].activeCount > 0 && states[p].segmentLength < activityVectorConfigurations[p].minPauseLength) {
                                isSuccess = false;
                                break;
                            }

                            states[p].segments++;
                            states[p].segmentLength = 0;
                        } else {
                            // all bits are set already (should never happen)
                            isSuccess = false;
                            break;
                        }
                    } while (--d > 0);
                } else if (d < 0) {
                    do {
                        int p = clearRandomBit(bitSet);

                        if (p >= 0) {
                            if (states[p].segmentLength < activityVectorConfigurations[p].minSegmentLength) {
                                isSuccess = false;
                                break;
                            }

                            states[p].segmentLength = 0;
                        } else {
                            // all bits are cleared already (should never happen)
                            isSuccess = false;
                            break;
                        }
                    } while (++d < 0);
                } else if (random.nextBoolean() && wantedCount < vectors) {
                    // d is zero; first set then clear a random bit (likely a different bit, but it can be the same)
                    // the previous if makes sure that we don't have the maximum number of AVs active already; in this
                    // case we will do nothing (i.e., don't change the activity)
                    int p = setRandomBit(bitSet, vectors);

                    if (p >= 0) {
                        if (states[p].activeCount > 0 && states[p].segmentLength < activityVectorConfigurations[p].minPauseLength) {
                            isSuccess = false;
                        } else {
                            states[p].segments++;
                            states[p].segmentLength = 0;

                            p = clearRandomBit(bitSet);

                            if (p >= 0) {
                                if (states[p].segmentLength < activityVectorConfigurations[p].minSegmentLength) {
                                    isSuccess = false;
                                }

                                states[p].segmentLength = 0;
                            } else {
                                // all bits are cleared already (should never happen)
                                isSuccess = false;
                            }
                        }
                    } else {
                        // all bits are set already (should never happen)
                        isSuccess = false;
                    }
                }

                if (isSuccess) {
                    // update states and check constraints

                    for (int i = 0; i < vectors; i++) {
                        ActivityVectorState state = states[i];

                        state.segmentLength++;
                        ActivityVectorConfiguration c = activityVectorConfigurations[i];
                        boolean isActive = bitSet.get(i);

                        if (isActive) {
                            state.activeCount++;

                            if (!state.activeInStopInterval && remainingSections < c.stopAfterSection && remainingSections > c.stopBeforeSection) {
                                state.activeInStopInterval = true;
                            }
                        }

                        if ((!c.allowInactive || state.activeCount > 0) && state.activeCount + remainingSections < c.minActiveSectionCount) {
                            isSuccess = false;
                            break;
                        }

                        if (state.activeCount > c.maxActiveSectionCount) {
                            isSuccess = false;
                            break;
                        }

                        if (state.segments > c.maxSegmentCount) {
                            isSuccess = false;
                            break;
                        }

                        if (state.segments + (remainingSections + (isActive ? 0 : 1)) / 2 < c.minSegmentCount) {
                            isSuccess = false;
                            break;
                        }

                        if (isActive) {
                            if (state.segmentLength > c.maxSegmentLength) {
                                isSuccess = false;
                                break;
                            }

                            if (state.segmentLength + remainingSections < c.minSegmentLength) {
                                isSuccess = false;
                                break;
                            }
                        }

                        if (!isActive && state.segmentLength > c.maxPauseLength) {
                            isSuccess = false;
                            break;
                        }

                        if (isActive && state.activeCount == 1 && section > c.startBeforeSection - 1) {
                            isSuccess = false;
                            break;
                        }

                        if (state.activeCount > 0 && section < c.startAfterSection + 1) {
                            isSuccess = false;
                            break;
                        }

                        if (isActive && remainingSections < c.stopBeforeSection + 1) {
                            isSuccess = false;
                            break;
                        }

                        if (state.activeCount > 0 && remainingSections < c.stopBeforeSection + 1 && !state.activeInStopInterval) {
                            isSuccess = false;
                            break;
                        }
                    }
                }

                iterations++;

                if (isSuccess) {
                    section++;
                    remainingSections--;
                    if (section < sections) {
                        tries[section] = 0;
                    }
                    continue nextSection;
                } else {
                    violations++;
                }

                if (iterations > maxIterations) {
                    throw new RuntimeException("Unable to find a valid song activity matrix within " + maxIterations + " iterations");
                }
            }

            // number of tries for current section exhausted, backtrack to the previous section
            section--;
            remainingSections++;

            if (section < 0) {
                // this should never happen, because we have a large number of tries for chord section 0
                throw new RuntimeException("Creation of song failed");
            }

            backtracks++;
        }

        long end = System.nanoTime();

        if (logger.isDebugEnabled()) {
            logger.debug("Fulfilled exact constraints in " + (end - startTime) / 1000000L + " ms. Iterations: " + iterations + " (" + iterations
                    * 1000000000L / (end - startTime) + " iterations/s), violations: " + violations + ", backtrack steps: " + backtracks);
        }

        return convertBitSetsToActivityVectors(songContext, activityVectorConfigurations, bitSets);
    }

    /**
     * Creates an ActivityMatrix that fulfills all or most of the constraints, based on a greedy algorithm.
     * 
     * @param songContext the song context
     * @param activityVectorConfigurations the ActivityVector configurations
     *
     * @return the ActivityMatrix
     */

    private ActivityMatrix createGreedyConstrainedActivityVectors(SongContext songContext,
            ActivityVectorConfiguration[] activityVectorConfigurations) {

        int sections = HarmonyUtils.getChordSectionCount(songContext);
        int vectors = activityVectorConfigurations.length;

        // convert minActive and maxActive percentages into chord section counts

        for (ActivityVectorConfiguration avc : activityVectorConfigurations) {
            // minActive has to be rounded up to an integer number of sections, maxActive has to be rounded down
            avc.minActiveSectionCount = (int) (sections * avc.minActive / 100d + 0.999999);
            avc.maxActiveSectionCount = (int) (sections * avc.maxActive / 100d);
        }

        int maxActivityVectors = Math.min(maxActivityCount, vectors);

        if (maxActivityVectors <= 0) {
            maxActivityVectors = getActivityVectorMaximum(vectors, 0.40, 0.2);
        }

        int[] startActivityCounts = processActivityCounts(this.startActivityCounts, vectors);
        int[] stopActivityCounts = processActivityCounts(this.stopActivityCounts, vectors);

        BitSet[] bitSets = new BitSet[sections];

        ActivityVectorState[][] allStates = new ActivityVectorState[sections][vectors];

        for (int i = 0; i < sections; i++) {
            bitSets[i] = new BitSet(vectors);

            for (int j = 0; j < vectors; j++) {
                allStates[i][j] = new ActivityVectorState();
            }
        }

        BitSet emptyBitSet = new BitSet(vectors);

        int section = 0;
        int remainingSections = sections - 1;

        long startTime = System.nanoTime();

        while (section < sections) {
            BitSet previousBitSet;

            if (section == 0) {
                previousBitSet = emptyBitSet;
            } else {
                previousBitSet = bitSets[section - 1];
            }

            int lastWantedCount = previousBitSet.cardinality();

            int tries = 0;
            int minError = Integer.MAX_VALUE;

            List<BitSet> minBitSetList = new ArrayList<BitSet>();
            List<ActivityVectorState[]> minStatesList = new ArrayList<ActivityVectorState[]>();

            while (tries++ < maxIterations) {
                int wantedCount = getWantedActivityVectorCount(section, sections, maxActivityVectors, lastWantedCount, startActivityCounts,
                        stopActivityCounts);

                int diff = wantedCount - lastWantedCount;

                copyStateFromPreviousSection(section, allStates);
                ActivityVectorState[] states = allStates[section];
                BitSet bitSet = (BitSet) previousBitSet.clone();
                bitSets[section] = bitSet;

                int error = 0;

                int d = diff;

                if (d > 0) {
                    do {
                        int p = setRandomBit(bitSet, vectors);

                        if (states[p].activeCount > 0 && states[p].segmentLength < activityVectorConfigurations[p].minPauseLength) {
                            error += 250 * (activityVectorConfigurations[p].minPauseLength - states[p].segmentLength);
                        }

                        states[p].segments++;
                        states[p].segmentLength = 0;
                    } while (--d > 0);
                } else if (d < 0) {
                    do {
                        int p = clearRandomBit(bitSet);

                        if (states[p].segmentLength < activityVectorConfigurations[p].minSegmentLength) {
                            error += 250 * (activityVectorConfigurations[p].minSegmentLength - states[p].segmentLength);
                        }

                        states[p].segmentLength = 0;
                    } while (++d < 0);
                } else if (random.nextBoolean() && wantedCount < vectors) {
                    // d is zero; first set then clear a random bit (likely a different bit, but it can be the same)
                    // the previous if makes sure that we don't have the maximum number of AVs active already; in this
                    // case we will do nothing (i.e., don't change the activity)
                    int p = setRandomBit(bitSet, vectors);

                    if (states[p].activeCount > 0 && states[p].segmentLength < activityVectorConfigurations[p].minPauseLength) {
                        error += 250 * (activityVectorConfigurations[p].minPauseLength - states[p].segmentLength);
                    }

                    states[p].segments++;
                    states[p].segmentLength = 0;

                    p = clearRandomBit(bitSet);

                    if (states[p].segmentLength < activityVectorConfigurations[p].minSegmentLength) {
                        error += 250 * (activityVectorConfigurations[p].minSegmentLength - states[p].segmentLength);
                    }

                    states[p].segmentLength = 0;
                }

                // update states and check constraints

                for (int i = 0; i < vectors; i++) {
                    ActivityVectorState state = states[i];

                    state.segmentLength++;
                    ActivityVectorConfiguration c = activityVectorConfigurations[i];
                    boolean isActive = bitSet.get(i);

                    if (isActive) {
                        state.activeCount++;

                        if (!state.activeInStopInterval && remainingSections < c.stopAfterSection && remainingSections > c.stopBeforeSection) {
                            state.activeInStopInterval = true;
                        }
                    }

                    if (section >= 5 && (!c.allowInactive || state.activeCount > 0) && 100.0d * state.activeCount / (section + 1) < c.minActive) {
                        error += 15 * (c.minActive - 100.0d * state.activeCount / (section + 1));
                    }

                    if (section >= 5 && 100.0d * state.activeCount / (section + 1) > c.maxActive) {
                        error += 15 * (100.0d * state.activeCount / (section + 1) - c.maxActive);
                    }

                    if (state.segments > c.maxSegmentCount) {
                        error += 400 * (state.segments - c.maxSegmentCount);
                    }

                    if (state.segments + (remainingSections + (isActive ? 0 : 1)) / 2 < c.minSegmentCount) {
                        error += 400;
                    }

                    if (isActive) {
                        if (state.segmentLength > c.maxSegmentLength) {
                            error += 250 * (state.segmentLength - c.maxSegmentLength);
                        }

                        if (state.segmentLength + remainingSections < c.minSegmentLength) {
                            error += 250;
                        }
                    }

                    if (!isActive && state.segmentLength > c.maxPauseLength) {
                        error += 250 * (state.segmentLength - c.maxPauseLength);
                    }

                    if (isActive && state.activeCount == 1 && section > c.startBeforeSection - 1) {
                        error += 100 * (section - c.startBeforeSection + 1);
                    }

                    if (state.activeCount > 0 && section < c.startAfterSection + 1) {
                        error += 100 * (c.startAfterSection + 1 - section);
                    }

                    if (isActive && remainingSections < c.stopBeforeSection + 1) {
                        error += 100 * (c.stopBeforeSection + 1 - remainingSections);
                    }

                    if (state.activeCount > 0 && remainingSections < c.stopBeforeSection + 1 && !state.activeInStopInterval) {
                        error += 100 * (c.stopBeforeSection + 1 - remainingSections);
                    }
                }

                if (error < minError) {
                    minBitSetList.clear();
                    minStatesList.clear();
                    minError = error;
                }

                if (error == minError) {
                    if (!minBitSetList.contains(bitSet)) {
                        minStatesList.add(cloneStates(states));
                        minBitSetList.add(bitSet);
                    }
                }
            }

            int offset = random.nextInt(minBitSetList.size());
            bitSets[section] = minBitSetList.get(offset);
            allStates[section] = minStatesList.get(offset);

            section++;
            remainingSections--;
        }

        long end = System.nanoTime();

        if (logger.isDebugEnabled()) {
            logger.debug("Fulfilled greedy constraints in " + (end - startTime) / 1000000L + " ms.");
        }

        return convertBitSetsToActivityVectors(songContext, activityVectorConfigurations, bitSets);
    }

    /**
     * Converts the BitSets into a set of ActivityVectors which have their activity set based on the BitSets.
     * 
     * @param songContext the song context
     * @param activityVectorConfigurations the array of activity vector configurations
     * @param bitSets the array of BitSets
     * 
     * @return the activity matrix
     */

    private ActivityMatrix convertBitSetsToActivityVectors(SongContext songContext, ActivityVectorConfiguration[] activityVectorConfigurations,
            BitSet[] bitSets) {
        Structure structure = songContext.getStructure();
        Harmony harmony = songContext.getHarmony();

        int vectors = activityVectorConfigurations.length;
        int ticks = structure.getTicks();
        ActivityMatrix activityMatrix = new ActivityMatrix();

        for (int i = 0; i < vectors; i++) {
            ActivityVectorConfiguration avc = activityVectorConfigurations[i];
            ActivityVector av = new ActivityVector(avc.name, ticks);
            // make sure that the ticks of all AVs equals the song length (setActivityState() below does not
            // ensure this, because it only sets active segments in the AV)
            av.addInactivity(ticks);
            avc.activityVector = av;
            activityMatrix.add(av);
        }

        int section = 0;
        int tick = 0;

        while (tick < ticks) {
            BitSet bitSet = bitSets[section];
            int length = harmony.getChordSectionTicks(tick);

            int pos = bitSet.nextSetBit(0);

            while (pos >= 0) {
                activityVectorConfigurations[pos].activityVector.setActivityState(tick, tick + length, true);
                pos = bitSet.nextSetBit(pos + 1);
            }

            tick += length;
            section++;

        }

        return activityMatrix;
    }

    /**
     * Copies the state from the previous chord section to the current chord section state.
     * 
     * @param section the section number
     * @param state the array of states
     */

    private void copyStateFromPreviousSection(int section, ActivityVectorState[][] state) {
        ActivityVectorState[] sourceArray;

        ActivityVectorState[] targetArray = state[section];
        int len = targetArray.length;

        if (section == 0) {
            // sourceArray is read-only, therefore we can reuse the same state instance
            sourceArray = new ActivityVectorState[len];
            ActivityVectorState emptyState = new ActivityVectorState();

            for (int k = 0; k < len; k++) {
                sourceArray[k] = emptyState;
            }
        } else {
            sourceArray = state[section - 1];
        }

        while (len > 0) {
            len--;
            ActivityVectorState source = sourceArray[len];
            ActivityVectorState target = targetArray[len];

            target.activeCount = source.activeCount;
            target.segmentLength = source.segmentLength;
            target.segments = source.segments;
            target.activeInStopInterval = source.activeInStopInterval;
        }
    }

    /**
     * Clones the given array of ActivityVectorStates.
     * 
     * @param states the states to clone
     * 
     * @return the cloned states
     */

    private ActivityVectorState[] cloneStates(ActivityVectorState[] states) {
        int len = states.length;
        ActivityVectorState[] newStates = new ActivityVectorState[states.length];

        for (int i = 0; i < len; i++) {
            newStates[i] = new ActivityVectorState(states[i]);
        }

        return newStates;
    }

    /**
     * Sets one random bit in the BitSet from false to true, if this is possible (i.e., if not all bits are set already). The number of the set bit is
     * returned or -1 if all bits were true.
     * 
     * @param bitSet the BitSet to modify
     * @param size the size of the BitSet
     * 
     * @return the number of the set bit (or -1 if no false bit existed)
     */

    private int setRandomBit(BitSet bitSet, int size) {
        int ones = bitSet.cardinality();

        if (ones >= size) {
            return -1;
        }

        int zeroes = size - ones;

        int bit;
        int pos;

        // choose random bit number
        bit = random.nextInt(zeroes);

        // set the bit'th zero bit

        pos = bitSet.nextClearBit(0);

        while (bit-- > 0) {
            pos = bitSet.nextClearBit(pos + 1);
        }

        bitSet.set(pos);

        return pos;
    }

    /**
     * Clears one random bit in the BitSet, if this is possible (i.e., if at least one bit is set to true). The number of the cleared bit is returned
     * or -1 if all bits were false.
     * 
     * @param bitSet the BitSet to modify
     * 
     * @return the number of the cleared bit (or -1 if no true bit existed)
     */

    private int clearRandomBit(BitSet bitSet) {
        int ones = bitSet.cardinality();

        if (ones == 0) {
            return -1;
        }

        int bit;

        // choose random bit number
        bit = random.nextInt(ones);

        // skip to the bit'th one bit
        int pos = bitSet.nextSetBit(0);

        while (bit-- > 0) {
            pos = bitSet.nextSetBit(pos + 1);
        }

        bitSet.clear(pos);

        return pos;
    }

    /**
     * Returns the number of wanted ActivityVectors for the given section.
     *
     * @param section the chord section number
     * @param sections the total number of chord sections
     * @param maxActivityVectors the maximum number of ActivityVectors
     * @param lastCount the number of ActivityVectors of the previous chord section
     * @param startActivityCounts the start activity counts of the song
     * @param stopActivityCounts the stop activity counts of the song
     * @return the wanted number of active ActivityVectors
     */

    private int getWantedActivityVectorCount(int section, int sections, int maxActivityVectors, int lastCount, int[] startActivityCounts,
            int[] stopActivityCounts) {
        int count;

        int increaseTill = Math.min(sections / 2, startActivityCounts.length) - 1;
        int decreaseFrom = sections - Math.min(sections / 2, stopActivityCounts.length + 1);

        if (section <= increaseTill) {
            // in fade-in phase
            count = startActivityCounts[section];
        } else if (section == decreaseFrom) {
            // chord section directly before the fade-out phase
            int firstStop = stopActivityCounts[section - decreaseFrom];
            int c = (lastCount + firstStop) / 2;

            while ((c == lastCount || c == firstStop) && c < maxActivityVectors) {
                c++;
            }

            count = c;
        } else if (section >= decreaseFrom + 1) {
            // in fade-out phase
            count = stopActivityCounts[section - decreaseFrom - 1];
        } else {
            // in between
            int min = Math.min(maxActivityVectors, minActivityCount);

            int num;

            do {
                num = min + random.nextInt(maxActivityVectors - min + 1);
            } while (Math.abs(num - lastCount) > maxActivityChangeCount || num == lastCount && random.nextFloat() >= 0.1f);

            count = num;
        }

        return count;
    }

    public void setArrangementEntries(ArrangementEntry[] arrangementEntries) {
        this.arrangementEntries = arrangementEntries;
    }

    @Override
    public void configure(SongContext songContext, Node node) throws XPathException {
        random = new Random(randomSeed);

        constraintMode = ConstraintMode.EXACT;
        String constraintModeString;

        try {
            constraintModeString = XMLUtils.parseString(random, "constraintMode", node);

            if (constraintModeString != null) {
                constraintMode = ConstraintMode.valueOf(constraintModeString.toUpperCase());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing constraint mode", e);
        }

        setConstraintMode(constraintMode);

        int maxIterations;

        if (constraintMode == ConstraintMode.EXACT) {
            maxIterations = 1000000;
        } else {
            maxIterations = 1000;
        }

        try {
            maxIterations = XMLUtils.parseInteger(random, "maxIterations", node);
        } catch (Exception e) {}

        setMaxIterations(maxIterations);

        int[] startActivityCounts = XMLUtils.parseIntegerListString(random, "startActivityCounts", node);

        if (startActivityCounts == null) {
            startActivityCounts = new int[] {1, 2, 3};
        }

        setStartActivityCounts(startActivityCounts);

        int[] stopActivityCounts = XMLUtils.parseIntegerListString(random, "stopActivityCounts", node);

        if (stopActivityCounts == null) {
            stopActivityCounts = new int[] {3, 2, 1};
        }

        setStopActivityCounts(stopActivityCounts);

        int minActivityCount = XMLUtils.parseInteger(random, "minActivityCount", node);
        setMinActivityCount(minActivityCount);

        int maxActivityCount = XMLUtils.parseInteger(random, "maxActivityCount", node);
        setMaxActivityCount(maxActivityCount);

        int maxActivityChangeCount = XMLUtils.parseInteger(random, "maxActivityChangeCount", node);
        setMaxActivityChangeCount(maxActivityChangeCount);

        NodeList nodeList = XMLUtils.getNodeList("activityVector", node);

        int activityVectorCount = nodeList.getLength();

        if (activityVectorCount == 0) {
            throw new RuntimeException("Need at least 1 ActivityVector");
        }

        Map<String, ActivityVectorConfiguration> activityVectorConfigurationHashMap = new LinkedHashMap<String, ActivityVectorConfiguration>(
                activityVectorCount);

        for (int i = 0; i < activityVectorCount; i++) {
            String name = XMLUtils.parseString(random, "@name", nodeList.item(i));

            if (activityVectorConfigurationHashMap.containsKey(name)) {
                throw new RuntimeException("ActivityVector \"" + name + "\" already defined");
            }

            double minActive = 0;

            try {
                minActive = Double.parseDouble(XMLUtils.parseString(random, "minActive", nodeList.item(i)));
            } catch (Exception e) {}

            boolean allowInactive = false;

            try {
                allowInactive = XMLUtils.parseBoolean(random, "minActive/@allowInactive", nodeList.item(i));
            } catch (Exception e) {}

            double maxActive = 100.0d;

            try {
                maxActive = Double.parseDouble(XMLUtils.parseString(random, "maxActive", nodeList.item(i)));
            } catch (Exception e) {}

            int startShift = 0;
            try {
                startShift = XMLUtils.parseInteger(random, "startShift", nodeList.item(i));
            } catch (Exception e) {}

            int stopShift = 0;
            try {
                stopShift = XMLUtils.parseInteger(random, "stopShift", nodeList.item(i));
            } catch (Exception e) {}

            int startBeforeSection = Integer.MAX_VALUE;
            try {
                startBeforeSection = XMLUtils.parseInteger(random, "startBeforeSection", nodeList.item(i));
            } catch (Exception e) {}

            int startAfterSection = -1;
            try {
                startAfterSection = XMLUtils.parseInteger(random, "startAfterSection", nodeList.item(i));
            } catch (Exception e) {}

            int stopBeforeSection = -1;
            try {
                stopBeforeSection = XMLUtils.parseInteger(random, "stopBeforeSection", nodeList.item(i));
            } catch (Exception e) {}

            int stopAfterSection = Integer.MAX_VALUE;
            try {
                stopAfterSection = XMLUtils.parseInteger(random, "stopAfterSection", nodeList.item(i));
            } catch (Exception e) {}

            int minSegmentCount = 0;
            try {
                minSegmentCount = XMLUtils.parseInteger(random, "minSegmentCount", nodeList.item(i));
            } catch (Exception e) {}

            int maxSegmentCount = Integer.MAX_VALUE;
            try {
                maxSegmentCount = XMLUtils.parseInteger(random, "maxSegmentCount", nodeList.item(i));
            } catch (Exception e) {}

            int minSegmentLength = 0;
            try {
                minSegmentLength = XMLUtils.parseInteger(random, "minSegmentLength", nodeList.item(i));
            } catch (Exception e) {}

            int maxSegmentLength = Integer.MAX_VALUE;
            try {
                maxSegmentLength = XMLUtils.parseInteger(random, "maxSegmentLength", nodeList.item(i));
            } catch (Exception e) {}

            int minPauseLength = 0;
            try {
                minPauseLength = XMLUtils.parseInteger(random, "minPauseLength", nodeList.item(i));
            } catch (Exception e) {}

            int maxPauseLength = Integer.MAX_VALUE;
            try {
                maxPauseLength = XMLUtils.parseInteger(random, "maxPauseLength", nodeList.item(i));
            } catch (Exception e) {}

            activityVectorConfigurationHashMap.put(name, new ActivityVectorConfiguration(name, minActive, allowInactive, maxActive, startShift,
                    stopShift, startBeforeSection, startAfterSection, stopBeforeSection, stopAfterSection, minSegmentCount, maxSegmentCount,
                    minSegmentLength, maxSegmentLength, minPauseLength, maxPauseLength));
        }

        setActivityVectorConfiguration(activityVectorConfigurationHashMap);

        nodeList = getActiveTrackNodes(node);
        int tracks = nodeList.getLength();

        if (tracks == 0) {
            throw new RuntimeException("Need at least 1 track");
        }

        ArrangementEntry[] arrangementEntries = new ArrangementEntry[tracks];

        for (int i = 0; i < tracks; i++) {
            String instrument = XMLUtils.parseString(random, "instrument", nodeList.item(i));

            if (instrument == null || instrument.equals("")) {
                throw new RuntimeException("Track has no instrument");
            }

            int transposition = 0;

            try {
                transposition = XMLUtils.parseInteger(random, "transposition", nodeList.item(i));
            } catch (Exception e) {}

            int velocity = songContext.getStructure().getMaxVelocity();

            try {
                velocity = XMLUtils.parseInteger(random, "velocity", nodeList.item(i));
            } catch (Exception e) {}

            NodeList sequenceEngineNodeList = XMLUtils.getNodeList("sequenceEngine", nodeList.item(i));

            NodeList nameNodeList = XMLUtils.getNodeList("activityVector", nodeList.item(i));

            String[] activityVectorNames = new String[nameNodeList.getLength()];

            for (int k = 0; k < nameNodeList.getLength(); k++) {
                activityVectorNames[k] = nameNodeList.item(k).getTextContent();
            }

            try {
                SequenceEngine sequenceEngine = XMLUtils.getInstance(songContext, SequenceEngine.class, sequenceEngineNodeList.item(random.nextInt(
                        sequenceEngineNodeList.getLength())), randomSeed, i);
                arrangementEntries[i] = new ArrangementEntry(instrument, sequenceEngine, transposition, velocity, activityVectorNames);
            } catch (Exception e) {
                throw new RuntimeException("Error instantiating SequenceEngine for instrument \"" + instrument + "\"", e);
            }
        }

        setArrangementEntries(arrangementEntries);

        nodeList = XMLUtils.getNodeList("activityVectorModification", node);
        int modificationCount = nodeList.getLength();
        ActivityVectorModification[] activityVectorModifications = new ActivityVectorModification[modificationCount];

        for (int i = 0; i < modificationCount; i++) {
            String modeString = XMLUtils.parseString(random, "@operator", nodeList.item(i));

            if (modeString == null) {
                modeString = "";
            }

            ActivityVectorModificationOperator operator;

            try {
                operator = Enum.valueOf(ActivityVectorModificationOperator.class, modeString.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid activity vector modification mode \"" + modeString + "\"");
            }

            String from = XMLUtils.parseString(random, "@from", nodeList.item(i));
            String to = XMLUtils.parseString(random, "@to", nodeList.item(i));
            String operand1 = XMLUtils.parseString(random, "@operand1", nodeList.item(i));
            String operand2 = XMLUtils.parseString(random, "@operand2", nodeList.item(i));
            String target = XMLUtils.parseString(random, "@target", nodeList.item(i));

            activityVectorModifications[i] = new ActivityVectorModification(operator, from, to, operand1, operand2, target);
        }

        this.activityVectorModifications = activityVectorModifications;
    }

    /**
     * Returns a node list of all active tracks. If at least one track is soloed, all soloed tracks are returned. Otherwise, all tracks which have not
     * been muted are returned.
     * 
     * @param node the node
     * @return the node list
     * @throws XPathExpressionException in case of an XPath problem
     */
    private NodeList getActiveTrackNodes(Node node) throws XPathExpressionException {
        // search for all soloed tracks

        NodeList nodeList = XMLUtils.getNodeList("track[@solo=\"true\"]", node);

        if (nodeList.getLength() == 0) {
            // no soloed tracks found, search for all tracks which are not muted

            nodeList = XMLUtils.getNodeList("track[@mute!=\"true\" or not(@mute)]", node);
        }

        return nodeList;
    }

    /**
     * Returns the maximum number of ActivityVectors to use at the same time, given the total number of ActivityVectors available. The method uses an
     * exponential drop-off so that the returned value is 1 for activityVectors = 1 and the value converges down to activityVectors*factor as
     * activityVectors goes to infinity. The lambda value specifies the speed of the exponential drop-off. The goal is to use almost all
     * ActivityVectors when the number of ActivityVectors is small and use (in relation) fewer ActivityVectors when the number of ActivityVectors
     * becomes larger.
     * 
     * @param activityVectors the number of ActivtiyVectors (must be positive)
     * @param factor the factor (between 0 and 1)
     * @param lambda the drop-off factor (must be positive)
     * 
     * @return the maximum number of ActivityVectors to use
     */

    private int getActivityVectorMaximum(int activityVectors, double factor, double lambda) {
        return (int) (0.5d + activityVectors * (factor + (1d - factor) * Math.exp(-lambda * (activityVectors - 1))));
    }

    public void setActivityVectorConfiguration(Map<String, ActivityVectorConfiguration> activityVectorConfigurationHashMap) {
        this.activityVectorConfigurationHashMap = activityVectorConfigurationHashMap;
    }

    /**
     * Processes the activity counts.
     * 
     * @param activityCounts the activity counts
     * @param maxCount the maximum count
     * @return the processed activity counts
     */

    private int[] processActivityCounts(int[] activityCounts, int maxCount) {
        int[] newActivityCounts = new int[activityCounts.length];

        for (int i = 0; i < activityCounts.length; i++) {
            if (activityCounts[i] <= 0) {
                throw new RuntimeException("All activity counts must be positive");
            } else {
                newActivityCounts[i] = Math.min(activityCounts[i], maxCount);
            }
        }

        return newActivityCounts;
    }

    public void setMinActivityCount(int minActiveCount) {
        this.minActivityCount = minActiveCount;
    }

    public void setMaxActivityCount(int maxActiveCount) {
        this.maxActivityCount = maxActiveCount;
    }

    public void setMaxActivityChangeCount(int maxActivityChangeCount) {
        this.maxActivityChangeCount = maxActivityChangeCount;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public ConstraintMode getConstraintMode() {
        return constraintMode;
    }

    public void setConstraintMode(ConstraintMode constraintMode) {
        this.constraintMode = constraintMode;
    }

    public void setStartActivityCounts(int[] startActivityCounts) {
        this.startActivityCounts = startActivityCounts;
    }

    public void setStopActivityCounts(int[] stopActivityCounts) {
        this.stopActivityCounts = stopActivityCounts;
    }

    /**
     * Represents an arrangement entry.
     */

    private static final class ArrangementEntry {
        /** The instrument name. */
        private final String instrument;

        /** The sequence engine. */
        private final SequenceEngine sequenceEngine;

        /** The transposition. */
        private final int transposition;

        /** The transposition. */
        private final int velocity;

        /** The names of ActivityVectors. */
        private final String[] activityVectorNames;

        /**
         * Constructor.
         * 
         * @param instrument the instrument name
         * @param sequenceEngine the SequenceEngine
         * @param transposition the transposition
         * @param velocity the velocity
         * @param activityVectorNames the ActivityVector names
         */

        private ArrangementEntry(String instrument, SequenceEngine sequenceEngine, int transposition, int velocity, String[] activityVectorNames) {
            this.instrument = instrument;
            this.sequenceEngine = sequenceEngine;
            this.transposition = transposition;
            this.velocity = velocity;
            this.activityVectorNames = activityVectorNames;
        }
    }

    /**
     * Container for ActivityVector configurations.
     */

    private static final class ActivityVectorConfiguration {
        /** The name. */
        private String name;

        /** The minimum activity (between 0 and 1). */
        private double minActive;

        /** The minimum number of active chord sections. */
        private int minActiveSectionCount;

        /** Boolean indicating whether complete inactivity is allowed. */
        private boolean allowInactive;

        /** The maximum activity (between 0 and 1). */
        private double maxActive;

        /** The maximum number of active chord sections. */
        private int maxActiveSectionCount;

        /** The start shift in ticks. */
        private int startShift;

        /** The stop shift in ticks. */
        private int stopShift;

        /** The number of chord sections to forbid activity, counted from the start. */
        private int startAfterSection;

        /** The latest section number where the activity must start, counted from the start. */
        private int startBeforeSection;

        /** The number of chord sections to forbid activity, counted from the end. */
        private int stopBeforeSection;

        /** The latest section number where the activity must stop, counted from the end. */
        private int stopAfterSection;

        /** The minimum number of active chord sections. */
        private int minSegmentCount;

        /** The maximum number of active chord sections. */
        private int maxSegmentCount;

        /** The minimum activity segment length, counted in chord sections. */
        private int minSegmentLength;

        /** The maximum activity segment length, counted in chord sections. */
        private int maxSegmentLength;

        /** The minimum pause segment length, counted in chord sections. */
        private int minPauseLength;

        /** The maximum pause segment length, counted in chord sections. */
        private int maxPauseLength;

        /** The ActivityVector instance. */
        private ActivityVector activityVector;

        private ActivityVectorConfiguration(String name, double minActive, boolean allowInactive, double maxActive, int startShift, int stopShift,
                int startBeforeSection, int startAfterSection, int stopBeforeSection, int stopAfterSection, int minSegmentCount, int maxSegmentCount,
                int minSegmentLength, int maxSegmentLength, int minPauseLength, int maxPauseLength) {
            this.name = name;
            this.minActive = minActive;
            this.allowInactive = allowInactive;
            this.maxActive = maxActive;
            this.startShift = startShift;
            this.stopShift = stopShift;
            this.startBeforeSection = startBeforeSection;
            this.startAfterSection = startAfterSection;
            this.stopBeforeSection = stopBeforeSection;
            this.stopAfterSection = stopAfterSection;
            this.minSegmentCount = minSegmentCount;
            this.maxSegmentCount = maxSegmentCount;
            this.minSegmentLength = minSegmentLength;
            this.maxSegmentLength = maxSegmentLength;
            this.minPauseLength = minPauseLength;
            this.maxPauseLength = maxPauseLength;
        }
    }

    /**
     * Holds the state for an ActivityVector during building the constrained AVs.
     */

    private static final class ActivityVectorState {
        /** The number of active chord sections to far. */
        private int activeCount;

        /** The number of activity segments so far. */
        private int segments;

        /**
         * The length of the current segment, counted in chord sections (if inactive this is the pause segment length).
         */
        private int segmentLength;

        /** Boolean indicating whether the activity vector is active within the stop interval. */
        private boolean activeInStopInterval;

        ActivityVectorState() {
        }

        ActivityVectorState(ActivityVectorState state) {
            this.activeCount = state.activeCount;
            this.segments = state.segments;
            this.segmentLength = state.segmentLength;
            this.activeInStopInterval = state.activeInStopInterval;
        }
    }

    private static final class ActivityVectorModification {
        /** The operator. */
        private ActivityVectorModificationOperator operator;

        /** The from tick. */
        private String from;

        /** The till tick. */
        private String to;

        /** The first ActivityVector operand. */
        private String operand1;

        /** The second ActivityVector operand. */
        private String operand2;

        /** The target ActivityVector operand. */
        private String target;

        /**
         * Constructor.
         * 
         * @param operator the operator
         * @param from the from tick
         * @param to the to tick
         * @param operand1 the first operand
         * @param operand2 the second operand
         * @param target the target operand
         */

        private ActivityVectorModification(ActivityVectorModificationOperator operator, String from, String to, String operand1, String operand2,
                String target) {
            int operands = operator.operands;

            if (operands == 0 && (operand1 != null || operand2 != null) || operands == 1 && (operand1 == null || operand2 != null) || operands == 2
                    && (operand1 == null || operand2 == null)) {
                throw new IllegalArgumentException("Mode \"" + operator + "\" needs exactly " + operands + " operand(s)");
            }

            this.operator = operator;
            this.from = from;
            this.to = to;
            this.operand1 = operand1;
            this.operand2 = operand2;
            this.target = target;
        }
    }
}
