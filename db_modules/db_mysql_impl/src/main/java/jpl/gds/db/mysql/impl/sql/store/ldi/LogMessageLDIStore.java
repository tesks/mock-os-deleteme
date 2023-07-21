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
package jpl.gds.db.mysql.impl.sql.store.ldi;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.ldi.ILogMessageLDIStore;
import jpl.gds.db.mysql.impl.sql.fetch.AbstractMySqlFetch;
import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.log.ILogMessage;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.metadata.context.ContextConfigurationType;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeTooLargeException;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.atomic.AtomicLong;


/**
 * This is the database write/storage interface to the LogMessage table in
 * the MPCS database. This class will receive an input log message and write
 * it to the LogMessage table in the database. This is done via LDI.
 * <p>
 * This class is very similar to the equivalent non-LDI class. The fields are
 * dumped into a BytesBuilder instead of a PreparedStatement, and written by
 * means of a table-specific call to the superclass.
 * <p>
 * startResource and stopResource also make table-specific calls to enable or
 * disable processing.
 * <p>
 * WARNING: If you log inside of insertLogMessage, make sure you do not insert
 * the log to the database.
 *
 * <p>
 * MPCS-4884 All warnings and errors generated here must use
 * the logWarningNoDb and logErrorNoDb calls. The problem is that it is possible to
 * get an infinite loop when the message is published. This cannot happen for every
 * warning or error, but the safe thing is to do all of them rather than to try to
 * figure out which ones may loop. StaticLDIStore must also do this.
 */
public class LogMessageLDIStore extends AbstractLDIStore implements ILogMessageLDIStore {
    private static final String SSE_PREFIX = "SSE::";
    private static final String FSW_PREFIX = "FSW::";
    private static final int MESSAGE_LENGTH = 1600;
    private static final int TYPE_LENGTH = 64;

    private static final String EMPTY_CLASSIFICATION = "UNKNOWN";
    private static final String EMPTY_TYPE = LogMessageType.GENERAL.toString();

    private final BytesBuilder _bb = new BytesBuilder();
    private AtomicLong lastMessageTime = new AtomicLong();


