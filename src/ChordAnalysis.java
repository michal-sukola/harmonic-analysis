import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChordAnalysis {
	Song song;
	int currentBeat=-1;
	
	public static PrintStream oldOut;
	
	private Beat getCurrentBeat() {
		return song.beats.get(currentBeat);
	}
	
	public ChordAnalysis(String midiFile) {
		try {
			oldOut = System.out;
			PrintStream ps = new PrintStream(new FileOutputStream("NUL:"));
			System.setOut(ps);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		MidiReader mr = new MidiReader(midiFile, -1, 32);
		song = mr.read();
		
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("chord_out.txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.setOut(new PrintStream(fos));
	}
	
	public void process() {
		step1();
		
		System.setOut(oldOut);
	}
	
	private void step1() {
		System.out.println("1: Initialization of Chord Analysis");
		step2();
	}
	
	private void step2() {
		System.out.println("2: Read next tones from ");
		
		if(currentBeat + 1 > song.beats.size() - 1) {
			step5();
			return;
		}
		
		currentBeat++;
		System.out.println("\tBeat: " + currentBeat);
		
		step3(getCurrentBeat());
	}
	
	private void step3(Beat beat) {
		System.out.println("3: Are there any tones in current beat?");
		if(beat.notes.size() > 0) {
			System.out.println("\tYes - going to step 6");
			System.out.println(beat);
			step6_7(beat);
		} else {
			System.out.println("\tNo - going to step 4");
			step4();
		}
	}
	
	private void step4() {
		System.out.println("4: Is there any tone before end of song?");
		boolean isTone = false;
		for (int i = currentBeat+1; i < song.beats.size(); i++) {
			Beat b = song.beats.get(i);
			if(b.notes.size() > 0) {
				isTone = true;
				break;
			}
			
		}
		
		if(isTone) {
			System.out.println("\tYes - going to step 2");
			step2();
		} else {
			System.out.println("\tNo - going to step 5");
			step5();
		}
	}
	
	private void step5() {
		List<Beat> beats = song.beats;
		
		for (int i = 0; i < beats.size(); i++) {
			Beat b = beats.get(i);
			
			if(b.containsForeignNotes() && (b.chordMark != null) ) {
				List<RawNote> foreignNotes = b.getForeignNotes();
				
				Integer[] chordTones = b.chordMark.getChordMidiNums();
				for (RawNote forNote : foreignNotes) {
					for (int chordTone : chordTones) {
						// ak je ton sucastou akordu
						if((forNote.midiNum % 12) == chordTone) {
							// potom zisti z ktorej je doby
							int beatNo = (int)(((double) forNote.startTick) / RawNote.resolution);
							Beat origBeat = song.beats.get(beatNo);
							
							// ak na tejto dobe nie je akord prirad akord z aktualnej doby
							if(origBeat.chordMark == null) {
								//oldOut.println("Step5, meas: " + origBeat.measure + ", oim:" + origBeat.orderInMeasure + ", ois: " + origBeat.orderInSong);
								origBeat.chordMark = new ChordMark(b.chordMark);
							}
						}
					}					
				}
				
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void step5old() {
		System.out.println("5a: Try to complete chords where tones were put into next beat");
		
		List<Beat> beats = song.beats;
		/*for (int i = 0; i < beats.size(); i++) {
			Beat b = beats.get(i);
			
			// dopln ak sme do tejto doby prenasali a nasli sme akord obsahujuci aspon jeden z prenasanych tonov
			if(b.containsForeignNotes() && (b.chordMark != null) &&
				b.chordMark.getContainedTonesCount(b.getToneMap()) > 0) {
				
				// musi byt predchadzajuca, kedze sme prenasali
				assert i-1 >= 0;
				// dopln na predchadzajucu ak tu sme nasli nejaky akord
				if(b.chordMark == null || b.chordMark.isIncomplete()) {
					Beat previousBeat = beats.get(i-1);
					previousBeat.setChordMark(b.chordMark);
				}
			}			
		}*/
		
		System.out.println("5b: Try to complete OPEN chords");
		
		//if(true) { return; }
		
		beatsLoop:
		for (int i = 0; i < beats.size(); i++) {
			Beat b = beats.get(i);
			// TODO Zoradit doby tak aby sa skusali najprv tie co su najblizsie
			if(b.chordMark != null && b.chordMark.isIncomplete()) {
				int measureNo = b.measure;
				Measure measure = song.measures.get(measureNo);
				
				// +++++|++-++ == 7 - 2 = 5
				int measStartBeatId = b.orderInSong - b.orderInMeasure;
				// 5 + 5 - 1 = 10 - 1 = 9
				int measEndBeatId = (int) (measStartBeatId + measure.getBeatCount() - 1);
				
				Map<Integer, Double> sumMap = b.getToneMap();
				
				// pripad 1 - spojenie s dalsim uplnym akordom do konca taktu, medzi nimi nic
				// -O--F- alebo aj --OF-      O open, F full chord, - ziadne tony
				if(measEndBeatId > b.orderInSong) { // iba ak nie je posledny v takte
					// najdi najblizsi plny akord v takte
					int nextBeatId = b.orderInSong + 1;
										
					while(nextBeatId <= measEndBeatId) {
						Beat nextBeat = song.beats.get(nextBeatId);
						
						if(nextBeat.notes.size() == 0) {
							nextBeatId++;
							continue;
						} else { // v skumanej dobe su nejake noty
							if(nextBeat.chordMark == null || nextBeat.chordMark.isIncomplete()) {

								// pripad 2 - OU|      O open, U unknown or open, | taktova ciara
								if(nextBeatId == measEndBeatId) {
									// scitaj O + U a skus ci daju akord
									sumMap = Chord.sumToneMaps(b.getToneMap(), nextBeat.getToneMap());
									ChordMark finalChordMark = Chord.getChord(sumMap, b);
									if(finalChordMark != null) {
										b.chordMark = finalChordMark;
										continue beatsLoop;
									}
								} else { // pripad 3 OU1..U>1
									// skus ci dohromady O + U>1 nedaju akord 
									
									sumMap = Chord.sumToneMaps(b.getToneMap(), nextBeat.getToneMap());
									ChordMark finalChordMark = Chord.getChord(sumMap, b);
									if(finalChordMark != null) {
										b.chordMark = finalChordMark;
										continue beatsLoop;
									}									
								}
								
							} else { // is complete
								
								// priklad 1 - OA
								if(nextBeatId == b.orderInSong + 1) {
									// skus ci vsetky tony z O su v A
									Set<Integer> openChordTones = sumMap.keySet();
									Set<Integer> fullChordTones = nextBeat.getToneMap().keySet();
									
									boolean allInsideFull = true;
									for(Integer openChordTone: openChordTones) {
										allInsideFull &= fullChordTones.contains(openChordTone);
										if(!allInsideFull) {
											break;
										}
									}
									
									if(allInsideFull) {
										b.chordMark = new ChordMark(nextBeat.chordMark);
									}
									
								} else { // priklad 3b - OU..UA pricom OU..U neda spolu akord
									// pozrieme ci z OU..UA O obsahuje vsetky tony z A
									// skus ci vsetky tony z O su v A
									Set<Integer> openChordTones = b.getToneMap().keySet();
									Set<Integer> fullChordTones = nextBeat.getToneMap().keySet();
									
									boolean allInsideFull = true;
									for(Integer openChordTone: openChordTones) {
										allInsideFull &= fullChordTones.contains(openChordTone);
										if(!allInsideFull) {
											break;
										}
									}
									
									if(allInsideFull) {
										b.chordMark = new ChordMark(nextBeat.chordMark);
									}
								}
							}
						}
						
						nextBeatId++;
					}
					
				}
				
			}
			
		}
		
		step21();
	}
	
	private void step6_7(Beat beat) {
		System.out.println("6-7: Find chord");
		
		//List<ChordMark> chords = Chord.getChords(beat);	
		//boolean foundChord = chords.size() > 0;
		ChordMark chordMark = Chord.getChord(beat.getToneMap(), beat);
		boolean foundChord = chordMark != null;
		
		// ak je to zakladny akord tj. +, - tak potom nechaj tak, inak skus co by sa naslo ak by sme rozdrobili dobu na dve mensie
		if(foundChord && !chordMark.getChord().equals(Chord.MAJOR_TRIAD) && !chordMark.getChord().equals(Chord.MINOR_TRIAD)) {
			System.out.println("\tChord is not + or -, it is " + chordMark + "  trying to divide the beat");
			// skus co by bolo na polovicnej dobe
			long beat1StartTick = beat.startTick;
			long beat1EndTick =  beat.startTick + ((beat.endTick - beat.startTick) / 2);
			long beat2StartTick = beat1EndTick + 1;
			long beat2EndTick = beat.endTick;
			
			Beat beat1 = new Beat(beat.measure, beat1StartTick, beat1EndTick, beat.orderInMeasure, beat.orderInSong);
			Beat beat2 = new Beat(beat.measure, beat2StartTick, beat2EndTick, beat.orderInMeasure + 1, beat.orderInSong + 1);
			
			// doba 1.1
			List<RawNote> notesToAdd = new LinkedList<RawNote>();
			for(RawNote n: beat.notes) {
				if(n.startTick >= beat1.startTick && n.startTick <= beat1.endTick) {
					long newNoteStartTick = n.startTick;
					long newNoteEndTick = Math.min(n.endTick, beat1.endTick);
					RawNote newNote = new RawNote(newNoteStartTick, newNoteEndTick, n.midiNum, n.track, RawNote.resolution);
					
					notesToAdd.add(newNote);
				}
			}
			beat1.notes = new LinkedList<RawNote>(notesToAdd);
			
			// doba 1.2
			notesToAdd.clear();
			for(RawNote n: beat.notes) {
				if(n.endTick >= beat2.startTick) {
					long newNoteStartTick = Math.max(beat2.startTick, n.startTick);
					long newNoteEndTick = n.endTick;
					RawNote newNote = new RawNote(newNoteStartTick, newNoteEndTick, n.midiNum, n.track, RawNote.resolution);
					
					notesToAdd.add(newNote);
					
				}
			}
			beat2.notes = new LinkedList<RawNote>(notesToAdd);
			
			ChordMark subChord1 = Chord.getChord(beat1.getToneMap(), beat1);
			System.out.println("Subbeat1: \n" + beat1);
			System.out.println("\n\t\tAvailable chords: " + Chord.getChordsInfo(beat1));
			System.out.println("\t\tChord selected: " + subChord1);
			
			ChordMark subChord2 = Chord.getChord(beat2.getToneMap(), beat2);
			System.out.println("Subbeat2: \n" + beat2);
			System.out.println("\n\t\tAvailable chords: " + Chord.getChordsInfo(beat2));
			System.out.println("\t\tChord selected: " + subChord2);
			
			
			System.out.println("\t\tSubChord1: " + subChord1 + ", SubChord2: " + subChord2 + "\n\t\tmeasure: " + (beat.measure + 1) + ", beat: " + (beat.orderInMeasure + 1));
			
			
		}
		
			
		if(!foundChord) {
			System.out.println("\tNot found going to step 10");
			step10(beat);
		} else {
			System.out.println("\tFound chord " + chordMark);
			
			System.out.println("\tAvailable chords: " + Chord.getChordsInfo(beat));
			
			System.out.print("\tGoing to step 2\n");
			
			// nastav znacku pre dobu
			beat.setChordMark(chordMark);
			
			step2();
		}
	}
	
	private void step10(Beat beat) {
		System.out.println("10: Are there more than 2 tones?");
		int numTones = beat.getToneMap().keySet().size();
		
		if(numTones > 2) {
			System.out.println("\tYes, going to step 14");
			step14(beat);
		} else {
			System.out.println("\tNo, going to step 11");
			step11(beat);
		}
	}
	
	private void step11(Beat beat) {
		System.out.println("11: Is tones' distance 3,4 or 7?");
		int numTones = beat.getToneMap().keySet().size();
		
		if(numTones < 2) {
			System.out.println("\tNo, going to step 12");
			step12(beat);
		} else { // numTones >= 2 --> numTones == 2
			assert numTones == 2;
			
			Set<Integer> midiNums = beat.getToneMap().keySet();
			
			for(int midiNum: midiNums) {
				int[] interval = {3,4,7};
				boolean isInInterval = false;
				for (int i = 0; i < interval.length; i++) {
					isInInterval |= midiNums.contains((midiNum + interval[i]) % 12);
				}

				if(isInInterval) {
					System.out.println("\tYes, going to step 18");
					step18(beat);
					return;
				}
			}
			
			System.out.println("\tNo, going to step 12");
			step12(beat);
		}
	}

	private void step12(Beat beat) {
		System.out.println("12: Is there measure line after this beat?");
		
		if(currentBeat + 1 > song.beats.size() - 1) {
			System.out.println("\tYes, going to step 2");
			step2();
			return;
		}
		
		Beat nextBeat = song.beats.get(currentBeat + 1);
		if(nextBeat.measure != beat.measure) {
			System.out.println("\tYes, going to step 2");
			step2();
			return;
		} else {
			System.out.println("\tNo, going to step 13");
			step13(beat);
		}
	}

	private void step13(Beat beat) {
		System.out.println("13: Add tones of current beat to next beat");
		System.out.println("\tChord not found in current beat");
		
		// beat.setChordMark(new ChordMark());
		
		if(currentBeat + 1 > song.beats.size() - 1) {
			System.out.println("No more beats");
			System.out.println("Going to step 2");
			step2();
			return;
		}
		
		Beat nextBeat = song.beats.get(currentBeat + 1);
		

		Map<Integer, Double> sumToneMap = Chord.sumToneMaps(beat.getToneMap(),nextBeat.getToneMap());
		
		ChordMark nextBeatChord = Chord.getChord(nextBeat.getToneMap(), nextBeat);
		ChordMark sumChord = Chord.getChord(sumToneMap);
		
		// ak sucasny aj dalsi su null potom prenasaj
		if((nextBeatChord != null) || (sumChord != null)) {
			// ak by sme po preneseni nedostali lepsi, tj. iny akord, napr. zo 4 tonov
			// potom preskoc prenasanie
			if(		(sumChord == null) || 
					( sumChord.equals(nextBeatChord) && (!sumChord.getChord().equals(Chord.DOMINANT_SEVENTH_INCOMPLETE) ) ) 
			   ) {
				System.out.println("\tWe are not getting better chord by carrying over the notes");
				System.out.println("\tGoing to step 2");
				step2();
				return;
			}	
		}
		
		// inak pridaj noty do dalsej doby
		
		List<RawNote> newNextBeatNotes = new LinkedList<RawNote>(nextBeat.notes);
		newNextBeatNotes.addAll(getCurrentBeat().notes);
		nextBeat.notes.clear();
		System.out.println("\tCarrying tones to next beat");
		for (RawNote note : newNextBeatNotes) {
			nextBeat.addNote(note);
		}
		
		System.out.println("\tGoing to step 2");
		step2();
	}
	
	private void step14(Beat beat) {
		System.out.println("14: Is there measure line after current beat?");
		if(currentBeat + 1 > song.beats.size() - 1) {
			System.out.println("\tYes, going to step 2");
			System.out.println("\tChord undefined");
			step2();
			return;
		}
		
		Beat nextBeat = song.beats.get(currentBeat + 1);
		if(nextBeat.measure != beat.measure) {
			System.out.println("\tYes, going to step 2");
			System.out.println("\tChord undefined");
			step2();
			return;
		} else {
			System.out.println("\tNo, going to step 15");
			step15(beat);
			return;
		}
	}

	private void step15(Beat beat) {
		//oldOut.println("15");
		System.out.println("15: Eliminate notes that are repeated in next beat");
		/*Beat nextBeat = song.beats.get(currentBeat + 1);
		
		List<RawNote> nextNotes = nextBeat.notes;
		List<RawNote> notesToRemove = new LinkedList<RawNote>();
		
		for (RawNote nextNote : nextNotes) {
			for(int i=0; i<beat.notes.size(); i++) {
				RawNote note = beat.notes.get(i);
				if((nextNote.midiNum % 12) == (note.midiNum % 12)) {
					notesToRemove.add(note);
				}
			}
		}
		
		beat.notes.removeAll(notesToRemove);
		
		System.out.println("\tNotes to remove: ");
		for (RawNote ntr : notesToRemove) {
			System.out.println("\t" + ntr);
		}
		
		List<RawNote> backupNotes = new LinkedList<RawNote>(beat.notes);
		
		beat.notes.clear();		
		beat.getToneMap().clear();
		
		for (RawNote backNote : backupNotes) {
			beat.addNote(backNote);
		}*/
		
		step12(beat);
		
		//step16(beat);
	}

	private void step16(Beat beat) {
		System.out.println("16: Is there remaining tercia or kvinta (3,4,7)?");
		Set<Integer> midiNums = beat.getToneMap().keySet();
		
		for(int midiNum: midiNums) {
			int[] interval = {3,4,7};
			boolean isInInterval = false;
			for (int i = 0; i < interval.length; i++) {
				isInInterval |= midiNums.contains((midiNum + interval[i]) % 12);
			}

			if(isInInterval) {
				System.out.println("\tYes, going to step 18");
				step18(beat);
				return;
			}
		}
		
		System.out.println("\tNo, going to step 17");
		step17(beat);
	}
	
	private void step17(Beat beat) {
		System.out.println("17: Undefined chord");
		System.out.println("Chord not defined, going to step 2");
		step2();
	}
	
	private void step18(Beat beat) {
		System.out.println("18: Do we get chord by adding some tone from prev or next beat?");
		if(currentBeat + 1 > song.beats.size() - 1) {
			System.out.println("\tNo, going to step 19");
			step19(beat);
			return;
		}
		
		assert beat.orderInSong == currentBeat;
		// skus pozriet dozadu
		// najdi najblizsiu neprazdnu dobu smerom dozadu
		
		Beat prevB = null;
		for(int i=currentBeat-1; i>=0; i--) {
			prevB = song.beats.get(i);
			
			if(prevB.notes.size() > 0) {
				if( (prevB.chordMark != null) && (prevB.measure == beat.measure)) {
					Integer[] chordTones = prevB.chordMark.getChordMidiNums();
					Set<Integer> beatTones = beat.getToneMap().keySet();
					
					
					int tonesIn = 0;
					
					for (int j = 0; j < chordTones.length; j++) {
						int tone = chordTones[j] % 12;
						
						//System.out.println("chord tone " + j + ": " + MidiReader.NOTE_NAMES[tone]);
						
						for (Integer beatMidi : beatTones) {
							//System.out.println("beat tone " + j + ": " + MidiReader.NOTE_NAMES[beatMidi % 12]);
							if(tone == (beatMidi % 12) ) {
								tonesIn++;
							}
						}
					}
					
					if(tonesIn >= 2) {
						//oldOut.println("Step18, meas: " + beat.measure + ", oim:" + beat.orderInMeasure + ", ois: " + beat.orderInSong);
						beat.setChordMark(new ChordMark(prevB.chordMark));
						System.out.println("\tYes - from previous beat, chord: " + prevB.chordMark);
						step2();
						return;
					}					
				} else {
					break;
				}
			}
		}
		
		// nenasli sme smerom dozadu
		
		// ak na dalsej dobe nie je akord potom prenes tony
		Beat nextBeat = (currentBeat + 1 > song.beats.size() - 1) ? null : song.beats.get(currentBeat + 1);
		
		if(nextBeat == null) {
			System.out.println("\tNo - not found in previous and there is no next beat");
			step2();
			return;
		}
		
		if( nextBeat.notes.size() == 0 ) {
			System.out.println("\tNo - not found in previous and no tones in next beat");
			System.out.println("\tGoing to step 12");
			step12(beat);
			return;
		}
		
		// ak su v dalsej dobe nejake tony
		ChordMark nextBeatChord = Chord.getChord(nextBeat.getToneMap(), nextBeat);
		
		// ak na dalsej dobe nie je akord alebo je tam nekompletny akord => prenes 
		if( (nextBeatChord == null) || (nextBeatChord.getChord().equals(Chord.DOMINANT_SEVENTH_INCOMPLETE)) ) {
			if(nextBeatChord == null) {
				System.out.println("\tNo - not found in previous and no chords in next beat");
			} else {
				System.out.println("\tNo - not found in previous and incomplete chord in next beat");
			}
			System.out.println("\tGoing to step 12");
			step12(beat);
			return;
		}
		
		// ak na dalsej dobe je akord
		// ak su tam oba tony oznac rovnakym akordom
		int tonesIn = 0;
		Integer[] chordTones = nextBeatChord.getChordMidiNums();
				
		for (int j = 0; j < chordTones.length; j++) {
			int tone = chordTones[j] % 12;
			
			for (Integer beatMidi : beat.getToneMap().keySet()) {
				if(tone == (beatMidi % 12) ) {
					tonesIn++;
				}
			}
		}
		
		if(tonesIn >= 2) {
			beat.setChordMark(new ChordMark(nextBeatChord));
			System.out.println("\tYes - both tones are in next beat, chord: " + nextBeatChord);
			System.out.println("\tGoing to step 2");
			step2();
			return;
		}
		
		// PRENESIE SA AK sa nenajde akord
		
		// Ak nic - tj. na dalsej je akord ale oba tony v nom nie su - tak potom oznac nasledovne
		// ak 3 - ako - so spodnym ako zakladnym
		// ak 4 - ako + so spodnym ako zakladnym
		// ak 7 - prenes a nenastav nic -- POZOR --
		Set<Integer> beatTones = beat.getToneMap().keySet();
		
		int lowerTone = Integer.MAX_VALUE;
		int higherTone = Integer.MIN_VALUE;
		
		for (Integer tone : beatTones) {
			lowerTone = Math.min(lowerTone, tone);
			higherTone = Math.max(higherTone, tone);
		}
		
		int interval = higherTone - lowerTone;
		
		ChordMark openChMark = null;
		if(interval == 3) {
			openChMark = new ChordMark(Chord.MINOR_TRIAD, lowerTone);
			beat.setChordMark(openChMark);
			System.out.println("\tNo, setting the open chord: " + openChMark);
		} else if(interval == 4) {
			openChMark = new ChordMark(Chord.MAJOR_TRIAD, lowerTone);
			beat.setChordMark(openChMark);
			System.out.println("\tNo, setting the open chord: " + openChMark);			
		} else if(interval == 7) {
			System.out.println("\tNo, interval for open chord 7, setting nothing");
			System.out.println("\tGoing to step 12");
			step12(beat);
			return;
		}
		
		step2();
	}
	
	private void step18old(Beat beat) {
		System.out.println("18: Do we get chord by adding some tone from prev or next beat?");
		if(currentBeat + 1 > song.beats.size() - 1) {
			System.out.println("\tNo, going to step 19");
			step19(beat);
			return;
		}
		
/*
		// je v predchadzajucej dobe najdeny akord
		Beat prevBeat = (beat.orderInSong - 1 > 0) ? song.beats.get(beat.orderInSong - 1) : null;
		
		if( (prevBeat != null) && (prevBeat.getChordMark() != null) ) {
			assert prevBeat.getChordMark().getChord() == null : "Null chord, basic: " + prevBeat.getChordMark().getMidiNum() +  " incomplete: " + prevBeat.getChordMark().isIncomplete() + ", prevBeatNo: " + prevBeat.orderInSong;
			
			int[] chordTones = prevBeat.chordMark.getChordMidiNums();
			Set<Integer> beatIntervalTones = beat.getToneMap().keySet();
			
			int beatTonesInChord = 0;
			for (int beatTone : beatIntervalTones) {
				for (int chordTone : chordTones) {
					if(beatTone == chordTone) {
						beatTonesInChord++;
						break;
					}
				}
			}
			// obsahuje oba tony?
			if(beatTonesInChord == beatIntervalTones.size()) {
				prevBeat.setChordMark(beat.chordMark);
				step2();
				return;
			}
				
		}
		
		
		// ak je to 3 alebo 4
		Set<Integer> midiNums = beat.getToneMap().keySet();
		
		for(int midiNum: midiNums) {
			int[] interval = {3,4,7};
			boolean isInInterval = false;
			for (int i = 0; i < interval.length; i++) {
				assert interval[i] == 3 || interval[i] == 4 || interval[i] == 7;
				
				isInInterval |= midiNums.contains((midiNum + interval[i]) % 12);
				
				if(isInInterval) {
					int basicTone = Math.min(midiNum % 12, (midiNum + interval[i]) % 12);
					
					if(interval[i] == 3) {
						//beat.setChordMark(new ChordMark(Chord.MINOR_TRIAD, basicTone));
						beat.chordMark = (new ChordMark(Chord.MINOR_TRIAD, basicTone));
						step2();
						return;
					} else if((interval[i] == 4)) {
						//beat.setChordMark(new ChordMark(Chord.MAJOR_TRIAD, basicTone));
						beat.chordMark = (new ChordMark(Chord.MAJOR_TRIAD, basicTone));
						step2();
						return;
					} else {
						step12(beat);
						return;
					}
					
				}
			}
		}
		*/
		// Stara strategia - hlada doplnenie v dalsej dobe
		
		Beat nextBeat = song.beats.get(currentBeat + 1);		
		boolean foundChord = false;
		
		Set<Set<Integer>> toneSubSets = nextBeat.getAllToneSubSets();

		for(Set<Integer> toneSubSet: toneSubSets) {
			@SuppressWarnings("unchecked")
			Map<Integer, Double> midiNumLengths = Chord.sumToneMaps(beat.getToneMap(), nextBeat.getToneMap());
			
			System.out.println("\t\tTry to add notes: " + toneSubSet);
			
			List<ChordMark> chordMarks = Chord.getChords(midiNumLengths);
			boolean isChord = chordMarks.size() > 0;
			
			if(isChord) {
				foundChord = true;
				System.out.println("\tYes");
				for(ChordMark chm: chordMarks) {
				  System.out.println("\t\tPotentional chord: " + chm);
				}
			}	
		}
		
		if(!foundChord) {
			System.out.println("\tNo, going to step 19");
			step19(beat);
		} else {
			@SuppressWarnings("unchecked")
			Map<Integer, Double> sumMap = Chord.sumToneMaps(beat.getToneMap(), nextBeat.getToneMap());
			
			ChordMark definiteChord = Chord.getChord(sumMap);
			System.out.println("\tDefinite chord:" + definiteChord);
			
			// nastav znacku pre dobu
			beat.setChordMark(definiteChord);
			System.out.print("\tGoing to step 2\n");
			step2();
		}
		
	}
		
	

	private void step19(Beat beat) {
		System.out.println("19: Is in beat interval 3,4 or 7?");
		Set<Integer> midiNums = beat.getToneMap().keySet();
		for(int midiNum: midiNums) {
			int[] interval = {3,4,7};
			boolean isInInterval = false;
			int intNum = -1;
			int tone = -1;
			
			for (int i = 0; i < interval.length; i++) {
				if(midiNums.contains((midiNum + interval[i]) % 12)) {
					isInInterval = true;
					intNum = interval[i];
					tone = midiNum;
				}
			}
			
			
			// ak nasledujuca doba je v tom istom takte
			// a zaroven na dalsej dobe sa neda najst samostatne akord
			// potom prenes noty do dalsej doby
			if(currentBeat + 1 < song.beats.size()) { // existuje dalsia doba
				Beat nextBeat = song.beats.get(currentBeat + 1);
				
				if(nextBeat.measure == beat.measure) {
					List<ChordMark> chords = Chord.getChords(nextBeat.getToneMap());
					if((chords == null) || (chords.size() == 0)) {
						// pridaj noty do dalsej doby						
						List<RawNote> newNextBeatNotes = new LinkedList<RawNote>(nextBeat.notes);
						newNextBeatNotes.addAll(getCurrentBeat().notes);
						nextBeat.notes.clear();						
						for (RawNote note : newNextBeatNotes) {
							nextBeat.addNote(note);
						}
					}	
				}
			}

			if(isInInterval) {
				System.out.println("\tYes");
				System.out.println("\tFound chord: OPEN_WITH_" + intNum);
				// Prirad znacku
				ChordMark cm = new ChordMark(IncompleteChord.valueOf("OPEN_WITH_" + intNum), tone);
				beat.setChordMark(cm);
				
				step2();
				return;
			}			

		}
		// sem by sme sa nemali dostat
		assert false;
		step2();
		return;
	}
	
	private void step21() {
		System.out.println("21: End of program");
	}

	public static void main(String[] args) throws Exception {
		ChordAnalysis ca = new ChordAnalysis(/*"midi1.mid"*/ /*"Promenade_Example.mid"*/ /*"sonataFDurMozart.mid"*/ /*"patet beeth.mid"*/ "34.MID" );
		ca.process();
		
		BufferedWriter bWriter = new BufferedWriter(new FileWriter("info.txt"));
		
		int i=0;
		for(Beat b: ca.song.beats) {
			String line = i + ": " + b.chordMark + "\n";
			System.out.print(line);
			bWriter.write(line);
			i++;
		}
		
		bWriter.close();
		
		
	}
}
