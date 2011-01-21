package com.soundhelix.patternengine;

import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.RandomSeedable;
import com.soundhelix.misc.XMLConfigurable;

/**
 * Represents a generator for patterns.
 *
 * @see com.soundhelix.misc.Pattern
 *
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public interface PatternEngine extends XMLConfigurable, RandomSeedable {
	/**
	 * Generates a pattern.
	 * 
	 * @param wildcardString the string containing the wildcard characters
	 *
	 * @return the pattern entry
	 */
	
	Pattern render(String wildcardString);
}