    /**
     * Creates an instance of LogMessageLDIStore.
     *
     * @param appContext The information about the current test session
     */
    public LogMessageLDIStore(final ApplicationContext appContext) {
        /*
         * MPCS-7135 - Add second argument to indicate this store
         * does not operate asynchronously.
         */
        super(appContext, ILogMessageLDIStore.STORE_IDENTIFIER, false);

        // Set initially if it hasn't been set already
        synchronized (LogMessageLDIStore.class) {
            if (MSG_PREFIX_FOR_SESSION.get() == null) {
                MSG_PREFIX_FOR_SESSION.set(sseFlag.isApplicationSse() ? SSE_PREFIX
                        : FSW_PREFIX);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * jpl.gds.db.api.sql.store.ldi.ILogMessageLDIStore#insertLogMessage(jpl.gds
     * .shared.log.ILogMessage)
     */
    @Override
    public void insertLogMessage(final ILogMessage elm)
            throws DatabaseException {

        if (!dbProperties.getUseDatabase() || isStoreStopped.get()) {
            return;
        }
        insertLogMessage(elm.getEventTime(),
                elm.getSeverity(),
                elm.getMessage(),
                elm.getLogType());
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * jpl.gds.db.api.sql.store.ldi.ILogMessageLDIStore#insertLogMessage(java.
     * util.Date, jpl.gds.shared.log.TraceSeverity, java.lang.String,
     * jpl.gds.shared.log.LogMessageType)
     */
    @Override
    public void insertLogMessage(final IAccurateDateTime eventTime,
                                 final TraceSeverity classification,
                                 final String message,
                                 final LogMessageType type)
            throws DatabaseException {
        if (!dbProperties.getUseDatabase() || isStoreStopped.get()) {
            return;
        }

        IContextKey contextKey = contextConfig.getContextId().getContextKey();
        final Long number = contextKey.getNumber();

        Long sessionId;
        Long contextId;

        Integer hostId = null;
        Integer contextHostId = null;

        //case 1) Session  - get number and set as sessionId, hostId in log message
        if (contextKey.getType() == ContextConfigurationType.SESSION) {
            sessionId = number;
            contextId = null;
            hostId = contextKey.getHostId();
        }
        //case 2) Context - get number and set as contextId, contextHostId in log message
        else {
            sessionId = null;
            contextId = number;
            contextHostId = contextKey.getHostId();
        }

        //case 3) Session with parent context - set number as sessionId, parent as contextId
        final Long parentNumber = contextKey.getParentNumber();
        if (parentNumber != null) {
            sessionId = number;
            contextId = parentNumber;
            hostId = contextKey.getHostId();
            contextHostId = contextKey.getParentHostId();
        }

        if (sessionId != null && sessionId < 0L) {
            throw new DatabaseException("LogMessage.sessionId cannot be negative");
        }

        if (contextId != null && contextId < 0L) {
            throw new DatabaseException("LogMessage.contextId cannot be negative");
        }

        if (hostId != null && hostId < 0) {
            throw new DatabaseException("LogMessage.hostId cannot be negative");
        }

        if (contextHostId != null && contextHostId < 0) {
            throw new DatabaseException("LogMessage.contextHostId cannot be negative");
        }

        try {
            synchronized (this) {
                if (!archiveController.isUp()) {
                    throw new IllegalStateException("This connection has already been closed");
                }

                _bb.clear();

                // Format as a line for LDI

                insertValOrNull(_bb, sessionId);
                _bb.insertSeparator();

                insertValOrNull(_bb, hostId);
                _bb.insertSeparator();

                _bb.insert(contextConfig.getContextId().getFragment());
                _bb.insertSeparator();

                final IAccurateDateTime rct = new AccurateDateTime();

                try {
                    _bb.insertDateAsCoarseFineSeparate(rct);
                } catch (final TimeTooLargeException ttle) {
                    trace.warn(dateExceedsWarning("LogMessage.rct", null, rct));
                }

                try {
                    _bb.insertDateAsCoarseFineSeparate(eventTime);
                } catch (final TimeTooLargeException ttle) {
                    trace.warn(dateExceedsWarning("LogMessage.eventTime", null, eventTime));
                }

                /** MPCS-5153 */
                _bb.insertTextComplainReplace(
                        (classification != null)
                                ? classification.getValueAsString()
                                : EMPTY_CLASSIFICATION);

                _bb.insertSeparator();

                // Prefix must be combined with message so we can do the
                // length check properly.

                final StringBuilder sb = new StringBuilder();

                final String prefix = MSG_PREFIX_FOR_SESSION.get();

                if (prefix != null) {
                    sb.append(prefix);
                }

                if (message != null) {
                    sb.append(message);
                }

                /** MPCS-5153  */
                _bb.insertTextAllowReplace(
                        checkLength("LogMessage.message",
                                MESSAGE_LENGTH,
                                sb.toString().replaceAll("[\r\n]{1,}", " ")));

                _bb.insertSeparator();

                /** MPCS-5153 */
                _bb.insertTextComplainReplace(
                        (type != null)
                                ? checkLength("LogMessage.type",
                                TYPE_LENGTH,
                                type.getValueAsString())
                                : EMPTY_TYPE);
                _bb.insertSeparator();

                insertValOrNull(_bb, contextId);
                _bb.insertSeparator();

                insertValOrNull(_bb, contextHostId);
                _bb.insertTerminator();

                // Add the line to the LDI batch
                writeToStream(_bb);
            }
        } catch (final RuntimeException re) {
            throw re;
        } catch (final Exception e) {
            final DatabaseException de = new DatabaseException(
                    "Error inserting LogMessage record into database: " +
                            e);
            de.initCause(e);
            throw de;
        }
    }

    @Override
    protected void startResource() {
        super.startResource();
        handler = new BaseMessageHandler() {
            @Override
            public synchronized void handleMessage(final IMessage m) {
                handleLogMessage((ILogMessage) m);
            }
        };
    }

    @Override
    protected void stopResource() {
        // check for idleness before shutting down. this attempts to ensure that log messages queued asynchronously in
        // the logging implementation are given enough time to flow through to disk.
        if (dbProperties.getLogLdiShutdownIdleCheck()) {
            final long idleInterval = dbProperties.getLogLdiIdleDurationMS();
            final long idleRetry = dbProperties.getLogLdiIdleCheckRetryMS();
            final int maxAttempts = dbProperties.getLogLdiIdleCheckMaxAttempts();
            long diff = System.currentTimeMillis() - lastMessageTime.get();
            int attempts = 0;
            while (diff < idleInterval && attempts++ < maxAttempts) {
                trace.debug("LogMessageLDIStore attempt ", attempts, ": only ", diff, " millis have elapsed since last message seen, sleeping");
                SleepUtilities.checkedSleep(idleRetry);
                diff = System.currentTimeMillis() - lastMessageTime.get();
            }
            trace.debug("LogMessageLDIStore no log messages received in the last ", idleInterval, "ms, continuing shutdown.");
        }
        super.stopResource();
        handler = null;
    }

    /**
     * Check a string column for length, and, if too long, truncate and issue
     * a warning message. This overrides the regular one so we can avoid
     * publishing while we are processing.
     *
     * @param what      The name of the column
     * @param maxLength The maximum supported length
     * @param value     The value in question
     * @return Value truncated if necessary
     */
    @Override
    protected String checkLength(final String what,
                                 final int maxLength,
                                 final String value) {
        if (value == null) {
            return null;
        }

        final String useValue = value.trim();
        final int length = useValue.length();

        if (length <= maxLength) {
            return useValue;
        }

        final StringBuilder sb = new StringBuilder(what);

        sb.append(" of ").append(length);

        sb.append(" is truncated to ").append(maxLength);

        sb.append((maxLength != 1) ? " characters" : " character");

        trace.warn(sb.toString());

        return useValue.substring(0, maxLength);
    }


    /**
     * Receive a log message from the internal message bus and insert the
     * information into the database
     *
     * @param message The external log message received on the internal bus
     */
    @Override
    public void handleLogMessage(final ILogMessage message) {
        lastMessageTime.set(System.currentTimeMillis());
        try {
            /*
             * MPCS-7796 - Added check for null session number. I
             * have no idea how we get here in that state, but somehow, we seem
             * to.
             */
            if ((message != null) && !isStoreStopped.get() && contextConfig.getContextId().getNumber() != null) {
                insertLogMessage(message);
                endSessionInfo.updateTimes(
                        new AccurateDateTime(message.getEventTime()), null, null);
            } else {
                trace.debug("Unable to handle message. Store is not up or context is not initialized. isStoreStopped=",
                        isStoreStopped.get());
            }
        } catch (final DatabaseException de) {
            // Avoid infinite loops, don't log with marker
            trace.error("LDI LogMessage storage failed: " + de.getMessage(), de.getCause());
        }
    }

}
