/*
 *   DecodedVorbisAudioInputStream
 *   
 *	JavaZOOM : vorbisspi@javazoom.net
 *			   http://www.javazoom.net
 *
 * ----------------------------------------------------------------------------
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
 * ----------------------------------------------------------------------------
 */

package com.allantaborda.jorbisspi;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/** This class implements the Vorbis decoding. */
public class VorbisAudioInputStream extends AudioInputStream implements VorbisConstants{
	private static final int playState_NeedHeaders = 0;
	private static final int playState_ReadData = 1;
	private static final int playState_WriteData = 2;
	private static final int playState_Done = 3;
	private static final int playState_BufferFull = 4;
	private static final int playState_Corrupt = -1;
	private CircularBuffer circularBuffer;
	private AudioInputStream oggBitStream;
	private SyncState oggSyncState;
	private StreamState oggStreamState;
	private Page oggPage;
	private Packet oggPacket;
	private Info vorbisInfo;
	private Comment vorbisComment;
	private DspState vorbisDspState;
	private Block vorbisBlock;
	private float[][][] pcmf;
	private int[] indexes;
	private int playState, convsize = BUFFER_SIZE * 2, bytes, index, i, bout;
	private byte[] abSingleByte, buffer, convbuffer = new byte[convsize];

	public VorbisAudioInputStream(AudioFormat outputFormat, AudioInputStream bitStream){
		/*
		 * The usage of a ByteArrayInputStream is a hack. (the infamous "JavaOne hack", because I did it on June 6th
		 * 2000 in San Francisco, only hours before a JavaOne session where I wanted to show mp3 playback with Java
		 * Sound.) It is necessary because in the FCS version of the Sun jdk1.3, the constructor of AudioInputStream
		 * throws an exception if its first argument is null. So we have to pass a dummy non-null value.
		 */
		super(new ByteArrayInputStream(new byte[0]), outputFormat, -1);
		circularBuffer = new CircularBuffer();
		oggBitStream = bitStream;
		oggSyncState = new SyncState();
		oggStreamState = new StreamState();
		oggPage = new Page();
		oggPacket = new Packet();
		vorbisInfo = new Info();
		vorbisComment = new Comment();
		vorbisDspState = new DspState();
		vorbisBlock = new Block(vorbisDspState);
		oggSyncState.init();
		playState = playState_NeedHeaders;
	}

