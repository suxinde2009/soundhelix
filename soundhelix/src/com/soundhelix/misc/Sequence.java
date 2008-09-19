package com.soundhelix.misc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a sequence, i.e., the notes and pauses of a single voice. A note
 * consists of a pitch (with 0 being c', 1 being c'# and so on), a velocity (between 0 and
 * Short.MAX_VALUE) and a positive length in ticks. A pause is represented by an
 * arbitrary pitch, a velocity of -1 and a positive length in ticks. The
 * velocity can be used to represent a note's volume, but after all, it is up to
 * the playback device how it interprets the velocity. For example, a device
 * might always play a note at its full volume and use the velocity to control
 * filter cut-off instead.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class Sequence {
	private List<Sequence.SequenceEntry> sequence = new ArrayList<Sequence.SequenceEntry>();
	private int totalTicks;
	boolean lastWasPause = false;

	/**
	 * Calls addNote(pitch,Short.MAX_VALUE,ticks).
	 * 
	 * @param pitch
	 *            the pitch
	 * @param ticks
	 *            the ticks
	 */

	public void addNote(int pitch, int ticks) {
		addNote(pitch,ticks,Short.MAX_VALUE);
	}

	/**
	 * Appends a new note with the given pitch, velocity and number of ticks,
	 * which logically means a note-down for the given number of ticks and a
	 * note-up afterwards. This method does nothing if ticks is 0. If velocity
	 * is 0, an equivalently sized pause is added.
	 * 
	 * @param pitch
	 *            the pitch
	 * @param velocity
	 *            the velocity (between 0 and 32767)
	 * @param ticks
	 *            the ticks
	 */

	public void addNote(int pitch, int ticks, short velocity) {
		if (ticks > 0) {
			if (velocity == 0) {
				addPause(ticks);
			} else {
				sequence.add(new SequenceEntry(pitch, velocity, ticks));
				this.totalTicks += ticks;
				lastWasPause = false;
			}
		}
	}

	/**
	 * Appends a pause with the given number of ticks. If the previous sequence
	 * entry already was a pause, that pause is extended by the number of ticks
	 * instead of adding another pause. The method does nothing if ticks is 0.
	 * 
	 * @param ticks
	 *            the ticks
	 */

	public void addPause(int ticks) {
		if (ticks > 0) {
			if (lastWasPause) {
				// extend the previous pause
				SequenceEntry e = sequence.get(sequence.size() - 1);
				e.ticks += ticks;
			} else {
				// add a new pause
				sequence.add(new SequenceEntry(0, (short) -1, ticks));
				lastWasPause = true;
			}

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
	 * @param index
	 *            the index
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

	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append('#');
		sb.append(totalTicks);
		sb.append('{');

		Iterator<SequenceEntry> i = sequence.iterator();

		while (i.hasNext()) {
			SequenceEntry entry = i.next();

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

	public class SequenceEntry {
		private int pitch;
		private short velocity;
		private int ticks;

		public SequenceEntry(int pitch, short velocity, int ticks) {
			this.pitch = pitch;
			this.velocity = velocity;
			this.ticks = ticks;
		}

		public int getPitch() {
			return pitch;
		}

		public short getVelocity() {
			return velocity;
		}

		public int getTicks() {
			return ticks;
		}

		public boolean isNote() {
			return velocity > 0;
		}

		public String toString() {
			return pitch + "/" + ticks + "/" + velocity;
		}
	}
}
