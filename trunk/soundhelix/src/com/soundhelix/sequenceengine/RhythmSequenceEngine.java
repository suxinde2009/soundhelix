package com.soundhelix.sequenceengine;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Track.TrackType;

/**
 * Implements a sequence engine for loop players.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

// TODO: make this all configurable or drop the class

public class RhythmSequenceEngine extends AbstractSequenceEngine {

	/** The pitch for each of the note pattern strings. */
    private int[] pitch = {1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 0, 0,
                           1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0};
			
	public RhythmSequenceEngine() {
		super();
	}
	
	public Track render(ActivityVector... activityVectors) {
		ActivityVector activityVector = activityVectors[0];

		Track track = new Track(TrackType.RHYTHM);

      	Sequence seq = new Sequence();

      	int ticksPerBeat = structure.getTicksPerBeat();
      	
      	int p = 0;
      	for (int tick = 0, patternTick = 0; tick < structure.getTicks(); tick++, patternTick++) {
      		if ((patternTick % pitch.length) == 0) {
      			p = 36;
      		}

      		if (activityVector.isActive(tick) && pitch[patternTick % pitch.length] > 0) {
      			seq.addNote(p, 1);
      			p++;
      		} else {
      			seq.addPause(1);
      		}

      		switch(ticksPerBeat) {
      		case 8:
      		case 6:
      			seq.addPause(1);
      			tick++;
      			break;
      		
      		case 12:
      		case 16:
      			seq.addPause(3);
      			tick += 3;
      			break;

      		}
      	}
      	
      	track.add(seq);
    
        return track;
	}
	
	
    public void configure(Node node, XPath xpath) throws XPathException {
    }
}
