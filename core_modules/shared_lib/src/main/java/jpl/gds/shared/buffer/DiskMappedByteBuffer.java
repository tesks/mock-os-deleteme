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
package jpl.gds.shared.buffer;


import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferOverflowException;

import jpl.gds.shared.log.Tracer;

/**
 * DiskMappedByteBuffer utilizes an array of diskMappedByteBufferItems to create
 * a diskMappedByteBuffer. The buffer itself is handled within the
 * DiskMappedByteBufferManager while read and write operations are conducted by
 * DiskMappedByteBuffer. Data is placed into the buffer through write functions
 * and removed from it through read functions.
 * 
 * 
 *
 *
 */
public class DiskMappedByteBuffer extends InputStream{
	
	protected Tracer logger;
	
	private DiskMappedByteBufferManager maint;
	
	//stats tracking
	private long startTime;
	private long writeDoneTime;
	private long readDoneTime;
	private long totalBytesInCount;
	private long totalBytesOutCount;
	

	/**
     * Construct the DiskMappedByteBuffer with the given configuration
     * information
     *
	 * Most of the code has moved to the DiskMappedByteBufferManager constructor.
	 *
     * @param configDMBB
     *            <code>DiskMappedByteBufferConfig</code> object containing all
     *            necessary configuration information to create a
     *            DiskMappedByteBuffer
     * 

     * @param logger
     *            The context Tracer
     */
	public DiskMappedByteBuffer(DiskMappedByteBufferConfig configDMBB, Tracer logger){
		this.logger = logger;
		// moved all setup configuration to DiskMappedByteBufferMaintenace since all of the variables moved there
		maint = new DiskMappedByteBufferManager(configDMBB, logger);
		
		//make sure maintenance variables are clear
		totalBytesInCount = 0;
		totalBytesOutCount = 0;
		startTime = System.currentTimeMillis();

	}
	
	/**
	 * Writes a single byte of data to the buffer. If the buffer is full, this
	 * function will hold until space is available or the buffer is placed into
	 * shutdown
	 * 
	 * @see jpl.gds.shared.buffer.DiskMappedByteBuffer#write(byte[] data, int
	 *      offset, int len)
	 * 
	 * @param data
	 *            Byte value to be written to the buffer
	 * @return When byte is written to the buffer, 1 is returned. If buffer is
	 *         in shutdown, -1 is returned.
	 */
	public int write(byte data){
		byte[] temp = {data};
		return write(temp, 0, 1);
	}
	
	/**
	 * Functions as write(data, 0, data.length)
	 * 
	 * Writes the entirety of the given byte array into the buffer. If the
	 * buffer does not have at least data.length number of contiguous bytes
	 * available, this function will hold until the space is available or the
	 * buffer is placed into shutdown.
	 * 
	 * @see jpl.gds.shared.buffer.DiskMappedByteBuffer#write(byte[] data, int
	 *      offset, int len)
	 * 
	 * @param data
	 *            Array of bytes to be placed into the buffer
	 * @return Number of bytes written to the buffer or -1 if no data was
	 *         written and buffer is in shutdown.
	 * 
	 * @throws BufferOverflowException
	 *             If even after verification there is insufficient space in a
	 *             DiskMappedByteBufferItem
	 *
	 * @throws IndexOutOfBoundsException
	 *             If the post-verification preconditions on the length of the
	 *             <tt>data</tt> array does not hold
	 */
	public int write(byte[]data){
		return write(data, 0, data.length);
	}
	
	/**
	 * Functions as <code>write(data, 0, len)</code>
	 * 
	 * Writes the specified number of bytes into the buffer, starting at the
	 * beginning of the byte array. If an invalid number (negative) of bytes to
	 * be written is given, then no data is written to the buffer. If the buffer
	 * does not have at least len number of contiguous bytes available, this
	 * function will hold until the space is available or the buffer is placed
	 * into shutdown.
	 * 
	 * @see jpl.gds.shared.buffer.DiskMappedByteBuffer#write(byte[] data, int
	 *      offset, int len)
	 * 
	 * @param data
	 *            Array containing bytes to be placed into the buffer
	 * @param len
	 *            Number of bytes, nonnegative, to be written to the buffer
	 * @return Number of bytes written to the buffer, 0 if an invalid number of
	 *         bytes is given, or -1 if no data was written and buffer is in
	 *         shutdown
	 *         
	 * @throws BufferOverflowException
	 *             If even after verification there is insufficient space in a
	 *             DiskMappedByteBufferItem
	 *
	 * @throws IndexOutOfBoundsException
	 *             If the post-verification preconditions on the <tt>len</tt>
	 *             parameter does not hold
	 */
	public int write(byte[] data, int len){
		return write(data, 0, len);
	}
	
