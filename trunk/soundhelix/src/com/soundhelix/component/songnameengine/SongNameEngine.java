package com.soundhelix.component.songnameengine;

import com.soundhelix.component.Component;

/**
 * Interface for song name engines.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public interface SongNameEngine extends Component {
    /**
     * Generates and returns a song name.
     * 
     * @return a song name
     */

    String createSongName();
}
