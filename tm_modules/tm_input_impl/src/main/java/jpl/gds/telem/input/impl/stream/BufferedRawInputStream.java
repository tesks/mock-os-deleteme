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
package jpl.gds.telem.input.impl.stream;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.buffer.DiskMappedByteBufferConfig;
import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.performance.IPerformanceData;
import jpl.gds.shared.performance.IPerformanceProvider;
import jpl.gds.shared.performance.PerformanceSummaryPublisher;
import jpl.gds.shared.performance.QueuePerformanceData;
import jpl.gds.telem.input.api.config.TelemetryInputProperties;
import jpl.gds.telem.input.api.stream.IRawInputStream;



/**
 * BufferedRawInputStream Handles interfacing the DiskBackedBufferedInputStream
 * with RawInputConnection (incoming data), RawInputHandler (conductor), and
 * RawInputStreamProcessor (outgoing data).
 * 
 *
 * MPCS-7839 - 1/6/16 - Name changed from
 *          DiskBackedBufferedInputStream to BufferedRawInputStream. All
 *          references updated as well.
 * 
 * MPCS-8083 - 06/06/16 - changed DiskBackedBufferedInputStream
 *          to LoggingdiskBackedBufferedInputStream for the newly added logging
 *          functionality
 */

public class BufferedRawInputStream implements IRawInputStream {

	// MPCS-8083 06/06/16 - changed to LoggingDiskBackedBufferedInputStream
	private final LoggingDiskBackedBufferedInputStream bis;
	// MPCS-7839  1/6/16 - moved DataInputStream to shared DiskBackedBufferedInputStream

	private QueuePerformanceData bufferPerformance = null;
	private QueuePerformanceData filePerformance = null;
	private final PerformanceReporter perfReporter;

	// MPCS-7839  1/6/16 - moved producerThread to shared DiskBackedBufferedInputStream

	byte[] data;
	
	private final ApplicationContext appContext;

	// MPCS-7839  1/6/16 - moved baseDirectory and fileDirectory to shared DiskBackedBufferedInputStream

	/**
	 * Primary constructor A thread is started with the DataInputStream and the
	 * DiskMappedByteBuffer where data is pushed into the buffer until an EOF
	 * flag is received (a report of -1 bytes returned) or any shutdown request
	 * is received.
	 * 
	 * @param appContext the current application context
	 * @param dis
	 *            the <code>DataInputStream</code> to be buffered
	 * @param defaultPath the default buffer output directory
	 * 
	 */
	public BufferedRawInputStream(final ApplicationContext appContext, final DataInputStream dis, final String defaultPath) {
		this(appContext, dis, defaultPath, null);
	}

	/**
	 * Constructor Configuration information is read from the
	 * SessionConfiguration option before a DiskBackedBufferedInputStream is
	 * instantiated with the configuration information and the given
	 * DataInputStream. A performance reporter is created and configured for the
	 * buffer and its associated files.
	 * 
	 * @param appContext the current application context
	 * @param dis
	 *            the <code>DataInputStream</code> to be buffered
	 * @param defaultPath the default buffer output directory
	 * @param data
	 *            the data in a byte array
	 * 
	 */
	public BufferedRawInputStream(final ApplicationContext appContext, final DataInputStream dis,  final String defaultPath, final byte[] data) {

		this.appContext = appContext;
		final TelemetryInputProperties configOpts = appContext.getBean(TelemetryInputProperties.class);

		//MPCS-7788  11/18/15 config constructor using RawInputConfig was removed, updated to now setup the config before using it.
		final DiskMappedByteBufferConfig configDMBB = new DiskMappedByteBufferConfig();
		configDMBB.setBufferItemSize(configOpts.getBufferItemSize());
		configDMBB.setBufferItemLimit(configOpts.getBufferItemCount());
		configDMBB.setConsumerWindowSize(configOpts.getBufferWindowSize());
		configDMBB.setMaintenanceDelay(configOpts.getBufferMaintenanceDelay());
		configDMBB.setFileSize(configOpts.getBufferFileSize());
		configDMBB.setFileLimit(configOpts.getBufferFileLimit());
		configDMBB.setBackupAllData(configOpts.getBackupAll());
		configDMBB.setDeleteFiles(configOpts.getDeleteBufferFiles());
		configDMBB.setUseFiles(configOpts.getUseFiles());
		configDMBB.setBufferDir(getDirPath(configOpts, defaultPath));

		//set up performance data objects
		bufferPerformance = new QueuePerformanceData(appContext.getBean(PerformanceProperties.class), "Input Stream Buffer", configOpts.getBufferItemCount(), false, false, "Array items");
		bufferPerformance.setYellowBound(configOpts.getBufferYellowLevel());
		bufferPerformance.setRedBound(configOpts.getBufferRedLevel());
		filePerformance = new QueuePerformanceData(appContext.getBean(PerformanceProperties.class), "Input Stream Buffer Files", configOpts.getBufferFileLimit(), false, false, "files");
		filePerformance.setYellowBound(configOpts.getBufferFileYellowLevel());
		filePerformance.setRedBound(configOpts.getBufferFileRedLevel());
		perfReporter = new PerformanceReporter();

		
		// MPCS-7839  1/6/16 - replaced creation of DiskMappedByteBuffer with creation of DiskBackedBufferedInputStream
        bis = new LoggingDiskBackedBufferedInputStream(dis, configDMBB,
                appContext.getBean(IMessagePublicationBus.class),
                                                       TraceManager.getTracer(appContext, Loggers.TLM_INPUT),
                configOpts.getRawSummaryTimerInterval());

		this.data = data;
	}


