package com.soundhelix.arrangementengine;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.harmonyengine.HarmonyEngine;
import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Arrangement;
import com.soundhelix.misc.Track;
import com.soundhelix.sequenceengine.SequenceEngine;
import com.soundhelix.util.HarmonyEngineUtils;
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
 * - minActive(n): the AV must be active for at least n% of the song; granularity is at chord section level; it is
 *                 also possible define that the AV may be inactive for the whole song or be active with at least n%
 *                 of the song
 * - maxActive(n): the AV must be active for at most n% of the song; granularity is at chord section level
 * - startBeforeSection(n): the AV must start before section n
 * - startAfterSection(n): the AV must start after section n
 * - stopBeforeSection(n): the AV must stop before section n, counted from the end (0 is the last chord section)
 * - stopAfterSection(n): the AV must stop after section n, counted from the end (0 is the last chord section)
 * - minSegmentCount(n): the AV must be active for at least n chord section segments
 * - maxSegmentCount(n): the AV must be active for at most n chord section segments
 * - minSegmentLength(n): the minimum AV segment length must be n chord sections
 * - maxSegmentLength(n): the maximum AV segment length must be n chord sections
 * - minPauseLength(n): the minimum AV pause length must be n chord sections
 * - maxPauseLength(n): the maximum AV pause length must be n chord sections
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
    /** The random generator. */
    private Random random;

    /** The maximum delta that activity can change in each chord section. */
    private int maxActivityChangeCount = 2;

    /** The minimum number of active ActivityVectors. */
    private int minActivityCount = 2;

    /** The maximum number of active ActivityVectors (determined dynamically if <= 0). */
    private int maxActivityCount;

    private String startActivityCountsString;
    private String stopActivityCountsString;

    private ArrangementEntry[] arrangementEntries;
    private Map<String, ActivityVectorConfiguration> activityVectorConfigurationHashMap;

    /** The maximum number of iterations before failing. */
    private int maxIterations;

    public SimpleArrangementEngine() {
        super();
    }

    public Arrangement render() {
        // SequenceEngines have been instantiated and configured, but the
        // Structure has not been set yet
        for (ArrangementEntry entry : arrangementEntries) {
            entry.sequenceEngine.setStructure(structure);
        }

        Map<String, ActivityVectorConfiguration> neededActivityVectors = getNeededActivityVectors();

        ActivityVectorConfiguration[] vectors = neededActivityVectors.values().toArray(new ActivityVectorConfiguration[neededActivityVectors.size()]);

        int tracks = arrangementEntries.length;

        if (logger.isDebugEnabled()) {
            logger.debug("Creating " + vectors.length + " ActivityVectors for " + tracks + " track" + (tracks == 1 ? "" : "s"));
        }

        // create the song activity matrix
        createConstrainedActivityVectors(vectors);
        dumpActivityVectors(vectors);
        shiftIntervalBoundaries(neededActivityVectors);

        Arrangement arrangement = createArrangement(neededActivityVectors);
        return arrangement;
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
     * @param neededActivityVector the map of needed activity vectors
     *
     * @return the created arrangement
     */

    private Arrangement createArrangement(Map<String, ActivityVectorConfiguration> neededActivityVector) {
        // use each SequenceEngine to render a track
        // each SequenceEngine is given the number of ActivityVectors it
        // requires

        Arrangement arrangement = new Arrangement(structure);

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

            Track track = sequenceEngine.render(list);
            track.transpose(entry.transposition);
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

    private void createConstrainedActivityVectors(ActivityVectorConfiguration[] activityVectorConfigurations) {

        int sections = HarmonyEngineUtils.getChordSectionCount(structure);
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

        int[] startActivityCounts = parseActivityCounts(startActivityCountsString, vectors);
        int[] stopActivityCounts = parseActivityCounts(stopActivityCountsString, vectors);

        int increaseTill = Math.min(sections / 2, startActivityCounts.length) - 1;
        int decreaseFrom = sections - Math.min(sections / 2, stopActivityCounts.length + 1);

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

    nextSection:
        while (section < sections) {
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
            int wantedCount = getWantedActivityVectorCount(section, sections, maxActivityVectors, lastWantedCount, increaseTill, decreaseFrom,
                    startActivityCounts, stopActivityCounts);

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
            logger.debug("Fulfilled constraints in " + ((end - startTime) / 1000000L) + " ms. Iterations: " + iterations + " ("
                    + (iterations * 1000000000L / (end - startTime)) + " iterations/s), violations: " + violations + ", backtrack steps: "
                    + backtracks);
        }

        convertBitSetsToActivityVectors(activityVectorConfigurations, bitSets);
    }

    /**
     * Converts the BitSets into a set of ActivityVectors which have their activity set based on the BitSets.
     *
     * @param activityVectorConfigurations the array of activity vector configurations
     * @param bitSets the array of BitSets
     */

    private void convertBitSetsToActivityVectors(ActivityVectorConfiguration[] activityVectorConfigurations, BitSet[] bitSets) {
        int vectors = activityVectorConfigurations.length;
        int ticks = structure.getTicks();

        for (int i = 0; i < vectors; i++) {
            ActivityVectorConfiguration avc = activityVectorConfigurations[i];
            ActivityVector av = new ActivityVector(avc.name, ticks);
            // make sure that the ticks of all AVs equals the song length (setActivityState() below does not
            // ensure this, because it only sets active segments in the AV)
            av.addInactivity(ticks);
            avc.activityVector = av;
        }

        HarmonyEngine harmonyEngine = structure.getHarmonyEngine();
        int section = 0;
        int tick = 0;

        while (tick < ticks) {
            BitSet bitSet = bitSets[section];
            int length = harmonyEngine.getChordSectionTicks(tick);

            int pos = bitSet.nextSetBit(0);

            while (pos >= 0) {
                activityVectorConfigurations[pos].activityVector.setActivityState(tick, tick + length, true);
                pos = bitSet.nextSetBit(pos + 1);
            }

            tick += length;
            section++;

        }
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
     * Dumps the activity vectors as an activity matrix.
     *
     * @param vectors the array of activity vector configurations
     */

    private void dumpActivityVectors(ActivityVectorConfiguration[] vectors) {
        if (!logger.isDebugEnabled()) {
            return;
        }

        StringBuilder sb = new StringBuilder("Song structure:\n");

        int chordSections = HarmonyEngineUtils.getChordSectionCount(structure);

        int digits = String.valueOf(chordSections - 1).length();
        int div = 1;

        for (int i = 1; i < digits; i++) {
            div *= 10;
        }

        int ticks = structure.getTicks();
        int maxLen = 0;

        for (ActivityVectorConfiguration avc : vectors) {
            maxLen = Math.max(maxLen, avc.name.length());
        }

        maxLen = Math.max(maxLen, "Section #".length());

        for (int d = 0; d < digits; d++) {
            if (d == 0) {
                sb.append(String.format("%" + maxLen + "s: ", "Section #"));
            } else {
                sb.append(String.format("%" + maxLen + "s  ", ""));
            }

            int n = 0;
            for (int tick = 0; tick < ticks; tick += structure.getHarmonyEngine().getChordSectionTicks(tick)) {
                sb.append((n / div) % 10);
                n++;
            }
            sb.append('\n');
            div /= 10;
        }

        for (int i = 0; i < maxLen + chordSections + 9; i++) {
            sb.append('=');
        }

        sb.append('\n');

        for (ActivityVectorConfiguration avc : vectors) {
            sb.append(String.format("%" + maxLen + "s: ", avc.name));

            ActivityVector av = avc.activityVector;

            for (int tick = 0; tick < ticks; tick += structure.getHarmonyEngine().getChordSectionTicks(tick)) {
                sb.append(av.isActive(tick) ? '*' : '-');
            }

            int activeTicks = av.getActiveTicks();
            sb.append(activeTicks > 0 ? String.format(" %5.1f%%%n", 100.0d * activeTicks / ticks) : "\n");
        }

        for (int i = 0; i < maxLen + chordSections + 9; i++) {
            sb.append('=');
        }

        sb.append('\n');

        sb.append(String.format("%" + maxLen + "s: ", "# active"));

        for (int tick = 0; tick < ticks; tick += structure.getHarmonyEngine().getChordSectionTicks(tick)) {
            int c = 0;

            for (ActivityVectorConfiguration avc : vectors) {
                if (avc.activityVector.isActive(tick)) {
                    c++;
                }
            }

            // output number in base-36 format (0-9,a-z)
            sb.append(Integer.toString(c, 36));
        }

        logger.debug(sb.toString());
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

    private int getWantedActivityVectorCount(int section, int sections, int maxActivityVectors, int lastCount, int increaseTill, int decreaseFrom,
            int[] startActivityCounts, int[] stopActivityCounts) {
        int count;

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
            } while (Math.abs(num - lastCount) > maxActivityChangeCount || (num == lastCount && random.nextFloat() >= 0.1f));

            count = num;
        }

        return count;
    }

    public void setArrangementEntries(ArrangementEntry[] arrangementEntries) {
        this.arrangementEntries = arrangementEntries;
    }

    public void setStartActivityCountsString(String startActivityCountsString) {
        this.startActivityCountsString = startActivityCountsString;
    }

    public void setStopActivityCountsString(String stopActivityCountsString) {
        this.stopActivityCountsString = stopActivityCountsString;
    }

    public void configure(Node node, XPath xpath) throws XPathException {
        random = new Random(randomSeed);

        int maxIterations = 1000000;

        try {
            maxIterations = XMLUtils.parseInteger(random, "maxIterations", node, xpath);
        } catch (Exception e) {
        }

        setMaxIterations(maxIterations);

        String activityString = XMLUtils.parseString(random, "startActivityCounts", node, xpath);

        if (activityString == null) {
            activityString = "1,2,3";
        }

        setStartActivityCountsString(activityString);

        activityString = XMLUtils.parseString(random, "stopActivityCounts", node, xpath);

        if (activityString == null) {
            activityString = "3,2,1";
        }

        setStopActivityCountsString(activityString);

        int minActivityCount = XMLUtils.parseInteger(random, "minActivityCount", node, xpath);
        setMinActivityCount(minActivityCount);

        int maxActivityCount = XMLUtils.parseInteger(random, "maxActivityCount", node, xpath);
        setMaxActivityCount(maxActivityCount);

        int maxActivityChangeCount = XMLUtils.parseInteger(random, "maxActivityChangeCount", node, xpath);
        setMaxActivityChangeCount(maxActivityChangeCount);

        NodeList nodeList = (NodeList) xpath.evaluate("activityVector", node, XPathConstants.NODESET);

        int activityVectorCount = nodeList.getLength();

        if (activityVectorCount == 0) {
            throw new RuntimeException("Need at least 1 ActivityVector");
        }

        Map<String, ActivityVectorConfiguration> activityVectorConfigurationHashMap = new LinkedHashMap<String, ActivityVectorConfiguration>(
                activityVectorCount);

        for (int i = 0; i < activityVectorCount; i++) {
            String name = XMLUtils.parseString(random, "attribute::name", nodeList.item(i), xpath);

            if (activityVectorConfigurationHashMap.containsKey(name)) {
                throw new RuntimeException("ActivityVector \"" + name + "\" already defined");
            }

            double minActive = 0;

            try {
                minActive = Double.parseDouble(XMLUtils.parseString(random, "minActive", nodeList.item(i), xpath));
            } catch (Exception e) {
            }

            boolean allowInactive = false;

            try {
                allowInactive = XMLUtils.parseBoolean(random, "minActive/attribute::allowInactive", nodeList.item(i), xpath);
            } catch (Exception e) {
            }

            double maxActive = 100.0d;

            try {
                maxActive = Double.parseDouble(XMLUtils.parseString(random, "maxActive", nodeList.item(i), xpath));
            } catch (Exception e) {
            }

            int startShift = 0;
            try {
                startShift = XMLUtils.parseInteger(random, "startShift", nodeList.item(i), xpath);
            } catch (Exception e) {
            }

            int stopShift = 0;
            try {
                stopShift = XMLUtils.parseInteger(random, "stopShift", nodeList.item(i), xpath);
            } catch (Exception e) {
            }

            int startBeforeSection = Integer.MAX_VALUE;
            try {
                startBeforeSection = XMLUtils.parseInteger(random, "startBeforeSection", nodeList.item(i), xpath);
            } catch (Exception e) {
            }

            int startAfterSection = -1;
            try {
                startAfterSection = XMLUtils.parseInteger(random, "startAfterSection", nodeList.item(i), xpath);
            } catch (Exception e) {
            }

            int stopBeforeSection = -1;
            try {
                stopBeforeSection = XMLUtils.parseInteger(random, "stopBeforeSection", nodeList.item(i), xpath);
            } catch (Exception e) {
            }

            int stopAfterSection = Integer.MAX_VALUE;
            try {
                stopAfterSection = XMLUtils.parseInteger(random, "stopAfterSection", nodeList.item(i), xpath);
            } catch (Exception e) {
            }

            int minSegmentCount = 0;
            try {
                minSegmentCount = XMLUtils.parseInteger(random, "minSegmentCount", nodeList.item(i), xpath);
            } catch (Exception e) {
            }

            int maxSegmentCount = Integer.MAX_VALUE;
            try {
                maxSegmentCount = XMLUtils.parseInteger(random, "maxSegmentCount", nodeList.item(i), xpath);
            } catch (Exception e) {
            }

            int minSegmentLength = 0;
            try {
                minSegmentLength = XMLUtils.parseInteger(random, "minSegmentLength", nodeList.item(i), xpath);
            } catch (Exception e) {
            }

            int maxSegmentLength = Integer.MAX_VALUE;
            try {
                maxSegmentLength = XMLUtils.parseInteger(random, "maxSegmentLength", nodeList.item(i), xpath);
            } catch (Exception e) {
            }

            int minPauseLength = 0;
            try {
                minPauseLength = XMLUtils.parseInteger(random, "minPauseLength", nodeList.item(i), xpath);
            } catch (Exception e) {
            }

            int maxPauseLength = Integer.MAX_VALUE;
            try {
                maxPauseLength = XMLUtils.parseInteger(random, "maxPauseLength", nodeList.item(i), xpath);
            } catch (Exception e) {
            }

            activityVectorConfigurationHashMap.put(name, new ActivityVectorConfiguration(name, minActive, allowInactive, maxActive, startShift,
                    stopShift, startBeforeSection, startAfterSection, stopBeforeSection, stopAfterSection, minSegmentCount, maxSegmentCount,
                    minSegmentLength, maxSegmentLength, minPauseLength, maxPauseLength));
        }

        setActivityVectorConfiguration(activityVectorConfigurationHashMap);

        nodeList = (NodeList) xpath.evaluate("track[@solo=\"true\"]", node, XPathConstants.NODESET);
        int tracks = nodeList.getLength();

        if (tracks == 0) {
            nodeList = (NodeList) xpath.evaluate("track", node, XPathConstants.NODESET);
            tracks = nodeList.getLength();

            if (tracks == 0) {
                throw new RuntimeException("Need at least 1 track");
            }
        }

        ArrangementEntry[] arrangementEntries = new ArrangementEntry[tracks];

        for (int i = 0; i < tracks; i++) {
            String instrument = XMLUtils.parseString(random, "instrument", nodeList.item(i), xpath);

            if (instrument == null || instrument.equals("")) {
                throw new RuntimeException("Track has no instrument");
            }

            int transposition = 0;

            try {
                transposition = XMLUtils.parseInteger(random, "transposition", nodeList.item(i), xpath);
            } catch (Exception e) {
            }

            Node sequenceEngineNode = (Node) xpath.evaluate("sequenceEngine", nodeList.item(i), XPathConstants.NODE);

            NodeList nameNodeList = (NodeList) xpath.evaluate("activityVector", nodeList.item(i), XPathConstants.NODESET);

            String[] activityVectorNames = new String[nameNodeList.getLength()];

            for (int k = 0; k < nameNodeList.getLength(); k++) {
                activityVectorNames[k] = nameNodeList.item(k).getTextContent();
            }

            try {
                SequenceEngine sequenceEngine = XMLUtils.getInstance(SequenceEngine.class, sequenceEngineNode, xpath, randomSeed, i);
                arrangementEntries[i] = new ArrangementEntry(instrument, sequenceEngine, transposition, activityVectorNames);
            } catch (Exception e) {
                throw new RuntimeException("Error instantiating SequenceEngine for instrument \"" + instrument + "\"", e);
            }
        }

        setArrangementEntries(arrangementEntries);
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

    private int[] parseActivityCounts(String string, int maxCount) {
        String[] c = string.split(",");
        int[] activityCounts = new int[c.length];

        for (int i = 0; i < c.length; i++) {
            try {
                activityCounts[i] = Integer.parseInt(c[i]);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Element \"" + c[i] + "\" in activity count string \"" + string + "\" is not a number");
            }

            if (activityCounts[i] <= 0) {
                throw new RuntimeException("Element \"" + activityCounts[i] + "\" in activity count string \"" + string + "\" is not positive");
            } else if (activityCounts[i] > maxCount) {
                activityCounts[i] = maxCount;
            }
        }

        return activityCounts;
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

    private static final class ArrangementEntry {
        private String instrument;
        private SequenceEngine sequenceEngine;
        private int transposition;
        private String[] activityVectorNames;

        private ArrangementEntry(String instrument, SequenceEngine sequenceEngine, int transposition, String[] activityVectorNames) {
            this.instrument = instrument;
            this.sequenceEngine = sequenceEngine;
            this.transposition = transposition;
            this.activityVectorNames = activityVectorNames;
        }
    }

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
    }
}