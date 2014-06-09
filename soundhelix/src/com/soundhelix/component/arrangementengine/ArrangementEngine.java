package com.soundhelix.component.arrangementengine;

import com.soundhelix.component.Component;
import com.soundhelix.misc.Arrangement;
import com.soundhelix.misc.SongContext;

/**
 * Interface for song arrangement generators.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public interface ArrangementEngine extends Component {
    /**
     * Renders and returns an Arrangement.
     * 
     * @param songContext songContext
     * 
     * @return the rendered arrangement
     */

    Arrangement render(SongContext songContext);
}
