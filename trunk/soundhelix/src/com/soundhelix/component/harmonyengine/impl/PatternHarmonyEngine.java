package com.soundhelix.component.harmonyengine.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Harmony;
import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.SongContext;
import com.soundhelix.misc.Structure;
import com.soundhelix.util.NoteUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a flexible HarmonyEngine based on user-specified patterns. The patterns consist of chord patterns and random table patterns. One of the
 * given chord patterns is chosen at random. A chord pattern consists of comma-separated combinations of chords and lengths in beats (separated by a
 * slash). A chord is either a simple chord name ("C" for C major, "Am" for A minor, etc.), a random table number (starting from 0), a random table
 * number (starting from 0) with a negative backreference (e.g., "0!0") to an already generated chord at an earlier position or it can be a positive
 * backreference to an already generated chord at an earlier position ("$0" for first chord, "$1" for second chord, etc.). If a random table number is
 * given, a chord is randomly chosen from that table. Chord random tables a comma-separated lists of chord names (e.g., "Am,G,F,Em,Dm"), they are
 * numbered starting from 0. For example, the chord pattern "Am/4,0/4,0!1/4,$1/4" means "A minor for 4 beats, a random chord from random table 0 for 4
 * beats, a random chord from random table 0 but not the same as the one from position 1 for 4 beats and the second chord again for 4 beats
 * " and could result in the chord sequence "Am/4,F/4,G/4,F/4" (given suitable random tables). Normally, each chord pattern is an individual chord
 * section. A pattern can be split into two or more chord sections by using "+" signs directly before a chord/length combination (e.g.,
 * "Am/4,F/4,G/4,C/4,+Am/4,F/4,G/4,Em/4").
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class PatternHarmonyEngine extends AbstractHarmonyEngine {

    /** The array of chords. */
    private Chord[] chords;

    /** The array of ticks per chord. */
    private int[] chordTicks;

    /** The array of chord section ticks. */
    private int[] sectionTicks;

    /** The array of chord patterns. */
    private ChordPattern[] chordPatterns;

    /** The array of chord random tables. */
    private String[][] chordRandomTables;

    /** The Boolean indicating whether chord distances should be minimized by default. Can be overridden per chordPattern tag. */
    private boolean minimizeChordDistance = true;

    /** The default crossover pitch. Can be overridden per chordPattern tag. */
    private int crossoverPitch = 3;

    /** The random generator. */
    private Random random;

    /**
     * Constructor.
     */

    public PatternHarmonyEngine() {
        super();
    }

    @Override
    public Harmony render(final SongContext songContext) {
        parsePattern(songContext);

        return new Harmony() {
            @Override
            public Chord getChord(int tick) {
                if (tick >= 0 && tick < songContext.getStructure().getTicks()) {
                    return chords[tick];
                } else {
                    return null;
                }
            }

            @Override
            public int getChordTicks(int tick) {
                if (tick >= 0 && tick < songContext.getStructure().getTicks()) {
                    return chordTicks[tick];
                } else {
                    return 0;
                }
            }

            @Override
            public int getChordSectionTicks(int tick) {
                if (tick >= 0 && tick < songContext.getStructure().getTicks()) {
                    return sectionTicks[tick];
                } else {
                    return 0;
                }
            }

            @Override
            public Set<String> getTags(int tick) {
                return Collections.emptySet();
            }

            @Override
            public int getTagTicks(int tick) {
                if (tick >= 0 && tick < songContext.getStructure().getTicks()) {
                    return songContext.getStructure().getTicks() - tick;
                } else {
                    return 0;
                }
            }

        };
    }

    /**
     * Parses the chord pattern.
     * 
     * @param songContext the song context
     */

    private void parsePattern(SongContext songContext) {
        Structure structure = songContext.getStructure();

        int ticks = structure.getTicks();

        chords = new Chord[ticks];
        chordTicks = new int[ticks];
        sectionTicks = new int[ticks];

        ChordPattern chordPattern = createPattern();
        String pat = chordPattern.chordPattern;

        // prepend a '+' sign, if not already present

        if (!pat.startsWith("+")) {
            pat = "+" + pat;
        }

        logger.info("Using harmony pattern " + pat);

        String[] c = pat.split(",");

        int tick = 0;
        int pos = 0;
        int sTicks = 0;

        List<Integer> sectionVector = new ArrayList<Integer>();

        Chord firstChord = null;

        while (tick < ticks) {
            String[] p = c[pos % c.length].split("/");

            int len = (int) (Double.parseDouble(p[1]) * structure.getTicksPerBeat() + 0.5d);

            String chordString = p[0];

            if (chordString.startsWith("+")) {
                if (sTicks > 0) {
                    sectionVector.add(sTicks);
                }
                chordString = chordString.substring(1);
                sTicks = 0;
            }

            Chord chord = Chord.parseChord(chordString, getCrossoverPitchValue(chordPattern));

            if (firstChord == null) {
                firstChord = chord;
            }

            if (getMinimizeChordDistanceValue(chordPattern)) {
                chord = chord.findChordClosestTo(firstChord);
            }

            for (int j = 0; j < len && tick < ticks; j++) {
                chordTicks[tick] = tick + len - j >= ticks ? ticks - tick : len - j;
                chords[tick] = chord;
                tick++;
                sTicks++;
            }

            pos++;
        }

        mergeAdjacentChords(songContext);

        sectionVector.add(sTicks);

        logger.debug("Chord sections: " + sectionVector.size());

        tick = 0;
        for (int section = 0; section < sectionVector.size(); section++) {
            int len = sectionVector.get(section);

            for (int i = 0; i < len; i++) {
                sectionTicks[tick] = len - i;
                tick++;
            }
        }
    }

    /**
     * Merges all adjacent equal chords into one chord.
     * 
     * @param songContext the song context
     */

    private void mergeAdjacentChords(SongContext songContext) {
        int ticks = songContext.getStructure().getTicks();
        int tick = 0;

        while (tick < ticks) {
            int t = tick + chordTicks[tick];

            while (t < ticks && chords[t].equals(chords[tick])) {
                t += chordTicks[t];
            }

            if (t != tick + chordTicks[tick]) {
                for (int i = tick; i < t; i++) {
                    chordTicks[i] = t - i;
                }
            }

            tick = t;
        }
    }

    public void setChordPatterns(ChordPattern[] chordPatterns) {
        if (chordPatterns == null || chordPatterns.length == 0) {
            throw new IllegalArgumentException("Need at least 1 chord pattern");
        }

        this.chordPatterns = chordPatterns;
    }

    public void setChordRandomTables(String[][] chordRandomTables) {
        if (chordRandomTables == null) {
            throw new IllegalArgumentException("chordRandomTables must not be null");
        }

        this.chordRandomTables = chordRandomTables;
    }

    /**
     * Creates a pattern that can be parsed using parsePattern().
     * 
     * @return a pattern
     */

    private ChordPattern createPattern() {
        StringBuilder sb = new StringBuilder();

        // choose a chord pattern at random
        ChordPattern chordPattern = chordPatterns[random.nextInt(chordPatterns.length)];
        String[] chords = Pattern.expandPatternString(chordPattern.chordPattern).split(",");

        int count = chords.length;

        String firstChord = null;
        String prevChord = null;

        List<String> chordList = new ArrayList<String>();

        for (int i = 0; i < count; i++) {
            if (sb.length() > 0) {
                sb.append(',');
            }

            String[] spec = chords[i].split("/");

            boolean nextSection;

            if (spec[0].startsWith("+")) {
                nextSection = true;
                spec[0] = spec[0].substring(1);
            } else {
                nextSection = false;
            }

            String chord;

            if (spec[0].indexOf(':') >= 0) {
                chord = spec[0];
            } else if (spec[0].startsWith("$")) {
                int refnum = Integer.parseInt(spec[0].substring(1));

                if (refnum < 0 || refnum >= chordList.size()) {
                    throw new RuntimeException("Invalid back reference $" + refnum);
                }

                chord = chordList.get(refnum);
            } else {
                // check if we have a note or a random table number

                int pitch = NoteUtils.getNotePitch(spec[0].substring(0, 1));

                if (pitch == Integer.MIN_VALUE) {
                    // we have a random chord table number

                    int pos = spec[0].indexOf('!');

                    int table;
                    int notrefnum;

                    if (pos > 0) {
                        table = Integer.parseInt(spec[0].substring(0, pos));
                        notrefnum = Integer.parseInt(spec[0].substring(pos + 1));
                    } else {
                        table = Integer.parseInt(spec[0]);
                        notrefnum = -1;
                    }

                    int num;
                    int it = 0;

                    do {
                        num = random.nextInt(chordRandomTables[table].length);
                        chord = chordRandomTables[table][num];
                        it++;
                        if (it > 1000) {
                            // try again
                            return createPattern();
                        }
                    } while (Chord.parseChord(chord, getCrossoverPitchValue(chordPattern)).equalsNormalized(Chord.parseChord(prevChord,
                            getCrossoverPitchValue(chordPattern))) || i == count - 1 && chord.equals(firstChord) || notrefnum >= 0 && Chord
                                    .parseChord(chord, getCrossoverPitchValue(chordPattern)).equalsNormalized(Chord.parseChord(chordList.get(
                                            notrefnum), getCrossoverPitchValue(chordPattern))));
                } else {
                    // we have a note, take the note (include 'm' suffix, if present)
                    chord = spec[0];
                }
            }

            sb.append(nextSection ? "+" : "").append(chord).append('/').append(spec[1]);
            prevChord = chord;

            if (i == 0) {
                firstChord = chord;
            }

            chordList.add(chord);
        }

        // create a new ChordPattern containing the random chord table replacement result
        return new ChordPattern(sb.toString(), chordPattern.minimizeChordDistance, chordPattern.crossoverPitch);
    }

    @Override
    public void configure(SongContext songContext, Node node) throws XPathException {
        random = new Random(randomSeed);

        try {
            crossoverPitch = XMLUtils.parseInteger(random, "crossoverPitch", node);
        } catch (Exception e) {}

        NodeList nodeList = XMLUtils.getNodeList("chordPattern", node);
        ChordPattern[] chordPatterns = new ChordPattern[nodeList.getLength()];

        for (int i = 0; i < nodeList.getLength(); i++) {
            String pattern = XMLUtils.parseString(random, nodeList.item(i));
            Boolean minimizeChordDistance = null;
            Integer crossoverPitch = null;

            try {
                boolean b = XMLUtils.parseBoolean(random, "@minimizeChordDistance", nodeList.item(i));
                minimizeChordDistance = Boolean.valueOf(b);
            } catch (Exception e) {}

            try {
                int p = XMLUtils.parseInteger(random, "@crossoverPitch", nodeList.item(i));
                crossoverPitch = Integer.valueOf(p);
            } catch (Exception e) {}

            chordPatterns[i] = new ChordPattern(pattern, minimizeChordDistance, crossoverPitch);
        }

        setChordPatterns(chordPatterns);

        nodeList = XMLUtils.getNodeList("chordRandomTable", node);
        String[][] chordRandomTables = new String[nodeList.getLength()][];

        for (int i = 0; i < nodeList.getLength(); i++) {
            String table = XMLUtils.parseString(random, nodeList.item(i));
            chordRandomTables[i] = table.split(",");
        }

        try {
            setMinimizeChordDistance(XMLUtils.parseBoolean(random, "minimizeChordDistance", node));
        } catch (Exception e) {}

        setChordRandomTables(chordRandomTables);
    }

    /**
     * Returns the ChordPattern's minimizeChordDistance value if defined, otherwise the default minimizeChordDistance value is returned.
     * 
     * @param pattern the ChordPattern
     * @return the value
     */
    boolean getMinimizeChordDistanceValue(ChordPattern pattern) {
        if (pattern != null && pattern.minimizeChordDistance != null) {
            return pattern.minimizeChordDistance.booleanValue();
        } else {
            return minimizeChordDistance;
        }
    }

    /**
     * Returns the ChordPattern's crossoverPitch value if defined, otherwise the default crossoverPitch value is returned.
     * 
     * @param pattern the ChordPattern
     * @return the value
     */
    int getCrossoverPitchValue(ChordPattern pattern) {
        if (pattern != null && pattern.crossoverPitch != null) {
            return pattern.crossoverPitch.intValue();
        } else {
            return crossoverPitch;
        }
    }

    public void setMinimizeChordDistance(boolean isMinimizeChordDistance) {
        this.minimizeChordDistance = isMinimizeChordDistance;
    }

    /**
     * Container for chord pattern details.
     */

    private static class ChordPattern {
        /** The chord pattern string. */
        private String chordPattern;

        /** The flag indicating whether chord distances should be minimized (may be null). */
        private Boolean minimizeChordDistance;

        /** The cross-over pitch (may be null). */
        private Integer crossoverPitch;

        /**
         * Constructor.
         * 
         * @param chordPattern the chord pattern
         * @param minimizeChordDistance flag indicating whether chord distances should be minimized
         * @param crossoverPitch the cross-over pitch
         */

        ChordPattern(String chordPattern, Boolean minimizeChordDistance, Integer crossoverPitch) {
            this.chordPattern = chordPattern;
            this.minimizeChordDistance = minimizeChordDistance;
            this.crossoverPitch = crossoverPitch;
        }
    }
}
