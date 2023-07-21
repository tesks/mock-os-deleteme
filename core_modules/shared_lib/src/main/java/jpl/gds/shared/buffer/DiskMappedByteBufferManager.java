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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.thread.SleepUtilities;

/**
 * The core of the DiskMappedByteBuffer resides within this class. The array of
 * DiskMappedByteBufferItems and the maintenance functions are contained within
 * here. The maintenance functions handle properly configuring the buffer and
 * coordinate any passing of data between the buffer and the hard disk. The
 * DiskMappedByteBuffer can read or write data to the buffer only through
 * requesting the current item marked for reading or writing.
 * 
 *
 */
public class DiskMappedByteBufferManager{
	private Tracer logger;

	//array and tracking
	private DiskMappedByteBufferItem[] mappedByteBufferArray;
	private final int bufferItemSize;
	private final AtomicInteger bufferItemLimit;
	private int consumerWindowSize;

	//file tracking
	private final long fileSize;
	private int fileCount;
	private final int fileLimit;
	private boolean useFiles;
	private boolean deleteFiles;
	private boolean backupAllData;
	private long currFileOffset;
	private String currFileName = "";
	private final String bufferDir;
	private RandomAccessFile currFile;

	//maintenance
	private int maintenanceIndex;

	private final ScheduledExecutorService maintenanceExecutor;
	private static final String BASE_WORKER_NAME = "DMBB Executor Service";
	private final long MAINTENANCE_DELAY;


	//buffer growth control
	private static final int BUFFER_INCREASE_COUNT = 3;
	private static final double BUFFER_INCREASE_PERCENT = 30.0;
	private static final double BUFFER_USAGE_THRESHOLD_PERCENT = 90.0;
	private int bufferIncreaseCounter;


	//buffer copy of last error information
	//currently not used, but in place for future reporting/retrieval by the user in some manner
	private IOException lastErrorObject;
	private int lastErrorItem;
	private int lastErrorOffset;

	private final AtomicInteger readItemIndex;
	private final AtomicInteger writeItemIndex;

	//members for performance tracking

	private int bufferItemCount = 1;
	private int bufferItemHighWaterMark;
	private int fileHighWaterMark;

	//consumer and producer lock
	private final ReentrantLock consumerLock = new ReentrantLock();
	private final ReentrantLock producerLock = new ReentrantLock();


	//shutdown atomic values
	private final AtomicBoolean produceDone;
	private final AtomicBoolean shutdown;

	/**
	 * Runnable that is to be executed on a regular basis by the maintenance
	 * executor. Normal maintenance is conduced each time. If the producer is
	 * near the end of the buffer, additional maintenance is conducted.
	 */
	private final Runnable maintenanceRunnable = new Runnable(){

		@Override
		public void run() {
			try {
				doNormalMaintenance();

				if(writeItemIndex.get() >= (int)(mappedByteBufferArray.length * 0.9)){
					doLockdownMaintenance();
				}

			} catch (final IOException e) {
				logger.warn("DMBB - error in maintenance ", e);
			}
		}
	};

	/**
	 * Callable utilized by the reader function. The consumer window size is
	 * increased and buffer elements in the consumer window are loaded.
	 */
	private final Callable<Integer> consumerWindowLoadCallable = new Callable<Integer>(){

		@Override
		public Integer call() throws Exception {
			consumerWindowSize+=10;
			doLoadConsumerWindowItems();
			return 1;
		}

	};
	
	/**
	 * Callable utilized to instruct the maintenance thread to clear the buffer.
	 */
	private final Callable<Integer> clearBufferCallable = new Callable<Integer>(){

		@Override
		public Integer call() throws Exception {
			doClearBuffer();
			return 1;
		}

	};


	
	/**
	 * Constructor for DiskMappedByteBufferManager. The
	 * <code>DiskMappedByteBufferConfig</code> item is used and the buffer is
	 * constructed, maintenance thread started, and all management items are
	 * configured.
	 * 
	 * @param dmbbConfig
	 *            set up all items according to the specified configurations
	 */
	protected DiskMappedByteBufferManager(DiskMappedByteBufferConfig dmbbConfig, Tracer logger){

		//set DiskMappedByteBufferConfig variables
		this.logger = logger;
		bufferItemSize = dmbbConfig.getBufferItemSize();
		bufferItemLimit = new AtomicInteger(dmbbConfig.getBufferItemLimit());
		consumerWindowSize = dmbbConfig.getConsumerWindowSize();

		MAINTENANCE_DELAY = dmbbConfig.getMaintenanceDelay();

		fileSize = dmbbConfig.getFileSize();
		fileLimit = dmbbConfig.getFileLimit();
		backupAllData = dmbbConfig.getBackupAllData();

		if(backupAllData){
			deleteFiles = false;
			useFiles = true;
		}
		else{
			deleteFiles = dmbbConfig.getDeleteFiles();
			useFiles = dmbbConfig.getUseFiles();
		}


		//create array and prepare the first item for read/write operations
		mappedByteBufferArray = new DiskMappedByteBufferItem[bufferItemLimit.get()];
		mappedByteBufferArray[0] = new DiskMappedByteBufferItem(bufferItemSize, logger);
		bufferIncreaseCounter = 0;

		//set pointers to the first item
		readItemIndex = new AtomicInteger(0);
		writeItemIndex = new AtomicInteger(0);

		//prep info for where the first diskMappedByteBuffer to go to a file will go
		bufferDir = dmbbConfig.getBufferDir();

		//attempt to create the directory
		if(new File(bufferDir).mkdirs()){
			try {
				moveToNewFile();
			} catch (final IOException e) {
				logger.debug("DiskMappedByteBuffer - irrelevant IOException thrown");
			}
		}
		else{
			logger.warn("Unable to create directory, switching to memory-only mode");
			useFiles = false;
		}

		//start counting the number of files
		setFileCount(useFiles?1:0);


		maintenanceIndex = 0;

		lastErrorItem = -1;
		lastErrorOffset = -1;

		//set atomic variables
		shutdown = new AtomicBoolean(false);
		produceDone = new AtomicBoolean(false);

		//set up maintenance
		final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(BASE_WORKER_NAME).build();
		maintenanceExecutor = Executors.newScheduledThreadPool(1, threadFactory);
		maintenanceExecutor.scheduleAtFixedRate(maintenanceRunnable, MAINTENANCE_DELAY, MAINTENANCE_DELAY, TimeUnit.MILLISECONDS);

	}

