/*
 *   VorbisAudioFileReader.
 * 
 *   JavaZOOM : vorbisspi@javazoom.net
 *			  http://www.javazoom.net 
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

import static java.nio.charset.StandardCharsets.UTF_8;
import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import com.jcraft.jorbis.JOrbisException;
import com.jcraft.jorbis.VorbisFile;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

/** This class implements the AudioFileReader class and provides an Ogg Vorbis file reader for use with the Java Sound Service Provider Interface. */
public class VorbisAudioFileReader extends AudioFileReader implements VorbisConstants{
	private static final int MARK_LIMIT = 64001;
	private SyncState oggSyncState;
	private StreamState oggStreamState;
	private Page oggPage;
	private Packet oggPacket;
	private Info vorbisInfo;
	private Comment vorbisComment;
	private DspState vorbisDspState;
	private InputStream oggBitStream;
	private byte[] buffer;
	private int bytes, index;

	/** Return the AudioFileFormat from the given file. */
	public AudioFileFormat getAudioFileFormat(File file) throws UnsupportedAudioFileException, IOException{
		try(InputStream is = new BufferedInputStream(new FileInputStream(file))){
			is.mark(MARK_LIMIT);
			getAudioFileFormat(is);
			is.reset();
			return getAudioFileFormat(is, (int) file.length(), Math.round((new VorbisFile(file.getAbsolutePath()).time_total(-1)) * 1000));
		}catch(JOrbisException e){
			throw new IOException(e.getMessage());
		}
	}

	/** Return the AudioFileFormat from the given URL. */
	public AudioFileFormat getAudioFileFormat(URL url) throws UnsupportedAudioFileException, IOException{
		try(InputStream is = url.openStream()){
			return getAudioFileFormat(is);
		}
	}

	/** Return the AudioFileFormat from the given InputStream. */
	public AudioFileFormat getAudioFileFormat(InputStream is) throws UnsupportedAudioFileException, IOException{
		try{
			if(!is.markSupported()) is = new BufferedInputStream(is);
			is.mark(MARK_LIMIT);
			return getAudioFileFormat(is, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED);
		}finally{
			is.reset();
		}
	}

