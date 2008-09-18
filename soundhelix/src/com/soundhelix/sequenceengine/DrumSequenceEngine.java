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

public class DrumSequenceEngine extends SequenceEngine {

	// pitch for each of the note pattern strings
	int[] pitch = {36,37,40,44,39,42};
	
	String[] notes4 = new String[] {
			"10001000100010001000100010001001100010001000100010001000100010101000100010001000100010001000100110001000100010001000100011101011",      // base
			"00001000000010000000100000001000000010000000100000001000000110100000100000001000000010000000100000001000000010000000100001001000",  // clap
            "10001000100010001100100010001000",    // closed
			"0010001000100010001000100010001000100010001000100010001000100101",    // open hihat
            "001",
            "00001000010010000000100001001010000010000100100000001000010010110000100001001000000010010100101000001000010010000000100101001011"  // snare
	};

	String[] notes3 = new String[] {
			"100100100100100100100100",      // base
			"000100000100000100000101",     // clap
			"100100100100100100100100",      // closed
			"001",
			"010",
			"000100000101000100000100000100000101000100000111"
	};

	// 0: bass
	// 1: base
	// 2: clap
	// 3: hihats
	// 4: snare
	
	
	int[] vectorMapping = new int[] {0,1,2,2,2,3};
	
	public DrumSequenceEngine() {
		super();
	}
	
	public Track render(ActivityVector... activityVector) {
		Track track = new Track(TrackType.RHYTHM);

		int factor;
		
		String[] notes;		
		int ticksPerBeat = structure.getTicksPerBeat();
		
		switch(ticksPerBeat) {
		case 12:
		case 6:
		case 3: notes = notes3;
		        factor = 1;
		        break;
		      
		case 16:
		case 8:
		case 4: notes = notes4;
		        factor = 1;
		        break;

		case 2: notes = notes4;
        		factor = 2;
        		break;

		case 1: notes = notes4;
        		factor = 4;
        		break;
        		
        default:
        		throw(new RuntimeException("Invalid ticks per beat"));
		}
		
		for(int i=0;i<activityVector.length;i++) {
			System.out.println("Vector "+(i+1)+": "+activityVector[i]);
		}
		
		
		for(int i=0;i<notes.length;i++) {
        	Sequence seq = new Sequence();
      	
        	for(int tick=0,patternTick=0;tick<structure.getTicks();tick++,patternTick++) {
        		if(activityVector[vectorMapping[i]].isActive(tick) && notes[i].charAt((patternTick*factor)%notes[i].length()) == '1') {
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
		return 4;
	}
	
    public void configure(Node node,XPath xpath) throws XPathException {
    }
}
