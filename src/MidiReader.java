import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;


class RawNote {
	long startTick;
	long endTick;
	int midiNum;
	public static int resolution;

	int track;

	int startMeasure, endMeasure;
	
	protected RawNote clone() {
		return new RawNote(this.startTick,this.endTick,this.midiNum,this.track,RawNote.resolution);
	}

	public RawNote(long startTick, long endTick, int midiNum, int track, int resolution) {
		this.startTick = startTick;
		this.endTick = endTick;
		this.midiNum = midiNum;
		this.track = track;
		RawNote.resolution = resolution;
	}
	
	@Override
	public String toString() {
		return track
		+ " "
		+ MidiReader.NOTE_NAMES[midiNum % 12]
		+ (midiNum / 12 - 1)
		+ " "
		+ startTick
		+ " "
		+ endTick
		+ " "
		+ getDuration() + " s"
				+ startMeasure + " e" + endMeasure;
	}
	
	public double getDuration() {
		return ((endTick - startTick + 1) / (double) resolution);
	}
    
	/*
	 * Returns true if this is part of measureNote
	 */
	// TODO pridat toleranciu
	public boolean isPartOf(RawNote measureNote) {
		boolean isPartOfNote = this.startTick >= measureNote.startTick;
		isPartOfNote &= this.endTick <= this.endTick;
		isPartOfNote &= this.midiNum == measureNote.midiNum;
		isPartOfNote &= this.track == measureNote.track;
		
		return isPartOfNote;
	}
}

class TimeSignatureChange {
	int nominator, denominator;
	long tick;

	public TimeSignatureChange(long tick, int nominator, int denominator) {
		this.nominator = nominator;
		this.denominator = denominator;
		this.tick = tick;
	}
}

class Measure {
	int numBeats;
	int order;
	long startTick;
	long endTick;

	static int resolution;

	List<RawNote> notes = new LinkedList<RawNote>();
	List<RawNote> subNotes = new LinkedList<RawNote>();
	List<MeasureSection> measureSections = new LinkedList<MeasureSection>();

	Measure(int numBeats, int order, long startTick, long endTick,
			int resolution) {
		this.numBeats = numBeats;
		this.order = order;
		this.startTick = startTick;
		this.endTick = endTick;
		Measure.resolution = resolution;
	}
	
	/**
	 * This adds new measure section to measure, but also joins with the last one added
	 * if it has the same chord
	 * */
	public void addMeasureSection(MeasureSection measureSection) {
		System.out.println("\tmeasureSection added, chord: " + measureSection.chordMark);
		
		ChordMark lastAddedChordMark = null;
		
		MeasureSection lastAddedSection = null;
		if(measureSections.size() > 0) {
			lastAddedSection = measureSections.get(measureSections.size() - 1);
			lastAddedChordMark = lastAddedSection.getChordMark();
		}
		
		
		ChordMark measSectionChordMark = measureSection.getChordMark();
		
		// Join if same as on previous
		if( (lastAddedSection != null) &&
		    ( (lastAddedChordMark == null && measSectionChordMark == null) || 
		      (lastAddedChordMark != null && lastAddedChordMark.equals(measSectionChordMark))
		    )
		  ) {
			System.out.println("LAST: \n" + lastAddedSection);
			System.out.println("TO_BE_JOINED: \n" + measureSection);
			MeasureSection joinedSection = lastAddedSection.merge(measureSection);
			measureSections.set(measureSections.size() - 1, joinedSection);
			System.out.println("JOINED: \n" + lastAddedSection);
		// otherwise just add
		} else {
			measureSections.add(measureSection);	
		}
	}
	
	public RawNote getShortestNote() {
		double minNoteDuration = Double.MAX_VALUE;
		RawNote minDurationNote = null;
		
		//System.out.println("Measure: " + this.order);
		
		for (RawNote note : notes) {
			//System.out.println("\ntesting note: " + note);
			if(note.getDuration() < minNoteDuration) {
				minNoteDuration = Math.min(minNoteDuration, note.getDuration());
				minDurationNote = note;
			}			
		}
		
		return minDurationNote;
	}

	public double getBeatCount() {
		return  ((endTick - startTick + 1) / (double) resolution);
	}

