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
package jpl.gds.shared.types;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import jpl.gds.shared.log.Tracer;

/**
 * Implements byte stream of file.
 *
 */
public class FileByteStream extends ByteStream {
    private Tracer log;
    private String filename;
    private DataInputStream in;
    private long length;
    private long offset;
    private ByteArraySlice slice = new ByteArraySlice();


    /**
     * Constructor.
     *
     * @param filename File name
     * @param tracer application context Tracer
     *
     * @throws IOException I/O error
     */
    public FileByteStream(String filename, Tracer tracer) throws IOException {
        this(new File(filename), tracer);
    }

    /**
     * Constructor.
     *
     * @param file
     *            File
     * @param tracer
     *            The application context Tracer
     *
     * @throws IOException
     *             I/O error
     */
    public FileByteStream(File file, Tracer tracer) throws IOException {
        this.in = new DataInputStream(
                 new BufferedInputStream(
                     new FileInputStream(file)));
        this.length = file.length();
        this.filename = file.toString();
        this.log = tracer;
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public void reset()
    {
        if (this.in != null)
        {
            try
            {
                this.in.close();

                this.in = null;
            }
            catch (IOException ignore)
            {
                // Make sure it has something to do for FindBugs
                this.in = null;
            }
        }

        this.length = 0;
        this.offset = 0;
        this.slice.array = null;
        this.slice.offset = 0;
        this.slice.length = 0;
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public long getLength() {
        return this.length;
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public long getOffset() {
        return this.offset;
    }


    /**
     * {@inheritDoc}
     **/
    @Override
    public void skip(final long bytes)
    {
        final long desired = this.offset + bytes;

        if (desired > this.length)
        {
            throw new IndexOutOfBoundsException("Tried to skip to byte "
                                                + desired
                                                + " of a "
                                                + this.length
                                                + " byte stream");
        }

        this.offset = desired;

        try
        {
            final int skipped = this.in.skipBytes((int) bytes);

            if (skipped != bytes)
            {
                log.error("Error skipping to byte " + desired);
                this.offset = this.length;
            }
        }
        catch (IOException e)
        {
            log.error("Error skipping to byte " + desired, e);
            this.offset = this.length;
        }
    }

    /**
     * The ByteArraySlice returned is overwritten with each new call.
     * The caller does not own it.
     *
     * {@inheritDoc}
     **/
    @Override
    public ByteArraySlice read(int bytes) {
        if ((this.offset + bytes) > this.length) {
            throw new IndexOutOfBoundsException("Tried to read to byte "
                                                + (this.offset + bytes)
                                                + " of a " + this.length
                                                + " byte stream");
        }

        if ((this.slice.array == null) || (this.slice.array.length < bytes)) {
            this.slice.array = new byte[bytes];
        }

        try {
            this.in.readFully(this.slice.array, 0, bytes);
        }
        catch (IOException e) {
            log.error("Error reading " + bytes + " bytes"
                      + " from offset " + this.offset
                      + " in file " + this.filename, e);
            this.offset = this.length;
            this.slice.offset = 0;
            this.slice.length = 0;
            return this.slice;
        }

        this.offset += bytes;

        this.slice.offset = 0;
        this.slice.length = bytes;
        return this.slice;
    }
}