	/** Return the AudioFileFormat from the given InputStream, length in bytes and length in milliseconds. */
	private AudioFileFormat getAudioFileFormat(InputStream bitStream, int mediaLength, int totalms) throws UnsupportedAudioFileException, IOException{
		HashMap<String, Object> aff_properties = new HashMap<>(), af_properties = new HashMap<>();
		if(totalms <= 0) totalms = 0;
		else aff_properties.put("duration", new Long(totalms * 1000));
		oggBitStream = bitStream;
		oggSyncState = new SyncState();
		oggStreamState = new StreamState();
		oggPage = new Page();
		oggPacket = new Packet();
		vorbisInfo = new Info();
		vorbisComment = new Comment();
		vorbisDspState = new DspState();
		new Block(vorbisDspState);
		bytes = 0;
		oggSyncState.init();
		index = 0;
		try{
			index = oggSyncState.buffer(BUFFER_SIZE);
			buffer = oggSyncState.data;
			bytes = readFromStream(buffer, index, BUFFER_SIZE);
			if(bytes == -1) throw new IOException("Cannot get any data from selected Ogg bitstream.");
			oggSyncState.wrote(bytes);
			if(oggSyncState.pageout(oggPage) != 1){
				if(bytes < BUFFER_SIZE) throw new IOException("EOF");
				throw new IOException("Input does not appear to be an Ogg bitstream.");
			}	
			oggStreamState.init(oggPage.serialno());
			vorbisInfo.init();
			vorbisComment.init();
			aff_properties.put("ogg.serial", new Integer(oggPage.serialno()));
			// error; stream version mismatch perhaps
			if(oggStreamState.pagein(oggPage) < 0) throw new IOException("Error reading first page of Ogg bitstream data.");
			// no page? must not be vorbis
			if(oggStreamState.packetout(oggPacket) != 1) throw new IOException("Error reading initial header packet.");
			// error case; not a vorbis header
			if(vorbisInfo.synthesis_headerin(vorbisComment, oggPacket) < 0) throw new IOException("This Ogg bitstream does not contain Vorbis audio data.");
			int i = 0;
			while(i < 2){
				while(i < 2){
					int result = oggSyncState.pageout(oggPage);
					if(result == 0) break;
					if(result == 1){
						oggStreamState.pagein(oggPage);
						while(i < 2){
							result = oggStreamState.packetout(oggPacket);
							if(result == 0) break;
							if(result == -1) throw new IOException("Corrupt secondary header. Exiting.");
							vorbisInfo.synthesis_headerin(vorbisComment, oggPacket);
							i++;
						}
					}
				}
				index = oggSyncState.buffer(BUFFER_SIZE);
				buffer = oggSyncState.data;
				bytes = readFromStream(buffer, index, BUFFER_SIZE);
				if(bytes == -1) break;
				if(bytes == 0 && i < 2) throw new IOException("End of file before finding all Vorbis headers!");
				oggSyncState.wrote(bytes);
			}
			// Read Ogg Vorbis comments
			byte[][] ptr = vorbisComment.user_comments;
			String currComment = "";
			for(int j = 0; j < ptr.length; j++){
				if(ptr[j] == null) break;
				currComment = (new String(ptr[j], 0, ptr[j].length - 1, UTF_8)).trim();
				String lcCurrComment = currComment.toLowerCase();
				if(lcCurrComment.startsWith("artist")) aff_properties.put("author", currComment.substring(7));	  	
				else if(lcCurrComment.startsWith("title")) aff_properties.put("title", currComment.substring(6));	  	
				else if(lcCurrComment.startsWith("album")) aff_properties.put("album", currComment.substring(6));	  	
				else if(lcCurrComment.startsWith("date")) aff_properties.put("date", currComment.substring(5));
				else if(lcCurrComment.startsWith("copyright")) aff_properties.put("copyright", currComment.substring(10));	  	
				else if(lcCurrComment.startsWith("comment")) aff_properties.put("description", currComment.substring(8));
				else if(lcCurrComment.startsWith("genre")) aff_properties.put("ogg.comment.genre", currComment.substring(6));	
				else if(lcCurrComment.startsWith("tracknumber")) aff_properties.put("ogg.comment.track", currComment.substring(12));	
				else{
					String[] acomm = currComment.split("=");
					if(acomm.length == 2) aff_properties.put("ogg.comment.ext." + acomm[0].toLowerCase(), acomm[1]);
				}
				aff_properties.put("ogg.comment.encodedby", new String(vorbisComment.vendor, 0, vorbisComment.vendor.length - 1));
			}
		}catch(IOException ioe){
			throw new UnsupportedAudioFileException(ioe.getMessage());
		}
		String dmp = vorbisInfo.toString();
		int ind = dmp.lastIndexOf("bitrate:");
		int minbitrate = -1;
		int nominalbitrate = -1;
		int maxbitrate = -1;
		if(ind != -1){
			String[] tokens = dmp.substring(ind + 8, dmp.length()).split(",");
			try{
				minbitrate = Integer.parseInt(tokens[0]);
				nominalbitrate = Integer.parseInt(tokens[1]);
				maxbitrate = Integer.parseInt(tokens[2]);
			}catch(IndexOutOfBoundsException e){}
		}
		if(nominalbitrate > 0) af_properties.put("bitrate", nominalbitrate);
		af_properties.put("vbr", true);
		if(minbitrate > 0) aff_properties.put("ogg.bitrate.min.bps", minbitrate);
		if(maxbitrate > 0) aff_properties.put("ogg.bitrate.max.bps", maxbitrate);
		if(nominalbitrate > 0) aff_properties.put("ogg.bitrate.nominal.bps", nominalbitrate);
		if(vorbisInfo.channels > 0) aff_properties.put("ogg.channels", vorbisInfo.channels);
		if(vorbisInfo.rate > 0) aff_properties.put("ogg.frequency.hz", vorbisInfo.rate);
		if(mediaLength > 0) aff_properties.put("ogg.length.bytes", mediaLength);
		aff_properties.put("ogg.version", vorbisInfo.version);
		float frameRate = -1;
		if(nominalbitrate > 0) frameRate = nominalbitrate / 8;
		else if(minbitrate > 0) frameRate = minbitrate / 8;
		return new VorbisAudioFileFormat(new AudioFormat(VORBISENC, vorbisInfo.rate, AudioSystem.NOT_SPECIFIED, vorbisInfo.channels, 1, frameRate, false, af_properties), mediaLength, aff_properties);
	}

	/** Return the AudioInputStream from the given InputStream. */
	public AudioInputStream getAudioInputStream(InputStream is) throws UnsupportedAudioFileException, IOException{
		try{		
			if(!is.markSupported()) is = new BufferedInputStream(is);
			is.mark(MARK_LIMIT);
			AudioFileFormat aff = getAudioFileFormat(is, AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED);
			is.reset();
			return new AudioInputStream(is, aff.getFormat(), aff.getFrameLength());
		}catch(UnsupportedAudioFileException | IOException e){
			is.reset();
			throw e;
		}
	}

	/** Return the AudioInputStream from the given File. */
	public AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException{
		return prepareAudioInputStream(new FileInputStream(file));
	}

	/** Return the AudioInputStream from the given URL. */
	public AudioInputStream getAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException{
		return prepareAudioInputStream(url.openStream());
	}

	private AudioInputStream prepareAudioInputStream(InputStream is) throws UnsupportedAudioFileException, IOException{
		try{
			return getAudioInputStream(is);
		}catch(Exception e){
			if(is != null) is.close();
			throw e;
		}
	}

	/**
	 * Reads from the oggBitStream_ a specified number of Bytes(bufferSize) worth starting at index and puts them in the specified buffer[].
	 * @return the number of bytes read or -1 if error.
	 */
	private int readFromStream(byte[] buffer, int index, int bufferSize){
		try{
			return oggBitStream.read(buffer, index, bufferSize);
		}catch(Exception e){
			return -1;
		}
	}

	/** Custom {@code AudioFileFormat} for OGG Vorbis file format **/
	private class VorbisAudioFileFormat extends AudioFileFormat{
		private Map<String, Object> props;

		VorbisAudioFileFormat(AudioFormat audioFormat, int mediaLength, Map<String, Object> properties){
			super(VORBIS, mediaLength, audioFormat, AudioSystem.NOT_SPECIFIED);
			props = Collections.unmodifiableMap(properties);
		}

		public Map<String,Object> properties(){
			return props;
		}
	}
}