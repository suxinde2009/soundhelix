package com.soundhelix.arrangementengine;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import com.soundhelix.util.HarmonyEngineUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a simple ArrangementEngine. The song starts with a configurable
 * "fade-in" of the number of active ActivityVectors and ends with a
 * configurable "fade-out" of the number of active ActivityVectors.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

// TODO: make ActivityVector constraint-compliance more efficient by ensuring the constraints
// (or aborting as early as possible) during creation instead of checking afterwards

public class SimpleArrangementEngine extends AbstractArrangementEngine {
	private Random random;

	private int[] startActivityCounts = {};
	private int[] stopActivityCounts = {};
	private int maxActivityChangeCount = 3;
	private int minActivityCount = 2;
	private int maxActivityCount = 0;   // determined dynamically if <= 0
	
	private String startActivityCountsString;
	private String stopActivityCountsString;

	private ArrangementEntry[] arrangementEntries;
	private HashMap<String,ActivityVectorConfiguration> activityVectorConfigurationHashMap;
	
	// maximum number of iterations before failing
	private int maxIterations;
	
	public SimpleArrangementEngine() {
		super();
	}
	
	public Arrangement render() {
		HashMap<String, ActivityVectorConfiguration> neededActivityVectors = getNeededActivityVectors();
		
		int tracks = arrangementEntries.length;

		createConstrainedActivityVectors(structure.getTicks(),tracks,neededActivityVectors);
		dumpActivityVectors(neededActivityVectors);
		shiftIntervalBoundaries(neededActivityVectors);

		Arrangement arrangement = createArrangement(neededActivityVectors,tracks);
		return arrangement;
	}

	private HashMap<String, ActivityVectorConfiguration> getNeededActivityVectors() {
		int tracks = arrangementEntries.length;
		
		HashMap<String,ActivityVectorConfiguration> neededActivityVector = new LinkedHashMap<String,ActivityVectorConfiguration>();

		for(int i=0;i<tracks;i++) {
			SequenceEngine sequenceEngine = arrangementEntries[i].sequenceEngine;
			// SequenceEngines have been instantiated and configured, but the
			// Structure has not been set yet
			sequenceEngine.setStructure(structure);
			
			String[] names = arrangementEntries[i].activityVectorNames;
			
			for(int k=0;k<names.length;k++) {
				ActivityVectorConfiguration avc = activityVectorConfigurationHashMap.get(names[k]);
				
				if(avc == null) {
					throw(new RuntimeException("Unknown ActivityVector \""+names[k]+"\""));
				}
				
				neededActivityVector.put(names[k],avc);
			}
		}
		return neededActivityVector;
	}

	private Arrangement createArrangement(HashMap<String, ActivityVectorConfiguration> neededActivityVector,int tracks) {
		// use each SequenceEngine to render a track
		// each SequenceEngine is given the number of ActivityVectors it
		// requires

		Arrangement arrangement = new Arrangement(structure);

		for(int i=0;i<tracks;i++) {
			SequenceEngine sequenceEngine = arrangementEntries[i].sequenceEngine;
			int num = sequenceEngine.getActivityVectorCount();

			ActivityVector[] list = new ActivityVector[num];
			String[] names = arrangementEntries[i].activityVectorNames;
			
			if(names.length != num) {
				throw(new RuntimeException("Need "+num+" ActivityVector"+(num == 1 ? "" : "s")+" for instrument "+arrangementEntries[i].instrument+", found "+names.length));
			}
			
			for(int k=0;k<num;k++) {
				list[k] = neededActivityVector.get(names[k]).activityVector;
			}

			Track track = sequenceEngine.render(list);
			track.transpose(arrangementEntries[i].transposition);
			arrangement.add(track,arrangementEntries[i].instrument);
		}
		
		return arrangement;
	}

	private void shiftIntervalBoundaries(HashMap<String, ActivityVectorConfiguration> neededActivityVector) {
		Iterator<ActivityVectorConfiguration> it = neededActivityVector.values().iterator();
		
		while(it.hasNext()) {
			ActivityVectorConfiguration avc = it.next();
			avc.activityVector.shiftIntervalBoundaries(avc.startShift,avc.stopShift);
		}
	}