	@Override
	public String toString() {
		//if(order == 28) System.exit(1);
		return "Measure #" + order + " beats#" + getBeatCount();
	}

	public void addNote(RawNote note) {
		notes.add(note);
	}

		/*
		 * Returns 1/1, 1/2, 1/4, 1/8, 1/16 or 1/32 according to the nearest duration of note
		 * with minimal duration in the measure.
		 * 
		 * e.g. for note with duration 1/18 = 0.0555 (closest to 1/16), returns 1/16 
		 */
		public double getMinimalStepForMeasure() {
			Measure measure = this;
			
			RawNote minNote = measure.getShortestNote();
			double minNoteDuration = minNote.getDuration();
			
			int[] denominators = {1,2,4,8,16,32};
			
			double minDifference = Double.MAX_VALUE;
			int minDenominator = Integer.MAX_VALUE;
			
			for(int i=0; i<denominators.length; i++) {
				int denominator = denominators[i];
				
				double difference = Math.abs((4.0 * 1.0 / denominator) - minNoteDuration);
				
				if(difference < minDifference) {
					minDifference = difference;
					minDenominator = denominator;
				}
			}
	//		System.out.println("\tMin Difference: " + minDifference);
			
			return 4.0 * 1.0 / minDenominator;
		}

	public static int getNumBeats(int nominator, int denominator) {
		double ratio = (4.0 / denominator) * nominator;
		return (int) Math.ceil(ratio);
	}
}

class Beat {
	long startTick, endTick;
	int measure;
	int orderInMeasure;
	int orderInSong;
	Scale[] scales = new Scale[2];
	static Song song;
	
	ChordMark chordMark = null;
	
	List<RawNote> notes = new LinkedList<RawNote>();
	//Map<Integer, Double> toneMap = new HashMap<Integer, Double>();
	
	public Scale getScale() {
		return scales[0];
	}
	
	public Beat(int measure, long startTick, long endTick, int orderInMeasure, int orderInSong) {
		this.measure = measure;
		this.startTick = startTick;
		this.endTick = endTick;
		this.orderInMeasure = orderInMeasure;
		this.orderInSong = orderInSong;
	}
	
	public void addNote(RawNote note) {
		notes.add(note);
	}
	
	public Map<Integer, Double> getToneMap() {
		Map<Integer, Double> toneMap = new HashMap<Integer, Double>();
		
		for(RawNote note: notes) {
			int toneId = note.midiNum % 12;
			double toneDur = note.getDuration();
			
			Double value = toneMap.get(toneId);
			if(value == null) {
				toneMap.put(toneId, toneDur);
			} else {
				toneMap.put(toneId, toneDur + value);
			}
		}
		
		return toneMap;
	}
	
	// obsahuje noty z predchadzajucich dob?
	public boolean containsForeignNotes() {
		for (RawNote note: notes) {
			// ak je pred aktualnou dobou, teda bola pripocitana z predchadzajucej doby
			if(note.startTick < this.startTick) {
				return true;
			}
		}
		return false;
	}
	
	public List<RawNote> getForeignNotes() {
		List<RawNote> foreignNotes = new LinkedList<RawNote>();
		for (RawNote note: notes) {
			// ak je pred aktualnou dobou, teda bola pripocitana z predchadzajucej doby
			if(note.startTick < this.startTick) {
				foreignNotes.add(note);
			}
		}
		return foreignNotes;
	}
	
	public String toShortString() {
		return "\t\tBeat - m:" + (measure + 1) + " o:" + (orderInMeasure + 1) + " ois:" + (orderInSong + 1) + "\n";
	}
	
	@Override
	public String toString() {
		String out = "\t\tBeat - m:" + (measure + 1) + " o:" + (orderInMeasure + 1) + " ois:" + (orderInSong + 1) + "\n";
		for (int i = 0; i < notes.size(); i++) {
			RawNote n = notes.get(i);
			out += "\t\t" + n.toString() + "\n";
		}
		out += "\n";
		
		Set<Integer> midiNums = getToneMap().keySet();
		for (Iterator<Integer> iterator = midiNums.iterator(); iterator.hasNext();) {
			Integer midiNum = iterator.next();
			Double duration = getToneMap().get(midiNum);
			
			String toneName = MidiReader.NOTE_NAMES[midiNum];
			out += "\t\t" + midiNum + " " + toneName + " d: " + duration + "\n";
		}
		
		return out;
	}
	
