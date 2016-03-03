package com.soundhelix.misc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a sequence, i.e., the notes and pauses of a single voice. A note consists of a pitch (with 0 being c', 1 being c#' and so on), a
 * velocity (between 0 and maxVelocity) and a positive length in ticks. A pause is represented by an arbitrary pitch, a velocity of -1 and a positive
 * length in ticks. The velocity can be used to represent a note's volume, but after all, it is up to the playback device how it interprets the
 * velocity. For example, a device might always play a note at its full volume and use the velocity to control filter cut-off instead.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public class Sequence implements Iterable<Sequence.SequenceEntry> {
    /** The list of sequence entries. */
    private List<Sequence.SequenceEntry> sequence = new ArrayList<Sequence.SequenceEntry>();

    /** The total number of ticks in this sequence. */
    private int totalTicks;

    /** Flag indicating whether the last note added was a pause. */
    private boolean lastWasPause;

    /** The song context. */
    private SongContext songContext;

    /**
     * Constructor.
     */

    @SuppressWarnings("unused")
    private Sequence() {
    }

    /**
     * Constructor.
     * 
     * @param songContext the song context
     */

    public Sequence(SongContext songContext) {
        if (songContext == null) {
            throw new IllegalArgumentException("songContext must not be null");
        }

        this.songContext = songContext;
    }

    @Override
    public Iterator<SequenceEntry> iterator() {
        return sequence.iterator();
    }

    /**
     * Calls addNote(pitch,ticks,maxVelocity).
     * 
     * @param pitch the pitch
     * @param ticks the ticks
     */

    public void addNote(int pitch, int ticks) {
        addNote(pitch, ticks, songContext.getStructure().getMaxVelocity());
    }

    /**
     * Calls addNote(pitch,ticks,velocity,false).
     * 
     * @param pitch the pitch
     * @param ticks the ticks
     * @param velocity the velocity
     */

    public void addNote(int pitch, int ticks, int velocity) {
        addNote(pitch, ticks, velocity, false);
    }

    /**
     * Appends a new note with the given pitch, velocity and number of ticks, which logically means a note-down for the given number of ticks and a
     * note-up afterwards. This method does nothing if ticks is 0. If velocity is 0, an equivalently sized pause is added.
     * 
     * @param pitch the pitch
     * @param velocity the velocity (between 0 and maxVelocity)
     * @param ticks the ticks
     * @param legato the legato flag
     */

    public void addNote(int pitch, int ticks, int velocity, boolean legato) {
        if (velocity < 0 || velocity > songContext.getStructure().getMaxVelocity()) {
            throw new IllegalArgumentException("Velocity not in allowed range");
        }

        if (ticks > 0) {
            if (velocity == 0) {
                addPause(ticks);
            } else {
                sequence.add(new SequenceEntry(pitch, velocity, ticks, legato));
                this.totalTicks += ticks;
                lastWasPause = false;
            }
        }
    }

    /**
     * Appends a pause with the given number of ticks. If the previous sequence entry already was a pause, that pause is extended by the number of
     * ticks instead of adding another pause. The method does nothing if ticks is 0.
     * 
     * @param ticks the ticks
     */

    public void addPause(int ticks) {
        if (ticks > 0) {
            if (lastWasPause) {
                // extend the previous pause
                SequenceEntry e = sequence.get(sequence.size() - 1);
                e.ticks += ticks;
            } else {
                // add a new pause
                sequence.add(new SequenceEntry(0, -1, ticks, false));
                lastWasPause = true;
            }

            this.totalTicks += ticks;
        }
    }

    /**
     * Extends the previous sequence entry (which must be note) by the given number of ticks. An IllegalStateException will be thrown if the sequence
     * is empty or if the previous entry is not a note.
     * 
     * @param ticks the number of ticks
     */

    public void extendNote(int ticks) {
        if (ticks > 0) {
            int size = sequence.size();

            if (size == 0) {
                throw new IllegalStateException("Sequence must not be emtpy");
            }

            if (lastWasPause) {
                throw new IllegalStateException("Previous entry is not a note");
            }

            SequenceEntry entry = sequence.get(size - 1);

            entry.ticks += ticks;
            this.totalTicks += ticks;
        }
    }

    /**
     * Returns the total number of ticks this sequence spans.
     * 
     * @see #size()
     * 
     * @return the number of ticks
     */

    public int getTicks() {
        return totalTicks;
    }

    /**
     * Returns the sequence entry with the given index.
     * 
     * @param index the index
     * 
     * @return the sequence entry at that index
     */

    public SequenceEntry get(int index) {
        return sequence.get(index);
    }

    /**
     * Returns the number of sequence entries this sequence contains.
     * 
     * @return the size of the sequence
     * 
     * @see #getTicks()
     */

    public int size() {
        return sequence.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append('#');
        sb.append(totalTicks);
        sb.append('{');

        for (SequenceEntry entry : sequence) {
            if (entry.isNote()) {
                sb.append(entry.getPitch());
            } else {
                sb.append('-');
            }
            sb.append('/');
            sb.append(entry.getTicks());
            sb.append(',');
        }

        sb.deleteCharAt(sb.length() - 1);

        return sb.append('}').toString();
    }

    /**
     * Transposes all notes of this sequence up by the given number of halftones.
     * 
     * @param halftones the number of halftones (positive or negative)
     */

    public void transpose(int halftones) {
        if (halftones == 0) {
            // nothing to do
            return;
        }

        for (SequenceEntry entry : sequence) {
            if (entry.isNote()) {
                entry.pitch += halftones;
            }
        }
    }

    /**
     * Scales the velocity of all notes by velocity/maxVelocity.
     * 
     * @param velocity the velocity
     */

    public void scaleVelocity(int velocity) {
        int maxVelocity = songContext.getStructure().getMaxVelocity();

        if (velocity == maxVelocity) {
            // nothing to do
            return;
        }

        for (Sequence.SequenceEntry entry : sequence) {
            if (entry.isNote()) {
                int v = (int) ((long) entry.getVelocity() * velocity / maxVelocity);

                if (v < 1) {
                    v = 1;
                } else if (v > maxVelocity) {
                    v = maxVelocity;
                }

                entry.velocity = v;
            }
        }
    }

    /**
     * Replaces the entry at the given tick by the given entry. The tick can be any tick within the sequence and any note can be given. Note that this
     * is an expensive operation, because the runtime grows linearly with the number of entries in the sequence.
     * 
     * @param tick the tick where the replacement should take place
     * @param entry the SequenceEntry to insert
     */

    public void replaceEntry(int tick, SequenceEntry entry) {
        // determine the insertion offset

        // FIXME: doesn't correctly cover all cases

        int offset = 0;
        int offsetTick = 0;

        while (offsetTick < tick) {
            offsetTick += sequence.get(offset).ticks;
            offset++;
        }

        if (tick < offsetTick) {
            sequence.get(offset - 1).ticks = tick - (offsetTick - sequence.get(offset - 1).ticks);

            int diff = tick + entry.ticks - offsetTick;

            if (diff == 0) {
                sequence.add(offset, entry);
                return;
            } else if (diff < 0) {
                sequence.add(offset, entry);
                sequence.add(offset + 1, new SequenceEntry(0, -1, -diff, false));
                return;
            } else {
                sequence.add(offset, entry);

                int prevLength = 0;
                offset++;

                do {
                    prevLength += sequence.get(offset).ticks;
                    sequence.remove(offset);
                } while (diff > prevLength);

                if (diff < prevLength) {
                    sequence.add(offset, new SequenceEntry(0, -1, prevLength - diff, false));
                }
                return;
            }
        } else {
            int prevLength = sequence.get(offset).ticks;

            int diff = entry.ticks - prevLength;

            if (diff == 0) {
                sequence.set(offset, entry);
            } else if (diff < 0) {
                sequence.set(offset, entry);
                sequence.add(offset + 1, new SequenceEntry(0, -1, -diff, false));
            } else {
                sequence.set(offset, entry);

                offset++;

                do {
                    prevLength += sequence.get(offset).ticks;
                    sequence.remove(offset);
                } while (entry.ticks > prevLength);

                if (entry.ticks < prevLength) {
                    sequence.add(offset, new SequenceEntry(0, -1, prevLength - entry.ticks, false));
                }
            }
        }
    }

    /**
     * A container for a sequence entry.
     */

    public static class SequenceEntry {
        /** The pitch. */
        private int pitch;

        /** The velocity. */
        private int velocity;

        /** The number of ticks. */
        private int ticks;

        /** The legato flag. */
        private boolean legato;

        /**
         * Constructor.
         * 
         * @param pitch the pitch
         * @param velocity the velocity
         * @param ticks the number of ticks
         * @param legato the legato flag
         */

        public SequenceEntry(int pitch, int velocity, int ticks, boolean legato) {
            this.pitch = pitch;
            this.velocity = velocity;
            this.ticks = ticks;
            this.legato = legato;
        }

        public int getPitch() {
            return pitch;
        }

        public int getVelocity() {
            return velocity;
        }

        public int getTicks() {
            return ticks;
        }

        public boolean isNote() {
            return velocity > 0;
        }

        public boolean isPause() {
            return velocity <= 0;
        }

        public boolean isLegato() {
            return legato;
        }

        @Override
        public String toString() {
            return pitch + "/" + ticks + "/" + velocity;
        }
    }
}
