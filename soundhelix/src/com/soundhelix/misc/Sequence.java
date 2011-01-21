package com.soundhelix.misc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a sequence, i.e., the notes and pauses of a single voice. A note
 * consists of a pitch (with 0 being c', 1 being c#' and so on), a velocity (between 0 and
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
	/** The list of sequence entries. */
	private List<Sequence.SequenceEntry> sequence = new ArrayList<Sequence.SequenceEntry>();

	/** The total number of ticks in this sequence. */
	private int totalTicks;
	
	/** Flag indicating whether the last note added was a pause. */
	private boolean lastWasPause;

	/**
	 * Calls addNote(pitch,ticks,Short.MAX_VALUE).
	 * 
	 * @param pitch the pitch
	 * @param ticks the ticks
	 */

	public void addNote(int pitch, int ticks) {
		addNote(pitch, ticks, Short.MAX_VALUE);
	}

	/**
	 * Calls addNote(pitch,Short.MAX_VALUE,ticks,false).
	 * 
	 * @param pitch the pitch
	 * @param ticks the ticks
	 * @param velocity the velocity
	 */

	public void addNote(int pitch, int ticks, short velocity) {
		addNote(pitch, ticks, velocity, false);
	}

	/**
	 * Appends a new note with the given pitch, velocity and number of ticks,
	 * which logically means a note-down for the given number of ticks and a
	 * note-up afterwards. This method does nothing if ticks is 0. If velocity
	 * is 0, an equivalently sized pause is added.
	 * 
	 * @param pitch the pitch
	 * @param velocity the velocity (between 0 and 32767)
	 * @param ticks the ticks
	 * @param legato the legato flag
	 */

	public void addNote(int pitch, int ticks, short velocity, boolean legato) {
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
	 * Appends a pause with the given number of ticks. If the previous sequence
	 * entry already was a pause, that pause is extended by the number of ticks
	 * instead of adding another pause. The method does nothing if ticks is 0.
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
				sequence.add(new SequenceEntry(0, (short) -1, ticks, false));
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

	/**
	 * Transposes all notes of this sequence up by the given
	 * number of halftones.
	 * 
	 * @param halftones the number of halftones (positive or negative)
	 */
	
	public void transpose(int halftones) {
		if (halftones == 0) {
			// nothing to do
			return;
		}
		
		Iterator<SequenceEntry> iter = sequence.iterator();
		
		while (iter.hasNext()) {
			SequenceEntry entry = iter.next();
			
			if (entry.isNote()) {
				entry.pitch += halftones;
			}
		}
	}

	/**
	 * Replaces the entry at the given tick by the given entry. The tick can be any tick within the sequence and
	 * any note can be given. Note that this is an expensive operation, because the runtime grows linearly with
	 * the number of entries in the sequence.
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
			// shorten the previous note
			sequence.get(offset - 1).ticks = tick - (offsetTick - sequence.get(offset - 1).ticks);
			
			int diff = tick + entry.ticks - offsetTick;
			
			if (diff == 0) {
				sequence.add(offset, entry);
				return;
			} else if (diff < 0) {
				sequence.add(offset, entry);
				sequence.add(offset + 1, new SequenceEntry(0, (short) -1, -diff, false));
				return;
			} else {
				sequence.add(offset, entry);

				int prevLength = 0;
				offset++;
				
				do {
					prevLength += sequence.get(offset).ticks;
					sequence.remove(offset);
				} while(diff > prevLength);

				if (diff < prevLength) {
                    sequence.add(offset, new SequenceEntry(0, (short) -1, prevLength - diff, false));
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
				sequence.add(offset + 1, new SequenceEntry(0, (short) -1, -diff, false));
			} else {
				sequence.set(offset, entry);

				offset++;

				do {
					prevLength += sequence.get(offset).ticks;
					sequence.remove(offset);
				} while(entry.ticks > prevLength);

				if (entry.ticks < prevLength) {
                    sequence.add(offset, new SequenceEntry(0, (short) -1, prevLength - entry.ticks, false));
				}
			}
		}
	}

	public static class SequenceEntry {
		private int pitch;
		private short velocity;
		private int ticks;
		private boolean legato;

		public SequenceEntry(int pitch, short velocity, int ticks, boolean legato) {
			this.pitch = pitch;
			this.velocity = velocity;
			this.ticks = ticks;
			this.legato = legato;
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
		
		public boolean isPause() {
			return velocity <= 0;
		}

		public boolean isLegato() {
			return legato;
		}
		
		public String toString() {
			return pitch + "/" + ticks + "/" + velocity;
		}
	}
	
	public static void test() {
		if (tested) {
			return;
		}
		
		tested = true;

		int number = 1;

		Sequence seq = new Sequence();

		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);

		test(number++, seq, 16, 60, 16, "#64{50/16,60/16,50/16,50/16}");

		seq = new Sequence();

		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);

		test(number++, seq, 16, 60, 8, "#64{50/16,60/8,-/8,50/16,50/16}");
		test(number++, seq, 16, Integer.MIN_VALUE, 8, "#64{50/16,-/8,-/8,50/16,50/16}");

		seq = new Sequence();

		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);

		test(number++, seq, 16, 60, 24, "#64{50/16,60/24,-/8,50/16}");

		seq = new Sequence();

		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);

		test(number++, seq, 16, 60, 32, "#64{50/16,60/32,50/16}");

		seq = new Sequence();

		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);

		test(number++, seq, 16, 60, 33, "#64{50/16,60/33,-/15}");

		seq = new Sequence();

		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);

		test(number++, seq, 16, 60, 48, "#64{50/16,60/48}");

		seq = new Sequence();

		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);

		test(number++, seq, -1, 50, 20, "#64{50/16,50/16,50/16,50/16}");
		test(number++, seq, 16, 60, 16, "#64{50/16,60/16,50/16,50/16}");

		seq = new Sequence();
		
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);

		test(number++, seq, 16, 60, 47, "#64{50/16,60/47,-/1}");

		number = 101;
		
		seq = new Sequence();
		
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);

		test(number++, seq, 8, 60, 20, "#64{50/8,60/20,-/4,50/16,50/16}");

		seq = new Sequence();
		
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);

		test(number++, seq, 8, 60, 8, "#64{50/8,60/8,50/16,50/16,50/16}");

		seq = new Sequence();
		
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);

		test(number++, seq, 8, 60, 4, "#64{50/8,60/4,-/4,50/16,50/16,50/16}");

		seq = new Sequence();
		
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);

		test(number++, seq, 8, 60, 24, "#64{50/8,60/24,50/16,50/16}");

		seq = new Sequence();
		
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);

		test(number++, seq, 8, 60, 30, "#64{50/8,60/30,-/10,50/16}");

		seq = new Sequence();
		
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);

		test(number++, seq, 8, 60, 40, "#64{50/8,60/40,50/16}");

		seq = new Sequence();
		
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);

		test(number++, seq, 8, 60, 56, "#64{50/8,60/56}");

		seq = new Sequence();
		
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);
		seq.addNote(50, 16);

		test(number++, seq, 8, 60, 55, "#64{50/8,60/55,-/1}");
	}
	
	private static boolean tested;
	
	public static void test(int number, Sequence seq, int tick, int pitch, int ticks, String str) {
		try {
			if (tick >= 0) {
				seq.replaceEntry(tick,
                    new SequenceEntry(pitch, pitch == Integer.MIN_VALUE ? -1 : (short) 32767, ticks, false));
			}
			
			int size = seq.size();
			int t = 0;
			
			for (int i = 0; i < size; i++) {
				t += seq.get(i).ticks;
			}
			
			if (t != seq.totalTicks) {
				System.out.println("Test " + number + " Expected: " + str);
				System.out.println("          Found: " + seq.toString());
                System.out.println("   ** MISMATCH: Expected " + seq.totalTicks + " ticks, got " + t + " ticks");
				return;
			}
		} catch (Exception e) {
			System.out.println("Test " + number + " Expected: " + str);
			System.out.println("   Found exception");
			e.printStackTrace();
			return;
		}
		
		String s = seq.toString();
		
		if (!s.equals(str)) {
		    System.out.println("Test " + number + " Expected: " + str);
		    System.out.println("          Found: " + s + "\n");
		} else {
		    System.out.println("Test " + number + " succeeded");
		}
	}	
}
