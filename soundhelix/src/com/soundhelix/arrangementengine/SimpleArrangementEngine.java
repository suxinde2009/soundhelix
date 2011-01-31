package com.soundhelix.arrangementengine;

import java.util.BitSet;
import java.util.HashMap;
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
 * Implements a simple ArrangementEngine. The song starts with a configurable "fade-in" of the number of active
 * ActivityVectors and ends with a configurable "fade-out" of the number of active ActivityVectors. For each
 * chord section in between, the number of active AVs is varied in the configured range.
 *
 * ActivityVector activity can be constrained individually to enforce certain properties. All AVs are filled at
 * the same time because there is always the above-mentioned overall song constraint that defines the number of
 * active AVs for each chord section. For each chord section a choice is made to activate or deactivate AVs or to
 * leave the number of active AVs unchanged. The list of generated AVs are called an activity matrix.
 *
 * The following constraints are supported for ActivityVectors:
 * 
 * - minActive(n): the AV must be active for at least n% of the song; granularity is at chord section level; it is
 *                 also possible define that the AV may be inactive for the whole song or be active with at least n%
 *                 of the song
 * - maxActive(n): the AV must be active for at most n% of the song; granularity is at chord section level
 * - startAfterSection(n): the AV must not be active before section n+1
 * - stopBeforeSection(n): the AV must not be active after section n+1 counted from the end
 * - minSegmentCount(n): the AV must be active for at least n chord section segments
 * - maxSegmentCount(n): the AV must be active for at most n chord section segments
 *
 * Local (peep-hole) constraints can be checked during AV generation when the activity per chord section is generated.
 * Such a constraint can be a success constraint, which means that a random choice can be redone on-the-fly until the
 * constraint is not violated anymore (if that's possible) or it can be a failure constraint, which means the
 * constraint can be checked locally but can make the whole creation fail. For example, the startAfterSection
 * constraint is a local success constraint, because if an AV is randomly chosen to become active too early, this
 * random selection can be repeated until an AV is chosen that does not violate that constraint (unless such an AV
 * doesn't exist). In contrast, the stopAfterSection constraint cannot be enforced so easily, because an AV can be
 * active in a chord section either because it becomes active (then the constraint can be made a success by choosing
 * another AV to become active instead) or because it is already active and stays active (because the number of
 * active AVs should increase).
 * 
 * Global constraints can only be checked after all AVs have been generated. No correction to enforce the constraints
 * are possible except for completely recreating all AVs. Currently, none of the constraints are really global.
 * 
 * The easiest (but least efficient) way to check constraints is of course to check them after the whole activity
 * matrix has been generated. However, the time to fulfill all constraints tends to grow exponentially with every 
 * constraint added. Therefore, most of the constraints are checked as early as possible.
 * 
 * The only low-cost constraint is currently the startAfterSection constraint. Using it hardly increases the
 * AV creation time at all. The minActive/maxActive constraints on the other hand are expensive for percentages larger
 * than the expected average percentage for minActive and percentages smaller than the expected average percentage for
 * maxActive. For example, if you have 20 AVs and 5 of them should active at least 60% of the time, you will need
 * quite a lot of creation iterations until these constraints are fulfilled.
 *
 * @author Thomas Schürger (thomas@schuerger.com)
 */

// TODO: make ActivityVector constraint-compliance more efficient by ensuring the constraints
// (or aborting as early as possible) during creation instead of checking afterwards

public class SimpleArrangementEngine extends AbstractArrangementEngine {
	/** The random generator. */
	private Random random;

	/** The array of start activity counts. */
	private int[] startActivityCounts = {};
	
	/** The array of stop activity counts. */
	private int[] stopActivityCounts = {};
	
	/** The maximum delta that activity can change in each chord section. */
	private int maxActivityChangeCount = 3;
	
	/** The minimum number of active ActivityVectors. */
	private int minActivityCount = 2;
	
    /** The maximum number of active ActivityVectors (determined dynamically if <= 0). */
	private int maxActivityCount;
	
	private String startActivityCountsString;
	private String stopActivityCountsString;

	private ArrangementEntry[] arrangementEntries;
	private Map<String, ActivityVectorConfiguration> activityVectorConfigurationHashMap;
	
	/** The maximum number of iterations before failing. */
	private int maxIterations;
	
	public SimpleArrangementEngine() {
		super();
	}
	
	public Arrangement render() {
		Map<String, ActivityVectorConfiguration> neededActivityVectors = getNeededActivityVectors();
		
		int tracks = arrangementEntries.length;

		ActivityVectorConfiguration[] vectors = neededActivityVectors.values().toArray(new ActivityVectorConfiguration[neededActivityVectors.size()]);
		
		createConstrainedActivityVectors(structure.getTicks(), tracks, vectors);
		dumpActivityVectors(vectors);
		shiftIntervalBoundaries(neededActivityVectors);

		Arrangement arrangement = createArrangement(neededActivityVectors);
		return arrangement;
	}

	private Map<String, ActivityVectorConfiguration> getNeededActivityVectors() {
		Map<String, ActivityVectorConfiguration> neededActivityVector =
					new LinkedHashMap<String, ActivityVectorConfiguration>();

		for (ArrangementEntry entry : arrangementEntries) {
			SequenceEngine sequenceEngine = entry.sequenceEngine;
			// SequenceEngines have been instantiated and configured, but the
			// Structure has not been set yet
			sequenceEngine.setStructure(structure);
			
			String[] names = entry.activityVectorNames;
			
			for (String name : names) {
				ActivityVectorConfiguration avc = activityVectorConfigurationHashMap.get(name);
				
				if (avc == null) {
					throw new RuntimeException("Unknown ActivityVector \"" + name + "\"");
				}
				
				neededActivityVector.put(name, avc);
			}
		}
		return neededActivityVector;
	}

	private Arrangement createArrangement(Map<String, ActivityVectorConfiguration> neededActivityVector) {
		// use each SequenceEngine to render a track
		// each SequenceEngine is given the number of ActivityVectors it
		// requires

		Arrangement arrangement = new Arrangement(structure);
		
		for (ArrangementEntry entry : arrangementEntries) {
			SequenceEngine sequenceEngine = entry.sequenceEngine;
			int num = sequenceEngine.getActivityVectorCount();

			ActivityVector[] list = new ActivityVector[num];
			String[] names = entry.activityVectorNames;
			
			if (names.length != num) {
				throw new RuntimeException("Need " + num + " ActivityVector" + (num == 1 ? "" : "s")
				        + " for instrument " + entry.instrument + ", found " + names.length);
			}
			
			for (int k = 0; k < num; k++) {
				list[k] = neededActivityVector.get(names[k]).activityVector;
			}

			Track track = sequenceEngine.render(list);
			track.transpose(entry.transposition);
			arrangement.add(track, entry.instrument);
		}
		
		return arrangement;
	}

	private void shiftIntervalBoundaries(Map<String, ActivityVectorConfiguration> neededActivityVector) {
		for (ActivityVectorConfiguration avc : neededActivityVector.values()) {
			avc.activityVector.shiftIntervalBoundaries(avc.startShift, avc.stopShift);
		}
	}

	private void createConstrainedActivityVectors(int ticks, int tracks, ActivityVectorConfiguration[] activityVectorConfigurations) {
		List<Integer> chordSectionStartTicks = HarmonyEngineUtils.getChordSectionStartTicks(structure);
		int chordSections = chordSectionStartTicks.size();
		
		int vectors = activityVectorConfigurations.length;

		logger.debug("Creating " + vectors + " ActivityVectors for " + tracks + " tracks");

		startActivityCounts = parseActivityCounts(startActivityCountsString, vectors);
		stopActivityCounts = parseActivityCounts(stopActivityCountsString, vectors);
		
		int tries = 0;

		Map<String, Integer> constraintFailure = null;
		
		boolean isDebug = logger.isDebugEnabled();
		
		if (isDebug) {
			constraintFailure = new HashMap<String, Integer>();
		}
		
		again: while (true) {
			// create ActivityVectors at random
			// then check if the constraints are met for each ActivityVector

			// note that the constraint startAfterSection is already taken care of in the following method
			// (unfortunately, it's not simple to do the same for stopBeforeSection)

			ActivityVector[] activityVectors;
			
			try {
				activityVectors = createActivityVectors(activityVectorConfigurations);
			} catch (ConstraintException e) {
				if (isDebug) {
					String name = e.getActivityVectorConfiguration() != null
					        ? e.getActivityVectorConfiguration().name : "unknown";
					String key = name + "/" + e.getReason();
					Integer current = constraintFailure.get(key);

					constraintFailure.put(key, current != null ? current + 1 : 1);
				}
				
				tries++;

			    if (tries >= maxIterations) {
			    	if (logger.isDebugEnabled()) {
			    		for (String key : constraintFailure.keySet()) {
			    			logger.debug("Constraint failures for " + key + ": " + constraintFailure.get(key));
			    		}
			    	}
			    					    	
                    throw new RuntimeException("Couldn't satisfy activity constraints within " + tries + " iterations");
			    } else {
			        // we haven't reached the iteration limit yet, retry
			        continue again;
			    }				        
			}
			
			for (int i = 0; i < vectors; i++) {
				ActivityVector av = activityVectors[i];
				ActivityVectorConfiguration avc = activityVectorConfigurations[i];
				
				// check if one of the constraints is violated

				double active = 100.0d * av.getActiveTicks() / ticks;
				int firstActiveTick = av.getFirstActiveTick();
				int segmentCount = av.getSegmentCount();
				
				if (active < avc.minActive && (!avc.allowInactive || active > 0) || active > avc.maxActive
				        || avc.startAfterSection + 1 >= chordSections || avc.stopBeforeSection + 1 >= chordSections
				        || avc.stopBeforeSection >= 0 && av.getLastActiveTick() >= chordSectionStartTicks.get(chordSections - 1 - avc.stopBeforeSection)
				        || avc.startAfterSection >= 0 && firstActiveTick >= 0 && firstActiveTick < chordSectionStartTicks.get(avc.startAfterSection + 1)
				        || (avc.minSegmentCount >= 0 || avc.maxSegmentCount < Integer.MAX_VALUE) && (segmentCount < avc.minSegmentCount || segmentCount > avc.maxSegmentCount)) {
				    
					if (isDebug) {
						String reason;
						
						if (active < avc.minActive && (!avc.allowInactive || active > 0)) {
							reason = "minActive";
						} else if (active > avc.maxActive) {
							reason = "maxActive";
						} else if (avc.startAfterSection + 1 >= chordSections) {
							reason = "startAfterSection";
						} else if (avc.stopBeforeSection + 1 >= chordSections) {
							reason = "stopBeforeSection";
						} else if (avc.stopBeforeSection >= 0 && av.getLastActiveTick() >= chordSectionStartTicks.get(chordSections - 1 - avc.stopBeforeSection)) {
							reason = "stopBeforeSection";
						} else if (avc.startAfterSection >= 0 && firstActiveTick >= 0 && firstActiveTick < chordSectionStartTicks.get(avc.startAfterSection + 1)) {
							// should not happen as this is already checked in createActivityVectors()
							reason = "startAfterSection";
						} else if (segmentCount < avc.minSegmentCount) {
							reason = "minSegmentCount";
						} else if (segmentCount > avc.maxSegmentCount) {
							reason = "maxSegmentCount";
						} else {
							reason = "unknown";
						}
						
						String key = avc.name + "/" + reason;
						Integer current = constraintFailure.get(key);
						
						constraintFailure.put(key, current != null ? current + 1 : 1);
					}
							
					tries++;

				    if (tries >= maxIterations) {
				    	if (logger.isDebugEnabled()) {
				    		for (String k : constraintFailure.keySet()) {
				    			logger.debug("Constraint failures for " + k + ": " + constraintFailure.get(k));
				    		}
				    	}
				    					    	
				        throw new RuntimeException("Couldn't satisfy activity constraints within "
				                + tries + " iterations");
				    } else {
				        // we haven't reached the iteration limit yet, retry
				        continue again;
				    }				        
				}
			}

			break;
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("Needed " + (tries + 1) + " iteration" + (tries > 0 ? "s" : "") + " to satisfy constraints");
		}
	}

	/**
	 * Dumps the activity vectors as an activity matrix.
	 *
	 * @param vectors the array of activity vector configurations
	 */
	
	private void dumpActivityVectors(ActivityVectorConfiguration[] vectors) {
		if (!logger.isDebugEnabled()) {
			return;
		}
		
		StringBuilder sb = new StringBuilder("Song structure\n");
		
		int ticks = structure.getTicks();
		
		int maxLen = 0;
		
		for (ActivityVectorConfiguration avc : vectors) {
			maxLen = Math.max(maxLen, avc.name.length());
		}

		for (ActivityVectorConfiguration avc : vectors) {
			sb.append(String.format("%" + maxLen + "s: ", avc.name));

			ActivityVector av = avc.activityVector;

            for (int tick = 0; tick < ticks; tick += structure.getHarmonyEngine().getChordSectionTicks(tick)) {
				if (av.isActive(tick)) {
					sb.append('*');
				} else {
					sb.append('-');
				}
			}
			
			int activeTicks = av.getActiveTicks();
			sb.append(activeTicks > 0 ? String.format(" %5.1f%%\n", 100.0d * activeTicks / ticks) : "\n");
		}

		sb.append(String.format("%" + maxLen + "s  ", ""));
		for (int tick = 0; tick < ticks; tick += structure.getHarmonyEngine().getChordSectionTicks(tick)) {
			int c = 0;
			
			for (ActivityVectorConfiguration avc : vectors) {
				if (avc.activityVector.isActive(tick)) {
					c++;
				}
			}

			// output number in base-36 format (0-9,a-z)
		    sb.append(Integer.toString(c, 36));
		}

		logger.debug(sb.toString());
	}
	
	/**
	 * Sets one random bit in the BitSet, if this is possible (i.e.,
	 * if not all bits are set already). From the set of false bits,
	 * one is chosen at random and that bit is set to true. The method
	 * avoids setting the bit given by avoidBit, unless there is only
	 * 1 bit left that can be set to true. It also avoids to set a
	 * bit if this would violate the startAfterSection or the stopBeforeSection
	 * constraint.
	 * 
	 * @param bitSet the BitSet to modify
	 * @param size the size of the BitSet
	 * @param avoidBit the bit number to avoid (or -1 to skip this step)
	 * @param section the section
	 * @param sections the sections
	 * @param activityVectorConfigurations the activity vector configurations
	 * @param activeSegments the active segments
	 * 
	 * @return the number of the set bit (or -1 if no clear bit existed)
	 */
	
	private int setRandomBit(BitSet bitSet, int size, int avoidBit, int section, int sections,
	                         ActivityVectorConfiguration[] activityVectorConfigurations, int[] activeSegments) {
		int ones = bitSet.cardinality();
		
		if (ones >= size) {
			return -1;
		}

		int zeroes = size - ones;

		int bit;
		int pos;
		
		int stopPos = sections - 1 - section;
		
		ActivityVectorConfiguration avc;
		
		int count = 100;
		
		do {
			do {		
				// choose random bit number
				bit = random.nextInt(zeroes);
			} while (zeroes > 1 && bit == avoidBit);

			// set the bit'th zero bit

			pos = bitSet.nextClearBit(0);

			while (bit-- > 0) {
				pos = bitSet.nextClearBit(pos + 1);
			}
			
			avc = activityVectorConfigurations[pos];
			
			// retry if we are trying to set a bit which shouldn't be set yet
			// note that this will handle the startAfterSection constraint completely, the stopBeforeSection
			// constraint is only handled partially
		} while (count-- > 0 && (section <= avc.startAfterSection || stopPos <= avc.stopBeforeSection));
		
		if (count < 0) {
			throw new ConstraintException(null, "Couldn't set bit");
		}
		
		bitSet.set(pos);
		
		// this is the start of a new segment
		activeSegments[pos]++;

		if (activeSegments[pos] > activityVectorConfigurations[pos].maxSegmentCount) {
			throw new ConstraintException(avc, "earlyMaxSegmentCount");
		}
		
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
	
	private int clearRandomBit(BitSet bitSet, int avoidBit) {
		int ones = bitSet.cardinality();
		
		if (ones == 0) {
			return -1;
		}

		int bit;
		
		do {
			// choose random bit number
			bit = random.nextInt(ones);
		} while (ones > 1 && bit == avoidBit);
		
		// skip to the bit'th one bit
		
		int pos = bitSet.nextSetBit(0);
		
		while (bit-- > 0) {
			pos = bitSet.nextSetBit(pos + 1);
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
	 * @param activityVectorConfigurations the activity vector configurations
	 *
	 * @return the array of ActivityVectors
	 */
	
	private ActivityVector[] createActivityVectors(ActivityVectorConfiguration[] activityVectorConfigurations) {
		HarmonyEngine he = structure.getHarmonyEngine();
		int sections = HarmonyEngineUtils.getChordSectionCount(structure);
		int vectors = activityVectorConfigurations.length;
		
		// create empty ActivityVectors
		
		ActivityVector[] activityVectors = new ActivityVector[vectors];
		int[] activeSections = new int[vectors];
		int[] minActiveSections = new int[vectors];
		int[] maxActiveSections = new int[vectors];
		int[] activeSegments = new int[vectors];
		
		for (int i = 0; i < vectors; i++) {
			activityVectors[i] = new ActivityVector();
			activityVectorConfigurations[i].activityVector = activityVectors[i];
			minActiveSections[i] = (int) (activityVectorConfigurations[i].minActive / 100.0d * sections);
			maxActiveSections[i] = (int) (activityVectorConfigurations[i].maxActive / 100.0d * sections);
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

    	int maxActivityVectors = Math.min(maxActivityCount, vectors);
    	
    	if (maxActivityVectors <= 0) {
    		maxActivityVectors = getActivityVectorMaximum(vectors, 0.40, 0.2);
    	}
    	
    	int lastWantedActivityVectors = -1;
    	
        for (int section = 0; section < sections; section++) {
        	int len = he.getChordSectionTicks(tick);
        	
        	// get the number of ActivityVectors we want active for this chord section
        	int wantedActivityVectors = getActivityVectorCount(section, sections, maxActivityVectors,
        			                                           lastWantedActivityVectors);
        	
        	// get the number of ActivityVectors that are currently active
        	int active = bitset.cardinality();

        	// add and/or remove bits from the BitSet until tracks
        	// bits are present
        	
        	// we try not to remove a bit that was added in the previous
        	// section and not to add a bit that was removed in the previous
        	// section
        	
        	if (active < wantedActivityVectors) {
        		do {
        		    lastAddedBit = setRandomBit(bitset, vectors, lastRemovedBit, section, sections,
        		            activityVectorConfigurations, activeSegments);
        		} while (bitset.cardinality() < wantedActivityVectors);
        	} else if (active > wantedActivityVectors) {
        		do {
        		    lastRemovedBit = clearRandomBit(bitset, lastAddedBit);
        		} while (bitset.cardinality() > wantedActivityVectors);
        	}
        	
        	// check the BitSet and add activity or inactivity intervals
        	// for the current section
        	
        	for (int i = 0; i < vectors; i++) {
        		if (bitset.get(i)) {
        			if (activeSections[i] >= maxActiveSections[i]) {
        				throw new ConstraintException(activityVectorConfigurations[i], "earlyMaxActive");
        			} else if (sections - 1 - section <= activityVectorConfigurations[i].stopBeforeSection) {
    			    	throw new ConstraintException(activityVectorConfigurations[i], "earlyStopBeforeSection");
        			}
        			
        			activeSections[i]++;        			 
           			activityVectors[i].addActivity(len);
        		} else {
            		// check if the still missing active sections are more than what is left
            		// we only need to check this in the inactive case
        			
    			    if (!activityVectorConfigurations[i].allowInactive && minActiveSections[i] - activeSections[i] > sections - 1 - section) {
    			    	throw new ConstraintException(activityVectorConfigurations[i], "earlyMinActive");
    			    }
    			    
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
	 * @param lastCount the last count

	 * @return the number of tracks
	 */
	
    private int getActivityVectorCount(int section, int sections, int maxActivityVectors, int lastCount) {
		// important: all of this must work properly when only few sections
        // and few ActivityVectors (or even 1) are available
		
		int increaseTill = Math.min(maxActivityVectors, Math.min(sections / 2, startActivityCounts.length)) - 1;
		int decreaseFrom = sections - Math.min(maxActivityVectors, Math.min(sections / 2,
		                                                                    stopActivityCounts.length + 1));
		
		if (section <= increaseTill) {
			// in fade-in phase
			return startActivityCounts[section];
		} else if (section == decreaseFrom) {
			int firstStop = stopActivityCounts[section - decreaseFrom];
			int count = (lastCount + firstStop) / 2;
			
			while ((count == lastCount || count == firstStop) && count < maxActivityVectors) {
				count++;
			}

			return count;
		} else if (section >= decreaseFrom + 1) {
			// in fade-out phase
			return stopActivityCounts[section - decreaseFrom - 1];
		} else {
			// in between
			int min = Math.min(maxActivityVectors, minActivityCount);
			
			int num;
			
			do {
				num = min + random.nextInt(maxActivityVectors - min + 1);
			} while (Math.abs(num - lastCount) > maxActivityChangeCount
					 || (num == lastCount && random.nextFloat() >= 0.1f));

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
	
	public void configure(Node node, XPath xpath) throws XPathException {
		random = new Random(randomSeed);

		int maxIterations = 100000;
		
		try {
			maxIterations = XMLUtils.parseInteger(random, "maxIterations", node, xpath);
		} catch (Exception e) {
		}
		
		setMaxIterations(maxIterations);

		String activityString = XMLUtils.parseString(random, "startActivityCounts", node, xpath);

		if (activityString == null) {
			activityString = "1,2,3";
		}

		setStartActivityCountsString(activityString);
		
		activityString = XMLUtils.parseString(random, "stopActivityCounts", node, xpath);

		if (activityString == null) {
			activityString = "3,2,1";
		}

		setStopActivityCountsString(activityString);

		int minActivityCount = XMLUtils.parseInteger(random, "minActivityCount", node, xpath);
		setMinActivityCount(minActivityCount);

		int maxActivityCount = XMLUtils.parseInteger(random, "maxActivityCount", node, xpath);
		setMaxActivityCount(maxActivityCount);

		int maxActivityChangeCount = XMLUtils.parseInteger(random, "maxActivityChangeCount", node, xpath);
		setMaxActivityChangeCount(maxActivityChangeCount);

		NodeList nodeList = (NodeList) xpath.evaluate("activityVector", node, XPathConstants.NODESET);

		int activityVectorCount = nodeList.getLength();

		if (activityVectorCount == 0) {
			throw new RuntimeException("Need at least 1 ActivityVector");
		}

		Map<String, ActivityVectorConfiguration> activityVectorConfigurationHashMap =
				new LinkedHashMap<String, ActivityVectorConfiguration>(activityVectorCount);
		
		for (int i = 0; i < activityVectorCount; i++) {
			String name = XMLUtils.parseString(random, "attribute::name", nodeList.item(i), xpath);

			if (activityVectorConfigurationHashMap.containsKey(name)) {
				throw new RuntimeException("ActivityVector \"" + name + "\" already defined");
			}
			
			double minActive = 0;
			
            try {
                minActive = Double.parseDouble(XMLUtils.parseString(random, "minActive", nodeList.item(i), xpath));
			} catch (Exception e) {}
			
			boolean allowInactive = false;

			try {
                allowInactive = XMLUtils.parseBoolean(random, "minActive/attribute::allowInactive", nodeList.item(i),
				        xpath);
			} catch (Exception e) {}

			double maxActive = 100.0d;
			
			try {
                maxActive = Double.parseDouble(XMLUtils.parseString(random, "maxActive", nodeList.item(i), xpath));
			} catch (Exception e) {}
			
			int startShift = 0;
			try {
				startShift = XMLUtils.parseInteger(random, "startShift", nodeList.item(i), xpath);
			} catch (Exception e) {}
			
			int stopShift = 0;
			try {
				stopShift = XMLUtils.parseInteger(random, "stopShift", nodeList.item(i), xpath);
			} catch (Exception e) {}

			int startAfterSection = -1;
			try {
                startAfterSection = XMLUtils.parseInteger(random, "startAfterSection", nodeList.item(i), xpath);
			} catch (Exception e) {}

			int stopBeforeSection = -1;
			try {
                stopBeforeSection = XMLUtils.parseInteger(random, "stopBeforeSection", nodeList.item(i), xpath);
			} catch (Exception e) {}

			int minSegmentCount = 0;
			try {
			    minSegmentCount = XMLUtils.parseInteger(random, "minSegmentCount", nodeList.item(i), xpath);
			} catch (Exception e) {}

			int maxSegmentCount = Integer.MAX_VALUE;
			try {
			    maxSegmentCount = XMLUtils.parseInteger(random, "maxSegmentCount", nodeList.item(i), xpath);
			} catch (Exception e) {}

            activityVectorConfigurationHashMap.put(name, new ActivityVectorConfiguration(name, minActive, allowInactive,
                    maxActive, startShift, stopShift, startAfterSection, stopBeforeSection, minSegmentCount,
                    maxSegmentCount));		
		}
				
		setActivityVectorConfiguration(activityVectorConfigurationHashMap);

		nodeList = (NodeList) xpath.evaluate("track[@solo=\"true\"]", node, XPathConstants.NODESET);
		int tracks = nodeList.getLength();

		if (tracks == 0) {
			nodeList = (NodeList) xpath.evaluate("track", node, XPathConstants.NODESET);
			tracks = nodeList.getLength();

			if (tracks == 0) {
				throw new RuntimeException("Need at least 1 track");
			}
		}
		
		ArrangementEntry[] arrangementEntries = new ArrangementEntry[tracks];
		
		for (int i = 0; i < tracks; i++) {
			String instrument = XMLUtils.parseString(random, "instrument", nodeList.item(i), xpath);

			int transposition = 0;
			
			try {
			    transposition = XMLUtils.parseInteger(random, "transposition", nodeList.item(i), xpath);
			} catch (Exception e) {
			}
			
			Node sequenceEngineNode = (Node) xpath.evaluate("sequenceEngine", nodeList.item(i), XPathConstants.NODE);

			NodeList nameNodeList = (NodeList) xpath.evaluate("activityVector", nodeList.item(i),
			        XPathConstants.NODESET);
			
			String[] activityVectorNames = new String[nameNodeList.getLength()];
			
			for (int k = 0; k < nameNodeList.getLength(); k++) {
				activityVectorNames[k] = nameNodeList.item(k).getTextContent();
			}
			
			try {
			    SequenceEngine sequenceEngine = XMLUtils.getInstance(SequenceEngine.class,
			    		sequenceEngineNode, xpath, randomSeed + 1 + i);
			    arrangementEntries[i] = new ArrangementEntry(instrument, sequenceEngine,
			    		transposition, activityVectorNames);
			} catch (Exception e) {
				throw new RuntimeException("Error instantiating SequenceEngine", e);
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
		
	private int getActivityVectorMaximum(int activityVectors, double factor, double lambda) {
        return (int) (0.5d + activityVectors * (factor + (1d - factor) * Math.exp(-lambda * (activityVectors - 1))));
	}
	
	public void setActivityVectorConfiguration(
			Map<String, ActivityVectorConfiguration> activityVectorConfigurationHashMap) {
		this.activityVectorConfigurationHashMap = activityVectorConfigurationHashMap;
	}

	private int[] parseActivityCounts(String string, int maxCount) {
		String[] c = string.split(",");
		int[] activityCounts = new int[c.length];
		
		for (int i = 0; i < c.length; i++) {
			try {
				activityCounts[i] = Integer.parseInt(c[i]);
			} catch (NumberFormatException e) {
				throw new RuntimeException("Element \"" + c[i] + "\" in activity count string \""
				        + string + "\" is not a number");
			}
		
			if (activityCounts[i] <= 0) {
				throw new RuntimeException("Element \"" + activityCounts[i] + "\" in activity count string \""
				        + string + "\" is not positive");
			} else if (activityCounts[i] > maxCount) {
				activityCounts[i] = maxCount;
			}	
		}
		
		return activityCounts;
	}
	
	private static final class ArrangementEntry {
		private String instrument;
		private SequenceEngine sequenceEngine;
		private int transposition;
		private String[] activityVectorNames;
		
		private ArrangementEntry(String instrument, SequenceEngine sequenceEngine, int transposition, String[] activityVectorNames) {
			this.instrument = instrument;
			this.sequenceEngine = sequenceEngine;
			this.transposition = transposition;
			this.activityVectorNames = activityVectorNames;
		}
	}
	
	private static final class ActivityVectorConfiguration {
		private String name;
		private double minActive;
		private boolean allowInactive;
		private double maxActive;
		private int startShift;
		private int stopShift;
		private int startAfterSection;
        private int stopBeforeSection;
        private int minSegmentCount;
        private int maxSegmentCount;
		private ActivityVector activityVector;

		private ActivityVectorConfiguration(String name, double minActive, boolean allowInactive, double maxActive, int startShift, int stopShift, int startAfterSection, int stopBeforeSection, int minSegmentCount, int maxSegmentCount) {
			this.name = name;
			this.minActive = minActive;
			this.allowInactive = allowInactive;
			this.maxActive = maxActive;
			this.startShift = startShift;
			this.stopShift = stopShift;
			this.startAfterSection = startAfterSection;
			this.stopBeforeSection = stopBeforeSection;
			this.minSegmentCount = minSegmentCount;
			this.maxSegmentCount = maxSegmentCount;
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
	
	private class ConstraintException extends RuntimeException {
		private ActivityVectorConfiguration avc;
		private String reason;
		
		public ConstraintException(ActivityVectorConfiguration avc, String reason) {
			super();
			this.avc = avc;
			this.reason = reason;
		}
		
		public String getReason() {
			return reason;
		}
		
		public ActivityVectorConfiguration getActivityVectorConfiguration() {
			return avc;
		}
	}
}