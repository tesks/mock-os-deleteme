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
import jpl.gds.db.api.sql.store.ICommandUpdateStore;
import jpl.gds.db.api.sql.store.ldi.ICommandMessageLDIStore;
import jpl.gds.db.api.types.CommandType;
import jpl.gds.db.mysql.impl.sql.store.CommandUpdateStore;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeTooLargeException;
import jpl.gds.tc.api.CommandStatusType;
import jpl.gds.tc.api.ICpdUplinkStatus;
import jpl.gds.tc.api.message.*;
import org.springframework.context.ApplicationContext;

import java.util.UUID;

/**
 * This is the database write/storage interface to the CommandMessage table in
 * the MPCS database. This class will receive an input command message and write
 * it to the CommandMessage table in the database. This is done via LDI.
 *
 * This class is very similar to the equivalent non-LDI class. The fields are
 * dumped into a BytesBuilder instead of a PreparedStatement, and written by
 * means of a table-specific call to the superclass.
 *
 * startResource and stopResource also make table-specific calls to enable or
 * disable processing.
 *
 * @version MPCS-10869 - added support for FileCFDP
 * <br>
 * TODO: Verify/update support for CLTU_F when the message usage is implemented
 */
public class CommandMessageLDIStore extends AbstractLDIStore implements ICommandMessageLDIStore {
    /**
     * Set true to finalize CommandMessage when final CommandStatus is written.
     */
    private static final boolean AUTOMATIC_FINALIZE = false;

    private static final int     REQUEST_ID_LENGTH  = 100;
    /** MPCS-7917  1024 => 4096 */
    private static final int     MESSAGE_LENGTH     = 4096;
    private static final int     FILE_LENGTH        = 1024;
    private static final int     CS_LENGTH          = 16;
    private static final int     STATUS_LENGTH      = 32;
    private static final int     FR_LENGTH          = 128;

    private static final String  EMPTY_COMMAND      = "";

    private final BytesBuilder   _bb                = new BytesBuilder();
    private final BytesBuilder   _bb_meta           = new BytesBuilder();

    /** Unsigned int that keeps generated requestIds unique */
    private long                 _distinguisher     = 0L;

    /** Store to update finalized status */
    private ICommandUpdateStore  _cus               = null;