	/**
	 * At the request of the write function, moves the production process to the
	 * next item in the buffer. The next element in the buffer is created, the
	 * current one is set to write complete, and the write index is incremented.
	 */
	private void moveToNextProduce(){
		mappedByteBufferArray[writeItemIndex.get() + 1]= new DiskMappedByteBufferItem(bufferItemSize, logger);
		mappedByteBufferArray[writeItemIndex.get()].setWriteDone(true);
		writeItemIndex.incrementAndGet();
	}

	/**
	 * At the request of the read function, moved the consumption process to the
	 * next item in the buffer. The item to be read from next is set as read in
	 * progress, the read index is moved to the next item, and the previous item
	 * is set as NO read in progress.
	 */
	private void moveToNextConsume() {

		try {
			mappedByteBufferArray[readItemIndex.get() + 1].setReadInProgress(true);
		} catch (final NullPointerException npe) {
			logger.warn("DiskMappedByteBuffer - npe @ read item " + readItemIndex.get(), npe);

		}

		mappedByteBufferArray[readItemIndex.getAndIncrement()].setReadInProgress(false);
	}

	/**
	 * Function executed by the maintenance executor every time it is called.
	 * Starting from where the last maintenance left off, or 0 if it has not
	 * been executed, up to the writeItemIndex.get()-1, various maintenance is
	 * conducted and can be considered in three groups:
	 * 
	 * <p>
	 * 1. Post consumer - (last maintenance index to readItemIndex.get()-1) <br>
	 * - Delete buffer items.
	 * 
	 * <p>
	 * 2. Pre consumer - (readItemIndex.get() to readItemIndex.get() +
	 * consumerWindowSize-1) <br>
	 * - Verify byte buffer is not null and is loaded from file, if files are
	 * used.
	 * 
	 * <p>
	 * 3. Post producer - (readItemIndex.get() + consumerWindowSize to
	 * writeItemIndex.get()) <br>
	 * - Backup to file and set byte buffer to null
	 * 
	 * <p>
	 * During all three stages, if all data is being kept, the buffer items are
	 * backed to the files if they have not been already. Due to potential
	 * overlap of 2 and 3, during 3 after the buffer is backed to file it is
	 * checked again for read in progress before removing the byte buffer.
	 * 
	 * <p>
	 * Because further maintenance will need to be conducted on items in 2 and
	 * 3, the index marked as the start of the next maintenance is the read item
	 * index at the time of transition from 1 to 2.
	 * 
	 * @throws IOException
	 *             When a write to file error occurs, all data is pulled back
	 *             from files immediately and the buffer is placed into
	 *             "read only" mode. If a file is unable to be read, IOexception
	 *             is thrown
	 */
	private void doNormalMaintenance() throws IOException{
		int currMaintIndex = maintenanceIndex;
		//maintenance up to the current consume item - backup all if not done, otherwise do cleanup
		for(;(currMaintIndex< readItemIndex.get())/* && !shutdown.get()*/;currMaintIndex++){
			if(backupAllData){
				checkAndPushToFile(currMaintIndex);
			}
			//delte files as they are no longer being used
			if(mappedByteBufferArray[currMaintIndex].getIsLast() && deleteFiles){

				final String tempFileName = mappedByteBufferArray[currMaintIndex].getFileName();

				if(tempFileName != null){
					final File tempFile = new File(tempFileName);
					tempFile.delete();
					setFileCount(fileCount - 1);
					logger.debug("deleted " + tempFileName);
				}
			}
			//only track files backing the buffer in this situation
			else if(mappedByteBufferArray[currMaintIndex].getIsLast() && !deleteFiles && !backupAllData){
				setFileCount(fileCount -1);
			}

			mappedByteBufferArray[currMaintIndex] = null;
		}

		//start maintenance at this location next time
		maintenanceIndex = currMaintIndex;

		//maintenance from the current read item through the consumer window
		//these need to be loaded, but if all data needs to be backed up and it's not been done, then do that as well
		for(;(currMaintIndex < (readItemIndex.get() + consumerWindowSize)) && (currMaintIndex < writeItemIndex.get())/* && !shutdown.get()*/;currMaintIndex++){
			if(backupAllData){
				checkAndPushToFile(currMaintIndex);
			}

			final boolean bufferLoaded = mappedByteBufferArray[currMaintIndex].isLoaded();
			final String loadFileName = mappedByteBufferArray[currMaintIndex].getFileName();
			final long loadFileOffset = mappedByteBufferArray[currMaintIndex].getFileOffset();

			if(!bufferLoaded){
				if(loadFileName != null && loadFileOffset >= 0){
					try{
						mappedByteBufferArray[currMaintIndex].pullFromFile();
						logger.debug("pulled index " + currMaintIndex + " from file");
					}
					catch(final FileNotFoundException f){
						f.printStackTrace();
						fileNotFoundShutdown(currMaintIndex);
					}
				}
				else{//loadFileName == null || loadFileOffset < 0
					logger.warn("DMBB - Item in consumer window cannot be populated from file, data stream will terminate early");
					logger.info("Due to early shutdown current backing files will not be deleted");
					//allow user to attempt to salvage data
					deleteFiles = false;
					setShutdown(false);
				}
			}
			//else item is loaded and therefore no action is necessary

		}

		//finally, maintenance between the read window and the producer
		//backup the data, like above, but handle buffer data as dictated by the options.
		for(;(currMaintIndex < writeItemIndex.get())/* && !shutdown.get()*/;currMaintIndex++){
			if(useFiles){
				final boolean pushed = checkAndPushToFile(currMaintIndex);
				//because pushing to file for each can take a while and the consumer window can creep up on is, make sure it's not needed before dropping it
				if(pushed && (currMaintIndex >= (readItemIndex.get() + consumerWindowSize) )){
					mappedByteBufferArray[currMaintIndex].emptyBuffer();
				}
			}
		}

		//for monitoring purposes
		setBufferItemCount();
	}

