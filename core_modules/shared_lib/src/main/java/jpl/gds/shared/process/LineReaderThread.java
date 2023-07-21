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
package jpl.gds.shared.process;

import static jpl.gds.shared.exceptions.ExceptionTools.rollUpMessages;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import jpl.gds.shared.exceptions.ExcessiveInterruptException;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.thread.SleepUtilities.FullWait;

/**
 * Threaded line reader which reads in lines from a file.
 * 
 */
public class LineReaderThread extends Thread {
    /**
     * Trace log.
     */
    protected static final Tracer log       = TraceManager.getTracer(Loggers.UTIL);

    /**
     * Static value representing an unstarted state.
     */
    protected static final int UNSTARTED = 0;
    /**
     * Static value representing a reading state.
     */
    protected static final int READING = 1;
    /**
     * Static value representing a stopped state.
     */
    protected static final int STOPPING = 2;
    /**
     * Static value representing a done state.
     */
    protected static final int DONE = 3;

    /**
     * Current state.
     */
    protected int state = UNSTARTED;
    /**
     * File reader.
     */
    protected BufferedReader reader;
    /**
     * Line handler.
     */
    protected LineHandler lineHandler;

    /**
     * Object for synchronization.
     */
    protected Object done = new Object();

    /**
     * Instantiates the file reader based on an input stream.
     * 
     * @param in
     *            input stream to create a reader from
     */
    public LineReaderThread(final InputStream in) {
        reader = new BufferedReader(new InputStreamReader(in));
    }

    /**
     * Instantiates the file reader based on another reader.
     * 
     * @param reader
     *            to create another reader from
     */
    public LineReaderThread(final Reader reader) {
        if (reader instanceof BufferedReader) {
            this.reader = (BufferedReader) reader;
        } else {
            this.reader = new BufferedReader(reader);
        }
    }

    /**
     * Sets the current line handler to process with.
     * 
     * @param lineHandler
     *            line handler
     */
    public void setLineHandler(final LineHandler lineHandler) {
        this.lineHandler = lineHandler;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        synchronized (done) {
            state = READING;
            try {
                while (state == READING) {
                    readLine();
                }
            } catch (final EOFException done) {
            } catch (final IOException e) {
                if (e.getMessage().indexOf("Stream closed") == -1) {
                    log.error("I/O Error reading line: " + e.getMessage(), e);
                }
            } finally {
                try {
                    reader.close();
                } catch (final IOException e) {
                    // we don't care
                }
            }
            state = DONE;
            done.notifyAll();
        }
    }

    /**
     * Busy wait until finished processing. TODO - remove busy wait design
     * 
     * @return true if the thread was interrupted
     */
    public boolean waitFor() {
        final FullWait fw = new FullWait(done);
        boolean interrupted = false;

        synchronized (done) {
            while (state != DONE) {
                try {
                    interrupted |= fw.fullWait().getInterrupted();
                } catch (final ExcessiveInterruptException eie) {
                    interrupted = true;

                    log.error("LineReaderThread.waitFor Error waiting: "
                            + rollUpMessages(eie));
                    break;
                }
            }
        }

        return interrupted;
    }

    /**
     * Stop running the thread.
     */
    public void stopReading() {
        state = STOPPING;
    }

    /**
     * Reads a line from the file reader.
     * 
     * @throws IOException
     *             thrown if unable to read line
     */
    protected void readLine() throws IOException {
        final String line = reader.readLine();
        if (line == null) {
            state = STOPPING;
        } else if (lineHandler != null) {
            lineHandler.handleLine(line);
        }
    }

}
