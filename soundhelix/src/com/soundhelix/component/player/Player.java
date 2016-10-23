package com.soundhelix.component.player;

import com.soundhelix.component.Component;
import com.soundhelix.misc.SongContext;

/**
 * Represents an interface for playing Arrangements. A player's task usually is to play the arrangement in real-time.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public interface Player extends Component {
    /**
     * Plays the arrangement last set by setArrangement(). The method will return as soon as playing has finished. The method open() must have been
     * called once prior to calling this method. This method will block until the song is finished or playback has been aborted via abortPlay().
     * 
     * @param songContext the song context
     */

    void play(SongContext songContext);

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
     * Aborts play(). When this message returns, the playback should already have stopped and all resources should have been released. If the player
     * isn't playing, nothing is done.
     */

    void abortPlay();
}
