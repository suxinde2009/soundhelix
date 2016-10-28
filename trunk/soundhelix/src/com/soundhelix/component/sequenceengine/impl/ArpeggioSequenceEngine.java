package com.soundhelix.component.sequenceengine.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.component.patternengine.PatternEngine;
import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Harmony;
import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.SongContext;
import com.soundhelix.misc.Structure;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a sequence engine that repeats user-specified patterns. Patterns are comma-separated integers (notes from chords) or minus signs
 * (pauses). For each chord the pattern with the best-matching length is used. The best-matching pattern is the one with the smallest length that is
 * equal to or larger than the required length or the pattern with the largest length if the former doesn't exist. At least some patterns with lengths
 * of powers of two should be provided.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class ArpeggioSequenceEngine extends AbstractSequenceEngine {
    /** The random generator. */
    protected Random random;

    /** Boolean indicating if chords should be normalized. */
    protected boolean isNormalizeChords = true;

    /** Boolean indicating of chord sections should be obeyed. */
    protected boolean obeyChordSections;

    /** The array of patterns. */
    private Pattern[] patterns;

    /**
     * Constructor.
     */

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
    public Track render(SongContext songContext, ActivityVector[] activityVectors) {
        Structure structure = songContext.getStructure();
        Harmony harmony = songContext.getHarmony();

        ActivityVector activityVector = activityVectors[0];

        int tick = 0;

        Sequence seq = new Sequence(songContext);
        int ticks = structure.getTicks();

        while (tick < ticks) {
            Chord chord = harmony.getChord(tick);

            if (isNormalizeChords) {
                chord = chord.normalize();
            }

            int chordTicks;

            if (obeyChordSections) {
                chordTicks = Math.min(harmony.getChordTicks(tick), harmony.getChordSectionTicks(tick));
            } else {
                chordTicks = harmony.getChordTicks(tick);
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
                    int velocity = entry.getVelocity();

                    if (entry.isPause()) {
                        // add pause
                        seq.addPause(len);
                    } else {
                        // normal note
                        int value = entry.getPitch();

                        boolean useLegato = entry.isLegato() ? pattern.isLegatoLegal(activityVector, tick + len, pos + 1) : false;

                        seq.addNote(chord.getPitch(value), len, velocity, useLegato);
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

        Track track = new Track(TrackType.MELODIC);
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
    public void configure(SongContext songContext, Node node) throws XPathException {
        Random random = new Random(randomSeed);

        NodeList patternEnginesNodes = XMLUtils.getNodeList("patternEngines", node);

        int patternEnginesCount = patternEnginesNodes.getLength();

        if (patternEnginesCount == 0) {
            throw new RuntimeException("Need at least 1 patternEngines tag");
        }

        Node patternEnginesNode = patternEnginesNodes.item(random.nextInt(patternEnginesCount));

        NodeList nodeList = XMLUtils.getNodeList("patternEngine", patternEnginesNode);

        if (nodeList.getLength() == 0) {
            throw new RuntimeException("Need at least 1 patternEngine");
        }

        try {
            setNormalizeChords(XMLUtils.parseBoolean(random, "normalizeChords", node));
        } catch (Exception e) {}

        try {
            setObeyChordSections(XMLUtils.parseBoolean(random, "obeyChordSections", node));
        } catch (Exception e) {}

        int patternEngineCount = nodeList.getLength();

        Map<Integer, Boolean> patternLengthMap = new HashMap<Integer, Boolean>(patternEngineCount);

        Pattern[] patterns = new Pattern[patternEngineCount];

        for (int i = 0; i < patternEngineCount; i++) {
            PatternEngine patternEngine;

            try {
                patternEngine = XMLUtils.getInstance(songContext, PatternEngine.class, nodeList.item(i), randomSeed, i);
            } catch (Exception e) {
                throw new RuntimeException("Error instantiating PatternEngine", e);
            }

            patterns[i] = patternEngine.render(songContext, "");
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
