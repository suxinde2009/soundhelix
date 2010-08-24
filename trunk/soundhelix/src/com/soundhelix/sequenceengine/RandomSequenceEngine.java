package com.soundhelix.sequenceengine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

import com.soundhelix.util.RandomUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a generic sequence engine that generates a random pattern, with random
 * pitches, random note and pause lengths, random legato and random velocity, as well as the ability to repeat
 * subpatterns.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

// FIXME: endless loops are possible in certain cases

public class RandomSequenceEngine extends AbstractMultiPatternSequenceEngine {

	/**
	 * The length of the base pattern. The total length of the pattern is this value times the length of the pattern
	 * string.
	 */
	private int patternTicks = 16;

	/**
	 * The pattern used to generate the final pattern. Equal characters refer to equal patterns, different characters
	 * to different patterns. For example, the string "ABAC" will generate the 3 different patterns A, B and C with the
	 * same length and will generate a final pattern consisting of patterns A, B, A and C concatenated.
	 */
	private String patternString = "ABACABAD";

	/** The probability to use a note rather than a pause. */
	private double noteProbability = 0.8;
	/** The probability to use legato if a note is generated and the next pattern component is also a note. */
	private double legatoProbability = 0.25;

	/** The minimum velocity to use. */
	private double minVelocity = 1d;
	/** The maximum velocity to use. */
	private double maxVelocity = 20000d;
	
	/** The list of offsets to choose from. The list may contain values more than once for extra weight. */
	private int[] offsets = {0,1,2,3,4,5,6};
	/** The list of note lengths to choose from. The list may contain values more than once for extra weight. */
	private int[] noteLengths = {1,2,3,2,1,1};
	/** The list of pause lengths to choose from. The list may contain values more than once for extra weight. */
	private int[] pauseLengths = {1,2,3,2,1,1};
	/** The minimum number of active ticks. */
	private int minActiveTicks = 1;
	/** The maximum number of active ticks. */
	private int maxActiveTicks = 16;

	
	/**
	 * The correlation between pitch and velocity. If 1, the velocity depends only on the pitch and not on the random
	 * value; if 0, the velocity only depends on the random value.
	 */
	private double pitchVelocityCorrelation = 0.75;
	
	/**
	 * The exponent for the power distribution used for velocity. 1 distributes linearly between minimum and maximum
	 * velocity, 2 quadratically, 0.5 like a square-root, etc. The same happens for a negative exponent, but reverses
	 * the meaning of minimum and maximum velocity.
	 */
	private double velocityExponent = 3.0d;
	
    public void configure(Node node,XPath xpath) throws XPathException {
    	random = new Random(randomSeed);
    	
		try {
			setObeyChordSubtype(XMLUtils.parseBoolean(random,"obeyChordSubtype",node,xpath));
		} catch (Exception e) {
		}

		setPatternTicks(XMLUtils.parseInteger(random,"patternTicks",node,xpath));
		setNoteProbability(XMLUtils.parseDouble(random,"noteProbability",node,xpath) / 100.0);
		setLegatoProbability(XMLUtils.parseDouble(random,"legatoProbability",node,xpath) / 100.0);
		setMinVelocity(XMLUtils.parseInteger(random,"minVelocity",node,xpath));
		setMaxVelocity(XMLUtils.parseInteger(random,"maxVelocity",node,xpath));
		setMinActiveTicks(XMLUtils.parseInteger(random,"minActiveTicks",node,xpath));
		setMaxActiveTicks(XMLUtils.parseInteger(random,"maxActiveTicks",node,xpath));
    	setOffsets(XMLUtils.parseIntegerListString(random,"offsets",node,xpath));
    	setNoteLengths(XMLUtils.parseIntegerListString(random,"noteLengths",node,xpath));
    	setPauseLengths(XMLUtils.parseIntegerListString(random,"pauseLengths",node,xpath));
		setPitchVelocityCorrelation(XMLUtils.parseDouble(random,"pitchVelocityCorrelation",node,xpath) / 100.0d);
		setVelocityExponent(XMLUtils.parseDouble(random,"velocityExponent",node,xpath));
		setPatternString(XMLUtils.parseString(random,"patternString",node,xpath));
		setPatterns(new String[] {generatePattern(patternString)});
    }
    
    /**
     * Generates a pattern that is based on the given pattern of patterns. Each character
     * in the pattern string corresponds to one generated pattern.
     * 
     * @param patternPattern the string of pattern characters 
     * 
     * @return the generated pattern
     */
    
    private String generatePattern(String patternPattern) {
    	// maps pattern characters to patterns
    	Map <Character,String> patternMap = new HashMap<Character,String>();
    	Set <String> patternSet = new HashSet<String>();
    	
    	// generate patterns from the pattern list and add them to total pattern

    	String basePattern = null;    	
    	StringBuilder totalPattern = new StringBuilder();
    	
    	for (int i = 0; i < patternPattern.length(); i++) {
    		char c = patternPattern.charAt(i);
    		String p = patternMap.get(c);
    		
    		if (p == null) {
    			if (basePattern == null) {
    				p = generateBasePattern();
    				basePattern = p;
    			} else {
    				// generate a modified pattern until we find one that we haven't used so far
    				do {
    					p = modifyPattern(basePattern,2);
    				} while(patternSet.contains(p));
    			}

    			if (logger.isDebugEnabled()) {
    				logger.debug("Pattern " + c + ": " + p);    			
    			}
    				
				patternMap.put(c,p);
				patternSet.add(p);
    		}

    		if (totalPattern.length() > 0) {
    			totalPattern.append(',');
    		}
    		
    		totalPattern.append(p);
    	}
    	
    	return totalPattern.toString();    
    }
    
