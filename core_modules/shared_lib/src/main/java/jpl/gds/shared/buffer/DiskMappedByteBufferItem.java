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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.thread.SleepUtilities;

/**
 * An individual ByteBuffer item that can push or pull its data to/from a
 * mappedByteBuffer on the disk. While it can function alone, it is designed to
 * function as part of a collection.
 * 
 * 
 */
class DiskMappedByteBufferItem {
	
	private Tracer logger;
	
	private ByteBuffer buffer;
	
	private boolean active;
	
	private boolean writeDone;
	private boolean readInProgress;

	private int length;
	private boolean isLast;
	private int readIndex;
	private int writeIndex;
	
	private String fileName;
	private long fileOffset;
	
	private IOException error;
	private int errorIndex;
	
	private static final int     TRY_OPERATION  = 10;
	private static final long    ONE_SECOND     = 1000L;
	
	/**
     * Default constructor. Because packets can be up to 64k, a size has been
     * selected to hold at least two packets per byte buffer item
     * 
     * @param logger
     *            The Tracer context logger
     */
	public DiskMappedByteBufferItem(Tracer logger){
		this(128000, logger);
	}
	
	/**
     * Constructs a MappedByteBufferItem with a buffer that can hold the
     * specified number of bytes.
     * 
     * @param mappedByteBufferSize
     *            Number of bytes that the item will be able to hold.
     * @param logger
     *            The Tracer context logger
     */
	public DiskMappedByteBufferItem(int mappedByteBufferSize, Tracer logger){
		buffer = ByteBuffer.allocate(mappedByteBufferSize);
		active = false;
		writeDone = false;
		readInProgress = false;
		fileName = null;
		fileOffset = -1;
		length = buffer.capacity();
		buffer.limit(buffer.capacity());
		isLast = false;
		readIndex = 0;
		writeIndex = 0;
		errorIndex = -1;
		this.logger = logger;

	}
	
	/**
	 * Constructor that copies the specified DiskMappedByteBufferItem.
	 * A deep copy is performed to duplicate the old item. A duplicate
	 * buffer is created with the data from the old item and, if present,
	 * a new string of the file name is copied over. 
	 *  
	 * @param old - item to be copied
	 */
	DiskMappedByteBufferItem(DiskMappedByteBufferItem old){
		if(old.buffer != null){
			buffer = old.buffer.duplicate();
		}
		active = old.active;
		writeDone = old.writeDone;
		readInProgress = old.readInProgress;
		fileName = old.fileName;
		fileOffset = old.fileOffset;
		length = old.length;
		isLast = old.isLast;
		readIndex = old.readIndex;
		writeIndex = old.writeIndex;
		if(old.error != null){
			error = old.error;
		}
		errorIndex = old.errorIndex;
	}
	
	/**
	 * Use the ByteBuffer bulk operation to put multiple bytes into the
	 * ByteBuffer. Returns a read only copy of the buffer. The ByteBuffer
	 * put(byte[]) function returns a copy of the buffer, therefore this one
	 * does as well. However, to prevent tampering of the buffer outside of the
	 * diskMappedByteBuffer object, a read only copy is returned.
	 * 
	 * A BufferOverflowException may be returned if there is insufficient space
	 * in the buffer to store the data store data in the ByteBuffer
	 * 
	 * @param src
	 *            Array of bytes to be stored
	 * @return A read-only copy of the updated ByteBuffer in this
	 *         DiskMappedByteBufferItem
	 * @throws BufferOverflowException
	 *             If there is insufficient space in this buffer
	 */
	public synchronized ByteBuffer put(byte[] src){
		return put(src, 0, src.length);
	}
	
	/**
	 * Use the ByteBuffer relative bulk operation to put zero or more bytes into
	 * the ByteBuffer. Starting from the given offset, the number of bytes
	 * specified by the length parameter are placed into the buffer at the
	 * current location of the writeIndex. If there are more bytes to be copied
	 * from the array than remain in this buffer, that is, if length >
	 * remaining(), then no bytes are transferred and a BufferOverflowException
	 * is thrown.
	 * 
	 * @param src
	 *            Array containing bytes to be stored
	 * @param offset
	 *            Starting index of the bytes to be stored
	 * @param length
	 *            Number of bytes to be stored
	 * @return A read-only copy of the updated ByteBuffer in this
	 *         DiskMappedByteBufferItem
	 * @throws BufferOverflowException
	 *             If there is insufficient space in this buffer
	 *
	 * @throws IndexOutOfBoundsException
	 *             If the preconditions on the <tt>offset</tt> and
	 *             <tt>length</tt> parameters do not hold
	 */
	public synchronized ByteBuffer put(byte[] src, int offset, int length){
		this.buffer.position(writeIndex);
		this.buffer.put(src, offset, length);
		this.writeIndex = buffer.position();
		
		// add notify, signal to read thread to resume reading
		this.notifyAll();
		return this.buffer.asReadOnlyBuffer();
	}
	