	/** Main loop. */
	public void execute(){
		// This code was developed by the jCraft group, as JOrbisPlayer.java, slightly modified by jOggPlayer developer and adapted by
		// JavaZOOM to suit the JavaSound SPI. Then further modified by Tom Kimpton to correctly play ogg files that would hang the player.
		switch(playState){
		case playState_NeedHeaders:
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
				// error; stream version mismatch perhaps
				if(oggStreamState.pagein(oggPage) < 0) throw new IOException("Error reading first page of Ogg bitstream data.");
				// no page? must not be vorbis
				if(oggStreamState.packetout(oggPacket) != 1) throw new IOException("Error reading initial header packet.");
				// error case; not a vorbis header
				if(vorbisInfo.synthesis_headerin(vorbisComment, oggPacket) < 0) throw new IOException("This Ogg bitstream does not contain Vorbis audio data.");
				i = 0;
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
				convsize = BUFFER_SIZE / vorbisInfo.channels;
				vorbisDspState.synthesis_init(vorbisInfo);
				vorbisBlock.init(vorbisDspState);
				pcmf = new float[1][][];
				indexes = new int[vorbisInfo.channels];
			}catch(IOException ioe){
				playState = playState_Corrupt;
				break;
			}
			playState = playState_ReadData;
			break;
		case playState_ReadData:
			int result;
			index = oggSyncState.buffer(BUFFER_SIZE);
			buffer = oggSyncState.data;
			bytes = readFromStream(buffer, index, BUFFER_SIZE);
			if(bytes == -1){
				playState = playState_Done;
				break;
			}else{
				oggSyncState.wrote(bytes);
				if(bytes == 0){
					if((oggPage.eos() != 0) || (oggStreamState.e_o_s != 0) || (oggPacket.e_o_s != 0)) playState = playState_Done;
					break;
				}
			}
			result = oggSyncState.pageout(oggPage);
			if(result == 0 || result == -1){
				playState = playState_ReadData;
				break;
			}
			oggStreamState.pagein(oggPage);
			playState = playState_WriteData;
			break;
		case playState_WriteData:
			// Decoding!
			while(true){
				result = oggStreamState.packetout(oggPacket);
				if(result == 0){
					playState = playState_ReadData;
					break;
				}else if(result == -1){ 
					// missing or corrupt data at this page position no reason to complain; already complained above
					continue;
				}else{
					// we have a packet. Decode it
					if(vorbisBlock.synthesis(oggPacket) == 0) vorbisDspState.synthesis_blockin(vorbisBlock);
					else continue;
					outputSamples();
					if(playState == playState_BufferFull) return;
				}
			}
			if(oggPage.eos() != 0) playState = playState_Done;
			break;
		case playState_BufferFull:
			if(circularBuffer.availableWrite() < 2 * vorbisInfo.channels * bout) break;
			circularBuffer.write(convbuffer, 0, 2 * vorbisInfo.channels * bout);
			outputSamples();
			break;
		case playState_Corrupt:
			// drop through to playState_Done...
		case playState_Done:
			oggStreamState.clear();
			vorbisBlock.clear();
			vorbisDspState.clear();
			vorbisInfo.clear();
			oggSyncState.clear();
			try{
				if(oggBitStream != null) oggBitStream.close();
				circularBuffer.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	/**
	 * This routine was extracted so that when the output buffer fills up, we can break
	 * out of the loop, let the music channel drain, then continue from where we were.
	 */
	private void outputSamples(){
		int samples;
		while((samples = vorbisDspState.synthesis_pcmout(pcmf, indexes)) > 0){
			float[][] pcmf0 = pcmf[0];
			bout = samples < convsize ? samples : convsize;
			double fVal = 0.0;
			// convert doubles to 16 bit signed ints (host order) and interleave
			for(i = 0; i < vorbisInfo.channels; i++){
				int pointer = i * 2;
				int mono = indexes[i];
				for(int j = 0; j < bout; j++){
					fVal = pcmf0[i][mono + j] * 32767.;
					int val = (int) (fVal);
					if(val > 32767) val = 32767;
					if(val < -32768) val = -32768;
					if(val < 0) val = val | 0x8000;
					convbuffer[pointer] = (byte) (val);
					convbuffer[pointer + 1] = (byte) (val >>> 8);
					pointer += 2 * (vorbisInfo.channels);
				}
			}
			if(circularBuffer.availableWrite() < 2 * vorbisInfo.channels * bout){
				playState = playState_BufferFull;
				return;
			}
			circularBuffer.write(convbuffer, 0, 2 * vorbisInfo.channels * bout);
			vorbisDspState.synthesis_read(bout);
		}
		playState = playState_ReadData;
	}

	/** Reads from the oggBitStream_ a specified number of Bytes(bufferSize) worth starting at index and puts them in the specified buffer[]. */
	private int readFromStream(byte[] buffer, int index, int bufferSize){
		int bytes = 0;
		try{
			bytes = oggBitStream.read(buffer, index, bufferSize);
		}catch(Exception e){
			bytes = -1;
		}
		return bytes;
	}

	public int read() throws IOException{
		if(abSingleByte == null) abSingleByte = new byte[1];
		int	nReturn = read(abSingleByte);
		if(nReturn == -1) return -1;
		return abSingleByte[0] & 0xFF;//$$fb 2001-04-14 nobody really knows that...
	}

	public int read(byte[] abData, int nOffset, int nLength) throws IOException{
		//$$fb 2001-04-22: this returns at maximum circular buffer length. This is not very efficient...
		//$$fb 2001-04-25: we should check that we do not exceed getFrameLength()!
		return circularBuffer.read(abData, nOffset, nLength);
	}

	public long skip(long lSkip) throws IOException{
		for(long lSkipped = 0; lSkipped < lSkip; lSkipped++){
			int	nReturn = read();
			if(nReturn == -1) return lSkipped;
		}
		return lSkip;
	}

	public int available() throws IOException{
		return circularBuffer.availableRead();
	}

	/** Close the stream. */
	public void close() throws IOException{
		circularBuffer.close();
		oggBitStream.close();
	}

	public boolean markSupported(){
		return false;
	}

	public void mark(int markLimit){
	}

	public void reset() throws IOException{
		throw new IOException("mark not supported");
	}

	private class CircularBuffer{
		private byte[] mAbData;
		private int nReadPos, nWritePos;
		private boolean closed;

		CircularBuffer(){
			mAbData = new byte[327670];
		}

		void close(){
			closed = true;
		}

		int availableRead(){
			return nWritePos - nReadPos;
		}

		int availableWrite(){
			return mAbData.length - availableRead();
		}

		int read(byte[] abData, int nOffset, int nLength){
			if(closed){
				if(availableRead() > 0) nLength = Math.min(nLength, availableRead());
				else return -1;
			}
			synchronized(this){
				if(availableRead() < nLength) execute();
				nLength = Math.min(availableRead(), nLength);
				int nRemainingBytes = nLength;
				while(nRemainingBytes > 0){
					while(availableRead() == 0){
						try{
							wait();
						}catch(InterruptedException e){
							e.printStackTrace();
						}
					}
					int	nAvailable = Math.min(availableRead(), nRemainingBytes);
					while(nAvailable > 0){
						int	nToRead = Math.min(nAvailable, mAbData.length - (nReadPos % mAbData.length));
						System.arraycopy(mAbData, nReadPos % mAbData.length, abData, nOffset, nToRead);
						nReadPos += nToRead;
						nOffset += nToRead;
						nAvailable -= nToRead;
						nRemainingBytes -= nToRead;
					}
					notifyAll();
				}
				return nLength;
			}
		}

		int write(byte[] abData, int nOffset, int nLength){
			synchronized(this){
				int nRemainingBytes = nLength;
				while(nRemainingBytes > 0){
					while(availableWrite() == 0){
						try{
							wait();
						}catch(InterruptedException e){
							e.printStackTrace();
						}
					}
					int	nAvailable = Math.min(availableWrite(), nRemainingBytes);
					while(nAvailable > 0){
						int	nToWrite = Math.min(nAvailable, mAbData.length - (nWritePos % mAbData.length));
						System.arraycopy(abData, nOffset, mAbData, nWritePos % mAbData.length, nToWrite);
						nWritePos += nToWrite;
						nOffset += nToWrite;
						nAvailable -= nToWrite;
						nRemainingBytes -= nToWrite;
					}
					notifyAll();
				}
				return nLength;
			}
		}
	}
}