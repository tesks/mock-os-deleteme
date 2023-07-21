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
package jpl.gds.db.api.sql.store.ldi;

import java.util.concurrent.atomic.AtomicReference;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.shared.log.ILogMessage;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * Interface for LogMessage LDI Store
 */
public interface ILogMessageLDIStore extends ILDIStore {
    /**
     * Store Identification for LDI Inserters
     */
    public static final StoreIdentifier STORE_IDENTIFIER = StoreIdentifier.LogMessage;
    
    /**
     * Database table fields as CSV.
     */
    String DB_LOG_MESSAGE_DATA_FIELDS = SESSION_ID + "," + HOST_ID + "," + FRAGMENT_ID + "," + "rctCoarse" + "," + "rctFine" + ","
            + "eventTimeCoarse" + "," + "eventTimeFine" + "," + "classification" + "," + "message" + "," + "type,"
            + CONTEXT_ID + "," + CONTEXT_HOST_ID;

    /**
     * Initial value of null can be reset to any string. Constructor will
     * populate if it is null. Set to empty string to disable, not to null.
     */
    AtomicReference<String> MSG_PREFIX_FOR_SESSION =
        new AtomicReference<String>(null);


    /**
     * Set message prefix for logging.
     *
     * @param prefix
     *            String prefix
     */
    public static void setMsgPrefixForSession(final String prefix) {
        if (prefix != null) {
            MSG_PREFIX_FOR_SESSION.set(prefix);
        }
    }

    /**
     * Insert a log message into the database
     *
     * @param elm
     *            The external log message
     *
     * @throws DatabaseException
     *             SQL exception
     */
    void insertLogMessage(ILogMessage elm) throws DatabaseException;

    /**
     * Insert a log message into the database
     *
     * @param eventTime
     *            The time the message was issued
     * @param classification
     *            The trace severity
     * @param message
     *            The message text
     * @param type
     *            The external log type
     *
     * @throws DatabaseException
     *             SQL exception
     */
    void insertLogMessage(IAccurateDateTime eventTime, TraceSeverity classification, String message,
                          LogMessageType type)
            throws DatabaseException;

    /**
     * Receive a log message from the internal message bus and insert the
     * information into the database
     *
     * @param message
     *            The external log message received on the internal bus
     */
    public void handleLogMessage(final ILogMessage message);

}