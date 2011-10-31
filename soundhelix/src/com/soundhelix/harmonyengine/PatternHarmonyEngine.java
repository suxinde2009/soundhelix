package com.soundhelix.harmonyengine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.misc.Chord;
import com.soundhelix.util.NoteUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a flexible HarmonyEngine based on user-specified patterns. The patterns consist
 * of chord patterns and random table patterns. One of the given
 * chord patterns is chosen at random. A chord pattern consists of comma-separated combinations of chords
 * and lengths in beats (separated by a slash). A chord is either a simple chord name ("C" for C major, "Am" for A
 * minor, etc.), a random table number (starting from 0), a random table number (starting from 0) with a negative
 * backreference (e.g., "0!0") to an already generated chord at an earlier position or it can be a positive
 * backreference to an already generated chord at an earlier position ("$0" for first chord, "$1" for second chord,
 * etc.). If a random table number is given, a chord is randomly chosen from that table. Chord random tables a
 * comma-separated lists of chord names (e.g., "Am,G,F,Em,Dm"), they are numbered starting from 0. For example, the
 * chord pattern "Am/4,0/4,0!1/4,$1/4" means "A minor for 4 beats, a random chord from random table 0 for 4 beats, a
 * random chord from random table 0 but not the same as the one from position 1 for 4 beats and the second chord again
 * for 4 beats" and could result in the chord sequence "Am/4,F/4,G/4,F/4" (given suitable random tables). Normally,
 * each chord pattern is an individual chord section. A pattern can be split into two or more chord sections by using
 * "+" signs directly before a chord/length combination (e.g., "Am/4,F/4,G/4,C/4,+Am/4,F/4,G/4,Em/4").
 *
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class PatternHarmonyEngine extends AbstractHarmonyEngine {
    private Chord[] chords;
    private int[] chordTicks;
    private int[] sectionTicks;
    
    private ChordPattern[] chordPatterns;
    private String[][] chordRandomTables;
    
    /** Boolean indicating whether chord distances should be minimized. */
    private boolean isMinimizeChordDistance = true;
    
    /** The random generator. */
    private Random random;
    
    public PatternHarmonyEngine() {
        super();
    }
    
    public Chord getChord(int tick) {
        if (chords == null) {
            parsePattern();
        }
        
        if (tick >= 0 && tick < structure.getTicks()) {
            return chords[tick];
        } else {
            return null;
        }
    }

    public int getChordTicks(int tick) {
        if (chords == null) {
            parsePattern();
        }

        if (tick >= 0 && tick < structure.getTicks()) {
            return chordTicks[tick];
        } else {
            return 0;
        }
    }

    public int getChordSectionTicks(int tick) {
        if (chords == null) {
            parsePattern();
        }

        if (tick >= 0 && tick < structure.getTicks()) {
            return sectionTicks[tick];
        } else {
            return 0;
        }        
    }
    
    private void parsePattern() {
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
        
        logger.debug("Using harmony pattern " + pat);

        String[] c = pat.split(",");
        
        int tick = 0;
        int pos = 0;
        int sTicks = 0;
        
        List<Integer> sectionVector = new ArrayList<Integer>();
        
        Chord firstChord = null;
        Chord previousChord = null;
        
        while (tick < ticks) {
            String[] p = c[pos % c.length].split("/");

            int len = (int) (Double.parseDouble(p[1]) * (double) structure.getTicksPerBeat() + 0.5d);
            
            String chordString = p[0];
            
            if (chordString.startsWith("+")) {
                if (sTicks > 0) {
                    sectionVector.add(sTicks);
                }
                chordString = chordString.substring(1);
                sTicks = 0;
            }
            
            Chord chord = Chord.parseChord(chordString);
                            
            if (firstChord == null) {
                firstChord = chord;
            }

            // use override for minimizeChordDistance, if provided, otherwise use global minimizeChordDistance value
            
            if (chordPattern.minimizeChordDistance == 1
                    || chordPattern.minimizeChordDistance == -1 && isMinimizeChordDistance) {
                chord = chord.findChordClosestTo(firstChord);
            }
            
            for (int j = 0; j < len && tick < ticks; j++) {
                chordTicks[tick] = tick + len - j >= ticks ? ticks - tick : len - j;
                chords[tick] = chord;
                tick++;
                sTicks++;
            }
            
            previousChord = chord;            
            pos++;
        }

        mergeAdjacentChords();        

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
        
        checkSanity();
    }

    /**
     * Merges all adjacent equal chords into one chord.
     */
    
    private void mergeAdjacentChords() {
        int ticks = structure.getTicks();
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
        String[] chords = chordPattern.chordPattern.split(",");
        
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
                    } while(Chord.parseChord(chord).equalsNormalized(Chord.parseChord(prevChord)) || i == count - 1 && chord.equals(firstChord)
                            || notrefnum >= 0 && Chord.parseChord(chord).equalsNormalized(Chord.parseChord(chordList.get(notrefnum))));
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
        return new ChordPattern(sb.toString(), chordPattern.minimizeChordDistance);
    }
    
    public void configure(Node node, XPath xpath) throws XPathException {        
        random = new Random(randomSeed);

        NodeList nodeList = (NodeList) xpath.evaluate("chordPattern", node, XPathConstants.NODESET);
        ChordPattern[] chordPatterns = new ChordPattern[nodeList.getLength()];
        
        for (int i = 0; i < nodeList.getLength(); i++) {
            String pattern =  XMLUtils.parseString(random, nodeList.item(i), xpath);
            int minimizeChordDistance = -1;
            
            try {
                boolean b =  XMLUtils.parseBoolean(random, "attribute::minimizeChordDistance", nodeList.item(i), xpath);
                minimizeChordDistance = b ? 1 : 0;
            } catch (Exception e) {}
            
            chordPatterns[i] = new ChordPattern(pattern, minimizeChordDistance);
        }

        setChordPatterns(chordPatterns);

        nodeList = (NodeList) xpath.evaluate("chordRandomTable", node, XPathConstants.NODESET);
        String[][] chordRandomTables = new String[nodeList.getLength()][];
        
        for (int i = 0; i < nodeList.getLength(); i++) {
            String table = XMLUtils.parseString(random, nodeList.item(i), xpath);
            chordRandomTables[i] = table.split(",");
        }

        try {
            setMinimizeChordDistance(XMLUtils.parseBoolean(random, "minimizeChordDistance", node, xpath));
        } catch (Exception e) {}

        setChordRandomTables(chordRandomTables);
    }

    public void setMinimizeChordDistance(boolean isMinimizeChordDistance) {
        this.isMinimizeChordDistance = isMinimizeChordDistance;
    }
    
    private class ChordPattern {
        private String chordPattern;
        private int minimizeChordDistance;
        
        public ChordPattern(String chordPattern, int minimizeChordDistance) {
            if (minimizeChordDistance < -1 || minimizeChordDistance > 1) {
                throw new RuntimeException("Invalid value " + minimizeChordDistance + " for minimizeChordDistance");
            }
            
            this.chordPattern = chordPattern;
            this.minimizeChordDistance = minimizeChordDistance;
        }
    }
}
