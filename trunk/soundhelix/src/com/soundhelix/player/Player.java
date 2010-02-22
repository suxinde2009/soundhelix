package com.soundhelix.player;

import com.soundhelix.misc.Arrangement;
import com.soundhelix.misc.RandomSeedable;
import com.soundhelix.misc.XMLConfigurable;

/**
 * Represents an interface for playing Arrangements.
 * 
 * @see Arrangement
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

// TODO: check if more methods should be available, for example,
// getting the current tick, setting the current tick

public interface Player extends XMLConfigurable,RandomSeedable {
	/**
	 * Opens all required resources for playing.
	 */

    public void open();
    
	/**
	 * Plays the given arrangement. The method will play the arrangement
	 * and will return as soon as playing has finished. The method
	 * open() must have been called once prior to calling this method.
	 * 
	 * @param arrangement the Arrangement to play
	 */

    public void play(Arrangement arrangement);

 	/**
 	 * Closes all required resources. The method play() must
 	 * not be called after resources have been closed, unless
 	 * open() has been called after that.
 	 */

    public abstract void close();
    
    public void setBPM(int bpm);
    
    public void abortPlay();
    
}
