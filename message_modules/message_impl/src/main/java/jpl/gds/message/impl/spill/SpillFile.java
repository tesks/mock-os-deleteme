/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.message.impl.spill;

import static jpl.gds.shared.exceptions.ExceptionTools.rollUpMessages;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * Random-access file consisting of records. Each record is an opaque byte array
 * prepended by a byte count. We close the raf when we are done writing, and
 * reopen it when we are ready for reading, so as not to hog the open file
 * descriptors. Normally, you use a SpillFileFactory to create and delete spill
 * files.
 */
public class SpillFile {
    /**
     * Only exception thrown by SpillFile (on purpose, anyway.).
     */
    public static final class SpillFileException extends Exception {
        /**
         * Default serial for serializable id.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Constructor initialized with an exception message.
         * @param message Message of the exception.
         */
        public SpillFileException(final String message) {
            super(message);
        }

        /**
         * Constructor initialized with an exception message and originating
         * exception.
         * @param message Message of the exception.
         * @param cause Exception to pass along.
         */
        public SpillFileException(final String message,
                final Throwable cause) {
            super(message, cause);
        }
    }

    private static final String ME = "SpillFile";

    // Set true to truncate reused files to zero-length
    private static final boolean TRUNCATE = false;

    private static final int TRY_OPEN = 10;
    private static final byte[] _empty = new byte[0];

    private static final int _flag = -1;

    /**
     * Try multiple times to open random-access file.
     * @param path Path to random access file.
     * @param mode Mode to open the file in.
     * @return New random access file.
     * @throws FileNotFoundException Thrown if unable to create the random
     *      access file.
     */
    private static RandomAccessFile openRAF(final File path, final String mode)
            throws FileNotFoundException {
        FileNotFoundException exc = null;

        for (int i = 0; i < TRY_OPEN; ++i) {
            try {
                return new RandomAccessFile(path, mode);
            } catch (final FileNotFoundException fnfe) {
                exc = fnfe;
            }
        }

        throw exc;
    }

    private final File _path;

    private final String _name;
    private final Tracer _trace;
    private RandomAccessFile _raf = null;
    private int _count = 0;
    private int _next = 0;

    private boolean _writing = true;

    private boolean _reading = false;

    /**
     * Open a random-access file for use as a spill-file. It starts in write
     * mode.
     * @param path File to write to.
     * @param trace Custom tracer, defaults to JmsDebugTracer.
     * @throws SpillFileException Thrown if unable to write to the spill file.
     */
    public SpillFile(final File path, final Tracer trace)
            throws SpillFileException {
        super();

        if (path == null) {
            throw new SpillFileException("Error constructing: null path");
        }

        this._path = path;
        this._name = this._path.getAbsolutePath();
        this._trace = (trace != null) ? trace : TraceManager.getTracer(Loggers.JMS);

        try {
            this._raf = openRAF(this._path, "rw");
        } catch (final FileNotFoundException fnfe) {
            throw new SpillFileException("Error constructing: " + this._name,
                fnfe);
        }
    }

    /**
     * Returns true if there are no more records to read.
     * @return if at end of file
     */
    public boolean atEOF() {
        return (getNext() == getCount());
    }

    /**
     * Close underlying random-access file.
     * @throws SpillFileException Thrown if unable to close the spill file.
     */
    public void close() throws SpillFileException {
        this._writing = false;
        this._reading = false;

        try {
            if (this._raf != null) {
                this._raf.close();
            }
        } catch (final IOException ioe) {
            throw new SpillFileException("Error closing: " + this._name, ioe);
        } finally {
            this._raf = null;
        }
    }

    /**
     * Prohibit further writes and close internal random-access file.
     * @throws SpillFileException Thrown if unable to close for writing.
     */
    public void closeForWriting() throws SpillFileException {
        try {
            if (this._reading) {
                throw new SpillFileException("Error in closeForWriting, mode "
                        + "is 'reading': " + this._name);
            }

            if (!this._writing) {
                throw new SpillFileException("Error in closeForWriting, mode "
                        + "is 'not writing': " + this._name);
            }
        } finally {
            close();
        }
    }

    /**
     * Close and delete underlying random-access file. Don't throw, because it
     * isn't a big deal if the file cannot be closed or deleted.
     */
    public void delete() {
        try {
            close();
        } catch (final SpillFileException sfe) {
            this._trace.error(ME, ".delete Error closing ", this._name,
                ": ", rollUpMessages(sfe), ExceptionTools.getMessage(sfe), sfe);
        } finally {
            if (!this._path.delete()) {
                this._trace.error(ME, ".delete Error deleting: ", this._name);
            }
        }
    }

    /**
     * Return the number of records written.
     * @return count of written records
     */
    public int getCount() {
        assert this._count >= 0;
        assert this._next >= 0;
        assert this._next <= this._count;

        return this._count;
    }

