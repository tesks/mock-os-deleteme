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
package jpl.gds.shared.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.sys.SystemUtilities;

/**
 * An InputStream that reads from a file and meters the data out to a reader at
 * a specified bitrate. It is capable of sending the file once, or of looping
 * through it until forcibly terminated.
 * 
 * Several things are important to note:
 * 
 * In order to make it as fast as possible, this class has been carefully
 * written to avoid minimal movement of data between byte buffers. An additional
 * readNoBuffer() method has been added, to allow readers to use the same byte
 * buffers used by this class. The classic read(buff, off, len) method is also
 * provided, input stream, so the caller can provide a buffer if he prefers.
 * 
 * Readers can only read using the same buffer size as this class uses to read
 * and queue the data. Attempts to read a buffer using an arbitrary size will
 * result in an exception.
 * 
 * The same thing applies to skipping data. One can only skip an even number of
 * buffers in the output queue, not an arbitrary number of bytes.
 * 
 * If the reader does not keep up, warnings will be logged and data will be
 * dropped. This class can currently buffer at most 2MB of data before
 * discarding.
 * 
 * Once open() is called, the output clock is ticking. Do not call open unless
 * you are really ready to read, or the output buffer will overflow and data
 * will be dropped.
 * 
 * Be sure to call close() as soon as you are done, or this class will continue
 * to expect a reader, and overflow messages will result.
 * 
 */
public class MeteredFileInputStream extends InputStream implements Runnable {

    private static Tracer tracer;

    /**
     * Maximum supportable data rate (bps).
     */
    public static final int MAX_RATE = 200000000; // 200 Mbps

    /**
     * Interval between buffer status reports.
     */
    private static final long DEFAULT_BUFFER_REPORT_INTERVAL = 3000;

    /** 
     * Default overflow buffer size.
     * 
     * Rename constant and make it bigger.
     * (5 MB instead of 1 MB) 
     */
    public static final long DEFAULT_OVERFLOW_SIZE = 5 * 1024 * 1024;
    /** 
     * Minimum overflow buffer size.
     * 
     */
    public static final int MIN_OVERFLOW_BUFFER_SIZE = 1024 * 1024;

    private static final int SECOND_BOUNDARY = 1000;
    private static final int HUNDREDTH_BOUNDARY = 10000;
    private static final int TENTH_BOUNDARY = 100000;

    private FileInputStream inputStream;
    private final int bufferSize; // read/write buffer size
    private final long overflowBufferSize;
    private final long timerInterval;
    private final long bufferReportInterval = DEFAULT_BUFFER_REPORT_INTERVAL;
    private long lastBufferReportTime;
    private Timer clock;
    private final LinkedBlockingQueue<byte[]> inputQueue;
    private final LinkedBlockingQueue<byte[]> outputQueue;
    private Thread readThread;
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
    private final AtomicBoolean dataDone = new AtomicBoolean(false);
    private final boolean infiniteSend;
    private final String filename;
    private final boolean verbose;

    /**
     * Constructor.
     * 
     * @param filename
     *            file path of the input file to read
     * @param bitrate
     *            desired output rate, in bps
     * @param infinite
     *            true to loop through the data file until stopped, false to
     *            stop after the file has been read once
     * @param verbose
     *            true to enable verbose logging, false to disable
     * @param trace
     * 			  context trace logger
     */
    public MeteredFileInputStream(final String filename, final long bitrate,
            final boolean infinite, final boolean verbose, Tracer trace) {
    	this(filename, bitrate, infinite, verbose, DEFAULT_OVERFLOW_SIZE, trace);
    }
    