	/**
	 * Return the DiskBackedBufferedInputStream connected to a DataInputStream.
	 * Allows all processors to pull data out without any modification to their
	 * current structure.
	 * 
	 * @return DataInputStream interfacing the RawInputStream.
	 */
	@Override
    public DataInputStream getDataInputStream(){
		return new DataInputStream(bis);
	}

	/**
	 * Get the data stored in the data byte array. Put in place to allow
	 * BufferedRawInputStream to be fully compatible and interchangeable with
	 * all RawInputStream calls.
	 * 
	 * @return the data stored in this stream
	 */
	@Override
    public byte[] getData(){
		//figure out what kind of max size of data we want to return on this???
		return this.data;
	}

	/**
	 * Close the stream. The boolean value given dictates if the shutdown will
	 * just close data input (false) or fully shutdown (true) the stream.
	 * 
	 * @param shutdownFull
	 *            - TRUE if the buffer is to be completely shutdown immediately
	 *            FALSE if the buffer is to stop buffering incoming data, but
	 *            allow all current data to flow out
	 * @throws IOException
	 *             if there is an error closing the stream
	 * 
	 *             A shutdown request is passed to the raw input stream buffer.
	 *             in either instance the reader thread is attempted to be
	 *             joined back with the main thread.
	 */
	public void close(final boolean shutdownFull) throws IOException{
		// MPCS-7839  1/6/16 - moved close functionality to shared DiskBackedBufferedInputStream
		bis.close(shutdownFull);

		perfReporter.deregister();
	}

	// MPCS-7839  1/6/16  - moved doProduceThread to shared DiskBackedBufferedInputStream

	/**
	 * Full shutdown of the stream. If any data is contained within the buffer
	 * it will be lost.
	 * 
	 * @see jpl.gds.shared.buffer.DiskBackedBufferedInputStream#close()
	 * 
	 * @throws IOException
	 *             an I/O Error occurs during closing of the
	 *             DiskMappedByteBuffer
	 */
	@Override
    public void close() throws IOException{
		close(true);
	}

	/**
	 * Returns the number of elements in the contained buffer.
	 * 
	 * @return length value of the array contained within the
	 *         DiskMappedByteBuffer
	 */
	public int getBufferSize(){
		return bis.getBufferSize();
	}

	/**
	 * Get the string representing the name of the file where disk backed data
	 * is being stored
	 * 
	 * @return String of the name of the file currently being utilized for
	 *         pushing data to disk
	 */
	public String getCurrFileName() {
		return bis.getCurrFileName();
	}

	/**
	 * Get the number of bytes written into the buffer.
	 * 
	 * @return Number of bytes written into the buffer
	 */
	public long getBytesIn(){
		return bis.getBytesIn();
	}

	/**
	 * Get the number of bytes read out of the buffer.
	 * 
	 * @return Number of bytes read out of the buffer.
	 */
	public long getBytesOut(){
		return bis.getBytesOut();
	}

	/**
	 * Get the data rate at which data has been consumed by the buffer, either
	 * until now or when consuming was terminated
	 * 
	 * @return Bytes per second at which data has been placed into the buffer.
	 */
	public double getBpsIn(){
		return bis.getBpsIn();
	}

	/**
	 * Get the data rate at which data has been produced by the buffer.
	 *  
	 * @return Bytes per second at which data has been given out by the buffer.
	 */
	public double getBpsOut(){
		return bis.getBpsOut();
	}

	/**
	 * Get the consumer window size of the contained buffer. The consumer window
	 * is the total number of elements, starting from the item currently being
	 * read from, that have their buffer loaded in memory. This many items
	 * potentially may not be loaded if it extends past the producer item or the
	 * end of the buffer.
	 * 
	 * @return Number of elements in the DiskMappedByteBuffer that may be kept
	 *         in memory and not solely backed to files
	 */
	public int getConsumerWindowSize(){
		return bis.getConsumerWindowSize();
	}

