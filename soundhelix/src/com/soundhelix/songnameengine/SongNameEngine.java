package com.soundhelix.songnameengine;

import com.soundhelix.misc.RandomSeedable;
import com.soundhelix.misc.XMLConfigurable;

/**
 * @author Thomas Schuerger
 */

public interface SongNameEngine extends RandomSeedable, XMLConfigurable {
    /**
     * Generates and returns a song name.
     * 
     * @return a song name
     */
    
    String createSongName();
}
