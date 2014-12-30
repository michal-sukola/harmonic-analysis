import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;





public class Namer {
	
	public static FlatSharpConflictResolution flatSharpRes = 
			//FlatSharpConflictResolution.CHOOSE_LESS_SHIFTS_IFEQ_FLAT;
			FlatSharpConflictResolution.CHOOSE_LESS_SHIFTS_IFEQ_SHARP;
	
	// 2ka teda D moze byt zobrazene ako disis ale iba v tonine dis mol
	public static final String[] sharpToneNames = {"His", "Cis", "D", "Dis", "E", "Eis", "Fis", "G", "Gis", "A", "Ais", "H"};
	
	public static final String[] flatToneNames = {"C", "Des", "D", "Es", "Fes", "F", "Ges", "G", "As", "A", "B", "Ces"};
	public static final String[] usualToneNames = {"C", null, "D", null, "E", "F", null, "G", null, "A", null, "H"};
	
	public static final String[] C_DUR_TONES =  {"C", "D", "E", "F", "G", "A", "H"};
	
	public static final String SHIFT_UP = "is";
	public static final String SHIFT_DOWN = "es";
	
	public static final String DUR = "dur";
	public static final String MOLL = "moll";
	
	public static final int DUR_ZERO_TONE = 0; // C v dur
	public static final int MOLL_ZERO_TONE = 9; // A v moll
	
	//{2,2,1,2,2,2,.....};
	public static final Integer[] DUR_INTERVALS = Arrays.copyOf(KeySignature.DUR_INTERVALS, 6); 
	public static final Integer[] MOLL_INTERVALS = Arrays.copyOf(KeySignature.MOLL_INTERVALS, 6);

	public static List<Integer> getSharpDurMidiTones() {
		List<Integer> sharpDurMidis = new ArrayList<Integer>(7);
		
		for (int numSharps = 1; numSharps <= 7; numSharps++) {
			int tone = (numSharps*7) % 12; // shift by 7 semitones times number of sharps
			
			sharpDurMidis.add(tone);
		}		
		return sharpDurMidis;
	}
	
	public static List<Integer> getFlatDurMidiTones() {
		List<Integer> flatDurMidis = new ArrayList<Integer>(7);
		
		for (int numFlats = 1; numFlats <= 7; numFlats++) {
			int tone = (numFlats*5) % 12; // shift by 7 semitones times number of sharps
			
			flatDurMidis.add(tone);
		}		
		return flatDurMidis;		
	}
	
	public static List<Integer> getSharpMollMidiTones() {
		List<Integer> sharpMollMidis = new ArrayList<Integer>(7);
		
		for (int numSharps = 1; numSharps <= 7; numSharps++) {
			int tone = (numSharps*7 + 9) % 12; // shift by 7 semitones times number of sharps and shifted by 9 (tone A)
			
			sharpMollMidis.add(tone);
		}		
		return sharpMollMidis;
	}
	
	public static List<Integer> getFlatMollMidiTones() {
		List<Integer> flatMollMidis = new ArrayList<Integer>(7);
		
		for (int numFlats = 1; numFlats <= 7; numFlats++) {
			int tone = (numFlats*5 + 9) % 12; // shift by 7 semitones times number of sharps shifted by 9 (tone A)
			
			flatMollMidis.add(tone);
		}		
		return flatMollMidis;		
	}
	
	
	private static void transformIncreaseOrDecrease(List<String> tones, int position, int incDec) {
		String harmToneName = tones.get(position);
		
		if(incDec < 0) { // for DUR
			// if contains "is" shifter then remove
			if(harmToneName.indexOf("is") > 0) {
				harmToneName = harmToneName.replaceAll("is", "");	
			} else {
				harmToneName = harmToneName + "es";
				harmToneName = harmToneName.replaceAll("Ees", "Es");
				harmToneName = harmToneName.replaceAll("Aes", "As");
			}
		} else { // for MOLL
			// if contains "es" down shifter then remove
			if(harmToneName.indexOf("es") >= 0 || harmToneName.indexOf("Es") >= 0 || harmToneName.indexOf("As") >= 0) {
				harmToneName = harmToneName.replaceAll("es", "");
				harmToneName = harmToneName.replaceAll("As", "A");
				harmToneName = harmToneName.replaceAll("Es", "E");
			} else {
				harmToneName = harmToneName + "is";
			}
		}
			
		
		
		tones.set(position, harmToneName);
	}
	
		
	private static void transformDurToHarmonic(List<String> durTones) {
		final int DUR_HARMONIC_POS = 5; // 6th tone to decrease		
		transformIncreaseOrDecrease(durTones, DUR_HARMONIC_POS, -1);
	}
	
