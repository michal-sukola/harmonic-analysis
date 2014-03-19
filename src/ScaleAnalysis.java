import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

// TODO ak mame toninovu dvojicu na rovnakom zakl tone potom hladame prvy + alebo - so zakladnym tonom z toninovej dvojice


// ak pozname predznamenanie a potom sa nam ukaze nova tonina ale nepotvrdi sa, 
// treba brat do uvahy tu povodnu - pozri bod 21
// toto sa tyka hlavne akordov, kde mozeme urcit ako stupnicu mol aj dur

// Vzdy ked si nevieme rady treba najprv vzdy znovu skusata akordy bod 6/26 a az ako poslednu moznost ist hladat 7 ton


public class ScaleAnalysis {
	
	enum ScaleType {
		DUR, MOL, DUR_MOL, CHROM, DUR_HARMONIC, MOL_MELODIC;
	}
	
	public class Scale {
		int midiNum;
		ScaleType scaleType;
		private boolean far;
		
		Scale(int midiNum, ScaleType scaleType) {
			this.midiNum = midiNum % 12;
			this.scaleType = scaleType;
		}
		
		Scale(Scale scale) {
			this.midiNum = scale.midiNum;
			this.scaleType = scale.scaleType;
		}
		
		public Integer[] getMidiTones() {
			Integer[] intervals = null;
			
			switch (scaleType) {
				case DUR:   intervals = Arrays.copyOf(KeySignature.DUR_INTERVALS, 7);	break;
				case MOL:   intervals = Arrays.copyOf(KeySignature.MOL_INTERVALS, 7);	break;
				case CHROM: intervals = Arrays.copyOf(KeySignature.CHROM_INTERVALS, 7); break;
			
			default:
				break;
			}
			
			Integer[] tones = new Integer[8];
			tones[0] = midiNum;
			for(int i=1; i<8; i++) {
				tones[i] = ( (tones[i - 1] + intervals[i-1]) % 12 );
			}
			
			return tones;
		}
		
		public boolean isInScaleTones(int toneMidi) {
			if(scaleType == ScaleType.DUR_MOL) {
				return false;
			}
			
			toneMidi = toneMidi % 12;
			Integer[] scaleTones = getMidiTones();
			for (int i = 0; i < scaleTones.length; i++) {
				if(toneMidi == scaleTones[i]) {
					return true;
				}
			}
			
			return false;
		}

		@Override
		public String toString() {
			return ChordMark.midiNumToToneString(midiNum) + " " + scaleType;
		}

		public void setFar(boolean b) {
			this.far = true;
		}
		
		public boolean getFar() {
			return this.far;
		}
		
	}
	
	public static PrintStream oldOut;
	
	Song song = null;
	Beat sectionStart = null;
	List<Integer> unimportantTones = new LinkedList<Integer>();
	
	public ScaleAnalysis(ChordAnalysis chordAnalysis) {
		this.song = chordAnalysis.song;
		Beat.song = song;
		this.sectionStart = song.beats.get(0);
		
		/*oldOut = System.out;
		
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("scale_out.txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.setOut(new PrintStream(fos));*/
	}
	
	private boolean moveSectionStartToNextBeat() {
		if(sectionStart.orderInSong + 1 >= song.beats.size()) {
			return false;
		}
		
		sectionStart = song.beats.get(sectionStart.orderInSong + 1);
		// pozri sa ale ci v danej dobe je aspon nejaka nota, ak nie preskoc
		if((sectionStart.notes == null) || (sectionStart.notes.size() == 0)) {
			return moveSectionStartToNextBeat();
		}
		
		return true;
	}
	
	private boolean moveSectionStartToNextBeatFrom(Beat b) {
		sectionStart = b;
		return moveSectionStartToNextBeat();
	}
	
	private boolean moveSectionStartToBeat(Beat b) {
		if(b != null) {
			sectionStart = b;
			return true;
		}
		
		return false;
	}
	
	class BeatChord {
		ChordMark chMark;
		Beat calBeat;
		
