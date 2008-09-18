package com.soundhelix.arrangementengine;

import java.util.BitSet;
import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.harmonyengine.HarmonyEngine;
import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Arrangement;
import com.soundhelix.sequenceengine.SequenceEngine;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a simple ArrangementEngine.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class SimpleArrangementEngine extends ArrangementEngine {
	private Random random = new Random();

	private ArrangementEntry[] arrangementEntries;
	
	public SimpleArrangementEngine() {
		super();
	}
	
	/*
	public Arrangement renderOld() {
       SequenceEngine se;
       ActivityVector[] av;
       
       int ticks = structure.getTicks();
       
       do {
    	   av = createActivityVectors(13);
       } while(av[3].isActive(0) || av[3].isActive(ticks-1) || 100*av[3].getActiveTicks()/ticks < 50);
       
       Arrangement arrangement = new Arrangement(structure);

       // arpeggio
       se = new ArpeggioSequenceEngine(structure);       
       arrangement.add(se.render(av[0]),0);
       
       // pad
       se = new PadSequenceEngine(structure);
       av[1].shiftIntervalBoundaries(0,-8);
       arrangement.add(se.render(av[1]),1);
       
       // bass
       //se = new PatternSequenceEngine(song,"0,-,-,0,-,-,0,-,1,-,-,0,-,-,0,-,0,-,-,0,-,-,0,-,0,-,-1,-,0,-,-,-,0,-,-,0,-,-,0,-,1,-,-,0,-,-,0,-,0,-,-,0,-,-,0,-,0,-,-1,-,0,-,4,1");       
       se = new PatternSequenceEngine(structure,"0,-,-,0,-,-,1,-,0,-,-,0,-,-,+,-,0,-,-,0,-,-,2,-,0,-,-,0,-,-,+,-");
       av[2].shiftIntervalBoundaries(0,-1);
       arrangement.add(se.render(av[2]),2);
       
       // drums 
       DrumSequenceEngine dse = new DrumSequenceEngine(structure);
       av[3].shiftIntervalBoundaries(0,-4);
       av[4].shiftIntervalBoundaries(8,8);
       arrangement.add(dse.render(av[3],av[4],av[5],av[6]),3);
           
       // acid
       //se = new PatternSequenceEngine(song,"0,0,1,-,0,0,-,2,0,0,1,-,-,3,1,4");  
       se = new PatternSequenceEngine(structure,"-,-,0,-,-,2,-,-,-,-,0,-,-,-,-,3,-,-,0,-,-,2,-,-,-,-,0,-,-,3,2,4,-,-,0,-,-,2,-,-,-,-,0,-,-,-,-,3,-,-,0,-,-,2,-,-,-,-,0,5,7,3,5,4");  
       arrangement.add(se.render(av[7]),4);

       // chords
       se = new ChordSequenceEngine(structure,"0,-,-,-,0,-,-,0,-,-,0,-,-,0,-,-,0,-,-,-,0,-,-,0,-,-,0,-,3,-,1,-,0,-,-,-,0,-,-,0,-,-,0,-,-,0,-,-,0,-,-,-,0,-,-,0,-,-,0,-,3,1,4,2");
       arrangement.add(se.render(av[8]),5);
       
       // breakbeat
       se = new RhythmSequenceEngine(structure);
       arrangement.add(se.render(av[9]),7);
       
       // acid 2
       se = new PatternSequenceEngine(structure,"0,0,-,0,-,-,0,-,0,-,-,-,-,-,-,-,0,0,-,0,-,-,0,-,0,-,-,-,-,4,2,3,0,0,-,0,-,-,0,-,0,-,-,-,-,-,-,-,0,0,-,0,-,-,0,-,2,-,-,-,-,4,2,3");  
       arrangement.add(se.render(av[10]),8);

       // coolecho
       se = new PatternSequenceEngine(structure,"0,0,0,-,0,-,-,0,-,-,0,-,3,-,-,-");  
       arrangement.add(se.render(av[11]),9);
    
       // melody
       //se = new MelodySequenceEngine(structure,"0,-,+,-,+,-,0,-,-,-,-,-,+,-,-,-,0,-,+,-,+,-,+,-,-,-,-,-,-,-,-,-");  
       //se = new PatternSequenceEngine(structure,"0,-,-,-,1,-,+,-,0,-,-,-,2,-,+,-,0,-,-,-,3,-,+,-,0,-,-,-,+,-,-,-");
       //arrangement.add(se.render(av[12]),6);
        
       // acid
       se = new PatternSequenceEngine(structure,"0:1,-,0:1,-,0:1,-,-,0:1,-,0:1,-,0:1,1:12000,-,0:1,-,0:1,-,0:1,-,0:1,-,-,0:1,0:1,3:20000,0:1,-,2:1,-,0:1,-,0:1,-,0:1,-,0:1,-,-,0:1,-,0:1,-,0:1,1:18000,-,0:1,-,0:1,-,0:1,-,0:1,-,-,0:1,0:1,3:30000,0:1,-,2:1,-,0:1,-");  
       arrangement.add(se.render(av[12]),10);

       return arrangement;
	}
    */
	
	public Arrangement render() {
	       
	       int tracks = arrangementEntries.length;
	       
	       System.out.println("Rendering");
	       
	       System.out.println("Creating vectors for "+tracks+" tracks");
	       
	       int vectors = 0;
	       
	       for(int i=0;i<tracks;i++) {
	    	   SequenceEngine sequenceEngine = arrangementEntries[i].sequenceEngine;
	    	   sequenceEngine.setStructure(structure);
	    	   vectors += sequenceEngine.getActivityVectorCount();
	       }

	       int ticks = structure.getTicks();

	       System.out.println("Creating "+vectors+" ActivityVectors");
	       ActivityVector[] av;

	       int tries = 0;
	       
	       again: while(true) {
	    	   av = createActivityVectors(vectors);

	    	   int v = 0;

	    	   for(int i=0;i<tracks;i++) {
	    		   SequenceEngine sequenceEngine = arrangementEntries[i].sequenceEngine;

	    		   double[] minRatios = arrangementEntries[i].minRatios;
	    		   double[] maxRatios = arrangementEntries[i].maxRatios;

	    		   int num = sequenceEngine.getActivityVectorCount();

	    		   for(int k=0;k<num;k++) {
	    			   double ratio = 100.0d*(double)av[v].getActiveTicks()/(double)ticks;

	    			   if(ratio < minRatios[k] || ratio > maxRatios[k]) {
	    				   System.out.println("Track "+i+", vector "+k+"  Ratio: "+ratio+"  min: "+minRatios[k]+" max: "+maxRatios[k]);

	    				   tries++;
	    				   
	    				   if(tries >= 1000) {
	    					   throw(new RuntimeException("Could satisfy constraints within "+tries+" tries"));
	    				   }
	    				   
	    				   continue again;
	    			   }

	    			   v++;
	    		   }
	    	   }

	    	   break;

	       }
          
	   	       //} while(av[3].isActive(0) || av[3].isActive(ticks-1) || 100*av[3].getActiveTicks()/ticks < 50);
	       
	       Arrangement arrangement = new Arrangement(structure);

	       int vector = 0;
	       
	       for(int i=0;i<tracks;i++) {
	    	   SequenceEngine sequenceEngine = arrangementEntries[i].sequenceEngine;
	    	   int num = sequenceEngine.getActivityVectorCount();
	    	   
	    	   ActivityVector[] list = new ActivityVector[num];
	    	   
	    	   for(int k=0;k<num;k++) {
	    		   list[k] = av[vector++];
	    	   }
	   
	    	   arrangement.add(sequenceEngine.render(list),arrangementEntries[i].channel);
	       }

	       return arrangement;
		}
	/**
	 * Sets one random bit in the BitSet, if this is possible.
	 * From the set of false bits, one is chosen at random and that
	 * bit is set.
	 * 
	 * @param bitSet the BitSet to modify
	 * @param size the size of the BitSet
	 * @param avoidBit the bit number to avoid (or -1)
	 * 
	 * @return the number of the set bit (or -1 if no clear bit existed)
	 */
	
	private int setRandomBit(BitSet bitset,int size,int avoidBit) {
		int ones = bitset.cardinality();
		
		if(ones >= size) {
			return -1;
		}

		int zeroes = size-ones;
		
		int bit;
		
		do {		
		// choose random bit number
		    bit = random.nextInt(zeroes);
		} while(zeroes > 1 && bit == avoidBit);
		
		// set the bit'th zero bit
		
		int pos = bitset.nextClearBit(0);
		
		while(bit-- > 0) {
			pos = bitset.nextClearBit(pos+1);
		}
		
		bitset.set(pos);
		
		return pos;	
	}
	
	/**
	 * Clears one random bit in the BitSet, if this is possible.
	 * From the set of true bits, one is chosen at random and that
	 * bit is cleared.
	 * 
	 * @param bitSet the BitSet to modify
	 * @param avoidBit the bit number to avoid (or -1)
	 * 
	 * @return the number of the cleared bit (or -1 if no set bit existed)
	 */
	
	private int clearRandomBit(BitSet bitSet,int avoidBit) {
		int ones = bitSet.cardinality();
		
		if(ones == 0) {
			return -1;
		}

		int bit;
		
		do {
		// choose random bit number
		 bit = random.nextInt(ones);
		} while(ones > 1 && bit == avoidBit);
		
		// skip to the bit'th one bit
		
		int pos = bitSet.nextSetBit(0);
		
		while(bit-- > 0) {
			pos = bitSet.nextSetBit(pos+1);
		}
		
		bitSet.clear(pos);
		
		return pos;	
	}
	
	private ActivityVector[] createActivityVectors(int num) {
		HarmonyEngine he = structure.getHarmonyEngine();
		int sections = he.getChordSectionCount();
		ActivityVector[] av = new ActivityVector[num];
		
		for(int i=0;i<num;i++) {
			av[i] = new ActivityVector();
		}
		
		BitSet bitset = new BitSet();
		int tick = 0;
		
        for(int section=0;section<sections;section++) {
        	int len = he.getChordSectionTicks(tick);
        	
        	int tracks = getTrackCount(section,sections,num*5/8);
        	
        	int card = bitset.cardinality();
        	
        	int lastAddedBit = -1;
        	int lastRemovedBit = -1;
        	
        	if(card < tracks) {
        		do {lastAddedBit = setRandomBit(bitset,num,lastRemovedBit);} while(bitset.cardinality() < tracks);
        	} else if(card > tracks) {
        		do {lastRemovedBit = clearRandomBit(bitset,lastAddedBit);} while(bitset.cardinality() > tracks);
        	} else if(card > 0 && Math.random() > 0.2) {
        		lastRemovedBit = clearRandomBit(bitset,lastAddedBit);
        		lastAddedBit = setRandomBit(bitset,num,lastRemovedBit);
        	}
        	
        	System.out.println("Section: "+section+"  Tracks: "+tracks+"  BitSet: "+bitset);

        	for(int i=0;i<num;i++) {
        		if(bitset.get(i)) {
        			av[i].addActivity(len);
        		} else {
        			av[i].addInactivity(len);
        		}
        	}
       	
            tick += len;
        }
				
		return av;	
	}
	
	/**
	 * Returns the number of tracks that should be played during the
	 * given section. There is always a "fade-in" of tracks (1, 2, ...)
	 * for the first number of sections and a "fade-out" of tracks (..., 2, 1)
	 * for the last number of sections. In between, the number of tracks is
	 * chosen randomly.
	 * 
	 * @param section the section number (between 0 and sections-1)
	 * @param sections the total number of sections
	 * @param maxTracks the maximum number of tracks to use

	 * @return the number of tracks
	 */
	
	private int getTrackCount(int section,int sections,int maxTracks) {
		int increaseTill = Math.min(maxTracks,Math.min(sections/2,4))-1;
		int decreaseFrom = sections-Math.min(maxTracks,Math.min(sections/2,3));
		
		if(section <= increaseTill) {
			return section+1;
		} else if(section >= decreaseFrom) {
			return sections-section;
		} else {
			int min = Math.min(maxTracks,2);
			int max = maxTracks;
			
			return min+random.nextInt(max-min+1);
		}
	}
	
	public void setArrangementEntries(ArrangementEntry[] arrangementEntries) {
		this.arrangementEntries = arrangementEntries;
	}
	
	public void configure(Node node,XPath xpath) throws XPathException {
		NodeList nodeList = (NodeList)xpath.evaluate("track",node,XPathConstants.NODESET);

		int tracks = nodeList.getLength();
		
		ArrangementEntry[] arrangementEntries = new ArrangementEntry[tracks];
		
		for(int i=0;i<tracks;i++) {
			int channel = XMLUtils.parseInteger("channel",nodeList.item(i),xpath);
			
			String minRatios = XMLUtils.parseString("minRatios",nodeList.item(i),xpath);
			String maxRatios = XMLUtils.parseString("maxRatios",nodeList.item(i),xpath);
			
			Node sequenceEngineNode = (Node)xpath.evaluate("sequenceEngine",nodeList.item(i),XPathConstants.NODE);

			try {
			    SequenceEngine sequenceEngine = XMLUtils.getInstance(SequenceEngine.class,sequenceEngineNode,xpath);
			    arrangementEntries[i] = new ArrangementEntry(channel,sequenceEngine,parseRatios(minRatios,sequenceEngine.getActivityVectorCount(),0d),parseRatios(maxRatios,sequenceEngine.getActivityVectorCount(),100d));
			} catch(Exception e) {
				throw(new RuntimeException("Error instantiating SequenceEngine",e));
			}	
		}
		
		setArrangementEntries(arrangementEntries);
	}
	
	private double[] parseRatios(String ratioString,int count,double defaultRatio) {
		double[] array = new double[count];

		if(ratioString == null || ratioString.equals("")) {

			for(int i=0;i<count;i++) {
				array[i] = defaultRatio;
			}
			
			return array;
		} else {
		
			String[] ratios = ratioString.split(",");

			if(ratios.length != count) {
				throw(new RuntimeException("Expected "+count+" ratio(s)+, got "+ratios.length));
			}

			for(int i=0;i<ratios.length;i++) {
				array[i] = Double.parseDouble(ratios[i]);
			}
		}
		
		return array;
	}
	
	private class ArrangementEntry {
		private int channel;
		private SequenceEngine sequenceEngine;
		private double[] minRatios;
		private double[] maxRatios;
		
		private ArrangementEntry(int channel,SequenceEngine sequenceEngine,double[] minRatios,double[] maxRatios) {
			this.channel = channel;
			this.sequenceEngine = sequenceEngine;
			this.minRatios = minRatios;
			this.maxRatios = maxRatios;
		}
	}
}
