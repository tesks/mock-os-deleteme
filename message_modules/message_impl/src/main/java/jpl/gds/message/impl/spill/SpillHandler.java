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

import java.util.LinkedList;

import jpl.gds.message.impl.spill.SpillFile.SpillFileException;
import jpl.gds.message.impl.spill.SpillFileFactory.SpillFileFactoryException;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * Controls spilling and unspilling of messages.
 */
public class SpillHandler {
    /**
     * Only exception thrown by SpillHandler (on purpose, anyway.).
     */
    public static final class SpillHandlerException extends Exception {
        /**
         * Default serial ID for serializable exception.
         */
        private static final long serialVersionUID = 1L;


        /**
         * Constructor.
         *
         * @param message Message text
         */
        public SpillHandlerException(final String message) {
            super(message);
        }


        /**
         * Constructor.
         *
         * @param message Message text
         * @param cause   Underlying cause
         */
        public SpillHandlerException(final String message,
                final Throwable cause) {
            super(message, cause);
        }
    }

    private static final String ME = "SpillHandler";

    // The smallest size we accept for _maximumSize
    private static final int SMALLEST_SIZE = 100;
    // Ordered from oldest to latest
    private final LinkedList<SpillFile> _populated =
            new LinkedList<SpillFile>();
    private final SpillFileFactory _factory;
    private final int _maximumSize;

    private final Tracer _trace;
    private SpillFile _inProgress = null;
    private SpillFile _unspiller = null;

    private int _spilled = 0;

    /**
     * Constructs a SpillHandler.
     * @param factory Spill factory used to create spill files.
     * @param maximumSize Maxmium size of the spill factory.
     * @param trace Custom tracer, defaults to JmsFastTracer.
     * @throws SpillHandlerException Thrown if an invalid factory is passed in
     *             as an argument.
     */
    public SpillHandler(final SpillFileFactory factory, final int maximumSize,
            final Tracer trace) throws SpillHandlerException {
        super();

        if (factory == null) {
            throw new SpillHandlerException("Error constructing: null factory");
        }

        this._factory = factory;
        this._maximumSize = Math.max(maximumSize, SMALLEST_SIZE);
        this._trace = (trace != null) ? trace : TraceManager.getTracer(Loggers.JMS);
    }

    /**
     * Returns the number of spilled records so far.
     * @return number of spilled records
     */
    public int getSpilled() {
        return this._spilled;
    }

    /**
     * Returns number of spilled records remaining.
     * @return number of remaining spill records
     */
    public int size() {
        int sum =
                ((this._inProgress != null) ? this._inProgress.size() : 0)
                        + ((this._unspiller != null) ? this._unspiller.size()
                                : 0);

        for (final SpillFile sf : this._populated) {
            sum += sf.size();
        }

        return sum;
    }

    /**
     * Spill a message.
     * @param message Message to spill.
     * @throws SpillHandlerException Thrown if there is an error spilling the
     *             message.
     */
    public void spill(final byte[] message) throws SpillHandlerException {
        if (this._inProgress == null) {
            try {
                this._inProgress = this._factory.getSpillFile();
            } catch (final SpillFileFactoryException sffe) {
                throw new SpillHandlerException(
                    "Could not create in-progress spill file", sffe);
            }
        }

        try {
            this._inProgress.writeRecord(message);

            this._trace.trace(ME , " Spilling record to in-progress");
        } catch (final SpillFileException sfe) {
            _inProgress = null;

            throw new SpillHandlerException("Could not spill", sfe);
        }

        if (this._inProgress.getCount() >= this._maximumSize) {
            // This guy is full

            try {
                this._inProgress.closeForWriting();

                this._populated.addLast(this._inProgress);

                this._trace.trace(ME , " Promoting in-progress file " ,
                    _inProgress);
            } catch (final SpillFileException sfe) {
                throw new SpillHandlerException(
                    "Could not promote in-progress spill file "
                            + "to populated", sfe);
            } finally {
                this._inProgress = null;
            }
        }

        ++this._spilled;
    }

    /**
     * Return true if we are spilling.
     * @return if spilling
     */
    public boolean spilling() {
        return !this._populated.isEmpty()
                || ((this._inProgress != null) && !this._inProgress.atEOF())
                || ((this._unspiller != null) && !this._unspiller.atEOF());
    }

    /**
     * Unspill a message, or return null if none.
     * @return Message or null.
     * @throws SpillHandlerException Thrown if an error occurs.
     */
    public byte[] unspill() throws SpillHandlerException {
        if (this._unspiller == null) {
            if (!this._populated.isEmpty()) {
                // Take the oldest populated spill file

                try {
                    this._unspiller = this._populated.removeFirst();

                    this._unspiller.openForReading();

                    this._trace.trace(ME,
                        " Demoting populated to unspiller ", _unspiller);
                } catch (final SpillFileException sfe) {
                    _unspiller = null;

                    throw new SpillHandlerException(
                        "Could not demote populated spill file "
                                + "to unspiller", sfe);
                }
            } else if ((this._inProgress == null) || this._inProgress.atEOF()) {
                return null;
            } else {
                // Must take non-empty but non-full in-progress spill file

                try {
                    this._inProgress.reopenForReading();

                    this._unspiller = this._inProgress;

                    this._trace.trace(ME, " Demoting in-progress to unspiller ", _unspiller);
                } catch (final SpillFileException sfe) {
                    throw new SpillHandlerException(
                        "Could not demote in-progress spill file "
                                + "to unspiller", sfe);
                } finally {
                    this._inProgress = null;
                }
            }
        }

        byte[] result = null;

        try {
            result = this._unspiller.readRecord();

            this._trace.trace(ME, " Unspilling record from unspiller");
        } catch (final SpillFileException sfe) {
            _unspiller = null;

            throw new SpillHandlerException("Could not unspill", sfe);
        }

        if (this._unspiller.atEOF()) {
            // This guy is no longer needed

            try {
                this._factory.shelve(this._unspiller);

                this._trace.trace(ME, " Shelving unspiller ", _unspiller);
            } catch (final SpillFileFactoryException sffe) {
                throw new SpillHandlerException("Could not shelve", sffe);
            } finally {
                this._unspiller = null;
            }
        }

        return result;
    }
}