	public static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
        Set<Set<T>> sets = new HashSet<Set<T>>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<T>());
            return sets;
        }
        
        List<T> list = new ArrayList<T>(originalSet);
        T head = list.get(0);
        Set<T> rest = new HashSet<T>(list.subList(1, list.size()));
        for (Set<T> set : powerSet(rest)) {
            Set<T> newSet = new HashSet<T>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }
	
	public Set<Set<Integer>> getAllToneSubSets() {
		return powerSet(getToneMap().keySet());
	}
	
	public void setChordMark(ChordMark chordMark) {
		this.chordMark = chordMark;
	}
	
	public ChordMark getChordMark() {
		return chordMark;
	}
	
	public Beat getPreviousBeat() {
		if ((orderInSong - 1) > 0) {
			return song.beats.get(orderInSong - 1);
		}
		return null;
	}
	
	public Scale getPreviousScale() {
		Scale previousScale = null;
		
		Beat previousBeat = getPreviousBeat();
		while( (previousBeat != null) && (previousBeat.scales[0] == null) ) {
			previousBeat = previousBeat.getPreviousBeat();
		}
		
		if((previousBeat != null) && (previousBeat.scales != null) && (previousBeat.scales[0]) != null) {
			previousScale = previousBeat.scales[0]; 
		}
		
		return previousScale;
	}
	
	public boolean isFarScale(Scale scale) {
		Scale previousScale = getPreviousScale();
		
		if(scale.scaleType == ScaleType.DUR_MOLL || previousScale.scaleType == ScaleType.DUR_MOLL) {
			return false;
		}
		
		List<Integer> thisScaleTonesList = Arrays.asList(scale.getMidiTones());
		Integer[] prevScaleTones = previousScale.getMidiTones();
		
		int numIntersecting = 0;
		
		for (Integer prevScaleTone : prevScaleTones) {
			if(thisScaleTonesList.contains(prevScaleTone)) {
				numIntersecting++;
			}
		}
		
		return (numIntersecting <= 3);
		
	}
	
	
	// TODO Ak pri priradovani toniny naspat pozerame vsetky tony, tj. nie je akord, nemusime sa zastavit, zastavime sa az na takej dobe, kde je akord a zaroven nejaky akordicky ton nepatri do toniny
	// potom ani doba, ktoru sme preskocili nepatri do toniny
	// ak sme preskocili nejaku dobu, kde sme nasli neakordicky ton, ktory nepatri do toniny ale po preskoceni sme nasli dobu s akordom, kde vsetky tony patria do toniny potom prehlasime cely usek za taku toninu a pokracujeme v hladani dalej, tj. ak na dobe s akordom nebol urcena tonina
	public void setScale(Scale scale, Song song) {
		
		this.scales[0] = scale;
		
		System.out.println("MidiReader.setScale: \t Setting scale " + scale +  " to beat: " + this.toShortString());
		
		// nie je tonina prilis vzdialena
		Scale previousScale = getPreviousScale();
		
			
		if(previousScale == null || scale == null) {
			return;
		}
		
		if(isFarScale(scale)) {
			scale.setFar(true);
			System.out.println("\tThe scale is FAR");
		}
		
		
		if(scale.scaleType.equals(ScaleType.DUR_MOLL)) {
			return;
		}
		
		System.out.println("\t\tTry to set the scale to previous beats, if they have all chord tones in");
		
		for (int i = this.orderInSong - 1; i > 0; i--) {
			Beat prevBeat = song.beats.get(i);
			
			System.out.println("\t\t\tTry to set to beat: " + prevBeat.toString().split("\n")[0]);
			
			// ak mame na predchadzajucej dobe neurcenu toninu
			if(prevBeat.scales[0] == null) {				
				// ak mame akordicke tony
				if(prevBeat.getChordMark() != null) {
					// pozri ci su vsetky obsiahnute v urcenej tonine
					Integer[] chordMidis = prevBeat.getChordMark().getChordMidiNums();
					boolean areAllIn = true;
					
					for (int chordMidi : chordMidis) {
						if(!scale.isInScaleTones(chordMidi)) {
							areAllIn = false;
							break;
						}
					}
					
					if(!areAllIn) {
						return; // nenastav toninu
					} else {
						prevBeat.scales[0] = scale;
						System.out.println("\t\t\t\tSame scale " + scale +" on previous beat: " + prevBeat.toShortString());
					}
				} else { // ak nemame akordicke tony, vyskusaj ci su vsetky tony v stupnici
					Set<Integer> beatMidis = prevBeat.getToneMap().keySet();
					
					boolean areAllIn = true;
					
					for (int beatMidi : beatMidis) {
						if(!scale.isInScaleTones(beatMidi)) {
							areAllIn = false;
							break;
						}
					}
					
					if(!areAllIn) {
						return; // nenastav toninu
					} else {
						prevBeat.scales[0] = scale;
						System.out.println("\t\t\t\tSame scale " + scale +" on previous beat: " + prevBeat);
					}
				}
			} else { // ak mame urcenu toninu potom skonci, nie je preco priradovat
				return;
			}
		}

	}

}