	private static void transformMollToHarmonic(List<String> mollTones) {
		final int MOLL_HARMONIC_POS = 6; // 7th tone to decrease		
		transformIncreaseOrDecrease(mollTones, MOLL_HARMONIC_POS, +1);
	}

	private static void transformMollToMelodic(List<String> mollTones) {
		transformMollToHarmonic(mollTones);
		
		final int MOLL_MELODIC_POS = 5; // 6th tone to decrease		
		transformIncreaseOrDecrease(mollTones, MOLL_MELODIC_POS, +1);
	}

	private static List<String> getScaleToneNames(int basicToneMidi, ScaleType scaleType) {
		// handle the trivial cases
		if( (scaleType.equals(ScaleType.DUR) || scaleType.equals(ScaleType.DUR_HARMONIC)) && ( basicToneMidi == 0 )) { // C DUR
			if(scaleType.equals(ScaleType.DUR)) {
				return new ArrayList<String>(Arrays.asList(C_DUR_TONES));	
			} else {
				//System.out.println("Trivial for C_DUR_HARMONIC");
				List<String> cDurHarmonic = new ArrayList<String>(Arrays.asList(C_DUR_TONES));
				transformDurToHarmonic(cDurHarmonic);
				return cDurHarmonic;
			}			
		}
		
		final int A_MOLL_SHIFT = 9;
		if( ( scaleType.equals(ScaleType.MOLL) || scaleType.equals(ScaleType.MOLL_HARMONIC) || scaleType.equals(ScaleType.MOLL_MELODIC) ) && ( basicToneMidi == A_MOLL_SHIFT )) { // A MOLL
			List<String> amollTones = new ArrayList<String>(Arrays.asList(C_DUR_TONES));
			Collections.rotate(amollTones, A_MOLL_SHIFT - C_DUR_TONES.length);
			
			if(scaleType.equals(ScaleType.MOLL)) {
				return amollTones;	
			} else if(scaleType.equals(ScaleType.MOLL_HARMONIC)) {
				List<String> aMollHarmonic = new ArrayList<String>(amollTones);
				transformMollToHarmonic(aMollHarmonic);
				return aMollHarmonic;
			} else {
				List<String> aMollMelodic = new ArrayList<String>(amollTones);
				transformMollToMelodic(aMollMelodic);
				return aMollMelodic;
			}
			
		}
		
		List<String> toneNames = null;
		
		boolean isSharp;
		boolean isFlat;
		int numSharps;
		int numFlats;
		
		if(scaleType.equals(ScaleType.DUR) || scaleType.equals(ScaleType.DUR_HARMONIC)) {
			isSharp = isSharpDur(basicToneMidi);
			isFlat = isFlatDur(basicToneMidi);
			
			numSharps = getSharpDurMidiTones().indexOf(basicToneMidi) + 1;
			numFlats = getFlatDurMidiTones().indexOf(basicToneMidi) + 1;			
		} else {
			isSharp = isSharpMoll(basicToneMidi);
			isFlat = isFlatMoll(basicToneMidi);
			
			numSharps = getSharpMollMidiTones().indexOf(basicToneMidi) + 1;
			numFlats = getFlatMollMidiTones().indexOf(basicToneMidi) + 1;
		}

		
		// handle the trick situation when the tone is in flat and also sharp
		if(isSharp && isFlat) {
			
			
			
			if(flatSharpRes.equals(FlatSharpConflictResolution.CHOOSE_LESS_SHIFTS_IFEQ_SHARP)) {
				if(numSharps == numFlats) { // 
					isSharp = true; 
					isFlat = false;	
				} else if(numSharps < numFlats) {
					isSharp = true; 
					isFlat = false;	
				} else { // numFlats < numSharps
					isSharp = false; 
					isFlat = true;	
				}
				
			} else if(flatSharpRes.equals(FlatSharpConflictResolution.CHOOSE_LESS_SHIFTS_IFEQ_FLAT)) { 
				if(numSharps == numFlats) {
					isSharp = false; 
					isFlat = true;	
				} else if(numSharps < numFlats) {
					isSharp = true; 
					isFlat = false;	
				} else { // numFlats < numSharps
					isSharp = false; 
					isFlat = true;	
				}			
			} else if(flatSharpRes.equals(FlatSharpConflictResolution.CHOOSE_FLATS_IFEQ_FLAT)) {
				isSharp = false; 
				isFlat = true;
			} else if(flatSharpRes.equals(FlatSharpConflictResolution.CHOOSE_SHARPS_IFEQ_SHARP)) {
				isSharp = true; 
				isFlat = false;
			} else {
				throw new RuntimeException("Not developed yet");
			}
		}

		
		
		switch (scaleType) {
		case DUR :
		case DUR_HARMONIC:
		
			if(isSharp) {
				final int INC_TONE_POSITION = 3; // 4th tone to be increased 
				
				int previousIncreased = (60 + basicToneMidi - 7) % 12;
			    List<String> previousToneNames = getScaleToneNames(previousIncreased, ScaleType.DUR);
	
				String increasedTone = previousToneNames.get(INC_TONE_POSITION) + "is";
				toneNames = new ArrayList<String>(previousToneNames);
				toneNames.set(INC_TONE_POSITION, increasedTone);
				Collections.rotate(toneNames, 3);
				
				if(scaleType.equals(ScaleType.DUR_HARMONIC)) {
					transformDurToHarmonic(toneNames);
				}
					
				return toneNames;
			}
			
			if(isFlat) {
				final int DEC_TONE_POSITION = 6; // 7th tone to be decreased
				
				int previousDecreased = (60 + basicToneMidi - 5) % 12;
			    List<String> previousToneNames = getScaleToneNames(previousDecreased, ScaleType.DUR);
	
				String decreasedTone = previousToneNames.get(DEC_TONE_POSITION) + "es";
				
				if(decreasedTone.equals("Ees")) {
					decreasedTone = "Es";
				} else if(decreasedTone.equals("Aes")) {
					decreasedTone = "As";
				}
				
				toneNames = new ArrayList<String>(previousToneNames);
				toneNames.set(DEC_TONE_POSITION, decreasedTone);
				Collections.rotate(toneNames, 4);
				
				if(scaleType.equals(ScaleType.DUR_HARMONIC)) {
					transformDurToHarmonic(toneNames);
				}
					
				return toneNames;
			}
			
			
			
			
			break;
		case MOLL:
		case MOLL_MELODIC:
		case MOLL_HARMONIC:
			if(isSharp) {
				final int INC_TONE_POSITION = 5; // 6th tone to be increased 
				
				int previousIncreased = (60 + basicToneMidi - 7) % 12;
			    List<String> previousToneNames = getScaleToneNames(previousIncreased, ScaleType.MOLL);
	
				String increasedTone = previousToneNames.get(INC_TONE_POSITION) + "is";
				toneNames = new ArrayList<String>(previousToneNames);
				toneNames.set(INC_TONE_POSITION, increasedTone);
				Collections.rotate(toneNames, 3);
				
				if(scaleType.equals(ScaleType.MOLL_HARMONIC)) {
					transformMollToHarmonic(toneNames);
				} 
				
				if(scaleType.equals(ScaleType.MOLL_MELODIC)) {
					transformMollToMelodic(toneNames);
				}
					
				return toneNames;
			}
			
			if(isFlat) {
				final int DEC_TONE_POSITION = 1; // 2nd tone to be decreased
				
				int previousDecreased = (60 + basicToneMidi - 5) % 12;
			    List<String> previousToneNames = getScaleToneNames(previousDecreased, ScaleType.MOLL);
	
				String decreasedTone = previousToneNames.get(DEC_TONE_POSITION) + "es";
				
				if(decreasedTone.equals("Ees")) {
					decreasedTone = "Es";
				} else if(decreasedTone.equals("Aes")) {
					decreasedTone = "As";
				}
				
				toneNames = new ArrayList<String>(previousToneNames);
				toneNames.set(DEC_TONE_POSITION, decreasedTone);
				Collections.rotate(toneNames, 4);
				
				if(scaleType.equals(ScaleType.MOLL_HARMONIC)) {
					transformMollToHarmonic(toneNames);
				}
				
				if(scaleType.equals(ScaleType.MOLL_MELODIC)) {
					transformMollToMelodic(toneNames);
				}
					
				return toneNames;
			}			
			
		default:
			break;
		}
		
		
		
		return null;
	}

