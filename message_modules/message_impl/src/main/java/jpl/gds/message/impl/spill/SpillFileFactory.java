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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import jpl.gds.message.impl.spill.SpillFile.SpillFileException;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * Factory to create spill files. We keep them on a list and reuse them.
 */
public class SpillFileFactory extends Object {
    /**
     * Only exception thrown by SpillFileFactory (on purpose, anyway.).
     */
    public static final class SpillFileFactoryException extends Exception {
        /**
         * Default serial ID for serializable exception.
         */
        private static final long serialVersionUID = 1L;


        /**
         * Constructor.
         *
         * @param message Message text
         */
        public SpillFileFactoryException(final String message) {
            super(message);
        }


        /**
         * Constructor.
         *
         * @param message Message text
         * @param cause   Underlying cause
         */
        public SpillFileFactoryException(final String message,
                final Throwable cause) {
            super(message, cause);
        }


        /**
         * Constructor.
         *
         * @param cause Underlying cause
         */
        public SpillFileFactoryException(final Throwable cause) {
            super(cause);
        }
    }

    private static final String ME = "SpillFileFactory";
    private static final String PREFIX = "sfp_"; // At least three characters

    private static final String SUFFIX = ".sf";
    private final File _path;
    private final String _name;
    private final Set<SpillFile> _closed = new HashSet<SpillFile>();
    private final boolean _keep;

    private final Tracer _trace;

    /**
     * Create a factory for spill files with a parent directory.
     * 
     * @param path
     *            Parent directory
     * @param keep
     *            Whether to keep the spill file.
     * @param trace
     *            Custom tracer, defaults to JmsDebugTracer.
     * @throws SpillFileFactoryException
     *             Thrown if directory path is not valid.
     */
    public SpillFileFactory(final File path, final boolean keep,
            final Tracer trace) throws SpillFileFactoryException {
        super();

        if (path == null) {
            throw new SpillFileFactoryException(
                    "Error constructing: null directory path");
        }

        this._path = path;
        /*
         * We want to to delete the SFP sub-directory on exit if it is empty.
         * This seems to do exactly that and does not generate an error if
         * the directory is NOT empty.
         */
        this._path.deleteOnExit();
        this._name = "SpillFileFactory:" + this._path.getAbsolutePath();
        this._trace = (trace != null) ? trace : TraceManager.getTracer(Loggers.JMS);
        this._keep = keep;

        this._trace.trace(ME , " Opening factory on " , this._name);
    }

    /**
     * Get rid of a spill file.
     * 
     * @param sf
     *            Spill file to remove.
     */
    public void delete(final SpillFile sf) {
        if (sf == null) {
            return;
        }

        this._closed.remove(sf);

        if (!this._keep) {
            sf.delete();

            this._trace.trace(ME, ".delete Deleting ", sf);
        }
    }

    /**
     * Get rid of all closed spill files.
     */
    public void deleteClosed() {
        if (!this._keep) {
            for (final SpillFile sf : this._closed) {
                sf.delete();

                this._trace.trace(ME, ".deleteClosed Deleting ", sf);
            }
        }

        this._closed.clear();
    }

    /**
     * Returns the spill file.
     * @return SpillFile Returns a spill file.
     * @throws SpillFileFactoryException
     *             Thrown if unable to open a spill file.
     */
    public SpillFile getSpillFile() throws SpillFileFactoryException {
        if (!this._keep && !this._closed.isEmpty()) {
            // Reuse an old one

            final Iterator<SpillFile> it = this._closed.iterator();
            final SpillFile result = it.next();

            it.remove();

            try {
                result.reopenForWriting();
            } catch (final SpillFileException sfe) {
                throw new SpillFileFactoryException(sfe);
            }

            this._trace.trace(ME, ".getSpillFile Reusing ", result);

            return result;
        }

        // Create a new one

        File tf = null;

        try {
            tf = File.createTempFile(PREFIX, SUFFIX, this._path);
        } catch (final IOException ioe) {
            throw new SpillFileFactoryException(
                    "Unable to create temporary file", ioe);
        }

        SpillFile result = null;

        try {
            result = new SpillFile(tf, this._trace);
        } catch (final SpillFileException sfe) {
            if (!tf.delete()) {
                this._trace.error(ME, ".getSpillFile Unable to delete '",
                        this._name, "'" , ExceptionTools.getMessage(sfe), sfe);
            }

            throw new SpillFileFactoryException(sfe);
        }

        this._trace.trace(ME, ".getSpillFile Creating ", result);

        return result;
    }

    /**
     * Close and save for reuse.
     * 
     * @param sf
     *            Spill file to save and reuse later.
     * @throws SpillFileFactoryException
     *             Thrown if unable to shelve spill file.
     */
    public void shelve(final SpillFile sf) throws SpillFileFactoryException {
        if ((sf == null) || this._closed.contains(sf)) {
            return;
        }

        try {
            sf.close();
        } catch (final SpillFileException sfe) {
            throw new SpillFileFactoryException(sfe);
        }

        this._closed.add(sf);

        this._trace.trace(ME, ".shelve Shelving ", sf);
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this._name;
    }
}
