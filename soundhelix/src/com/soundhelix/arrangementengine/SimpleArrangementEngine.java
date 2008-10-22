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
import com.soundhelix.misc.Track;
import com.soundhelix.sequenceengine.SequenceEngine;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a simple ArrangementEngine. From the given set of SequenceEngines
 * the number of needed ActivityVectors is determined and a subset of these
 * vectors is selected to be active for each chord section. The song starts with
 * a "fade-in" of the number of active ActivityVectors and ends with a "fade-out"
 * of the number of active ActivityVectors.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

// TODO: extend the activity group scheme from the DrumSequenceEngine to this class
//       so that arbitrary combinations of sequences across tracks can be put into an
//       activity group

public class SimpleArrangementEngine extends ArrangementEngine {
	private final Random random = new Random();

	private ArrangementEntry[] arrangementEntries;
	
	// maximum number of tries before failing
	private static final int MAX_TRIES = 2500;
	
	public SimpleArrangementEngine() {
		super();
	}
	
	public Arrangement render() {
		int ticks = structure.getTicks();
		int tracks = arrangementEntries.length;

		// count the total number of ActivityVectors we need
		// most SequenceEngines use 1 ActivityVector, but some use
		// more

		int vectors = 0;

		for(int i=0;i<tracks;i++) {
			SequenceEngine sequenceEngine = arrangementEntries[i].sequenceEngine;
			// SequenceEngines have been instantiated and configured, but the
			// Structure has not been set yet
			sequenceEngine.setStructure(structure);
			vectors += sequenceEngine.getActivityVectorCount();
		}

		System.out.println("Creating "+vectors+" ActivityVectors for "+tracks+" tracks");

		ActivityVector[] activityVectors;

		int tries = 0;

		again: while(true) {
			// create ActivityVectors at random
			// then check if the constraints (minimum and maximum
			// activity ratio) are met for each ActivityVector

			activityVectors = createActivityVectors(vectors);

			int v = 0;

			for(int i=0;i<tracks;i++) {
				SequenceEngine sequenceEngine = arrangementEntries[i].sequenceEngine;

				// get min/max ratio arrays

				double[] minRatios = arrangementEntries[i].minRatios;
				double[] maxRatios = arrangementEntries[i].maxRatios;

				int num = sequenceEngine.getActivityVectorCount();

				for(int k=0;k<num;k++) {
					// compute current activity ratio
					double ratio = 100.0d*(double)activityVectors[v].getActiveTicks()/(double)ticks;

					// check if ratio is outside the required bounds

					if(ratio < minRatios[k] || ratio > maxRatios[k]) {
						tries++;

						if(tries >= MAX_TRIES) {
							throw(new RuntimeException("Couldn't satisfy activity constraints within "+tries+" tries"));
						} else {
							// one constraint wasn't satisfied, retry
							continue again;
						}
					}

					v++;
				}
			}

			break;
		}

		// use each SequenceEngine to render a track
		// each SequenceEngine is given the number of ActivityVectors it
		// requires

		Arrangement arrangement = new Arrangement(structure);

		int vectorNum = 0;

		for(int i=0;i<tracks;i++) {
			SequenceEngine sequenceEngine = arrangementEntries[i].sequenceEngine;
			int num = sequenceEngine.getActivityVectorCount();

			ActivityVector[] list = new ActivityVector[num];

			for(int k=0;k<num;k++) {
				list[k] = activityVectors[vectorNum++];
			}

			Track track = sequenceEngine.render(list);
			track.transpose(arrangementEntries[i].transposition);
			arrangement.add(track,arrangementEntries[i].instrument);
		}

		return arrangement;
	}
	
	/**
	 * Sets one random bit in the BitSet, if this is possible (i.e.,
	 * if not all bits are set already). From the set of false bits,
	 * one is chosen at random and that bit is set to true. The method
	 * avoids setting the bit given by avoidBit, unless there is only
	 * 1 bit left that can be set to true.
	 * 
	 * @param bitSet the BitSet to modify
	 * @param size the size of the BitSet
	 * @param avoidBit the bit number to avoid (or -1 to skip this step)
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
	 * Clears one random bit in the BitSet, if this is possible
	 * (i.e., if at least one bit is set to true). From the set
	 * of true bits, one is chosen at random and that bit is cleared.
	 * The method avoids clearing the bit given by avoidBit, unless
	 * there is only 1 bit left that can be set to false.
	 * 
	 * @param bitSet the BitSet to modify
	 * @param avoidBit the bit number to avoid (or -1 to skip this set)
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
	
	/**
	 * Creates num ActivityVectors. A BitSet is used to
	 * to represent which of the ActivityVectors should be
	 * active. The BitSet is changed on each new chord
	 * section by removing or adding bits randomly.
	 * 
	 * @param num the number of ActivityVectors to create
	 *
	 * @return the array of ActivityVectors
	 */
	
