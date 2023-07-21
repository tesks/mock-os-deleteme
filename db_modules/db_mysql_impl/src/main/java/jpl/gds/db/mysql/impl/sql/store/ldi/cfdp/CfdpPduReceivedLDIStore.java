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
package jpl.gds.db.mysql.impl.sql.store.ldi.cfdp;

import jpl.gds.cfdp.message.api.CfdpMessageType;
import jpl.gds.cfdp.message.api.ICfdpPduReceivedMessage;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpPduReceivedLDIStore;
import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.time.TimeTooLargeException;
import org.springframework.context.ApplicationContext;

public class CfdpPduReceivedLDIStore extends AbstractCfdpLDIStore implements ICfdpPduReceivedLDIStore {

    private final BytesBuilder _bb = new BytesBuilder();

    /**
     * Creates an instance of ICfdpPduReceivedMessageLDIStore.
     *
     * @param appContext The information about the current test session
     */
    public CfdpPduReceivedLDIStore(final ApplicationContext appContext) {
        super(appContext, ICfdpPduReceivedLDIStore.STORE_IDENTIFIER, false);
    }

    /**
     * Stop storing command messages.
     */
    @Override
    protected void stopResource() {
        super.stopResource();

        if (messagePublicationBus != null) {
            messagePublicationBus.unsubscribe(CfdpMessageType.CfdpPduReceived, handler);
        }

    }

    @Override
    public void insertMessage(ICfdpPduReceivedMessage m) throws DatabaseException {

        if (dbProperties.getUseDatabase() && !isStoreStopped.get() && m instanceof ICfdpPduReceivedMessage) {
            final ICfdpPduReceivedMessage cfdpPduReceivedMessage = m;

            try {

                synchronized (this) {
                    if (!archiveController.isUp()) {
                        throw new IllegalStateException("This connection has already been closed");
                    }

                    _bb.clear();

                    // Format as a line for LDI

                    insertValOrNull(_bb, extractSessionIdIfAny(cfdpPduReceivedMessage.getContextKey()));
                    _bb.insertSeparator();

                    insertValOrNull(_bb, extractSessionHostIdIfAny(cfdpPduReceivedMessage.getContextKey()));
                    _bb.insertSeparator();

                    insertValOrNull(_bb, extractSessionFragmentIfAny(cfdpPduReceivedMessage.getContextKey()));
                    _bb.insertSeparator();

                    try {
                        _bb.insertDateAsCoarseFineSeparate(cfdpPduReceivedMessage.getEventTime());
                    } catch (final TimeTooLargeException ttle) {
                        TraceManager.getDefaultTracer().warn(

                                "pduTime exceeded maximum");
                    }

                    _bb.insertTextAllowReplace(cfdpPduReceivedMessage.getHeader().getCfdpProcessorInstanceId());
                    _bb.insertSeparator();

                    _bb.insertTextAllowReplace(cfdpPduReceivedMessage.getPduId());
                    _bb.insertSeparator();

                    String flattenedMetadata = "";
                    String delim = "";

                    for (String s : cfdpPduReceivedMessage.getMetadata()) {
                        flattenedMetadata += delim + s.replaceAll(",", "\\,");
                        delim = ",";
                    }
                    _bb.insertTextAllowReplace(flattenedMetadata);
                    _bb.insertSeparator();

                    _bb.insert(extractContextId(cfdpPduReceivedMessage.getContextKey()));
                    _bb.insertSeparator();

                    _bb.insert(extractContextHostId(cfdpPduReceivedMessage.getContextKey()));
                    _bb.insertTerminator();

                    // Add the line to the LDI batch
                    writeToStream(_bb);
                }

            } catch (

                    final RuntimeException re) {
                throw re;
            } catch (final Exception e) {
                throw new DatabaseException("Error inserting CfdpPduReceived record intodatabase", e);
            }

        }

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
                ICfdpPduReceivedMessage cfdpPduReceivedMessage = (ICfdpPduReceivedMessage) m;

                try {
                    insertMessage(cfdpPduReceivedMessage);
                } catch (final DatabaseException de) {
                    trace.error("LDI CfdpPduReceived storage failed: ", de);
                }

            }

        };

        // Subscribe to CFDP PDU Received Messages on the internal bus
        if (messagePublicationBus != null) {
            messagePublicationBus.subscribe(CfdpMessageType.CfdpPduReceived, handler);
        }
    }

}
