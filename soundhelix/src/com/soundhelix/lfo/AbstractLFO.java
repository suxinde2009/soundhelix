package com.soundhelix.lfo;

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

    /** The random seed. */
    protected long randomSeed;

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

        double angle = TWO_PI * ((double) tick * rotationsPerTick + phase);
        int value = minAmplitude + (int) (0.5d + (maxAmplitude - minAmplitude) * getValue(angle));

        if (value > maxValue) {
            return maxValue;
        } else if (value < minValue) {
            return minValue;
        } else {
            return value;
        }
    }

    @Override
    public void setBeatSpeed(double rotationsPerBeat, int ticksPerBeat) {
        this.rotationsPerTick = rotationsPerBeat / (double) ticksPerBeat;
        isConfigured = true;
    }

    @Override
    public void setSongSpeed(double rotationsPerSong, int ticksPerSong) {
        this.rotationsPerTick = rotationsPerSong / (double) ticksPerSong;
        isConfigured = true;
    }

    @Override
    public void setActivitySpeed(double rotationsPerActivity, int startTick, int endTick) {
        this.rotationsPerTick = rotationsPerActivity / (double) (endTick - startTick);
        this.phase -= rotationsPerTick * (double) startTick;
        isConfigured = true;
    }

    @Override
    public void setTimeSpeed(double rotationsPerSecond, int ticksPerBeat, double bpm) {
        this.rotationsPerTick = rotationsPerSecond * 60.0d / bpm / (double) ticksPerBeat;
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
}
