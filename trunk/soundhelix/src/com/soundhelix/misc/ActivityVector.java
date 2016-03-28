package com.soundhelix.misc;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Represents a bit vector specifying for each tick whether a voice should be active or not. The bit vector grows dynamically as needed. This vector
 * should be considered a strong hint for a SequenceEngine whether to add notes or to add pauses. However, it is not strictly forbidden to play notes
 * while inactive. For example, after an activity interval, a final note could be played at the start of the following inactivity interval.
 * 
 * An ActivityVector must always span the whole length of a song.
 * 
 * @see com.soundhelix.component.sequenceengine.SequenceEngine
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class ActivityVector {
    /** The bit set used (each bit represents a tick activity). */
    private final BitSet bitSet;

    /** The length of the vector in ticks. */
    private int totalTicks;

    /** The name of the ActivityVector. */
    private String name;

    /**
     * Constructor. Initializes the internal BitSet's initial size to the default number of bits.
     * 
     * @param name the name
     */

    public ActivityVector(String name) {
        this.name = name;
        bitSet = new BitSet();
    }

    /**
     * Constructor. Initializes the internal BitSet's initial size to the given number of bits. This method should be used when the final number of
     * bits is (roughly) known in advance.
     * 
     * @param name the name
     * @param bits the initial number of bits
     */

    public ActivityVector(String name, int bits) {
        this.name = name;
        bitSet = new BitSet(bits);
    }

    /**
     * Appends an activity interval with the specified number of ticks.
     * 
     * @param ticks the number of ticks
     */

    public void addActivity(int ticks) {
        bitSet.set(totalTicks, totalTicks + ticks);
        totalTicks += ticks;
    }

    /**
     * Appends an inactivity interval with the specified number of ticks.
     * 
     * @param ticks the number of ticks
     */

    public void addInactivity(int ticks) {
        bitSet.clear(totalTicks, totalTicks + ticks);
        totalTicks += ticks;
    }

    /**
     * Returns the activity state of the specified tick. If the tick number is beyond the end of the vector, false is returned.
     * 
     * @param tick the tick
     * 
     * @return true if the tick is active, false otherwise
     */

    public boolean isActive(int tick) {
        if (tick >= totalTicks) {
            return false;
        }

        return bitSet.get(tick);
    }

    /**
     * Returns the length of the interval beginning with the given tick, i.e., the number of ticks until the activity state changes or the end of the
     * vector is reached.
     * 
     * @param tick the tick
     * 
     * @return the number of ticks until the next change or the vector ends
     */

    public int getIntervalLength(int tick) {
        if (bitSet.get(tick)) {
            // bit is active; search for the next clear bit
            // this bit must exist, because we use a BitSet
            return bitSet.nextClearBit(tick) - tick;
        } else {
            // bit is inactive; search for the next set bit
            // if there is no set bit, return the remaining
            // number of ticks (the number of ticks until the
            // song ends)

            int num = bitSet.nextSetBit(tick);

            if (num == -1) {
                if (tick >= totalTicks) {
                    return 0;
                } else {
                    return totalTicks - tick;
                }
            } else {
                return num - tick;
            }
        }
    }

    /**
     * Returns the name of this ActivityVector.
     * 
     * @return the name
     */

    public String getName() {
        return name;
    }

    /**
     * Returns the total number of ticks this ActivityVector spans.
     * 
     * @return the total number of ticks
     */

    public int getTicks() {
        return totalTicks;
    }

    /**
     * Returns the number of ticks this ActivityVector is active.
     * 
     * @return the number of active ticks
     */

    public int getActiveTicks() {
        return bitSet.cardinality();
    }

    /**
     * Returns the tick where the ActivityVector becomes active for the first time. If the ActvityVector never becomes active, -1 is returned.
     * 
     * @return the first activity tick (or -1)
     */

    public int getFirstActiveTick() {
        return bitSet.nextSetBit(0);
    }

    /**
     * Returns the tick where the ActivityVector is active for the last time. If the ActvityVector never becomes active, -1 is returned.
     * 
     * @return the last activity tick (or -1)
     */

    public int getLastActiveTick() {
        return bitSet.length() - 1;
    }

    /**
     * Returns the tick where the ActivityVector becomes inactive for the first time. If the ActvityVector never becomes in inactive, -1 is returned.
     * 
     * @return the first inactivity tick
     */

    public int getFirstInactiveTick() {
        int tick = bitSet.nextClearBit(0);

        if (tick >= totalTicks) {
            return -1;
        } else {
            return tick;
        }
    }

    /**
     * Modifies the ActivityVector so that it has the given state in the interval from from (inclusive) to till (exclusive). The vector will be
     * extended, if necessary.
     * 
     * @param from the starting tick (inclusive)
     * @param till the ending tick (exclusive)
     * @param state the state of the interval
     */

    public void setActivityState(int from, int till, boolean state) {
        if (till > totalTicks) {
            totalTicks = till;
        }

        bitSet.set(from, till, state);
    }

    /**
     * Flips all bits of the ActivityVector in the given tick range.
     * 
     * @param from the from tick (inclusive)
     * @param till the till tick (exclusive)
     */

    public void flipActivityState(int from, int till) {
        if (till > totalTicks) {
            totalTicks = till;
        }

        bitSet.flip(from, till);
    }

    /**
     * Modifies the ActivityVector so that all interval changes from inactive to active are postponed by startTicks and all changes from active to
     * inactive are postponed by stopTicks ticks. startTicks and stopTicks may also be negative to prepone instead of postpone. The start of the first
     * interval is never modified, whereas the end of the last interval is never postponed.
     * 
     * @param startTicks the number of ticks to prepone or postpone starting
     * @param stopTicks the number of ticks to prepone or postpone stopping
     */

    public void shiftIntervalBoundaries(int startTicks, int stopTicks) {
        if (startTicks == 0 && stopTicks == 0) {
            return;
        }

        int tick = 0;

        while (tick < totalTicks) {
            tick += getIntervalLength(tick);

            boolean active = isActive(tick);

            if (stopTicks < 0 && !active) {
                setActivityState(tick + stopTicks, tick, false);
            } else if (stopTicks > 0 && tick < totalTicks && !active) {
                setActivityState(tick, tick + stopTicks, true);
                tick += stopTicks;
            } else if (startTicks < 0 && active) {
                setActivityState(tick + startTicks, tick, true);
            } else if (startTicks > 0 && active) {
                setActivityState(tick, tick + startTicks, false);
                tick += startTicks;
            }
        }
    }

    /**
     * Counts the number of activity segments, which is the number of consecutive blocks of activity in the vector.
     * 
     * @return the number of activity segments
     */

    public int getActivitySegmentCount() {
        int segments = 0;
        int pos = -1;

        while (true) {
            pos = bitSet.nextSetBit(pos + 1);

            if (pos == -1) {
                return segments;
            }

            segments++;

            pos = bitSet.nextClearBit(pos + 1);

            if (pos == -1) {
                return segments;
            }
        }
    }

    /**
     * Returns a string representation of the ActivityVector.
     * 
     * @return a string representation
     */

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        sb.append('=');

        int tick = 0;
        boolean first = true;

        while (tick < totalTicks) {

            int len = getIntervalLength(tick);

            if (!first) {
                sb.append(',');
            } else {
                first = false;
            }

            sb.append(isActive(tick) ? "1/" + len : "0/" + len);

            tick += len;
        }

        return sb.toString();
    }

    /**
     * Returns an array that contains all activity and pause segment lengths in ticks, sorted by starting tick number. Every activity segment will
     * have a positive length, every pause segment will have a negative length.
     * 
     * @return the array containing the segment lengths
     */

    public int[] getSegmentLengths() {
        List<Integer> list = new ArrayList<Integer>();

        int tick = 0;

        while (tick < totalTicks) {
            if (bitSet.get(tick)) {
                int nextTick = bitSet.nextClearBit(tick);
                list.add(nextTick - tick);
                tick = nextTick;
            } else {
                int nextTick = bitSet.nextSetBit(tick);

                if (nextTick == -1) {
                    nextTick = totalTicks;
                }

                list.add(tick - nextTick);
                tick = nextTick;
            }
        }

        // convert list to int array

        int size = list.size();
        int[] result = new int[size];

        for (int i = 0; i < size; i++) {
            result[i] = list.get(i);
        }

        return result;
    }

    /**
     * Applies a logical NOT to the BitSet operand and replaces the given range of this ActivityVector's BitSet with the result.
     * 
     * @param operand the operand
     * @param fromTick the from tick (inclusive)
     * @param tillTick the till tick (exclusive)
     */

    public void applyLogicalNot(ActivityVector operand, int fromTick, int tillTick) {
        if (operand.getTicks() != totalTicks) {
            throw new IllegalArgumentException("Operand must have the same number of ticks as the target");
        }

        try {
            // bitSet is modified by the operation, so we must clone it first
            BitSet bitSet = (BitSet) operand.bitSet.clone();
            bitSet.flip(fromTick, tillTick);
            replaceBitSetRange(bitSet, fromTick, tillTick);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Applies a logical AND between the two BitSet operands and replaces the given range of this ActivityVector's BitSet with the result.
     * 
     * @param operand1 the first operand
     * @param operand2 the second operand
     * @param fromTick the from tick (inclusive)
     * @param tillTick the till tick (exclusive)
     */

    public void applyLogicalAnd(ActivityVector operand1, ActivityVector operand2, int fromTick, int tillTick) {
        if (operand1.getTicks() != totalTicks || operand2.getTicks() != totalTicks) {
            throw new IllegalArgumentException("Operands must have the same number of ticks as the target");
        }

        try {
            // bitSet1 is modified by the operation, so we must clone it first
            BitSet bitSet1 = (BitSet) operand1.bitSet.clone();
            BitSet bitSet2 = operand2.bitSet;
            bitSet1.and(bitSet2);
            replaceBitSetRange(bitSet1, fromTick, tillTick);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Applies a logical AND NOT between the two BitSet operands and replaces the given range of this ActivityVector's BitSet with the result.
     * 
     * @param operand1 the first operand
     * @param operand2 the second operand
     * @param fromTick the from tick (inclusive)
     * @param tillTick the till tick (exclusive)
     */

    public void applyLogicalAndNot(ActivityVector operand1, ActivityVector operand2, int fromTick, int tillTick) {
        if (operand1.getTicks() != totalTicks || operand2.getTicks() != totalTicks) {
            throw new IllegalArgumentException("Operands must have the same number of ticks as the target");
        }

        try {
            // bitSet1 is modified by the operation, so we must clone it first
            BitSet bitSet1 = (BitSet) operand1.bitSet.clone();
            BitSet bitSet2 = operand2.bitSet;
            bitSet1.andNot(bitSet2);
            replaceBitSetRange(bitSet1, fromTick, tillTick);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Applies a logical OR between the two BitSet operands and replaces the given range of this ActivityVector's BitSet with the result.
     * 
     * @param operand1 the first operand
     * @param operand2 the second operand
     * @param fromTick the from tick (inclusive)
     * @param tillTick the till tick (exclusive)
     */

    public void applyLogicalOr(ActivityVector operand1, ActivityVector operand2, int fromTick, int tillTick) {
        if (operand1.getTicks() != totalTicks || operand2.getTicks() != totalTicks) {
            throw new IllegalArgumentException("Operands must have the same number of ticks as the target");
        }

        try {
            // bitSet1 is modified by the operation, so we must clone it first
            BitSet bitSet1 = (BitSet) operand1.bitSet.clone();
            BitSet bitSet2 = operand2.bitSet;
            bitSet1.or(bitSet2);
            replaceBitSetRange(bitSet1, fromTick, tillTick);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Applies a logical XOR between the two BitSet operands and replaces the given range of this ActivityVector's BitSet with the result.
     * 
     * @param operand1 the first operand
     * @param operand2 the second operand
     * @param fromTick the from tick (inclusive)
     * @param tillTick the till tick (exclusive)
     */

    public void applyLogicalXor(ActivityVector operand1, ActivityVector operand2, int fromTick, int tillTick) {
        if (operand1.getTicks() != totalTicks || operand2.getTicks() != totalTicks) {
            throw new IllegalArgumentException("Operands must have the same number of ticks as the target");
        }

        try {
            // bitSet1 is modified by the operation, so we must clone it first
            BitSet bitSet1 = (BitSet) operand1.bitSet.clone();
            BitSet bitSet2 = operand2.bitSet;
            bitSet1.xor(bitSet2);
            replaceBitSetRange(bitSet1, fromTick, tillTick);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Replaces this ActivityVector's BitSet range from fromTick (inclusive) to tillTick (exclusive) with the content of the given source BitSet in
     * that range.
     * 
     * @param sourceBitSet the source BitSet to copy the range from
     * @param fromTick the start tick of the range (inclusive)
     * @param tillTick the end tick of the range (exclusive)
     */

    private void replaceBitSetRange(BitSet sourceBitSet, int fromTick, int tillTick) {
        // clear the range of the target BitSet
        bitSet.clear(fromTick, tillTick);

        for (int i = sourceBitSet.nextSetBit(fromTick); i >= 0 && i < tillTick; i = sourceBitSet.nextSetBit(i + 1)) {
            bitSet.set(i);
        }
    }
}
