import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MeasureSection {
	Measure measure;
	double startBeat;
	double endBeat;
	
	List<RawNote> notes = new LinkedList<RawNote>();
	
	ChordMark chordMark = null;
	
	private static double MINIMAL_NOTE_DURATION = 1.0/128.0;
	
	public MeasureSection(Measure measure, double startBeat, double endBeat) {
		if(startBeat > endBeat) {
			throw new RuntimeException("Start mark is after end mark");
		}
		
		this.measure = measure;
		this.startBeat = startBeat;
		this.endBeat = endBeat;
		
		// fill notes to the section
		for (RawNote measureNote : measure.notes) {
			boolean isInSection = measureNoteIsInSection(measureNote);
						
			if (isInSection) {
				//System.out.println(getNoteInfo(note));
				long newNoteStart = Math.max(measureNote.startTick, this.getStartTick());
				long newNoteEnd = Math.min(measureNote.endTick, this.getEndTick());

				//System.out.println("nnS: " + newNoteStart);
				//System.out.println("nnE: " + newNoteEnd);
				
				RawNote newNote = new RawNote(newNoteStart, newNoteEnd,
						measureNote.midiNum, measureNote.track, Measure.resolution);


				// System.out.println(getNoteInfo(newNote));

				notes.add(newNote);

			}
			
		}
	}
	
	public int getDifMidiNumCount() {
		List<Integer> difMidiNums = new LinkedList<Integer>();
		for(RawNote sectionNote: this.notes) {
			if(!difMidiNums.contains(sectionNote.midiNum)) {
				difMidiNums.add(sectionNote.midiNum);
			}
		}
		
		return difMidiNums.size();
	}
	
	private boolean measureNoteIsInSection(RawNote measureNote) {
		double measureNoteStartBeat = ((double)(measureNote.startTick - measure.startTick)) / Measure.resolution;
		double measureNoteEndBeat = ((double)(measureNote.endTick - measure.startTick)) / Measure.resolution;
		
		
		// cela nota vnutri 		|*****|
		boolean isInSection = (measureNoteStartBeat >= this.startBeat)
				&& (measureNoteEndBeat <= this.endBeat);
		// zlava vnutri 			|  **|***
		isInSection |= (measureNoteStartBeat >= this.startBeat)
				&& (measureNoteStartBeat <= this.endBeat)
				&& (measureNoteEndBeat >= this.endBeat);
		// zprava vnutri 		 ***|**   |
		isInSection |= (measureNoteStartBeat <= this.startBeat)
				&& (measureNoteEndBeat >= this.startBeat)
				&& (measureNoteEndBeat <= this.endBeat);
		// cela vnutri a precnieva *|*****|*
		isInSection |= (measureNoteStartBeat <= this.startBeat)
				&& (measureNoteEndBeat >= this.endBeat);
		
		return isInSection;
	}
	
	private long getStartTick() {
		long startTick = (long)Math.ceil(this.measure.startTick + this.startBeat * Measure.resolution);
		
		return startTick;
	}
	
	private long getEndTick() {
		long endTick = (long)Math.ceil(this.measure.startTick + this.endBeat * Measure.resolution);
		
		return endTick;
	}
	
	public MeasureSection merge(MeasureSection neighborSection) {
		boolean isAdjacentSection  = (this.measure == neighborSection.measure);
				isAdjacentSection &= (neighborSection.endBeat > this.endBeat) && (neighborSection.startBeat == this.endBeat);
				isAdjacentSection |= (neighborSection.startBeat < this.startBeat) && (neighborSection.endBeat == this.startBeat);
				System.out.println("this measure: " + this.measure + ", neighbor measure: " + neighborSection.measure) ;
				System.out.println("Is same measure: " + (this.measure == neighborSection.measure) );
				
		if (! isAdjacentSection) {
			System.out.println("Is same measure: " + (this.measure == neighborSection.measure) );
			System.out.println("This section: " + this);
			System.out.println("Neighbor section: " + neighborSection);
			
			throw new RuntimeException("You are trying to merge with non adjacent section");
		}
		
		MeasureSection mergedSection = new MeasureSection(this.measure, this.startBeat, neighborSection.endBeat);
		
		return mergedSection;
	}
	
	private Map<Integer, Double> getToneMap() {
		Map<Integer, Double> toneMap = new HashMap<Integer, Double>();
		
		List<RawNote> notesToRemove = new LinkedList<RawNote>();
		
		for(RawNote note: notes) {
			int toneId = note.midiNum % 12;
			double toneDur = note.getDuration();
			
			// skip notes that are too short
			if(toneDur < MINIMAL_NOTE_DURATION) {
				//System.out.println("==> Removing note: " + note); 
				notesToRemove.add(note);
				continue;
			}
			
			Double value = toneMap.get(toneId);
			if(value == null) {
				toneMap.put(toneId, toneDur);
			} else {
				toneMap.put(toneId, toneDur + value);
			}
		}
		
		// remove too short notes
		notes.removeAll(notesToRemove);
		
		return toneMap;
	}
	
	
	private RawNote getMeasureNoteForSectionNote(RawNote sectionNote) {
		for(RawNote measureNote: this.measure.notes) {
		    boolean sectionNoteIsPartOfMeasureNote = sectionNote.isPartOf(measureNote); 

		    if(sectionNoteIsPartOfMeasureNote) {
				return measureNote;
			}
		}
		
		return null;
	}
	
	private Map<Integer, Double> getExtendedToneMap() {
		// get toneMap from the measure section
		Map<Integer, Double> toneMap = new HashMap<Integer, Double>(getToneMap());
		
		// look which notes begins before current section and add weight also from the previous sections from current measure
		List<RawNote> sectionNotes = this.notes;
		for(RawNote sectionNote: sectionNotes) {
			//System.out.println(sectionNote.startTick + " " + this.getStartTick());

			// search for start and end tick of the note in measure context
			RawNote measureNote = this.getMeasureNoteForSectionNote(sectionNote);
			//System.out.println("sectionNote: " + sectionNote);
			//System.out.println("measureNote: " + measureNote);
			
			if(measureNote != null) {
				// get the duration of midi num of the section note
				Double sectionNoteMidiNumDuration = toneMap.get(sectionNote.midiNum % 12);
				// remove the section note duration and add the duration of the whole note in measure
				//System.out.println("sectionNoteMidiNumDuration: " + sectionNoteMidiNumDuration + ", sectionNote: " + sectionNote + ", measureNote: " + measureNote);
				double newDuration = sectionNoteMidiNumDuration - sectionNote.getDuration() + measureNote.getDuration();
				toneMap.put(sectionNote.midiNum % 12, newDuration);
			}
		}
		
		return toneMap;
	}
	
	public ChordMark getChordMark() {
		Map<Integer, Double> toneMap = getExtendedToneMap();
		for (Integer midiNum : toneMap.keySet()) {
			System.out.println("\tTone: " + MidiReader.NOTE_NAMES[(midiNum % 12)] + ", length: " + toneMap.get(midiNum));
		}
		
		
		
		return Chord.getChord(toneMap);
	}

	public double getChordTonesWeight(ChordMark sectionChordMark) {
		Map<Integer, Double> toneMap = getExtendedToneMap();
		double weight = 0.0;
		
		for(Integer chordMidiNum: sectionChordMark.getChordMidiNums()) {
			if(toneMap.keySet().contains(chordMidiNum)) {
				weight += toneMap.get(chordMidiNum);
			}
		}
		return weight;
	}

	public double getNonChordTonesWeight(ChordMark sectionChordMark) {
		Map<Integer, Double> toneMap = getExtendedToneMap();
		List<Integer> chordMidiNums = Arrays.asList(sectionChordMark.getChordMidiNums());
		double weight = 0.0;
		
		for(Integer sectionMidiNum: toneMap.keySet()) {
			// only add if the section midi num is not contained in chord midi num list
			if(! chordMidiNums.contains(sectionMidiNum)) {
				weight += toneMap.get(sectionMidiNum);
			}
		}
		
		return weight;
	}

	/* cuts one MeasureSection of length shortestNoteDuration from Measure section, returns trimmed section */
	public MeasureSection cutFromBegining(double shortestNoteDuration) {
		double startSection = this.startBeat + shortestNoteDuration;
		double endSection = this.endBeat;
		
		if(startSection < endSection) {
			return new MeasureSection(this.measure, startSection, endSection);
		}
		
		return null;
	}

	public double getDuration() {
		return this.endBeat - this.startBeat;
	}
	
	@Override
	public String toString() {
		return "(" + this.measure.order  + ") MS: " + this.startBeat + " - " + this.endBeat + " : " + this.getDuration();
	}
	
	
}