	private ActivityVector[] createActivityVectors(int num) {
		HarmonyEngine he = structure.getHarmonyEngine();
		int sections = he.getChordSectionCount();
		
		// create num empty ActivityVectors
		
		ActivityVector[] activityVectors = new ActivityVector[num];
		
		for(int i=0;i<num;i++) {
			activityVectors[i] = new ActivityVector();
		}
		
		// start with an empty BitSet
		// the BitSet contains the number of vectors that are
		// currently active
		BitSet bitset = new BitSet();
		
		int tick = 0;

    	int lastAddedBit = -1;
    	int lastRemovedBit = -1;
    	
    	// the maximum number of ActivityVectors that may be active
    	// at each point in time
    	int maxActivityVectors = getActivityVectorMaximum(num,0.65,0.2);
    	int lastWantedActivityVectors = -1;
    	
        for(int section=0;section<sections;section++) {
        	int len = he.getChordSectionTicks(tick);
        	
        	// get the number of ActivityVectors we want active for this chord section
        	int wantedActivityVectors = getActivityVectorCount(section,sections,maxActivityVectors,lastWantedActivityVectors);
        	
        	// get the number of tracks that are currently active
        	int card = bitset.cardinality();

        	// add and/or remove bits from the BitSet until tracks
        	// bits are present
        	
        	// we try not to remove a bit that was added in the previous
        	// section and not to add a bit that was removed in the previous
        	// section
        	
        	if(card < wantedActivityVectors) {
        		do {lastAddedBit = setRandomBit(bitset,num,lastRemovedBit);} while(bitset.cardinality() < wantedActivityVectors);
        	} else if(card > wantedActivityVectors) {
        		do {lastRemovedBit = clearRandomBit(bitset,lastAddedBit);} while(bitset.cardinality() > wantedActivityVectors);
        	} else if(card > 0 && Math.random() > 0.2) {
        		lastRemovedBit = clearRandomBit(bitset,lastAddedBit);
        		lastAddedBit = setRandomBit(bitset,num,lastRemovedBit);
        	}
        	
        	//System.out.println("Section: "+section+"  Tracks: "+wantedActivityVectors+"  BitSet: "+bitset);

        	// check the BitSet and add activity or inactivity intervals
        	// for the current section
        	
        	for(int i=0;i<num;i++) {
        		if(bitset.get(i)) {
        			activityVectors[i].addActivity(len);
        		} else {
        			activityVectors[i].addInactivity(len);
        		}
        	}
       	
            tick += len;
            
            lastWantedActivityVectors = wantedActivityVectors;
        }
				
		return activityVectors;	
	}
	
	/**
	 * Returns the number of ActivityVectors that should be active during the
	 * given section. There is always a "fade-in" of ActivityVectors (1, 2, ...)
	 * for the first number of sections and a "fade-out" of ActivityVectors (..., 2, 1)
	 * for the last number of sections. In between, the number of ActivityVectors is
	 * chosen randomly.
	 * 
	 * @param section the section number (between 0 and sections-1)
	 * @param sections the total number of sections
	 * @param maxActivityVectors the maximum number of tracks to use

	 * @return the number of tracks
	 */
	
	private int getActivityVectorCount(int section,int sections,int maxActivityVectors,int lastCount) {
		// important: all of this must work properly when only few sections
        // and few ActivityVectors (or even 1) are available
		
		int increaseTill = Math.min(maxActivityVectors,Math.min(sections/2,4))-1;
		int decreaseFrom = sections-Math.min(maxActivityVectors,Math.min(sections/2,3));
		
		if(section <= increaseTill) {
			// in fade-in phase
			return section+1;
		} else if(section >= decreaseFrom) {
			// in fade-out phase
			return sections-section;
		} else {
			// in between
			int min = Math.min(maxActivityVectors,2);
			
			int num;
			
			do {
				num = min+random.nextInt(maxActivityVectors-min+1);
			} while(Math.abs(num-lastCount) > 3);
			
			return num;
		}
	}
	
