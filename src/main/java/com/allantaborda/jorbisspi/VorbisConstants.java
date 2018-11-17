package com.allantaborda.jorbisspi;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;

public interface VorbisConstants{
	public static final Encoding VORBISENC = new Encoding("VORBISENC");
	public static final Type VORBIS = new Type("VORBIS", "ogg");
	public static final int BUFFER_SIZE = 2048;
}