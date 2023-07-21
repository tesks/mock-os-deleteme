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
import jpl.gds.cfdp.message.api.ICfdpIndicationMessage;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpIndicationLDIStore;
import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.time.TimeTooLargeException;
import org.springframework.context.ApplicationContext;

public class CfdpIndicationLDIStore extends AbstractCfdpLDIStore implements ICfdpIndicationLDIStore {

    private final BytesBuilder _bb = new BytesBuilder();

    /**
     * Creates an instance of ICfdpIndicationMessageLDIStore.
     *
     * @param appContext The information about the current test session
     */
    public CfdpIndicationLDIStore(final ApplicationContext appContext) {
        super(appContext, ICfdpIndicationLDIStore.STORE_IDENTIFIER, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insertMessage(final ICfdpIndicationMessage m) throws DatabaseException {

        if (dbProperties.getUseDatabase() && !isStoreStopped.get() && m instanceof ICfdpIndicationMessage) {
            final ICfdpIndicationMessage cfdpIndicationMessage = m;

            try {

                synchronized (this) {
                    if (!archiveController.isUp()) {
                        throw new IllegalStateException("This connection has already been closed");
                    }

                    _bb.clear();

                    // Format as a line for LDI

                    insertValOrNull(_bb, extractSessionIdIfAny(cfdpIndicationMessage.getContextKey()));
                    _bb.insertSeparator();

                    insertValOrNull(_bb, extractSessionHostIdIfAny(cfdpIndicationMessage.getContextKey()));
                    _bb.insertSeparator();

                    insertValOrNull(_bb, extractSessionFragmentIfAny(cfdpIndicationMessage.getContextKey()));
                    _bb.insertSeparator();

                    try {
                        _bb.insertDateAsCoarseFineSeparate(cfdpIndicationMessage.getEventTime());
                    } catch (final TimeTooLargeException ttle) {
                        TraceManager.getDefaultTracer().warn(

                                "indicationTime exceeded maximum");
                    }

                    _bb.insertTextAllowReplace(cfdpIndicationMessage.getHeader().getCfdpProcessorInstanceId());
                    _bb.insertSeparator();

                    _bb.insertTextAllowReplace(cfdpIndicationMessage.getIndicationType().name());
                    _bb.insertSeparator();

                    if (cfdpIndicationMessage.getCondition() != null) {
                        _bb.insertTextAllowReplace(cfdpIndicationMessage.getCondition().toString());
                    } else {
                        _bb.insertNULL();
                    }
                    _bb.insertSeparator();

                    _bb.insertTextAllowReplace(cfdpIndicationMessage.getTransactionDirection().name());
                    _bb.insertSeparator();

                    _bb.insertLongAsUnsigned(cfdpIndicationMessage.getSourceEntityId());
                    _bb.insertSeparator();

                    _bb.insertLongAsUnsigned(cfdpIndicationMessage.getTransactionSequenceNumber());
                    _bb.insertSeparator();

                    _bb.insert(cfdpIndicationMessage.getServiceClass());
                    _bb.insertSeparator();

                    _bb.insertLongAsUnsigned(cfdpIndicationMessage.getDestinationEntityId());
                    _bb.insertSeparator();

                    _bb.insert(cfdpIndicationMessage.getInvolvesFileTransfer() ? 1 : 0);
                    _bb.insertSeparator();

                    _bb.insertLongAsUnsigned(cfdpIndicationMessage.getTotalBytesSentOrReceived());
                    _bb.insertSeparator();

                    if (cfdpIndicationMessage.getTriggeringType() != null) {
                        _bb.insertTextAllowReplace(cfdpIndicationMessage.getTriggeringType().name());
                    } else {
                        _bb.insertNULL();
                    }
                    _bb.insertSeparator();

                    if (cfdpIndicationMessage.getPduId() != null) {
                        _bb.insertTextAllowReplace(cfdpIndicationMessage.getPduId());
                    } else {
                        _bb.insertNULL();
                    }
                    _bb.insertSeparator();

                    if (cfdpIndicationMessage.getTriggeringPduFixedHeader() != null) {
                        _bb.insert(cfdpIndicationMessage.getTriggeringPduFixedHeader().getVersion());
                    } else {
                        _bb.insertNULL();
                    }
                    _bb.insertSeparator();

                    if (cfdpIndicationMessage.getTriggeringPduFixedHeader() != null
                            && cfdpIndicationMessage.getTriggeringPduFixedHeader().getType() != null) {
                        _bb.insertTextAllowReplace(
                                cfdpIndicationMessage.getTriggeringPduFixedHeader().getType().name());
                    } else {
                        _bb.insertNULL();
                    }
                    _bb.insertSeparator();

                    if (cfdpIndicationMessage.getTriggeringPduFixedHeader() != null
                            && cfdpIndicationMessage.getTriggeringPduFixedHeader().getDirection() != null) {
                        _bb.insertTextAllowReplace(
                                cfdpIndicationMessage.getTriggeringPduFixedHeader().getDirection().name());
                    } else {
                        _bb.insertNULL();
                    }
                    _bb.insertSeparator();

                    if (cfdpIndicationMessage.getTriggeringPduFixedHeader() != null
                            && cfdpIndicationMessage.getTriggeringPduFixedHeader().getTransmissionMode() != null) {
                        _bb.insertTextAllowReplace(
                                cfdpIndicationMessage.getTriggeringPduFixedHeader().getTransmissionMode().name());
                    } else {
                        _bb.insertNULL();
                    }
                    _bb.insertSeparator();

                    if (cfdpIndicationMessage.getTriggeringPduFixedHeader() != null) {
                        _bb.insert(cfdpIndicationMessage.getTriggeringPduFixedHeader().isCrcFlagPresent() ? 1 : 0);
                    } else {
                        _bb.insertNULL();
                    }
                    _bb.insertSeparator();

                    if (cfdpIndicationMessage.getTriggeringPduFixedHeader() != null) {
                        _bb.insert(cfdpIndicationMessage.getTriggeringPduFixedHeader().getDataFieldLength());
                    } else {
                        _bb.insertNULL();
                    }
                    _bb.insertSeparator();

                    if (cfdpIndicationMessage.getTriggeringPduFixedHeader() != null) {
                        _bb.insert(cfdpIndicationMessage.getTriggeringPduFixedHeader().getEntityIdLength());
                    } else {
                        _bb.insertNULL();
                    }
                    _bb.insertSeparator();

                    if (cfdpIndicationMessage.getTriggeringPduFixedHeader() != null) {
                        _bb.insert(cfdpIndicationMessage.getTriggeringPduFixedHeader()
                                .getTransactionSequenceNumberLength());
                    } else {
                        _bb.insertNULL();
                    }
                    _bb.insertSeparator();

                    if (cfdpIndicationMessage.getTriggeringPduFixedHeader() != null) {
                        _bb.insertLongAsUnsigned(
                                cfdpIndicationMessage.getTriggeringPduFixedHeader().getSourceEntityId());
                    } else {
                        _bb.insertNULL();
                    }
                    _bb.insertSeparator();

                    if (cfdpIndicationMessage.getTriggeringPduFixedHeader() != null) {
                        _bb.insertLongAsUnsigned(
                                cfdpIndicationMessage.getTriggeringPduFixedHeader().getTransactionSequenceNumber());
                    } else {
                        _bb.insertNULL();
                    }
                    _bb.insertSeparator();

                    if (cfdpIndicationMessage.getTriggeringPduFixedHeader() != null) {
                        _bb.insertLongAsUnsigned(
                                cfdpIndicationMessage.getTriggeringPduFixedHeader().getDestinationEntityId());
                    } else {
                        _bb.insertNULL();
                    }
                    _bb.insertSeparator();

                    _bb.insert(extractContextId(cfdpIndicationMessage.getContextKey()));
                    _bb.insertSeparator();

                    _bb.insert(extractContextHostId(cfdpIndicationMessage.getContextKey()));
                    _bb.insertTerminator();

                    // Add the line to the LDI batch
                    writeToStream(_bb);
                }

            } catch (

                    final RuntimeException re) {
                throw re;
            } catch (final Exception e) {
                throw new DatabaseException("Error inserting CfdpIndication record intodatabase", e);
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
                ICfdpIndicationMessage cfdpIndicationMessage = (ICfdpIndicationMessage) m;

                try {
                    insertMessage(cfdpIndicationMessage);
                } catch (final DatabaseException de) {
                    trace.error("LDI CfdpIndication storage failed: ", de);
                }

            }

        };

        // Subscribe to CFDP Indication Messages on the internal bus
        if (messagePublicationBus != null) {
            messagePublicationBus.subscribe(CfdpMessageType.CfdpIndication, handler);
        }

    }

    /**
     * Stop storing command messages.
     */
    @Override
    protected void stopResource() {
        super.stopResource();

        if (messagePublicationBus != null) {
            messagePublicationBus.unsubscribe(CfdpMessageType.CfdpIndication, handler);
        }

    }

}
