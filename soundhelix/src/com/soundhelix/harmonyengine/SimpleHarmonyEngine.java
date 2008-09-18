package com.soundhelix.harmonyengine;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Chord.ChordSubtype;
import com.soundhelix.misc.Chord.ChordType;

public class SimpleHarmonyEngine extends HarmonyEngine {

	public SimpleHarmonyEngine() {
		super();
	}
	
	public Chord getChord(int tick) {
		switch((tick/structure.getTicksPerBar()) & 7) {
		case 0: return new Chord(-3,ChordType.MINOR,ChordSubtype.BASE_0);
		case 1: return new Chord(-7,ChordType.MAJOR,ChordSubtype.BASE_0);
		case 2:
		case 3: return new Chord(-10,ChordType.MINOR,ChordSubtype.BASE_4);
		case 4: return new Chord(-3,ChordType.MINOR,ChordSubtype.BASE_0);
		case 5: return new Chord(0,ChordType.MAJOR,ChordSubtype.BASE_0);
		case 6:
		case 7: return new Chord(-5,ChordType.MAJOR,ChordSubtype.BASE_0);
		}
		
		return null;
	}

	public int getChordTicks(int tick) {
        int bar = tick / structure.getTicksPerBar();
		int t = tick % structure.getTicksPerBar();
		int t2 = tick % (structure.getTicksPerBar()*2);
        
		if((bar&3) < 2)
        	return(structure.getTicksPerBar()-t);
        else
        	return(structure.getTicksPerBar()*2-t2);
	}

	public int getChordSectionTicks(int tick) {
		int t = tick % (structure.getTicksPerBar()*4);
		return structure.getTicksPerBar()*4-t;
	}
	
	public void configure(Node node,XPath xpath) throws XPathException {
	}
}
