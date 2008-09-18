package com.soundhelix.arrangementengine;

import org.apache.log4j.Logger;

import com.soundhelix.misc.Arrangement;
import com.soundhelix.misc.Structure;
import com.soundhelix.misc.XMLConfigurable;

/**
 * Represents an abstract generator for song arrangements.
 * 
 * @author Thomas Schürger
 */

public abstract class ArrangementEngine implements XMLConfigurable {
	protected final Logger logger;
	
	protected Structure structure;
	
	public ArrangementEngine() {
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
}