	/**
	 * Utilizing the ByteBuffer get function with the matching arguments,
	 * starting at the offset provided, the number of bytes indicated by length
	 * are placed into the source buffer. If length is greater than remaining,
	 * then BufferUnderflowException is thrown.
	 * 
	 * @param src
	 *            buffer to house the data to be returned
	 * @param offset
	 *            offset to start placing data into the buffer
	 * @param len
	 *            number of bytes to be read
	 * @return The number of bytes returned in the buffer
	 * 
	 * @throws IOException
	 *             The stored error is thrown when its marked location is
	 *             reached.
	 */
	public synchronized int get(byte[] src, int offset, int len) throws IOException{
		int writeIndexCopy = writeIndex;
		//throw the error if appropriate.
		if(readIndex >= errorIndex && error != null && errorIndex >=0){

			//EOF exceptions can keep throwing, others just once since processing could continue
			if(error instanceof EOFException){
				len = -1;
			}
			else{
				logger.debug("DMBB Item - throwing error!");
				throw error;
			}
		}
		
		else{ 
			//get how far we're reading
			//if writing is done, can't run into the write index
			if(writeDone){
				len = Math.min(len, (buffer.limit() - readIndex) );
				if(buffer.limit() < writeIndexCopy){
					logger.warn("Buffer limit is " + buffer.limit() + ", but write index is " + writeIndexCopy + ". Data may have been lost.");
				}
			}
			else{ //writing is not done
				len = Math.min(len, (writeIndexCopy - readIndex) );
			}
			//if there's an error need to stop short
			if(errorIndex > 0 && error != null){
				len = Math.min(len, errorIndex - readIndex);
			}
			//now that we finally have a specified size get the bytes

			if(buffer == null){
				logger.debug("!!!!!!!!!!Super JIT load!!!!!!!!!!");
				pullFromFile();
			}
			buffer.position(readIndex);

			try{
				buffer.get(src, offset, len);
			}
			catch(IndexOutOfBoundsException e){
				e.getStackTrace();
			}
			
			readIndex = buffer.position();
		}
		
		return len;
	}

	
	/**
	 * Get data, fills the buffer.
	 * 
	 * @param src
	 *            Buffer to house the data being returned.
	 * @return The number of bytes being returned. -1 if end of file was
	 *         reached.
	 * 
	 * @throws IOException
	 *             The stored error is thrown when its marked location is
	 *             reached.
	 */
	public synchronized int get(byte[] src) throws IOException{
		return this.get(src, 0, src.length);
	}
	
	/**
	 * get method that returns a single byte as an integer value (0-255). -1
	 * will be returned if end of file was reached
	 * 
	 * @return Integer value of the byte being returned, or -1 if end of file
	 *         was reached
	 * @throws IOException
	 *             The stored error is thrown when its marked location is
	 *             reached.
	 */
	public synchronized int get() throws IOException{
		byte[] src = {(byte)0};
		int ret = this.get(src,0,1);
		
		if(ret < 0){
			return ret;
		}
		return src[0] & 0xFF;
	}
	
	/**
	 * Delete the byteBuffer contained by the diskMappedByteBufferItem.
	 * 
	 * This allows the system to deallocate the memory contained by this buffer,
	 * but keep all other metadata. Primarily to be used while the data is
	 * backed to a file
	 */
	public synchronized void emptyBuffer(){
		buffer = null;
	}
	
	/**
	 * When a diskMappedByteBufferItem transfers its buffer to a
	 * diskMappedByteBuffer the data in the buffer is deleted to conserve
	 * memory. Prior to conducting read operations the buffer must be pulled
	 * from the file. All other relevant metadata is still in memory.
	 * 
	 * @throws IOException
	 *             thrown when the file was unable to be accessed
	 */
	public synchronized void pullFromFile() throws IOException{
		RandomAccessFile memoryMappedFile = null;
 
		/*
		 * A buffer should be empty to pull from a file. If it is not, then it must be assumed that the current data is
		 * either duplicate data of what is in the file or new data that would be lost in this operation
		 */
		if(this.buffer == null && this.fileName != null){
			for(int i = 0; i < TRY_OPERATION; ++i){
				if (i > 0){
					// Sleep a short random time to break synchronization with other
	                // processes
					
					randomSleep(ONE_SECOND);
				}
				
				try{
					memoryMappedFile = new RandomAccessFile(fileName, "r");
					
					//force the data to be pulled from the file into the in-memory ByteBuffer and set this buffer
					buffer = memoryMappedFile.getChannel().map(FileChannel.MapMode.READ_ONLY, fileOffset, length).load();
					//the limit and position aren't stored when the buffer is pushed to disk.
					buffer.limit(length);
					//buffer.position(0);
						
					memoryMappedFile.close();
					
					//no need to go through the loop again
					break;
				}
				//if not found, try again until either the max number of attempts are reached
				catch(FileNotFoundException e){
					if(i == (TRY_OPERATION-1)){
						this.error = new EOFException("DMBB Item - failed to load from file @ offset - " + fileName + " @ " + String.valueOf(fileOffset));
						this.errorIndex = 0;
						throw e;
					}
				}
			}
		}
		else if(this.buffer == null){
			logger.error("DiskMappedByteBufferItem pullFromFile was called, but data cannot be populated.");
			this.error = new EOFException("DMBB Item - data cannot be loaded with stored information. file @ offset - " + String.valueOf(fileName) + " @ " + String.valueOf(fileOffset));
			this.errorIndex = 0;
			throw new FileNotFoundException("DMBB Item - data cannot be loaded with stored information. file @ offset - " + String.valueOf(fileName) + " @ " + String.valueOf(fileOffset));
		}
		
	}
	