	private void doPreConsumeMaintenance() throws IOException{
		int currMaintIndex = maintenanceIndex;
		//maintenance up to the current consume item - backup all if not done, otherwise do cleanup
		for(;(currMaintIndex< readItemIndex.get())/* && !shutdown.get()*/;currMaintIndex++){
			if(backupAllData){
				checkAndPushToFile(currMaintIndex);
			}
			//delte files as they are no longer being used
			if(mappedByteBufferArray[currMaintIndex].getIsLast() && deleteFiles){

				final String tempFileName = mappedByteBufferArray[currMaintIndex].getFileName();

				if(tempFileName != null){
					final File tempFile = new File(tempFileName);
					tempFile.delete();
					setFileCount(fileCount - 1);
					logger.debug("deleted " + tempFileName);
				}
			}
			//only track files backing the buffer in this situation
			else if(!deleteFiles && !backupAllData){
				setFileCount(fileCount -1);
			}

			mappedByteBufferArray[currMaintIndex] = null;
		}

		//start maintenance at this location next time
		maintenanceIndex = currMaintIndex;
	}

	/**
	 * Maintenance function performed when the producer is near the end of the
	 * array. Both the producer and consumer locks are locked in order to
	 * prevent either thread from performing operations on the buffer. All
	 * buffer items are shifted down as far as possible. If all data is being
	 * backed up, the index to be shifted to zero is the maintenance index,
	 * otherwise the read item index is shifted to 0 and maintenance is set to
	 * 0. All items from the minimum index to the read index are moved down from
	 * their original index to this new value (eg:
	 * readItem.set(readItem.get()-lowestIndex)).
	 * 
	 * <p>
	 * Additionally, if the number of items currently in use is greater than the
	 * allowed percentage and the buffer has completed less than the specified
	 * number of expansions, then the buffer is increased by a number of
	 * elements corresponding to the set percentage.
	 * 
	 * @throws IOException
	 * 
	 */
	private void doLockdownMaintenance() throws IOException{
		//prevent read and write
		consumerLock.lock();
		producerLock.lock();
		try{

			//allows maintenance to be caught up with read item, preventing some files from being left behind
			doPreConsumeMaintenance();
			final int readItem = readItemIndex.get();
			//items from the maintenance index to the current read item may not have been backed to file

			final int writeItem = writeItemIndex.get();

			//drop down both indexes
			readItemIndex.set(0);
			writeItemIndex.getAndAdd(-readItem);

			//if too much of the buffer is in use and it can be expanded, do so.
			if ((writeItem - readItem + 1) >= (mappedByteBufferArray.length * (BUFFER_USAGE_THRESHOLD_PERCENT/100)) && (bufferIncreaseCounter < BUFFER_INCREASE_COUNT)){
				final int newBuffCount = (int)Math.ceil(mappedByteBufferArray.length * (1.0 + (BUFFER_INCREASE_PERCENT/100)));  
				final long newBuffSpace = bufferItemSize * newBuffCount;
				final long maxDiskSpace = fileSize * fileLimit;

				if(newBuffSpace <= maxDiskSpace){
					bufferItemLimit.set(newBuffCount);
					bufferIncreaseCounter++;
				}
			}

			//create the new buffer
			final DiskMappedByteBufferItem[] tempBuff = new DiskMappedByteBufferItem[bufferItemLimit.get()];

			//copy data from the old buffer to the new buffer
			System.arraycopy(mappedByteBufferArray, readItem, tempBuff, 0, (writeItem - readItem)+1);

			mappedByteBufferArray = tempBuff;
			maintenanceIndex = 0;
		}
		finally{
			//now that the maintenance is done, let reading & writing continue
			consumerLock.unlock();
			producerLock.unlock();
		}
	}

