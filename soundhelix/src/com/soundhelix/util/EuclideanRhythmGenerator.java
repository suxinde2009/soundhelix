package com.soundhelix.util;

import java.util.Arrays;

/**
 * Implements the Bjorklund algorithm (due to E. Bjorklund), which is the basis for generating Euclidean rhythms, as mentioned in the paper
 * "The Euclidean Algorithm Generates Traditional Musical Rhythms" by Godfried Toussaint. It generates all mentioned patterns in the paper correctly.
 * It may look like it does not generate E(5, 16) correctly, but actually this is a mistake in the paper (the mentioned pattern in the paper contains
 * 17 steps instead of 16; it ends with an additional pause). The correct pattern for E(5, 16) is "x..x..x..x..x...". The original paper can be found
 * at http://archive.bridgesmathart.org/2005/bridges2005-47.pdf.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com) * 
 */

public class EuclideanRhythmGenerator {
    
    /**
     * Generates the Euclidean rhythm with the given number of pulses spread across the given number of steps. 
     * 
     * @param pulses the number of pulses (must be >= 0 and must not be larger than steps)
     * @param steps the number of steps (must be > 0)
     * 
     * @return a step-element boolean array containing the pattern
     */
    
    public static boolean[] generate(int pulses, int steps) {
        if (pulses > steps || pulses < 0 || steps <= 0) {
            throw new IllegalArgumentException();
        }

        if (pulses == 0) {
            // easy case: all false
            return new boolean[steps];
        } else if (pulses == steps) {
            // easy case: all true
            boolean[] result = new boolean[steps];
            Arrays.fill(result, true);
            return result;
        }

        boolean[][] array = new boolean[steps][steps];
        int[] len = new int[steps];

        int pauses = steps - pulses;
        Arrays.fill(array[0], 0, pulses, true);
        len[0] = Math.max(pulses, pauses);
        len[1] = Math.min(pulses, pauses);

        int height = 2;
        int maxHeight = 1;
        int maxLen = len[0];
        int minLen = len[1];

        while (maxLen > minLen + 1) {
            int cutOff = Math.min(minLen, maxLen - minLen);
            for (int i = 0; i < maxHeight; i++) {
                System.arraycopy(array[i], len[i] - cutOff, array[height], 0, cutOff);
                len[i] -= cutOff;
                len[height] = cutOff;
                height++;
            }
            
            minLen = cutOff;
            maxLen = len[0];

            for (int i = height - 1; i >= maxHeight; i--) {
                if (len[i] == len[0]) {
                    maxHeight = i + 1;
                    break;
                }
            }
        }

        boolean[] result = new boolean[steps];

        int p = 0;
        for (int k = 0; k < len[0]; k++) {
            for (int i = 0; i < height; i++) {
                if (k < len[i]) {
                    result[p++] = array[i][k];
                }
            }
        }

        return result;
    }

    public static void main(String[] args) {
        generate(5, 13);
        generate(1, 2);
        generate(1, 3);
        generate(1, 4);
        generate(4, 12);
        generate(2, 3);
        generate(2, 5);
        generate(3, 4);
        generate(3, 5);
        generate(3, 7);
        generate(3, 8);
        generate(4, 7);
        generate(4, 9);
        generate(4, 11);
        generate(5, 6);
        generate(5, 7);
        generate(5, 8);
        generate(5, 9);
        generate(5, 11);
        generate(5, 12);
        generate(5, 16);
        generate(7, 8);
        generate(7, 12);
        generate(7, 16);
        generate(9, 16);
        generate(11, 24);
        generate(13, 24);
    }
}