	private void createConstrainedActivityVectors(int ticks,int tracks,HashMap<String, ActivityVectorConfiguration> neededActivityVector) {
		List<Integer> chordSectionStartTicks = HarmonyEngineUtils.getChordSectionStartTicks(structure);
		int chordSections = chordSectionStartTicks.size();
		
		int vectors = neededActivityVector.size();

		logger.debug("Creating "+vectors+" ActivityVectors for "+tracks+" tracks");

		startActivityCounts = parseActivityCounts(startActivityCountsString,vectors);
		stopActivityCounts = parseActivityCounts(stopActivityCountsString,vectors);
		
		ActivityVector[] activityVectors;

		int tries = 0;

		Map<String,Integer> constraintFailure = null;
		
		boolean isDebug = logger.isDebugEnabled();
		
		if(isDebug) {
			constraintFailure = new HashMap<String,Integer>();
		}
		
		again: while(true) {
			// create ActivityVectors at random
			// then check if the constraints (minimum and maximum
			// activity ratio) are met for each ActivityVector

			activityVectors = createActivityVectors(vectors);

			Iterator<ActivityVectorConfiguration> it = neededActivityVector.values().iterator();
			
			for(int i=0;i<vectors;i++) {
				ActivityVectorConfiguration avc = it.next();

				ActivityVector av = activityVectors[i];
				
				double active = 100.0d*av.getActiveTicks()/ticks;

				// check if one of the constraints is violated

				if(active < avc.minActive && (!avc.allowInactive || active > 0) || active > avc.maxActive ||
				   avc.startAfterSection+1 >= chordSections || avc.stopBeforeSection+1 >= chordSections ||
				   avc.startAfterSection >= 0 && av.getFirstActiveTick() < chordSectionStartTicks.get(avc.startAfterSection+1) ||
 	               avc.stopBeforeSection >= 0 && av.getLastActiveTick()+1 > chordSectionStartTicks.get(chordSections-1-avc.stopBeforeSection)) {
				    
					if(isDebug) {
						String reason;
						
						if(active < avc.minActive && (!avc.allowInactive || active > 0)) {
							reason = "minActive";
						} else if(active > avc.maxActive) {
							reason = "maxActive";
						} else if(avc.startAfterSection+1 >= chordSections) {
							reason = "startAfterSection";
						} else if(avc.stopBeforeSection+1 >= chordSections) {
							reason = "stopBeforeSection";
						} else if(avc.startAfterSection >= 0 && av.getFirstActiveTick() < chordSectionStartTicks.get(avc.startAfterSection+1)) {
							reason = "startAfterSection";
						} else if(avc.stopBeforeSection >= 0 && av.getLastActiveTick()+1 > chordSectionStartTicks.get(chordSections-1-avc.stopBeforeSection)) {
							reason = "stopAfterSection";
						} else {
							reason = "unknown";
						}
						
						String key = avc.name+"/"+reason;
						Integer current = constraintFailure.get(key);
						
						constraintFailure.put(key,current != null ? current+1 : 1);
					}
										
					tries++;

				    if(tries >= maxIterations) {
				    	if(logger.isDebugEnabled()) {
				    		Iterator<String> it2 = constraintFailure.keySet().iterator();

				    		while(it2.hasNext()) {
				    			String k = it2.next();
				    			logger.debug("Constraint failures for "+k+": "+constraintFailure.get(k));
				    		}
				    	}
				    					    	
				        throw(new RuntimeException("Couldn't satisfy activity constraints within "+tries+" iterations"));
				    } else {
				        // we haven't reached the iteration limit yet, retry
				        continue again;
				    }				        
				}

				avc.activityVector = av;
			}

			break;
		}
		
		logger.debug("Needed "+(tries+1)+" iteration"+(tries > 0 ? "s" : "")+" to satisfy constraints");
	}