	/**
	 * Determines the directory path where the files backing the buffer will be
	 * located. If the directory value in the RawInputConfig matches the
	 * SESSION_DIR value, then the session directory structure is utilized.
	 * Otherwise, the given value is assumed to be a valid path and is utilized.
	 * 
	 * @param configOpts
	 *            A valid RawInputConfig object
	 * @param sessConfig
	 *            A valid SessionConfiguration object
	 * @return A String representing the path to the folder where all buffer
	 *         backing files will be located.
	 */
	private String getDirPath(final TelemetryInputProperties configOpts, final String defaultDirPath){
		final String dirVal = configOpts.getBufferDir();
		String dirPath = null;

		//If possible use info from the current application to setup a path for files
		if(dirVal == null){
			//string holding the constructed directory path
			dirPath = defaultDirPath;
		}
		else{
			dirPath = dirVal;
		}

		dirPath += File.separator + "BufferedRawInputStream" + File.separator;
;
		//put each creation under a specific folder
		dirPath += File.separator + ManagementFactory.getRuntimeMXBean().getName().split("@")[0] + "-" + Long.toString(System.currentTimeMillis());

		return dirPath;
	}
	
	/**
	 * Clears the DiskBackedBufferedInputStream
	 * 
	 * @throws UnsupportedOperationException
	 *            if the IRawInputStream does not support buffer clearing
	 * @throws IOException
	 *             if the clearBuffer request threw an exception, was
	 *             interrupted while waiting, or could not be scheduled for
	 *             execution
	 * @throws IllegalStateException
	 *             if the buffer is not in an operational state
	 */
	@Override
    public void clearInputStreamBuffer() throws UnsupportedOperationException, IOException, IllegalStateException{
		bis.clearBuffer();
	}
	
	
	
	
	/**
	 * Update and report the performance data regarding the buffer itself.
	 * 
	 * @return a list of the performance data items for the buffer 
	 */
	public List<IPerformanceData> getBufferPerformanceData(){

		if(bufferPerformance != null){
			bufferPerformance.setCurrentQueueSize(bis.getBufferItemCount());
			bufferPerformance.setHighWaterMark(bis.getBufferItemHighWaterMark());
			bufferPerformance.setMaxQueueSize(bis.getBufferSize());

			return Arrays.asList((IPerformanceData)bufferPerformance);
		}

		return new LinkedList<IPerformanceData>();
	}

	/**
	 * Update and report the performance data regarding the buffer files.
	 * 
	 * @return a list of the performance data items for the buffer files
	 */
	public List<IPerformanceData> getFilePerformanceData() {

		if(filePerformance != null){
			filePerformance.setCurrentQueueSize(bis.getFileCount());
			filePerformance.setHighWaterMark(bis.getFileHighWaterMark());

			return Arrays.asList((IPerformanceData)filePerformance);
		}
		return new LinkedList<IPerformanceData>();
	}

	/**
	 * PerformanceReporter class, to allow performance data to be reported
	 * 
	 *
	 */
	public class PerformanceReporter implements IPerformanceProvider {

		/** The performance provider name */
		private static final String THIS_PROVIDER = "Buffered Input Stream";

		/**
		 * Constructor. Registers with the performance summary publisher for performance data requests.
		 */
		public PerformanceReporter() {
			/**
			 * MPCS-7927 - trivisk 2/3/2016 - Registering with the
			 * SessionBasedPerformanceSummaryPublisher. This was not correctly
			 * changed after refactoring and moving the base
			 * PerformanceSummaryPublisher class to shared lib.
			 */
			appContext.getBean(PerformanceSummaryPublisher.class).registerProvider(this);
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see jpl.gds.shared.performance.IPerformanceProvider#getProviderName()
		 */
		@Override
		public String getProviderName() {
			return THIS_PROVIDER;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see jpl.gds.shared.performance.IPerformanceProvider#getPerformanceData()
		 */
		@Override
		public List<IPerformanceData> getPerformanceData() {

			final List<IPerformanceData> perfList = new LinkedList<IPerformanceData>();
			//only one instance to get the data from. Both buffer and file performance data are 
			perfList.addAll(BufferedRawInputStream.this.getBufferPerformanceData());
			perfList.addAll(BufferedRawInputStream.this.getFilePerformanceData());

			return perfList;
		}

		/**
		 * De-registers with the performance summary publisher for performance data requests.
		 */
		public void deregister() {
			appContext.getBean(PerformanceSummaryPublisher.class).deregisterProvider(this);
		}

	}
}
