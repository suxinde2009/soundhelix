package com.soundhelix.component.sequenceengine;

import com.soundhelix.component.Component;
import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.SongContext;
import com.soundhelix.misc.Track;

/**
 * Represents a generator for sequences. Generated sequences are combined into a track.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public interface SequenceEngine extends Component {
    /**
     * Renders one or more sequences (i.e., voices) as a track. The method should check the given ActivityVectors to decide when to insert notes and
     * when to insert pauses. The method should also take care that played notes are not sustained beyond inactive intervals of the ActivityVector
     * (however, it is not strictly forbidden to do so). The returned track must contain at least one sequence. The length of each sequence must match
     * the length of the song. The method must take the song's harmony into consideration.
     * 
     * @param songContext the song context
     * @param activityVectors the activity vectors
     * 
     * @return the track
     */

    Track render(SongContext songContext, ActivityVector[] activityVectors);

    /**
     * Returns the required number of ActivityVectors. For most implementations, this will be 1, but certain implementations, like for
     * multi-instrument sequences, more than 1 might be required.
     * 
     * @return the number of ActivityVectors
     */

    int getActivityVectorCount();
}
