import java.io.BufferedWriter;
import java.io.FileWriter;

public class DynamicChordAnalysis {
	private Song song;
	private String midiFile;
	
	public DynamicChordAnalysis(String midiFile) {
		this.midiFile = midiFile;
		MidiReader mr = new MidiReader(midiFile, -1, 32);
		song = mr.read();
	}
	
	/*
	 * Process one measure
	 */
	private void processMeasure(Measure measure) {
		// check whether there are any notes
		if(measure.notes.size() == 0) {
			// add empty measure section
			MeasureSection measureSection = new MeasureSection(measure, 0, measure.getBeatCount());
			measureSection.chordMark = null;
			//System.out.println("Nist tu nie je");
			measure.addMeasureSection(measureSection);
			return;
		}
			
		
		// find minimal length note in the measure and save its duration

		double shortestNoteDuration = measure.getMinimalStepForMeasure();
		
		// initial measureSection in length of shortestNote setting
		double sectionStart = 0.0;
		double sectionEnd = shortestNoteDuration;
		MeasureSection previousSection = null;
				
		MeasureSection measureSection = new MeasureSection(measure, sectionStart, sectionEnd);
		
		while(measureSection != null) {
			System.out.println("Measure section: " + measureSection);
			
			ChordMark chordMarkInSection = measureSection.getChordMark();
			// if chord is found in current section
			if(chordMarkInSection != null) {
				measureSection.chordMark = new ChordMark(chordMarkInSection);
				measure.addMeasureSection(measureSection);
				
				previousSection = measureSection;
				sectionStart = previousSection.endBeat;
				sectionEnd = sectionStart + shortestNoteDuration;
				
				if(sectionStart >= measureSection.measure.getBeatCount()) {
					// TODO Urobit reconciliaciu spat
					System.out.println("Looking back if the chord can be assigned from previous section");
					System.out.println("Previous section: " + previousSection + " chordMark: " + previousSection.chordMark);
					
					System.out.println("End of measure @1");
					return;
				}
				
				measureSection = new MeasureSection(measure, sectionStart, sectionEnd);
			} else { // if the chord is not in current section
				// look if we can find chord in next section
						
				double nextSectionStart =  measureSection.endBeat;
				double nextSectionEnd = nextSectionStart + shortestNoteDuration;
				
				// check if we are in the same measure
				if(nextSectionStart >= measureSection.measure.getBeatCount()) {
					// TODO Urobit reconciliaciu spat
					System.out.println("Looking back if the chord can be assigned from previous section");
					System.out.println("Previous section: " + previousSection + " chordMark: " + ((previousSection == null) ? null : previousSection.chordMark) );

					// if we are not at the very beginning of the measure and there is a chord mark to look on					
					if(previousSection != null && previousSection.chordMark != null) { 
						double chordTonesWeigth = measureSection.getChordTonesWeight(previousSection.chordMark);
						double otherTonesWeight = measureSection.getNonChordTonesWeight(previousSection.chordMark);
						
						System.out.println("chord tones weight: " + chordTonesWeigth);
						System.out.println("non-chord tones weight: " + otherTonesWeight);
						
						// if the chord tones from previous section are the majority of the measure section then assign the same chord
						if(chordTonesWeigth >= otherTonesWeight) {
							measureSection.chordMark = new ChordMark(previousSection.chordMark);
							measure.addMeasureSection(measureSection);
						} else {
							measureSection.chordMark = null;
							measure.addMeasureSection(measureSection);
						}
					} else {
						measure.addMeasureSection(measureSection);
					}
					
					
					System.out.println("End of measure @2");
					return;
				}
				
				MeasureSection nextSection = new MeasureSection(measure, nextSectionStart, nextSectionEnd);
				ChordMark nextSectionChordMark = nextSection.getChordMark();
				
				if(nextSectionChordMark == null) { // if NOT found in next section
					MeasureSection joinedSection = measureSection.merge(nextSection);
										
					/*int difMidiNumCount = joinedSection.getDifMidiNumCount();
					
					
					if(difMidiNumCount > 4) {
						// cut from beginning sections until we have less than 4 tones or a section of length shortestNote 
						//MeasureSection cutSection = new MeasureSection(measure, joinedSection.startBeat, joinedSection.startBeat + shortestNoteDuration);
						
						MeasureSection sectionToBeCut = joinedSection;
						int sectionsCut = 0;
						
						for(;;) {
							boolean isLessEqual4tones = sectionToBeCut.getDifMidiNumCount() <= 4;
							boolean isMinimalSection = sectionToBeCut.getDuration() <= shortestNoteDuration;
							if(isLessEqual4tones || isMinimalSection) {
								break;
							}
							
							sectionToBeCut = sectionToBeCut.cutFromBegining(shortestNoteDuration);
							sectionsCut++;
						}
						
						if(sectionsCut > 0) {
							double cutSecStart = joinedSection.startBeat;
							double cutSecEnd = sectionToBeCut.startBeat;
							MeasureSection cutSection = new MeasureSection(measure, cutSecStart, cutSecEnd);
							
							// in cutSection there are no chordMarks
							measure.addMeasureSection(cutSection);
							
							measureSection = sectionToBeCut;
							previousSection = cutSection;
						} else { // we have minimal section or we failed to get rid of some tones
							// can we get here? I think no
							throw new RuntimeException("Failed to cut from begining of section");
						}
						
						
						
					} else {
						// previousSection remains same, in the beginning it is null
						measureSection = joinedSection;	
					}*/
					
					measureSection = joinedSection;
					
				} else { // if found in next section
					// get chord and non chord tones weight
					double currentSectionChordTonesWeight = measureSection.getChordTonesWeight(nextSectionChordMark);
					double currentSectionNonChordTonesWeight = measureSection.getNonChordTonesWeight(nextSectionChordMark);
					
					// if the sum of weights of chord tones duration >= sum of weight of other tones 
					if(currentSectionChordTonesWeight >= currentSectionNonChordTonesWeight) {
						// assign same chord mark on the current one
						measureSection.chordMark = new ChordMark(nextSectionChordMark);
					}
					// we add section also if no chordMark was found
					measure.addMeasureSection(measureSection);
					
					// assign also on the next one
					nextSection.chordMark = new ChordMark(nextSectionChordMark);
					measure.addMeasureSection(nextSection);
					
					// shift the cursor
					previousSection = measureSection;
					sectionStart = nextSectionEnd;
					sectionEnd = sectionStart + shortestNoteDuration;
					measureSection = new MeasureSection(measure, sectionStart, sectionEnd);
					
					
				}
				
			}	
			
			/*
			// if the measure was not found in previous section and not in current one
			// previousChordMark == null && chordMarkInSection == null 
			} else if(previousSection == null || previousSection.chordMark == null) {
				// we are joining only if we have not found 4 different tones
				if(measureSection.getDifMidiNumCount() < DIF_TONES_COUNT) {
					if(previousSection != null) {
						MeasureSection joinedSection = previousSection.merge(measureSection);
						previousSection = measureSection;
						measureSection = joinedSection;
					} else {
						previousSection = null; // remains null
						sectionStart = measureSection.startBeat; // should be 0
						// DEBUG
						if(sectionStart != 0) throw new RuntimeException("sectionStart should not be 0");
						
						sectionEnd = measureSection.endBeat + shortestNoteDuration;
						measureSection = new MeasureSection(measure, sectionStart, sectionEnd);
					}
				} else {
					measureSection.chordMark = ChordMark.NONE; // mark that this will not be processed
					measure.addMeasureSection(measureSection);
					previousSection = measureSection;
					
					sectionStart = previousSection.endBeat;
					sectionEnd = sectionStart + shortestNoteDuration;
					measureSection = new MeasureSection(measure, sectionStart, sectionEnd);
				}
				
			// if the measure was found in previous section but not found in current one
			// previousChordMark != null && chordMarkInSection == null
			} else if(previousSection.chordMark != null) {
				if(measureSection.getDifMidiNumCount() < DIF_TONES_COUNT) {
					
				} else {
					
				}				
			} else { // if no chord found then look on next section
				previousSection = measureSection;
				previousChordMark = null;
				sectionStart = sectionEnd;
				sectionEnd = sectionEnd + shortestNoteDuration;
			}*/
		}
	}
	