	private void dumpActivityVectors(HashMap<String,ActivityVectorConfiguration> neededActivityVector) {
		if(!logger.isDebugEnabled()) {
			return;
		}
		
		StringBuilder sb = new StringBuilder("Song structure\n");
		
		int ticks = structure.getTicks();
		
		int maxLen = 0;
		Iterator<ActivityVectorConfiguration> it = neededActivityVector.values().iterator();
		
		while(it.hasNext()) {
			maxLen = Math.max(maxLen,it.next().name.length());
		}

		it = neededActivityVector.values().iterator();

		while(it.hasNext()) {
			ActivityVectorConfiguration avc = it.next();
			sb.append(String.format("%"+maxLen+"s: ",avc.name));

			ActivityVector av = avc.activityVector;

			for(int tick=0;tick<ticks;tick += structure.getHarmonyEngine().getChordSectionTicks(tick)) {
				if(av.isActive(tick)) {
					sb.append('*');
				} else {
					sb.append('-');
				}
			}
			
			int activeTicks = av.getActiveTicks();
			sb.append(activeTicks > 0 ? String.format(" %5.1f%%\n",100.0d*activeTicks/ticks) : "\n");
		}

		sb.append(String.format("%"+maxLen+"s  ",""));
		for(int tick=0;tick<ticks;tick += structure.getHarmonyEngine().getChordSectionTicks(tick)) {
			int c=0;
			
			it = neededActivityVector.values().iterator();
			
			while(it.hasNext()) {
				if(it.next().activityVector.isActive(tick)) {
					c++;
				}
			}
			
		    sb.append(Integer.toString(c,36));
		}

		logger.debug(sb.toString());
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
	 * Creates num ActivityVectors, where each activity interval can only
	 * start on a chord section boundary and stop one tick before the next
	 * boundary or song end. A BitSet is used to to represent which of the
	 * ActivityVectors should be active. The BitSet is changed on each new
	 * chord section by removing or adding bits randomly.
	 * 
	 * @param num the number of ActivityVectors to create
	 *
	 * @return the array of ActivityVectors
	 */
	
	private ActivityVector[] createActivityVectors(int num) {
		HarmonyEngine he = structure.getHarmonyEngine();
		int sections = HarmonyEngineUtils.getChordSectionCount(structure);
		
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

    	int maxActivityVectors = Math.min(maxActivityCount,num);
    	
    	if(maxActivityVectors <= 0) {
    		maxActivityVectors = getActivityVectorMaximum(num,0.40,0.2);
    	}
    	
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
        	} else if(card > 0 && random.nextFloat() < 0.5f) {
        		lastRemovedBit = clearRandomBit(bitset,lastAddedBit);
        		lastAddedBit = setRandomBit(bitset,num,lastRemovedBit);
        	}
        	
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
	 * given section. There is always a "fade-in" of ActivityVectors (as specified by startActivityCounts array)
	 * for the first number of sections and a "fade-out" of ActivityVectors (as specified by stopActivityCounts array)
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
		
		int increaseTill = Math.min(maxActivityVectors,Math.min(sections/2,startActivityCounts.length))-1;
		int decreaseFrom = sections-Math.min(maxActivityVectors,Math.min(sections/2,stopActivityCounts.length+1));
		
		if(section <= increaseTill) {
			// in fade-in phase
			return startActivityCounts[section];
		} else if(section == decreaseFrom) {
			int firstStop = stopActivityCounts[section-decreaseFrom];
			int count = (lastCount+firstStop)/2;
			
			while((count == lastCount || count == firstStop) && count < maxActivityVectors) {
				count++;
			}

			return count;
		} else if(section >= decreaseFrom+1) {
			// in fade-out phase
			return stopActivityCounts[section-decreaseFrom-1];
		} else {
			// in between
			int min = Math.min(maxActivityVectors,minActivityCount);
			
			int num;
			
			do {
				num = min+random.nextInt(maxActivityVectors-min+1);
			} while(Math.abs(num-lastCount) > maxActivityChangeCount || (num == lastCount && random.nextFloat() >= 0.1f));

			return num;
		}
	}
	
	public void setArrangementEntries(ArrangementEntry[] arrangementEntries) {
		this.arrangementEntries = arrangementEntries;
	}
	
	public void setStartActivityCountsString(String startActivityCountsString) {
		this.startActivityCountsString = startActivityCountsString;
	}

