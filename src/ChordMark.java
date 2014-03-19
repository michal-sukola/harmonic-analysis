import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ChordMark {
	private Chord chord = null;
	private int midiNum = -1;
	private boolean incomplete = false;
	private IncompleteChord incChord = null;
	
	//public static ChordMark NONE = new ChordMark((Chord) null, -1);
	
	public int getMidiNum() {
		return midiNum % 12;
	}
	
	public boolean isIncomplete() {
		return incomplete;
	}
	
	public IncompleteChord getIncChord() {
		return incChord;
	}
	
	public Chord getChord() {
		return chord;
	}
	
	public static String midiNumToToneString(int midiNum) {
		return MidiReader.NOTE_NAMES[midiNum % 12];
	}
	
	public String getChordToneString() {
		return midiNumToToneString(midiNum);
	}
	
	public String getChordTonesString() {
		String s = "";
		for (Integer toneMidi : getChordMidiNums()) {
			s += midiNumToToneString(toneMidi) + " ";
		}
		
		return s;
	}
	
	public Integer[] getChordMidiNums() {
		if(isIncomplete()) {
			int basicTone = getMidiNum() % 12;
			int interval = new Integer(incChord.name().substring(incChord.name().length()-1));
			
			int secondTone = (basicTone + interval) % 12; 
			
			return new Integer[] {basicTone, secondTone};
		}
		
		Chord chord = getChord();
		int basicTone = getMidiNum() % 12;
		
		int arraySize = chord.getNumTones();
		Integer[] chordNums = new Integer[arraySize];
		int[] intervals = chord.getIntervals();
		
		chordNums[0] = basicTone;
		for (int i = 1; i < chordNums.length; i++) {
			chordNums[i] = (chordNums[i-1] + intervals[i-1]) % 12;
		}
		
		Arrays.sort(chordNums);
		
		return chordNums;
	}
	
	
	public ChordMark(IncompleteChord chord, int tone) {
		this.incChord = chord;
		this.incomplete = true;
		this.midiNum = tone % 12;
	}
	
	public ChordMark(Chord chord, int tone) {
		this.chord = chord;
		this.midiNum = tone % 12;
		//assert (this.chord == null) : "Create empty chord chord mark: " + this.chord;
		
		if(this.chord == null) {
			throw new RuntimeException("Null chord at ChordMark constructor");
		}
	}
	
	public ChordMark(ChordMark chM) {
		this(chM.chord, chM.midiNum);
		if(chM.isIncomplete()) {
			this.incomplete = true;
		} else {
			if(chM.chord == null) {
				throw new RuntimeException("Null chord at ChordMark constructor");
			}
		}
		
		
	}
	
	@Override
	public String toString() {
		if(chord == null) {
			if(this.incomplete) {
				return "*" + this.incChord.name();
			} else {
				return "null";
			}
		}

		return (MidiReader.NOTE_NAMES[midiNum] + " " + chord.name());
	}
	
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		if(!(obj instanceof ChordMark)) {
			return false;
		}
		
		ChordMark cm = (ChordMark) obj;
		return (midiNum == cm.midiNum) && chord.equals(cm.chord);
	}
	
	public static void main(String[] args) {
		Integer[] ints = {5,3,6,1,8,4};
		Arrays.sort(ints);
		System.out.println(Arrays.toString(ints));
	}

	public int getContainedTonesCount(Map<Integer, Double> toneMap) {
		int containedCount = 0;
		
		Integer[] chordMidis = getChordMidiNums();
		for (int chordTone : chordMidis) {
			if(toneMap.containsKey(chordTone)) {
				containedCount++;
			}
		}
				
		return containedCount;
	}

	public void setMidiNum(Integer basicTone) {
		if(! getChord().equals(Chord.DIMINISHED_SEVENTH)) {
			throw new RuntimeException("Only allowed for chord: " + Chord.DIMINISHED_SEVENTH);
		}
		
		this.midiNum = basicTone % 12;
	}
}
