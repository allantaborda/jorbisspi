/*
 *   VorbisFormatConversionProvider.
 * 
 *   JavaZOOM : vorbisspi@javazoom.net
 *				http://www.javazoom.net
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package com.allantaborda.jorbisspi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.spi.FormatConversionProvider;

/** ConversionProvider for VORBIS files. */
public class VorbisFormatConversionProvider extends FormatConversionProvider implements VorbisConstants{
	private static final AudioFormat.Encoding[] EMPTY_ENCODING_ARRAY = new AudioFormat.Encoding[0];
	private static final AudioFormat[] EMPTY_FORMAT_ARRAY = new AudioFormat[0];
	private static final boolean t = true, f = false;
	//One row for each source format
	private static final boolean[][] CONVERSIONS = {
		{f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,t,t,f,f,f,f,f,f,f,f,f,f}, // 0
		{f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,t,t,f,f,f,f,f,f,f,f}, // 1
		{f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,t,t,f,f,f,f,f,f}, // 2
		{f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,t,t,f,f,f,f}, // 3
		{f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,t,t,f,f}, // 4
		{f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,t,t}, // 5
		{f,f,f,f,f,f,f,f,f,f,f,f,t,t,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f}, // 18
		{f,f,f,f,f,f,f,f,f,f,f,f,f,f,t,t,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f}, // 19
		{f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,t,t,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f}, // 20
		{f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,t,t,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f}, // 21
		{f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,t,t,f,f,f,f,f,f,f,f,f,f,f,f,f,f}, // 22
		{f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,t,t,f,f,f,f,f,f,f,f,f,f,f,f}, // 23
		{t,t,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f}, // 36
		{f,f,t,t,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f}, // 37
		{f,f,f,f,t,t,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f}, // 38
		{f,f,f,f,f,f,t,t,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f}, // 39
		{f,f,f,f,f,f,f,f,t,t,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f}, // 40
		{f,f,f,f,f,f,f,f,f,f,t,t,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f,f}, // 41
	};
	private static final AudioFormat[] INPUT_FORMATS = {
		new AudioFormat(VORBISENC, 32000.0F, -1, 1, -1, -1, f), // 0
		new AudioFormat(VORBISENC, 32000.0F, -1, 2, -1, -1, f), // 1
		new AudioFormat(VORBISENC, 44100.0F, -1, 1, -1, -1, f), // 2
		new AudioFormat(VORBISENC, 44100.0F, -1, 2, -1, -1, f), // 3
		new AudioFormat(VORBISENC, 48000.0F, -1, 1, -1, -1, f), // 4
		new AudioFormat(VORBISENC, 48000.0F, -1, 2, -1, -1, f), // 5
		new AudioFormat(VORBISENC, 16000.0F, -1, 1, -1, -1, f), // 18
		new AudioFormat(VORBISENC, 16000.0F, -1, 2, -1, -1, f), // 19
		new AudioFormat(VORBISENC, 22050.0F, -1, 1, -1, -1, f), // 20
		new AudioFormat(VORBISENC, 22050.0F, -1, 2, -1, -1, f), // 21
		new AudioFormat(VORBISENC, 24000.0F, -1, 1, -1, -1, f), // 22
		new AudioFormat(VORBISENC, 24000.0F, -1, 2, -1, -1, f), // 23
		new AudioFormat(VORBISENC, 8000.0F, -1, 1, -1, -1, f), // 36
		new AudioFormat(VORBISENC, 8000.0F, -1, 2, -1, -1, f), // 37
		new AudioFormat(VORBISENC, 11025.0F, -1, 1, -1, -1, f), // 38
		new AudioFormat(VORBISENC, 11025.0F, -1, 2, -1, -1, f), // 39
		new AudioFormat(VORBISENC, 12000.0F, -1, 1, -1, -1, f), // 40
		new AudioFormat(VORBISENC, 12000.0F, -1, 2, -1, -1, f), // 41
	};
	private static final AudioFormat[] OUTPUT_FORMATS;
	static {
		float[] sampleRates = {8000f, 11025f, 12000f, 16000f, 22050f, 24000f, 32000f, 44100f, 48000f};
		OUTPUT_FORMATS = new AudioFormat[4 * sampleRates.length];
		int c = -1;
		for(float sr : sampleRates){
			OUTPUT_FORMATS[++c] = new AudioFormat(sr, 16, 1, t, f);
			OUTPUT_FORMATS[++c] = new AudioFormat(sr, 16, 1, t, t);
			OUTPUT_FORMATS[++c] = new AudioFormat(sr, 16, 2, t, f);
			OUTPUT_FORMATS[++c] = new AudioFormat(sr, 16, 2, t, t);
		}
	}
	private List<AudioFormat.Encoding> srcEncodings, trgEncodings;
	private Map<AudioFormat, List<AudioFormat.Encoding>> trgEncodingsFromSourceFormat;
	private Map<AudioFormat, Map<AudioFormat.Encoding, List<AudioFormat>>> trgFormatsFromSourceFormat;

