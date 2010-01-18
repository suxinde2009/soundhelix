package com.soundhelix.sequenceengine;

import java.util.Random;

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
    	StringBuilder sb = new StringBuilder();

    	final int ticks = 16;
    	final double noteRatio = 0.7;
    	final double slideRatio = 0.3;
    	
    	double minVolume = 1d;
    	double maxVolume = 16000d;
    	
    	// 1 = only pitch dependent, 0 = not pitch dependent
    	double pitchFactor = 0.7;
    	int maxPitch = 5;
    	double order = 3.0d;

    	boolean currentIsNote;
    	boolean nextIsNote = random.nextDouble() < noteRatio;

    	for(int i=0;i<ticks;) {
    		int length = random.nextInt(2)+1;

    		if (i + length > ticks) {
    			length = ticks - i;
    		}

    		currentIsNote = nextIsNote;
        	nextIsNote = random.nextDouble() < noteRatio;

        	if (currentIsNote) {    		
    			int pitch = random.nextInt(maxPitch + 1);
    			
    			double v = ((1.0d-pitchFactor)*random.nextDouble() + pitchFactor*pitch/(maxPitch));
    			int volume = (int) RandomUtils.getPowerDouble(v, minVolume, maxVolume, order);
    			
    			boolean isSlide = nextIsNote && (i + length < ticks) && random.nextDouble() < slideRatio;
    			
    			if (isSlide) {
    				sb.append(pitch + "~/" + length + ":" + volume);
    			} else {
       				sb.append(pitch+ "/" + length+ ":" +volume);   			
    			}
    		} else {
    			sb.append("-/"+length);
    		}
    		
    		i += length;
    		
    		if (i < ticks) {
    			sb.append(',');
    		}
    	}
    
    	logger.debug("String: "+sb);
    	
    	return sb.toString();    
    }
}