    /**
     * Return next index to be read.
     * @return next index number
     */
    public int getNext() {
        assert this._count >= 0;
        assert this._next >= 0;
        assert this._next <= this._count;

        return this._next;
    }

    /**
     * Open random-access file and set up for reading from the top.
     * @throws SpillFileException Thrown if unable open for reading.
     */
    public void openForReading() throws SpillFileException {
        if (this._reading) {
            throw new SpillFileException("Error in openForReading, mode "
                    + "is 'reading': " + this._name);
        }

        if (this._writing) {
            throw new SpillFileException("Error in openForReading, mode "
                    + "is 'writing': " + this._name);
        }

        try {
            this._raf = openRAF(this._path, "r");
        } catch (final FileNotFoundException fnfe) {
            throw new SpillFileException("Error reopening: "
                + this._name, fnfe);
        }

        this._reading = true;
        this._next = 0;
    }

    /**
     * Read a record from the current position. Note that we cannot rely on the
     * EOFException because we are reusing the files, and the physical length
     * may be beyond the logical length.
     * @return Record as byte array
     * @throws SpillFileException Thrown if an error occurs with the spillfile.
     */
    public byte[] readRecord() throws SpillFileException {
        if (!this._reading) {
            throw new SpillFileException("Closed for reading: " + this._name);
        }

        if (this._next >= this._count) {
            throw new SpillFileException("EOF reading: " + this._name);
        }

        try {
            final int flag = this._raf.readInt();
            final int length = this._raf.readInt();
            byte[] result = _empty;

            if (length > 0) {
                result = new byte[length];

                this._raf.readFully(result);
            }

            ++this._next;

            if (flag != _flag) {
                throw new SpillFileException("Error reading flag: "
                        + this._name);
            }

            return result;
        } catch (final EOFException eofe) {
            throw new SpillFileException("EOF reading: " + this._name, eofe);
        } catch (final IOException ioe) {
            throw new SpillFileException("Error reading: " + this._name, ioe);
        }
    }

    /**
     * Reset random-access file and set up for reading from the top. Note that
     * we do not do a close.
     * @throws SpillFileException Throw if an error occurs with the spillfile.
     */
    public void reopenForReading() throws SpillFileException {
        if (this._reading) {
            throw new SpillFileException("Error in reopenForReading, mode "
                    + "is 'reading': " + this._name);
        }

        if (!this._writing) {
            throw new SpillFileException("Error in reopenForReading, mode "
                    + "is 'not writing': " + this._name);
        }

        try {
            this._raf.seek(0L);
        } catch (final IOException ioe) {
            throw new SpillFileException("Error seeking: " + this._name, ioe);
        }

        this._writing = false;
        this._reading = true;
        this._next = 0;
    }

    /**
     * Reset random-access file and set up for writing from the top. This is
     * done to reuse a spill file.
     * @throws SpillFileException Throw if an error occurs with the spillfile.
     */
    public void reopenForWriting() throws SpillFileException {
        if (this._reading) {
            throw new SpillFileException("Error in reopenForWriting, mode "
                    + "is 'reading': " + this._name);
        }

        if (this._writing) {
            throw new SpillFileException("Error in reopenForWriting, mode "
                    + "is 'writing': " + this._name);
        }

        try {
            this._raf = openRAF(this._path, "rw");
        } catch (final FileNotFoundException fnfe) {
            throw new SpillFileException("Error reopening: "
                + this._name, fnfe);
        }

        this._writing = true;
        this._reading = false;
        this._count = 0;
        this._next = 0;

        if (TRUNCATE) {
            try {
                this._raf.setLength(0L);
            } catch (final IOException ioe) {
                throw new SpillFileException("Error truncating: " + this._name,
                    ioe);
            }
        }
    }

    /**
     * Return number of unprocessed records.
     * @return count of unprocessed records
     */
    public int size() {
        assert this._count >= 0;
        assert this._next >= 0;
        assert this._next <= this._count;

        return (this._count - this._next);
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this._name;
    }

    /**
     * Write a record to the end of the file.
     * @param record Record to write.
     * @throws SpillFileException Throw if an error occurs with the spillfile.
     */
    public void writeRecord(final byte[] record) throws SpillFileException {
        if (!this._writing) {
            throw new SpillFileException("Closed for writing: " + this._name);
        }

        try {
            this._raf.writeInt(_flag);

            if (record != null) {
                this._raf.writeInt(record.length);

                if (record.length > 0) {
                    this._raf.write(record);
                }
            } else {
                this._raf.writeInt(0);
            }

            ++this._count;
        } catch (final IOException ioe) {
            throw new SpillFileException("Error writing: " + this._name, ioe);
        }
    }
}
