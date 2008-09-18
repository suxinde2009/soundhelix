package com.soundhelix.misc;

import com.soundhelix.util.ConsistentRandom;
import com.soundhelix.util.NoteUtils;

/**
 * Defines a chord. A chord is immutable, consists of a pitch (with 0 being C'),
 * a type (major, minor) and a subtype (base 0, base 4 and base 6). The pitch
 * always identifies the base/main note of the chord, which need not be the
 * lowest note of the chord.
 * 
 * Note that even though the pitch of the base note also defines the
 * octave, it is up to the caller to make use of or ignore the pitch's
 * octave.
 * 
 * @author Thomas Sch�rger
 *
 */

public class Chord {
	
	public enum ChordType {
		MAJOR,MINOR
	};

	public enum ChordSubtype {
		BASE_0,BASE_4,BASE_6
	};

    private final int pitch;
    private final ChordType type;
    private final ChordSubtype subtype;

	public Chord(int pitch,ChordType type,ChordSubtype subtype) {
		this.pitch = pitch;
		this.type = type;
		this.subtype = subtype;
	}
	
	private static ConsistentRandom random;

	/**
	 * Returns the pitch of the chord's base note. Note that
	 * this need not be the low note of the chord.
	 * 
	 * @return the pitch of the base note
	 */
	
	public int getPitch() {
		return pitch;
	}
	
	/**
	 * Returns the chord's type.
	 * 
	 * @return the type
	 */
	
	public ChordType getType() {
		return type;
	}

	/**
	 * Returns the chord's subtype.
	 * 
	 * @return the subtype
	 */

	public ChordSubtype getSubtype() {
		return subtype;
	}

	public boolean isMajor() {
		return type == ChordType.MAJOR;
	}
	
	public boolean isMinor() {
		return type == ChordType.MINOR;
	}
	
	public boolean equals(Chord otherChord) {
		return otherChord != null && this.pitch == otherChord.pitch && this.type == otherChord.type && this.subtype == otherChord.subtype;
	}
	
	public String toString() {
		return NoteUtils.getNoteName(pitch).toUpperCase()+(isMinor() ? "m" : "")+(subtype == ChordSubtype.BASE_4 ? "4" : subtype == ChordSubtype.BASE_6 ? "6" : "")+"+"+getLowPitch()+"/"+getMiddlePitch()+"/"+getHighPitch();
	}

	/**
	 * Returns the sum of the absolute pitch differences between this chord and
	 * the given chord of the low, middle and high note. This is a measure of
	 * how far the two chords are away from each other on the keyboard.
	 * 
	 * @param otherChord
	 * 
	 * @return the pitch difference
	 */
	
	public int getPitchDistance(Chord otherChord) {
        return Math.abs(getLowPitch()-otherChord.getLowPitch()) +
               Math.abs(getMiddlePitch()-otherChord.getMiddlePitch()) +
               Math.abs(getHighPitch()-otherChord.getHighPitch());
	}

	/**
	 * Returns a version of the given chord that is closest to this chord. This
	 * method chooses the version of the chord that minimizes the pitch distance
	 * of the middle note of the two chords. This may include modifying the
	 * subtype and/or octave of the chord. If there is a tie between two
	 * possible chord candidates, one of the chords is chosen randomly, but
	 * consistently.
	 * 
	 * @param otherChord the chord to return a close version of
	 * 
	 * @return a close version of the chord
	 */
	
