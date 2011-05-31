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
import com.soundhelix.misc.Chord.ChordSubtype;
import com.soundhelix.misc.Pattern;
import com.soundhelix.misc.Pattern.PatternEntry;
import com.soundhelix.misc.Sequence;
import com.soundhelix.misc.Track;
import com.soundhelix.misc.Track.TrackType;
import com.soundhelix.util.HarmonyEngineUtils;
import com.soundhelix.util.XMLUtils;

/**
 * Implements a sequence engine that plays random notes of the current chord
 * from a list of possible offsets with a given rhythmic pattern. For each
 * distinct chord section, a set of random notes is generated and used for
 * each occurrence of the chord section.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class RandomPatternSequenceEngine extends AbstractSequenceEngine {    
    private static final int[] MAJOR_TABLE = new int[] {0, 4, 7};
    private static final int[] MINOR_TABLE = new int[] {0, 3, 7};

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

    public Track render(ActivityVector[] activityVectors) {
        ActivityVector activityVector = activityVectors[0];

        Sequence seq = new Sequence();
        HarmonyEngine harmonyEngine = structure.getHarmonyEngine();
        
        int tick = 0;
        int ticks = structure.getTicks();
        
        Map<String, Pattern> melodyHashtable = createMelodies();
        
        while (tick < ticks) {
            int len = harmonyEngine.getChordSectionTicks(tick);
            Pattern p = melodyHashtable.get(HarmonyEngineUtils.getChordSectionString(structure, tick));
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
        
        Track track = new Track(TrackType.MELODY);
        track.add(seq);
        return track;
    }

    /**
     * Creates a pitch pattern for each distinct chord section and
     * returns a hashtable mapping chord section strings to
     * pitch patterns.
     * 
     * @return a hashtable mapping chord section strings to pitch patterns
     */
    
    private Map<String, Pattern> createMelodies() {
        HarmonyEngine he = structure.getHarmonyEngine();
        
        Map<String, Pattern> sectionMap = new HashMap<String, Pattern>();
        
        int tick = 0;
        
        while (tick < structure.getTicks()) {
            String section = HarmonyEngineUtils.getChordSectionString(structure, tick);
            int len = he.getChordSectionTicks(tick);
            
            if (!sectionMap.containsKey(section)) {
                // no pattern created yet; create one
                List<PatternEntry> list = new ArrayList<PatternEntry>();                
                
                int lastValue = Integer.MIN_VALUE;
                int pos = 0;
                
                for (int i = 0; i < len;) {
                    PatternEntry entry = pattern.get(pos % patternLength);
                    Chord chord = he.getChord(tick + i);
                    int t = entry.getTicks();
                    
                    if (entry.isPause()) {
                        list.add(new PatternEntry(t));
                    } else {
                        int value;
                        
                        do {
                            value = offsets[random.nextInt(offsets.length)];
                        } while(value == lastValue);

                        if (true) {
                            if (chord.getSubtype() == ChordSubtype.BASE_4) {
                                value++;
                            } else if (chord.getSubtype() == ChordSubtype.BASE_6) {
                                value--;
                            }
                        }
                        
                        int octave = value >= 0 ? value / 3 : (value - 2) / 3;
                        int offset = ((value % 3) + 3) % 3;
                        
                         if (chord.isMajor()) {
                            list.add(new PatternEntry(octave * 12 + MAJOR_TABLE[offset] + chord.getPitch(),
                                                      entry.getVelocity(), t, entry.isLegato()));
                        } else {
                            list.add(new PatternEntry(octave * 12 + MINOR_TABLE[offset] + chord.getPitch(),
                                                      entry.getVelocity(), t, entry.isLegato()));
                        }
                        
                        lastValue = value;
                    }
                    
                    pos++;
                    i += t;
                }
   
                sectionMap.put(section, new Pattern(list.toArray(new PatternEntry[list.size()])));
            } else {
                // melody already created, skip chord section
            }
            
            tick += len;
        }
        
        return sectionMap;
    }
    
    public void configure(Node node, XPath xpath) throws XPathException {
        random = new Random(randomSeed);
        
        NodeList nodeList = (NodeList) xpath.evaluate("pattern", node, XPathConstants.NODESET);
        setPattern(XMLUtils.parseString(random, nodeList.item(random.nextInt(nodeList.getLength())), xpath));
        
        String offsetString = XMLUtils.parseString(random, "offsets", node, xpath);
        
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
