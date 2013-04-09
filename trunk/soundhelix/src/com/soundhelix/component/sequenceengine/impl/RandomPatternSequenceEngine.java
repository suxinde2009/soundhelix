package com.soundhelix.component.sequenceengine.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.xpath.XPathException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.soundhelix.misc.ActivityVector;
import com.soundhelix.misc.Chord;
import com.soundhelix.misc.Harmony;
import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.Pattern.PatternEntry;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.SongContext;
import com.soundhelix.misc.Structure;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.util.HarmonyUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a sequence engine that plays random notes of the current chord from a list of possible offsets with a given rhythmic pattern. For each
 * distinct chord section, a set of random notes is generated and used for each occurrence of the chord section.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class RandomPatternSequenceEngine extends AbstractSequenceEngine {
    private static String defaultPatternString = "0";
    private Pattern pattern;
    private int patternLength;
    private int[] offsets;

    private Random random;

    public RandomPatternSequenceEngine() {
        this(defaultPatternString);
    }

    public RandomPatternSequenceEngine(String patternString) {
        super();
        logger.warn("Class RandomPatternSequenceEngine is deprecated and will be removed. Please consider using "
                + "RandomPatternEngine or RandomFragmentPatternEngine with another SequenceEngine instead");
        setPattern(patternString);
    }

    @Override
    public Track render(SongContext songContext, ActivityVector[] activityVectors) {
        Structure structure = songContext.getStructure();
        Harmony harmony = songContext.getHarmony();
        
        ActivityVector activityVector = activityVectors[0];

        Sequence seq = new Sequence();

        int tick = 0;
        int ticks = structure.getTicks();

        Map<String, Pattern> melodyHashtable = createMelodies(songContext);

        while (tick < ticks) {
            int len = harmony.getChordSectionTicks(tick);
            Pattern p = melodyHashtable.get(HarmonyUtils.getChordSectionString(songContext, tick));
            int pos = 0;

            for (int i = 0; i < len;) {
                PatternEntry entry = p.get(pos);
                int l = entry.getTicks();

                if (activityVector.isActive(tick + i)) {
                    if (entry.isPause()) {
                        // add pause
                        seq.addPause(l);
                    } else {
                        seq.addNote(entry.getPitch(), l, entry.getVelocity(), entry.isLegato());
                    }
                } else {
                    // add pause
                    seq.addPause(l);
                }

                pos++;
                i += l;
            }

            tick += len;
        }

        Track track = new Track(TrackType.MELODIC);
        track.add(seq);
        return track;
    }

    /**
     * Creates a pitch pattern for each distinct chord section and returns a hashtable mapping chord section strings to pitch patterns.
     * 
     * @return a hashtable mapping chord section strings to pitch patterns
     */

    private Map<String, Pattern> createMelodies(SongContext songContext) {
        Structure structure = songContext.getStructure();
        Harmony harmony = songContext.getHarmony();

        Map<String, Pattern> sectionMap = new HashMap<String, Pattern>();

        int tick = 0;

        while (tick < structure.getTicks()) {
            String section = HarmonyUtils.getChordSectionString(songContext, tick);
            int len = harmony.getChordSectionTicks(tick);

            if (!sectionMap.containsKey(section)) {
                // no pattern created yet; create one
                List<PatternEntry> list = new ArrayList<PatternEntry>();

                int lastValue = Integer.MIN_VALUE;
                int pos = 0;

                for (int i = 0; i < len;) {
                    PatternEntry entry = pattern.get(pos % patternLength);
                    Chord chord = harmony.getChord(tick + i);

                    int t = entry.getTicks();

                    if (entry.isPause()) {
                        list.add(new PatternEntry(t));
                    } else {
                        int value;

                        do {
                            value = offsets[random.nextInt(offsets.length)];
                        } while (value == lastValue);

                        list.add(new PatternEntry(chord.getPitch(value), entry.getVelocity(), t, entry.isLegato()));

                        lastValue = value;
                    }

                    pos++;
                    i += t;
                }

                sectionMap.put(section, new Pattern(list.toArray(new PatternEntry[list.size()])));
            }

            tick += len;
        }

        return sectionMap;
    }

    @Override
    public void configure(SongContext songContext, Node node) throws XPathException {
        random = new Random(randomSeed);

        NodeList nodeList = XMLUtils.getNodeList("pattern", node);
        setPattern(XMLUtils.parseString(random, nodeList.item(random.nextInt(nodeList.getLength()))));

        String offsetString = XMLUtils.parseString(random, "offsets", node);

        if (offsetString == null || offsetString.equals("")) {
            offsetString = "0,1,2";
        }

        String[] offsetList = offsetString.split(",");

        int[] offsets = new int[offsetList.length];

        for (int i = 0; i < offsetList.length; i++) {
            offsets[i] = Integer.parseInt(offsetList[i]);
        }

        setOffsets(offsets);
    }

    public void setPattern(String patternString) {
        this.pattern = Pattern.parseString(patternString, "");
        this.patternLength = pattern.size();
    }

    public void setOffsets(int[] offsets) {
        this.offsets = offsets;
    }
}