	/**
	 * The number of bytes specified are written to the buffer from the given
	 * byte array. data[offset] is the first byte and every byte up to, but not
	 * including data[offset+len], is written to the buffer. If an offset or
	 * length is given that would attempt to place an element with an index not
	 * existing within the array into the buffer (eg: less than 0 or at least
	 * data.length), the function returns immediately and no data is placed into
	 * the buffer. If all given values are valid, then the specified number of
	 * bytes of data are written to the buffer. If the buffer does not have at
	 * least len number of contiguous bytes available, this function will hold
	 * until the space is available or the buffer is placed into shutdown.
	 * 
	 * @param data
	 *            Array containing bytes to be placed into the buffer
	 * @param offset
	 *            Starting index in the data array to
	 * @param len
	 *            Number of bytes from the data array to be written
	 * 
	 * @return Number of bytes written to the buffer, 0 if an invalid number of
	 *         bytes and/or offset is given, or -1 if no data was written and
	 *         buffer is in shutdown
	 *
	 * @throws BufferOverflowException
	 *             If even after verification there is insufficient space in a
	 *             DiskMappedByteBufferItem
	 *
	 * @throws IndexOutOfBoundsException
	 *             If the post-verification preconditions on the <tt>offset</tt>
	 *             and <tt>len</tt> parameters do not hold
	 * 
	 *  Function largely rewritten due to move of
	 *          the buffer to DiskMappedByteBufferManager and its no longer
	 *          transparent access. Requests for a DiskMappedByteBufferItem to
	 *          be written to, and related information, are handled by the
	 *          maintenance and data is written to this individual item, blind
	 *          of its place in the buffer. If more data needs to be written,
	 *          the process is repeated.
	 */
	public int write(byte[] data, int offset, int len){
		
		if (offset < 0 || len < 0 || ((data.length) - (offset + len) < 0)){
			logger.warn("DMBB - Invalid byte array given to write. arrLen " + data.length + ", offset " + offset + ", length " + len);
			return 0;
		}
		
		
		DiskMappedByteBufferItem currWriteItem = null;
		
		//keep track of where the remaining data to be put.
		int currOffset = offset;
		int currLen = len;
		int written = 0;
		int originalWriteOffset = 0;
		
		while(written < len){

			if(maint.isProduceDone()){
				if(written == 0){
					return -1;
				}
				return written;
			}

			//  moved lock and start of try block to prevent any errors with current
			// write item being erased between request and actual use of it.
			
			//lock write
			maint.producerLock();
			try{

				currWriteItem = maint.getCurrentWriteItem();
				currLen = Math.min(len, currWriteItem.getCapacity());

				//request next buffer if the current isn't sufficient
				if((currWriteItem.getCapacity() - currWriteItem.getWriteIndex()) < currLen){

					currWriteItem = maint.getNextWriteItem();
					
					// equals(null) throws NullPointerException, changed to "== null" to get a proper check.
					//if data cannot be written or produce side is shut down, return -1 (EOF flag)
					if((currWriteItem == null) || maint.isProduceDone()){
						return -1;
					}

				}
				else{
					currWriteItem = maint.getCurrentWriteItem();
				}


				originalWriteOffset = currWriteItem.getWriteIndex();

				//write it and update all tracking
				currWriteItem.put(data, currOffset, currLen);
				//track how much was written this time
				written += (currWriteItem.getWriteIndex() - originalWriteOffset);
				
				//adjust tracking info
				currOffset = offset + written;
				currLen = Math.min(len - written, currWriteItem.getCapacity());
				
			}
			finally{
				maint.producerUnlock();
			}
		}
		
		totalBytesInCount += written;
		return written;
	}
	
