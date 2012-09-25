package com.soundhelix.component.songnameengine;

import com.soundhelix.misc.RandomSeedable;
import com.soundhelix.misc.XMLConfigurable;

/**
 * Interface for song name engines.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public interface SongNameEngine extends RandomSeedable, XMLConfigurable {
    /**
     * Generates and returns a song name.
     * 
     * @return a song name
     */

    String createSongName();
}
