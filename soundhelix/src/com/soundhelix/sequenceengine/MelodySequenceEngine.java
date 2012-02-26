package com.soundhelix.sequenceengine;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.harmonyengine.HarmonyEngine;
import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.Pattern.PatternEntry;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.patternengine.PatternEngine;
import com.soundhelix.util.HarmonyEngineUtils;
import com.soundhelix.util.NoteUtils;
import com.soundhelix.util.RandomUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a sequence engine that uses a randomly generated melody, played with a given rhythmic pattern. For each
 * distinct chord section, a melody is generated and used for each occurrence of the chord section.
 *
 * @author Thomas Schürger (thomas@schuerger.com)
 */

// TODO: add proper configurability

public class MelodySequenceEngine extends AbstractSequenceEngine {

    /** Wildcard for pitch on chord. */
    private static final char ON_CHORD = '#';

    /** Wildcard for free pitch. */
    private static final char FREE = '+';
    
    /** Wildcard for repeated pitch. */
    private static final char REPEAT = '*';

    /** The minimum pitch to use. */
    private int minPitch = -3;

    /** The maximum pitch to use. */
    private int maxPitch = 12;

    /** The input pattern for melodies. */
    private Pattern pattern;

    /** The pitch distances. */
    private int[] pitchDistances = new int[] {-2, -1, 0, 1, 2};
    
    /** The random generator. */
    private Random random;
    
    public MelodySequenceEngine() {
        super();
    }

    @Override
    public Track render(ActivityVector[] activityVectors) {
        ActivityVector activityVector = activityVectors[0];

        Sequence seq = new Sequence();
        HarmonyEngine harmonyEngine = structure.getHarmonyEngine();
        
        int tick = 0;
        int ticks = structure.getTicks();

        Map<String, Pattern> melodyHashMap = createMelodies();

        while (tick < ticks) {
            int len = harmonyEngine.getChordSectionTicks(tick);
            Pattern p = melodyHashMap.get(HarmonyEngineUtils.getChordSectionString(structure, tick));

            int pos = 0;
            int tickEnd = tick + len;

            while (tick < tickEnd) {
                PatternEntry entry = p.get(pos % p.size());
                int l = entry.getTicks();

                if (tick + l > tickEnd) {
                    l = tickEnd - tick;
                }

                if (activityVector.isActive(tick)) {    
                    if (entry.isPause()) {
                        seq.addPause(l);
                    } else {
                        seq.addNote(entry.getPitch(), l, entry.getVelocity(), entry.isLegato());
                    }
                } else {
                    seq.addPause(l);
                }

                pos++;
                tick += l;
            }
        }
        
        Track track = new Track(TrackType.MELODY);
        track.add(seq);
        return track;
    }
    
    /**
     * Returns a random pitch which is near the given pitch and on the C/Am scale.
     * 
     * @param pitch the starting pitch
     * 
     * @return the random pitch
     */
    
    private int getRandomPitch(int pitch) {
        int p;
        boolean again;
        
        int iterations = 10000;
        
        do {
            again = false;
            p = pitch;
            
            int distance = pitchDistances[random.nextInt(pitchDistances.length)];
            p += distance;
            
            if (distance > 0) {
                // if p is not on the C/Am scale, move up until it is
                while (!NoteUtils.isOnScale(p)) {
                    p++;
                }            
            } else if (distance < 0) {
                // if p is not on the C/Am scale, move down until it is
                while (!NoteUtils.isOnScale(p)) {
                    p--;
                }
            } else {
                // don't move, but check if pitch is on the C/Am scale
                if (!NoteUtils.isOnScale(p)) {
                    // pitch has to be changed, because it is invalid
                    // we must go up or down, so we'll retry                
                    again = true;
                }
            }
        } while (--iterations > 0 && (again || p < minPitch || p > maxPitch));
        
        if (iterations == 0) {
            throw new RuntimeException("Wider range between minPitch and maxPitch required");
        }

        return p;
    }

    /**
     * Returns a random pitch which is near the given pitch and is one of the given chords notes.
     * 
     * @param chord the chord
     * @param pitch the starting pitch
     * 
     * @return the random pitch
     */
    
    private int getRandomPitch(Chord chord, int pitch) {
        int p;
        boolean again;
        
        int iterations = 10000;
        
        do {
            again = false;
            p = pitch;
            
            int distance = pitchDistances[random.nextInt(pitchDistances.length)];
            p += distance;
            
            if (distance > 0) {
                // if p is not a chord pitch, move up until it is
                while (!chord.containsPitch(p)) {
                    p++;
                }            
            } else if (distance < 0) {
                // if p is not a chord pitch, move down until it is
                while (!chord.containsPitch(p)) {
                    p--;
                }
            } else {
                // don't move, but check if pitch is a chord pitch
                if (!chord.containsPitch(p)) {
                    // pitch has to be changed, because it is invalid
                    // we must go up or down, so we'll retry                
                    again = true;
                }
            }
        } while (--iterations > 0 && (again || p < minPitch || p > maxPitch));
        
        if (iterations == 0) {
            throw new RuntimeException("Wider range between minPitch and maxPitch required");
        }

        return p;
    }

