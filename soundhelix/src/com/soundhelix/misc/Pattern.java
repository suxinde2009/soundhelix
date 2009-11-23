package com.soundhelix.misc;

/**
 * Represents a pattern, which is like a repeatable Sequence, but is immutable
 * and allows particular notes as well as wildcards used for special purposes.
 * Patterns can be created from pattern strings.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public class Pattern {
	private PatternEntry[] pattern;
	private int totalTicks;

	public Pattern(PatternEntry[] pattern) {
		this.pattern = pattern;

		int ticks = 0;
		
		for(int i=0;i<pattern.length;i++) {
			ticks += pattern[i].getTicks();
		}
		
		this.totalTicks = ticks;
	}
	
	public static Pattern parseString(String patternString) {
		return parseString(patternString,"");
	}

	public static Pattern parseString(String patternString,String wildcardString) {
		if(wildcardString == null) {
			wildcardString = "";
		}
		
		PatternEntry[] pattern;
		
		String[] p = patternString.split(",");
		int len = p.length;

		pattern = new PatternEntry[len];
		
		// format: offset/ticks:velocity or offset~/ticks:velocity
		
		for(int i=0;i<len;i++) {
			String[] a = p[i].split(":");
			short v = (a.length > 1 ? Short.parseShort(a[1]) : Short.MAX_VALUE);
			String[] b = a[0].split("/");
			int t = (b.length > 1 ? Integer.parseInt(b[1]) : 1);
			
			boolean legato = b[0].endsWith("~");
			
			if(legato) {
				// cut off legato character
				b[0] = b[0].substring(0,b[0].length()-1);
			}

			if(b[0].equals("-")) {
				pattern[i] = new Pattern.PatternEntry(t);
			} else if(b[0].length() == 1 && wildcardString.indexOf(b[0]) >= 0) {
				pattern[i] = new PatternEntry(b[0].charAt(0),v,t,legato);
			} else {
				pattern[i] = new PatternEntry(Integer.parseInt(b[0]),v,t,legato);
			}
		}

		return new Pattern(pattern);
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
	 * Returns the sequence entry with the given index.
	 * 
	 * @param index
	 *            the index
	 * 
	 * @return the sequence entry at that index
	 */

	public PatternEntry get(int index) {
		return pattern[index];
	}

	/**
	 * Returns the number of pattern entries this pattern contains.
	 * Note that in general this is not the number of ticks.
	 * 
	 * @return the size of the pattern
	 * 
	 * @see #getTicks()
	 */

	public int size() {
		return pattern.length;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append('#');
		sb.append(totalTicks);
		sb.append('{');

		for(int i=0;i<pattern.length;i++) {
			sb.append(pattern[i].toString());
			sb.append(',');
		}

		sb.setLength(sb.length()-1);

		return sb.append('}').toString();
	}
	
	public static class PatternEntry {
		private int pitch;
		private short velocity;
		private int ticks;
		private boolean isWildcard;
		private char wildcardCharacter;
		private boolean legato;

		public PatternEntry(int ticks) {
			this.velocity = 0;
			this.ticks = ticks;
		}

		public PatternEntry(int pitch,short velocity,int ticks,boolean legato) {
			this.pitch = pitch;
			this.velocity = velocity;
			this.ticks = ticks;
			this.legato = legato;
		}

		public PatternEntry(char wildcardCharacter,short velocity,int ticks,boolean legato) {
			this.velocity = velocity;
			this.ticks = ticks;
			this.wildcardCharacter = wildcardCharacter;
			this.isWildcard = true;
			this.legato = legato;
		}

		public char getWildcardCharacter() {
			return wildcardCharacter;
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
			return !isWildcard && velocity > 0;
		}

		public boolean isWildcard() {
			return isWildcard;
		}

		public boolean isPause() {
			return velocity <= 0;
		}

		public boolean isLegato() {
			return legato;
		}
		
		public String toString() {
			if(isPause()) {
				return "-"+(ticks > 1 ? "/"+ticks : "");
			} else {
				return (isWildcard ? ""+wildcardCharacter : ""+pitch) + (ticks > 1 ? "/" + ticks : "") + (velocity == Short.MAX_VALUE ? "" : ":" + velocity);
			}
		}
	}
	
	/**
	 * Checks whether it is legal to use legato at the note that ends on the given tick. The tick must
	 * be the tick on which the note would be switched off if no legato was used; the same applies to
	 * the pattern offset. Legato is legal for the tick if there is a subsequent note on the pattern
	 * that lies in the activity range of the activity vector. I.e., it is illegal to use legato on a
	 * note which ends outside of an activity interval.
	 */
	
	public boolean isLegatoLegal(ActivityVector activityVector,int tick,int patternOffset) {	
		if(!activityVector.isActive(tick)) {
			return false;
		}
		
		// get the remaining length of the activity interval
		int activityLength = activityVector.getIntervalLength(tick);

		int patternLength = size();
		
		for(int i=0;i<activityLength;i++) {
			PatternEntry entry = get(patternOffset%patternLength);
		
			if(entry.isNote() || entry.isWildcard() && entry.getVelocity() > 0) {
				return true;
			}
			
			patternOffset++;
			i += entry.getTicks();
		}
		
		return false;
	}
}