	private static boolean isFlatMoll(int basicToneMidi) {
		boolean isFlat;
		isFlat = getFlatMollMidiTones().contains(basicToneMidi);
		return isFlat;
	}

	private static boolean isSharpMoll(int basicToneMidi) {
		boolean isSharp;
		isSharp = getSharpMollMidiTones().contains(basicToneMidi);
		return isSharp;
	}

	private static boolean isFlatDur(int basicToneMidi) {
		boolean isFlat;
		isFlat = getFlatDurMidiTones().contains(basicToneMidi);
		return isFlat;
	}

	private static boolean isSharpDur(int basicToneMidi) {
		boolean isSharp;
		isSharp = getSharpDurMidiTones().contains(basicToneMidi);
		return isSharp;
	}

	public static String getScaleName(Scale scale) {
		// backup old flatSharp resolution
		FlatSharpConflictResolution oldFlatRes = Namer.flatSharpRes;
		// DUR
		if(scale.scaleType.equals(ScaleType.DUR) || scale.scaleType.equals(ScaleType.DUR_HARMONIC) ) {
			if(isSharpDur(scale.midiNum) && isFlatDur(scale.midiNum)) {
				Namer.flatSharpRes = FlatSharpConflictResolution.CHOOSE_FLATS_IFEQ_FLAT;
				List<String> flatScaleTones = getScaleToneNames(scale.midiNum, scale.scaleType);
				
				Namer.flatSharpRes = FlatSharpConflictResolution.CHOOSE_SHARPS_IFEQ_SHARP;
				List<String> sharpScaleTones = getScaleToneNames(scale.midiNum, scale.scaleType);
				
				return flatScaleTones.get(0) + " " + scale.scaleType + "/" + sharpScaleTones.get(0) + " " + scale.scaleType;
			}
		}
		// MOLL
		if(scale.scaleType.equals(ScaleType.MOLL) || scale.scaleType.equals(ScaleType.MOLL_HARMONIC) || scale.scaleType.equals(ScaleType.MOLL_MELODIC) ) {
			if(isSharpMoll(scale.midiNum) && isFlatMoll(scale.midiNum)) {
				Namer.flatSharpRes = FlatSharpConflictResolution.CHOOSE_FLATS_IFEQ_FLAT;
				List<String> flatScaleTones = getScaleToneNames(scale.midiNum, scale.scaleType);
				
				Namer.flatSharpRes = FlatSharpConflictResolution.CHOOSE_SHARPS_IFEQ_SHARP;
				List<String> sharpScaleTones = getScaleToneNames(scale.midiNum, scale.scaleType);
				
				return flatScaleTones.get(0) + " " + scale.scaleType + "/" + sharpScaleTones.get(0) + " " + scale.scaleType;
			}
		}
		
		List<String> scaleTones = getScaleToneNames(scale.midiNum, scale.scaleType);
		
		Namer.flatSharpRes = oldFlatRes;
		return scaleTones.get(0) + " " + scale.scaleType;
	}
	
