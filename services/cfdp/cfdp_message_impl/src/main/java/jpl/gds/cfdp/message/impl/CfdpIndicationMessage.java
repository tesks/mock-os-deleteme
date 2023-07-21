/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.cfdp.message.impl;

import com.google.protobuf.InvalidProtocolBufferException;
import jpl.gds.cfdp.data.api.*;
import jpl.gds.cfdp.message.api.CfdpMessageType;
import jpl.gds.cfdp.message.api.ICfdpIndicationMessage;
import jpl.gds.serialization.cfdp_message_impl.Proto3CfdpIndicationMessage;
import jpl.gds.serialization.cfdp_message_impl.Proto3CfdpIndicationMessage.Proto3CfdpFaultConditionEnum;
import jpl.gds.serialization.cfdp_message_impl.Proto3CfdpIndicationMessage.Proto3CfdpIndicationTypeEnum;
import jpl.gds.serialization.cfdp_message_impl.Proto3CfdpIndicationMessage.Proto3CfdpTransactionDirectionEnum;
import jpl.gds.serialization.cfdp_message_impl.Proto3CfdpIndicationMessage.Proto3CfdpTriggeredByTypeEnum;
import jpl.gds.serialization.messages.Proto3AbstractMessage;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.message.BaseBinaryMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.MessageRegistry;
import org.springframework.context.ApplicationContext;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.util.List;

import static jpl.gds.cfdp.data.api.ECfdpIndicationType.*;

/**
 * Class CfdpIndicationMessage
 * @since R8
 */
public class CfdpIndicationMessage extends ACfdpMessage implements ICfdpIndicationMessage {

    private ECfdpIndicationType indicationType;
    private ICfdpCondition condition;
    private ECfdpTransactionDirection transactionDirection;
    private long sourceEntityId;
    private long transactionSequenceNumber;
    private byte serviceClass;
    private long destinationEntityId;

    private boolean involvesFileTransfer;

    /* Below used for transactions involving file transfer */
    private long totalBytesSentOrReceived;

    private ECfdpTriggeredByType triggeringType;

    private String pduId;
    private FixedPduHeader triggeringPduFixedHeader;
    // (file-directive-pdu-def | file-data-pdu-def)
    // context-def

    public CfdpIndicationMessage(final ApplicationContext appContext) {
        super(CfdpMessageType.CfdpIndication, appContext);
    }

    public CfdpIndicationMessage(final ApplicationContext appContext, final Proto3CfdpIndicationMessage msg)
            throws InvalidProtocolBufferException {
        super(CfdpMessageType.CfdpIndication, appContext, msg.getSuper(), msg.getHeader());

        if (msg.getType() != null) {
            indicationType = ECfdpIndicationType.valueOf(msg.getType().name());
        }

        if (msg.getCondition() != null) {

            try {
                condition = ECfdpFaultCondition.valueOf(msg.getCondition().name());
            } catch (final IllegalArgumentException iae1) {

                try {
                    condition = ECfdpNonFaultCondition.valueOf(msg.getCondition().name());
                } catch (final IllegalArgumentException iae2) {
                    // Weird. But do nothing. It'll just remain null.
                }

            }

        }

        if (msg.getTransactionDirection() != null) {
            transactionDirection = ECfdpTransactionDirection.valueOf(msg.getTransactionDirection().name());

        }

        sourceEntityId = msg.getSourceEntityId();
        transactionSequenceNumber = msg.getTransactionSequenceNumber();
        serviceClass = Byte.parseByte(msg.getServiceClass());
        destinationEntityId = msg.getDestinationEntityId();
        involvesFileTransfer = msg.getInvolvesFileTransfer();
        totalBytesSentOrReceived = msg.getTotalBytesSentOrReceived();

        if (msg.getTriggeringType() != null) {
            triggeringType = ECfdpTriggeredByType.valueOf(msg.getTriggeringType().name());
        }

        pduId = msg.getPduId();
        triggeringPduFixedHeader = populateFixedPduHeader(msg.getTriggeringPduFixedHeader());
    }

