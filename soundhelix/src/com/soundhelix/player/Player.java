package com.soundhelix.player;

import com.soundhelix.misc.Arrangement;
import com.soundhelix.misc.RandomSeedable;
import com.soundhelix.misc.XMLConfigurable;

/**
 * Represents an interface for playing Arrangements. A player's task usually is to play the
 * arrangement in real-time.
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

    /**
     * Gets the playback speed in milli-BPM.
     * 
     * @return the speed in milli-BPM
     */
    
    public int getMilliBPM();

    /**
     * Sets the playback speed in milli-BPM. Setting the BPM speed should be possible while
     * the player is playing an arrangement.
     *
     * @param milliBPM the speed in milli-BPM
     */
    
    public void setMilliBPM(int milliBPM);
    
    /**
     * Aborts playback. Calling this method should stop the player if it is currently
     * playing an arrangement.
     */
    
    public void abortPlay();
    
}
