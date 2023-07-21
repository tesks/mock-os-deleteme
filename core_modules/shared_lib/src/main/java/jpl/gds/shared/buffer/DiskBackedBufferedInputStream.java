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
import java.io.IOException;
import java.io.InputStream;

import jpl.gds.shared.log.Tracer;

/**
 * DiskBackedBufferedInputStream Handles interfacing the DiskMappedByteBuffer
 * with RawInputConnection (incoming data), RawInputHandler (conductor), and
 * RawInputStreamProcessor (outgoing data). Automates pushing data into the
 * stream buffer and allows the data to be pulled out as desired.
 * 
 *
 */

public class DiskBackedBufferedInputStream extends DiskMappedByteBuffer{


	//protected static Tracer logger;

	//  added volatile keyword, changed from DataInputStream to InputStream
	private volatile InputStream dis;

	private Thread producerThread;


	//keep track of directory information for cleanup
	private String fileDirectory;

	/**
     * Constructor A thread is started with the InputStream and the
     * DiskMappedByteBuffer where data is pushed into the buffer until an EOF
     * flag is received (a report of -1 bytes returned) or any shutdown request
     * is received.
     * 
     * @param dis
     *            the <code>InputStream</code> to be buffered
     * @param configDMBB
     *            <code>DiskMappedByteBufferConfig</code> is utilized to
     *            configure the <code>DiskMappedByteBuffer</code> at the core of
     *            the DiskBackedBufferedInputStream.
     * @param logger
     *            The context Tracer
     * 
     */
	public DiskBackedBufferedInputStream(InputStream dis, DiskMappedByteBufferConfig configDMBB, Tracer logger){
		
		super(configDMBB, logger);
		
		// set the file directory variable.
		fileDirectory = configDMBB.getBufferDir();
		this.dis = dis;
		//create a thread that will allow the buffer to consume all data from the input stream
		producerThread = doProduceThread();
		producerThread.start();
	}

	/**
	 * Constructor Creates a DiskBackedBufferedInputStream utilizing the default
	 * values in an unconfigured <code>DiskMappedByteBufferConfig</code> object
	 * 
	 * @param dis the <code>InputStream</code> to be buffered
	 */
	//public DiskBackedBufferedInputStream(InputStream dis){
	//	this(dis, new DiskMappedByteBufferConfig());
	//}
	

	/**
	 * Full shutdown of the DiskBackedBufferedInputStream. If files are not
	 * being kept, they have been deleted by the buffer and the directories
	 * known to have been created by BufferedRawInputStream will be deleted
	 * 
	 * @throws IOException
	 *             an I/O Error occurs during closing of the
	 *             DiskMappedByteBuffer
	 */
	@Override
    public void close() throws IOException{
		close(true);

		if(isDeleteFiles()){
			File deletingFolder = new File(fileDirectory);
			File tempFolder = new File(deletingFolder.getParent());

			// change loop parameters to remove any terminations errors
			while(deletingFolder.delete()){
				deletingFolder = tempFolder;
				tempFolder = new File(deletingFolder.getParent());
			}
		}
	}

	/**
	 * Shutdown the DiskBackedBufferedInputStream. Will either conduct a "soft"
	 * shutdown where data is no longer read from the InputStream, but data in
	 * the buffer can be read (false) or a hard shutdown where data is no longer
	 * read from the InputStream and the buffer is closed (true).
	 * 
	 * @param shutdownFull
	 *            TRUE if a full shutdown is completed (no data in or out),
	 *            FALSE if partial shutdown (no more data in, but read out is
	 *            allowed)
	 * @throws IOException if the input stream cannot be closes
	 */
	@Override
    public void close(boolean shutdownFull) throws IOException{
		//shut down the buffer
		setShutdown(shutdownFull);
		try {
			producerThread.join(1000);
		} catch (InterruptedException e) {
		}
		dis.close();
	}

	/**
	 * Connection controlled feeding of data into the buffer. Will continue to
	 * consume, or attempt to consume, all input data until an End of File error
	 * or shutdown request is given. If any other error is received on the
	 * stream, it is stored and attempts to read continue.
	 * 
	 * @return A thread object that will feed data from the input stream into
	 *         the buffer
	 */
	private Thread doProduceThread(){
		Thread rThread = new Thread("BufferedRawInputStream - Producer thread"){
			@Override
            public void run(){
				byte[] writeBuff = new byte[getBufferItemSize()];
				int bytesRead = 0;

				while(!getProduceDone()){
					try {
						bytesRead = dis.read(writeBuff);
					}
					catch (IOException e) {
						//some other IO exception besides an end of file error
						//store it and start closing up shop
						setShutdown(false);
						setError(e);

					}
					catch (Exception e){
						//some other error occurred, we definitely need to shut thingds down
						setShutdown(false);
						setError(new EOFException(e.getMessage()));
					}
					if(bytesRead < 0){
						//reached an End of File
						setShutdown(false);
						//store an EOF exception?
						setError(new EOFException());
						bytesRead = 0;
					}
					write(writeBuff,bytesRead);
				}
				// write index is no longer visible to the outside.
				logger.debug("BufferedInputStream - all data pushed into buffer");
				setInputDoneTime(System.currentTimeMillis());
			}
		};
		return rThread;
	}

}