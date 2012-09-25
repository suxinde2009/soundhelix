package com.soundhelix.component.player;

import com.soundhelix.misc.Arrangement;
import com.soundhelix.misc.Component;

/**
 * Represents an interface for playing Arrangements. A player's task usually is to play the arrangement in real-time.
 *
 * @see Arrangement
 *
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public interface Player extends Component {
    /**
     * Opens all required resources for playing.
     */

    void open();

    /**
     * Gets the arrangement to play.
     *
     * @return the arrangement
     */

    Arrangement getArrangement();

    /**
     * Sets the arrangement to play.
     *
     * @param arrangement the arrangement
     */

    void setArrangement(Arrangement arrangement);

    /**
     * Plays the arrangement last set by setArrangement(). The method will return as soon as playing has finished. The method open() must have been
     * called once prior to calling this method.
     */

    void play();

    /**
     * Closes all required resources. The method play() must not be called after resources have been closed, unless open() has been called after that.
     */

    void close();

    /**
     * Gets the playback speed in milli-BPM.
     *
     * @return the speed in milli-BPM
     */

    int getMilliBPM();

    /**
     * Sets the playback speed in milli-BPM. Setting the BPM speed should be possible while the player is playing an arrangement.
     *
     * @param milliBPM the speed in milli-BPM
     */

    void setMilliBPM(int milliBPM);

    /**
     * Skips to the specified tick. If this was done or triggered successfully, true is returned, otherwise false is returned. The player may not
     * support skipping at all or may not support skipping backwards. In this case, the player must ignore the skip request and must return false.
     * This method should return immediately, even if skipping takes a while to complete. Skipping to a different tick while in the midst of skipping
     * to a different tick may or may not be supported by the player.
     *
     * @param tick the tick to skip to
     *
     * @return true if skipping was successful
     */

    boolean skipToTick(int tick);

    /**
     * Returns the current tick of the player. If the player is not yet playing, if it is in the warm-up phase or in the cool-down phase or playing
     * has finished, -1 will be returned.
     *
     * @return the current tick (or -1)
     */

    int getCurrentTick();

    /**
     * Aborts playback. Calling this method should stop the player if it is currently playing an arrangement.
     */

    void abortPlay();
}