	/*
	 * main function to run whole dynamic chord analysis
	 */
	public void process() throws Exception {
		int i = 1;
		for (Measure measure : song.measures) {
			//if(i == 297) {
				processMeasure(measure);
			//}
			i++;
		}
		
		
		BufferedWriter bWriter = new BufferedWriter(new FileWriter(this.midiFile + "_analysis.txt"));
		
		int m=1;
		for(Measure measure : song.measures) {
			//double measDuration = 0;
			for(MeasureSection measureSection : measure.measureSections) {
				String line = m + ":" + measureSection.endBeat + ": " + measureSection.getChordMark() + "\n";
				//System.out.println(line);
				bWriter.write(line);
				//measDuration += measureSection.getDuration();
			}
			
			//bWriter.write("--> duration: " + measDuration + "\n\n");
			m++;
		}
		
		bWriter.close();
	}
	
	
	public static void main(String[] args) {
		String fileName = "be_son1a.mid";
		if(args != null && args.length > 0) {
			fileName = args[0];
		}
		
		DynamicChordAnalysis dChA = new DynamicChordAnalysis(fileName);
		
		/*List<Measure> measures = dChA.song.measures;
		
		for (Measure measure : measures) {
			System.out.println("==========================");
			System.out.println("Measure length: " + measure.getBeatCount());
			System.out.println("Measure: " + (measure.order ));
			System.out.println("\tMinNote: " + measure.getShortestNote().getDuration());
			System.out.println("\tMinStep: " + measure.getMinimalStepForMeasure());
			System.out.println("\tMinStep: " + measure.getMinimalStepForMeasure());
			System.out.println("--------------------------\n");
		}
		
		/*Measure firstMeasure = measures.get(1);
		
		// 1. doba 1.taktu
		MeasureSection measureSection = new MeasureSection(firstMeasure, 1.0, 2.0);
		
		
		
		for (RawNote noteInSection : measureSection.notes) {
			System.out.println(noteInSection);
		} 
		*/
		
		try {
			dChA.process();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
}
