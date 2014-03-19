import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public enum Chord {
  MAJOR_TRIAD { public int[] getIntervals() 	{ return new int[] {4,3}; } },
  MINOR_TRIAD { public int[] getIntervals() 	{ return new int[] {3,4}; } },
  AUGMENTED_TRIAD { public int[] getIntervals(){ return new int[] {4,4}; } },
  DIMINISHED_TRIAD { public int[] getIntervals(){ return new int[] {3,3}; } },
  DOMINANT_SEVENTH { public int[] getIntervals(){ return new int[] {4,3,3}; } },
  DIMINISHED_SEVENTH { public int[] getIntervals(){ return new int[] {3,3,3}; } },
  DIMINISHED_MINOR_SEVENTH { public int[] getIntervals(){ return new int[]{3,3,4}; } },
  MAJOR_SEVENTH { public int[] getIntervals(){ return new int[]{4,3,4}; } },
  MINOR_SEVENTH { public int[] getIntervals(){ return new int[]{3,4,3}; } },
  AUGMENTED_SEVENTH { public int[] getIntervals(){ return new int[]{4,4,3}; } },
  MINOR_MAJOR_SEVENTH { public int[] getIntervals(){ return new int[]{3,4,4}; } },
  DOMINANT_SEVENTH_INCOMPLETE { public int[] getIntervals(){ return new int[]{4,6}; } };
  public abstract int[] getIntervals();
  
  public int getNumTones() {
	  return getIntervals().length + 1;
  }
  
  public boolean equalsToOneOf(Chord... chords) {
	  for (Chord chord : chords) {
		 if(this.equals(chord)) {
			 return true;
		 }
	  }
	  return false;
  }
  
  private static Map<Integer, Double> getChordToneMap(Map<Integer, Double> toneMap, ChordMark chordMark) {
	  if(chordMark.getChord() == null) {
		  return null;
	  }
	  
	  Map<Integer, Double> chordToneMap = new HashMap<Integer, Double>();
	  	  
	  int midiNum = chordMark.getMidiNum();
	  
	  int offset = midiNum;
	  
	  chordToneMap.put(offset, toneMap.get(offset));
	  
	  int[] intervals = chordMark.getChord().getIntervals();
	  
	  for (int i = 0; i < intervals.length; i++) {
		  int interval = intervals[i];
		  offset = (offset + interval) % 12;
		  
		  chordToneMap.put(offset, toneMap.get(offset));
	  }
	  
	  return chordToneMap;
  }
  
  public static double getAbsolutChordLength(Map<Integer, Double> toneMap, ChordMark chordMark) {
	  if(chordMark.getChord() == null) {
		  return 0;
	  }
	  
	  Map<Integer, Double> chordToneMap = Chord.getChordToneMap(toneMap, chordMark);
	  Set<Integer> midiNums = chordToneMap.keySet();
	  
	  /*double length = 0.0;
	  for (Integer midiNum : midiNums) {
		  length += chordToneMap.get(midiNum);
	  }
	  
	  return length;*/
	  
	  double length = Double.MAX_VALUE;
	  
	  for (Integer midiNum : midiNums) {
		  double toneLength = chordToneMap.get(midiNum);
		  if(toneLength < length) {
			  length = toneLength;
		  }
	  }
	  
	  return length;
	  
	  
  }
  
  public static ChordMark getChord(Map<Integer, Double> toneMap) {
	  return getChord(toneMap, null);
  }
  
  public static ChordMark getChord(Map<Integer, Double> toneMap, Beat beat) {
	  // najprv trvanie potom pocet tonov
	  // +,-,D7 maju prijoritu (zm7)
	  List<ChordMark> chords = getChords(toneMap);
	  
	  if(chords.size() == 0) {
		  return null;
	  }
	  
	  // Ak mame na vyber z dvoch akordov a zaroven sa lisia iba v jednom tone, 
	  // pricom vzdialenost tychto tonov je 2 a zaroven je jeden casovo po druhom smerom nadol,
	  // potom vyberieme ten ktory obsahuje druhy v poradi
	  if((chords.size() == 2) && (beat != null)) {
		  Integer[] chordTones1 = chords.get(0).getChordMidiNums();
		  Integer[] chordTones2 = chords.get(1).getChordMidiNums();
		  // odcitaj druhy akord od prveho
		  List<Integer> chord1List = Arrays.asList(chordTones1);
		  List<Integer> chord2List = Arrays.asList(chordTones2);
		  Collections.sort(chord1List);
		  Collections.sort(chord2List);
		  
		  List<Integer> chord1MinusChord2 = new LinkedList<Integer>(chord1List); 
		  chord1MinusChord2.removeAll(Arrays.asList(chordTones2));
		  
		  boolean differentInOneTone = chord1MinusChord2.size() == 1;
		  
		  if(differentInOneTone) {			  
			  System.out.println("### Diff ###");
			  
			  int diffTone1 = chord1MinusChord2.get(0);
			  
			  List<Integer> chord2MinusChord1 = new LinkedList<Integer>(chord2List);
			  chord2MinusChord1.removeAll(Arrays.asList(chordTones1));
			  
			  int diffTone2 = chord2MinusChord1.get(0);
			  
			  RawNote note1 = findLastRawNoteForTone(beat, diffTone1);
			  RawNote note2 = findLastRawNoteForTone(beat, diffTone2);
			  
			  RawNote firstNote = null;
			  RawNote secondNote = null;
			  
			  if(note1.startTick < note2.startTick) {
				  firstNote = note1;
				  secondNote = note2;
			  } else if(note2.startTick < note1.startTick) {
				  firstNote = note2;
				  secondNote = note1;
			  } else {
				  // not eligible for this test
			  }
			  
			  // ak je druhy ton smerom nadol
			  if(firstNote != null) {
				  if(secondNote.midiNum < firstNote.midiNum) {
					  System.out.println("@@@ super special, distance not 2 down @@@");
					  if(secondNote.midiNum - firstNote.midiNum == 2) {
						  System.out.println("$$$ super special, distance 2 down $$$");
						  return chords.get(1);  
					  }					  
				  }
			  }
			  
		  }
	  }	  
	  
	  // pozri sa na maximalnu dlzku
	  // pouzi toleranciu
	  double tolerance = 0.125;
	  double maxLength = Chord.getMaxLength(chords, toneMap); 
			  //Integer.MIN_VALUE;
	  List<ChordMark> maxLengthChords = new LinkedList<ChordMark>();
	  for (ChordMark maxToneMark : chords) {
		  double chLength = getAbsolutChordLength(toneMap, maxToneMark);
		  System.out.println("chord available: " + maxToneMark + ", length: " + chLength);
		  
		  		  
		  if(chLength + tolerance >= maxLength) {
			  maxLengthChords.add(maxToneMark);
		  }
	  }	 
	  
	  
	  
	  if(maxLengthChords.size() == 1) {
		  
		  return maxLengthChords.get(0);
	  }
	  
	  
	  // ak mame viac s rovnakou dlzkou, v tolerancii potom
	  // kolko ich je s maximalnym poctom tonov
	  
	  int maxNumTones = Integer.MIN_VALUE;
	  for (ChordMark chordMark : maxLengthChords) {
		  if(chordMark.getChord().getNumTones() > maxNumTones) {
			  maxNumTones = chordMark.getChord().getNumTones();
		  }
	  }	  
	  
	  List<ChordMark> maxToneChords = new LinkedList<ChordMark>();
	  for (ChordMark chordMark : maxLengthChords) {
		  if(chordMark.getChord().getNumTones() == maxNumTones) {
			  maxToneChords.add(chordMark);
		  }
	  }
	  
	  if(maxToneChords.size() == 1) {
		  return maxToneChords.get(0);
	  }	  
	  
/*	  
	  double maxLength = Integer.MIN_VALUE;
	  List<ChordMark> maxLengthChords = new LinkedList<ChordMark>();
	  for (ChordMark maxToneMark : maxToneChords) {
		  double chLength = getAbsolutChordLength(toneMap, maxToneMark);
		  //System.out.println("CHM " + maxToneMark + " length: " + chLength);
		  
		  if(chLength > maxLength) {
			  maxLength = chLength;
			  maxLengthChords = new LinkedList<ChordMark>();
			  maxLengthChords.add(maxToneMark);
		  } else if(chLength == maxLength) {
			  maxLengthChords.add(maxToneMark);
		  }
	  }
	  
	  if(maxLengthChords.size() == 1) {
		  return maxLengthChords.get(0);
	  } */	  
	  
	  // inak - este dorobit, zobrat najdlhsi z nich alebo napr. pridat unikatnost do definicie akordov a podla toho vybrat
	  
	  
	  return maxToneChords.get(0);
  }
  
	private static double getMaxLength(List<ChordMark> chords, Map<Integer, Double> beatToneMap) {
		double maxLength = Integer.MIN_VALUE;
		for (ChordMark maxToneMark : chords) {
			double chLength = getAbsolutChordLength(beatToneMap, maxToneMark);

			if (chLength > maxLength) {
				maxLength = chLength;
			}
		}

		return maxLength;
	}

	public static RawNote findLastRawNoteForTone(Beat beat, int toneMidi) {
    	List<RawNote> notesForTone = findRawNotesForTone(beat, toneMidi);
    	
    	if(notesForTone.size() == 0) {
    		return null;
    	}
    	
    	long maxStartTick = Integer.MIN_VALUE;
    	RawNote lastNote = null;
    	for (RawNote rawNote : notesForTone) {
			if(rawNote.startTick > maxStartTick) {
				maxStartTick = rawNote.startTick;
				lastNote = rawNote;
			}
		}
    	
    	return lastNote;
    	
    }
  
    public static List<RawNote> findRawNotesForTone(Beat beat, int toneMidi) {
    	List<RawNote> notesForTone = new LinkedList<RawNote>();
		for (RawNote note : beat.notes) {
			if( (note.midiNum % 12) == (toneMidi % 12) ) {
				notesForTone.add(note);
			}
		}
    	
    	return notesForTone;
    }
  
  	public static List<ChordMark> getChords(Map<Integer, Double> toneMap) {
		Set<Integer> midiNums = toneMap.keySet();
  		int numTones = midiNums.size();

		List<ChordMark> chordMarks = new LinkedList<ChordMark>();

		if (numTones < 3) {
			return chordMarks;
		}

		// some preprocessing maybe
		for (int midiNum : midiNums) {
			Chord[] chords = Chord.values();
			for (Chord chord : chords) {
				int[] intervals = chord.getIntervals();
				// System.out.println(Arrays.toString(intervals));

				int offset = midiNum;
				boolean isChord = true;
				for (int i = 0; i < intervals.length; i++) {
					offset = (offset + intervals[i]) % 12;
					if (!midiNums.contains(offset)) {
						isChord = false;
						break;
					}
				}

				if (isChord) {
					ChordMark chordMark = new ChordMark(chord, midiNum);
					//System.out.println("\tFound chord: " + chordMark);
					chordMarks.add(chordMark);
				}
			}
		}

		return chordMarks;		
  	}
  	
  	public static Map<Integer, Double> sumToneMaps(Map<Integer, Double>... maps) {
  		if((maps.length == 0) || (maps == null)) {
  			return null;
  		} else if(maps.length == 1) {
  			return maps[0];
  		}
  		
  		Map<Integer, Double> sumMap = new HashMap<Integer, Double>();
  		
  		for (Map<Integer, Double> map : maps) {
  			for (Integer key : map.keySet()) {
  				if(sumMap.containsKey(key)) {
  					double oldVal = sumMap.get(key);
  					sumMap.put(key, oldVal + map.get(key));
  				} else {
  					sumMap.put(key, map.get(key));
  				}
  			}
		}
  		
  		return sumMap;		
  	}
  
	public static List<ChordMark> getChords(Beat beat) {
		return getChords(beat.getToneMap());
	}

	public static String getChordsInfo(Beat beat) {
		// TODO Auto-generated method stub
		String info = "\n";
		List<ChordMark> chMs = Chord.getChords(beat);
		for (ChordMark chordMark : chMs) {
			info += "\t\t\t" + chordMark.toString() + ", length: " + Chord.getAbsolutChordLength(beat.getToneMap(), chordMark) + ", numTones: " + chordMark.getChord().getNumTones() + "\n";
		}
		
		return info;
	}
	
}
