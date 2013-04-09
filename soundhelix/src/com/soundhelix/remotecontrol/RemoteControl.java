package com.soundhelix.remotecontrol;

import com.soundhelix.misc.SongContext;

/**
 * Interface for remote-controlling a player.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public interface RemoteControl extends Runnable {
    /**
     * Gets the song context to be remote-controlled.
     * 
     * @return the song context
     */

    SongContext getSongContext();

    /**
     * Sets the song context to be remote-controlled.
     * 
     * @param songContext the song context
     */

    void setSongContext(SongContext songContext);
}