	/**
	 * A diskMappedByteBuffer keeps its data in a memory-only allocated
	 * ByteBuffer until this function is called. The given file name includes
	 * the relative or absolute file path to where the file will be created. If
	 * the file does not exist, then it is created with a size of offset+length.
	 * If the file does exist, but the offset+length is larger than the current
	 * information, the file is extended. The data is written to the file and
	 * closed.
	 * 
	 * @param memoryMappedFile
	 *            Random Access File handle where the data will be backed.
	 * @param fileName
	 *            Name of the file where this buffer is being stored.
	 * @param offset
	 *            Starting offset within the file to be written.
	 * 
	 * @throws IOException
	 *            The file was unable to be opened.
	 */
	public synchronized void pushToFile(RandomAccessFile memoryMappedFile, String fileName, long offset) throws IOException{
		
		MappedByteBuffer tempBuffer;
		
		//create the mapped byte buffer, which has to be mapped to a file
		try{
			
			//make sure this info is saved
			this.fileName = fileName;
			this.fileOffset = offset;
			
			//save aside this value
			length = writeIndex;
			
			//set the position to the write location, in prep for the next step
			buffer.limit(length);
			buffer.position(0);
			
			tempBuffer = memoryMappedFile.getChannel().map(FileChannel.MapMode.READ_WRITE, offset, length);
		
			//writes to the memory mapped buffer
			tempBuffer.put(buffer);	

			//ensure the data has been written to disk
			tempBuffer.force();
			
			//since the data is backed on the disk, we are sure writing is done on this item 
			writeDone = true;
		}
		catch(IOException e){
			logger.error("DMBB Item - IO error when writing to file ", e);
			//reset file info, shouldn't assume it did make it to disk
			fileName = null;
			fileOffset = -1;
		}
		
	}
	
	/**
	 * Report if this item is flagged as being used. A diskMappedByteBuffer
	 * should only be flagged as active if it is currently being used for
	 * reading or writing.
	 * 
	 * @return boolean if the current item is active
	 */
	public boolean isActive(){
		return this.active;
	}
	
	/**
	 * Sets the flag that states if this item is being used or not.
	 * 
	 * @param newVal
	 *            boolean indicating the active status of the
	 *            diskMappedByteBufferItem
	 */
	public void setActive(boolean newVal){
		this.active = newVal;
	}

	/**
	 * Represents the total number of bytes that can be held by the buffer,
	 * regardless of the current "limit", or marked end of written data.
	 * 
	 * @return Total capacity of the buffer in bytes
	 */
	public int getCapacity(){
		return this.buffer.capacity();
	}
	
	// changed to synchronized, added notify
	/**
	 * Store an error in this item at the current write position.
	 * 
	 * @param thrown
	 *            Error to be stored.
	 */
	public synchronized void setError(IOException thrown){
		this.error = thrown;
		this.errorIndex = writeIndex;
		this.notifyAll();
	}
	
	/**
	 * Remove the stored error and reset the error index.
	 */
	public void clearError(){
		this.error = null;
		this.errorIndex = -1;
	}
	
	/**
	 * Returns null if the buffer has not been backed by a file. Returns a
	 * String if the buffer has been backed by a file.
	 * 
	 * @return returns the name of the file that is held by the
	 *         diskMappedByteBuffer
	 */
	public String getFileName(){
		return this.fileName;
	}
	
	/**
	 * Get the file offset value stored.
	 * 
	 * @return fileOffset value stored in this item.
	 */
    public long getFileOffset(){
    	return fileOffset;
    }
	
	/**
	 * Indicates if the current item "is the last" item on a file.
	 * 
	 * @return boolean value indicating if this diskMappedByteBuffer is the last
	 *         on an allocated file by default will be false.
	 */
	public boolean getIsLast(){
		return this.isLast;
	}
	