	public VorbisFormatConversionProvider(){
		srcEncodings = new SetList<>();
		trgEncodings = new SetList<>();
		for(AudioFormat format : INPUT_FORMATS) srcEncodings.add(format.getEncoding());
		for(AudioFormat format : OUTPUT_FORMATS) trgEncodings.add(format.getEncoding());
		trgEncodingsFromSourceFormat = new HashMap<>();
		trgFormatsFromSourceFormat = new HashMap<>();
		for(int c = 0; c < INPUT_FORMATS.length; c++){
			List<AudioFormat.Encoding> supportedTrgEncodings = new SetList<>();
			trgEncodingsFromSourceFormat.put(INPUT_FORMATS[c], supportedTrgEncodings);
			Map<AudioFormat.Encoding, List<AudioFormat>> trgFormatsFromTrgEncodings = new HashMap<>();
			trgFormatsFromSourceFormat.put(INPUT_FORMATS[c], trgFormatsFromTrgEncodings);
			for(int d = 0;  d < OUTPUT_FORMATS.length; d++){
				if(CONVERSIONS[c][d]){
					AudioFormat.Encoding trgEncoding = OUTPUT_FORMATS[d].getEncoding();
					supportedTrgEncodings.add(trgEncoding);
					List<AudioFormat> supportedTrgFormats = trgFormatsFromTrgEncodings.get(trgEncoding);
					if(supportedTrgFormats == null){
						supportedTrgFormats = new SetList<>();
						trgFormatsFromTrgEncodings.put(trgEncoding, supportedTrgFormats);
					}
					supportedTrgFormats.add(OUTPUT_FORMATS[d]);
				}
			}
		}
	}

	public AudioFormat.Encoding[] getSourceEncodings(){
		return srcEncodings.toArray(new AudioFormat.Encoding[srcEncodings.size()]);
	}

	public AudioFormat.Encoding[] getTargetEncodings(){
		return trgEncodings.toArray(new AudioFormat.Encoding[trgEncodings.size()]);
	}

	public boolean isSourceEncodingSupported(AudioFormat.Encoding srcEncoding){
		return srcEncodings.contains(srcEncoding);
	}

	public boolean isTargetEncodingSupported(AudioFormat.Encoding trgEncoding){
		return trgEncodings.contains(trgEncoding);
	}

	public AudioFormat.Encoding[] getTargetEncodings(AudioFormat srcFormat){
		for(Map.Entry<AudioFormat, List<AudioFormat.Encoding>> entry : trgEncodingsFromSourceFormat.entrySet()){
			if(matches(entry.getKey(), srcFormat)){
				List<AudioFormat.Encoding> l = entry.getValue();
				return l.toArray(new AudioFormat.Encoding[l.size()]);
			}
		}
		return EMPTY_ENCODING_ARRAY;
	}

	public AudioFormat[] getTargetFormats(AudioFormat.Encoding trgEncoding, AudioFormat srcFormat){
		for(Map.Entry<AudioFormat, Map<AudioFormat.Encoding, List<AudioFormat>>> entry : trgFormatsFromSourceFormat.entrySet()){
			if(matches(entry.getKey(), srcFormat)){
				List<AudioFormat> l = entry.getValue().get(trgEncoding);
				return l == null ? EMPTY_FORMAT_ARRAY : l.toArray(new AudioFormat[l.size()]);
			}
			
		}
		return EMPTY_FORMAT_ARRAY;
	}

	public boolean isConversionSupported(AudioFormat trgFormat, AudioFormat sourceFormat){
		AudioFormat[] tf = getTargetFormats(trgFormat.getEncoding(), sourceFormat);
		for(int c = 0; c < tf.length; c++) if(tf[c] != null && matches(tf[c], trgFormat)) return t;
		return f;
	}

	public AudioInputStream getAudioInputStream(AudioFormat.Encoding trgEncoding, AudioInputStream ais){
		return getAudioInputStream(new AudioFormat(trgEncoding, -1, -1, -1, -1, -1, ais.getFormat().isBigEndian()), ais);
	}

	/** Returns converted AudioInputStream. */
	public AudioInputStream getAudioInputStream(AudioFormat tFormat, AudioInputStream ais){
		if(isConversionSupported(tFormat, ais.getFormat())) return new VorbisAudioInputStream(tFormat, ais);
		throw new IllegalArgumentException("Conversion not supported");
	}

	/**
	 * Utility method to check whether these values match, taking into account -1.
	 * @return t if any of the values is -1 or both values have the same value.
	 */
	private static boolean doMatch(int i1, int i2){
		return i1 == -1 || i2 == -1 || i1 == i2;
	}

	/**
	 * @see #doMatch(int,int)
	 */
	private static boolean doMatch(float f1, float f2){
		return f1 == -1 || f2 == -1 || Math.abs(f1 - f2) < 1.0e-9;
	}

	/**
	 * Tests whether 2 AudioFormats have matching formats. A field matches when it is -1 in at least one of the formats or the field is the same in both formats.<br>
	 * Exceptions:
	 * <ul>
	 * <li>Encoding must always be equal for a match.
	 * <li> For a match, endianness must be equal if SampleSizeInBits is not  -1 and greater than 8 bit in both formats.<br> In other words:
	 * If SampleSizeInBits is -1 in either format or both formats have a SampleSizeInBits < 8, endianness does not matter.
	 * </ul>
	 * This is a proposition to be used as AudioFormat.matches. It can therefore be considered as a temporary workaround.
	 */
	private boolean matches(AudioFormat fmt1, AudioFormat fmt2){
		return fmt1.getEncoding().equals(fmt2.getEncoding())
			&& (fmt2.getSampleSizeInBits() <= 8 
				|| fmt1.getSampleSizeInBits() == -1 
				|| fmt2.getSampleSizeInBits() == -1 
				|| fmt1.isBigEndian() == fmt2.isBigEndian())
			&& doMatch(fmt1.getChannels(), fmt2.getChannels())
			&& doMatch(fmt1.getSampleSizeInBits(), fmt2.getSampleSizeInBits())
			&& doMatch(fmt1.getFrameSize(), fmt2.getFrameSize())
			&& doMatch(fmt1.getSampleRate(), fmt2.getSampleRate())
			&& doMatch(fmt1.getFrameRate(), fmt2.getFrameRate());
	}

	private class SetList<E> extends ArrayList<E>{
		public boolean add(E element){
			if(contains(element)) return false;
			super.add(element);
			return true;
		}
	}
}