    /**
     * Creates an instance of ICommandMessageLDIStore.
     *
     * @param appContext
     *            The information about the current test session
     */
    public CommandMessageLDIStore(final ApplicationContext appContext) {
        /*
         * MPCS-7135 - Add second argument to indicate this store
         * does not operate asynchronously.
         */
        super(appContext, ICommandMessageLDIStore.STORE_IDENTIFIER, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insertMessage(final IUplinkMessage m) throws DatabaseException {
    	/*
         * TODO: MPCS-10869 CLTU-F has a message type, but is not an actual message class at this time.
         *       When it has been implemented and is used, it needs to be added to this.
         */
        if (!dbProperties.getUseDatabase() || isStoreStopped.get()) {
            return;
        }

        if (m instanceof ICpdUplinkStatusMessage) {
            // Note that we use the session configuration from the message,
            // not the local one.

            final ICpdUplinkStatusMessage statusMsg = (ICpdUplinkStatusMessage) m;

            if (statusMsg.getDoNotInsert()) {
                return;
            }

            final ICpdUplinkStatus cus = statusMsg.getStatus();

            // Command type just needs to not be SSE

			// MPCS-9908 - the Context Key isn't populated in the status message, but has been stored in the metadata header,
			//                 so added getMetadataHeader() call.
            insertStatus(statusMsg.getMetadataHeader().getContextKey(), CommandType.FLIGHT_SOFTWARE_COMMAND, cus.getId(),
                         cus.getTimestamp(), cus.getStatus(), null, cus.getDssId(), cus.getBit1RadTime(),
                         cus.getLastBitRadTime());

        }
        else if (m instanceof ICpdUplinkMessage) {
            /** MPCS-6057  Set SSE status */

            final ICpdUplinkMessage iCmdMsg = (ICpdUplinkMessage) m;

            insertMessage(iCmdMsg.getICmdRequestId(), m.getEventTime(), m.getDatabaseString(), getCommandType(m), iCmdMsg.getOriginalFilename(),
                    iCmdMsg.getScmfFilename(), iCmdMsg.getICmdRequestStatus(), iCmdMsg.getICmdRequestFailureReason(), m.getCommandedSide(),
                    iCmdMsg.getChecksum(), iCmdMsg.getTotalCltus(), iCmdMsg.getDssId(), iCmdMsg.getBit1RadTime(),
                          iCmdMsg.getLastBitRadTime(), true, contextConfig.getContextId().getNumber());
        }
        else {
            /** MPCS-6057   Get SSE status */
            /** MPCS-6328 Get Sequence Directive status */
            boolean ok = true;
            long sid = contextConfig.getContextId().getNumber();

            if (m.isType(CommandMessageType.SseCommand) || m.isType(CommandMessageType.FileCfdp)) {
                final ITransmittableCommandMessage scm = (ITransmittableCommandMessage) m;

                ok = scm.isSuccessful();

            }
            if (m.isType(CommandMessageType.FileCfdp)
                    && m.getContextKey() != null
                    && m.getContextKey().getNumber() != null
                    && m.getContextKey().getNumber() != 0L) {
                sid = m.getContextKey().getNumber();
            }
            
            /*
             * MPCS-9142 - Removed final else if. In R7.7 this referred only to
             * SequenceDirectiveMessage, but in R8 uses ITransmittableCommandMessage. As of this
             * JIRA, only SseCommand implements this interface.
             */

            insertMessage(null,
                          m.getEventTime(),
                          m.getDatabaseString(),
                          getCommandType(m),
                          null,
                          null,
                          null,
                          null,
                          m.getCommandedSide(),
                          null,
                          null,
                          null,
                          null,
                          null,
                          ok,
                          sid);
        }
    }

    /**
     * Insert a command message and a command status into the database.
     *
     * @param rawRequestId
     *            The request id
     * @param eventTime
     *            The time the message was issued
     * @param command
     *            The command text
     * @param type
     *            The command type
     * @param originalFile
     *            The original file
     * @param scmfFile
     *            The SCMF file
     * @param rawStatus
     *            Status of command
     * @param failReason
     *            Error text or null if no error
     * @param cs
     *            The commanded side
     * @param checksum
     *            File checksum
     * @param totalCltus
     *            Total CLTUs
     * @param dssId
     *            Station
     * @param bit1RadTime
     *            Start radiation time
     * @param lastBitRadTime
     *            End radiation time
     * @param isSuccessful
     *            True if successful (only false for SSE)
     *
     * @throws DatabaseException
     *             Throws SQLException
     */
    protected void insertMessage(final String rawRequestId, final IAccurateDateTime eventTime, final String command,
                                 final CommandType type, final String originalFile, final String scmfFile,
                                 final CommandStatusType rawStatus, final String failReason, final String cs,
                                 final Long checksum, final Long totalCltus, final Integer dssId,
                                 final IAccurateDateTime bit1RadTime, final IAccurateDateTime lastBitRadTime,
                                 final boolean isSuccessful, final long sessionId)
            throws DatabaseException
    {
        /** MPCS-6057 Add SSE status */

        final CommandType ct = checkCommandType(type);
        final CommandStatusType status = checkCommandStatus(ct, rawStatus, isSuccessful);
        final String requestId = checkRequestId(ct, rawRequestId);
        final int hostId = contextConfig.getContextId().getHostId();

        if (sessionId <= 0L) {
            throw new DatabaseException("CommandMessage.sessionId cannot be less than 1");
        }

        if (hostId < 0) {
            throw new DatabaseException("CommandMessage.hostId cannot be negative");
        }

        try {
            synchronized (this) {
                if (!archiveController.isUp()) {
                    throw new IllegalStateException("This connection has already been closed");
                }

                // CommandMessage

                _bb.clear();

                // Format as a line for LDI

                _bb.insert(sessionId);
                _bb.insertSeparator();

                _bb.insert(hostId);
                _bb.insertSeparator();

                _bb.insert(contextConfig.getContextId().getFragment());
                _bb.insertSeparator();

                /** MPCS-5153 */
                _bb.insertTextComplainReplace(checkLength("CommandMessage.requestId", REQUEST_ID_LENGTH, requestId));

                _bb.insertSeparator();

                /** MPCS-5153  */
                _bb.insertTextAllowReplace((command != null) ? checkLength("CommandMessage.message", MESSAGE_LENGTH, command) : EMPTY_COMMAND);

                _bb.insertSeparator();

                /** MPCS-5153  */
                _bb.insertTextComplainReplace(ct.toString());
                _bb.insertSeparator();

                /** MPCS-5153 */
                _bb.insertTextOrNullComplainReplace(checkLength("CommandMessage.originalFile", FILE_LENGTH, originalFile));

                _bb.insertSeparator();

                _bb.insertTextOrNullComplainReplace(checkLength("CommandMessage.scmfFile", FILE_LENGTH, scmfFile));

                _bb.insertSeparator();

                /** MPCS-5153 */
                _bb.insertTextOrNullComplainReplace(checkLength("CommandMessage.commandedSide", CS_LENGTH, cs));
                _bb.insertSeparator();

                _bb.insert(status.isFinal() ? 1 : 0);
                _bb.insertSeparator();

                /** MPCS-5835  Armor two longs */

                _bb.insertLongOrNull(checkIntegerRange("CommandMessage.checksum", 0L, MAX_UNSIGNED_INT, true, checksum));

                _bb.insertSeparator();

                _bb.insertLongOrNull(checkIntegerRange("CommandMessage.totalCltus", 0L, MAX_UNSIGNED_INT, true, totalCltus));

                _bb.insertTerminator();

                // CommandStatus

                formatStatus(contextConfig.getContextId().getContextKey(), requestId, eventTime, status, failReason,
                             dssId, bit1RadTime, lastBitRadTime, _bb_meta, sessionId);

                writeToStream(_bb, _bb_meta);
            }
        }
        catch (final RuntimeException re) {
            throw re;
        }
        catch (final Exception e) {
            throw new DatabaseException("Error inserting CommandMessage/CommandStatus record into database", e);
        }
    }

    /**
     * Insert a command status message into the database.
     *
     * Warning is a false positive, we're not calling the system finalize.
     *
     * @param iContextKey
     *            Session configuration
     * @param type
     *            Command type
     * @param rawRequestId
     *            Request id
     * @param eventTime
     *            The time the message was issued
     * @param rawStatus
     *            Command status
     * @param failReason
     *            Error text or null if no error
     * @param dssId
     *            Station
     * @param bit1RadTime
     *            Start radiation time
     * @param lastBitRadTime
     *            End radiation time
     *
     * @throws DatabaseException
     *             Throws SQLException
     */
    protected void insertStatus(final IContextKey iContextKey, final CommandType type, final String rawRequestId,
                                final IAccurateDateTime eventTime, final CommandStatusType rawStatus,
                                final String failReason, final Integer dssId, final IAccurateDateTime bit1RadTime,
                                final IAccurateDateTime lastBitRadTime)
            throws DatabaseException {
        /** MPCS-6057 Add SSE status to checkCommandStatus */

        if (!dbProperties.getUseDatabase() || isStoreStopped.get()) {
            return;
        }

        final CommandType ct = checkCommandType(type);
        final CommandStatusType status = checkCommandStatus(ct, rawStatus, true);
        final String requestId = StringUtil.emptyAsNull(rawRequestId);
        try {
            synchronized (this) {
                if (!archiveController.isUp()) {
                    throw new IllegalStateException("This connection has already been closed");
                }

                formatStatus(iContextKey, requestId, eventTime, status, failReason, dssId, bit1RadTime,
                             lastBitRadTime, _bb_meta, iContextKey.getNumber());

                writeToStream(null, _bb_meta);
            }
        }
        catch (final RuntimeException re) {
            throw re;
        }
        catch (final Exception e) {
            throw new DatabaseException("Error inserting CommandStatus record into database", e);
        }

        // If finalized, update finalized state of associated CommandMessage.

        if ((_cus != null) && status.isFinal()) {
            _cus.finalize(iContextKey.getHostId(), iContextKey.getNumber(), iContextKey.getFragment(), requestId);
        }
    }

    /**
     * Format a status message for insertion into the database
     *
     * @param iContextKey
     *            Session configuration
     * @param requestId
     *            Request id
     * @param eventTime
     *            The time the message was issued
     * @param status
     *            Status
     * @param failReason
     *            Error text or null if no error
     * @param dssId
     *            Station
     * @param bit1RadTime
     *            Start radiation time
     * @param lastBitRadTime
     *            End radiation time
     * @param bb
     *            Bytes builder to populate
     *
     * @throws DatabaseException
     *             Throws SQLException
     */
    protected void formatStatus(final IContextKey iContextKey, final String requestId,
                                final IAccurateDateTime eventTime, final CommandStatusType status,
                                final String failReason, final Integer dssId, final IAccurateDateTime bit1RadTime,
                                final IAccurateDateTime lastBitRadTime, final BytesBuilder bb, final Long sessionId)
            throws DatabaseException {
        final Integer hostId = iContextKey.getHostId();

        if (null == sessionId) {
            throw new NullPointerException("CommandStatus.sessionId must be initialized.");
        }

        if (null == hostId) {
            throw new NullPointerException("CommandStatus.hostId must be initialized.");
        }

        if (sessionId <= 0L) {
            throw new DatabaseException("CommandStatus.sessionId cannot be less than 1");
        }

        if (hostId < 0) {
            throw new DatabaseException("CommandStatus.hostId cannot be negative");
        }

        try {
            if ((requestId == null) || requestId.isEmpty()) {
                throw new DatabaseException("CommandStatus must have requestId");
            }

            if (status == null) {
                throw new DatabaseException("CommandStatus must have status");
            }

            bb.clear();

            // Format as a line for LDI

            bb.insert(sessionId);
            bb.insertSeparator();

            bb.insert(hostId);
            bb.insertSeparator();

            bb.insert(iContextKey.getFragment());
            bb.insertSeparator();

            /** MPCS-5153 */
            bb.insertTextComplainReplace(checkLength("CommandStatus.requestId", REQUEST_ID_LENGTH, requestId));

            bb.insertSeparator();

            final IAccurateDateTime rct = new AccurateDateTime();

            try {
                bb.insertDateAsCoarseFineSeparate(rct);
            }
            catch (final TimeTooLargeException ttle) {
                trace.warn(Markers.DB, dateExceedsWarning("CommandStatus.rct", null, rct));
            }

            try {
                bb.insertDateAsCoarseFineSeparate(eventTime);
            }
            catch (final TimeTooLargeException ttle) {
                trace.warn(Markers.DB, dateExceedsWarning("CommandStatus.eventTime", null, eventTime));
            }

            /** MPCS-515 */
            bb.insertTextComplainReplace(checkLength("CommandStatus.status", STATUS_LENGTH, status.toString()));

            bb.insertSeparator();

            String fr = StringUtil.emptyAsNull(failReason);

            if ("NONE".equalsIgnoreCase(fr)) {
                // We don't want NONE, NULL means no error
                fr = null;
            }

            /** MPCS-5153 */
            bb.insertTextOrNullComplainReplace(checkLength("CommandStatus.failReason", FR_LENGTH, fr));

            bb.insertSeparator();

            if (bit1RadTime != null) {
                try {
                    bb.insertDateAsCoarseFineSeparate(bit1RadTime);
                }
                catch (final TimeTooLargeException ttle) {
                    trace.warn(Markers.DB, dateExceedsWarning("CommandStatus.bit1RadTime", null, bit1RadTime));
                }
            }
            else {
                bb.insertNULL();
                bb.insertSeparator();

                bb.insertNULL();
                bb.insertSeparator();
            }

            if (lastBitRadTime != null) {
                try {
                    bb.insertDateAsCoarseFineSeparate(lastBitRadTime);
                }
                catch (final TimeTooLargeException ttle) {
                    trace.warn(Markers.DB, dateExceedsWarning("CommandStatus.lastBitRadTime", null, lastBitRadTime));
                }
            }
            else {
                bb.insertNULL();
                bb.insertSeparator();

                bb.insertNULL();
                bb.insertSeparator();
            }

            /*
             * BEGIN MPCS-4839 For historical reasons we allow
             * negative values here and fix them. Otherwise must be within
             * bounds.
             */

            int mdss = ((dssId != null) ? Math.max(dssId, StationIdHolder.MIN_VALUE) : StationIdHolder.MIN_VALUE);

            if (!isStationInRange(mdss)) {
                trace.error(Markers.DB,
                        "CommandStatus DSS " , mdss , " is outside of range [" , StationIdHolder.MIN_VALUE
                        , "-" , StationIdHolder.MAX_VALUE , "] and will be forced to " , StationIdHolder.MIN_VALUE);

                mdss = StationIdHolder.MIN_VALUE;
            }

            bb.insert(mdss);
            bb.insertSeparator();

            /*
             * END MPCS-4839
             */

            bb.insert(status.isFinal() ? 1 : 0);
            bb.insertTerminator();
        }
        catch (final RuntimeException re) {
            throw re;
        }
        catch (final Exception e) {
            throw new DatabaseException("Error formatting CommandStatus record", e);
        }
    }

    /**
     * Check value of command type and redo as necessary.
     *
     * @param ct
     *            Command type.
     *
     * @return Desired command type
     */
    protected CommandType checkCommandType(final CommandType ct) {
        if (ct == null) {
            return CommandType.UNKNOWN_COMMAND;
        }

        return ct;
    }

    /**
     * Check value of command status for SSE or null, and redo as necessary.
     *
     * @param ct
     *            Command type.
     * @param cst
     *            Original status
     * @param isSuccessful
     *            True if successful (only false for SSE or FileCfdp)
     *
     * @return Desired command status
     */
    protected CommandStatusType checkCommandStatus(final CommandType ct,
                                                   final CommandStatusType cst,
                                                   final boolean isSuccessful) {
        /** MPCS-6057Get SSE status */
        /** MPCS-6328 Add Sequence directive status. */
        if (CommandType.SSE_COMMAND.equals(ct) || CommandType.SEQUENCE_DIRECTIVE.equals(ct)) {
            return (isSuccessful ? CommandStatusType.Radiated : CommandStatusType.Send_Failure);
        }

        // MPCS-10869 - added support for FILE CFDP
        if(CommandType.FILE_CFDP.equals(ct)) {
            return (isSuccessful ? CommandStatusType.Submitted : CommandStatusType.Failed);
        }

        if (cst == null) {
            // Assume radiated if we are not given a status.

            return CommandStatusType.Radiated;
        }

        return cst;
    }

    /**
     * Check a requestId.
     *
     * @param ct
     *            Command type
     * @param rawRequestId
     *            Initial request id
     *
     * @return Possible modified request id
     */
    protected String checkRequestId(final CommandType ct, final String rawRequestId) {
        String requestId = StringUtil.emptyAsNull(rawRequestId);

        if (requestId != null) {
            requestId = checkLength("CommandMessage.requestId", REQUEST_ID_LENGTH, requestId);
        }
        else {
            requestId = generateRequestId(ct);
        }

        return requestId;
    }

    /**
     * Generate a requestId.
     *
     * @param ct
     *            Command type
     *
     * @return Request id
     */
    protected synchronized String generateRequestId(final CommandType ct) {
        final long pid = GdsSystemProperties.getIntegerPid();
        final UUID uuid = new UUID(System.currentTimeMillis(), ((pid << Integer.SIZE) | _distinguisher));
        ++_distinguisher;

        if (_distinguisher > Integer.MAX_VALUE) {
            _distinguisher = 0L;
        }

        final StringBuilder sb = new StringBuilder();

        if (CommandType.SSE_COMMAND.equals(ct)) {
            sb.append("SSE-");
        }
        else {
            sb.append("FSW-");
        }

        sb.append(uuid);

        return sb.toString();
    }

    /**
     * Start this store.
     */
    @Override
    public void startResource() {
        super.startResource();

        handler = new BaseMessageHandler() {
            /**
             * {@inheritDoc}
             */
            @Override
            public synchronized void handleMessage(final IMessage m) {
                handleCommandMessage((IUplinkMessage) m);
            }
        };

        // Subscribe to command messages on the internal bus
        if (messagePublicationBus != null) {
            messagePublicationBus.subscribe(CommandMessageType.HardwareCommand, handler);
            messagePublicationBus.subscribe(CommandMessageType.FlightSoftwareCommand, handler);
            messagePublicationBus.subscribe(CommandMessageType.SseCommand, handler);
            messagePublicationBus.subscribe(CommandMessageType.FileLoad, handler);
            messagePublicationBus.subscribe(CommandMessageType.Scmf, handler);
            messagePublicationBus.subscribe(CommandMessageType.RawUplinkData, handler);
            messagePublicationBus.subscribe(CommandMessageType.UplinkStatus, handler);
            /**
             * MPCS-6328  Add Sequence directive messages to
             * subscription
             */
            messagePublicationBus.subscribe(CommandMessageType.SequenceDirective, handler);
            messagePublicationBus.subscribe(CommandMessageType.FileCfdp, handler);
            messagePublicationBus.subscribe(CommandMessageType.CltuF, handler);
        }

        if (AUTOMATIC_FINALIZE) {
            _cus = new CommandUpdateStore(appContext);
        }
    }

    /**
     * Stop storing command messages.
     */
    @Override
    protected void stopResource() {
       super.stopResource();

        // Unsubscribe from command messages on the internal bus
        if (messagePublicationBus != null) {
            messagePublicationBus.unsubscribe(CommandMessageType.HardwareCommand, handler);
            messagePublicationBus.unsubscribe(CommandMessageType.FlightSoftwareCommand, handler);
            messagePublicationBus.unsubscribe(CommandMessageType.SseCommand, handler);
            messagePublicationBus.unsubscribe(CommandMessageType.FileLoad, handler);
            messagePublicationBus.unsubscribe(CommandMessageType.Scmf, handler);
            messagePublicationBus.unsubscribe(CommandMessageType.RawUplinkData, handler);
            messagePublicationBus.unsubscribe(CommandMessageType.UplinkStatus, handler);
            /**
             * MPCS-6328 Add Sequence directive messages to
             * subscription
             */
            messagePublicationBus.unsubscribe(CommandMessageType.SequenceDirective, handler);
            messagePublicationBus.unsubscribe(CommandMessageType.FileCfdp, handler);
            messagePublicationBus.unsubscribe(CommandMessageType.CltuF, handler);
        }

        if (_cus != null) {
            _cus.stopUpdateStore();
            _cus = null;
        }
     }

    /**
     * Receive a command message from the internal message bus and insert the
     * information into the database
     *
     * @param message
     *            The external command message received on the internal bus
     */
    protected void handleCommandMessage(final IUplinkMessage message) {
        try {
            insertMessage(message);
            endSessionInfo.updateTimes(new AccurateDateTime(message.getEventTime()), null, null);
        }
        catch (final DatabaseException de) {
            trace.error("LDI CommandMessage storage failed: ", de);
        }
    }

    /**
     * Extract command type from message.
     *
     * @param aum
     *            Message
     *
     * @return Command type
     *
     * @throws DatabaseException
     *             If cannot convert
     */
    protected CommandType getCommandType(final IUplinkMessage aum) throws DatabaseException {
        final String type = StringUtil.safeTrim(aum.getType().getSubscriptionTag());

        // See what type of message we're dealing with

        CommandType cmt = null;

        try {
            cmt = new CommandType(type);
        }
        catch (final IllegalArgumentException iae) {
            throw new DatabaseException("Unrecognized command message type received in command message store: " + type, iae);
        }

        return cmt;
    }
}
