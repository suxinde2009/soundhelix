package com.soundhelix.sequenceengine;

import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.RandomSeedable;
import com.soundhelix.misc.Structure;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.XMLConfigurable;

/**
 * Represents a generator for sequences. Generated sequences are combined into a track.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public interface SequenceEngine extends XMLConfigurable, RandomSeedable {
    /**
     * Sets the structure.
     * 
     * @param structure the structure
     */

    void setStructure(Structure structure);

    /**
     * Renders one or more sequences (i.e., voices) as a track. The method should check the given ActivityVectors to decide when to insert notes and
     * when to insert pauses. The method should also take care that played notes are not sustained beyond inactive intervals of the ActivityVector
     * (however, it is not strictly forbidden to do so). The returned track must contain at least one sequence. The length of each sequence must match
     * the length of the song. The method must take the song's HarmonyEngine into consideration.
     * 
     * @param activityVectors the activity vectors
     * 
     * @return the track
     */

    Track render(ActivityVector[] activityVectors);

    /**
     * Returns the required number of ActivityVectors. For most implementations, this will be 1, but certain implementations, like for
     * multi-instrument sequences, more than 1 might be required.
     * 
     * @return the number of ActivityVectors
     */

    int getActivityVectorCount();
}