	/**
	 * Accessory function used by maintenance to backup data to files as
	 * necessary. If the data is already backed to a file, then the function
	 * immediately returns. Otherwise the buffer item indicated by the given
	 * index is given the current file, the file name, and an offset in bytes
	 * and is told to put its data there. After the data is pushed, the amount
	 * of data contained by the buffer item is added to the current file offset.
	 * When the offset has surpassed the set file size, the current file is
	 * closed and a new one is opened
	 * 
	 * @param indexToBePushed
	 *            Index integer of the item in the mappedByteBufferArray that
	 *            will be checked and pushed to a backing file
	 * 
	 * @return TRUE if the buffer is backed to a file, FALSE if it is not
	 * 
	 * @throws IOException
	 *             If a file write fails and switching to memory only mode
	 *             incurs an I/O error or an I/O error occurs during closing a
	 *             file.
	 */
	private boolean checkAndPushToFile(int indexToBePushed) throws IOException{
		//if already backed, return
		if( (mappedByteBufferArray[indexToBePushed].getFileName() != null) && (mappedByteBufferArray[indexToBePushed].getFileOffset() >= 0) ){
			return true;
		}
		//if there is no file open, open if allowed necessary
		if(currFile == null){
			//don't open a new file if it's not allowed
			if((fileCount >= fileLimit) && !backupAllData){
				return false;
			}
			else{
				logger.info("DMBB - backing of files has resumed");
				moveToNewFile();
				if(currFile == null){
					return false;
				}
			}
		}
		
		/* 
		 *  check and move to next file BEFORE committing the current item to a file.
		 *           This way we only create files that we need. Additionally, alter the check behavior
		 *           so files will be less than or equal to the file size value, unless file size is less than
		 *           the size of buffer items.
		 */
		if( currFileOffset > 0 && (currFileOffset + mappedByteBufferArray[indexToBePushed].getWriteIndex()) > fileSize){
			final int lastIndex = indexToBePushed - 1;
			if(lastIndex >= 0 && (mappedByteBufferArray[lastIndex] != null)){
				mappedByteBufferArray[lastIndex].setIsLast(true);
			}
			moveToNewFile();
			
			// moveToNewFile could have gone south and we're now in memory only mode
			if(currFile == null){
				return false;
			}
		}
		
		try {
			mappedByteBufferArray[indexToBePushed].pushToFile(currFile, currFileName, currFileOffset);
			currFileOffset += mappedByteBufferArray[indexToBePushed].getLimit();

		} catch (final IOException e1) {

			e1.printStackTrace();
			switchToMemoryOnly();
			return false;
		}
		
		return true;
	}

	/**
	 * Convenience function that assists the DiskMappedByteBuffer in moving to a
	 * new file. If there is an issue closing the old file or with switching to
	 * memory only mode, which loads data from backing buffers, then an
	 * IOException is thrown
	 * 
	 * @throws IOException
	 *             If an I/O error occurs
	 */
	private void moveToNewFile() throws IOException{
		//close the old file if it's open
		if(currFile != null){
			currFile.close();
		}
		//if we can move to a new file, do it
		if((fileCount < fileLimit) || backupAllData){
			//update tracking info
			currFileName = bufferDir + File.separator + Long.toString(System.currentTimeMillis()) + ".dat";
			currFileOffset = 0;
			//try to make the new file. if not, there's a problem and we need to switch to memory-only mode
			try {
				currFile = new RandomAccessFile(currFileName, "rw");
				setFileCount(fileCount + 1);
			} catch (final FileNotFoundException e) {
				//  Moved printing of stack trace to separate debug level print.
				//change to memory only mode
				logger.warn("DiskMappedByteBuffer - Unable to move to new file");
				logger.debug(e);

				switchToMemoryOnly();
			}
		}
		else{
			currFile = null;
			logger.info("DMBB - At file count limit, backing to files halted");
		}
	}

	/**
	 * Function executed by consumerWindowCallable, which is conducted by the
	 * maintenance executor in its single thread while regular maintenance is
	 * not being conducted. When the consumer thread requests for this function
	 * to be executed, all items that are within the consumer window are loaded
	 * from their backing files if they are not.
	 * 
	 * @throws IOException
	 *             An I/O error occurs in reading from a file.
	 */
	private void doLoadConsumerWindowItems() throws IOException{
		//starting at the read index, through the consumer window
		int currLoadIndex = readItemIndex.get();
		try{
			for(;currLoadIndex < (readItemIndex.get() + consumerWindowSize) && currLoadIndex < writeItemIndex.get();currLoadIndex++){

				final boolean bufferLoaded = mappedByteBufferArray[currLoadIndex].isLoaded();
				final String loadFileName = mappedByteBufferArray[currLoadIndex].getFileName();
				final long loadFileOffset = mappedByteBufferArray[currLoadIndex].getFileOffset();
				//if it can be loaded, do so
				if(!bufferLoaded && loadFileName != null && loadFileOffset >= 0){
					mappedByteBufferArray[currLoadIndex].pullFromFile();
					logger.debug("pulled index " + currLoadIndex + " from file");
				} //however, if the buffer is empty and it can't be loaded, throw exception.
				else if(!bufferLoaded && (loadFileName == null || loadFileOffset < 0)){
					logger.warn("DMBB - data needs to be loaded, but cannot");
					logger.info("item " + currLoadIndex + " FileName " + String.valueOf(loadFileName) + " offset " + loadFileOffset);
					logger.info("consume @ " + readItemIndex.get() + " produce @ " + writeItemIndex.get());
					throw new FileNotFoundException();
				}
				//else item is loaded and therefore no action is necessary

			}
		}
		//if a buffer couldn't be loaded, catch the exception here (all items after the exception don't need to be loaded
		catch(final FileNotFoundException f){
			fileNotFoundShutdown(currLoadIndex);
		}
	}