	public void setArrangementEntries(ArrangementEntry[] arrangementEntries) {
		this.arrangementEntries = arrangementEntries;
	}
	
	public void configure(Node node,XPath xpath) throws XPathException {
		NodeList nodeList = (NodeList)xpath.evaluate("track",node,XPathConstants.NODESET);

		int tracks = nodeList.getLength();

		if(tracks == 0) {
			throw(new RuntimeException("Need at least 1 track"));
		}
		
		ArrangementEntry[] arrangementEntries = new ArrangementEntry[tracks];
		
		for(int i=0;i<tracks;i++) {
			int instrument = XMLUtils.parseInteger("instrument",nodeList.item(i),xpath);

			int transposition = 0;
			
			try {
			    transposition = XMLUtils.parseInteger("transposition",nodeList.item(i),xpath);
			} catch(Exception e) {}
			
			String minRatios = XMLUtils.parseString("minRatios",nodeList.item(i),xpath);
			String maxRatios = XMLUtils.parseString("maxRatios",nodeList.item(i),xpath);
			
			Node sequenceEngineNode = (Node)xpath.evaluate("sequenceEngine",nodeList.item(i),XPathConstants.NODE);

			try {
			    SequenceEngine sequenceEngine = XMLUtils.getInstance(SequenceEngine.class,sequenceEngineNode,xpath);
			    arrangementEntries[i] = new ArrangementEntry(instrument,sequenceEngine,parseRatios(minRatios,sequenceEngine.getActivityVectorCount(),0d),parseRatios(maxRatios,sequenceEngine.getActivityVectorCount(),100d),transposition);
			} catch(Exception e) {
				throw(new RuntimeException("Error instantiating SequenceEngine",e));
			}	
		}
		
		setArrangementEntries(arrangementEntries);
	}
	
	/**
	 * Parses the given ratio string and returns an array with its contents.
	 * If the ratio string is empty, an array of default ratios is returned.
	 * 
	 * @param ratioString the ratio string (may also be empty or null)
	 * @param count the expected number of ratios
	 * @param defaultRatio the ratio value to use when the string is undefined
	 *
	 * @return an array of doubles containing the ratios
	 */
	
	private double[] parseRatios(String ratioString,int count,double defaultRatio) {
		double[] array = new double[count];

		if(ratioString == null || ratioString.equals("")) {
			// use the default ratio for all ActivityVectors
			
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
	
	/**
	 * Returns the maximum number of ActivityVectors to use at the same time,
	 * given the total number of ActivityVectors available. The method uses an
	 * exponential drop-off so that the returned value is 1 for activityVectors
	 * = 1 and the value converges down to activityVectors*factor as activityVectors goes to infinity.
	 * The lambda value specifies the speed of the exponential drop-off. The goal
	 * is to use almost all ActivityVectors when the number of ActivityVectors is small and
	 * use (in relation) fewer ActivityVectors when the number of ActivityVectors becomes larger.
	 * 
	 * @param activityVectors the number of ActivtiyVectors (must be positive)
	 * @param factor the factor (between 0 and 1)
	 * @param lambda the drop-off factor (must be positive)
	 * 
	 * @return the maximum number of ActivityVectors to use
	 */
		
	private int getActivityVectorMaximum(int activityVectors,double factor,double lambda) {
		return (int)(0.5d+(double)activityVectors*(factor+(1d-factor)*Math.exp(-lambda*(double)(activityVectors-1))));
	}
	
	private class ArrangementEntry {
		private int instrument;
		private SequenceEngine sequenceEngine;
		private double[] minRatios;
		private double[] maxRatios;
		private int transposition;
		
		private ArrangementEntry(int instrument,SequenceEngine sequenceEngine,double[] minRatios,double[] maxRatios,int transposition) {
			this.instrument = instrument;
			this.sequenceEngine = sequenceEngine;
			this.minRatios = minRatios;
			this.maxRatios = maxRatios;
			this.transposition = transposition;
		}
	}
}
