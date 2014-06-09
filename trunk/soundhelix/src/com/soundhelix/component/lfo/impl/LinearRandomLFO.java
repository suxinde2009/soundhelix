package com.soundhelix.component.lfo.impl;

import java.util.Random;

import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;

import com.soundhelix.misc.SongContext;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a low frequency oscillator (LFO) that linearly interpolates between a given number of random values in a round-robin manner.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class LinearRandomLFO extends AbstractLFO {
    /** The random generator. */
    private Random random;

    /** The number of random values. */
    private int valueCount;

    /** The random values. */
    private double[] values;

    /** The minimum distance between neighboring values. */
    private double minDistance;

    /** The maximum distance between neighboring values. */
    private double maxDistance;

    @Override
    public double getValue(double angle) {
        // normalize angle into the range [0,2*Pi[
        angle = ((angle % TWO_PI) + TWO_PI) % TWO_PI;

        double index = angle / TWO_PI * valueCount;
        double fraction = index - Math.floor(index);
        int intIndex = (int) index;

        return (1.0d - fraction) * values[intIndex] + fraction * values[(intIndex + 1) % valueCount];
    }

    @Override
    public final void configure(SongContext songContext, Node node) throws XPathException {
        random = new Random(randomSeed);

        int values = XMLUtils.parseInteger(random, "valueCount", node);

        if (values <= 0) {
            throw new RuntimeException("valueCount must be positive");
        }

        setValueCount(values);

        double minDist = XMLUtils.parseDouble(random, XMLUtils.getNode("minDistance", node));
        double maxDist = XMLUtils.parseDouble(random, XMLUtils.getNode("maxDistance", node));

        if (minDist < 0.0 || minDist > 0.5) {
            throw new RuntimeException("minDistance must be in the range [0, 0.5]");
        }

        if (maxDist < 0.0 || maxDist > 1.0) {
            throw new RuntimeException("maxDistance must be in the range [0, 1.0]");
        }

        if (maxDist < minDist) {
            throw new RuntimeException("minDistance must be <= maxDistance");
        }

        setMinDistance(minDist);
        setMaxDistance(maxDist);

        generateValues();
    }

    /**
     * Generates the LFO values.
     */

    private void generateValues() {
        values = new double[valueCount];
        double diff;

        do {
            values[0] = random.nextDouble();

            for (int i = 1; i < valueCount; i++) {
                double value;

                do {
                    value = random.nextDouble();
                    diff = Math.abs(value - values[i - 1]);
                } while (diff < minDistance || diff > maxDistance);

                values[i] = value;
            }

            // special handling for the last value
            diff = Math.abs(values[0] - values[valueCount - 1]);
        } while (diff < minDistance || diff > maxDistance);
    }

    public void setValueCount(int valueCount) {
        this.valueCount = valueCount;
    }

    public double getMinDistance() {
        return minDistance;
    }

    public void setMinDistance(double minDistance) {
        this.minDistance = minDistance;
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
    }
}