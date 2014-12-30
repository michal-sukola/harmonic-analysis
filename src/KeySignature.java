
public enum KeySignature {
	CLEAR, ONE_SHARP, TWO_SHARP, THREE_SHARPS, FOUR_SHARPS, FIVE_SHARPS, SIX_SHARPS, SEVEN_SHARPS,
	FOUR_FLATS, THREE_FLATS, TWO_FLATS, ONE_FLAT;
	
	public static Integer[] DUR_INTERVALS =   {2,2,1,2,2,2,1,2,2,1,2,2,2,1}; // najcastejsie
	public static Integer[] MOLL_INTERVALS =   {2,1,2,2,1,3,1,2,1,2,2,1,3,1};
	public static Integer[] CHROM_INTERVALS = {1,1,1,1,1,1,1,1,1,1,1,1,1,1};
	public static Integer[] DUR_HARMONIC_INTERVALS = {2,2,1,2,1,3,1,2,2,1,2,1,3,1}; 
	// durova melodicka neexistuje
	public static Integer[] MOL_HARMONIC_INTERVALS = {2,1,2,2,1,3,1,2,1,2,2,1,3};	// najcastejsia
	public static Integer[] MOL_MELODIC_INTERVALS = {2,1,2,2,2,2,1,2,1,2,2,2,2,1}; // 30 percent pouzivania
	
	public class KeyPair {
		int firstMidiNum, secondMidiNum; // mod 12 cislo tonu
		ScaleType firstScaleType, secondScaleType;
		
		private KeyPair(int firstMidiNum, ScaleType firstScaleType, int secondMidiNum, ScaleType secondScaleType) {
			this.firstMidiNum = firstMidiNum;
			this.secondMidiNum = secondMidiNum;
			this.firstScaleType = firstScaleType;
			this.secondScaleType = secondScaleType;
		}
		
		boolean isParalel() {
			return firstScaleType.equals(ScaleType.DUR) && secondScaleType.equals(ScaleType.MOLL);  
		}
		
		boolean isSameNamed() {
			boolean isSameNamed = firstScaleType.equals(ScaleType.DUR) && secondScaleType.equals(ScaleType.DUR);
			isSameNamed |= firstScaleType.equals(ScaleType.MOLL) && secondScaleType.equals(ScaleType.MOLL);
			
			return isSameNamed;
		}
		
		@Override
		public String toString() {
			return null;
		}
	}
	
	public KeyPair getKeyPair() {
		switch (this) {
			case CLEAR: return new KeyPair(0, ScaleType.DUR, 9, ScaleType.MOLL);
			case ONE_SHARP: return new KeyPair(7, ScaleType.DUR, 4, ScaleType.MOLL);
			case TWO_SHARP: return new KeyPair(2, ScaleType.DUR, 11, ScaleType.MOLL);
			case THREE_SHARPS: return new KeyPair(9, ScaleType.DUR, 6, ScaleType.MOLL);
			case FOUR_SHARPS: return new KeyPair(4, ScaleType.DUR, 1, ScaleType.MOLL);
			case FIVE_SHARPS: return new KeyPair(11, ScaleType.DUR, 8, ScaleType.MOLL);
			case SIX_SHARPS: return new KeyPair(6, ScaleType.DUR, 3, ScaleType.MOLL); // "6 Sharps or 6 Flats - FIS(GES)+/DIS(ES)-";
			case SEVEN_SHARPS: return new KeyPair(1, ScaleType.DUR, 10, ScaleType.MOLL); //"7 Sharps or 5 Flats - DES+/BES-";
			case FOUR_FLATS: return new KeyPair(8, ScaleType.DUR, 5, ScaleType.MOLL); //"4 Flats - AS+/F-";
			case THREE_FLATS: return new KeyPair(3, ScaleType.DUR, 0, ScaleType.MOLL); //"3 Flats - ES+/C-";
			case TWO_FLATS: return new KeyPair(10, ScaleType.DUR, 7, ScaleType.MOLL); //"2 Flats - BES+/G-";
			case ONE_FLAT: return new KeyPair(5, ScaleType.DUR, 2, ScaleType.MOLL); //"1 Flat - F+/D-";
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
