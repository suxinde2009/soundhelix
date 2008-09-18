package com.soundhelix.sequenceengine;

/**
 * Represents an abstract generator for note sequences.
 * 
 * @see Sequence
 * 
 * @author Thomas Schürger
 */

import org.apache.log4j.Logger;

import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Structure;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.XMLConfigurable;

public abstract class SequenceEngine implements XMLConfigurable {
	protected final Logger logger;

	protected Structure structure;
	
    public SequenceEngine() {
    	logger = Logger.getLogger(this.getClass());
    }

    public void setStructure(Structure structure) {
    	this.structure = structure;
    }
    
	/**
	 * Renders one or more sequences (i.e., voices) for the given song. The
	 * method should check the given ActivityVector to decide when to insert
	 * notes and when to insert pauses. The method should also take care that
	 * played notes are not sustained beyond inactive intervals of the
	 * ActivityVector (however, it is not strictly forbidden to do so). The
	 * returned list must contain at least one sequence. The length of each
	 * sequence must match the length of the song. The method must take the
	 * song's HarmonyEngine into consideration but must not use the song's
	 * transposition.
	 */

     public abstract Track render(ActivityVector... activityVectors);

     /**
      * Returns the required number of ActivityVectors. For most implementations,
      * this will be 1, but certain implementations, like for multi-instrument
      * sequences, more than 1 might be required. Subclasses should override
      * this.
      * 
      * @return
      */
     
     public int getActivityVectorCount() {
    	 return 1;
     }
}