class SubBeat extends Beat {

	public SubBeat(int measure, long startTick, long endTick,
			int orderInMeasure, int orderInSong) {
		super(measure, startTick, endTick, orderInMeasure, orderInSong);
		// TODO Auto-generated constructor stub
	}
}

class Song {
	List<Measure> measures;
	List<Beat> beats;
	List<SubBeat> subBeats;

	public Song(List<Measure> measures, List<Beat> beats, List<SubBeat> subBeats) {
		this.measures = measures;
		this.beats = beats;
		this.subBeats = subBeats;
	}

	public Beat getNextBeat(Beat fromBeat) {
		return ( (fromBeat.orderInMeasure + 1) < this.beats.size() ) ? ( this.beats.get(fromBeat.orderInSong + 1) ): null;
	}
}

public class MidiReader {
	String fileName;
	Sequence sequence;
	public static final int NOTE_ON = 0x90, NOTE_OFF = 0x80, TIME_SIG = 0x58;
	public static final String[] NOTE_NAMES = { "C", "C#", "D", "D#", "E", "F",
			"F#", "G", "G#", "A", "A#", "B" };
	public static int tickCorrection;
	public static int noteRes;
	List<TimeSignatureChange> timeSignatureChanges = new LinkedList<TimeSignatureChange>();
	
	public static final double minNoteDuration = 4.0 * 1.0/32.0; // minimalna dlzka je 1/32 nota, tj. 0.125 doby

	public MidiReader(String fileName, int tickCorrection, int noteRes) {
		this.fileName = fileName;
		MidiReader.tickCorrection = tickCorrection;
		MidiReader.noteRes = noteRes;
	}