	/**
	 * Reads a single byte and returns the value as an integer.
	 * 
	 * @return 0-255 for valid byte value read, -1 if buffer is at EOF.
	 * 
	 * @throws IOException
	 *             Thrown when the stored exception (generated by the incoming
	 *             data stream) is reached.
	 */
	@Override
    public int read() throws IOException{
		byte[] readData = new byte[1];
		int byteRead = read(readData, 0, 1);
		if(byteRead == -1){
			return -1;
		}
		return (readData[0] & 0xFF);
	}
	
	/**
	 * The given byte array is filled with data from the buffer. If no error is
	 * enqueued and the array cannot be filled, the function holds until it can
	 * be filled or an error is received. If an error is found to be located at
	 * a point within the given array, then only data up to the error is
	 * returned and a second attempt to read will cause the error to be thrown.
	 * 
	 * @param data
	 *            byte array to be filled with data
	 * @return number of bytes read. -1 is returned if no bytes read because end
	 *         of file has been reached
	 * 
	 * @throws IOException
	 *             Thrown when the stored exception (generated by the incoming
	 *             data stream) is reached.
	 */
	@Override
    public int read(byte[] data) throws IOException{
		return read(data, 0, data.length);
	}
	
	/**
	 * Starting at the beginning of the array, the specified number of bytes are
	 * placed into the given byte array. If no error is enqueued and the
	 * requested number of bytes are not available, the function holds until the
	 * request can be fulfilled or an error is received. If an error is found to
	 * be located at a point within the requested data, then only data up to the
	 * error is returned and a second attempt to read will cause the error to be
	 * thrown.
	 * 
	 * @param data
	 *            byte array to hold read data
	 * @param len
	 *            number of bytes to be read
	 * @return number of bytes read. -1 is returned if no bytes read because end
	 *         of file had been reached
	 * 
	 * @throws IOException
	 *             Thrown when the stored exception (generated by the incoming
	 *             data stream) is reached.
	 */
	public int read(byte[] data, int len) throws IOException{
		return read(data, 0, len);
	}
	
