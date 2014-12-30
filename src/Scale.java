import java.util.Arrays;


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
			case MOLL:   intervals = Arrays.copyOf(KeySignature.MOLL_INTERVALS, 7);	break;
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
		if(scaleType == ScaleType.DUR_MOLL) {
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