	public Song read() {
		List<RawNote> noteBuffer = new LinkedList<RawNote>();
		List<RawNote> noteList = new LinkedList<RawNote>();

		try {
			sequence = MidiSystem.getSequence(new File(this.fileName));
		} catch (InvalidMidiDataException e) {
			System.err.println("Midi file corrupted!");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("File not found or unreadable!");
			e.printStackTrace();
		}

		System.out.println("Midi summary: ");
		System.out.println("Resolution: " + sequence.getResolution());
		System.out.println("Type: " + sequence.getDivisionType());

		int noteNum = 0;
		int trackNumber = 0;
		for (Track track : sequence.getTracks()) {
			trackNumber++;
			for (int i = 0; i < track.size(); i++) {
				MidiEvent event = track.get(i);
				MidiMessage message = event.getMessage();

				if (message instanceof ShortMessage) {
					ShortMessage sm = (ShortMessage) message;

					if (sm.getCommand() == NOTE_ON
							|| sm.getCommand() == NOTE_OFF) {
						int key = sm.getData1();
						int velocity = sm.getData2();

						if (sm.getCommand() == NOTE_ON && velocity > 0) {
							noteBuffer.add(new RawNote(event.getTick(), -1,
									key, trackNumber, sequence.getResolution()));
						} else if ((sm.getCommand() == NOTE_ON && velocity == 0)
								|| (sm.getCommand() == NOTE_OFF)) {
							if (sm.getCommand() == NOTE_OFF)
								System.out.println("NOFF");
							int noteIndex = -1;
							for (int j = 0; j < noteBuffer.size(); j++) {
								RawNote rawNote = noteBuffer.get(j);
								if (rawNote.midiNum == key) {
									noteIndex = j;
									break;
								}
							}
							if (noteIndex == -1) {
								throw new RuntimeException("Tone not terminated");
							}
							RawNote endedNote = noteBuffer.remove(noteIndex);
							endedNote.endTick = event.getTick()
									+ tickCorrection;
							noteList.add(endedNote);
							// System.out.println(endedNote);
							noteNum++;
						}
					}
				} else {
					if (event.getMessage() instanceof MetaMessage) {
						MetaMessage metaMsg = (MetaMessage) event.getMessage();
						if (metaMsg.getType() == TIME_SIG) { // time signature
							// System.out.println(Arrays.toString(metaMsg.getData()));
							byte[] data = metaMsg.getData();
							byte nominator = data[0];
							byte denominator = (byte) (1 << data[1]);
							TimeSignatureChange tsch = new TimeSignatureChange(
									event.getTick(), nominator, denominator);
							timeSignatureChanges.add(tsch);
							// System.out.println("@" + event.getTick() + " " +
							// nominator + "/" + denominator);
						}
					}
				}

				// System.out.println();
			}
		}
		System.out.println("Note number: " + noteNum);
		// docitali sme noty
		// vytvor prazdne takty

		// vypis zmeny dob v taktu
		for (int i = 0; i < timeSignatureChanges.size(); i++) {
			TimeSignatureChange tsc = timeSignatureChanges.get(i);
			System.out.println("Time signature change #" + i + " at: "
					+ tsc.tick);
		}

		List<Long> measureStarts = new ArrayList<Long>(100);

		// nasimuluj 4/4 takt
		if (timeSignatureChanges.size() == 0) {
			timeSignatureChanges.add(new TimeSignatureChange(0, 4, 4));
		}

		long numTicks = sequence.getTickLength();

		if (timeSignatureChanges.size() == 1) {
			TimeSignatureChange tsc = timeSignatureChanges.get(0);
			double measNumBeats = Measure.getNumBeats(tsc.nominator,
					tsc.denominator);
			// System.out.println("Number of beats per measure: " +
			// measNumBeats);

			long measCount = (long) Math.ceil(numTicks
					/ (sequence.getResolution() * measNumBeats));
			for (long i = 0; i < measCount; i++) {
				long measPos = (long) Math.ceil(i * sequence.getResolution()
						* measNumBeats);
				measureStarts.add(measPos);
				// System.out.println("Measure #" + i + " start: " + measPos +
				// "/" + (measPos/sequence.getResolution()));
			}
		} else {
			//int measOrder = 0;
			for (int i = 1; i < timeSignatureChanges.size(); i++) {
				TimeSignatureChange curTsc = timeSignatureChanges.get(i);
				TimeSignatureChange prevTsc = timeSignatureChanges.get(i - 1);
				double measNumBeats = Measure.getNumBeats(prevTsc.nominator,
						prevTsc.denominator);

				System.out.println("N/D: " + prevTsc.nominator + "/"
						+ prevTsc.denominator + " = " + measNumBeats);

				long curNumTicks = curTsc.tick - prevTsc.tick;

				int measCount = (int) Math.ceil(curNumTicks
						/ (sequence.getResolution() * measNumBeats));
				for (int j = 0; j < measCount; j++) {
					long measPos = (long) Math.ceil(j
							* sequence.getResolution() * measNumBeats);
					measPos += prevTsc.tick;
					measureStarts.add(measPos);
					// System.out.println("Measure #" + measOrder + " start: " +
					// measPos + "/" + (measPos/sequence.getResolution()));
					//measOrder++;
				}
			}

			TimeSignatureChange lastTsc = timeSignatureChanges
					.get(timeSignatureChanges.size() - 1);
			long curNumTicks = sequence.getTickLength() - lastTsc.tick;
			double measNumBeats = Measure.getNumBeats(lastTsc.nominator,
					lastTsc.denominator);

			int measCount = (int) Math.ceil(curNumTicks
					/ (sequence.getResolution() * measNumBeats));
			for (int j = 0; j < measCount; j++) {
				long measPos = (long) Math.ceil(j * sequence.getResolution()
						* measNumBeats);
				measPos += lastTsc.tick;
				measureStarts.add(measPos);
				//measOrder++;
			}
		}

		// vypis info o taktoch
		for (int i = 0; i < measureStarts.size(); i++) {
			long mStart = measureStarts.get(i);
			long mNextStart = (i + 1 < measureStarts.size()) ? measureStarts
					.get(i + 1) : sequence.getTickLength();
			System.out.println("Measure #" + i + " start: " + mStart + "/"
					+ (mStart / sequence.getResolution()) + " "
					+ ((mNextStart - mStart) / sequence.getResolution()));
		}

		System.out.println("\nNotes before division to measures:");
		// urci notam zaciatocny a konecny takt
		for (RawNote note : noteList) {
			for (int i = 0; i < measureStarts.size(); i++) {
				long mStart = measureStarts.get(i);
				// nastav startMeasure
				if (note.startTick >= mStart && i + 1 < measureStarts.size()
						&& note.startTick < measureStarts.get(i + 1)) {
					note.startMeasure = i;
				} else if (note.startTick >= mStart
						&& i + 1 == measureStarts.size()) {
					note.startMeasure = i;
				}

				// nastav endMeasure
				if (note.endTick >= mStart && i + 1 < measureStarts.size()
						&& note.endTick < measureStarts.get(i + 1)) {
					note.endMeasure = i;
				} else if (note.endTick >= mStart
						&& i + 1 == measureStarts.size()) {
					note.endMeasure = i;
				}

			}
			System.out.println(note);
		}
		System.out.println("Note count: " + noteList.size());

		List<RawNote> newNotes = new LinkedList<RawNote>();
		List<RawNote> notesToRemove = new LinkedList<RawNote>();

		for (int i = 0; i < noteList.size(); i++) {
			RawNote note = noteList.get(i);
			// ak ich treba delit

			if (note.startMeasure < note.endMeasure) {
				for (int j = note.startMeasure; j <= note.endMeasure; j++) {
					long mStart = measureStarts.get(j);

					long noteStartInMeas = Math.max(note.startTick, mStart);
					long noteEndInMeas = (j + 1 < measureStarts.size()) ? Math
							.min(note.endTick, measureStarts.get(j + 1) - 1)
							: note.endTick;
					RawNote newNote = new RawNote(noteStartInMeas,
							noteEndInMeas, note.midiNum, note.track, sequence.getResolution());
					newNote.startMeasure = j;
					newNote.endMeasure = j;

					newNotes.add(newNote);
				}
				notesToRemove.add(note);
			}
		}

		noteList.removeAll(notesToRemove);
		noteList.addAll(newNotes);

		System.out.println("\nNotes after division to measures:");
		for (int i = 0; i < noteList.size(); i++) {
			RawNote note = noteList.get(i);
			System.out.println(note);
		}
		System.out.println("Note count: " + noteList.size());

		// Vytvor takty
		List<Measure> measures = new ArrayList<Measure>(15);
		for (int i = 0; i < measureStarts.size(); i++) {
			long mStart = measureStarts.get(i);
			long mNextStart = (i + 1 < measureStarts.size()) ? measureStarts
					.get(i + 1) : sequence.getTickLength();

			int numBeats = (int) ((mNextStart - mStart) / sequence
					.getResolution());

			Measure measure = new Measure(numBeats, i, mStart, mNextStart - 1,
					sequence.getResolution());
			// pridaj noty
			for (int j = 0; j < noteList.size(); j++) {
				RawNote note = noteList.get(j);
				if (note.startMeasure == i) {
					if(note.getDuration() >= minNoteDuration) {
						measure.addNote(note);	
					}					
				}
			}
			measures.add(measure);
		}

		System.out.println("\nMeasures:");
		for (int i = 0; i < measures.size(); i++) {
			Measure meas = measures.get(i);
			System.out.println(meas);
			List<RawNote> notes = meas.notes;

			for (RawNote note : notes) {
				System.out.println("\t" + note);
			}
		}
		
		// rozbi noty na poddoby (napr. 1/8, 1/16 atd)
		List<SubBeat> subBeats = new LinkedList<SubBeat>();
		int subBeatOrderGlobal = 0;
		
		for (int m = 0; m < measures.size(); m++) {
			Measure meas = measures.get(m);
			int subBeatOrderMeasure = 0;
			
			int subBeatDenominator = 8; // urcuje 1/n, tj. ci ide o 1/4, 1/8 notu a podobne
			
			List<RawNote> notes = meas.notes;
			
			int ratio = subBeatDenominator / 4;
			
			
			for (int subBeat = 0; subBeat < meas.getBeatCount()*ratio; subBeat++) {
				long subBeatStart = meas.startTick + (Measure.resolution/ratio) * subBeat;
				long subBeatEnd = subBeatStart + (Measure.resolution/ratio) - 1;
				
				SubBeat sbeat = new SubBeat(m, subBeatStart, subBeatEnd, subBeatOrderMeasure, subBeatOrderGlobal);
				
				for (RawNote note : notes) {
					boolean isInSubBeat = (note.startTick >= subBeatStart)
							&& (note.endTick <= subBeatEnd);
					isInSubBeat |= (note.startTick >= subBeatStart)
							&& (note.startTick <= subBeatEnd)
							&& (note.endTick >= subBeatEnd);
					isInSubBeat |= (note.startTick <= subBeatStart)
							&& (note.endTick >= subBeatStart)
							&& (note.endTick <= subBeatEnd);
					isInSubBeat |= (note.startTick <= subBeatStart)
							&& (note.endTick >= subBeatEnd);
					
					if (isInSubBeat) {
						//System.out.println(getNoteInfo(note));
						long newNoteStart = Math.max(subBeatStart, note.startTick);
						long newNoteEnd = Math.min(subBeatEnd, note.endTick);

						RawNote newNote = new RawNote(newNoteStart, newNoteEnd,
								note.midiNum, note.track, sequence.getResolution());
						newNote.startMeasure = newNote.endMeasure = note.startMeasure;

						// System.out.println(getNoteInfo(newNote));

						meas.subNotes.add(newNote);

						sbeat.addNote(newNote);
					}
				}
				// subBeat dokonceny
				
				subBeats.add(sbeat);
								
				subBeatOrderGlobal++;
				subBeatOrderMeasure++;
			}			
		}
		
		// rozbi noty na doby
		for (int i = 0; i < measures.size(); i++) {
			Measure meas = measures.get(i);
			
			// skopiruj zoznam aj jednotlive noty
			List<RawNote> notes = new LinkedList<RawNote>(meas.notes);
			for (RawNote note : notes) {
				note = note.clone();
			}
			

			List<RawNote> notesToAdd = new LinkedList<RawNote>();
			notesToRemove = new LinkedList<RawNote>();

			for (RawNote note : notes) {
				for (int beat = 0; beat < meas.getBeatCount(); beat++) {
					long beatStart = meas.startTick + Measure.resolution * beat;
					long beatEnd = beatStart + Measure.resolution - 1;
					// System.out.println("t:" + i + " bs: " + beatStart +
					// " be: " + beatEnd);
					// ak je v dobe
					boolean isInBeat = (note.startTick >= beatStart)
							&& (note.endTick <= beatEnd);
					isInBeat |= (note.startTick >= beatStart)
							&& (note.startTick <= beatEnd)
							&& (note.endTick >= beatEnd);
					isInBeat |= (note.startTick <= beatStart)
							&& (note.endTick >= beatStart)
							&& (note.endTick <= beatEnd);
					isInBeat |= (note.startTick <= beatStart)
							&& (note.endTick >= beatEnd);
					
					if (isInBeat) {
						//System.out.println(getNoteInfo(note));
						long newNoteStart = Math.max(beatStart, note.startTick);
						long newNoteEnd = Math.min(beatEnd, note.endTick);

						RawNote newNote = new RawNote(newNoteStart, newNoteEnd,
								note.midiNum, note.track, sequence.getResolution());
						newNote.startMeasure = newNote.endMeasure = note.startMeasure;

						// System.out.println(getNoteInfo(newNote));

						notesToAdd.add(newNote);
						notesToRemove.add(note);
						

					}

				}
			}
			
			notes.addAll(notesToAdd);
			notes.removeAll(notesToRemove);
		}

		System.out.println("\nMeasures:");
		for (int i = 0; i < measures.size(); i++) {
			Measure meas = measures.get(i);
			System.out.println(meas);
			List<RawNote> notes = meas.notes;

			for (RawNote note : notes) {
				System.out.println("\t" + note);
			}
		}
		
		int numBeatsInSong = (int) (sequence.getTickLength() / sequence.getResolution());
		List<Beat> beats = new ArrayList<Beat>(numBeatsInSong);
		int beatOrderGlobal = 0;
		
		for(int i=0; i<measures.size(); i++) {
			Measure meas = measures.get(i);
			
			for (int b = 0; b < meas.numBeats; b++) {
				long beatStart = meas.startTick + Measure.resolution * b;
				long beatEnd = beatStart + Measure.resolution - 1;
				Beat beat = new Beat(i, beatStart, beatEnd, b, beatOrderGlobal);
				beats.add(beat);
				beatOrderGlobal++;

				for(RawNote note: meas.notes) {

					// System.out.println("t:" + i + " bs: " + beatStart +
					// " be: " + beatEnd);
					// ak je v dobe
					boolean isInBeat = (note.startTick >= beatStart)
							&& (note.endTick <= beatEnd);
					isInBeat |= (note.startTick >= beatStart)
							&& (note.startTick <= beatEnd)
							&& (note.endTick >= beatEnd);
					isInBeat |= (note.startTick <= beatStart)
							&& (note.endTick >= beatStart)
							&& (note.endTick <= beatEnd);
					isInBeat |= (note.startTick <= beatStart)
							&& (note.endTick >= beatEnd);
					
					if(isInBeat) {
						beat.addNote(note);
					}
				}
			}
		}
		
		// odstrani male noty
		for (Beat beat : beats) {
			notesToRemove.clear();
			List<RawNote> beatNotes = beat.notes;
			for (RawNote note : beatNotes) {
				if(note.getDuration() < (1.0/128.0)) {
					notesToRemove.add(note);
				}
			}
			beatNotes.removeAll(notesToRemove);
		}
		
		for (int i = 0; i < beats.size(); i++) {
			Beat beat = beats.get(i);
			if(i-1 >= 0) {
				if(beats.get(i-1).measure != beat.measure) {
					System.out.println((beat.measure + 1) + "----------------------------------------------");
				}
			}			
			System.out.println(beat.orderInSong + " " + beat.orderInMeasure + " " + beat.startTick + " " + beat.endTick);
			
			for(int j=0; j<beat.notes.size();j++) {
				RawNote note = beat.notes.get(j);
				System.out.println("\t" + note);
			}
		}
		
		for (int i = 0; i < subBeats.size(); i++) {
			SubBeat subBeat = subBeats.get(i);
			if(i-1 >= 0) {
				if(subBeats.get(i-1).measure != subBeat.measure) {
					System.out.println((subBeat.measure + 1) + "----------------------------------------------");
				}
			}			
			System.out.println(subBeat.orderInSong + " " + subBeat.orderInMeasure + " " + subBeat.startTick + " " + subBeat.endTick);
			
			for(int j=0; j<subBeat.notes.size();j++) {
				RawNote note = subBeat.notes.get(j);
				System.out.println("\t" + note);
			}
		}
		
		

		Song song = new Song(measures, beats, subBeats);
		
		return song;
	}

	/*private long normalizeTickEnd(long tickEnd) {
		System.out.println("Tick bef: " + tickEnd);
		int beatTicksNum = this.sequence.getResolution();
		double shortestTicksNum = beatTicksNum / MidiReader.noteRes;
		
		double shortestInTickEnd = tickEnd / shortestTicksNum;
		long intShortestInTickEnd = (long) shortestInTickEnd;
		
		if(shortestInTickEnd - intShortestInTickEnd >= 0.5) {
			System.out.println("Tick aft: " + (long)((intShortestInTickEnd + 1) * shortestTicksNum));
			return (long)((intShortestInTickEnd + 1) * shortestTicksNum);
		}
		System.out.println("Tick aft: " + (long)((intShortestInTickEnd) * shortestTicksNum));

		return (long) (intShortestInTickEnd * shortestTicksNum);
	}*/

	public static void main(String[] args) {
		MidiReader mr = new MidiReader("sonataFDurMozart.mid", -1, 32);
		mr.read();
	}
}