	/**
	 * Starting at the given offset, the specified number of bytes are placed
	 * into the given byte array. If no error is enqueued and the requested
	 * number of bytes are not available, the function holds until the request
	 * can be fulfilled or an error is received. If an error is found to be
	 * located at a point within the requested data, then only data up to the
	 * error is returned and a second attempt to read will cause the error to be
	 * thrown.
	 * 
	 * @param data
	 *            byte array to hold read data
	 * @param offset
	 *            starting offset to put data
	 * @param len
	 *            number of bytes to be read
	 * @return number of bytes read. -1 is returned if no bytes read because end
	 *         of file had been reached
	 * 
	 * @throws IOException
	 *             Thrown when the stored exception (generated by the incoming
	 *             data stream) is reached or a DiskMappedByteBufferItem
	 *             retrieved for reading is null.
	 * 
	 * Function largely rewritten due to move of
	 *          the buffer to DiskMappedByteBufferManager and its no longer
	 *          transparent access. Requests for a DiskMappedByteBufferItem to
	 *          be read from, and related information, are handled by the
	 *          maintenance and data is read from this individual item, blind of
	 *          its place in the buffer.
	 */
	@Override
    public int read(byte[]data, int offset, int len) throws IOException{

		//make sure the number of requested bytes can fit
		if((offset | len | ((data.length) - (offset + len))) < 0){
			return 0;
		}
		
		//if buffer has been fully shutdown, reading is done
		if(maint.isCompleteShutdown()){
			return -1;
		}
	
		int numBytesRead = 0;
		int bytesThisRead = 0;
		int bytesLeft = len;
		
		maint.consumerLock();
		
		try{
			
			// moved get current read item into locked section.
			DiskMappedByteBufferItem currReadItem = maint.getCurrentReadItem();
			if(currReadItem == null){
				throw new IOException("DiskMappedByteBuffer: Start of read, currentReadItem is null");
			}
			
			//  As per the InputStreamContract, if there is no data to be read, then read must hold until there is data to be returned
			//				            , the EOF signal is sent, or an error is thrown.
			while(currReadItem.getReadIndex() >= currReadItem.getWriteIndex() && !currReadItem.isWriteDone() && !currReadItem.willReadError()){
				maint.consumerUnlock();

				maint.doWait();

				maint.consumerLock();

				currReadItem = maint.getCurrentReadItem();
				
				if(currReadItem == null){
					throw new IOException("DiskMappedByteBuffer: After waiting currentReadItem is null");
				}
			}


			//continue until all data is read
			while(bytesLeft > 0){	
				
				//request window to be loaded if the current item isn't loaded
				while(!currReadItem.isLoaded()){
					//release lock while waiting. maintenance timer with lockdown maintenance may need to occur before consumer window load request happens
					maint.consumerUnlock();
					//have maintenance load the consumer window NOW and wait for it
					maint.loadConsumerWindow();
					maint.consumerLock();
					
					currReadItem = maint.getCurrentReadItem();
					
					if(currReadItem == null){
						throw new IOException("DiskMappedByteBuffer: After loadConsumerWindow currentReadItem is null");
					}
				}
				//if the current item is loaded, read
				if(currReadItem.isLoaded()){
					try{
						bytesThisRead = currReadItem.get(data, offset + numBytesRead, bytesLeft);
					}
					//EOF shouldn't be thrown, but just in case let's handle it.
					catch(EOFException eof){
						bytesLeft = 0;
						if(numBytesRead == 0){
							numBytesRead = -1;
							maint.setShutdown(true);
						}
					}
					//throw the error if no data has been read. If data is currently waiting to be returned cut it short and send it off
					catch(IOException e){
						if (numBytesRead == 0){
							currReadItem.clearError();
							throw e;
						}
						else{
							bytesLeft = 0;
							bytesThisRead = 0;
						}
					}
					
					//determine why we didn't read all
					if(bytesLeft > 0){

						//when requesting the next item maintenance determines if there is more to be read from the current item or not.
						currReadItem = maint.getNextReadItem();
						
						if(currReadItem == null){
							throw new IOException("DiskMappedByteBuffer: After getNextReadItem currentReadItem is null");
						}
						
						
						//caught up to producer, we're done for this read
						if(currReadItem.getReadIndex() >= currReadItem.getWriteIndex()){
							
							bytesLeft = 0;	
						}
						else{
							//(readIndex < writeIndex)
							//keep reading
						}
					}
					
					//if we read data, update the tracking variables
					if(bytesThisRead >= 0){
						bytesLeft -= bytesThisRead;
						numBytesRead += bytesThisRead;
					}
					//just got EOF, this read's done
					else if(bytesThisRead < 0){
						bytesLeft = 0;
						if(numBytesRead == 0){
							numBytesRead = -1;
							maint.setShutdown(true);
						}
					}
					//else bytesThisRead == 0, do nothing extra
						
				}
			}
		}
		finally{
			maint.consumerUnlock();
		}
		
		if(numBytesRead > 0){
			totalBytesOutCount += numBytesRead;
		}
		return numBytesRead;
	}
	
	/*
	 * removed moveToNextProduce, moveToNextConsume,
	 * doNormalMaintenance, doPreConsumeMaintenance, doLockdownMaintenance,
	 * checkAndPushToFile moveToNewFile, and doLoadConsumerWindowItems - all
	 * moved to DiskMappedByteBufferManager
	 */
	
	/**
	 * Almost identical to InputStream's skip Skips over and discards
	 * <code>n</code> bytes of data from this input stream. The
	 * <code>skip</code> method may, for a variety of reasons, end up skipping
	 * over some smaller number of bytes, possibly <code>0</code>. This may
	 * result from any of a number of conditions; reaching end of file before
	 * <code>n</code> bytes have been skipped is only one possibility. The
	 * actual number of bytes skipped is returned. If <code>n</code> is
	 * negative, no bytes are skipped.
	 *
	 * <p>
	 * The <code>skip</code> method of this class creates a byte array and then
	 * repeatedly reads into it until <code>n</code> bytes have been read or the
	 * end of the stream has been reached.
	 *
	 * @param n
	 *            the number of bytes to be skipped.
	 * @return the actual number of bytes skipped.
	 * @exception IOException
	 *                if the stream does not support seek, or if some other I/O
	 *                error occurs.
	 */
    @Override
    public long skip(long n) throws IOException {

        long remaining = n;
        int nr;

        if (n <= 0) {
            return 0;
        }

        byte[] skipBuffer = new byte[(int)n];
        
        //should read once. if it ends up reading twice it is most likely due to reaching EOF or a buffered error is triggered.
        while (remaining > 0) {
            nr = read(skipBuffer, 0, (int)remaining);
            if (nr < 0) {
                break;
            }
            remaining -= nr;
        }

        return n - remaining;
    }
    