    /**
     * Creates a melody for each distinct chord section and returns a map mapping chord section strings to
     * melody patterns.
     * 
     * @return a map mapping chord section strings to melody patterns
     */
    
    private Map<String, Pattern> createMelodies() {
        HarmonyEngine he = structure.getHarmonyEngine();
        
        int patternLength = pattern.size();
        
        Map<String, Pattern> ht = new HashMap<String, Pattern>();
        
        int ticks = structure.getTicks();
        int tick = 0;
        int pos = 0;
        
        while (tick < ticks) {
            String section = HarmonyEngineUtils.getChordSectionString(structure, tick);
            int len = he.getChordSectionTicks(tick);
            
            if (!ht.containsKey(section)) {
                // no melody created yet; create one
                List<PatternEntry> list = new ArrayList<PatternEntry>();                
                
                int previousPitch = Integer.MIN_VALUE;

                // choose random start pitch in the range
                int pitch = minPitch + random.nextInt(maxPitch - minPitch + 1);
                             
                for (int i = 0; i < len;) {
                    PatternEntry entry = pattern.get(pos % patternLength);
                    Chord chord = he.getChord(tick + i);
                    int t = entry.getTicks();
                    
                    if (entry.isPause()) {
                        list.add(new PatternEntry(t));
                    } else if (entry.isWildcard() && entry.getWildcardCharacter() == FREE) {
                        pitch = getRandomPitch(pitch);
                        list.add(new PatternEntry(pitch, entry.getVelocity(), t, entry.isLegato()));
                    } else if (entry.isWildcard() && entry.getWildcardCharacter() == REPEAT
                               && previousPitch != Integer.MIN_VALUE && chord.containsPitch(pitch)) {
                        // reuse the previous pitch
                        list.add(new PatternEntry(pitch, entry.getVelocity(), t, entry.isLegato()));
                    } else if (!entry.isWildcard() || entry.getWildcardCharacter() == ON_CHORD) {
                        // TODO: only allow ON_CHORD wildcard, not normal offsets
                        pitch = getRandomPitch(chord, pitch);
                        list.add(new PatternEntry(pitch, entry.getVelocity(), t, entry.isLegato()));
                    }
                    
                    pos++;
                    i += t;
                    
                    previousPitch = pitch;
                }
   
                ht.put(section, new Pattern(list.toArray(new PatternEntry[list.size()])));
            }
            
            tick += len;
        }
        
        return ht;
    }
    
    @Override
    public void configure(Node node, XPath xpath) throws XPathException {
        random = new Random(randomSeed);
        
        try {
            setPitchDistances(XMLUtils.parseIntegerListString(random, "pitchDistances", node, xpath));
        } catch (Exception e) {
        }

        try {
            setMinPitch(XMLUtils.parseInteger(random, (Node) xpath.evaluate("minPitch", node, XPathConstants.NODE),
                    xpath));
        } catch (Exception e) {
        }

        try {
            setMaxPitch(XMLUtils.parseInteger(random, (Node) xpath.evaluate("maxPitch", node, XPathConstants.NODE),
                    xpath));
        } catch (Exception e) {
        }
        
        if (maxPitch - minPitch < 5) {
            throw new RuntimeException("minPitch and maxPitch must be at least 5 halftones apart");
        }
        
        NodeList nodeList = (NodeList) xpath.evaluate("patternEngine", node, XPathConstants.NODESET);

        if (nodeList.getLength() == 0) {
            throw new RuntimeException("Need at least one pattern engine");
        }
        
        PatternEngine patternEngine;
        
        try {
            int i = random.nextInt(nodeList.getLength());
            patternEngine = XMLUtils.getInstance(PatternEngine.class, nodeList.item(i), xpath, randomSeed, i);
        } catch (Exception e) {
            throw new RuntimeException("Error instantiating PatternEngine", e);
        }
        
        Pattern pattern = patternEngine.render("" + ON_CHORD + FREE + REPEAT);
        setPattern(pattern);
    }
    
    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public int getMinPitch() {
        return minPitch;
    }

    public void setMinPitch(int minPitch) {
        this.minPitch = minPitch;
    }

    public int getMaxPitch() {
        return maxPitch;
    }

    public void setMaxPitch(int maxPitch) {
        this.maxPitch = maxPitch;
    }

    public int[] getPitchDistances() {
        return pitchDistances;
    }

    public void setPitchDistances(int[] pitchDistances) {
        this.pitchDistances = pitchDistances;
    }
}
