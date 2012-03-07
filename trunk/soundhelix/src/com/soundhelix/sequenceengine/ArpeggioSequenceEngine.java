package com.soundhelix.sequenceengine;

import java.util.HashMap;
import java.util.Map;
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
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.patternengine.PatternEngine;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a sequence engine that repeats user-specified patterns. Patterns are comma-separated integers (notes from chords) or minus signs
 * (pauses). For each chord the pattern with the best-matching length is used. The best-matching pattern is the one with the smallest length that is
 * equal to or larger than the required length or the pattern with the largest length if the former doesn't exist. At least some patterns with lengths
 * of powers of two should be provided.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

// TODO: allow specifying velocities in arpeggio patterns (like in the PatternSequenceEngine)

public class ArpeggioSequenceEngine extends AbstractSequenceEngine {
    /** The random generator. */
    protected Random random;

    /** Boolean indicating if chords should be normalized. */
    protected boolean isNormalizeChords = true;

    /** Boolean indicating of chord sections should be obeyed. */
    protected boolean obeyChordSections;

    /** The array of patterns. */
    private Pattern[] patterns;

    public ArpeggioSequenceEngine() {
        super();
    }

    public void setPatterns(Pattern[] patterns) {
        this.patterns = patterns;
    }

    public void setObeyChordSections(boolean obeyChordSections) {
        this.obeyChordSections = obeyChordSections;
    }

    @Override
    public Track render(ActivityVector[] activityVectors) {
        ActivityVector activityVector = activityVectors[0];

        HarmonyEngine harmonyEngine = structure.getHarmonyEngine();

        int tick = 0;

        Sequence seq = new Sequence();
        int ticks = structure.getTicks();

        while (tick < ticks) {
            Chord chord = harmonyEngine.getChord(tick);

            if (isNormalizeChords) {
                chord = chord.normalize();
            }

            int chordTicks;

            if (obeyChordSections) {
                chordTicks = Math.min(harmonyEngine.getChordTicks(tick), harmonyEngine.getChordSectionTicks(tick));
            } else {
                chordTicks = harmonyEngine.getChordTicks(tick);
            }

            Pattern pattern = getArpeggioPattern(chordTicks);
            int patternLength = pattern.size();
            int pos = 0;

            for (int t = 0; t < chordTicks;) {
                Pattern.PatternEntry entry = pattern.get(pos % patternLength);
                int len = entry.getTicks();

                if (t + len > chordTicks) {
                    len = chordTicks - t;
                }

                if (activityVector.isActive(tick)) {
                    short vel = entry.getVelocity();

                    if (entry.isPause()) {
                        // add pause
                        seq.addPause(len);
                    } else {
                        // normal note
                        int value = entry.getPitch();

                        boolean useLegato = entry.isLegato() ? pattern.isLegatoLegal(activityVector, tick + len, pos + 1) : false;

                        seq.addNote(chord.getPitch(value), len, vel, useLegato);
                    }
                } else {
                    // add pause
                    seq.addPause(len);
                }

                t += len;
                tick += len;
                pos++;
            }
        }

        Track track = new Track(TrackType.MELODY);
        track.add(seq);

        return track;
    }

    /**
     * Returns an optimal arpeggio pattern for the given length, based on a best-fit selection. The method returns the shortest pattern that has a
     * length of len or more. If such a pattern doesn't exist, returns the longest pattern shorter than len, which is the longest pattern available.
     * 
     * @param len the length
     * 
     * @return the arpeggio pattern
     */

    private Pattern getArpeggioPattern(int len) {
        // slow implementation, but this method is only called
        // once per chord and we normally don't have a whole lot of patterns

        // might use binary search or caching later

        int bestIndex = -1;
        int bestIndexLen = Integer.MAX_VALUE;
        int maxIndex = -1;
        int maxIndexLen = -1;

        for (int i = 0; i < patterns.length; i++) {
            int l = patterns[i].getTicks();

            if (l >= len && l < bestIndexLen) {
                bestIndex = i;
                bestIndexLen = l;
            } else if (l >= maxIndexLen) {
                maxIndex = i;
                maxIndexLen = l;
            }
        }

        if (bestIndex != -1) {
            return patterns[bestIndex];
        } else {
            // we haven't found an optimal pattern
            // use the longest one we've found
            return patterns[maxIndex];
        }
    }

    @Override
    public void configure(Node node, XPath xpath) throws XPathException {
        Random random = new Random(randomSeed);

        NodeList patternEnginesNodes = (NodeList) xpath.evaluate("patternEngines", node, XPathConstants.NODESET);

        int patternEnginesCount = patternEnginesNodes.getLength();

        if (patternEnginesCount == 0) {
            throw new RuntimeException("Need at least 1 patternEngines tag");
        }

        Node patternEnginesNode = patternEnginesNodes.item(random.nextInt(patternEnginesCount));

        NodeList nodeList = (NodeList) xpath.evaluate("patternEngine", patternEnginesNode, XPathConstants.NODESET);

        if (nodeList.getLength() == 0) {
            throw new RuntimeException("Need at least 1 patternEngine");
        }

        try {
            setNormalizeChords(!XMLUtils.parseBoolean(random, "obeyChordSubtype", node, xpath));
            logger.warn("The tag \"obeyChordSubtype\" has been deprecated. Use \"normalizeChords\" with inverted value instead.");
        } catch (Exception e) {
        }

        try {
            setNormalizeChords(XMLUtils.parseBoolean(random, "normalizeChords", node, xpath));
        } catch (Exception e) {
        }

        try {
            setObeyChordSections(XMLUtils.parseBoolean(random, "obeyChordSections", node, xpath));
        } catch (Exception e) {
        }

        int patternEngineCount = nodeList.getLength();

        Map<Integer, Boolean> patternLengthMap = new HashMap<Integer, Boolean>(patternEngineCount);

        Pattern[] patterns = new Pattern[patternEngineCount];

        for (int i = 0; i < patternEngineCount; i++) {
            PatternEngine patternEngine;

            try {
                patternEngine = XMLUtils.getInstance(PatternEngine.class, nodeList.item(i), xpath, randomSeed, i);
            } catch (Exception e) {
                throw new RuntimeException("Error instantiating PatternEngine", e);
            }

            patterns[i] = patternEngine.render("");
            int ticks = patterns[i].getTicks();

            if (patternLengthMap.containsKey(ticks)) {
                throw new RuntimeException("Another pattern with " + ticks + " ticks was already provided");
            }

            patternLengthMap.put(ticks, true);
        }

        setPatterns(patterns);
    }

    public void setNormalizeChords(boolean isNormalizeChords) {
        this.isNormalizeChords = isNormalizeChords;
    }
}