	public Chord findClosestChord(Chord otherChord) {
		//System.out.println("Find closest chord of "+otherChord+" for "+this);
		
		if(pitch == otherChord.getPitch() && type == otherChord.getType()) {
			// our chord clearly is the best choice in this case, regardless
			// of the subtype of the other chord
			return this;
		}
		
		int pitch1 = getMiddlePitch();
		int pitch2 = otherChord.getMiddlePitch();

		if(pitch2 < pitch1) {
			Chord lastChord = otherChord;
			
			while(true) {				
				Chord chord = lastChord.getHigherChord();
				//System.out.println("Chord: "+chord+" low pitch: "+chord.getLowPitch()+"  lastChord: "+lastChord+" low pitch: "+lastChord.getLowPitch());
				
				if(chord.getMiddlePitch() >= pitch1) {
					// the new chord's low pitch has now reached at least pitch1
					// the last chord's low pitch was lower than pitch1
					
					// check if chord or lastChord is better
					
					int diff1 = chord.getMiddlePitch()-pitch1;
					int diff2 = pitch1-lastChord.getMiddlePitch();
					
					if(diff1 < diff2) {
						return chord;
					} else if(diff1 > diff2 ){
						return lastChord;
					} else {
						System.out.println("Tie between "+lastChord+" and "+chord+"  Reference: "+this);
						
						if(random == null) {
							random = new ConsistentRandom();
						}
						
						if(random.getBoolean(lastChord.toString()+"#"+chord.toString())) {
							System.out.println("Using "+chord);
							return chord;
						} else {
							System.out.println("Using "+lastChord);
							return lastChord;
						}
					}
				}
				
				lastChord = chord;
			}
		} else {
			Chord lastChord = otherChord;
			
			while(true) {
				Chord chord = lastChord.getLowerChord();
				//System.out.println("Chord: "+chord+" low pitch: "+chord.getLowPitch()+"  lastChord: "+lastChord+" low pitch: "+lastChord.getLowPitch());
				
				if(chord.getMiddlePitch() <= pitch1) {
					// the new chord's low pitch has now reached at most pitch1
					// the last chord's low pitch was higher than pitch1
					
					// check if chord or lastChord is better
					
					int diff1 = pitch1-chord.getMiddlePitch();
					int diff2 = lastChord.getMiddlePitch()-pitch1;

					if(diff1 < diff2) {
						return chord;
					} else if(diff1 > diff2) {
						return lastChord;
					} else {
						System.out.println("Tie between "+lastChord+" and "+chord+"  Reference: "+this);
						
						if(random == null) {
							random = new ConsistentRandom();
						}
						
						if(random.getBoolean(lastChord.toString()+"#"+chord.toString())) {
							System.out.println("Using "+chord);
							return chord;
						} else {
							System.out.println("Using "+lastChord);
							return lastChord;
						}
					}
				}
				
				lastChord = chord;
			}			
		}
	}

	/**
	 * Returns the pitch of the low note of the chord, respecting the chord's
	 * subtype.
	 * 
	 * @return the pitch of the low note
	 */
	
	public int getLowPitch() {
		if(subtype == ChordSubtype.BASE_0) {
			return pitch;
		} else if(subtype == ChordSubtype.BASE_4) {
			return isMajor() ? pitch+4 : pitch+3;
		} else {
			return pitch-5;
		}		
	}

	/**
	 * Returns the pitch of the middle note of the chord, respecting the chord's
	 * subtype.
	 * 
	 * @return the pitch of the middle note
	 */
	
	public int getMiddlePitch() {
		if(subtype == ChordSubtype.BASE_6) {
			return pitch;
		} else if(subtype == ChordSubtype.BASE_0) {
			return isMajor() ? pitch+4 : pitch+3;
		} else {
			return pitch+7;
		}		
	}

	/**
	 * Returns the pitch of the high note of the chord, respecting the chord's
	 * subtype.
	 * 
	 * @return the pitch of the high note
	 */

	public int getHighPitch() {
		if(subtype == ChordSubtype.BASE_4) {
			return pitch+12;
		} else if(subtype == ChordSubtype.BASE_6) {
			return isMajor() ? pitch+4 : pitch+3;
		} else {
			return pitch+7;
		}		
	}
	
	/**
	 * Returns a new version of the chord that is one step
	 * higher than the chord. Effectively, this method replaces
	 * the low note of the chord with that note transposed one
	 * octave up. This always involves changing the subtype
	 * of the chord.
	 * 
	 * @return the higher chord
	 */
	
    public Chord getHigherChord() {
        if(subtype == ChordSubtype.BASE_0) {
        	return new Chord(getPitch(),getType(),ChordSubtype.BASE_4);
        } else if(subtype == ChordSubtype.BASE_4) {
        	return new Chord(getPitch()+12,getType(),ChordSubtype.BASE_6);
        } else {
        	return new Chord(getPitch(),getType(),ChordSubtype.BASE_0);
        }
    }
    
	/**
	 * Returns a new version of the chord that is one step
	 * lower than the chord. Effectively, this method replaces
	 * the high note of the chord with that note transposed one
	 * octave down. This always involves changing the subtype
	 * of the chord.
	 * 
	 * @return the higher chord
	 */
    
    public Chord getLowerChord() {
        if(subtype == ChordSubtype.BASE_0) {
        	return new Chord(getPitch(),getType(),ChordSubtype.BASE_6);
        } else if(subtype == ChordSubtype.BASE_4) {
        	return new Chord(getPitch(),getType(),ChordSubtype.BASE_0);
        } else {
        	return new Chord(getPitch()-12,getType(),ChordSubtype.BASE_4);
        }    	
    }
    
    public int hashCode() {
    	return toString().hashCode();
    }
}