    /**
     * Creates and returns a random pattern.
     * 
     * @return the random pattern
     */
    
    private String generateBasePattern() {
    	StringBuilder sb;
    	int activeTicks = 0;
    	
    	do {
    		sb = new StringBuilder();
    		activeTicks = 0;
    		
    		final int minPitch = findMinimum(offsets);
    		final int maxPitch = findMaximum(offsets);
    		final int pitchDiff = maxPitch - minPitch;

    		boolean currentIsNote;
    		boolean nextIsNote = random.nextDouble() < noteProbability;

    		
    		for (int i = 0; i < patternTicks;) {
    			currentIsNote = nextIsNote;
    			nextIsNote = random.nextDouble() < noteProbability;

    			int length;

    			if (currentIsNote) {
    				length = noteLengths[random.nextInt(noteLengths.length)];
    			} else {
    				length = pauseLengths[random.nextInt(pauseLengths.length)];
    			}
    			
    			if (i + length > patternTicks) {
    				length = patternTicks - i;
    			}

    			if (currentIsNote) {    		
    				int pitch = offsets[random.nextInt(offsets.length)];

    				double v = (pitchDiff == 0 ? 0.5 : ((1.0d - pitchVelocityCorrelation) * random.nextDouble() +
    						pitchVelocityCorrelation * (pitch - minPitch) / pitchDiff));

    				int volume;

    				if (velocityExponent >= 0.0d) {
    					volume = (int) RandomUtils.getPowerDouble(v, minVelocity, maxVelocity, velocityExponent);
    				} else {
    					volume = (int) RandomUtils.getPowerDouble(v, maxVelocity, minVelocity, -velocityExponent);
    				}

    				boolean isLegato = nextIsNote && (i + length < patternTicks) && random.nextDouble() < legatoProbability;

    				if (isLegato) {
    					sb.append(pitch).append("~/").append(length).append(':').append(volume);
    				} else {
    					sb.append(pitch).append("/").append(length).append(':').append(volume);   			
    				}
    				
    				activeTicks += length;
    				
    			} else {
    				sb.append("-/").append(length);
    			}

    			i += length;

    			if (i < patternTicks) {
    				sb.append(',');
    			}
    		}
    	} while(activeTicks < minActiveTicks || activeTicks > maxActiveTicks);

    	return sb.toString();
    }
    
    /**
     * Takes the given pattern and returns a modified version of it by
     * doing the given number of random swaps. It is possible for the
     * pattern to be identical to the original pattern.
     * 
     * @param pattern the original pattern
     * @param modifications the number of modifications
     * 
     * @return the modified pattern
     */

    private String modifyPattern(String pattern, int modifications) {
    	String[] values = pattern.split(",");
    	int length = values.length;
    	
    	for (int i = 0; i < modifications; i++) {
    		int x,y;
    		
    		do {
    			x = random.nextInt(length);
    			y = random.nextInt(length);
    		} while(x == y && length > 1);

    		String z = values[x];
    		values[x] = values[y];
    		values[y] = z;
    	}
    	
    	StringBuilder sb = new StringBuilder();
    	
    	for (int i = 0; i < length;i++) {
    		sb.append(values[i]);
    		
    		if (i < length - 1) {
    			sb.append(',');
    		}
    	}
    	
    	return sb.toString();
    }
    
    /**
     * Finds the maximum integer from the given list of ints.
     * If the list is empty, Integer.MIN_VALUE is returned.
     *
     * @param list the list of ints
     * 
     * @return the maximum integer from the list
     */
    
    private int findMaximum(int[] list) {
    	int maximum = Integer.MIN_VALUE;
    	int num = list.length;
    	
    	for (int i = 0; i < num; i++) {
    		maximum = Math.max(list[i], maximum);
    	}
    	
    	return maximum;
    }

    /**
     * Finds the minimum integer from the given list of ints.
     * If the list is empty, Integer.MAX_VALUE is returned.
     *
     * @param list the list of ints
     * 
     * @return the minimum integer from the list
     */

    private int findMinimum(int[] list) {
    	int minimum = Integer.MAX_VALUE;
    	int num = list.length;
    	
    	for (int i = 0; i < num; i++) {
    		minimum = Math.min(list[i], minimum);
    	}
    	
    	return minimum;
    }

    public void setPatternTicks(int patternTicks) {
    	this.patternTicks = patternTicks;
    }
    
    public void setNoteProbability(double noteProbability) {
    	this.noteProbability = noteProbability;
    }
    
    public void setLegatoProbability(double legatoProbability) {
    	this.legatoProbability = legatoProbability;
    }
    
    public void setOffsets(int[] offsets) {
    	this.offsets = offsets;
    }

	public void setMinVelocity(double minVelocity) {
		this.minVelocity = minVelocity;
	}

	public void setMaxVelocity(double maxVelocity) {
		this.maxVelocity = maxVelocity;
	}

	public void setPatternString(String patternString) {
		this.patternString = patternString;
	}

	public void setNoteLengths(int[] noteLengths) {
		this.noteLengths = noteLengths;
	}

	public void setPauseLengths(int[] pauseLengths) {
		this.pauseLengths = pauseLengths;
	}

	public void setPitchVelocityCorrelation(double pitchVelocityCorrelation) {
		this.pitchVelocityCorrelation = pitchVelocityCorrelation;
	}

	public void setVelocityExponent(double velocityExponent) {
		this.velocityExponent = velocityExponent;
	}

	public void setMinActiveTicks(int minActiveTicks) {
		this.minActiveTicks = minActiveTicks;
	}

	public void setMaxActiveTicks(int maxActiveTicks) {
		this.maxActiveTicks = maxActiveTicks;
	}
}
