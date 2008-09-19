package com.soundhelix.sequenceengine;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Track.TrackType;

/**
 * Implements a sequence engine that repeats user-specified patterns.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

// TODO: make all patterns configurable (including mapping from patterns to ActivityVectors)

public class DrumSequenceEngine extends SequenceEngine {

	// pitch for each of the note pattern strings
	int[] pitch = {36,37,40,44,39,42};

	// notes for ticks per beat that are a power of 2
	String[] notes4 = new String[] {
			// base
			"10001000100010001000100010001001100010001000100010001000100010101000100010001000100010001000100110001000100010001000100011101011",
			// clap
			"00001000000010000000100000001000000010000000100000001000000110100000100000001000000010000000100000001000000010000000100001001000",
            // closed hihat
			"10001000100010001100100010001000",
			// open hihat
			"0010001000100010001000100010001000100010001000100010001000100101",
            // tick
			"001",
            // snare
            "00001000010010000000100001001010000010000100100000001000010010110000100001001000000010010100101000001000010010000000100101001011"
	};

	// notes for ticks per beat that are 3 times a power of 2
	String[] notes3 = new String[] {
			// base
			"100100100100100100100100",
			// clap
			"000100000100000100000101",
			// closed hihat
			"100100100100100100100100",
			// open hihat
			"001",
			// tick
			"010",
			// snare
			"000100000101000100000100000100000101000100000111"
	};

	// maps note lanes to ActivityVectors
	// we give the 3 hihat lanes (2, 3 and 4) the same ActivityVector
	int[] vectorMapping = new int[] {0,1,2,2,2,3};
	
	public DrumSequenceEngine() {
		super();
	}
	
	public Track render(ActivityVector... activityVector) {
		Track track = new Track(TrackType.RHYTHM);

		int stretchFactor;
		
		String[] notes;		
		int ticksPerBeat = structure.getTicksPerBeat();
		
		switch(ticksPerBeat) {
		case 12:
		case 6:
		case 3: notes = notes3;
		        stretchFactor = 1;
		        break;
		      
		case 16:
		case 8:
		case 4: notes = notes4;
		        stretchFactor = 1;
		        break;

		case 2: notes = notes4;
        		stretchFactor = 2;
        		break;

		case 1: notes = notes4;
        		stretchFactor = 4;
        		break;
        		
        default:
        		throw(new RuntimeException("Invalid ticks per beat"));
		}
	
		for(int i=0;i<notes.length;i++) {
        	Sequence seq = new Sequence();
      	
        	for(int tick=0,patternTick=0;tick<structure.getTicks();tick++,patternTick++) {
        		if(activityVector[vectorMapping[i]].isActive(tick) && notes[i].charAt((patternTick*stretchFactor)%notes[i].length()) == '1') {
        			seq.addNote(pitch[i],1);
        		} else {
        			seq.addPause(1);
        		}
        		
        		switch(ticksPerBeat) {
        		case 6:
        		case 8: seq.addPause(1);
        				tick++;
        		        break;
        		        
        		case 12:
        		case 16: seq.addPause(3);
        		         tick += 3;
        		         break;
        		}
        	}
      
        	track.add(seq);
		}    
		
        return track;
	}
	
	public int getActivityVectorCount() {
		// we generate 6 sequences, but these are
		// based on 4 ActivityVectors
		return 4;
	}
	
    public void configure(Node node,XPath xpath) throws XPathException {
    }
}