	/**
	 * Reports if writing has been completed.
	 * 
	 * @return TRUE if no more data can be written, FALSE if more can be written
	 */
	public boolean getProduceDone(){
		return maint.isProduceDone();
	}
	
	/**
	 * Record an IO exception at the current write location. Due to the buffered
	 * nature of the data, errors in the stream must be buffered as well so they
	 * are encountered at the point in the stream as they were received.
	 * 
	 * @param err
	 *            the IO exception to be stored
	 */
	public void setError(IOException err){
		maint.setError(err);
	}
	
	/**
	 * get the current size of the buffer
	 * 
	 * @return Number of elements in the buffer's array
	 */
	public int getBufferSize() {
		return maint.getBufferSize();
	}
	

	/**
	 * Get the number of elements in the buffer that are in use.
	 * 
	 * @return Number of elements between, and including, the read and write
	 *         indexed items in the buffer
	 */
	public int getBufferItemCount(){
		return maint.getBufferItemCount();
	}
	
	/**
     * Get the size, in bytes, specified for the DiskMappedByteBufferItems
     * 
     * @return Number of bytes of data each DiskMappedByteBuffer can hold 
     */
	public int getBufferItemSize(){
		return maint.getBufferItemSize();
	}
	
	/**
	 * Get the maximum number of elements that were utilized at any point of
	 * this buffer's lifetime
	 * 
	 * @return The maximum value bufferItemCount has possessed during the
	 *         lifetime of this buffer
	 */
	public int getBufferItemHighWaterMark(){
		return maint.getBufferItemHighWaterMark();
	}
	
	/**
	 * Get if the buffer is deleting files when they are no longer backing data
	 * in the buffer
	 * 
	 * @return TRUE if files are deleted when no longer in use. FALSE if files
	 *         are kept when no longer in use
	 */
	public boolean isDeleteFiles(){
		return maint.isDeleteFiles();
	}
	
	/*
	 * returned isBackupAllData, added isUseFiles.
	 * both used for RawInputMessenger reporting
	 */
	/**
	 * Get if all data is being written to disk files
	 * 
	 * @return TRUE if all data is being written to files, FALSE otherwise
	 */
	public boolean isBackupAllData(){
		return maint.isBackupAllData();
	}
	
	/**
	 * Get if disk files are being used
	 * 
	 * @return TRUE if disk files are being used to back the buffer, FALSE if
	 *         not
	 */
	public boolean isUseFiles(){
		return maint.isUseFiles();
	}
	

	/**
     * Gets the number of files currently in use
     * 
     * @return Number of files currently being used to back the buffer
     */
	public int getFileCount(){
		return maint.getFileCount();
	}
	
	/**
	 * Get the maximum number of files that the buffer can can be utilized to back the buffer.
	 * 
	 * @return maximum number of files that can be used
	 */
	public int getFileLimit(){
		return maint.getFileLimit();
	}
	
	/**
	 * Get the maximum number of files that were utilized at any point of this
	 * buffer's lifetime
	 * 
	 * @return The maximum value fileCount has possessed during the lifetime of
	 *         this buffer
	 */
	public int getFileHighWaterMark(){
		return maint.getFileHighWaterMark();
	}
	
	/**
     * Name of the current file being used to back data
     * 
     * @return File name, including known path, to the file currently open 
     */
	public String getCurrFileName(){
		return maint.getCurrFileName();
	}
	
	/**
	 * Get the consumer window size. The consumer window is the total number of
	 * elements, starting from the item currently being read from, that have
	 * their buffer loaded in memory. This many items potentially may not be
	 * loaded if it extends past the producer item or the end of the buffer.
	 * 
	 * @return Number of elements in the DiskMappedByteBuffer that may be kept
	 *         in memory and not solely backed to files
	 */
	public int getConsumerWindowSize(){
		return maint.getConsumerWindowSize();
	}
	
