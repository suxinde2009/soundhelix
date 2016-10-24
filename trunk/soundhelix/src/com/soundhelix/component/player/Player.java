package com.soundhelix.component.player;

import com.soundhelix.component.Component;
import com.soundhelix.misc.SongContext;

/**
 * Represents an interface for playing Arrangements. A player's task is to play the arrangement in real-time. All methods except for play() are
 * callable and should have an immediate or delayed effect while play() is running in another thread.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public interface Player extends Component {
    /**
     * Plays the arrangement from the song context. The method will return as soon as playing has finished. This method will block until the song is
     * finished or playback has been aborted successfully via abortPlay(). Only one thread can run play() at a time.
     * 
     * @param songContext the song context
     * 
     * @throws IllegalStateException if play() is called while play() is running in another thread
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
