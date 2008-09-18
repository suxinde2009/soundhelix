package com.soundhelix.player;

/**
 * Represents an abstract player for Arrangements.
 * 
 * @see Arrangement
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

import org.apache.log4j.Logger;

import com.soundhelix.misc.Arrangement;
import com.soundhelix.misc.XMLConfigurable;

public abstract class Player implements XMLConfigurable {
	protected final Logger logger;

    public Player() {
    	logger = Logger.getLogger(this.getClass());
    }

	/**
	 * Opens all required resources for playing.
	 */

    public abstract void open();
    
	/**
	 * Plays the given arrangement. The method will play the arrangement
	 * and will return as soon as playing has finished. The method
	 * open() must have been called once prior to calling this method.
	 * 
	 * @param arrangement the Arrangement to play
	 */

     public abstract void play(Arrangement arrangement);

 	/**
 	 * Closes all required resources. The method play() must
 	 * not be called after resources have been closed, unless
 	 * open() has been called after that.
 	 */

      public abstract void close();
}
