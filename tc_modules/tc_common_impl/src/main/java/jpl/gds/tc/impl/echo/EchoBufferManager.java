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
package jpl.gds.tc.impl.echo;

import java.util.ArrayList;
import java.util.Arrays;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
* EchoBufferManager presents a view of the multiple command echo 
* data buffers as a single array.
*
*
* 09/19/17 - MPCS-9106 - Updated dropBuffersToByte
*          to be able to drop a portion of the first buffer
*/
public class EchoBufferManager {
    private int size = 0;
    private int numBuffs = 0;
    private final ArrayList<byte[]> buff = new ArrayList<byte[]>();
    
    private final Tracer trace;

    /**
    * Default constructor
     * @param appContext the current application context
    */
    public EchoBufferManager(final ApplicationContext appContext){
        this.trace = TraceManager.getTracer(appContext, Loggers.CMD_ECHO);
    }
    
    /**
    * Consume one byte array into the list.
    * @param buffer byte array to be consumed
    */
    public void consume(final byte[] buffer) {
        numBuffs = numBuffs + 1;
        size = size + buffer.length;
        trace.debug("EchoBufferManager, got buffer, size now " + size + " from new buffer " + 
                     buffer.length);
        buff.add(buffer);
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
    *
    * @throws ArrayIndexOutOfBoundsException Array bounds error
    */
    public int getLen(final int buffId) throws ArrayIndexOutOfBoundsException {
        if (buffId < 0 || buffId >= buff.size()) {
            throw new ArrayIndexOutOfBoundsException("buffId out of range " + buffId);
        }
        return buff.get(buffId).length;
    }
    
    /**
    * Gets the length of the first buffer
    * @return length in bytes of first buffer
    */
    public int getFirstBufferLen() {
        if (buff.isEmpty()) {
            return 0;
        }
        int len0 = 0;
        try {
            len0 = this.getLen(0);
        } catch (final ArrayIndexOutOfBoundsException e) { 
        }
        return len0;
    }
    /**
    * Gets a byte as an integer at a selected offset with in composite
    * array.
    * @param off offset into the array 0..len
    * @return byte value at offset
    *
    * @throws ArrayIndexOutOfBoundsException Array bounds error
    */
    public int getByte(final int off) throws ArrayIndexOutOfBoundsException {
        if (off < 0 || off >= size) {
            throw new ArrayIndexOutOfBoundsException("offset out of range " + off + " of len " + size);
        }
        int posInBuff = 0;
        int lenSoFar = 0;
        byte[] rdm;
        
        for (int bPos = 0; bPos < buff.size(); ++bPos) {
            rdm = buff.get(bPos);
            if (off < (lenSoFar + rdm.length)) {
                posInBuff = off - lenSoFar;
                return rdm[posInBuff];
            }
            lenSoFar += rdm.length;      		
        }
        return 0;
    }

    /**
     * Removes buffers that only contain bytes with lower offset than the given offset.
     * @param bytePos the starting offset
     */
    public void dropBuffersToByte(final int bytePos) {
    	int bytesStillToDrop = bytePos;
        while (bytesStillToDrop > 0) {
        	final int len = getFirstBufferLen();
        	if (len <= bytesStillToDrop) {
        		bytesStillToDrop = bytesStillToDrop - len;
        		dropFirstBuffer();
        	} else {
        		bytesStillToDrop -= dropFirstBufferToByte(bytesStillToDrop);
        	}
        }
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
        final byte[] rdm =  buff.get(0);
        size -= rdm.length;
        trace.debug("ByteBuffer, dropFirstBuffer, size = " + size);
        numBuffs -= 1;
        buff.remove(0);
    }
    
    /**
     * Remove the first number of specified bytes from the first buffer in the
     * composite array.
     * 
     * @param bytePos
     *            The number of bytes to be removed
     * @return The actual number of bytes removed
     */
    public int dropFirstBufferToByte(final int bytePos){
        int bytesToRemove = bytePos;
        final byte[] old = buff.get(0);
        if(bytesToRemove <= 0){
            return 0;
        } else if ( bytesToRemove > old.length){
            bytesToRemove = old.length;
        }
        buff.remove(0);
        if(bytesToRemove != old.length){
            final byte[] newFirst = Arrays.copyOfRange(old, bytePos, old.length);
            buff.add(0, newFirst);
        }
        return bytesToRemove;
    }
    
    /**
    * Gets a byte array from the composite array. 
    * @param off offset into the composite array 0..len
    * @param blen number of bytes to copy
    * @return extracted byte array
    *
    * @throws ArrayIndexOutOfBoundsException Array bounds error
    */
    public byte[] getBuffer(final int off, final int blen) throws ArrayIndexOutOfBoundsException {
        if (off < 0 || (off+blen) > size) {
            throw new ArrayIndexOutOfBoundsException("offset out of range " + off + " plus blen=" + blen + " of len " + size);
        }
        trace.debug("getBuffer off " + off + " blen " + blen);
            this.probe();
            
        final byte[] b = new byte[blen];

        int startPosInBuff = 0;
        int endPosInBuff = 0;
        int lenSoFar = 0;
        int bStart = 0;
        byte[] rdm;
        for (int bPos = 0; bPos < buff.size(); ++bPos) {
            rdm = buff.get(bPos);
            // Find the start buffer based upon offset. Skip buffers
            // prior to the requested offset.
            if (off >= lenSoFar + rdm.length) {
                lenSoFar += rdm.length;              
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
                final int bytesLeft = blen - bStart;
                endPosInBuff = rdm.length - 1;
                int numToCopy = endPosInBuff - startPosInBuff + 1;
                if (numToCopy > bytesLeft) {
                   endPosInBuff = startPosInBuff + bytesLeft - 1;
                   numToCopy = bytesLeft;
                }
                trace.debug("getBuffer: startPosInBuff " + startPosInBuff + " bStart " + bStart +
                        " endPosInBuff " + endPosInBuff + " numToCopy " + numToCopy + " " + " RDM size " + 
                        rdm.length + " b length " + b.length);
                System.arraycopy(rdm, startPosInBuff, b, bStart, numToCopy);
                bStart += numToCopy;
                lenSoFar += rdm.length;              
            }
        }
        return b;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (buff.isEmpty()) {
            return "EchoBufferManager is empty";
        } else {
            return "EchoBufferManager numBuffs=" + numBuffs + " numBytes=" + size;
        }
    }


    /**
     * Probe.
     * Prints debug statements regarding the number of arrays in this EchoBufferManager and the size of each
     */    
    public void probe() {
        if (buff.isEmpty()) {
            trace.debug("EchoBufferManager is empty");
        } else {
            final StringBuilder str = new StringBuilder("EchoBufferManager numBuffs=" + numBuffs + " numBytes=" + size);
            for (int i = 0; i < buff.size(); ++i) {
                final byte [] rdm = buff.get(i);
                str.append("[" + i + "] len=" + rdm.length);
            }
            trace.debug(str.toString());
        }
    }
    

    /**
     * Search for a byte sequence.
     *
     * @param seqToFind  Candidate byte array
     * @param startIndex Starting index
     *
     * @return Index where found or -1
     */
    public int findByteSequence(final byte[] seqToFind, int startIndex) {
       if (seqToFind == null || seqToFind.length < 1) {
    	   throw new IllegalArgumentException("Sequence to find is null or empty");
       }
       int firstByteIndex = findNextByte(startIndex, seqToFind[0]);
       while (firstByteIndex != -1) {
    	   if (matchBytes(seqToFind, firstByteIndex)) {
    		   return firstByteIndex;
    	   } else {
    		   startIndex = firstByteIndex + 1;
    	   }
    	   firstByteIndex = findNextByte(startIndex, seqToFind[0]);
       }
       return firstByteIndex;
    }
    

    /**
     * Search for a byte.
     *
     * @param startIndex Starting index
     * @param byteValue  Byte to search for
     *
     * @return Index of found byte or -1
     */
    public int findNextByte(final int startIndex, final int byteValue) {
    	int index = startIndex;
    	while (index < size) {
    		final int thisByte = getByte(index);
    		if (thisByte == byteValue) {
    			return index;
    		}
    		index++;
    	}
    	return -1;  	
    }


    /**
     * Compare byte arrays.
     *
     * @param seqToMatch Byte array
     * @param startIndex Starting index
     *
     * @return True if they match
     */
    public boolean matchBytes(final byte[] seqToMatch, final int startIndex) {
    	if (startIndex + seqToMatch.length > size) {
    		return false;
    	}
    	for (int i = 0; i < seqToMatch.length; i++) {
    		if (getByte(startIndex + i) != seqToMatch[i]) {
    			return false;
    		}
    	}
    	return true;
    }
}
