package com.soundhelix.arrangementengine;

import com.soundhelix.misc.Arrangement;
import com.soundhelix.misc.RandomSeedable;
import com.soundhelix.misc.Structure;
import com.soundhelix.misc.XMLConfigurable;

/**
 * Interface for song arrangement generators.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public interface ArrangementEngine extends XMLConfigurable, RandomSeedable {

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
