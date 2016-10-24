package com.soundhelix.misc;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;

import com.soundhelix.misc.Pattern.PatternEntry;
import com.soundhelix.util.EuclideanRhythmGenerator;

/**
 * Represents a pattern, which is like a repeatable Sequence, but is immutable and allows particular notes (or offsets) as well as wildcards used for
 * special purposes. Patterns can be created from pattern strings.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class Pattern implements Iterable<PatternEntry> {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(new Throwable().getStackTrace()[0].getClassName());

    /** The pattern for matching groups. */
    private static final java.util.regex.Pattern GROUP_PATTERN = java.util.regex.Pattern.compile("\\(([^()]+)\\)([*+\\-])(\\d+)");

    /** The pattern for matching groups. */
    private static final java.util.regex.Pattern GROUP_CHAR_PATTERN = java.util.regex.Pattern.compile("[()]");

    /** The pattern for matching functions. */
    private static final java.util.regex.Pattern FUNCTION_PATTERN = java.util.regex.Pattern.compile("([0-9A-Za-z]+)\\((([^,)]*,)*[^,)]*)\\)");

    /** The pattern for matching integers with optional sign. */
    private static final java.util.regex.Pattern TRANSPOSITION_PATTERN = java.util.regex.Pattern.compile("(?<=^|,)(-?\\d+)");

    /** The PatternEntry array. */
    private PatternEntry[] pattern;

    /** The total number of ticks. */
    private int totalTicks;

    /**
     * Private constructor.
     */

    @SuppressWarnings("unused")
    private Pattern() {
    }

    /**
     * Constructor.
     * 
     * @param pattern the pattern
     */

    public Pattern(PatternEntry[] pattern) {
        this.pattern = pattern;

        int ticks = 0;

        for (int i = 0; i < pattern.length; i++) {
            ticks += pattern[i].getTicks();
        }

        this.totalTicks = ticks;
    }

    /**
     * Parses the given pattern string using no wildcards.
     * 
     * @param songContext the song context
     * @param patternString the pattern string
     * 
     * @return the pattern
     */

    @SuppressWarnings("unused")
    private static Pattern parseString(SongContext songContext, String patternString) {
        return parseString(songContext, patternString, null, songContext.getStructure().getTicksPerBeat());
    }

    /**
     * Parses the given pattern string using no wildcards.
     * 
     * @param songContext the song context
     * @param patternString the pattern string
     * @param targetTPB the ticks per beat the pattern is for
     * 
     * @return the pattern
     */

    public static Pattern parseString(SongContext songContext, String patternString, int targetTPB) {
        return parseString(songContext, patternString, null, targetTPB);
    }

    /**
     * Parses the given pattern string using the given wildcard string.
     * 
     * @param songContext the song context
     * @param patternString the pattern string
     * @param wildcardString the wildcard string
     * 
     * @return the pattern
     */

    @SuppressWarnings("unused")
    private static Pattern parseString(SongContext songContext, String patternString, String wildcardString) {
        return parseString(songContext, patternString, wildcardString, 4);
    }

    /**
     * Parses the given pattern string using the given wildcard string.
     * 
     * @param songContext the song context
     * @param patternString the pattern string
     * @param wildcardString the wildcard string
     * @param targetTPB the ticks per beat the pattern is for
     * 
     * @return the pattern
     */

    public static Pattern parseString(SongContext songContext, String patternString, String wildcardString, int targetTPB) {
        patternString = expand(patternString);

        if (patternString == null || patternString.equals("")) {
            return null;
        }

        if (wildcardString == null) {
            wildcardString = "";
        }

        int currentTPB = songContext.getStructure().getTicksPerBeat();

        if (targetTPB == -1) {
            // -1 means no tick scaling
            targetTPB = currentTPB;
        }

        PatternEntry[] pattern;

        String[] p = patternString.split(",");
        int len = p.length;

        pattern = new PatternEntry[len];

        // format: offset/ticks:velocity or offset~/ticks:velocity

        for (int i = 0; i < len; i++) {
            String[] a = p[i].split(":");
            int v = a.length > 1 ? Integer.parseInt(a[1]) : songContext.getStructure().getMaxVelocity();
            String[] b = a[0].split("/");
            int t = b.length > 1 ? Integer.parseInt(b[1]) : 1;

            if (t * currentTPB % targetTPB != 0) {
                throw new RuntimeException("Tick value " + t + " in pattern \"" + patternString + "\" can't be scaled by a ratio of " + currentTPB
                        + ":" + targetTPB);
            } else {
                t = t * currentTPB / targetTPB;
            }

            boolean legato = b[0].endsWith("~");

            if (legato) {
                // cut off legato character
                b[0] = b[0].substring(0, b[0].length() - 1);
            }

            if (b[0].equals("-")) {
                pattern[i] = new Pattern.PatternEntry(t);
            } else if (b[0].length() == 1 && wildcardString.indexOf(b[0]) >= 0) {
                pattern[i] = new PatternEntry(b[0].charAt(0), v, t, legato);
            } else {
                pattern[i] = new PatternEntry(Integer.parseInt(b[0]), v, t, legato);
            }
        }

        return new Pattern(pattern);
    }

    /**
     * Expands the given pattern string until no more expansion is possible. This method search for occurences of the pattern "(string)*count" and
     * replaces this with the string concatenated count times using the separator character in between. These constructs can be nested. The
     * replacement starts with the innermost occurrence.
     * 
     * @param patternString the pattern string
     * 
     * @return the expanded pattern string
     */

    public static String expandPatternString(String patternString) {
        if (patternString == null || patternString.equals("")) {
            return null;
        }

        String string = patternString;

        while (true) {
            StringBuffer sb = new StringBuffer();
            Matcher matcher = GROUP_PATTERN.matcher(string);
            boolean found = false;

            while (matcher.find()) {
                String str = matcher.group(1);
                char operator = matcher.group(2).charAt(0);
                int number = Integer.parseInt(matcher.group(3));

                String replacement;

                switch (operator) {
                    case '*':
                        replacement = multiply(str, number);
                        break;

                    case '+':
                        replacement = transpose(str, number);
                        break;

                    case '-':
                        replacement = transpose(str, -number);
                        break;

                    default:
                        throw new RuntimeException("Invalid operator \"" + operator + "\"");
                }

                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                found = true;
            }

            if (!found) {
                if (GROUP_CHAR_PATTERN.matcher(string).find()) {
                    throw new IllegalArgumentException("Pattern string \"" + patternString + "\" is invalid");
                }

                return string;
            }

            matcher.appendTail(sb);
            string = sb.toString();
        }
    }

    /**
     * Evaluates all functions given in the pattern string and replaces them with their results.
     * 
     * @param patternString the pattern string
     * @return the pattern string with functions evaluated
     */

    private static String evaluateFunctions(String patternString) {
        if (patternString == null || patternString.equals("")) {
            return null;
        }

        Matcher m = FUNCTION_PATTERN.matcher(patternString);

        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String function = m.group(1);

            if (function.equals("E")) {
                String params = m.group(2);

                String[] p = params.split(",");

                if (p.length != 4) {
                    throw new RuntimeException("Function E requires 4 parameters");
                }

                int pulses = Integer.parseInt(p[0]);
                int steps = Integer.parseInt(p[1]);
                String note = p[2];
                String pause = p[3];

                boolean[] rhythm = EuclideanRhythmGenerator.generate(pulses, steps);

                StringBuilder r = new StringBuilder();

                for (boolean b : rhythm) {
                    if (r.length() > 0) {
                        r.append(",");
                    }
                    if (b) {
                        r.append(note);
                    } else {
                        r.append(pause);
                    }
                }

                LOGGER.debug("E(" + pulses + "," + steps + "," + note + "," + pause + ") = " + r);

                m.appendReplacement(sb, r.toString());
            } else {
                m.appendReplacement(sb, m.group(0));
            }
        }

        m.appendTail(sb);

        return sb.toString();
    }

    /**
     * Multiplies the string, which means the string is concatenated count times using the separator in between.
     * 
     * @param str the string
     * @param count the number of concatenations
     * 
     * @return the multiplied string
     */

    private static String multiply(String str, int count) {
        int len = str.length();

        if (len == 0 || count == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder((len + 1) * count - 1);

        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(',');
            }

            sb.append(str);
        }

        return sb.toString();
    }

    /**
     * Transposes the pattern string by the given delta.
     * 
     * @param str the string
     * @param delta the transposition delta
     *
     * @return the multiplied string
     */

    private static String transpose(String str, int delta) {
        if (delta == 0) {
            // nothing to do
            return str;
        }

        StringBuffer sb = new StringBuffer(str.length());

        Matcher matcher = TRANSPOSITION_PATTERN.matcher(str);

        while (matcher.find()) {
            int number = Integer.valueOf(matcher.group(1));
            matcher.appendReplacement(sb, String.valueOf(number + delta));
        }

        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Returns the number of ticks of the given pattern string. The pattern string is expanded if necessary to count the number of ticks.
     * 
     * @param patternString the pattern string
     * 
     * @return the number of ticks
     */

    public static int getStringTicks(String patternString) {
        patternString = expand(patternString);

        String[] p = patternString.split(",");
        int len = p.length;
        int ticks = 0;

        // format: offset/ticks:velocity or offset~/ticks:velocity

        for (int i = 0; i < len; i++) {
            String[] a = p[i].split(":");
            String[] b = a[0].split("/");
            int t = b.length > 1 ? Integer.parseInt(b[1]) : 1;
            ticks += t;
        }

        return ticks;
    }

    /**
     * Expands the pattern string.
     * 
     * @param patternString the pattern string
     * @return the expanded pattern strings
     */

    private static String expand(String patternString) {
        patternString = evaluateFunctions(patternString);
        patternString = expandPatternString(patternString);
        return patternString;
    }

    /**
     * Transposes the pattern up by the given pitch (which may be negative) and returns it as a new pattern (or the original pattern if pitch is 0).
     * Only the notes will be affected by this operation (wildcards and pauses are not affected).
     * 
     * @param pitch the number of halftones to transpose up (may be negative)
     * 
     * @return a new pattern that is a transposed version of this pattern
     */

    public Pattern transpose(int pitch) {
        if (pitch == 0) {
            // nothing to do; as instances are immutable, we can just return this pattern
            return this;
        }

        PatternEntry[] newPattern = new PatternEntry[pattern.length];

        for (int i = 0; i < pattern.length; i++) {
            PatternEntry entry = pattern[i];
            if (entry.isNote()) {
                // make a modified copy of the PatternEntry
                newPattern[i] = new PatternEntry(entry.pitch + pitch, entry.velocity, entry.ticks, entry.isLegato);
            } else {
                // just use the original PatternEntry (pauses and wildcards)
                newPattern[i] = entry;
            }
        }

        return new Pattern(newPattern);
    }

    /**
     * Returns a new pattern whose entries' ticks are scaled by the given factor.
     * 
     * @param factor the scale factor
     * 
     * @return a new pattern that is a scaled by the factor
     */

    public Pattern scale(double factor) {
        if (factor == 1.0d) {
            // nothing to do; as instances are immutable, we can just return this pattern
            return this;
        }

        if (factor <= 0.0d) {
            throw new IllegalArgumentException("Factor must be > 0");
        }

        PatternEntry[] newPattern = new PatternEntry[pattern.length];

        for (int i = 0; i < pattern.length; i++) {
            PatternEntry entry = pattern[i];
            int ticks = (int) (entry.ticks * factor + 0.5d);

            if (ticks == 0) {
                throw new IllegalArgumentException("Scaling leads to 0 ticks");
            }

            if (entry.isNote()) {
                newPattern[i] = new PatternEntry(entry.pitch, entry.velocity, ticks, entry.isLegato);
            } else if (entry.isWildcard()) {
                newPattern[i] = new PatternEntry(entry.wildcardCharacter, entry.velocity, ticks, entry.isLegato);
            } else {
                newPattern[i] = new PatternEntry(ticks);
            }
        }

        return new Pattern(newPattern);
    }

    /**
     * Returns the total number of ticks this pattern spans.
     * 
     * @see #size()
     * 
     * @return the number of ticks
     */

    public int getTicks() {
        return totalTicks;
    }

    /**
     * Returns the pattern entry with the given index.
     * 
     * @param index the index
     * 
     * @return the sequence entry at that index
     */

    public PatternEntry get(int index) {
        return pattern[index];
    }

    /**
     * Returns the number of pattern entries this pattern contains. Note that in general this is not the number of ticks.
     * 
     * @return the size of the pattern
     * 
     * @see #getTicks()
     */

    public int size() {
        return pattern.length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append('#');
        sb.append(totalTicks);
        sb.append('{');

        for (int i = 0; i < pattern.length; i++) {
            sb.append(pattern[i].toString());
            sb.append(',');
        }

        sb.setLength(sb.length() - 1);

        return sb.append('}').toString();
    }

    /**
     * Checks whether it is legal to use legato at the note that ends on the given tick. The tick must be the tick on which the note would be switched
     * off if no legato was used; the same applies to the pattern offset. Legato is legal for the tick if there is a subsequent note on the pattern
     * that lies in the activity range of the activity vector. I.e., it is illegal to use legato on a note which ends outside of an activity interval.
     * 
     * @param activityVector the activity vector
     * @param tick the tick
     * @param patternOffset the pattern offset
     * 
     * @return true if legato is legal, false otherwise
     */

    public boolean isLegatoLegal(ActivityVector activityVector, int tick, int patternOffset) {
        if (!activityVector.isActive(tick)) {
            return false;
        }

        // get the remaining length of the activity interval
        int activityLength = activityVector.getIntervalLength(tick);

        int patternLength = size();

        int i = 0;
        while (i < activityLength) {
            PatternEntry entry = get(patternOffset % patternLength);

            if (entry.isNote() || entry.isWildcard() && entry.getVelocity() > 0) {
                return true;
            }

            patternOffset++;
            i += entry.getTicks();
        }

        return false;
    }

    /**
     * Provides an iterator that iterates over all PatternEntries of this pattern.
     * 
     * @return the iterator
     */

    @Override
    public Iterator<PatternEntry> iterator() {
        return new Iterator<PatternEntry>() {
            private PatternEntry[] array = pattern;
            private int pos;

            @Override
            public boolean hasNext() {
                return pos < array.length;
            }

            @Override
            public PatternEntry next() {
                if (hasNext()) {
                    return array[pos++];
                } else {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Represents a pattern entry.
     */

    public static final class PatternEntry {
        /** The pitch. */
        private int pitch;

        /** The velocity. */
        private final int velocity;

        /** The number of ticks. */
        private final int ticks;

        /** Flag for wildcards. */
        private boolean isWildcard;

        /** The wildcard character. */
        private char wildcardCharacter;

        /** Flag for legato. */
        private boolean isLegato;

        /**
         * Constructor.
         * 
         * @param ticks the number of ticks
         */

        public PatternEntry(int ticks) {
            this.velocity = 0;
            this.ticks = ticks;
        }

        /**
         * Constructor.
         * 
         * @param pitch the pitch
         * @param velocity the velocity
         * @param ticks the number of ticks
         * @param legato the legato flag
         */

        public PatternEntry(int pitch, int velocity, int ticks, boolean legato) {
            this.pitch = pitch;
            this.velocity = velocity;
            this.ticks = ticks;
            this.isLegato = legato;
        }

        /**
         * Constructor.
         * 
         * @param wildcardCharacter the wildcard character
         * @param velocity the velocity
         * @param ticks the number of ticks
         * @param legato the legato flag
         */

        public PatternEntry(char wildcardCharacter, int velocity, int ticks, boolean legato) {
            this.velocity = velocity;
            this.ticks = ticks;
            this.wildcardCharacter = wildcardCharacter;
            this.isWildcard = true;
            this.isLegato = legato;
        }

        /**
         * Returns the wildcard character.
         * 
         * @return the wildcard character
         */

        public char getWildcardCharacter() {
            return wildcardCharacter;
        }

        /**
         * Returns the pitch.
         * 
         * @return the pitch
         */

        public int getPitch() {
            return pitch;
        }

        /**
         * Returns the velocity.
         * 
         * @return the velocity
         */

        public int getVelocity() {
            return velocity;
        }

        /**
         * Returns the number of ticks.
         * 
         * @return the number of ticks
         */

        public int getTicks() {
            return ticks;
        }

        /**
         * Returns true if this is a note, false otherwise.
         * 
         * @return true if this is a note, false otherwises
         */

        public boolean isNote() {
            return !isWildcard && velocity > 0;
        }

        /**
         * Returns true if this is a wildcard, false otherwise.
         * 
         * @return true if this is a wildcard, false otherwise
         */

        public boolean isWildcard() {
            return isWildcard;
        }

        /**
         * Returns true if this is a pause, false otherwise.
         * 
         * @return true if this is a pause, false otherwise
         */

        public boolean isPause() {
            return velocity <= 0;
        }

        /**
         * Returns the legato flag.
         * 
         * @return the legato flag
         */

        public boolean isLegato() {
            return isLegato;
        }

        public String toString(SongContext songContext) {
            if (isPause()) {
                return "-" + (ticks > 1 ? "/" + ticks : "");
            } else {
                return (isWildcard ? "" + wildcardCharacter : "" + pitch) + (ticks > 1 ? "/" + ticks : "") + (velocity == songContext.getStructure()
                        .getMaxVelocity() ? "" : ":" + velocity);
            }
        }
    }
}
