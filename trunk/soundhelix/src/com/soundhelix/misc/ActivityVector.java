package com.soundhelix.misc;

import java.util.BitSet;

/**
 * Represents a bit vector specifying for each tick whether a voice
 * should be active or not. This vector should be considered
 * a strong hint for a SequenceEngine whether to add notes or to add pauses.
 * However, it is not strictly forbidden to play notes while inactive. For example,
 * after an activity interval, a final note could be played at the start of the
 * following inactivity interval.
 *
 * An ActivityVector must always span the whole length of a song.
 * 
 * @see com.soundhelix.sequenceengine.SequenceEngine
 *
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class ActivityVector {
    private BitSet bitSet = new BitSet();
    private int totalTicks;
    
    /**
     * Appends an activity interval with the specified number
     * of ticks.
     * 
     * @param ticks the number of ticks
     */
    
    public void addActivity(int ticks) {
    	bitSet.set(totalTicks,totalTicks+ticks);
    	totalTicks += ticks;
    }

    /**
     * Appends an inactivity interval with the specified number
     * of ticks.
     * 
     * @param ticks the number of ticks
     */
    
    public void addInactivity(int ticks) {
    	bitSet.clear(totalTicks,totalTicks+ticks);
    	totalTicks += ticks;
    }
    
    /**
     * Returns the activity state of the specified tick. If the
     * tick number is beyond the end of the vector, false is returned.
     * 
     * @param tick the tick
     * 
     * @return true if the tick is active, false otherwise
     */
    
    public boolean isActive(int tick) {
    	if(tick >= totalTicks)
    		return false;
    	
    	return bitSet.get(tick);
    }
    
    /**
     * Returns the length of the interval beginning with the
     * given tick, i.e., the number of ticks until the activity
     * state changes or the end of the vector is reached.
     * 
     * @param tick the tick
     * 
     * @return the number of ticks until the next change or the vector ends
     */
    
    public int getIntervalLength(int tick) {
    	if(bitSet.get(tick)) {
    		// bit is active; search for the next clear bit
    		// this bit must exist, because we use a BitSet
    		return bitSet.nextClearBit(tick)-tick;
    	} else {
    		// bit is inactive; search for the next set bit
    		// if there is no set bit, return the remaining
    		// number of ticks (the number of ticks until the
    		// song ends)
    		
    		int num = bitSet.nextSetBit(tick);
    		
    		if(num == -1) {
    			return totalTicks-tick;
    		} else {
    			return num-tick;
    		}
    	}
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
     * Returns the tick where the ActivityVector becomes active for the
     * first time. If the ActvityVector never becomes active, -1 is
     * returned.
     * 
     * @return the first activity tick (or -1)
     */
    
    public int getFirstActiveTick() {
    	return bitSet.nextSetBit(0);
    }

    /**
     * Returns the tick where the ActivityVector becomes inactive for
     * the first time. If the ActvityVector never becomes in inactive,
     * -1 is returned.
     * 
     * @return the first inactivity tick
     */
    
    public int getFirstInactiveTick() {
    	int tick = bitSet.nextClearBit(0);
    	
    	if(tick >= totalTicks) {
    		return -1;
    	} else {
    		return tick;
    	}
    }
    
    /**
     * Modifies the ActivityVector so that it has the given state in
     * the interval from from (inclusive) to till (exclusive). The
     * vector will be extended, if necessary.
     * 
     * @param from the starting tick (inclusive)
     * @param till the ending tick (exclusive)
     * @param state the state of the interval
     */
    
    public void setActivityState(int from,int till,boolean state) {
    	if(till > totalTicks) {
    		totalTicks = till;
    	}
    	
    	bitSet.set(from,till,state);
    }
    
	/**
	 * Modifies the ActivityVector so that all interval changes
	 * from inactive to active are preponed by preStartTicks and all
	 * changes from active to inactive preponed by preStopTicks.
	 * preStartTicks and preStopTicks may also be negative to postpone
	 * instead of prepone. The first interval (starting at tick 0) is
	 * never modified.
	 * 
	 * @param preStartTicks the number of ticks to prepone before or postpone after starting
	 * @param preStopTicks the number of ticks to prepone before or postpone after stopping
	 */
	
	public void shiftIntervalBoundaries(int preStartTicks,int preStopTicks) {
		int tick = 0;
		
		while(tick < totalTicks) {
			tick += getIntervalLength(tick);
			
			boolean active = isActive(tick);
			
			if(preStopTicks > 0 && !active) {
				setActivityState(tick-preStopTicks,tick,false);
			} else if(preStopTicks < 0 && tick < totalTicks && !active) {
				setActivityState(tick,tick-preStopTicks,true);
				tick  -= preStopTicks;
			} else if(preStartTicks > 0 && active) {				
				setActivityState(tick-preStartTicks,tick,true);
			} else if(preStartTicks < 0 && active) {
				setActivityState(tick,tick-preStartTicks,false);
				tick  -= preStartTicks;
			}
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		int tick=0;
		
		while(tick < totalTicks) {
			
			int len = getIntervalLength(tick);
			
			if(sb.length() > 0) sb.append(',');
			sb.append(isActive(tick) ? "1/"+len : "0/"+len);
			
			tick += len;
		}

		return sb.toString();
	}
}
