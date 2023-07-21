/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package jpl.gds.tm.service.impl.frame;

import java.util.ArrayList;
import java.util.List;

import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.tm.service.api.frame.IPresyncFrameMessage;

/**
 * FrameSyncBuffer presents a view of the raw data of a single array.
 * It internally consists of a list of IPresyncFrameMessage objects.
 * Utility functions can also find the previous ERT time stamp
 * and the current bitRate.
 * 
 */
class FrameSyncBuffer {
	// variables to optimize access to buff
	private int size = 0;
	private int numBuffs = 0;
	// a first time flag to set testid and firstErt
	private boolean firstTime = true;
	// the first ert value before the first buffer
	private IAccurateDateTime firstErt = new AccurateDateTime(0);
	// the actual buffer list
	private final List<IPresyncFrameMessage> buff = new ArrayList<IPresyncFrameMessage>();
	
	private static final boolean debug = false;
	
	/**
	 * Default constructor
	 */
	FrameSyncBuffer() {
	}
	/**
	 * Consume one IPresyncFrameMessage into the list.
	 * @param iPresyncFrameMessage the IPresyncFrameMessage to be consumed
	 */
	public void consume(IPresyncFrameMessage iPresyncFrameMessage) {
		if (firstTime) {
			firstErt = iPresyncFrameMessage.getErt(); // won't be quite correct first time, but....
			firstTime = false;
		}
		numBuffs = numBuffs + 1;
		size = size + iPresyncFrameMessage.getNumBytes();
		if (debug) {
			System.out.println("ByteBuffer, got RDM, size now " + size + " from rdm " +
					iPresyncFrameMessage.getNumBytes());
		}
		buff.add(iPresyncFrameMessage);
	}

	/**
	 * Gets the number of buffers in use
	 * @return the number of buffers
	 */
	public int getNumBuffers() {
		return numBuffs;
	}
	/**
	 * Gets the number of bytes in the full list.
	 * @return the number of bytes in the composite array
	 */
	public int getNumBytes() {
		return size;
	}
	/**
	 * Gets the length of a selected buffer id (0..numBuffs)
	 * @param buffId the buffer id number
	 * @return length in bytes of selected buffer
	 * @throws ArrayIndexOutOfBoundsException Catches array issues.
	 */
	public int getLen(int buffId) throws ArrayIndexOutOfBoundsException {
		if (buffId < 0 || buffId >= buff.size()) {
			throw new ArrayIndexOutOfBoundsException("buffId out of range " + buffId);
		}
		IPresyncFrameMessage rdm =  buff.get(buffId);
		return rdm.getNumBytes();
	}
	
	/**
	 * Gets the length of the first buffer
	 * @return length in bytes of first buffer
	 */
	public int lenFirstBuffer() {
		if (buff.isEmpty()) {
			return 0;
		}
		int len0 = 0;
		try {
			len0 = this.getLen(0);
		} catch (ArrayIndexOutOfBoundsException e) { 
		    e.printStackTrace();
		}
		return len0;
	}
	
	/**
	 * Gets a byte as an integer at a selected offset with in composite
	 * array.
	 * @param off offset into the array 0..length
	 * @return byte value at offset
	 * @throws ArrayIndexOutOfBoundsException Catches array issues
	 */
	public int getByte(int off) throws ArrayIndexOutOfBoundsException {
		if (off < 0 || off >= size) {
			throw new ArrayIndexOutOfBoundsException("offset out of range " + off + " of len " + size);
		}
		int posInBuff = 0;
		int lenSoFar = 0;
		IPresyncFrameMessage rdm;
		for (int bPos = 0; bPos < buff.size(); ++bPos) {
			rdm = buff.get(bPos);
			if (off < (lenSoFar + rdm.getNumBytes())) {
				posInBuff = off - lenSoFar;
				return rdm.get(posInBuff);
			}
			lenSoFar += rdm.getNumBytes();      		
		}
		return 0;
	}

	/**
	 * Gets a unsigned 32 integer as a long at a selected offset with in composite
	 * array.
	 * @param off offset into the array 0..(length-4)
	 * @return u32 value at offset
	 * @throws ArrayIndexOutOfBoundsException Catches array issues
	 */
	public long getU32(int off) throws ArrayIndexOutOfBoundsException {
		byte[] sbuff = this.getBuffer(off, 4);
		// will optimize this later with logic from getByte
		long val = ((sbuff[0] << 24) & 0xff000000L)
		| ((sbuff[1] << 16) & 0x00ff0000L)
		| ((sbuff[2] <<  8) & 0x0000ff00L)
		| ((sbuff[3]      ) & 0x000000ffL);
		return val;        
	}
	