	public void setStopActivityCountsString(String stopActivityCountsString) {
		this.stopActivityCountsString = stopActivityCountsString;
	}
	
	public void configure(Node node,XPath xpath) throws XPathException {
		random = new Random(randomSeed);

		int maxIterations = 100000;
		
		try {
			maxIterations = XMLUtils.parseInteger(random,"maxIterations",node,xpath);
		} catch(Exception e) {}
		
		setMaxIterations(maxIterations);

		String activityString = XMLUtils.parseString(random,"startActivityCounts",node,xpath);

		if(activityString == null) {
			activityString = "1,2,3";
		}

		setStartActivityCountsString(activityString);
		
		activityString = XMLUtils.parseString(random,"stopActivityCounts",node,xpath);

		if(activityString == null) {
			activityString = "3,2,1";
		}

		setStopActivityCountsString(activityString);

		int minActivityCount = XMLUtils.parseInteger(random,"minActivityCount",node,xpath);
		setMinActivityCount(minActivityCount);

		int maxActivityCount = XMLUtils.parseInteger(random,"maxActivityCount",node,xpath);
		setMaxActivityCount(maxActivityCount);

		int maxActivityChangeCount= XMLUtils.parseInteger(random,"maxActivityChangeCount",node,xpath);
		setMaxActivityChangeCount(maxActivityChangeCount);

		NodeList nodeList = (NodeList)xpath.evaluate("activityVector",node,XPathConstants.NODESET);

		int activityVectorCount = nodeList.getLength();

		if(activityVectorCount == 0) {
			throw(new RuntimeException("Need at least 1 ActivityVector"));
		}

		LinkedHashMap<String,ActivityVectorConfiguration> activityVectorConfigurationHashMap = new LinkedHashMap<String,ActivityVectorConfiguration>(activityVectorCount);
		
		for(int i=0;i<activityVectorCount;i++) {
			String name = XMLUtils.parseString(random,"attribute::name",nodeList.item(i),xpath);

			if(activityVectorConfigurationHashMap.containsKey(name)) {
				throw(new RuntimeException("ActivityVector \""+name+"\" already defined"));
			}
			
			double minActive = 0;
			
			try {
				minActive = Double.parseDouble(XMLUtils.parseString(random,"minActive",nodeList.item(i),xpath));
			} catch(Exception e) {}
			
			boolean allowInactive = false;

			try {
				allowInactive = XMLUtils.parseBoolean(random,"minActive/attribute::allowInactive",nodeList.item(i),xpath);
			} catch(Exception e) {}

			double maxActive = 100.0d;
			
			try {
				maxActive = Double.parseDouble(XMLUtils.parseString(random,"maxActive",nodeList.item(i),xpath));
			} catch(Exception e) {}
			
			int startShift = 0;
			try {
				startShift = XMLUtils.parseInteger(random,"startShift",nodeList.item(i),xpath);
			} catch(Exception e) {}
			
			int stopShift = 0;
			try {
				stopShift = XMLUtils.parseInteger(random,"stopShift",nodeList.item(i),xpath);
			} catch(Exception e) {}

			int startAfterSection = -1;
			try {
			    startAfterSection = XMLUtils.parseInteger(random,"startAfterSection",nodeList.item(i),xpath);
			} catch(Exception e) {}

			int stopBeforeSection = -1;
			try {
			    stopBeforeSection = XMLUtils.parseInteger(random,"stopBeforeSection",nodeList.item(i),xpath);
			} catch(Exception e) {}

			activityVectorConfigurationHashMap.put(name,new ActivityVectorConfiguration(name,minActive,allowInactive,maxActive,startShift,stopShift,startAfterSection,stopBeforeSection));		
		}
				
		setActivityVectorConfiguration(activityVectorConfigurationHashMap);

		nodeList = (NodeList)xpath.evaluate("track[@solo=\"true\"]",node,XPathConstants.NODESET);
		int tracks = nodeList.getLength();

		if(tracks == 0) {
			nodeList = (NodeList)xpath.evaluate("track",node,XPathConstants.NODESET);
			tracks = nodeList.getLength();

			if(tracks == 0) {
				throw(new RuntimeException("Need at least 1 track"));
			}
		}
		
		ArrangementEntry[] arrangementEntries = new ArrangementEntry[tracks];
		
		for(int i=0;i<tracks;i++) {
			int instrument = XMLUtils.parseInteger(random,"instrument",nodeList.item(i),xpath);

			int transposition = 0;
			
			try {
			    transposition = XMLUtils.parseInteger(random,"transposition",nodeList.item(i),xpath);
			} catch(Exception e) {}
			
			Node sequenceEngineNode = (Node)xpath.evaluate("sequenceEngine",nodeList.item(i),XPathConstants.NODE);

			NodeList nameNodeList = (NodeList)xpath.evaluate("activityVector",nodeList.item(i),XPathConstants.NODESET);
			
			String[] activityVectorNames = new String[nameNodeList.getLength()];
			
			for(int k=0;k<nameNodeList.getLength();k++) {
				activityVectorNames[k] = nameNodeList.item(k).getTextContent();
			}
			
			try {
			    SequenceEngine sequenceEngine = XMLUtils.getInstance(SequenceEngine.class,sequenceEngineNode,xpath,randomSeed+1+i);
			    arrangementEntries[i] = new ArrangementEntry(instrument,sequenceEngine,transposition,activityVectorNames);
			} catch(Exception e) {
				throw(new RuntimeException("Error instantiating SequenceEngine",e));
			}	
		}
		
		setArrangementEntries(arrangementEntries);
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
		return (int)(0.5d+activityVectors*(factor+(1d-factor)*Math.exp(-lambda*(activityVectors-1))));
	}
	
