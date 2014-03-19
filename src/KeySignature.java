
public enum KeySignature {
	CLEAR, ONE_SHARP, TWO_SHARP, THREE_SHARPS, FOUR_SHARPS, FIVE_SHARPS, SIX_SHARPS, SEVEN_SHARPS,
	FOUR_FLATS, THREE_FLATS, TWO_FLATS, ONE_FLAT;
	
	public static Integer[] DUR_INTERVALS =   {2,2,1,2,2,2,1,2,2,1,2,2,2,1};
	public static Integer[] MOL_INTERVALS =   {2,1,2,2,1,3,1,2,1,2,2,1,3,1};
	public static Integer[] CHROM_INTERVALS = {1,1,1,1,1,1,1,1,1,1,1,1,1,1};
	public static Integer[] DUR_HARMONIC_INTERVALS = {2,2,1,2,1,3,1,2,2,1,2,1,3,1};
	public static Integer[] MOL_MELODIC_INTERVALS = {2,1,2,2,2,2,1,2,1,2,2,2,2,1};
	
	public class KeyPair {
		int firstMidiNum, secondMidiNum; // mod 12 cislo tonu
		ScaleAnalysis.ScaleType firstScaleType, secondScaleType;
		
		private KeyPair(int firstMidiNum, ScaleAnalysis.ScaleType firstScaleType, int secondMidiNum, ScaleAnalysis.ScaleType secondScaleType) {
			this.firstMidiNum = firstMidiNum;
			this.secondMidiNum = secondMidiNum;
			this.firstScaleType = firstScaleType;
			this.secondScaleType = secondScaleType;
		}
		
		boolean isParalel() {
			return firstScaleType.equals(ScaleAnalysis.ScaleType.DUR) && secondScaleType.equals(ScaleAnalysis.ScaleType.MOL);  
		}
		
		boolean isSameNamed() {
			boolean isSameNamed = firstScaleType.equals(ScaleAnalysis.ScaleType.DUR) && secondScaleType.equals(ScaleAnalysis.ScaleType.DUR);
			isSameNamed |= firstScaleType.equals(ScaleAnalysis.ScaleType.MOL) && secondScaleType.equals(ScaleAnalysis.ScaleType.MOL);
			
			return isSameNamed;
		}
		
		@Override
		public String toString() {
			return null;
		}
	}
	
	public KeyPair getKeyPair() {
		switch (this) {
			case CLEAR: return new KeyPair(0, ScaleAnalysis.ScaleType.DUR, 9, ScaleAnalysis.ScaleType.MOL);
			case ONE_SHARP: return new KeyPair(7, ScaleAnalysis.ScaleType.DUR, 4, ScaleAnalysis.ScaleType.MOL);
			case TWO_SHARP: return new KeyPair(2, ScaleAnalysis.ScaleType.DUR, 11, ScaleAnalysis.ScaleType.MOL);
			case THREE_SHARPS: return new KeyPair(9, ScaleAnalysis.ScaleType.DUR, 6, ScaleAnalysis.ScaleType.MOL);
			case FOUR_SHARPS: return new KeyPair(4, ScaleAnalysis.ScaleType.DUR, 1, ScaleAnalysis.ScaleType.MOL);
			case FIVE_SHARPS: return new KeyPair(11, ScaleAnalysis.ScaleType.DUR, 8, ScaleAnalysis.ScaleType.MOL);
			case SIX_SHARPS: return new KeyPair(6, ScaleAnalysis.ScaleType.DUR, 3, ScaleAnalysis.ScaleType.MOL); // "6 Sharps or 6 Flats - FIS(GES)+/DIS(ES)-";
			case SEVEN_SHARPS: return new KeyPair(1, ScaleAnalysis.ScaleType.DUR, 10, ScaleAnalysis.ScaleType.MOL); //"7 Sharps or 5 Flats - DES+/BES-";
			case FOUR_FLATS: return new KeyPair(8, ScaleAnalysis.ScaleType.DUR, 5, ScaleAnalysis.ScaleType.MOL); //"4 Flats - AS+/F-";
			case THREE_FLATS: return new KeyPair(3, ScaleAnalysis.ScaleType.DUR, 0, ScaleAnalysis.ScaleType.MOL); //"3 Flats - ES+/C-";
			case TWO_FLATS: return new KeyPair(10, ScaleAnalysis.ScaleType.DUR, 7, ScaleAnalysis.ScaleType.MOL); //"2 Flats - BES+/G-";
			case ONE_FLAT: return new KeyPair(5, ScaleAnalysis.ScaleType.DUR, 2, ScaleAnalysis.ScaleType.MOL); //"1 Flat - F+/D-";
			default:
				return null;
		}
	}
	
	public String toString() {
		switch (this) {
		case CLEAR: return "No Flats or sharps - C+/A-";
		case ONE_SHARP: return "1 Sharp - G+/E-";
		case TWO_SHARP: return "2 Sharps - D+/B-";
		case THREE_SHARPS: return "3 Sharps - A+/FIS-";
		case FOUR_SHARPS: return "4 Sharps - E+/CIS-";
		case FIVE_SHARPS: return "5 Sharps or 7 Flats - B+/GIS-";
		case SIX_SHARPS: return "6 Sharps or 6 Flats - FIS(GES)+/DIS(ES)-";
		case SEVEN_SHARPS: return "7 Sharps or 5 Flats - DES+/BES-";
		case FOUR_FLATS: return "4 Flats - AS+/F-";
		case THREE_FLATS: return "3 Flats - ES+/C-";
		case TWO_FLATS: return "2 Flats - BES+/G-";
		case ONE_FLAT: return "1 Flat - F+/D-";
		default:
			return "UNDEFINED";
		}
	}
	
	
	public static String info() {
		String out = "";
		for (int i = 0; i < KeySignature.values().length; i++) {
			KeySignature sig = KeySignature.values()[i];
			out += i + ": " + sig + "\n";
		}
		return out;
	}
	
	public static void main(String[] args) {
		System.out.println(KeySignature.info());
	}

}
