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
import jpl.gds.cfdp.data.api.ECfdpPduDirection;
import jpl.gds.cfdp.data.api.ECfdpPduType;
import jpl.gds.cfdp.data.api.ECfdpTransmissionMode;
import jpl.gds.cfdp.data.api.FixedPduHeader;
import jpl.gds.cfdp.message.api.CfdpMessageType;
import jpl.gds.cfdp.message.api.ICfdpMessage;
import jpl.gds.cfdp.message.api.ICfdpMessageHeader;
import jpl.gds.serialization.cfdp_message_impl.Proto3CfdpMessageHeader;
import jpl.gds.serialization.cfdp_message_impl.Proto3FixedPduHeader;
import jpl.gds.serialization.cfdp_message_impl.Proto3FixedPduHeader.Proto3CfdpPduDirectionEnum;
import jpl.gds.serialization.cfdp_message_impl.Proto3FixedPduHeader.Proto3CfdpPduTypeEnum;
import jpl.gds.serialization.cfdp_message_impl.Proto3FixedPduHeader.Proto3CfdpTransmissionModeEnum;
import jpl.gds.serialization.messages.Proto3AbstractMessage;
import jpl.gds.shared.message.Message;
import org.springframework.context.ApplicationContext;

public abstract class ACfdpMessage extends Message implements ICfdpMessage {

    protected final ApplicationContext appContext;

    private final ICfdpMessageHeader header;

    public ACfdpMessage(final CfdpMessageType cfdpMessageType, final ApplicationContext appContext) {
		super(cfdpMessageType, System.currentTimeMillis());
        this.appContext = appContext;
        header = appContext.getBean(ICfdpMessageHeader.class);
    }

    public ACfdpMessage(final CfdpMessageType cfdpMessageType, final ApplicationContext appContext,
                        final Proto3AbstractMessage abstractMsg, final Proto3CfdpMessageHeader cfdpMessageHeader)
            throws InvalidProtocolBufferException {
        super(cfdpMessageType, abstractMsg);
        this.appContext = appContext;
        header = appContext.getBean(ICfdpMessageHeader.class);

        if (cfdpMessageHeader != null) {
            header.setCfdpProcessorInstanceId(cfdpMessageHeader.getCfdpProcessorInstanceId());
        }

    }

    @Override
    public String getOneLineSummary() {
        String summary = "CFDPProcessorInstanceId=" + header.getCfdpProcessorInstanceId();
        return summary;
    }

    protected Proto3CfdpMessageHeader.Builder getBuilder() {
        Proto3CfdpMessageHeader.Builder headerBuilder = null;

        if (header != null) {
            headerBuilder = Proto3CfdpMessageHeader.newBuilder();
            headerBuilder.setCfdpProcessorInstanceId(header.getCfdpProcessorInstanceId());
        }

        return headerBuilder;
    }

    @Override
    public ICfdpMessageHeader getHeader() {
        return header;
    }

    protected FixedPduHeader populateFixedPduHeader(final Proto3FixedPduHeader h) {
        FixedPduHeader fixedPduHeader = null;

        if (h != null) {
            fixedPduHeader = new FixedPduHeader();

            try {
                fixedPduHeader.setVersion(Byte.parseByte(h.getVersion()));
            } catch (NumberFormatException nfe) {
                // Do nothing
            }

            if (h.getType() != null) {
                fixedPduHeader.setType(ECfdpPduType.valueOf(h.getType().name()));
            }

            if (h.getDirection() != null) {
                fixedPduHeader.setDirection(ECfdpPduDirection.valueOf(h.getDirection().name()));
            }

            if (h.getTransmissionMode() != null) {
                fixedPduHeader.setTransmissionMode(ECfdpTransmissionMode.valueOf(h.getTransmissionMode().name()));
            }

            fixedPduHeader.setCrcFlagPresent(h.getCrcFlagPresent());
            fixedPduHeader.setDataFieldLength((short) h.getDataFieldLength());
            fixedPduHeader.setEntityIdLength((byte) h.getEntityIdLength());
            fixedPduHeader.setTransactionSequenceNumberLength((byte) h.getTransactionSequenceNumberLength());
            fixedPduHeader.setSourceEntityId(h.getSourceEntityId());
            fixedPduHeader.setTransactionSequenceNumber(h.getTransactionSequenceNumber());
            fixedPduHeader.setDestinationEntityId(h.getDestinationEntityId());
        }

        return fixedPduHeader;
    }

    protected Proto3FixedPduHeader.Builder build(final FixedPduHeader h) {
        Proto3FixedPduHeader.Builder builder = Proto3FixedPduHeader.newBuilder();

        if (h != null) {
            builder.setVersion(Byte.toString(h.getVersion()));

            if (h.getType() != null) {
                builder.setType(Proto3CfdpPduTypeEnum.valueOf(h.getType().name()));
            }

            if (h.getDirection() != null) {
                builder.setDirection(
                        Proto3CfdpPduDirectionEnum.valueOf(h.getDirection().name()));
            }

            if (h.getTransmissionMode() != null) {
                builder.setTransmissionMode(
                        Proto3CfdpTransmissionModeEnum.valueOf(h.getTransmissionMode().name()));
            }

            builder.setCrcFlagPresent(h.isCrcFlagPresent())
                    .setDataFieldLength(h.getDataFieldLength())
                    .setEntityIdLength(h.getEntityIdLength())
                    .setTransactionSequenceNumberLength(h.getTransactionSequenceNumberLength())
                    .setSourceEntityId(h.getSourceEntityId())
                    .setTransactionSequenceNumber(h.getTransactionSequenceNumber())
                    .setDestinationEntityId(h.getDestinationEntityId());
        }

        return builder;
    }

    @Override
    public String toString() {
        return getOneLineSummary();
    }

}