	/**
	 * Tell the diskMappedByteBuffer if it is the last item on a file or not.
	 * 
	 * @param newVal
	 *            boolean new value for the isLast variable
	 */
	public void setIsLast(boolean newVal){
		this.isLast = newVal;
	}
	
	/**
	 * Get the limit value set in the buffer.
	 * 
	 * The limit value may be less than or equal to the capacity of the buffer.
	 * By default the limit value is equal to the capacity. When writing is
	 * complete, the limit is set to the writeIndex. This does not change the
	 * capacity of the buffer, but prevents the readIndex from moving past this
	 * value.
	 * 
	 * @return The value of the limit on the buffer.
	 */
	public int getLimit(){
		return this.buffer.limit();
	}

	/**
	 * Get buffer load status. A buffer may not be present on the
	 * diskMappedByteBuffer. Returns true if a buffer is present, false if it is
	 * null
	 * 
	 * @return boolean showing if the buffer is present or not
	 */
	public boolean isLoaded(){
		return(buffer != null);

	}
	
	/**
	 * Get readInProgress boolean.
	 * 
	 * @return Boolean showing the current value of the readInProgress flag
	 */
	public boolean isReadInProgress(){
		return this.readInProgress;
	}
	
	/**
	 * set readInProgress boolean.
	 * 
	 * @param newVal
	 *            Boolean value that the readInProgess should be set to.
	 */
	public void setReadInProgress(boolean newVal){
		this.readInProgress = newVal;
		
		if(newVal == true){
			setActive(true);
		}
		else if(newVal == false && isWriteDone()){
			setActive(false);
		}
	}
	
	/**
	 * ByteBuffers have only one "position" value. In order to allow reading and
	 * writing to occur on the same diskMappedByteBuffer at different locations
	 * readIndex is used to track the ByteBuffer's position during read
	 * operations
	 * 
	 * @return Integer value of the read index.
	 */
	public int getReadIndex(){
		return readIndex;
	}
	
	/**
	 * The remaining() function from ByteBuffer returns the number of bytes
	 * between the current position and the limit. Because the position may not
	 * be set to the current read position this value is calculated. While a
	 * singular diskMappedByteBuffer item should NOT be utilized by more than
	 * one thread at once, the remaining read value is calculated instead of
	 * moving the position and returning the value to prevent invalid values
	 * from being returned.
	 * 
	 * @return Number of bytes between the readIndex and the limit.
	 */
	public int remainingRead(){
		return (this.buffer.limit() - readIndex);
	}
	
	
	/**
	 * The remaining() function from ByteBuffer returns the number of bytes
	 * between the current position and the limit. Because the position may not
	 * be set to the current write position this value is calculated. While a
	 * singular diskMappedByteBuffer item should NOT be utilized by more than
	 * one thread at once, the remaining write value is calculated instead of
	 * moving the position and returning the value to prevent invalid values
	 * from being returned.
	 * 
	 * @return - Number of bytes between the writeIndex and the limit.
	 */
	public int remainingWrite(){
		return (this.buffer.limit() - writeIndex);
	}
	
	/**
	 * Get write done status.
	 * 
	 * @return Boolean value showing if writing is done to the current item or
	 *         not.
	 */
	public boolean isWriteDone(){
		return this.writeDone;
	}
	
	/**
	 * Set write done status
	 * 
	 * @param newVal
	 *            The new value of the writeDone flag.
	 */
	public synchronized void setWriteDone(boolean newVal){
		if(newVal == true){
			buffer.limit(writeIndex);
			
			if(!isReadInProgress()){
				setActive(false);
			}
		}
		else{ //newVal == false
			buffer.limit(buffer.capacity());
		}
		this.writeDone = newVal;
		this.notifyAll();
	}
	
	/**
	 * ByteBuffers have only one "position" value. In order to allow reading and
	 * writing to occur on the same diskMappedByteBuffer at different locations
	 * writeIndex is used to track the ByteBuffer's position during write
	 * operations
	 * 
	 * @return Integer value of the current write index
	 */
	public int getWriteIndex(){
		return writeIndex;
	}
	
	/**
	 * Tries to sleep for a randomized interval, less than or equal to the
	 * specified time, but at least one millisecond.
	 *
	 * @param sleep
	 *            Sleep interval
	 *
	 * @return True if interrupted
	 */
    private boolean randomSleep(final long sleep)
    {
        return SleepUtilities.checkedSleep(
                Math.max((long) (Math.random() * sleep), 1L));
    }
    
	/**
	 * Returns a boolean value indicating if the next read operation will retrun
	 * an error instead
	 * 
	 * @return TRUE if an error will be returned on the next read, FALSE if not.
	 */
    public boolean willReadError(){
    	if(error != null && readIndex == errorIndex){
    		return true;
    	}
    	else{
    		return false;
    	}
    }
}