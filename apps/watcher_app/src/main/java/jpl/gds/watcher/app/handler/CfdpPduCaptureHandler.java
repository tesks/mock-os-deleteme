/*
 * Copyright 2006-2017. California Institute of Technology.
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
package jpl.gds.watcher.app.handler;

import java.util.HashMap;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.message.IMessage;
import jpl.gds.tm.service.api.cfdp.ICfdpPduMessage;

/**
 * The CfdpPduCaptureHandler verifies each of the queue messages are CFDP PDU messages before packing up the metadta and
 * binary data for storage and transmission
 */
public class CfdpPduCaptureHandler extends AbstractCaptureHandler<ICfdpPduMessage> {

    /**
     * Creates an instance of CfdpPduCaptureHandler
     * 
     * @param appContext
     *            the current application context
     */
    public CfdpPduCaptureHandler(final ApplicationContext appContext) {
        super(appContext, null);
    }

    // Because no templates exist for this metadata, the one line summary is always returned
    @Override
    protected String constructCsvMetadataEntry(final ICfdpPduMessage msg) {
        return msg.getOneLineSummary() + System.lineSeparator();
    }

    @Override
    protected byte[] prepDataBytes(final ICfdpPduMessage msg) {
        return msg.getPdu().getData();
    }

    /**
     * This function returns an empty map. No templates exist for representing this metadata.
     * {@inheritDoc}
     */
    @Override
    protected HashMap<String, Object> messageToMap(final ICfdpPduMessage msg) {
        return new HashMap<>();
    }

    @Override
    public ICfdpPduMessage castMessage(final IMessage msg) {
        return (ICfdpPduMessage) msg;
    }

    @Override
    protected String getMessageClassName() {
        return ICfdpPduMessage.class.toString();
    }

    @Override
    protected String getCsvHeader() {
        return "Message Info" + System.lineSeparator();
    }

}
