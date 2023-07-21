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
package jpl.gds.tm.service.impl.cfdp;

import java.util.List;

import jpl.gds.ccsds.api.cfdp.ICfdpPdu;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.tm.service.api.cfdp.ICfdpMessageFactory;
import jpl.gds.tm.service.api.cfdp.ICfdpPduMessage;
import jpl.gds.tm.service.api.frame.ITelemetryFrameMessage;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * A factory for creation of PDU messages.
 *
 * @since R8
 *
 * MPCS-9048 - 01/08/18 - added function for packets
 * MPCS-9449 - 01/31/18 - Updated methods to reflect changes in ICfdpPduMessage contents
 * MPCS-9550 - 07/16/18 - Modified methods to take context and frame / packet objects
 *
 */
public final class CfdpMessageFactory implements ICfdpMessageFactory {


    /**
     * Constructor.
     */
    public CfdpMessageFactory() {
        // do nothing
    }


    @Override
    public ICfdpPduMessage createPduMessage(final List<ITelemetryFrameMessage> frames, final ICfdpPdu pdu,
                                            IContextConfiguration context) {
        return new CfdpPduMessage(frames, pdu, context);
    }

    @Override
    public ICfdpPduMessage createPduMessage(final ITelemetryPacketMessage packet, final ICfdpPdu pdu,
                                            IContextConfiguration context) {
        return new CfdpPduMessage(packet, pdu, context);
    }

    @Override
    public ICfdpPduMessage createSimulatorPduMessage(ITelemetryPacketMessage packet, ICfdpPdu pdu, IContextConfiguration context) {
        return new CfdpPduMessage(packet, pdu, context, true);
    }

}