	/**
	 * Organizes a soft shutdown of the buffer when data was unable to be
	 * retrieved from the disk. Because it is unknown how much of the file data
	 * is in buffer, how much was not but is recoverable, and how much was lost,
	 * all files are kept so the user will potentially have a copy of any
	 * missing data. The buffer is put into memory only mode and an End of File
	 * marker is placed at the beginning of the buffer item indicated by the
	 * index.
	 * 
	 * @param earlyEndIndex
	 *            index of the buffer item where the early End of File marker
	 *            will be set.
	 */
	private void fileNotFoundShutdown(int earlyEndIndex){
		logger.info("DiskMappedByteBuffer - File Not Found shutdown - All backing files will be kept");
		//don't delete data, allow user to attempt to recover 
		deleteFiles = false;
		useFiles = false;
		setShutdown(false);
		//set so that the end of data is now at this item
		writeItemIndex.set(earlyEndIndex);
		if(!mappedByteBufferArray[writeItemIndex.get()].isLoaded()){
			mappedByteBufferArray[writeItemIndex.get()] = new DiskMappedByteBufferItem(logger);
		}
		//prevent further producing (bringing in) of data
		setShutdown(false);
		try {
			currFile.close();
		} catch (final IOException e) {
			logger.debug("DiskMappedByteBuffer - fileNotFoundShutdown issue while closing file", e);
		}
	}

	/**
	 * When a disk write error has occurred, no further data is written to disk
	 * and to prevent any future disk errors all data is recovered from the disk
	 * and the buffer reverts to memory-only mode.
	 * 
	 * @throws IOException
	 *             Thrown when a mapped byte buffer was unable to retrieve its
	 *             data from a file.
	 */
	private void switchToMemoryOnly() throws IOException{
		final int readItemCopy = readItemIndex.get();
		final int writeItemCopy = writeItemIndex.get();
		int check;
		int pulled = 0;

		backupAllData = false;

		logger.warn("DiskMappedByteBuffer - Switching to memory-only mode");

		for(check = readItemCopy + 1;check < writeItemCopy; check++){
			if(!mappedByteBufferArray[check].isLoaded() && (mappedByteBufferArray[check].getFileName() != null)){
				try{
					mappedByteBufferArray[check].pullFromFile();
					logger.debug("pulled index " + check + " from file");
					pulled++;
				}
				catch(final FileNotFoundException f){
					logger.info("DMBB - not all data could be reverted to memory, stream will terminate early");
					fileNotFoundShutdown(check);
				}
			}
		}
		if(deleteFiles && !backupAllData){
			removeAllDiskData();
		}

		logger.debug("Pulled " + pulled + " blocks from files");

		useFiles = false;
		// no point in keeping the filename now that we're in memory only mode.
		currFile = null;
	}

	/**
	 * Delete all created files and the base directory.
	 */
	private void removeAllDiskData(){
		final File tempFile = new File(bufferDir);
		//make sure all files are deleted
		final File[] files = tempFile.listFiles();
		if(files!= null){
			for(final File f: files){
				f.delete();
				setFileCount(fileCount - 1);
			}
		}
		tempFile.delete();
	}


	/**
	 * Record an IO exception at the current write location. Due to the buffered
	 * nature of the data, errors in the stream must be buffered as well so they
	 * are encountered at the point in the stream as they were received.
	 * 
	 * @param thrown
	 *            the IO exception to be stored
	 */
	protected void setError(IOException thrown){
		lastErrorObject = thrown;
		lastErrorItem = writeItemIndex.get();
		lastErrorOffset = mappedByteBufferArray[lastErrorItem].getWriteIndex();
		mappedByteBufferArray[lastErrorItem].setError(thrown);
	}

	/**
	 * get the current size of the buffer
	 * 
	 * @return Number of elements in the buffer's array
	 */
	protected int getBufferSize(){
		return mappedByteBufferArray.length;
	}

	/**
	 * Force the buffer to calculate the number of elements currently in use
	 * (number of elements between, and including, the read and write index
	 * items). The item count high water mark is also updated
	 */
	private void setBufferItemCount(){
		bufferItemCount = (writeItemIndex.get() - readItemIndex.get()) + 1;

		bufferItemHighWaterMark = Math.max(bufferItemCount, bufferItemHighWaterMark);
	}

	/**
	 * Get the number of elements in the buffer that are in use.
	 * 
	 * @return Number of elements between, and including, the read and write
	 *         indexed items in the buffer
	 */
	protected int getBufferItemCount(){
		return bufferItemCount;
	}

	/**
	 * Get the size, in bytes, specified for the DiskMappedByteBufferItems
	 * 
	 * @return Number of bytes of data each DiskMappedByteBuffer can hold
	 */
	protected int getBufferItemSize(){
		return bufferItemSize;
	}