	/**
	 * Get the previous ERT at the selected offset. ERT is time tagged
	 * at the end of a buffer, so we need the previous one
	 * @param off offset into the composite array 0..length
	 * @return long ERT
	 * @throws ArrayIndexOutOfBoundsException Catches array Issues
	 */
	public IAccurateDateTime getErt(int off) throws ArrayIndexOutOfBoundsException  {
		if (off < 0 || off >= size) {
			throw new ArrayIndexOutOfBoundsException("offset out of range " + off + " of len " + size);
		}
		int lenSoFar = 0;
		IAccurateDateTime prevErt = firstErt;
		IPresyncFrameMessage rdm;
		for (int bPos = 0; bPos < buff.size(); ++bPos) {
			rdm = buff.get(bPos);
			if (off < (lenSoFar + rdm.getNumBytes())) {
				return prevErt;
			}
			lenSoFar += rdm.getNumBytes();
			prevErt = rdm.getErt();
		}
		return firstErt;
	}
	/**
	 * Get the station information object at the offset into the composite array
	 * @param off offset into the composite array 0..length
	 * @return station information object
	 * @throws ArrayIndexOutOfBoundsException Catches array issues.
	 */
	public IStationTelemInfo getDSNInfo(int off) throws ArrayIndexOutOfBoundsException {
		if (off < 0 || off >= size) {
			throw new ArrayIndexOutOfBoundsException("offset out of range " + off + " of len " + size);
		}
		int lenSoFar = 0;
		IPresyncFrameMessage rdm;
		for (int bPos = 0; bPos < buff.size(); ++bPos) {
			rdm = buff.get(bPos);
			if (off < (lenSoFar + rdm.getNumBytes())) {
				return rdm.getStationInfo();
			}
			lenSoFar += rdm.getNumBytes(); 
		}
		return null;
	}
	
	/**
	 * Remove the first buffer in the composite array
	 */
	public void dropFirstBuffer() {
		if (buff.isEmpty()) {
			size = 0;
			numBuffs = 0;
			return;
		}
		IPresyncFrameMessage rdm =  buff.get(0);
		firstErt = rdm.getErt();
		size -= rdm.getNumBytes();
		if (debug) {
			System.out.println("ByteBuffer, dropFirstBuffer, size = " + size);
		}
		numBuffs -= 1;
		buff.remove(0);
	}
	
	/**
	 * Gets a byte array from the composite array. 
	 * @param off offset into the composite array 0..length
	 * @param blen number of bytes to copy
	 * @return extracted byte array
	 * @throws ArrayIndexOutOfBoundsException Catches array issues
	 */
	public byte[] getBuffer(int off, int blen) throws ArrayIndexOutOfBoundsException {
		if (off < 0 || (off+blen) > size) {
			throw new ArrayIndexOutOfBoundsException("offset out of range " + off + " plus blen=" + blen + " of len " + size);
		}
		if (debug) {
			System.out.println("getBuffer off " + off + " blen " + blen);
			this.probe();
		}
		byte[] b = new byte[blen];

		int startPosInBuff = 0;
		int endPosInBuff = 0;
		int lenSoFar = 0;
		int bStart = 0;
		IPresyncFrameMessage rdm;
		for (int bPos = 0; bPos < buff.size(); ++bPos) {
			rdm = buff.get(bPos);
			// Find the start buffer based upon offset. Skip buffers
			// prior to the requested offset.
			if (off >= lenSoFar + rdm.getNumBytes()) {
				lenSoFar += rdm.getNumBytes();              
				continue;
			} else if (bStart >= blen) {
				// We've copied all needed bytes
				break;
			} else {
				startPosInBuff = 0;
				if (lenSoFar < off) {
					startPosInBuff = off - lenSoFar;
				}
				// Decide how any bytes to copy.  Assume the remaining of the current
				// buffer, then check to see if this is more than the number of bytes
				// left in the return buffer, and adjust the number of bytes if needed
				int bytesLeft = blen - bStart;
				endPosInBuff = rdm.getNumBytes() - 1;
				int numToCopy = endPosInBuff - startPosInBuff + 1;
				if (numToCopy > bytesLeft) {
					endPosInBuff = startPosInBuff + bytesLeft - 1;
					numToCopy = bytesLeft;
				}
				if (debug) {
					System.out.println("getBuffer: startPosInBuff " + startPosInBuff + " bStart " + bStart +
							" endPosInBuff " + endPosInBuff + " numToCopy " + numToCopy + " " + " RDM size " + 
							rdm.getData().length + " b length " + b.length);
				}
				System.arraycopy(rdm.getData(), startPosInBuff, b, bStart, numToCopy);
				bStart += numToCopy;
				lenSoFar += rdm.getNumBytes();              
			}
		}
		return b;
	}
	
	@Override
	public String toString() {
		if (buff.isEmpty()) {
			return "ByteBuffer is empty";
		} else {
			return "ByteBuffer numBuffs=" + numBuffs + " numBytes=" + size;
		}
	}
	
	/**
	 * This prints out the state of the ByteBuffer to the console (whether it is empty or its content+size)
	 */
	public void probe() {
		if (buff.isEmpty()) {
			System.out.println("ByteBuffer is empty");
		} else {
			System.out.println("ByteBuffer numBuffs=" + numBuffs + " numBytes=" + size);
			for (int i = 0; i < buff.size(); ++i) {
				IPresyncFrameMessage rdm = buff.get(i);
				System.out.println("[" + i + "] len=" + rdm.getNumBytes() + " ERT=" + rdm.getErt());
			}
		}
	}
}
