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
	/** The logger. */
	protected final Logger logger;
	
	/** The structure. */
	protected Structure structure;
	
	/** The random seed. */
	protected long randomSeed;
	
	/**
	 * Constructor.
	 */
	
	public AbstractArrangementEngine() {
		logger = Logger.getLogger(getClass());
	}	

	/**
	 * Sets the structure.
	 * 
	 * @param structure the structure
	 */
	
	public void setStructure(Structure structure) {
		if (this.structure != null) {
			throw new RuntimeException("Structure already set");
		}
		
		this.structure = structure;
	}
	
	/**
	 * Renders and returns an Arrangement.
	 * 
	 * @return the rendered arrangement
	 */
	
    public abstract Arrangement render();
    
    /**
     * Sets the random seed.
     * 
     * @param randomSeed the random seed
     */
    
    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    /**
     * Returns the random seed.
     * 
     * @return the random seed
     */
    
    public long getRandomSeed() {
        return randomSeed;
    }
}