	/**
	 * reports if a full shutdown is being conducted
	 * 
	 * @return TRUE if full shutdown is being completed (no read or write),
	 *         FALSE if not
	 */
	public boolean isShutdown(){
		return maint.isCompleteShutdown();
	}
	
	/**
	 * Shutdown the buffer. Will either conduct a "soft" shutdown where incoming
	 * data is stopped, but data in the buffer can be read (false) or a hard
	 * shtudown and all data in the buffer is lose (true).
	 * 
	 * @param shutdown
	 *            TRUE if a full shutdown is completed (no read or write to
	 *            buffer), FALSE if partial shutdown (no more write in, but read
	 *            out is allowed)
	 */
	public void setShutdown(boolean shutdown){
		//  Pass shutdown request through to maintenance.
		maint.setShutdown(shutdown);
	}
    
	// Added close functions to add further compatibility with Java's InputStream
	
	/**
	 * Similar to {@code close()}, however all resources may not be released
	 * immediately.
	 * 
	 * @param shutdownFull
	 *            TRUE if all resources are to be released immediately. FALSE to
	 *            release incoming data resources, but continue allowing data to
	 *            be read out.
	 * 
	 * @throws IOException
	 *             - If an I/O error occurs.
	 */
    public void close(boolean shutdownFull) throws IOException{
    	//shut down the buffer
    	setShutdown(shutdownFull);
    }
    
    /**
     * @see java.io.InputStream#close()
     */
    @Override
    public void close() throws IOException{
    	close(true);
    }
    

	/**
	 * Get the data rate at which data has been consumed by the buffer, either
	 * until now or when consuming was terminated
	 * 
	 * @return Bytes per second at which data has been placed into the buffer.
	 */
    public double getBpsIn(){
    	if(writeDoneTime == 0){
    		return bps(totalBytesInCount, System.currentTimeMillis());
    	}
    	return bps(totalBytesInCount, writeDoneTime);
    }
    
	/**
	 * Get the data rate at which data has been produced by the buffer.
	 * 
	 * @return Megabytes per second at which data has been given out by the
	 *         buffer.
	 */
    public double getBpsOut(){
    	if(readDoneTime == 0){
    		return bps(totalBytesOutCount,System.currentTimeMillis());
    	}
    	return bps(totalBytesOutCount, readDoneTime);
    }
    
	/**
	 * Calculates the data rate for the given size from when the buffer was
	 * created until the given time or now, if the given time value is invalid
	 * (less than the start time).
	 * 
	 * @param bytes
	 *            bytes received over a period of time
	 * @param timeCompare
	 *            Time when the specified number of bytes had been sent
	 * @return bps calculated
	 */
    private double bps(long bytes, long timeCompare){
    	if( (timeCompare - startTime) > 0){
    		return ((bytes * 1000.0) / (timeCompare - startTime));
    	}
    	return ((bytes * 1000.0) / (System.currentTimeMillis() - startTime));
    }
    
	/**
	 * Get the number of bytes written into the buffer.
	 * 
	 * @return Number of bytes written into the buffer
	 */
    public long getBytesIn(){
    	return totalBytesInCount;
    }
    
	/**
	 * Get the number of bytes read out of the buffer.
	 * 
	 * @return Number of bytes read out of the buffer.
	 */
    public long getBytesOut(){
    	return totalBytesOutCount;
    }
    
	/**
	 * Set the time when consuming data into the buffer was complete.
	 * 
	 * @param val
	 *            a valid millisecond time value
	 */
    public void setInputDoneTime(long val){
    	writeDoneTime = val;
    }
    
	/**
	 * Set the time when all data had been produced by the buffer.
	 * 
	 * @param val
	 *            a valid millisecond time value
	 */
    public void setOutputDoneTime(long val){
    	readDoneTime = val;
    }
    

	/**
	 * Send request to the manager for the buffer and disk files to be removed.
	 * 
	 * @throws IOException
	 *             if the clearBuffer request threw an exception, was
	 *             interrupted while waiting, or could not be scheduled for
	 *             execution
	 * @throws IllegalStateException
	 *             if the buffer is not in an operational state
	 */
    public void clearBuffer() throws IOException, IllegalStateException{
    	maint.clearBuffer();
    }
    
}
