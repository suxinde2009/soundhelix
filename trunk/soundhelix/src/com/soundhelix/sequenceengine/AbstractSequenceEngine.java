package com.soundhelix.sequenceengine;

/**
 * Represents an abstract generator for note sequences. This class provides
 * a dummy implementation of RandomSeedable, which is a no-op (it stores the
 * random seed and can return the stored seed, but doesn't use the seed). Subclasses
 * that want to make use of random-seedability must override setRandomSeed() and
 * getRandomSeed() accordingly. This is to make sure that a subclass is able to
 * react to calls to setRandomSeed() to re-seed the internal random generators, if
 * needed.
 * 
 * @see Sequence
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

import org.apache.log4j.Logger;

import com.soundhelix.misc.Structure;

public abstract class AbstractSequenceEngine implements SequenceEngine {
    protected final Logger logger;

    protected Structure structure;
    protected long randomSeed;

    public AbstractSequenceEngine() {
        logger = Logger.getLogger(this.getClass());
    }

    public void setStructure(Structure structure) {
        this.structure = structure;
    }

    /**
     * Returns the required number of ActivityVectors. For most implementations, this will be 1, but certain implementations, like for
     * multi-instrument sequences, more than 1 might be required. Subclasses should override this.
     * 
     * @return the number of ActivityVectors
     */

    public int getActivityVectorCount() {
        return 1;
    }

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public long getRandomSeed() {
        return randomSeed;
    }
}
