package com.soundhelix.remotecontrol;

import com.soundhelix.player.Player;

/**
 * Interface for remote-controlling a player.
 *
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public interface RemoteControl extends Runnable {
    /**
     * Sets the player that is to be remote-controlled.
     * 
     * @param player the player
     */
    
    void setPlayer(Player player);
    
    /** 
     * Gets the player that is to be remote-controlled.
     * 
     * @return the player
     */
    
    Player getPlayer();
}