    @Override
    public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));
    }

    @Override
    public String getOneLineSummary() {
        return "CfdpIndication " + indicationType
                + (indicationType == ECfdpIndicationType.FAULT ? "(" + condition + "): " : ": ")
                + super.getOneLineSummary() + ", TxDirection=" + transactionDirection + ", SourceEntity="
                + Long.toUnsignedString(sourceEntityId) + ", TxSeqNumber="
                + Long.toUnsignedString(transactionSequenceNumber) + ", ServiceClass=" + serviceClass
                + ", DestinationEntity=" + Long.toUnsignedString(destinationEntityId) + ", FileTransfer="
                + involvesFileTransfer
                + (transactionDirection == ECfdpTransactionDirection.OUT ? ", FileBytesSent=" : ", FileBytesReceived=")
                + Long.toUnsignedString(totalBytesSentOrReceived);
    }

    @Override
    public Proto3CfdpIndicationMessage build() {
        final Proto3CfdpIndicationMessage.Builder builder = Proto3CfdpIndicationMessage.newBuilder();
        builder.setSuper((Proto3AbstractMessage) super.build());
        builder.setHeader(super.getBuilder());

        if (indicationType != null) {
            builder.setType(Proto3CfdpIndicationTypeEnum.valueOf(indicationType.name()));
        }

        if (condition != null) {
            builder.setCondition(Proto3CfdpFaultConditionEnum.valueOf(condition.toString()));
        }

        if (transactionDirection != null) {
            builder.setTransactionDirection(Proto3CfdpTransactionDirectionEnum.valueOf(transactionDirection.name()));
        }

        builder.setSourceEntityId(sourceEntityId).setTransactionSequenceNumber(transactionSequenceNumber)
                .setServiceClass(Byte.toString(serviceClass)).setDestinationEntityId(destinationEntityId)
                .setInvolvesFileTransfer(involvesFileTransfer).setTotalBytesSentOrReceived(totalBytesSentOrReceived);

        if (triggeringType != null) {
            builder.setTriggeringType(Proto3CfdpTriggeredByTypeEnum.valueOf(triggeringType.name()));
        }

        builder.setPduId(pduId);
        builder.setTriggeringPduFixedHeader(build(triggeringPduFixedHeader));
        return builder.build();
    }

    /**
     * Gets the escaped CSV text form of the object.
     *
     * @return CSV string
     */
    @Override
    public String getEscapedCsv() {

        if (indicationType != TRANSACTION
                && indicationType != NEW_TRANSACTION_DETECTED
                && indicationType != FAULT
                && indicationType != TRANSACTION_FINISHED
                && indicationType != ABANDONED) {
            /*
             * Only those indication types that jpl.gds.automation.mtak.MtakDownlinkServerApp will
             * accept are required to have valid escaped CSV.
             */
            return null;
        }

        final StringBuilder sb = new StringBuilder(256);

        sb.append("cfdp-ind");
        sb.append(CSV_SEPARATOR);
        sb.append(indicationType.getMtakCsvKeyword());
        sb.append(CSV_SEPARATOR);
        sb.append(getEventTimeString());
        sb.append(CSV_SEPARATOR);
        sb.append(Long.toUnsignedString(getSourceEntityId()));
        sb.append(CSV_SEPARATOR);
        sb.append(Long.toUnsignedString(getTransactionSequenceNumber()));

        return (sb.toString());
    }

    /**
     * BinaryParseHandler is the message-specific protobuf parse handler for
     * creating this Message from its binary representation.
     */
    public static class BinaryParseHandler extends BaseBinaryMessageParseHandler {

        private final ApplicationContext appContext;

        /**
         * Constructor.
         *
         * @param context current application context
         */
        public BinaryParseHandler(final ApplicationContext context) {
            this.appContext = context;
        }

        @Override
        public IMessage[] parse(final List<byte[]> content) throws IOException {

            for (final byte[] messageBytes : content) {
                final Proto3CfdpIndicationMessage protoCfdpIndicationMessage = Proto3CfdpIndicationMessage
                        .parseFrom(messageBytes);
                final ICfdpIndicationMessage cfdpIndicationMesage = appContext.getBean(ICfdpIndicationMessage.class,
                        protoCfdpIndicationMessage);
                addMessage(cfdpIndicationMesage);
            }

            return getMessages();
        }

    }

    @Override
    public ECfdpIndicationType getIndicationType() {
        return indicationType;
    }

    @Override
    public ECfdpTransactionDirection getTransactionDirection() {
        return transactionDirection;
    }

    @Override
    public long getSourceEntityId() {
        return sourceEntityId;
    }

    @Override
    public long getTransactionSequenceNumber() {
        return transactionSequenceNumber;
    }

    @Override
    public byte getServiceClass() {
        return serviceClass;
    }

    @Override
    public long getDestinationEntityId() {
        return destinationEntityId;
    }

    @Override
    public boolean getInvolvesFileTransfer() {
        return involvesFileTransfer;
    }

    @Override
    public long getTotalBytesSentOrReceived() {
        return totalBytesSentOrReceived;
    }

    @Override
    public ECfdpTriggeredByType getTriggeringType() {
        return triggeringType;
    }

    @Override
    public String getPduId() {
        return pduId;
    }

    @Override
    public FixedPduHeader getTriggeringPduFixedHeader() {
        return triggeringPduFixedHeader;
    }

    @Override
    public ICfdpIndicationMessage setIndicationType(final ECfdpIndicationType indicationType) {
        this.indicationType = indicationType;
        return this;
    }

    @Override
    public ICfdpIndicationMessage setTransactionDirection(final ECfdpTransactionDirection transactionDirection) {
        this.transactionDirection = transactionDirection;
        return this;
    }

    @Override
    public ICfdpIndicationMessage setSourceEntityId(final long sourceEntityId) {
        this.sourceEntityId = sourceEntityId;
        return this;
    }

    @Override
    public ICfdpIndicationMessage setTransactionSequenceNumber(final long transactionSequenceNumber) {
        this.transactionSequenceNumber = transactionSequenceNumber;
        return this;
    }

    @Override
    public ICfdpIndicationMessage setServiceClass(final byte serviceClass) {
        this.serviceClass = serviceClass;
        return this;
    }

    @Override
    public ICfdpIndicationMessage setDestinationEntityId(final long destinationEntityId) {
        this.destinationEntityId = destinationEntityId;
        return this;
    }

    @Override
    public ICfdpIndicationMessage setInvolvesFileTransfer(final boolean involvesFileTransfer) {
        this.involvesFileTransfer = involvesFileTransfer;
        return this;
    }

    @Override
    public ICfdpIndicationMessage setTotalBytesSentOrReceived(final long totalBytesSentOrReceived) {
        this.totalBytesSentOrReceived = totalBytesSentOrReceived;
        return this;
    }

    @Override
    public ICfdpIndicationMessage setTriggeringType(final ECfdpTriggeredByType triggeringType) {
        this.triggeringType = triggeringType;
        return this;
    }

    @Override
    public ICfdpIndicationMessage setPduId(final String pduId) {
        this.pduId = pduId;
        return this;
    }

    @Override
    public ICfdpIndicationMessage setTriggeringPduFixedHeader(final FixedPduHeader triggeringPduFixedHeader) {
        this.triggeringPduFixedHeader = triggeringPduFixedHeader;
        return this;
    }

    @Override
    public ICfdpCondition getCondition() {
        return condition;
    }

    @Override
    public ICfdpIndicationMessage setCondition(final ICfdpCondition condition) {
        this.condition = condition;
        return this;
    }

}