	public void setActivityVectorConfiguration(
			HashMap<String, ActivityVectorConfiguration> activityVectorConfigurationHashMap) {
		this.activityVectorConfigurationHashMap = activityVectorConfigurationHashMap;
	}

	private int[] parseActivityCounts(String string,int maxCount) {
		String[] c = string.split(",");
		int[] activityCounts = new int[c.length];
		
		for(int i=0;i<c.length;i++) {
			try {
				activityCounts[i] = Integer.parseInt(c[i]);
			} catch(NumberFormatException e) {
				throw(new RuntimeException("Element \""+c[i]+"\" in activity count string \""+string+"\" is not a number"));
			}
		
			if(activityCounts[i] <= 0) {
				throw(new RuntimeException("Element \""+activityCounts[i]+"\" in activity count string \""+string+"\" is not positive"));
			} else if(activityCounts[i] > maxCount) {
				activityCounts[i] = maxCount;
			}	
		}
		
		return activityCounts;
	}
	
	private static class ArrangementEntry {
		private int instrument;
		private SequenceEngine sequenceEngine;
		private int transposition;
		private String[] activityVectorNames;
		
		private ArrangementEntry(int instrument,SequenceEngine sequenceEngine,int transposition,String[] activityVectorNames) {
			this.instrument = instrument;
			this.sequenceEngine = sequenceEngine;
			this.transposition = transposition;
			this.activityVectorNames = activityVectorNames;
		}
	}
	
	private static class ActivityVectorConfiguration {
		private String name;
		private double minActive;
		private boolean allowInactive;
		private double maxActive;
		private int startShift;
		private int stopShift;
		private int startAfterSection;
        private int stopBeforeSection;
		private ActivityVector activityVector;

		private ActivityVectorConfiguration(String name,double minActive,boolean allowInactive,double maxActive,int startShift,int stopShift,int startAfterSection,int stopBeforeSection) {
			this.name = name;
			this.minActive = minActive;
			this.allowInactive = allowInactive;
			this.maxActive = maxActive;
			this.startShift = startShift;
			this.stopShift = stopShift;
			this.startAfterSection = startAfterSection;
			this.stopBeforeSection = stopBeforeSection;
		}	
	}

	public void setMinActivityCount(int minActiveCount) {
		this.minActivityCount = minActiveCount;
	}

	public void setMaxActivityCount(int maxActiveCount) {
		this.maxActivityCount = maxActiveCount;
	}

	public void setMaxActivityChangeCount(int maxActivityChangeCount) {
		this.maxActivityChangeCount = maxActivityChangeCount;
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}
}