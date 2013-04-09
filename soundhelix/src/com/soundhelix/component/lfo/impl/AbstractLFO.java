package com.soundhelix.component.lfo.impl;

import com.soundhelix.component.lfo.LFO;
import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.SongContext;

/**
 * This class implements the basic methods for setting the mode and speed of the LFO as well as the minimum and maximum amplitude and value to return.
 * In addition, the LFO's starting phase can be set. The type of LFO (e.g., sine, triangle, sawtooth or rectangle wave) must be implemented by
 * subclasses. Subclasses can implement any type of waveform (for example, Bezier-spline interpolated random waveforms), but must return their results
 * so that they depend on the selected speed in a natural way and that they are consistent (same method parameters must return same results for the
 * same configuration of the same instance).
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public abstract class AbstractLFO implements LFO {
    /** Two times Pi. */
    protected static final double TWO_PI = 2.0d * Math.PI;

    /** The LFO mode. */
    private enum Mode {
        /** Constant angular speed. */
        CONSTANT_SPEED,
        
        /** Synchronized to segment pairs. */
        SYNC_TO_SEGMENT_PAIRS
    }
    
    /** The song context. */
    protected SongContext songContext;
    
    /** The random seed. */
    protected long randomSeed;

    /** The LFO mode. */
    private Mode mode;
    
    /** The minimum amplitude value. */
    private int minValue = Integer.MIN_VALUE;

    /** The maximum amplitude value. */
    private int maxValue = Integer.MAX_VALUE;

    /** The minimum amplitude value. */
    private int minAmplitude;

    /** The maximum amplitude value. */
    private int maxAmplitude;

    /** The number of rotations per tick. */
    private double rotationsPerTick;

    /** The number of rotations per activity segment pair. */
    private double rotationsPerSegmentPair;

    /** The name of the activity vector. */
    private String activityVectorName;

    /** The segment lengths of the ActivityVector. */
    private int[] segmentLengths;

    /** The start phase in number of rotations. */
    private double phase;

    /** True if one of the set...Speed() methods has been called. */
    private boolean isConfigured;

    /**
     * Returns the LFO's value of the given angle as a double. The returned value must be between 0 and 1 (both inclusive).
     * 
     * @param angle the angle in radians (non-negative)
     * 
     * @return the LFO's value (between 0 and 1, both inclusive)
     */

    protected abstract double getValue(double angle);

    @Override
    public int getTickValue(int tick) {
        if (!isConfigured) {
            throw new RuntimeException("LFO speed not set yet");
        }

        double angle;
        
        if (mode == Mode.CONSTANT_SPEED) {
            angle = TWO_PI * (tick * rotationsPerTick + phase);
        } else if (mode == Mode.SYNC_TO_SEGMENT_PAIRS) {
            angle = getSegmentAngle(tick);
        } else {
            throw new RuntimeException("Invalid mode");
        }
        
        int value = minAmplitude + (int) (0.5d + (maxAmplitude - minAmplitude) * getValue(angle));

        if (value > maxValue) {
            return maxValue;
        } else if (value < minValue) {
            return minValue;
        } else {
            return value;
        }
    }

    private double getSegmentAngle(int tick) {
        if (segmentLengths == null) {
            ActivityVector av = songContext.getActivityMatrix().get(activityVectorName);
            
            if (av == null) {
                throw new RuntimeException("Unknown ActivityVector \"" + activityVectorName + "\"");
            }
            
            segmentLengths = av.getSegmentLengths();
        }
        
        // identify which type of segment the tick is in and determine the tick's position relative to the segment start
        // as well as the length of that segment
        
        int currentTick = 0;
        
        int tickInSegment = 0;
        int segmentLength = 0;
        
        for (int length : segmentLengths) {
            if (length > 0) {
                if (tick < currentTick + length) {
                    tickInSegment = tick - currentTick;
                    segmentLength = length;
                    break;
                }
                
                currentTick += length;
            } else {
                // length is negative
                if (tick < currentTick - length) {
                    tickInSegment = tick - currentTick;
                    segmentLength = length;
                    break;
                }
                
                // length is negative
                currentTick -= length;
            }
        }
        
        if (segmentLength > 0) {
            // first half (0 to pi)
            return TWO_PI * (0.5d * tickInSegment / segmentLength * rotationsPerSegmentPair + phase);
        } else {
            // second half (pi to 2*pi)
            return TWO_PI * (0.5d + 0.5d * tickInSegment / (-segmentLength) * rotationsPerSegmentPair + phase);
        }
    }
    
    @Override
    public void setBeatSpeed(double rotationsPerBeat, int ticksPerBeat) {
        this.rotationsPerTick = rotationsPerBeat / ticksPerBeat;
        this.mode = Mode.CONSTANT_SPEED;
        isConfigured = true;
    }

    @Override
    public void setSongSpeed(double rotationsPerSong, int ticksPerSong) {
        this.rotationsPerTick = rotationsPerSong / ticksPerSong;
        this.mode = Mode.CONSTANT_SPEED;
        isConfigured = true;
    }

    @Override
    public void setActivitySpeed(double rotationsPerActivity, int startTick, int endTick) {
        this.rotationsPerTick = rotationsPerActivity / (endTick - startTick);
        this.phase -= rotationsPerTick * startTick;
        this.mode = Mode.CONSTANT_SPEED;
        isConfigured = true;
    }

    @Override
    public void setTimeSpeed(double rotationsPerSecond, int ticksPerBeat, double bpm) {
        this.rotationsPerTick = rotationsPerSecond * 60.0d / bpm / ticksPerBeat;
        this.mode = Mode.CONSTANT_SPEED;
        isConfigured = true;
    }

    @Override
    public void setSegmentPairSpeed(double rotationsPerSegmentPair, String activityVectorName) {
        this.rotationsPerSegmentPair = rotationsPerSegmentPair;
        this.activityVectorName = activityVectorName;
        this.mode = Mode.SYNC_TO_SEGMENT_PAIRS;
        isConfigured = true;
    }

    @Override
    public void setPhase(double phase) {
        this.phase = phase;
    }

    @Override
    public void setMinValue(int minValue) {
        this.minValue = minValue;
    }

    @Override
    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }

    @Override
    public void setMinAmplitude(int minAmplitude) {
        this.minAmplitude = minAmplitude;
    }

    @Override
    public void setMaxAmplitude(int maxAmplitude) {
        this.maxAmplitude = maxAmplitude;
    }

    @Override
    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    @Override
    public long getRandomSeed() {
        return randomSeed;
    }

    @Override
    public void setSongContext(SongContext songContext) {
        this.songContext = songContext;
    }
}