	public static String getChordName(ChordMark chordMark, Scale scale) {
		List<Integer> scaleTones = Arrays.asList(scale.getMidiTones());
		// if the scale contains the midi tone
		int indexOfChordTone = scaleTones.indexOf(chordMark.getMidiNum());
		if(indexOfChordTone > -1) { // if found
			// get the tones from scale
			List<String> scaleToneNames = getScaleToneNames(scale.midiNum, scale.scaleType);
			// index tells which is the chord basic tone order in the scale
			String chordToneName = scaleToneNames.get(indexOfChordTone);
			
			return chordToneName + " " + chordMark.getChord().name();
		}
		
		// if not found in chord then return the dummy name
		System.out.println("Chord tone not found in scale");
		return chordMark.toString();
	}

	public static void main(String[] args) {
	
		System.out.println("A moll: " + getScaleToneNames(9, ScaleType.MOLL) );
		
		Namer.flatSharpRes = FlatSharpConflictResolution.CHOOSE_LESS_SHIFTS_IFEQ_SHARP;
		
		int[] increasedDursMidis = {0, 7, 2, 9, 4, 11, 6, 1};
		
		for (int i = increasedDursMidis.length - 1; i >= 0; i--) {
			int tone = increasedDursMidis[i];
			System.out.print("Sharp DUR scale for tone: " + tone + " = ");
						
			List<String> scaleTones = getScaleToneNames(tone, ScaleType.DUR);
			
			for (int j = 0; j < scaleTones.size(); j++) {
				System.out.print(scaleTones.get(j) + " ");
			}
			System.out.println();
		}
		
		System.out.println();
		
		Namer.flatSharpRes = FlatSharpConflictResolution.CHOOSE_LESS_SHIFTS_IFEQ_SHARP;
		
		int[] decreasedDursMidis = {5, 10, 3, 8, 1, 6, 11};
		
		for (int i = 0; i < decreasedDursMidis.length; i++) {
			int tone = decreasedDursMidis[i];
			System.out.print("Flat DUR scale for tone: " + tone + " = ");			
			
			List<String> scaleTones = getScaleToneNames(tone, ScaleType.DUR);
			
			for (int j = 0; j < scaleTones.size(); j++) {
				System.out.print(scaleTones.get(j) + " ");
			}
			System.out.println();
		}
		
		System.out.println();
	
		Namer.flatSharpRes = FlatSharpConflictResolution.CHOOSE_LESS_SHIFTS_IFEQ_SHARP;
		
		for (int i = increasedDursMidis.length - 1; i >= 0; i--) {
			int tone = increasedDursMidis[i];
			System.out.print("Sharp DUR_HARMONIC scale for tone: " + tone + " = ");
						
			List<String> scaleTones = getScaleToneNames(tone, ScaleType.DUR_HARMONIC);
			
			for (int j = 0; j < scaleTones.size(); j++) {
				System.out.print(scaleTones.get(j) + " ");
			}
			System.out.println();
		}
		
		System.out.println();
		
		Namer.flatSharpRes = FlatSharpConflictResolution.CHOOSE_LESS_SHIFTS_IFEQ_SHARP;
		
		for (int i = 0; i < decreasedDursMidis.length; i++) {
			int tone = decreasedDursMidis[i];
			System.out.print("Flat DUR_HARMONIC scale for tone: " + tone + " = ");			
			
			List<String> scaleTones = getScaleToneNames(tone, ScaleType.DUR_HARMONIC);
			
			for (int j = 0; j < scaleTones.size(); j++) {
				System.out.print(scaleTones.get(j) + " ");
			}
			System.out.println();
		}
		
		System.out.println();
		
		int[] increasedMollMidis = {9, 4, 11, 6, 1, 8, 3, 10};
		
		Namer.flatSharpRes = FlatSharpConflictResolution.CHOOSE_LESS_SHIFTS_IFEQ_SHARP;
		
		for (int i = increasedMollMidis.length - 1; i >= 0; i--) {
			int tone = increasedMollMidis[i];
			System.out.print("Sharp MOLL scale for tone: " + tone + " = ");
						
			List<String> scaleTones = getScaleToneNames(tone, ScaleType.MOLL);
			
			for (int j = 0; j < scaleTones.size(); j++) {
				System.out.print(scaleTones.get(j) + " ");
			}
			System.out.println();
		}
		
		System.out.println();
		
		int[] decreasedMollMidis = {2, 7, 0, 5, 10, 3, 8};
		
		Namer.flatSharpRes = FlatSharpConflictResolution.CHOOSE_LESS_SHIFTS_IFEQ_SHARP;
		
		for (int i = 0; i < decreasedMollMidis.length; i++) {
			int tone = decreasedMollMidis[i];
			System.out.print("Flat MOLL scale for tone: " + tone + " = ");			
			
			List<String> scaleTones = getScaleToneNames(tone, ScaleType.MOLL);
			
			for (int j = 0; j < scaleTones.size(); j++) {
				System.out.print(scaleTones.get(j) + " ");
			}
			System.out.println();
		}
		
		System.out.println();
		
		Namer.flatSharpRes = FlatSharpConflictResolution.CHOOSE_LESS_SHIFTS_IFEQ_SHARP;
		
		for (int i = increasedMollMidis.length - 1; i >= 0; i--) {
			int tone = increasedMollMidis[i];
			System.out.print("Sharp MOLL_HARMONIC scale for tone: " + tone + " = ");
						
			List<String> scaleTones = getScaleToneNames(tone, ScaleType.MOLL_HARMONIC);
			
			for (int j = 0; j < scaleTones.size(); j++) {
				System.out.print(scaleTones.get(j) + " ");
			}
			System.out.println();
		}
		
		System.out.println();
		
		Namer.flatSharpRes = FlatSharpConflictResolution.CHOOSE_LESS_SHIFTS_IFEQ_SHARP;
		
		for (int i = 0; i < decreasedMollMidis.length; i++) {
			int tone = decreasedMollMidis[i];
			System.out.print("Flat MOLL_HARMONIC scale for tone: " + tone + " = ");			
			
			List<String> scaleTones = getScaleToneNames(tone, ScaleType.MOLL_HARMONIC);
			
			for (int j = 0; j < scaleTones.size(); j++) {
				System.out.print(scaleTones.get(j) + " ");
			}
			System.out.println();
		}
		
		System.out.println();
		
		Namer.flatSharpRes = FlatSharpConflictResolution.CHOOSE_LESS_SHIFTS_IFEQ_SHARP;
		
		for (int i = increasedMollMidis.length - 1; i >= 0; i--) {
			int tone = increasedMollMidis[i];
			System.out.print("Sharp MOLL_MELODIC scale for tone: " + tone + " = ");
						
			List<String> scaleTones = getScaleToneNames(tone, ScaleType.MOLL_MELODIC);
			
			for (int j = 0; j < scaleTones.size(); j++) {
				System.out.print(scaleTones.get(j) + " ");
			}
			System.out.println();
		}
		
		System.out.println();
		
		Namer.flatSharpRes = FlatSharpConflictResolution.CHOOSE_LESS_SHIFTS_IFEQ_SHARP;
		
		for (int i = 0; i < decreasedMollMidis.length; i++) {
			int tone = decreasedMollMidis[i];
			System.out.print("Flat MOLL_MELODIC scale for tone: " + tone + " = ");			
			
			List<String> scaleTones = getScaleToneNames(tone, ScaleType.MOLL_MELODIC);
			
			for (int j = 0; j < scaleTones.size(); j++) {
				System.out.print(scaleTones.get(j) + " ");
			}
			System.out.println();
		}
		
		System.out.println("\n\nChord name assignments: ");
		
		System.out.println("C major in C dur:");
		Scale s = new Scale(0, ScaleType.DUR);
		System.out.println(getChordName(new ChordMark(Chord.MAJOR_TRIAD, 0), s));
		
		System.out.println("\nC major in F dur:");
		s = new Scale(5, ScaleType.DUR);
		System.out.println(getChordName(new ChordMark(Chord.MAJOR_TRIAD, 0), s));
		
		System.out.println("???");
		
		System.out.println("\n\nScale name assignments: ");
		// Cis Dur = Des dur
		s = new Scale(1,ScaleType.DUR);
		System.out.print("1-DUR: ");
		System.out.println(getScaleName(s));
		
		// Fis Dur = Ges dur
		s = new Scale(6,ScaleType.DUR);
		System.out.print("6-DUR: ");
		System.out.println(getScaleName(s));
		
		// H Dur = Ces dur
		s = new Scale(11,ScaleType.DUR);
		System.out.print("11-DUR: ");
		System.out.println(getScaleName(s));
		
		// Ais mol = B mol
		s = new Scale(10,ScaleType.MOLL);
		System.out.print("10-MOLL: ");
		System.out.println(getScaleName(s));
		
		// Es mol = Dis mol
		s = new Scale(3,ScaleType.MOLL);
		System.out.print("3-MOLL: ");
		System.out.println(getScaleName(s));
		
		// As mol = Gis mol
		s = new Scale(8,ScaleType.MOLL);
		System.out.print("8-MOLL: ");
		System.out.println(getScaleName(s));
		
		// H moll
		s = new Scale(11,ScaleType.MOLL);
		System.out.print("\n11-MOLL: ");
		System.out.println(getScaleName(s));
		
		// E dur
		s = new Scale(4,ScaleType.DUR);
		System.out.print("4-DUR: ");
		System.out.println(getScaleName(s));
	}
	
	
	
}
