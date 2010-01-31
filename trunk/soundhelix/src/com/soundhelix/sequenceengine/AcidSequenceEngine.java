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
 * Implements a sequence engine that generates a random pattern.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class AcidSequenceEngine extends AbstractMultiPatternSequenceEngine {

    public void configure(Node node,XPath xpath) throws XPathException {
    	random = new Random(randomSeed);
    	
		try {
			setObeyChordSubtype(XMLUtils.parseBoolean(random,"obeyChordSubtype",node,xpath));
		} catch(Exception e) {}
		
		setPatterns(new String[] {generatePattern()});
    }
    
    private String generatePattern() {
    	final String pattern = "ABAC";

    	// maps pattern characters to patterns
    	Map <Character,String> patternMap = new HashMap<Character,String>();
    	Set <String> patternSet = new HashSet<String>();
    	
    	// generate patterns from the pattern list and add them to total pattern

    	String basePattern = null;    	
    	StringBuilder totalPattern = new StringBuilder();
    	
    	for(int i=0;i<pattern.length();i++) {
    		char c = pattern.charAt(i);
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

    			logger.debug("Pattern " + c + ": "+p);    			

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
    
    private String generateBasePattern() {
    	StringBuilder sb = new StringBuilder();

    	final int ticks = 16;
    	final double noteRatio = 0.8;
    	final double slideRatio = 0.25;
    	
    	double minVolume = 1d;
    	double maxVolume = 20000d;
    	
    	// 1 = only pitch dependent, 0 = not pitch dependent
    	double pitchFactor = 0.7;
    	int maxPitch = 6;
    	double order = 3.0d;

    	boolean currentIsNote;
    	boolean nextIsNote = random.nextDouble() < noteRatio;

    	for(int i=0;i<ticks;) {
    		int length = random.nextInt(2) + 1;

    		if (i + length > ticks) {
    			length = ticks - i;
    		}

    		currentIsNote = nextIsNote;
        	nextIsNote = random.nextDouble() < noteRatio;

        	if (currentIsNote) {    		
    			int pitch = random.nextInt(maxPitch + 1);
    			
    			double v = ((1.0d - pitchFactor) * random.nextDouble() + pitchFactor * pitch / maxPitch);
    			int volume = (int) RandomUtils.getPowerDouble(v, minVolume, maxVolume, order);
    			
    			boolean isSlide = nextIsNote && (i + length < ticks) && random.nextDouble() < slideRatio;
    			
    			if (isSlide) {
    				sb.append(pitch).append("~/").append(length).append(":").append(volume);
    			} else {
       				sb.append(pitch).append("/").append(length).append(":").append(volume);   			
    			}
    		} else {
    			sb.append("-/").append(length);
    		}
    		
    		i += length;
    		
    		if (i < ticks) {
    			sb.append(',');
    		}
    	}
    	
    	return sb.toString();
    }
    
    private String modifyPattern(String pattern, int modifications) {
    	String[] values = pattern.split(",");
    	int length = values.length;
    	
    	for(int i=0;i<modifications;i++) {
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
    	
    	for(int i=0;i<length;i++) {
    		sb.append(values[i]);
    		
    		if(i < length-1) {
    			sb.append(',');
    		}
    	}
    	
    	return sb.toString();
    }
}