    /**
     * Constructor.
     * 
     * @param filename
     *            file path of the input file to read
     * @param bitrate
     *            desired output rate, in bps
     * @param infinite
     *            true to loop through the data file until stopped, false to
     *            stop after the file has been read once
     * @param verbose
     *            true to enable verbose logging, false to disable
     * @param overflowBuffer size of the overflow buffer in bytes
     * @param trace
     * 			  context trace logger
     */
    public MeteredFileInputStream(final String filename, final long bitrate,
            final boolean infinite, final boolean verbose, final long overflowBuffer, Tracer trace) {

        if (bitrate > MAX_RATE) {
            throw new IllegalArgumentException(
                    "This stream cannot support a bitrate of more than "
                            + MAX_RATE);
        }
        
        if (overflowBuffer < MIN_OVERFLOW_BUFFER_SIZE){
        	throw new IllegalArgumentException(
                    "This stream cannot support a buffer size of less than "
                            + MIN_OVERFLOW_BUFFER_SIZE);
        }
        
        this.infiniteSend = infinite;
        this.filename = filename;
        this.verbose = verbose;
        this.overflowBufferSize = overflowBuffer;
        
        tracer = trace;

        /*
         * Establish buffer size and clock rate based upon bitrate. This fairly
         * dumb algorithm attempts to balance buffer size with clock rate to
         * produce something reasonable. The clock will tick at the rate of 1,
         * 10, 100, or 1000 ticks per second based upon the data rate.
         */
        final long byteRate = bitrate / 8;
        double ticksPerSec = 0;

        if (byteRate <= SECOND_BOUNDARY) {
            ticksPerSec = 1;
        } else if (byteRate <= TENTH_BOUNDARY) {
            ticksPerSec = 10;
        } else if (byteRate <= HUNDREDTH_BOUNDARY) {
            ticksPerSec = 100;
        } else {
            ticksPerSec = 1000;
        }
        
        this.bufferSize = (int) Math.round(byteRate / ticksPerSec);
        this.timerInterval = Math.round(1000.0 / ticksPerSec);
        this.inputQueue = new LinkedBlockingQueue<byte[]>((int)(overflowBufferSize
                / this.bufferSize));
        this.outputQueue = new LinkedBlockingQueue<byte[]>((int)(overflowBufferSize
                / this.bufferSize));

    }

    /**
     * Returns the output clock interval.
     * 
     * @return clock interval in milliseconds.
     */
    public long getClockInterval() {

        return this.timerInterval;
    }

    /**
     * Returns the input/output buffer size being used by this input stream.
     * This is how much it reads or writes at one time.
     * 
     * @return buffer size in bytes
     */
    public int getReadBufferSize() {

        return this.bufferSize;
    }

    /**
     * Returns the overflow buffer size being used by this input stream.
     * This is how much it can queue to the output queue before dropping data.
     * 
     * @return buffer size in bytes
     * 
     */
    public long getOverflowBufferSize() {

    	return this.overflowBufferSize;
    }