	/**
	 * Get the maximum number of elements that were utilized at any point of
	 * this buffer's lifetime
	 * 
	 * @return The maximum value bufferItemCount has possessed during the
	 *         lifetime of this buffer
	 */
	protected int getBufferItemHighWaterMark(){
		return bufferItemHighWaterMark;
	}

	/**
	 * Get if the buffer is deleting files when they are no longer backing data
	 * in the buffer
	 * 
	 * @return TRUE if files are deleted when no longer in use. FALSE if files
	 *         are kept when no longer in use
	 */
	protected boolean isDeleteFiles(){
		return this.deleteFiles;
	}

	/**
	 * Get if the buffer is backing all data to files. If all data is being
	 * backed into files, then all data, including already consumed data, is
	 * pushed to a file and all files are kept.
	 * 
	 * @return TRUE if all data is being backed to files. FALSE if not
	 */
	protected boolean isBackupAllData(){
		return this.backupAllData;
	}
	
	/**
	 * Get if files are being used to back data to disk
	 * 
	 * @return True if disk based files are being used, false if buffer is
	 *         memory-only
	 */
	protected boolean isUseFiles(){
		return this.useFiles;
	}

	/**
	 * Updates the file count with the new value and updates the high water
	 * mark, or maximum number, of files used
	 * 
	 * @param newFileCount
	 *            New number of files being utilized.
	 */
	private void setFileCount(int newFileCount){
		fileCount = newFileCount;

		fileHighWaterMark = Math.max(fileCount, fileHighWaterMark);

		logger.debug("Files: " + fileCount);

	}

	/**
	 * Gets the number of files currently in use
	 * 
	 * @return Number of files currently being used to back the buffer
	 */
	protected int getFileCount(){
		return fileCount;
	}
	
	/**
	 * Get the maximum number of files that can be used
	 * 
	 * @return Number of files that can be used to back the buffer
	 */
	protected int getFileLimit(){
		return this.fileLimit;
	}

	/**
	 * Get the maximum number of files that were utilized at any point of this
	 * buffer's lifetime
	 * 
	 * @return The maximum value fileCount has possessed during the lifetime of
	 *         this buffer
	 */
	protected int getFileHighWaterMark(){
		return fileHighWaterMark;
	}

