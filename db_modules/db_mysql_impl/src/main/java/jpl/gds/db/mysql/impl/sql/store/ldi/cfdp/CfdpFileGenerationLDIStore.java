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
import jpl.gds.cfdp.message.api.ICfdpFileGenerationMessage;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpFileGenerationLDIStore;
import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.time.TimeTooLargeException;
import org.springframework.context.ApplicationContext;

public class CfdpFileGenerationLDIStore extends AbstractCfdpLDIStore implements ICfdpFileGenerationLDIStore {

    private final BytesBuilder _bb = new BytesBuilder();

    /**
     * Creates an instance of ICfdpFileGenerationMessageLDIStore.
     *
     * @param appContext The information about the current test session
     */
    public CfdpFileGenerationLDIStore(final ApplicationContext appContext) {
        super(appContext, ICfdpFileGenerationLDIStore.STORE_IDENTIFIER, false);
    }

    /**
     * Stop storing command messages.
     */
    @Override
    protected void stopResource() {
        super.stopResource();

        if (messagePublicationBus != null) {
            messagePublicationBus.unsubscribe(CfdpMessageType.CfdpFileGeneration, handler);
        }

    }

    @Override
    public void insertMessage(ICfdpFileGenerationMessage m) throws DatabaseException {

        if (dbProperties.getUseDatabase() && !isStoreStopped.get() && m instanceof ICfdpFileGenerationMessage) {
            final ICfdpFileGenerationMessage cfdpFileGenerationMessage = m;

            try {

                synchronized (this) {
                    if (!archiveController.isUp()) {
                        throw new IllegalStateException("This connection has already been closed");
                    }

                    _bb.clear();

                    // Format as a line for LDI

                    insertValOrNull(_bb, extractSessionIdIfAny(cfdpFileGenerationMessage.getContextKey()));
                    _bb.insertSeparator();

                    insertValOrNull(_bb, extractSessionHostIdIfAny(cfdpFileGenerationMessage.getContextKey()));
                    _bb.insertSeparator();

                    insertValOrNull(_bb, extractSessionFragmentIfAny(cfdpFileGenerationMessage.getContextKey()));
                    _bb.insertSeparator();

                    try {
                        _bb.insertDateAsCoarseFineSeparate(cfdpFileGenerationMessage.getEventTime());
                    } catch (final TimeTooLargeException ttle) {
                        TraceManager.getDefaultTracer().warn(

                                "eventTime exceeded maximum");
                    }

                    _bb.insertTextAllowReplace(cfdpFileGenerationMessage.getHeader().getCfdpProcessorInstanceId());
                    _bb.insertSeparator();

                    _bb.insertTextAllowReplace(cfdpFileGenerationMessage.getDownlinkFileMetadataFileLocation());
                    _bb.insertSeparator();

                    _bb.insertTextAllowReplace(cfdpFileGenerationMessage.getDownlinkFileLocation());
                    _bb.insertSeparator();

                    _bb.insert(extractContextId(cfdpFileGenerationMessage.getContextKey()));
                    _bb.insertSeparator();

                    _bb.insert(extractContextHostId(cfdpFileGenerationMessage.getContextKey()));
                    _bb.insertTerminator();

                    // Add the line to the LDI batch
                    writeToStream(_bb);
                }

            } catch (

                    final RuntimeException re) {
                throw re;
            } catch (final Exception e) {
                throw new DatabaseException("Error inserting CfdpFileGeneration record intodatabase", e);
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
                ICfdpFileGenerationMessage cfdpFileGenerationMessage = (ICfdpFileGenerationMessage) m;

                try {
                    insertMessage(cfdpFileGenerationMessage);
                } catch (final DatabaseException de) {
                    trace.error("LDI CfdpFileGeneration storage failed: ", de);
                }

            }

        };

        // Subscribe to CFDP File Generation Messages on the internal bus
        if (messagePublicationBus != null) {
            messagePublicationBus.subscribe(CfdpMessageType.CfdpFileGeneration, handler);
        }
    }

}
