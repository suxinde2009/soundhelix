package com.soundhelix.component.patternengine;

import com.soundhelix.component.Component;
import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.SongContext;

/**
 * Represents a generator for patterns.
 * 
 * @see com.soundhelix.misc.Pattern
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public interface PatternEngine extends Component {
    /**
     * Generates a pattern.
     * 
     * @param songContext the song context
     * @param wildcardString the string containing the wildcard characters
     * 
     * @return the pattern entry
     */

    Pattern render(SongContext songContext, String wildcardString);
}
