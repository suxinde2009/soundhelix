package com.soundhelix.arrangementengine;

import org.apache.log4j.Logger;

import com.soundhelix.misc.Arrangement;
import com.soundhelix.misc.Structure;

/**
 * Abstract implementation of an ArrangementEngine, which provides some basic
 * functionality. 
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public abstract class AbstractArrangementEngine implements ArrangementEngine {
	protected final Logger logger;
	
	protected Structure structure;
	protected long randomSeed;
	
	public AbstractArrangementEngine() {
		logger = Logger.getLogger(getClass());
	}	

	public void setStructure(Structure structure) {
		if(this.structure != null) {
			throw(new RuntimeException("Structure already set"));
		}
		
		this.structure = structure;
	}
	
	/**
	 * Renders and returns an Arrangement.
	 * 
	 * @return the rendered arrangement
	 */
	
    public abstract Arrangement render();
    
    public void setRandomSeed(long randomSeed) {
    	 this.randomSeed = randomSeed;
     }

     public long getRandomSeed() {
    	 return randomSeed;
     }
}