		BeatChord(ChordMark ch, Beat b) {
			this.chMark = ch;
			this.calBeat = b;
		}
		
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "b: " + this.calBeat + ", ch: " + this.chMark;
		}
	}
	
	private BeatChord getNextKnownChordFrom(Beat fromBeat) {
		ChordMark chMark = null;
		Beat calBeat = fromBeat;
		while((chMark == null) && (calBeat.orderInSong + 1 < song.beats.size())) {
			calBeat = song.beats.get(calBeat.orderInSong + 1);
			chMark = calBeat.chordMark;
		}
		
		if(chMark == null) {
			return null;
		}
		
		return new BeatChord(chMark, calBeat);
	}
	
	private BeatChord getNextKnownChordFromSectionStart() {
		ChordMark chMark = sectionStart.getChordMark();
		Beat calBeat = sectionStart;
		while((chMark == null) && (calBeat.orderInSong + 1 < song.beats.size())) {
			calBeat = song.beats.get(calBeat.orderInSong + 1);
			chMark = calBeat.chordMark;
		}
		
		if(chMark == null) {
			return null;
		}
		
		return new BeatChord(chMark, calBeat);
	}
	



	private static KeySignature getSignatureFromUser() {
		System.out.println(KeySignature.info());
		String option = "";
		try {
			option = new BufferedReader(new InputStreamReader(System.in)).readLine();
		} catch (IOException e) {
			System.err.println("Error reading signaure from user");
			e.printStackTrace();
		}
		
		boolean inputOK = false;
		
		for (int i = 0; i < KeySignature.values().length && !inputOK; i++) {
			if(option.trim().equals("" + i)) {
				inputOK = true;
			}
		}
		
		KeySignature signature = null;
		if(!inputOK) {
			System.out.println("Wrong option choosen, setting no signature by default");
		} else {
			signature = KeySignature.values()[new Integer(option.trim())];
		}
		
		return signature;
	}
	
	private BeatChord getNextDurMolChordFrom(Beat fromBeat) {
		boolean isDur = false;
		boolean isMol = false;
		
		BeatChord nextDurMolChordBeat = null;

		do {
			nextDurMolChordBeat = getNextKnownChordFrom(fromBeat);
			if(nextDurMolChordBeat == null) {
				isDur = isMol = false;
				break;
			} else {
				isDur = nextDurMolChordBeat.chMark.getChord().equals(Chord.MAJOR_TRIAD);
				isMol = nextDurMolChordBeat.chMark.getChord().equals(Chord.MINOR_TRIAD);
			}
			fromBeat = song.getNextBeat(fromBeat);
		} while (!(isDur || isMol) );

		if(isDur || isMol) {
			System.out.println("\tFound " + nextDurMolChordBeat.chMark);
			
			return nextDurMolChordBeat;
		}
				
		return null;
	}

	private void step1() {
		System.out.println("Step1: First beat is the start of first section");
		step2();
	}
	
	private void step2() {
		System.out.println("Step2: Fill in the key signature or press enter if unknown:");
		//KeySignature signature = getSignatureFromUser();
		
		// tu treba zatial rucne zadat predznamenanie
		KeySignature signature = null;
		
		step3(signature);
	}
	
	private void step3(KeySignature signature) {
		System.out.println("Step3: Is Key signature chosen?");
		if(signature == null) {
			System.out.println("\tNo signature choosen, trying to assign scale by first +/- chord");
						
			
	/*		// C+/A- | -(na inom zakl tone) + - + + - D7(s inou dvojicou) potom sa treba vratit na neurceny usek od zaciatku po D7 a skusat najst 7 tonov ako je popisane nizsie
			
			// DONE Najdeme prvy akord, ak to nie je D7, Dm7 ani Dim7 a ani +,- hladame dalsi akord az dokym sa neobjavi niektory zo spomenutych
			// TODO Ak je prvy takyto akord D7, Dim7, Dm7 potom skusime najst sedem tonov 
						// (akordickych potom hocijakych) a pozrieme, ktoru z dvojice tonin tvoria
						// ak nenajdeme sedem tonov za sebou pre dur ani mol potom
						//  skusame najpr dur a to tak, ze hladame, na ktorej dobe sa objavi ton, ktory do dur dtupnice nepatri
						// to iste skusame aj pre mol a porovname doby, podla vzdialenosti od aktualnej doby
						// toninu urcime podla toho, ktora tonina sa menila neskor (tu nastavime)
			// TODO ak je takyto prvy akord  + alebo - akord a urcime ho ako stupnicu tj. pre napr. D+ nastavime D dur a sedem tonov bude z D duru pre G- nastavime G mol a aj tony z G mol

			// TODO Pre usek na zaciatku, kde sme neskumali toninu tj. od doby 0 po dobu s vyskytom D7, Dim7, Dm7, + alebo -
			// Skusame zoradit 7 tonov do stupnice - ak sa nam to nepodari potom sa posuvame o dobu atd.
			// tj. prekryvame 7 tonovym oknom dobu za dobou a hladame take tony, ktore urcia toninu
	*/
			
			// nastavi sa podla prveho + alebo -
			System.out.println("\tCurrent beat: " + sectionStart.toShortString());
			BeatChord nextDurMolChordBeat = getNextDurMolChordFrom(sectionStart);
						 
			if(nextDurMolChordBeat != null) {
				System.out.println("\tFound " + nextDurMolChordBeat.chMark);
				
				boolean isDur = nextDurMolChordBeat.chMark.getChord().equals(Chord.MAJOR_TRIAD);
				Scale scale = null;
				
				if(isDur) {
					scale = new Scale(nextDurMolChordBeat.chMark.getMidiNum() % 12, ScaleType.DUR);
				} else {
					scale = new Scale(nextDurMolChordBeat.chMark.getMidiNum() % 12, ScaleType.MOL);				
				}
	
				// na cely usek (moze byt aj len 1. doba
				nextDurMolChordBeat.calBeat.setScale(scale, song);
				System.out.println("\tAssigned scale: " + scale);
	
				// Chod na 21
				System.out.println("\tGoing to step 21");
				step21(scale);
				return;
			}

			// skok  na 26, ak uz do konca skladby nie je + ani -
			step26();			
			return;
		} else {
			System.out.println("\tYes, going to step 4");
			step4(signature);
		}
	}
	
	private void step4(KeySignature keySig) {
		System.out.println("Step4: Determine scale pair from key signature");
		
		KeySignature.KeyPair keyPair = keySig.getKeyPair();
		
		System.out.println("\tFound pair: " + keyPair);
		step5(keyPair);
	}
	
	private void step5(KeySignature.KeyPair keyPair) {
		System.out.println("Step5: Read first known chord from the current section");
		
		BeatChord beatChord = getNextKnownChordFromSectionStart();
		ChordMark chMark = beatChord.chMark;
		Beat calBeat = beatChord.calBeat;
		
		System.out.println("\tFirst chord: " + chMark + ", at b/m/bIm: " + 
							calBeat.orderInSong + "/" + calBeat.measure + "/" + calBeat.orderInMeasure);
		
		if(chMark == null) {
			System.out.println("\tNo chords found, going to step 28");
			step28();
			return;
		}
		
		// znacka akordu, v ktorej bol dobe a preznamenanie
		step6(chMark, calBeat, keyPair);
	}

	private void step6(ChordMark firstChMark, Beat chBeat, KeySignature.KeyPair keyPair) {
		System.out.println("Step6: Decide whether is +, -, D7, D7 incomplete or Dim7");
		Chord chord = firstChMark.getChord();
		
		
		switch (chord) {
		case MAJOR_TRIAD:
			System.out.println("\tIs +, going to step 7");
			step7(firstChMark, chBeat, keyPair);
			break;
		case MINOR_TRIAD:
			System.out.println("\tIs -, going to step 7");
			step7(firstChMark, chBeat, keyPair);
			break;
		case DOMINANT_SEVENTH:
			System.out.println("\tIs D7, going to step 9");
			step9(firstChMark, chBeat, keyPair);
			break;
		case DOMINANT_SEVENTH_INCOMPLETE:
			System.out.println("\tIs D7 incomplete, going to step 9");
			step9(firstChMark, chBeat, keyPair);
			break;
		case DIMINISHED_SEVENTH:
			System.out.println("\tIs Dim7, going to step 13");
			step13(firstChMark, chBeat, keyPair);
			break;
		case DIMINISHED_MINOR_SEVENTH:
			System.out.println("\tIs Dm7, going to step 11");
			step11(firstChMark, chBeat, keyPair);
			break;
		default:
			step17(chBeat);
			break;
		}		
	}
	
	private void step7(ChordMark firstChMark, Beat chBeat, KeySignature.KeyPair keyPair) {
		System.out.println("Step7: Get basic chord tone of 1st chord");
		System.out.println("\tBasic chord tone is: " + firstChMark.getChordToneString());
		System.out.println("\tGoing to step 8");
		
		step8(firstChMark, chBeat, keyPair);
	}
	
	
	private void step8(ChordMark firstChMark, Beat chBeat, KeySignature.KeyPair keyPair) {
		System.out.println("Step8: Is the basic tone one of the key signature pair?");
		
		// ak je + porovnavame s durovou, a ak je minus tak porovnavame s molovou
		boolean toneInDur = false;
		boolean toneInMol = false;
				
		if(firstChMark.getChord().equals(Chord.MAJOR_TRIAD)) {
			toneInDur = firstChMark.getMidiNum() == keyPair.firstMidiNum;
		} else if(firstChMark.getChord().equals(Chord.MAJOR_TRIAD)) {
			toneInMol = firstChMark.getMidiNum() == keyPair.secondMidiNum;
		}

		if(toneInDur || toneInMol) {
			System.out.print("\tKey in beat: " + chBeat.orderInSong + "/" + chBeat.orderInMeasure + ":");
			
			String basicTone = firstChMark.getChordToneString();
			Scale durOrMolScale = null;
			
			if(toneInDur) {
				System.out.println(basicTone + " DUR");
				durOrMolScale = new Scale(firstChMark.getMidiNum(), ScaleType.DUR);
			} else {
				System.out.println(basicTone + " MOL");
				durOrMolScale = new Scale(firstChMark.getMidiNum(), ScaleType.MOL);
			}
			
			chBeat.setScale(durOrMolScale, song);

			// DONE Skoc na bod, ktory testuje ci novy ton meni toninu			
			if(moveSectionStartToNextBeatFrom(chBeat)) {
				step21(durOrMolScale);
			} else {
				step28();
			}
		} else {
			// tu pozname iba toninovy par, nechame tak a nic nepriradime, nevieme urcit toninu a 
			// skusame dalsie akordy
			
			// skusime prve tri durove alebo molove kvintakordy ak medzi nimi nie je urcujuci akord
			// potom skusime zostavit 7 roznych tonov do oktavy.
			// ked sa nam to nepodari potom neurcime na aktualnej dobe nic a posunieme sa dalej
			
			// DONE ked priradujeme toninu, tak vzdy pozrieme dozadu az na dobu, na ktorej nie je urcena tonina 
			// a zaroven sa smerom dozadu tonina nezmenila potom
			// pozri na akordicke tony s neurcenou toninou a ak su vsetky obsiahnute v tonoch toniny 
			// nastav ju aj na tuto dobu

			// pytagorejske komma - rozdiel medzi stupanim po oktavach a po kvintach 
			
			if(moveSectionStartToNextBeatFrom(chBeat)) {
				step26();
			} else {
				step28();
			}
		}
	}
	
	private Scale d7isMajMinAfter(ChordMark d7Mark, Beat chBeat, BeatChord nextChordBeat, KeySignature.KeyPair keyPair) {
		Scale scale = null;
		int basicTone = d7Mark.getMidiNum();
		
		// pridame 3 * 36 aby sme nerobili zaporne modulo :) kvoli jave
		int durKeyNum = (12 * 3) + basicTone;
		int molKeyNum = (12 * 3) + basicTone;
		
		molKeyNum -= 7;
		durKeyNum -= 7;		
		
		// odstran posun mimo 0 az 12
		durKeyNum %= 12;
		molKeyNum %= 12;
				
		ChordMark nextChordMark = nextChordBeat.chMark;
		
		if(! nextChordMark.getChord().equalsToOneOf(Chord.MAJOR_TRIAD, Chord.MINOR_TRIAD)) {
			return null;
		}
		
		if(nextChordMark.getChord().equals(Chord.MAJOR_TRIAD)) {
			if(nextChordMark.getMidiNum() == durKeyNum) {
				scale = new Scale(durKeyNum,ScaleType.DUR);	
			}			
		} else {
			if(nextChordMark.getMidiNum() == molKeyNum) {
				scale = new Scale(molKeyNum,ScaleType.MOL);
			}
		}
		
		return scale;
	}
	
	private Scale d7isFrigidAfter(ChordMark d7Mark, Beat chBeat, BeatChord nextChordBeat, KeySignature.KeyPair keyPair) {
		Scale scale = null;
		int basicTone = d7Mark.getMidiNum();
		int durMolKeyNum = (12*3 + basicTone - 1) % 12;
		
		ChordMark nextChordMark = nextChordBeat.chMark;
		
		if(! nextChordMark.getChord().equalsToOneOf(Chord.MAJOR_TRIAD, Chord.MINOR_TRIAD)) {
			return null;
		}
		
		if(nextChordMark.getChord().equals(Chord.MINOR_TRIAD)) {
			if(nextChordMark.getMidiNum() == durMolKeyNum) {
				scale = new Scale(durMolKeyNum,ScaleType.MOL);
			}
		} else {
			if(nextChordMark.getMidiNum() == durMolKeyNum) {
				scale = new Scale(durMolKeyNum,ScaleType.DUR);
			}
		}
		
		// POZOR ked sa nan pytame nastavujeme pri DUR az za nasledujucim akordom
		
		return scale;
	}
	
	private Scale d7isFalseEnding(ChordMark d7Mark, Beat chBeat, BeatChord nextChordBeat, KeySignature.KeyPair keyPair) {
		Scale scale = null;
		int basicTone = d7Mark.getMidiNum();
		int minKeyNum = (basicTone + 2) % 12;
		int majKeyNum = (basicTone + 1) % 12;
		
		ChordMark nextChordMark = nextChordBeat.chMark;
		
		if(! nextChordMark.getChord().equalsToOneOf(Chord.MAJOR_TRIAD, Chord.MINOR_TRIAD)) {
			return null;
		}
		
		int durMolKeyNum = (basicTone - 7 + 12) % 12;
		
		if(nextChordMark.getChord().equals(Chord.MINOR_TRIAD)) {
			if(nextChordMark.getMidiNum() == minKeyNum) {
				scale = new Scale(durMolKeyNum,ScaleType.DUR);
			}
		} else {
			if(nextChordMark.getMidiNum() == majKeyNum) {
				scale = new Scale(durMolKeyNum,ScaleType.MOL);
			}
		}		
		
		return scale;		
	}
	
	
	
	private void step9(ChordMark d7Mark, Beat chBeat) {
		step9(d7Mark, chBeat, null);
	}
	
	private void step9(ChordMark d7Mark, Beat chBeat, KeySignature.KeyPair keyPair) {
		System.out.println("Step9: Determine key for D7 or D7 incomplete");
		// na overenie
		assert (d7Mark.getChord() == Chord.DOMINANT_SEVENTH) || 
			   (d7Mark.getChord() == Chord.DOMINANT_SEVENTH_INCOMPLETE);
		
		BeatChord nextChordBeat = getNextKnownChordFromDifferentFrom(d7Mark, chBeat);
		
		Scale scale = null;
		
		// skus dur mol
		scale = d7isMajMinAfter(d7Mark, chBeat, nextChordBeat, keyPair);
		
		
		
		// ak nie skus falosny zaver
		if(scale == null) {
			scale = d7isFalseEnding(d7Mark, chBeat, nextChordBeat, keyPair);
		}
		// ak nie skus frigicku dominantu
		if(scale == null) {
			scale = d7isFrigidAfter(d7Mark, chBeat, nextChordBeat, keyPair);
		}
		
		// inak prirad dur_mol
		if(scale == null) {
			int durMolKey = (d7Mark.getMidiNum() - 7 + 12) % 12;
			scale = new Scale(durMolKey, ScaleType.DUR_MOL);
			
			chBeat.setScale(scale, song);
			
			if(moveSectionStartToNextBeatFrom(chBeat)) {
				step24(d7Mark);
				return;
			} else {
				step28();
				return;
			}
		}
		
		// prirad stupnicu
		for (int i = chBeat.orderInSong; i <= nextChordBeat.calBeat.orderInSong; i++) {
			song.beats.get(i).setScale(scale, song);
		}
		
		if(moveSectionStartToNextBeatFrom(nextChordBeat.calBeat)) {
			step21(scale);
			return;
		} else {
			step28();
			return;
		}

	}
	
	private void oldStep9(ChordMark d7Mark, Beat chBeat, KeySignature.KeyPair keyPair) {
		System.out.println("Step9: Determine key for D7 or D7 incomplete");
		// na overenie
		assert (d7Mark.getChord() == Chord.DOMINANT_SEVENTH) || 
			   (d7Mark.getChord() == Chord.DOMINANT_SEVENTH_INCOMPLETE);
		
		
		
		
		
		

		Scale scale = null;
		int basicTone = d7Mark.getMidiNum();
		
		// pridame 3 * 36 aby sme nerobili zaporne modulo :) kvoli jave
		int durKeyNum = (12 * 3) + basicTone;
		int molKeyNum = (12 * 3) + basicTone;
		
		molKeyNum -= 7;
		durKeyNum -= 7;		
		
		// odstran posun mimo 0 az 12
		durKeyNum %= 12;
		molKeyNum %= 12;		
		
		
		// DONE Ak mame tri akordy Dim7, Dm7, D7 potom pozname novu toninovu dvojicu - rovnomenne toniny 
				// mozeme vyskusat najprv dur, mol ako na zaciatku a podla toho urcit a zaroven neurceny usek tiez nastavit
				// DONE toto by sa malo vyriesit postupnym vracanim sa pri urceni toniny
				
				// klamny zaver 
				
				// hladame molovy alebo durovy akord podla zakladneho tonu ak najdeme tak na cely usek po akord 
				// vratane nastavime podla toho ci je + alebo - na dur alebo mol a  potom ideme na bod, kde hladame 
				// ci sa objavi ine midi cislo
				
				// moze sa stat, ze najdeme taky akord (tzv. klamny zaver), ktory je o 2 poltony vyssie a je molovy 
				// potom je stupnica DUR !!! a zakladny ton tejto DUR stupnice je ZT D7 - 7
		
				// alebo najdeme akord durovy, ktory je o 1 polton vyssie ako ZT D7,
				// potom mame stupnicu MOL !!! a zakladny ton tejto DUR stupnice je ZT D7 - 7
				BeatChord nextChordBeat = getNextKnownChordFrom(chBeat);
				while(nextChordBeat != null) {
					// System.out.println("beatNo: " + nextChordBeat.calBeat.orderInSong);

					List<Integer> nextChordTones = Arrays.asList(nextChordBeat.chMark.getChordMidiNums());
					
					
					// ak obsahuje zakladny ton durovy a k nemu velku terciu
					// staci najst aj len interval // pozriet ci toto musim prisposobit, pretoze v akordickej analyze priradujeme aj neupl akordom cely akord
					if(nextChordTones.contains((basicTone + 2) % 12)) {
						scale = new Scale(durKeyNum, ScaleType.DUR);
						for (int i = chBeat.orderInSong; i <= nextChordBeat.calBeat.orderInSong; i++) {
							song.beats.get(i).setScale(scale, song);
						}
						
						System.out.println("### KLAMNY na dobe " + chBeat);
						// ist na hladanie meniaceho tonu
						if(moveSectionStartToNextBeatFrom(chBeat)) {
							step21(scale);
							return;
						} else {
							step28();
							return;					
						}
					} // ak obsahuje zakladny ton molovy a k nemu malu terciu
					else if(nextChordTones.contains(molKeyNum) && nextChordTones.contains((molKeyNum + 3) % 12)) {
						scale = new Scale(molKeyNum, ScaleType.MOL);
						for (int i = chBeat.orderInSong; i <= nextChordBeat.calBeat.orderInSong; i++) {
							song.beats.get(i).setScale(scale, song);
						}
						System.out.println("### KLAMNY na dobe " + chBeat);
						// ist na hladanie meniaceho tonu
						if(moveSectionStartToNextBeatFrom(chBeat)) {
							step21(scale);
							return;
						} else {
							step28();
							return;
						}
					} else {
						nextChordBeat = getNextKnownChordFrom(nextChordBeat.calBeat);
					}			
				}
		
		
		System.out.print("\tKey in beat: " + chBeat.orderInSong + "/" + chBeat.orderInMeasure + ":");
		System.out.println(ChordMark.midiNumToToneString(durKeyNum) + " DUR/" +
						   ChordMark.midiNumToToneString(molKeyNum) + "MOL");
		
		// Toninovu dvojicu berieme bud z predznamenania alebo !!!! z poslednej urcenej stupnice - aktualnej
		// ak pozname predznamenie cize toninovu dvojicu potom priradit tu toninu, ktora je v zodpovedajucom pare
		
		if(keyPair != null) {
			System.out.println("\tKey pair previously determined, trying to assign to one of pair");
			if(keyPair.firstMidiNum == durKeyNum) {
				scale = new Scale(durKeyNum, ScaleType.DUR);
				System.out.println("\tDUR found from key pair");
			} else if(keyPair.secondMidiNum == molKeyNum) {
				scale = new Scale(durKeyNum, ScaleType.MOL);
				System.out.println("\tMOL found from key pair");
			} else {
				scale = new Scale(durKeyNum, ScaleType.DUR_MOL);
				System.out.println("\tNot confirmed from key pair");
			}
		} else {
			scale = new Scale(durKeyNum, ScaleType.DUR_MOL);
		}
		
		chBeat.setScale(scale, song);
		
		// Ak sme urcili dur alebo mol treba ist na hladanie meniaceho tonu
		if((scale.scaleType == ScaleType.DUR) || (scale.scaleType == ScaleType.MOL)) {
			step21(scale);
			return;
		} else {
			if(moveSectionStartToNextBeatFrom(chBeat)) {
				step24(d7Mark);
				return;
			} else {
				step28();
				return;
			}
		}
		
	}
	
	
	private void step10(ChordMark chMark, Beat chBeat) {
		System.out.println("Step10: Joined with #6");
	}
	
	private void step11(ChordMark dm7Mark, Beat dm7Beat) {
		step11(dm7Mark, dm7Beat, null);
	}
		
	private void step11(ChordMark dm7Mark, Beat dm7Beat, KeySignature.KeyPair keyPair) {
		System.out.println("Step11: Determine key for Dm7");
		// pre overenie
		assert dm7Mark.getChord() == Chord.DIMINISHED_MINOR_SEVENTH;
		
		// opat finta proti modulu zapornych cisel
		int durMolKeyNum = (12 + (dm7Mark.getMidiNum() - 2) ) % 12;
		
		System.out.print("Key in beat: " + dm7Beat.orderInSong + "/" + dm7Beat.orderInMeasure + ":");
		System.out.println(ChordMark.midiNumToToneString(durMolKeyNum) + " DUR/" +
						   ChordMark.midiNumToToneString(durMolKeyNum) + "MOL");
		
		// DONE Toninovu dvojicu berieme bud z predznamenania alebo !!!! z poslednej urcenej stupnice - aktualnej
		// DONE ak pozname predznamenie cize toninovu dvojicu potom priradit tu toninu, ktora je v zodpovedajucom pare
		Scale scale = null;
		if(keyPair != null) {
			System.out.println("\tKey pair previously determined, trying to assign to one of pair");
			if(keyPair.firstMidiNum == durMolKeyNum) {
				scale = new Scale(durMolKeyNum, ScaleType.DUR);
				System.out.println("\tDUR found from key pair");
			} else if(keyPair.secondMidiNum == durMolKeyNum) {
				scale = new Scale(durMolKeyNum, ScaleType.MOL);
				System.out.println("\tMOL found from key pair");
			} else {
				scale = new Scale(durMolKeyNum, ScaleType.DUR_MOL);
				System.out.println("\tNot confirmed from key pair");
			}
		} else {
			scale = new Scale(durMolKeyNum, ScaleType.DUR_MOL);
			
			// TODO tiez algoritmus ako pri klamnom zavere?		
			// TODO co ak nastavime dur aj mol ???
			
			// TODO Ak mame tri akordy Dim7, Dm7, D7 potom pozname novu toninovu dvojicu - rovnomenne toniny
			// mozeme vyskusat najprv dur, mol ako na zaciatku a podla toho urcit a zaroven neurceny usek tiez nastavit
		}
		
		dm7Beat.setScale(scale, song);		

		// Ak sme urcili dur alebo mol treba ist na hladanie meniaceho tonu
		if(scale.scaleType.equals(ScaleType.DUR) || scale.scaleType.equals(ScaleType.MOL)) {
			step21(scale);
			return;
		}
		
		if(moveSectionStartToNextBeatFrom(dm7Beat)) {
			step24(dm7Mark);
		} else {
			step28();
		}
	}
		

	private void step12() {
		System.out.println("Step12: Joined into #11");
	}
	
	private boolean hasSameTones(ChordMark chordA, ChordMark chordB) {
		Set<Integer> chordATones = new HashSet<Integer>(Arrays.asList(chordA.getChordMidiNums()));
		Set<Integer> chordBTones = new HashSet<Integer>(Arrays.asList(chordB.getChordMidiNums()));
		
		if( chordATones.containsAll(chordBTones) ) {
			return true; 
		}
		return false;
	}
	
	private BeatChord getNextKnownChordFromDifferentFrom(ChordMark differentFromMark, Beat differentFromBeat) {
		// 2 - preskoc mnohonasobne casti Dim7/D7/etc. akordov
		Beat nextBeat = song.getNextBeat(differentFromBeat);
		
		if(nextBeat == null) {
			return null;
		}
		
		Set<Integer> chordTones = new HashSet<Integer>(Arrays.asList(differentFromMark.getChordMidiNums()));
		Set<Integer> nextBeatTones = nextBeat.getToneMap().keySet();
		
		if( chordTones.containsAll(nextBeatTones) ) {
			return getNextKnownChordFromDifferentFrom(differentFromMark, song.getNextBeat(nextBeat)); 
		}
			
		BeatChord nextChord = getNextKnownChordFrom(differentFromBeat);
		
		// preskoc prazdne
		if(nextChord == null) {
			return getNextKnownChordFromDifferentFrom(differentFromMark, song.getNextBeat(nextBeat));
		}
		
		// 1 - preskoc mnohonasobne rovnake dim7 akordy
		while((nextChord != null) && (  nextChord.equals(differentFromMark.getChord()) && (nextChord.chMark.getMidiNum() == differentFromMark.getMidiNum())  )) {
			nextChord = getNextKnownChordFrom(nextChord.calBeat);
		}
		
		// vrat akord
		return nextChord;
	}
	
		// TODO zatial nevieme aka je to tonina, pretoze obe tam patria - toto sa nam potvrdi na dalsom + alebo - akorde
		// ako vlastne urcime co je zakladny ton dim7 a stupnice?
		// rozvoj moze byt - bud stupne 1 polton (plus 1 midi cislo % 12) potom zakladnym tonom dim7 je povodny ton, ktory postupil a zakladnym tonom stupnice je ten ton, do ktoreho dim7 postupil
		// ak postupia dva tony o 1 polton - potom ak je ich vzdialenost 3 % 12 potom spodny je zakladny ton dim7 a ton, do ktoreho postupi je zakldany ton toniny.
		// ak postupia dva tony o 1 polton - potom ak je ich vzdialenost 9 % 12 potom vrchny je zakladny ton dim7 a ton, do ktoreho postupi je zakldany ton toniny 

	
	private void step13(ChordMark dim7Mark, Beat dim7Beat) {
		step13(dim7Mark, dim7Beat, null);
	}
	
	private void step13(ChordMark dim7Mark, Beat dim7Beat, KeySignature.KeyPair keyPair) {
		System.out.println("Step13: Finding confirmation for Dim7 - 1");
		// pre overenie
		assert dim7Mark.getChord() == Chord.DIMINISHED_SEVENTH;
		
		BeatChord beatChord = getNextKnownChordFromDifferentFrom(dim7Mark, dim7Beat);		
		ChordMark nextChMark = beatChord.chMark;
		
		System.out.println(dim7Beat);
/*		if(dim7Beat.measure > 150) {
			try {
				System.in.read();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/
		
		if(! nextChMark.getChord().equalsToOneOf(Chord.MAJOR_TRIAD, Chord.MINOR_TRIAD)) {
			if(moveSectionStartToNextBeatFrom(dim7Beat)) {
				step26();
				return;
			} else {
				step28();
				return;
			}
		}
		
		Integer[] chordTones = dim7Mark.getChordMidiNums();
		
		for (Integer basicTone : chordTones) {
			int incrementedTone = (basicTone + 1) % 12;
			
			int majMinBasicTone = nextChMark.getMidiNum();
			
			if(majMinBasicTone == incrementedTone) {
				Beat nextChBeat = beatChord.calBeat;
				
				Scale scale = null;
				if(nextChMark.getChord().equals(Chord.MAJOR_TRIAD)) {
					scale = new Scale(incrementedTone, ScaleType.DUR);
				} else if(nextChMark.getChord().equals(Chord.MINOR_TRIAD)) {
					scale = new Scale(incrementedTone, ScaleType.MOL);
				}
				
				dim7Beat.chordMark.setMidiNum(basicTone);
				
				for (int i = dim7Beat.orderInSong; i <= nextChBeat.orderInSong; i++) {
					Beat b = song.beats.get(i);
					b.setScale(scale, song);	
				}				
				
				// ak sme nasli spravny akord potom chod na hladanie meniaceho tonu za maj/min
				if (moveSectionStartToNextBeatFrom(nextChBeat)) {
					// hladaj meniaci ton
					step21(scale);
				} else {
					step28();
				}
			}
		}
		
		// ak sme nenasli spravny maj/min akord potom hladaj podla dalsieho akordu
		if(moveSectionStartToNextBeatFrom(dim7Beat)) {
			step26();
			return;
		} else {
			step28();
			return;
		}
	}
	
	
	private void oldStep13(ChordMark dim7Mark, Beat dim7Beat, KeySignature.KeyPair keyPair) {		
		System.out.println("Step13: Finding confirmation for Dim7 - 1");
		// pre overenie
		assert dim7Mark.getChord() == Chord.DIMINISHED_SEVENTH;
		// najdi najblizsi znamy (iny) akord, tj. taky, ktory neobsahuje tony aktualneho dim7
		// dim7#1, dim7#1, INY_AKORD
		// alebo dim7#1, --#1 - --#1 je vysek z dim7#1 potom mozeme preskocit --#1 (aj viac krat) a dalej hladat potvrdenie
		BeatChord beatChord = getNextKnownChordFromDifferentFrom(dim7Mark, dim7Beat);		
		ChordMark nextChMark = beatChord.chMark;
		Beat nextChBeat = beatChord.calBeat;
		
		// TODO INY_AKORD == +/-
		// TODO zatial nevieme aka je to tonina, pretoze obe tam patria - toto sa nam potvrdi na dalsom + alebo - akorde
		// ako vlastne urcime co je zakladny ton dim7 a stupnice?
		// rozvoj moze byt - bud stupne 1 polton (plus 1 midi cislo % 12) potom zakladnym tonom dim7 je povodny ton, ktory postupil a zakladnym tonom stupnice je ten ton, do ktoreho dim7 postupil
		// ak postupia dva tony o 1 polton - potom ak je ich vzdialenost 3 % 12 potom spodny je zakladny ton dim7 a ton, do ktoreho postupi je zakldany ton toniny.
		// ak postupia dva tony o 1 polton - potom ak je ich vzdialenost 9 % 12 potom vrchny je zakladny ton dim7 a ton, do ktoreho postupi je zakldany ton toniny
		if(nextChMark.getChord().equalsToOneOf(Chord.MAJOR_TRIAD, Chord.MINOR_TRIAD)) {
			Integer[] nextChordTones = nextChMark.getChordMidiNums();
			Integer[] incrementedTones = new Integer[2];
			
			int numIncremented = 0;
			
			for (Integer dim7Tone : dim7Mark.getChordMidiNums()) {
				if(Arrays.asList(nextChordTones).contains((dim7Tone + 1) % 12)) {
					incrementedTones[numIncremented] = dim7Tone;
					numIncremented++;
				}
			}
			
			if(numIncremented == 1) {
				System.out.println("\t1 tone incremeted, dim7 basic tone is " + MidiReader.NOTE_NAMES[((incrementedTones[0] - 1 + 12) % 12)] );
				
				ScaleType scaleType = nextChMark.getChord().equalsToOneOf(Chord.MAJOR_TRIAD) ? ScaleType.DUR : ScaleType.MOL;
				Scale scale = new Scale(incrementedTones[0] % 12, scaleType); 
				
				System.out.println("Scale: " + scale);
				
				System.out.println("From beat #" + dim7Beat.orderInSong + " until #" + nextChBeat.orderInSong + 
						   " is the scale " + scale);
				
				for(int i=dim7Beat.orderInSong; i<=nextChBeat.orderInSong; i++) {
					Beat beatInInterval = song.beats.get(i);
					
					beatInInterval.setScale(scale, song);
				}
				
				if(moveSectionStartToNextBeatFrom(nextChBeat)) {
					step21(scale);
					return;
				} else {
					step28();
					return;
				}
			} else if(numIncremented == 2) {
				int incTonesDistance = (incrementedTones[0] - incrementedTones[1] + 12) % 12;
				
				if(incTonesDistance == 3) {
					System.out.println("2 MAJ MIN tones incremented, distance is 3");
					ScaleType scaleType = nextChMark.getChord().equalsToOneOf(Chord.MAJOR_TRIAD) ? ScaleType.DUR : ScaleType.MOL;
					int basicScaleTone = Math.min((incrementedTones[0] + 1) % 12, (incrementedTones[1] + 1) % 12);
					Scale scale = new Scale(basicScaleTone, scaleType); 
					
					System.out.println("Scale: " + scale);
					
					System.out.println("From beat #" + dim7Beat.orderInSong + " until #" + nextChBeat.orderInSong + 
							   " is the scale " + scale);
					
					// TODO nastavit a vypisat zakladny ton dim7
					
					for(int i=dim7Beat.orderInSong; i<=nextChBeat.orderInSong; i++) {
						Beat beatInInterval = song.beats.get(i);
						
						beatInInterval.setScale(scale, song);
					}
					
					if(moveSectionStartToNextBeatFrom(nextChBeat)) {
						step21(scale);
						return;
					} else {
						step28();
						return;
					}
				} else if(incTonesDistance == 9) {
					System.out.println("2 MAJ MIN tones incremented, distance is 9");
					ScaleType scaleType = nextChMark.getChord().equalsToOneOf(Chord.MAJOR_TRIAD) ? ScaleType.DUR : ScaleType.MOL;
					int basicScaleTone = Math.max((incrementedTones[0] + 1) % 12, (incrementedTones[1] + 1) % 12);
					Scale scale = new Scale(basicScaleTone, scaleType); 
					
					System.out.println("Scale: " + scale);
					
					System.out.println("From beat #" + dim7Beat.orderInSong + " until #" + nextChBeat.orderInSong + 
							   " is the scale " + scale);
					
					// TODO nastavit a vypisat zakladny ton dim7
					
					for(int i=dim7Beat.orderInSong; i<=nextChBeat.orderInSong; i++) {
						Beat beatInInterval = song.beats.get(i);
						
						beatInInterval.setScale(scale, song);
					}
					
					if(moveSectionStartToNextBeatFrom(nextChBeat)) {
						step21(scale);
						return;
					} else {
						step28();
						return;
					}
				} else {
					if(moveSectionStartToNextBeatFrom(dim7Beat)) {
						step26();
						return;
					} else {
						step28();
						return;
					}
				}
			} else { // ak ziaden nepostupil - skusaj akordy
				if(moveSectionStartToNextBeatFrom(dim7Beat)) {
					step26();
					return;
				} else {
					step28();
					return;
				}
			}			
		}
		
		// INY_AKORD == dim7#2 - tj. s inym zakladnym tonom
		if(nextChMark.getChord().equals(Chord.DIMINISHED_SEVENTH)) {
			// ci nenastala chyba a nemame rovnaky zakladny ton
			assert hasSameTones(dim7Mark, nextChMark);
			
					
			// TODO spravit pre urcenu toninu, nielen dvojicu
			// vieme iba v tom pripade, ked mame urcenu toninu a potom ak medzi 4 tonami tohoto akordu 
			// je po zvyseni o 1 polton ton = zakladnemu tonu stupnice potom aj na tejto dobe 
			// ostava ta ista stupnica, zakladny ton je povodny (nezvyseny), na ktorom sme nasli zhodu, 
			// priradime akordu   


			if(keyPair != null) {
				// A	ak pozname toninovu dvojicu 
				//		ak je toninova dvojica (C/Am - paralelnych) paralelna potom vieme urcit 
				// ak nejaky ton z akordu po zvyseni o 1 = niektoremu zakldanemu tonu z dvojice potom vieme, 
				// ze toninova dvojica pokracuje (nevieme akordu urcit zakladny ton)
				if(keyPair.isParalel()) {
					Integer[] chordTones = nextChMark.getChordMidiNums();
					for (Integer chordTone : chordTones) {
						if((chordTone + 1 == keyPair.firstMidiNum) || (chordTone + 1 == keyPair.secondMidiNum) ) {
							System.out.println("The keypair: " + keyPair + " remains");
							
							if(moveSectionStartToNextBeatFrom(dim7Beat)) {
								step26();
								return;
							} else {
								step28();
								return;
							} // TODO posielat aj keyPair 
							// TODO ist na 26?
						}
					}
					
				}
				
				// ak je t.dvojica rovnomenna (Cdur/Cmol rovnomenna) potom vieme podla tonu, ktory stupol o 1 polton
				// oproti predchadzajucemu dim7 akordu
				// urcit zakladny ton druheho akordu tj. Dim7#2 - je to jeho "nestupnuta" verzia a dalej pokracuje toninova dvojica
				if(keyPair.isSameNamed()) {
					Integer[] nextChordTones = nextChMark.getChordMidiNums();
					for (Integer nextChordTone : nextChordTones) {
						Integer[] thisChordTones = dim7Mark.getChordMidiNums();
						for (Integer thisChordTone : thisChordTones) {
							// ak niektory ton stupol
							if(thisChordTone + 1 == nextChordTone) {
								System.out.println("The keypair: " + keyPair + " remains");
								System.out.println("Dim7 basic tone is " + thisChordTone);
								
								if(moveSectionStartToNextBeatFrom(dim7Beat)) {
									step26();
									return;
								} else {
									step28();
									return;
								} // TODO posielat aj keyPair 
								// TODO ist na 26?
							}
						}						
					}
					
				}
				
			}
			
			// TODO co ak nevieme tonovu dvojicu - asi ideme testovat dalej
			if (moveSectionStartToBeat(nextChBeat)) {
				// hladaj dalsi akord - vrati sa na 13
				step26();
			} else {
				step28();
			}
			
			
		}
		
		// 4 - dim7#1, D7#1 - urcime podla D7 a rovnaku podpiseme aj pod dim7 (plati aj pre nekompletny)
		// INY_AKORD == D7#1|D7INC#1
		if(nextChMark.getChord().equalsToOneOf(Chord.DOMINANT_SEVENTH, Chord.DOMINANT_SEVENTH_INCOMPLETE)) {
			if(moveSectionStartToNextBeatFrom(dim7Beat)) {
				step26();
				return;
			} else {
				step28();
				return;
			} // TODO posielat aj keyPair
			// DONE Nastavi sa to podla D7 pri spatnom vracani? tj. ak su tony Dim7 vsetky v tonine
			// NIE TODO treba prerobit algoritmus spatneho priradovania aby bral 
			// do molovej stupnice siedmy zvyseny tj. (o 1 polton nizsie ako zakl. ton molovej)
			// do durovej st. znizeny siesty (pri C dur je to As)
			// urcene v dobe s D7
		}
		
		// 5 - Ak nasleduje maj+7#1|min+7#1|min-7#1
		// INY_AKORD = dim7#1, maj+7#1|min+7#1|min-7#1
		// TODO tiez molovy, co to znamena?
		if (nextChMark.getChord().equalsToOneOf(Chord.MAJOR_SEVENTH,
				Chord.MINOR_SEVENTH, Chord.MINOR_MAJOR_SEVENTH)) {
			Integer[] dim7Midis = dim7Mark.getChordMidiNums();

			// zisti ci z dim7 jeden ton postupil na
			// zakladny ton zo skupiny testovanych akordov
			for (Integer dim7Tone : dim7Midis) {
				// ak sa z dim7#1 jeden ton posuva (+1 midi) na zakladny ton maj+7#1|min+7#1|min-7#1 
				if (((dim7Tone + 1) % 12) == nextChMark.getMidiNum()) {
					
					// TODO !!! mozno bude treba porovnavat realne vzdialenosti (nie modulo 12)
					
					// potom zakladny ton maj+7#1|min+7#1|min-7#1 je zakladny ton toniny
					int scaleMidiNum = nextChMark.getMidiNum();
					ScaleType scaleType = null;

					switch (nextChMark.getChord()) {
					case MAJOR_SEVENTH:
						scaleType = ScaleType.DUR;
						break;
					default:
						scaleType = ScaleType.MOL;
						break;
					}

					// TODO vieme este urcit zakladny ton dim7, je to povodny ktory stupol do maj+7 atd.
					
					Scale scale = new Scale(scaleMidiNum, scaleType);

					dim7Beat.setScale(scale, song);
					nextChBeat.setScale(scale, song);

					if (moveSectionStartToNextBeatFrom(nextChBeat)) {
						step26();
						return;
					} else {
						step28();
						return;
					}

				}
			}

			// ziaden nepostupil potom nevieme urcit toninu pre dim7
			
			System.out.println("Cannot determine the scale, going to next beat");

			if (moveSectionStartToBeat(nextChBeat)) {
				// hladaj dalsi akord - vrati sa na 13
				step26();
			} else {
				step28();
			}

			return;
		}
		
		// 6 - dim7#1, Dm7
		// INY_AKORD = Dm7
		if(nextChMark.getChord().equals(Chord.DIMINISHED_MINOR_SEVENTH)) {

			
			Integer[] dim7Midis = dim7Mark.getChordMidiNums();
			
			// pozreme ci niektory z tonov dim7 = zakladnemu tonu Dm7
			for (Integer dim7Tone : dim7Midis) {
				if(dim7Tone == nextChMark.getMidiNum()) {
					// zakladny ton je o 2 nizsie ako zakladny ton Dm7?
					int scaleMidiNum = (dim7Tone + 12 - 2) % 12;
					// aky je typ toniny? DUR_MOL?
					Scale scale = new Scale(scaleMidiNum, ScaleType.DUR_MOL);
					
					// TODO pri spatnom prehladavani, treba pozerat aj na DUR_MOL a na toninove
					
					// ak mame jednoznacne urcenu toninu a jedna tonina pasuje na zakladny akord Dm7 
					// potom nastavime tuto toninu pod Dim7 aj pod Dm7 a ideme dalej - tj hladame ton, kt meni toninu

					dim7Beat.setScale(scale, song);
					nextChBeat.setScale(scale, song);
					
					
					if (moveSectionStartToBeat(nextChBeat)) {
						// hladaj meniaci ton
						step21(scale);
					} else {
						step28();
					}
					
					return;
				}
			}
			
		}
		
		// 7 - dim7#1, Aug+7
		// INY_AKORD = Aug+7
		if(nextChMark.getChord().equalsToOneOf(Chord.AUGMENTED_SEVENTH, Chord.AUGMENTED_TRIAD)) {
			BeatChord chordAfterAugPlus7 = getNextKnownChordFrom(nextChBeat);
			Integer[] chordAfterAugPlus7Tones = chordAfterAugPlus7.chMark.getChordMidiNums();
			
			// ak zakladny ton aug+7 klesa o 1 polton v dalsom rozvedeni
			int loweredBasicAugPlus7Tone = (nextChMark.getMidiNum() + 12 - 1) % 12;

			Scale scale = null;
			
			if(Arrays.asList(chordAfterAugPlus7Tones).contains(loweredBasicAugPlus7Tone)) {
				// potom zakladny ton toniny je tercia k zakladnemu tonu aug+7
				int scaleMidiNum = (nextChMark.getMidiNum() + 4) % 12;
				// a nastavime dur
				scale = new Scale(scaleMidiNum, ScaleType.DUR);
			} else if(Arrays.asList(chordAfterAugPlus7Tones).contains( (nextChMark.getMidiNum() - 3) % 12)) {
				// ak nenajdeme dur ale dalsi akord obsahuj o 9 zvyseny ton potom
				int scaleMidiNum = (nextChMark.getMidiNum() - 3 + 12) % 12;
				// a nastavime mol
				scale = new Scale(scaleMidiNum, ScaleType.MOL);
			}
			
			// TODO toto urcovanie Aug7 teba urobit vseobecne a pridat do bodov 6 aj 26
			
			if(scale != null) {
				dim7Beat.setScale(scale, song);
				nextChBeat.setScale(scale, song);
				
				if (moveSectionStartToBeat(nextChBeat)) {
					// hladaj meniaci ton
					step21(scale);
				} else {
					step28();
				}
				
				return;
			}
			
			// ak sme nevedeli najst toniu pre aug7, tak sa posunieme az za Aug7
			
			if (moveSectionStartToNextBeatFrom(nextChBeat)) {
				step26(scale);
			} else {
				step28();
			}			
			
		}
		
		// TODO
		// TODO Co ak to nie je ani jeden z tychto akordov, moze sa to vobec stat?
		
		System.out.println(nextChMark);
		System.out.println(nextChMark.getChord());
		System.out.println("Chord tones: " + nextChMark.getChordTonesString());
		System.out.println("DEGUG, END");
		

	
		// DONE zmenit hladanie dalsieho znameho akordu takto,
		// 1 - DONE v hladani -  dim7#1, dim7#1, ..., +/- toto vieme vyriesit - toto je vlastne napisane hore --^
		// DONE 3 - Ak dim7#1, dim7#2 - vieme iba v tom pripade, ked mame urcenu toninu a potom ak medzi 4 tonami tohoto akordu je po zvyseni o 1 polton ton = zakladnemu tonu stupnice potom aj na tejto dobe ostava ta ista stupnica, zakladny ton je povodny (nezvyseny), na ktorom sme nasli zhodu, priradime akordu   
		// A							ak pozname toninovu dvojicu 
		// 									ak je toninova dvojica (C/Am - paralelnych) paralelna potom vieme urcit ak nejaky ton z akordu po zvyseni o 1 = niektoremu zakldanemu tonu z dvojice potom vieme, ze toninova dvojica pokracuje (nevieme akordu urcit zakladny ton)
		//									ak je t.dvojica rovnomenna (Cdur/Cmol rovnomenna) potom vieme podla tonu, ktory stupol o 1 polton urcit zakladny ton akordu - je to jeho "nestupnuta" verzia a dalej pokracuje toninova dvojica		

		// 4 - dim7#1, D7#1 - urcime podla D7 a rovnaku podpiseme aj pod dim7 (plati aj pre nekompletny)
		// 5 - dim7#1, maj+7#1|min+7#1|min-7#1 - ak sa z dim7#1 jeden ton posuva (+1 midi) 
		// na zakladny ton maj+7#1|min+7#1|min-7#1 potom zakladny ton maj+7 je zakladny ton toniny
		// 5 - DONE moze sa aj viac ako 1 posuvat (ale vzdy len jeden nahor)
		// 5 - DONE co ak sa ziadny neposunie, nastavit obe toniny na dobu s dim7? (beriem ze ano) - opravit
		//	   DONE nevieme urcit ani zakladny ton akordu ani stupnicu
		// 5 - DONE ak postupil potom nastavit stupnicu aj pod maj+7#1|min+7#1|min-7#1 a potom ist hladat az za ne? (beriem ze ano)
		//     DONE ano nastavit aj na dalsi
		// 5 - DONE a aku stupnicu nastavit?
		// 	   DONE podla nazvu akordu ak zacina na MAJOR potom dur inac MOL
		
		// DONE 6 - dim7#1, Dm7 - pozreme ci niektory z tonov dim7 = zakladnemu tonu Dm7, 
		// ak ano potom aj Dim7 aj Dm7 
		// patria do tej istej toniny urcenej cez Dm7 (o 2 poltony nizsie ako zakladny ton Dm7 = zak. ton stupnice)
		// DONE 7 - dim7#1, Aug+7|Aug - ak zakladny ton aug+7 klesa o 1 polton v dalsom rozvedeni 
		// potom zakladny ton toniny je tercia k zakladnemu tonu aug+7
		
		// dim7#1, AK - kde AK nie je +/- ani vysek -- z dim7 potom na dobe s dim7#1 nevieme urcit toninu
		
		System.out.println("Step13: Finding confirmation for Dim7");
		
	}
	
		
	private ScaleType competeScales(Beat startBeat, final int basicTone) {

		final Integer[] DUR_INT = Arrays.copyOf(KeySignature.DUR_INTERVALS, 7);
		final Integer[] MOL_INT = Arrays.copyOf(KeySignature.MOL_INTERVALS, 7);
		
		Beat nextBeat = startBeat;
		final Set<Integer> chordTones = new HashSet<Integer>();
		
		class ScaleTypeReturner {
			Integer[] durMidiTones;
			Integer[] molMidiTones;
			
			public ScaleTypeReturner() {
				durMidiTones = new Scale(basicTone, ScaleType.DUR).getMidiTones();
				molMidiTones = new Scale(basicTone, ScaleType.MOL).getMidiTones();
			}
			
			public ScaleType getScaleType() {
				int scaleCode = 0; // 00 ziadna 0, 01 - dur 1, 10 - mol 2, 11 dur aj mol 3
				
				if(Arrays.asList(durMidiTones).containsAll(chordTones)) {
					scaleCode += 1;
				}
				
				if(Arrays.asList(molMidiTones).containsAll(chordTones)) {
					scaleCode += 2;
				}
				
				if( (scaleCode > 0) && (scaleCode < 3) ) {
					switch (scaleCode) {
						case 1: return ScaleType.DUR;
						case 2: return ScaleType.MOL;
					}
				}	
				
				if(scaleCode == 3) {
					// TODO co ak obe skoncili na rovnakej dobe? Vratit DUR_MOL?
					return ScaleType.DUR_MOL;
				} else {
					return null;
				}
			}
		}
		
		while( (nextBeat != null) && (chordTones.size() < 7) ) {
			if(nextBeat.chordMark != null) {
				chordTones.addAll(Arrays.asList(nextBeat.chordMark.getChordMidiNums()));
			}
			
			Integer[] durMidiTones = new Scale(basicTone, ScaleType.DUR).getMidiTones();
			Integer[] molMidiTones = new Scale(basicTone, ScaleType.MOL).getMidiTones();
			
			
			ScaleType scaleTypeInBeat = new ScaleTypeReturner().getScaleType();
						
			if(scaleTypeInBeat == null) {
				nextBeat = song.getNextBeat(nextBeat);
			} else {
				// TODO co ak skoncia na rovnakej dobe, vratit DUR_MOL?
				return scaleTypeInBeat;
			}
		}
		
		if(nextBeat == null) { // nevedeli sme najst 7 akordickych
			// TODO zober kolko sa da akordickych a dopln hocijakymi aj v bode 17
			// TODO ako na toto?
			nextBeat = startBeat;
			Set<Integer> beatsTones = new HashSet<Integer>();
			
			while( (nextBeat != null) && (beatsTones.size() < 7) ) {
				beatsTones.addAll(nextBeat.getToneMap().keySet());
			}
			
		}
		
		return null;
	}

	private void oldstep13(ChordMark dim7Mark, Beat dim7Beat) {
		System.out.println("Step13: Finding confirmation for Dim7 - 1");
		// pre overenie
		assert dim7Mark.getChord() == Chord.DIMINISHED_SEVENTH;
		// najdi najblizsi znamy akord
		// TODO zmenit hladanie dalsieho znameho akordu takto,
		// DONE v hladani -  dim7#1, dim7#1, ..., +/- toto vieme vyriesit - toto je vlastne napisane hore --^
		// ??? co ak dim7#1, dim7#2 - napisat pod dim7#1 obe toniny a ist riesit dim7#2? 
		// dim7#1, D7#1 - urcime podla D7 a rovnaku podpiseme aj pod dim7
		// dim7#1, --#1 - --#1 je vysek z dim7#1 potom mozeme preskocit --#1 (aj viac krat) a dalej hladat potvrdenie
		// dim7#1, maj+7#1|min+7#1|min-7#1 - ak sa z dim7#1 jeden ton posuva (+1 midi) na zakladny ton maj+7#1|min+7#1|min-7#1 potom zakladny ton maj+7 je zakladny ton toniny		// dim7#1, AK - kde AK nie je +/- ani vysek -- z dim7 potom na dobe s dim7#1 nevieme urcit toninu
		// dim7#1, AK kde AK je Dm7, Aug+7, -- (kde nie je vysek z dim7#1 p
		
		
		// najdeme najblizsi akord
		BeatChord beatChord = getNextKnownChordFromSectionStart();		
		ChordMark durMolChMark = beatChord.chMark;
		Beat durMolBeat = beatChord.calBeat;
		
		// ak nevieme najst taky akord, tak sa posun na 24 alebo 28
		if(beatChord.calBeat == null || beatChord.chMark == null) {
			if(moveSectionStartToNextBeat()) {
				System.out.println("going to 24? to be defined - scale unknown");
				step24(dim7Mark);
			} else {
				step28();
			}
			return;
		}
		
		
		boolean confirmed  = durMolChMark.getChord() == Chord.MAJOR_TRIAD;
				confirmed |= durMolChMark.getChord() == Chord.MINOR_TRIAD;
				
		if(confirmed) {
			step14(dim7Mark, durMolChMark, dim7Beat, durMolBeat);
			return;
		} else {
			if(moveSectionStartToNextBeat()) {
				System.out.println("going to 24? to be defined - scale unknown");
				// TODO zmenit na bod 26
				step26();
				//step24(dim7Mark);
			} else {
				step28();
			}
		}		
	}	

	private void step14(ChordMark dim7Mark, ChordMark durMolChMark, Beat dim7Beat, Beat durMolBeat) {
		System.out.println("Step14: Determine basic tone for + or - chord after Dim7");
		
		// pre overenie
		assert dim7Mark.getChord() == Chord.DIMINISHED_SEVENTH;
		assert durMolChMark.getChord() == Chord.MAJOR_TRIAD || durMolChMark.getChord() == Chord.MINOR_TRIAD;
		
		int durMolBasicTone = durMolChMark.getMidiNum() % 12;
		System.out.println("Basic tone for + or - is: " + durMolChMark.getChordToneString() + " (" + durMolBasicTone + ")");
		step15(dim7Mark, durMolChMark, dim7Beat, durMolBeat, durMolBasicTone);
	}	

	private void step15(ChordMark dim7Mark, ChordMark durMolChMark, Beat dim7Beat, Beat durMolBeat, int durMolBasicTone) {
		System.out.println("Step15: Finding confirmation for Dim7 - 2");
		// najdi ton o poltonu nizsi
		int loweredBasicTone = (12 + durMolBasicTone - 1) % 12;
		
		boolean containsLowered = false;
		Integer[] chordMidiNums = dim7Mark.getChordMidiNums();
		
		for (int i = 0; i < chordMidiNums.length; i++) {
			if((chordMidiNums[i] % 12) == loweredBasicTone) {
				containsLowered = true;
				break;
			}
		}
		
		if(containsLowered) {
			step16(dim7Mark, durMolChMark, dim7Beat, durMolBeat);
			return;
		} else {
			step17(dim7Beat);
		}		
	}

	private void step16(ChordMark dim7Mark, ChordMark durMolChMark, Beat dim7Beat, Beat durMolBeat) {
		System.out.println("Step16: Finding confirmation for Dim7 - 3");
		// pre overenie
		assert (durMolChMark.getChord() == Chord.MAJOR_TRIAD) || (durMolChMark.getChord() == Chord.MINOR_TRIAD);
		
		String toneName = durMolChMark.getChordToneString();
		System.out.print("Key in beat: " + dim7Beat.orderInSong + "/" + dim7Beat.orderInMeasure + ":");
		ScaleType scaleType = null;
		if(durMolChMark.getChord() == Chord.MAJOR_TRIAD) {
			System.out.println(toneName + "DUR");
			scaleType = ScaleType.DUR;
		} else {
			System.out.println(toneName + "MOL");
			scaleType = ScaleType.MOL;
		}
		
		Scale scale = new Scale(durMolChMark.getMidiNum(), scaleType);
		
		// TODO Priraď dobám začínajúc dobou s výskytom Dim7 akordu až po dobu s výskytom akordu + alebo – meno tóniny
		Beat oneBeforeDurMol = song.beats.get(durMolBeat.orderInSong - 1);
		System.out.println("From beat #" + dim7Beat.orderInSong + " until #" + oneBeforeDurMol.orderInSong + 
						   " is the scale " + durMolChMark.getChordToneString() + " " + scaleType.toString());
		
		for(int i=dim7Beat.orderInSong; i<=durMolBeat.orderInSong; i++) {
			Beat beatInInterval = song.beats.get(i);
			
			beatInInterval.setScale(scale, song);
		}
		
		if(moveSectionStartToNextBeatFrom(durMolBeat)) {
			step26();
		} else {
			step28();
		}
		
		
		/*if(durMolBeat.orderInSong + 1 < song.beats.size()) {
			Beat oneAfterDurMol = song.beats.get(durMolBeat.orderInSong + 1);
			sectionStart = oneAfterDurMol;
			step26();
		} else {
			step28();
		}*/		 
	}
	
	private void step17(Beat beat) {
		// TODO Prerobit tak aby sa brali 7 akordickych tonov porade bez obmedzeni taktom alebo dobou
		

		
		
		System.out.println("Step17: Finding 7 chord tones in current beat");
		int measureNo = beat.measure;
		Measure measure = song.measures.get(measureNo);
		// - - - - -
		int numBeatsInMeas = (int) measure.getBeatCount();
		
		int measStartBeatId = beat.orderInSong;
		int measEndBeatId = measStartBeatId + (numBeatsInMeas - beat.orderInMeasure - 1);
		
		int lastBeat = measStartBeatId;
		
		List<Integer> chordTones = new LinkedList<Integer>(); 
		for (int i = measStartBeatId; i <= measEndBeatId; i++) {
			Beat curBeat = song.beats.get(i);
			ChordMark chM = curBeat.chordMark;
			// TODO vyriesit nekompletne akordy
			if( (chM != null) && (chM.getChord() != null) && (chM.isIncomplete() == false) ) {
				Integer[] tones = chM.getChordMidiNums();
				for (int j = 0; j < tones.length; j++) {
					int toneNum = tones[j] % 12;
					if( (!chordTones.contains(toneNum)) && (chordTones.size() < 7) ) {
						chordTones.add(toneNum);
						lastBeat = i;
					}
				}
			}
		}
		
		if(chordTones.size() == 7) {
			step19(chordTones, lastBeat);
		} else if(chordTones.size() > 0) { // menej ako 7 ale viac aspon 1
			chordTones.clear();
			int start = beat.orderInSong;
			lastBeat = start;
			for (int i = start; (chordTones.size() < 7) && (i + 1 < song.beats.size()); i++) {
				Beat curBeat = song.beats.get(i);
				ChordMark chM = curBeat.chordMark;
				// TODO vyriesit nekompletne akordy
				if( (chM != null) && (chM.getChord() != null) && (chM.isIncomplete() == false) ) {
					Integer[] tones = chM.getChordMidiNums();
					for (int j = 0; j < tones.length; j++) {
						int toneNum = tones[j] % 12;
						if( (!chordTones.contains(toneNum)) && (chordTones.size() < 7) ) {
							chordTones.add(toneNum);
							lastBeat = i;
						}
					}
				}
			}
			
			if(chordTones.size() == 7) {
				step19(chordTones, lastBeat);
			} else {
				step18(beat);
			}
		} else { // ziadny
			step18(beat);
		}
	}
	

	private void step18(Beat beat) {
		System.out.println("Step18: Get 7 any tones from section start");

		List<Integer> chordTones = new LinkedList<Integer>(); 
		int start = sectionStart.orderInSong;
		int lastBeat = start;
		for (int i = start; (chordTones.size() < 7) && (i + 1 < song.beats.size()); i++) {
			Beat curBeat = song.beats.get(i);
			Set<Integer> toneKeys = curBeat.getToneMap().keySet();
			for (Integer toneKey: toneKeys) {
				toneKey = toneKey % 12;
				if( (!chordTones.contains(toneKey)) && (chordTones.size() < 7) ) {
					chordTones.add(toneKey);
					lastBeat = i;
				}
			}
		}
		
		if(chordTones.size() < 7) {
			if(moveSectionStartToNextBeatFrom(beat)) {
				step26();
			} else {
				step28();
			}
		} else { // nasli sme 7 tonov
			// overenie ci mame prave 7 tonov, teda nie viac
			assert chordTones.size() == 7;
			
			step19(chordTones, lastBeat);
		}		
	}
	
	private static boolean isFromIntervals(List<Integer> chordTones, Integer[] intervals) {
		// urob lokalnu kopiu, ktoru mozno usortovat bez zmeny originalu
		chordTones = new LinkedList<Integer>(chordTones);
		Collections.sort(chordTones);
		
		int[] chordIntervals = new int[chordTones.size()];
		for (int i = 0; i < chordIntervals.length; i++) {
			int tone = chordTones.get(i);
			int nextTone = chordTones.get( (i+1) % chordTones.size());
			int interval = (12 + (nextTone - tone)) % 12;
			
			chordIntervals[i] = interval;
		}
		
		// tu skusam ci po rotacii nedostanem spravny interval
		for (int offset = 0; offset < 7; offset++) {
			boolean isEqual = true;
			for (int i = 0; i < chordIntervals.length; i++) {
				int offsetedTone = chordIntervals[(i + offset) % chordIntervals.length];
				int toneFromInt = intervals[i];
				
				if(toneFromInt != offsetedTone) {
					isEqual = false;
					break;
				}
			}
			if(isEqual) {
				return true;
			}
		}
		
		return false;
	}
	
	private int getChordTonesFirstTone(List<Integer> chordTones, Integer[] intervals) {
		// urob lokalnu kopiu, ktoru mozno usortovat bez zmeny originalu
		chordTones = new LinkedList<Integer>(chordTones);
		Collections.sort(chordTones);
		
		int[] chordIntervals = new int[chordTones.size()];
		for (int i = 0; i < chordIntervals.length; i++) {
			int tone = chordTones.get(i);
			int nextTone = chordTones.get( (i+1) % chordTones.size());
			int interval = (12 + (nextTone - tone)) % 12;
			
			chordIntervals[i] = interval;
		}
		
		// tu skusam ci po rotacii nedostanem spravny interval
		for (int offset = 0; offset < 7; offset++) {
			boolean isEqual = true;
			for (int i = 0; i < chordIntervals.length; i++) {
				int offsetedTone = chordIntervals[(i + offset) % chordIntervals.length];
				int toneFromInt = intervals[i];
				
				if(toneFromInt != offsetedTone) {
					isEqual = false;
					break;
				}
			}
			
			if(isEqual) {
				int firstTone = chordTones.get(offset);
				return firstTone % 12;
			}
		}
		
		throw new RuntimeException("Wrong call of function getChordTonesFirstTone");
	}

	private void step19(List<Integer> chordTones, int lastBeat) {
		System.out.println("Step19: Try to find the scale type dur/mol/chrom");

		final Integer[] DUR_INT = Arrays.copyOf(KeySignature.DUR_INTERVALS, 7);
		final Integer[] MOL_INT = Arrays.copyOf(KeySignature.MOL_INTERVALS, 7);
		final Integer[] CHROM_INT = Arrays.copyOf(KeySignature.CHROM_INTERVALS, 7);
		
		// kontrola
		assert chordTones.size() == 7;
		
		Collections.sort(chordTones);
		
		if(isFromIntervals(chordTones, DUR_INT)) {
			int scaleMidi = getChordTonesFirstTone(chordTones, DUR_INT);
			System.out.println("In Beat #" + sectionStart.toShortString() + " to #" + song.beats.get(lastBeat).toShortString() +
					" is " + MidiReader.NOTE_NAMES[scaleMidi] + " DUR");
			Scale scale = new Scale(scaleMidi, ScaleType.DUR);
			
			for(int i=sectionStart.orderInSong; i<=lastBeat; i++ ) {
				Beat beatInInterval = song.beats.get(i);
				
				beatInInterval.setScale(scale, song);
			}
			
			if(moveSectionStartToNextBeatFrom(song.beats.get(lastBeat))) {
				step21(scale);
			} else {
				step28();
			}
		} else if(isFromIntervals(chordTones, MOL_INT)) {
			int scaleMidi = getChordTonesFirstTone(chordTones, MOL_INT);
			System.out.println("In Beat #" + sectionStart.toShortString() + " to #" + song.beats.get(lastBeat).toShortString() +
					" is " + MidiReader.NOTE_NAMES[scaleMidi] + " MOL");
			Scale scale = new Scale(scaleMidi, ScaleType.MOL);
			
			for(int i=sectionStart.orderInSong; i<=lastBeat; i++ ) {
				Beat beatInInterval = song.beats.get(i);
				
				beatInInterval.setScale(scale, song);
			}
			
			if(moveSectionStartToNextBeatFrom(song.beats.get(lastBeat))) {
				step21(scale);
			} else {
				step28();
			}
		} else if(isFromIntervals(chordTones, CHROM_INT)) {
			int scaleMidi = getChordTonesFirstTone(chordTones, CHROM_INT);
			System.out.println("In Beat #" + sectionStart.orderInSong + " to #" + lastBeat +
					" is " + " CHROM");
			Scale scale = new Scale(scaleMidi, ScaleType.CHROM);
			
			for(int i=sectionStart.orderInSong; i<=lastBeat; i++ ) {
				Beat beatInInterval = song.beats.get(i);
				
				beatInInterval.setScale(scale, song);
			}
			
			if(moveSectionStartToNextBeatFrom(song.beats.get(lastBeat))) {
				step21(scale);
			} else {
				step28();
			}
		} else {
			if(moveSectionStartToNextBeat()) {
				step26();
			} else {
				step28();
			}
		}		
		
	}
	
	private void step20() {
		System.out.println("Step20: Added to step 21");
	}
	
	private void step21(Scale scale) {
		System.out.println("Step21: Search next tones to find one that is not part of last found scale");
		System.out.println("Current beat: " + sectionStart.toShortString());

		// TODO Ak mame nastaveny dur narazime na ton, ktory je v dur medzi 5 tym a siestym (napr. v c dur je to gis/as)
		// 	 	a ak je sucastou akordu, ktory obsahuje bud o 3 nizsi alebo o 4 vyssi (alebo oba) potom ak zaroven tento 
		//      akord neobsahuje iny cudzi ton potom zostava v urcenej dur

		//		Ak vsak najdeme akord, ktory obsahuje ton bud o 4 nizsi a o 3 vyssi alebo oba potom
		//		potom ak zaroven tento akord neobsahuje iny cudzi ton potom zostava v paralelnej mol
		//		nastavime na tuto dobu zmeneny akord, posunieme sa a ideme znova na 21.
		
		
		int i = sectionStart.orderInSong;
		
		while(i < song.beats.size()) {
			Beat curBeat = song.beats.get(i);
			Set<Integer> tonesInBeat = curBeat.getToneMap().keySet();
			
			for (Integer toneMidi : tonesInBeat) {
				if(! scale.isInScaleTones(toneMidi)) {
					
					for (int j = sectionStart.orderInSong; j <= i-1; j++) {
						song.beats.get(j).setScale(new Scale(scale.midiNum, scale.scaleType), song);
					}
					
					sectionStart = curBeat;
					step22(toneMidi, scale);
					return;
				}
			}
			
			System.out.println("\t Same scale as on previous beat?! - to be confirmed");
			
			i++;
		}
		
		// Ak uz nie je ziadny ton, ktory by menil toninu potom skonci
		System.out.println("No tone to change scale - the same until end");
		step28();		
	}
	
	// pozriet bod 22
	private void step22(int tonePotenChangingScale, Scale scale) {
		System.out.println("Step22: Determine whether this tone changes the scale");
		
		System.out.println("Current beat is: " + sectionStart.toShortString());
		System.out.println("\tTone in question is: " + ChordMark.midiNumToToneString(tonePotenChangingScale));
		
		Beat beat = sectionStart;
		
		// ak je na danej dobe ton, ktory je sucastou akordu, no tento akord moze byt aj rozlozeny nie nevyhnutne len v danej dobe (toto ale ma poriesene akordicka analyza
		
		// ak v dobe s potencialne meniacim tonom nie je akord potom nemeni
		// alebo
		// ak mame ton, ktory vznikol posunutim niektoreho tonu zo stupnice o 1 polton ale potom sa ukaze jeho povodna verzia (neposunuta) a aj dalsi najblizsi ton v smere posunutia v ramci toniny (napr. ak zvyseny je FIS potom G aj F v Cdur) tak nemeni
		if(beat.chordMark == null || beat.chordMark.getChord() == null || beat.chordMark.isIncomplete()) {
			System.out.println("Tone " + ChordMark.midiNumToToneString(tonePotenChangingScale) + " in #" + sectionStart.orderInSong +
					"is unimportant and doesn't change the scale");
			
			// POZOR
			beat.setScale(new Scale(scale.midiNum, scale.scaleType), song);
			
			if(moveSectionStartToNextBeat()) {
				step21(scale);
			} else {
				step28();
			}
			return;
		}

		// TODO Ak sa posunuty ton zopakuje (a nie je medzi nim potvrdenie povodnej stupnice) potom ak je sucastou akordu potom
		// meni toninu a menil toninu uz pri svojom prvom vyskyte, teda nastav toninu od prveho vyskytu po opakovanie
		// potom chod na 26
		
		if(beat.chordMark.isIncomplete() == false) {
			Integer[] chordTones = beat.chordMark.getChordMidiNums();

			boolean isUnimportant = true;
			for (int i = 0; i < chordTones.length; i++) {
				// ak je sucastou akordu
				// TODO Alebo sa viac krat opakuje
				if((tonePotenChangingScale % 12) == (chordTones[i] % 12)) {
					isUnimportant = false;
					break;
				}	
			}
			
			if(isUnimportant) {
				System.out.println("Tone " + ChordMark.midiNumToToneString(tonePotenChangingScale) + " in #" + sectionStart.orderInSong +
						"is unimportant and doesn't change the scale");
				// TODO akosi poznacit, ktory ton to je do unimportant listu a prerobit podla toho list
				if(moveSectionStartToNextBeat()) {
					step21(scale);
				} else {
					step28();
				}
			} else {
				if(moveSectionStartToNextBeat()) {
					step23();
				} else {
					step28();
				}
			}
		}
		// TODO zmenu toniny oznacime az ked sa potvrdi vid dole
		// TODO ak sme nasli ton ktory moze menit toninu a potom ak najdem dalsie tony do poctu 7 tonov stupnice z akordov potom zo vsetkych a ked este stale nepasuje do novej toniny potom pozreme ci priamo najblizsie neprazdny akord je D7 a ak ani D7 nepotvrdi potom tento ton aj tak nemeni toninu 
	}
	
	
	private void step23() {
		System.out.println("Step23: Mark beat with changing tone as section start and go to step26");
		
		step26();
	}
	
	// TODO co ak mame akord s meniacim tonom
	private void step24(ChordMark lastChordMark) {
		System.out.println("Step24: Is here the same chord as on previous (non-empty) beat?");

		Beat beat = sectionStart;
		
		// ak v dalsej dobe nie je akord
		if(beat.getChordMark() == null || beat.getChordMark().getChord() == null || beat.getChordMark().isIncomplete()) {
			step26();
			return;
		}

    	// ak je tu to iste	
		if(beat.getChordMark().equals(lastChordMark)) {
			System.out.println("Same chord - scale remains the same");
			step25(lastChordMark);
		} else {
			step26();
		}
	}
	
	private void step25(ChordMark lastChordMark) {
		System.out.println("Step25: If we found same chord at step24 we try again");
		if(moveSectionStartToNextBeat()) {
			step24(lastChordMark);
		} else {
			step28();
		}
	}
	
	private void step26() {
		step26(null);
	}
	
	private void step26(Scale scale) {
		System.out.println("Step26: We ask for know chords");
		Beat beat = sectionStart;
		
		ChordMark chordMark = beat.getChordMark();
		
		if(chordMark == null || chordMark.getChord() == null) {
			step17(beat);
			return;
		}
		
		Chord chord = chordMark.getChord();
		
		switch (chord) {
		case MAJOR_TRIAD:
			System.out.println("\tIs +");
		case MINOR_TRIAD:
			System.out.println("\tIs -");
			step17(beat);
			break;
		case DOMINANT_SEVENTH:
			System.out.println("\tIs D7, going to step 9");
			step9(chordMark, beat);
			break;
		case DOMINANT_SEVENTH_INCOMPLETE:
			System.out.println("\tIs D7 incomplete, going to step 9");
			step9(chordMark, beat);
			break;
		case DIMINISHED_SEVENTH:
			System.out.println("\tIs Dim7, going to step 13");
			step13(chordMark, beat);
			break;
		case DIMINISHED_MINOR_SEVENTH:
			System.out.println("\tIs Dm7, going to step 11");
			step11(chordMark, beat);
			break;
		default:
			step18(beat);
			break;
		}		
		
	}
	
	private void step27() {
		System.out.println("Step27: Added to point 26");
	}
	
	private void step28() {
		System.out.println("Step28: Verification - not implemented yet");
		step29();
	}

	private void step29() {
		System.out.println("Step29: End of program");
	}
	
	private void process() {
		step1();
	}

	public static void main(String[] args) throws IOException {
		/*PrintStream oo = System.out;
		
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("scale_out.txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		System.setOut(new PrintStream(fos));*/
		
		for (int j = 1; j <= 38; j++) {
			String fileName = ((j < 10) ? ("0" + j) : ("" + j) ) + ".MID";
			String analysisName = fileName + ".txt";
			
			System.out.print("Processing file: " + fileName + " ...");
			
			ChordAnalysis chAnalysis = new ChordAnalysis(fileName);
			//ChordAnalysis chAnalysis = new ChordAnalysis("sonataFDurMozart.mid");
			chAnalysis.process();
			
			ScaleAnalysis scAnalysis = new ScaleAnalysis(chAnalysis);
			scAnalysis.process();
			
			//System.setOut(oo);
			
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(analysisName));
		
			int i=0;
			for(Beat b: scAnalysis.song.beats) {
				String line = (i+1) + ": "+ ( (b.chordMark == null) ? "null null" : b.chordMark) + ' ' + b.getScale() + "\n";
				bWriter.write(line);
				i++;
			}
			
			bWriter.close();
			
			System.out.println("done");
		}
		/*
			String fileName = "sonataFDurMozart.mid";
			String analysisName = fileName + ".txt";
			
			System.out.print("Processing file: " + fileName + " ...");
			
			ChordAnalysis chAnalysis = new ChordAnalysis(fileName);
			//ChordAnalysis chAnalysis = new ChordAnalysis("sonataFDurMozart.mid");
			chAnalysis.process();
			
			ScaleAnalysis scAnalysis = new ScaleAnalysis(chAnalysis);
			scAnalysis.process();
			
			//System.setOut(oo);
			
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(analysisName));
		
			int i=0;
			for(Beat b: scAnalysis.song.beats) {
				String line = (i+1) + ": " + ( (b.chordMark == null) ? "null null" : b.chordMark) + ' ' + b.getScale() + "\n";
				bWriter.write(line);
				i++;
			}
			
			bWriter.close();
			
			System.out.println("done");*/
		
						
	}

	
	
}