    /**
     * Reads from the input stream and returns a byte buffer, as opposed to
     * reading into a buffer supplied by the caller. This buffer is guaranteed
     * to now belong to the caller, and will not be overwritten by this class.
     * Unless at end of file, this method will block until data is available or
     * this FileMeteredInputStream is closed.
     * 
     * @return byte array, which will be empty if at end of file
     */
    public synchronized byte[] readNoBuffer() {

        if (this.inputQueue.isEmpty() && this.outputQueue.isEmpty()
                && this.dataDone.get()) {
            return new byte[0];
        }

        /*
         * Try to read from the output queue until we get data or a shutdown
         * request.
         */
        while (!this.shutdownRequested.get()) {
            try {
                final byte[] data = this.outputQueue.poll(1000,
                        TimeUnit.MILLISECONDS);
                if (data != null) {
                    return data;
                }
            } catch (final InterruptedException e) {
                SystemUtilities.doNothing();
            }
        }
        return new byte[0];

    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#read(byte[], int, int)
     * 
     * @throws IllegalArgumentException
     *             if the input buffer or requested length is shorter than the
     *             read buffer size in this object.
     */
    @Override
    public synchronized int read(final byte[] buffer, final int off, final int len) 
            throws IllegalArgumentException {

        final byte[] tempBuff = readNoBuffer();
        if (tempBuff == null) {
            return -1;
        } else {
            if (buffer.length < tempBuff.length) {
                throw new IllegalArgumentException(
                        "Input buffer must be at least " + this.bufferSize
                                + " bytes in size to read from this stream");
            }
            if (len < tempBuff.length) {
                throw new IllegalArgumentException("Can only read at least "
                        + tempBuff.length
                        + " bytes from this stream at this time");
            }
            System.arraycopy(tempBuff, 0, buffer, off, len);
            return tempBuff.length;
        }

    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#read()
     * @throws UnsupportedOperationException
     *             always
     */
    @Override
    public int read() throws IOException, UnsupportedOperationException {

        throw new UnsupportedOperationException(
                "Byte-wise read is not supported by this stream type");
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#available()
     * 
     * @throws IllegalStateException
     *             if the available bytes exceeds the maximum value
     *             representable as an int (which can happen).
     */
    @Override
    public int available() throws IllegalStateException {

        long totalBytes = 0;
        for (final byte[] buff : this.outputQueue) {
            totalBytes += buff.length;
        }
        if (totalBytes > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "Current length of the output buffer does not fit in an int");
        }
        return (int) totalBytes;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#markSupported()
     */
    @Override
    public boolean markSupported() {

        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#mark(int)
     * @throws UnsupportedOperationException
     *             always
     */
    @Override
    public void mark(final int limit) throws UnsupportedOperationException {

        throw new UnsupportedOperationException(
                "Mark is not supported by this stream type");
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#reset()
     * @throws UnsupportedOperationException
     *             always
     */
    @Override
    public void reset() throws UnsupportedOperationException {

        throw new UnsupportedOperationException(
                "Reset is not supported by this stream type");
    }

    /**
     * Skips bytes in the output queue. Will block until the specified number of
     * bytes has been skipped or at end of file.
     * 
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#skip(long)
     * @throws IllegalArgumentException
     *             if input limit is not evenly divisible by buffer size.
     */
    @Override
    public long skip(final long limit) throws IllegalArgumentException {

        if (limit < 0 || limit % this.bufferSize != 0) {
            throw new IllegalArgumentException(
                    "Number of bytes to skip must be a multiple of the read buffer size for this stream type");
        }
        if (limit == 0) {
            return 0;
        }
        long totalBytes = 0;
        for (long loops = (limit % this.bufferSize); loops > 0; loops--) {
            final byte[] temp = readNoBuffer();
            if (temp == null) {
                break;
            }
            totalBytes += temp.length;
        }
        return totalBytes;
    }

    /**
     * Opens the input stream. This starts reading the file content into the
     * input buffer and starts the output clock.
     * 
     * @throws IOException
     *             if there is a problem opening the stream
     */
    public synchronized void open() throws IOException {

        this.shutdownRequested.set(false);
        this.dataDone.set(false);
        this.inputStream = new FileInputStream(this.filename);
        this.readThread = new Thread(this);
        this.readThread.start();
        startOutputClock();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#close()
     */
    @Override
    public synchronized void close() {

        /*
         * First stop the file input thread and wait for it to obey.
         */
    	this.shutdownRequested.set(true);
    	if (this.readThread != null) {
    		while (!this.dataDone.get()) {
    			try {
    				Thread.sleep(250);   

    			} catch (final InterruptedException e) {
    				SystemUtilities.doNothing();
    			}
    		}
    		this.readThread = null;
    	}

    	/*
         * Now stop the output clock.
         */
        stopOutputClock();

        /*
         * Close the input stream.
         */
        if (this.inputStream != null) {
            try {
                this.inputStream.close();
            } catch (final IOException e) {
                SystemUtilities.doNothing();
            }
            this.inputStream = null;
        }

        /*
         * Throw out all the queued data.
         */
        this.inputQueue.clear();
        this.outputQueue.clear();

    }

    /**
     * Starts the output clock. This controls data movement between the input
     * and output queues.
     */
    private void startOutputClock() {

        this.clock = new Timer();

        this.clock.scheduleAtFixedRate(new TimerTask() {

            /**
             * {@inheritDoc}
             * 
             * @see java.util.TimerTask#run()
             */
            @Override
            public void run() {
            	
            	/* Add buffer status reporting in
            	 * verbose mode.
            	 */
            	long now = System.currentTimeMillis();
            	
            	if (verbose && (now - lastBufferReportTime) > bufferReportInterval) {
            		tracer.info("Current output buffer size is " + String.valueOf(outputQueue.size() * bufferSize) + " bytes");
            		lastBufferReportTime = now;
            	}

                byte[] toWrite = null;

                /*
                 * Poll a buffer from the input queue. Block until data is
                 * received or shutdown is requested.
                 */
                while (toWrite == null && !MeteredFileInputStream.this.shutdownRequested.get()) {

                    try {
                        toWrite = MeteredFileInputStream.this.inputQueue.poll(1000, TimeUnit.MILLISECONDS);
                    } catch (final InterruptedException e) {
                        SystemUtilities.doNothing();
                    }
                    if (toWrite == null) {
                        /*
                         * If we did not get data because the input file is
                         * done, give up.
                         */
                        if (MeteredFileInputStream.this.dataDone.get()) {
                            break;
                        } else {
                            tracer.warn("File input is not keeping up with clock rate");
                        }
                    }
                }
                /*
                 * Write the buffer to the output queue. If the output queue is
                 * full, the data sink pulling from the output queue is too
                 * slow, and we drop the data in this buffer.
                 */
                if (toWrite != null) {

                	/* Enhance the error message issued upon failure.*/

                    final boolean toClient = MeteredFileInputStream.this.outputQueue.offer(toWrite);
                    if (!toClient) {
                        tracer.error("Buffer output size is " + String.valueOf(outputQueue.size()) + ". Data sink is too slow. Discarding "
                                + toWrite.length + " bytes of data.");
                    }
                }

            }
        }, 0, this.timerInterval);
    }

    /**
     * Stops the output clock. No more data will be moved between the input and
     * output queues.
     */
    private void stopOutputClock() {

        if (this.clock != null) {
            this.clock.purge();
            this.clock.cancel();
            this.clock = null;
        }
    }

    /**
     * This is the method that reads from the input file and continually loads
     * the input queue. It should be executed on its own thread.
     * 
     * {@inheritDoc}
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {

        try {
            long totalBytes = 0;
            /*
             * Open the input file.
             */
            this.inputStream = new FileInputStream(this.filename);

            /*
             * Loop until out of input data or requested to shutdown.
             */
            while (!this.shutdownRequested.get() && !this.dataDone.get()) {

                /*
                 * We create a new buffer every time, because the buffer is put
                 * into the input queue and cannot be re-used.
                 */
                byte[] buffer = new byte[this.bufferSize];

                /*
                 * Read the first buffer.
                 */
                int bytesRead = this.inputStream.read(buffer);
                totalBytes = bytesRead;

                /*
                 * Loop until end of file or shutdown requested.
                 */
                while (bytesRead != -1 && !this.shutdownRequested.get()) {

                    /*
                     * Usually the buffer is the same size. Account for the case
                     * where there is not enough data to fill the buffer by
                     * copying to a smaller buffer.
                     */
                    if (bytesRead < this.bufferSize) {
                        final byte[] tempBuffer = new byte[bytesRead];
                        System.arraycopy(buffer, 0, tempBuffer, 0, bytesRead);
                        buffer = tempBuffer;
                    }
                    /*
                     * Put the buffer just read into the input queue. Block
                     * until this is successful.
                     */
                    try {
                        boolean accepted = false;
                        while (!accepted && !this.shutdownRequested.get()) {
                            accepted = this.inputQueue.offer(buffer);
                            if (!accepted) {
                            	// change to debug. condition is logged elsewhere
                                tracer.debug("File input is blocked. Input buffer is full.");
                            }
                        }
                    } catch (final Exception e) {
                        /*
                         * We may have been interrupted to force shutdown.
                         */
                        if (this.shutdownRequested.get()) {
                            break;
                        }
                    }

                    /*
                     * Next read.
                     */
                    buffer = new byte[this.bufferSize];
                    bytesRead = this.inputStream.read(buffer);
                    totalBytes += bytesRead;
                }

                if (this.verbose) {
                    tracer.info("Data file read is done. Total of "
                            + totalBytes + " bytes read");
                }
                
                /*
                 * At end of file. Close the input stream.
                 */
                this.inputStream.close();
                
                /*
                 * If running in infinite send mode, we have to close and
                 * re-open the input file. Otherwise, we set data done to true.
                 * 
                 * Add check for shutdown flag.
                 */
                if (this.infiniteSend && !shutdownRequested.get()) {
                    if (this.verbose) {
                        tracer.info("(Re)starting transfer of file "
                                + this.filename);
                    }
                    /*
                     * Re-open the input stream.
                     */
                    this.inputStream = new FileInputStream(this.filename);
                } else {
                	this.dataDone.set(true);
                }
            }

        } catch (final IOException e) {
            e.printStackTrace();
            tracer.error("IO exception reading from input file: "
                    + e.toString());
        }

        /*
         * Notify other objects that we are done reading input data.
         */
        this.dataDone.set(true);

    }

}