	/**
	 * Name of the current file being used to back data
	 * 
	 * @return File name, including known path, to the file currently open
	 */
	protected String getCurrFileName(){
		return currFileName;
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
	protected int getConsumerWindowSize(){
		return consumerWindowSize;
	}
	
	/**
	 * set signals showing if incoming data is complete. An EOF is set at this
	 * point, the current write item is set as the last for its file, and no
	 * more data is allowed to be written.
	 */
	protected void setProduceDone(){
		setError(new EOFException());
		produceDone.set(true);
		getCurrentWriteItem().setIsLast(true);
	}

	/**
	 * Shutdown the buffer. Will either conduct a "soft" shutdown where incoming
	 * data is stopped, but data in the buffer can be read (false) or a hard
	 * shtudown and all data in the buffer is lose (true).
	 * 
	 * @param complete
	 *            TRUE if a full shutdown is completed (no read or write to
	 *            buffer), FALSE if partial shutdown (no more write in, but read
	 *            out is allowed)
	 */
	protected void setShutdown(boolean complete){
		shutdown.set(complete);
		setProduceDone();
		if(complete){

			maintenanceExecutor.shutdown();
			//wait for the last maintenance to finish executing before continuing 
			try {
				maintenanceExecutor.awaitTermination(10,TimeUnit.SECONDS);
			} catch (final InterruptedException e1) {
				logger.debug("DiskMappedByteBuffer - InterruptedException while waiting for maintenanceExecutor to terminate");
			}
			//alert the user that maintenance may throw errors and attempt to force it closed.
			if(!maintenanceExecutor.isTerminated()){
				logger.info("DiskMappedByteBuffer - maintenanceExecutor did NOT terminate within the allowed time, there may be unexpected errors");
				maintenanceExecutor.shutdownNow();
			}

			if(useFiles){
				try {
					if(currFile != null){
						if(backupAllData){
							consumerLock.lock();
							try{
								//do maintenance just once more to make sure all data is written to files
								doNormalMaintenance();
								//push current item to file. Will throw an additional error if normal maintenance also encounters an error
								checkAndPushToFile(writeItemIndex.get());
							}
							finally{
								consumerLock.unlock();
							}
						}
						currFile.close();
					}
				}
				catch (final IOException e) {
					logger.warn("IOException encountered while closing file");
				}
				finally{
					if(deleteFiles){
						removeAllDiskData();
					}
				}
			}
		}
	}

	/**
	 * reports if a full shutdown is being conducted
	 * 
	 * @return TRUE if full shutdown is being completed (no read or write),
	 *         FALSE if not
	 */
	protected boolean isCompleteShutdown(){
		return shutdown.get();
	}


	/**
	 * Returns a reference to the currently read item in the
	 * mappedByteBufferArray
	 * 
	 * @return the DiskMappedByteBuffer item where reading last occurred
	 */
	protected DiskMappedByteBufferItem getCurrentReadItem(){
		return mappedByteBufferArray[readItemIndex.get()];
	}

	/**
	 * If the producer and consumer are not on the same DiskMappedByteBufferItem
	 * in the mappedByteBufferArray, the next item to be read from is returned.
	 * Otherwise, the current item being read from is returned.
	 * 
	 * @return the newest DiskMappedByteBuffer item where reading is to be
	 *         continued from
	 * 
	 */
	protected DiskMappedByteBufferItem getNextReadItem(){

		//at the end of a buffer, and can move along
		if(readItemIndex.get() < writeItemIndex.get() && (mappedByteBufferArray[readItemIndex.get()].getReadIndex() >= mappedByteBufferArray[readItemIndex.get()].getWriteIndex()) ){

			if(!mappedByteBufferArray[readItemIndex.get()].isWriteDone()){

				mappedByteBufferArray[readItemIndex.get()].setWriteDone(true);
			}

			moveToNextConsume();
		}

		return mappedByteBufferArray[readItemIndex.get()];
	}

	/**
	 * Returns a reference to the DiskMappedByteBufferItem where writing is to
	 * be continued
	 * 
	 * @return the DiskMappedByteBufferItem where writing last occurred
	 */
	protected DiskMappedByteBufferItem getCurrentWriteItem(){
		return mappedByteBufferArray[writeItemIndex.get()];
	}

	/**
	 * Gets the next DiskMappedByteBufferItem to be written to and returns a
	 * reference to it. If the current item is at the end of the buffer, a delay
	 * is invoked until maintenance has had an opportunity to be conducted.
	 * 
	 * @return the next DiskMappedByteBufferItem to be written on.
	 * 
	 * @throws IllegalMonitorStateException If the thread calling this method does not hold the producer lock.
	 * 
	 * Updated to unlock when sleeping, if lock
	 *          is held , in order to allow maintenance to be performed.
	 */
	protected DiskMappedByteBufferItem getNextWriteItem(){

		int writeWait = 0;
		//Get current state of lock in regards to this thread. lock is handled differently in this method depending on this value
		if(!producerLock.isHeldByCurrentThread()){
			throw new IllegalMonitorStateException("This method can only be utilized within a thread that has acquired the producer lock");
		}
		
		DiskMappedByteBufferItem retItem = null;
		
		try{
			// added check for full shutdown. Will hold up here otherwise
			//maintenance should have shifted before we get here, but if we have, then writer needs to wait for maintenance to fix it.
			while((writeItemIndex.get() + 1) >= bufferItemLimit.get() && !shutdown.get()){

				//only start printing message after second wait.
				if(writeWait >= 10){
					logger.warn("DiskMappedByteBuffer - Currently unable to put data in buffer. Data processing is running slow or buffer settings are suboptimal.");
					writeWait = 0;
				}
				writeWait++;

				//Release lock for sleep. This will allow lockdown maintenance to actually fix the buffer...
				producerUnlock();

				randomSleep(MAINTENANCE_DELAY);

				//Reacquire the lock
				producerLock();
				
			}
			if(!shutdown.get()){
				moveToNextProduce();
				retItem = mappedByteBufferArray[writeItemIndex.get()];
			}
		}
		
		//before leaving, even if something unexpected occurred, make sure lock state is back to how it was before this method was called
		finally{
			if(!producerLock.isLocked()){
				producerLock();
			}
		}
		return retItem;
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
	 * Acquires the producer lock.
	 *
	 * <p>
	 * Acquires the producer lock if it is not held by another thread and
	 * returns immediately, setting the lock hold count to one.
	 *
	 * <p>
	 * If the current thread already holds the lock then the hold count is
	 * incremented by one and the method returns immediately.
	 *
	 * <p>
	 * If the producer lock is held by another thread then the current thread
	 * becomes disabled for thread scheduling purposes and lies dormant until
	 * the producer lock has been acquired, at which time the producer lock hold
	 * count is set to one.
	 */
	protected void producerLock(){
		producerLock.lock();
	}

	/**
	 * Attempts to release the producer lock.
	 *
	 * <p>
	 * If the current thread is the holder of the producer lock then the hold
	 * count is decremented. If the hold count is now zero then the producer
	 * lock is released. If the current thread is not the holder of the producer
	 * lock then {@link IllegalMonitorStateException} is thrown.
	 *
	 * @throws IllegalMonitorStateException
	 *             if the current thread does not hold the producer lock
	 */
	protected void producerUnlock(){
		producerLock.unlock();
	}

	/**
	 * Acquires the consumer lock.
	 *
	 * <p>
	 * Acquires the consumer lock if it is not held by another thread and
	 * returns immediately, setting the lock hold count to one.
	 *
	 * <p>
	 * If the current thread already holds the lock then the hold count is
	 * incremented by one and the method returns immediately.
	 *
	 * <p>
	 * If the consumer lock is held by another thread then the current thread
	 * becomes disabled for thread scheduling purposes and lies dormant until
	 * the consumer lock has been acquired, at which time the consumer lock hold
	 * count is set to one.
	 */
	protected void consumerLock(){
		consumerLock.lock();
	}

	/**
	 * Attempts to release the consumer lock.
	 *
	 * <p>
	 * If the current thread is the holder of the consumer lock then the hold
	 * count is decremented. If the hold count is now zero then the consumer
	 * lock is released. If the current thread is not the holder of the consumer
	 * lock then {@link IllegalMonitorStateException} is thrown.
	 *
	 * @throws IllegalMonitorStateException
	 *             if the current thread does not hold the consumer lock
	 */
	protected void consumerUnlock(){
		consumerLock.unlock();
	}
	
	/**
	 * Schedules the maintenance executor to load from file the number of
	 * elements in the buffer specified by the buffer window size, starting with
	 * the current read item
	 */
	protected void loadConsumerWindow(){
		try {
			maintenanceExecutor.submit(consumerWindowLoadCallable).get();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		} catch (final ExecutionException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Gets the current value of the produceDone atomic variable
	 * 
	 * @return boolean value of the produceDone atomic variable
	 */
	protected boolean isProduceDone(){
		return produceDone.get();
	}
	
	
	/**
	 * Clear the buffer by creating a new empty buffer and deleting all of the
	 * backing files
	 * 
	 * @throws UnsupportedOperationException
	 *             there was an issue clearing the buffer
	 */
	private int doClearBuffer() throws UnsupportedOperationException{
		
		producerLock.lock();
		consumerLock.lock();
		
		try{
			
			// since the read and write pointers has been reset.
    		doNotify();
    		
    		IOException tempError = null;

    		if(writeItemIndex.get() + mappedByteBufferArray[writeItemIndex.get()].getWriteIndex() != 0){
    			
    			//keep the errorif we're at one
    			if(lastErrorItem == writeItemIndex.get() && lastErrorOffset == mappedByteBufferArray[writeItemIndex.get()].getWriteIndex() && lastErrorObject != null){
    				tempError = lastErrorObject;
    			}
    			
    			//the "original" length of the buffer is not kept, so just recreate it at the current length
    			mappedByteBufferArray = new DiskMappedByteBufferItem[bufferItemLimit.get()];
    			mappedByteBufferArray[0] = new DiskMappedByteBufferItem(bufferItemSize, logger);
    			
    			writeItemIndex.set(0);
    			readItemIndex.set(0);
    			
    			setBufferItemCount();
    			
    			removeAllDiskData();

    			//attempt to recreate the directory - should be unique this time
    			if(new File(bufferDir).mkdirs()){
    				try {
    					moveToNewFile();
    				} catch (final IOException e) {
    					logger.debug("DiskMappedByteBuffer - irrelevant IOException thrown");
    				}
    			}
    			else{
    				logger.warn("Unable to create directory, switching to memory-only mode");
    				useFiles = false;
    			}

    			//start counting the number of files
    			setFileCount(useFiles?1:0);

    			//make sure maintenance variables are clear
    			maintenanceIndex = 0;

    			lastErrorItem = -1;
    			lastErrorOffset = -1;
    			lastErrorObject = null;

    			if(tempError != null){
    				setError(tempError);
    			}
    		}
    		else{
    			throw new UnsupportedOperationException("Buffer currently contains no data. Try again later.");
    		}
    	}
    	finally{
    		
    		consumerLock.unlock();
    		producerLock.unlock();
    	}
		return 1;
	}
	
	/**
	 * Sets up the maintenance executor to clear the buffer.
	 * 
	 * @throws IOException
	 *             if the <code>clearBufferCallable</code> threw an exception,
	 *             was interrupted while waiting, or could not be scheduled for
	 *             execution
	 * @throws IllegalStateException
	 *             if the buffer is not in an operational state
	 *             
	 */
	protected void clearBuffer() throws IOException, IllegalStateException{
		
		if(!shutdown.get() && !maintenanceExecutor.isShutdown()){
			try {
				maintenanceExecutor.submit(clearBufferCallable).get();
			} catch (final InterruptedException e) {
				throw new IOException("DiskMappedByteBufferManager: The buffer clearing operation was interrupted - " + e.getMessage());
			} catch (final ExecutionException e) {
				throw new IOException("DiskMappedByteBufferManager: " + e.getCause().getMessage());
			} catch (final RejectedExecutionException e){
				throw new IllegalStateException("DiskMappedByteBufferManager: The buffer is no longer in use. No data to be cleared.");
			}
		}
		else{
			throw new IllegalStateException("The buffer has been shut down. No data to be cleared.");
		}
	}
	
	

	/**
	 * Causes the read thread to wait until either another thread invokes the
	 * {@link java.lang.Object#notify()} method or the
	 * {@link java.lang.Object#notifyAll()} method for the current read object.
	 * In other words, this method behaves exactly as if it simply performs the
	 * call {@code wait(0)}.
	 * 
	 * @see java.lang.Object#wait(long)
	 */
	protected void doWait(){
		synchronized (mappedByteBufferArray[readItemIndex.get()]){
			try {
				mappedByteBufferArray[readItemIndex.get()].wait();
			} catch (final InterruptedException e) {
				logger.debug("DiskMappedByteBufferManager: The wait for a read process was interrupted, continuing...");
			}
		}
	}
	
	/**
	 * Wakes up all consumer threads that are waiting on the current read
	 * object's monitor. A read thread waits on a
	 * {@code DiskMappedByteBufferItem} object's monitor by calling one of the
	 * {@code DiskMappedByteBufferManager.wait} methods.
	 */
	protected void doNotify(){
		synchronized(mappedByteBufferArray[readItemIndex.get()]){
			mappedByteBufferArray[readItemIndex.get()].notifyAll();
		}
	}

}