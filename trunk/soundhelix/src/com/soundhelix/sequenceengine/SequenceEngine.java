package com.soundhelix.sequenceengine;

/**
 * Represents an abstract generator for note sequences. This class provides
 * a dummy implementation of RandomSeedable, which is a no-op (it stores the
 * random seed and can return the stored seed, but doesn't use the seed). Subclasses
 * that want to make use of random-seedability must override setRandomSeed() and
 * getRandomSeed() accordingly. This is to make sure that a subclass is able to
 * react to calls to setRandomSeed() to re-seed the internal random generators.
 * 
 * @see Sequence
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

import org.apache.log4j.Logger;

import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.RandomSeedable;
import com.soundhelix.misc.Structure;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.XMLConfigurable;

public abstract class SequenceEngine implements XMLConfigurable,RandomSeedable {
	protected final Logger logger;
	
	protected Structure structure;
	
	// this is private on intention, because it should
	// not be read/modified by subclasses
	private long randomSeed = System.nanoTime();
	
    public SequenceEngine() {
    	logger = Logger.getLogger(this.getClass());
    }

    public void setStructure(Structure structure) {
    	this.structure = structure;
    }
    
	/**
	 * Renders one or more sequences (i.e., voices). The
	 * method should check the given ActivityVectors to decide when to insert
	 * notes and when to insert pauses. The method should also take care that
	 * played notes are not sustained beyond inactive intervals of the
	 * ActivityVector (however, it is not strictly forbidden to do so). The
	 * returned list must contain at least one sequence. The length of each
	 * sequence must match the length of the song. The method must take the
	 * song's HarmonyEngine into consideration.
	 */

     public abstract Track render(ActivityVector[] activityVectors);

     /**
      * Returns the required number of ActivityVectors. For most implementations,
      * this will be 1, but certain implementations, like for multi-instrument
      * sequences, more than 1 might be required. Subclasses should override
      * this.
      * 
      * @return the number of ActivityVectors
      */
     
     public int getActivityVectorCount() {
    	 return 1;
     }
     
     public void setRandomSeed(long randomSeed) {
    	 this.randomSeed = randomSeed;
     }

     public long getRandomSeed() {
    	 return randomSeed;
     }
}
