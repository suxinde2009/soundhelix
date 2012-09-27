package com.soundhelix.component.arrangementengine;

import com.soundhelix.component.Component;
import com.soundhelix.misc.Arrangement;
import com.soundhelix.misc.Structure;

/**
 * Interface for song arrangement generators.
 *
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public interface ArrangementEngine extends Component {

    /**
     * Sets the structure.
     *
     * @param structure the structure
     */

    void setStructure(Structure structure);

    /**
     * Renders and returns an Arrangement.
     *
     * @return the rendered arrangement
     */

    Arrangement render();
}
