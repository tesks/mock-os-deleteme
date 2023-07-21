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
package jpl.gds.tm.service.impl.packet;

import java.util.Map;

import jpl.gds.shared.holders.FrameIdHolder;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.tm.service.api.packet.IPacketMessageFactory;
import jpl.gds.tm.service.api.packet.IPacketSummaryMessage;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;
import jpl.gds.tm.service.api.packet.PacketSummaryRecord;

/**
 * A static factory for creating instances of IPacketMessage.
 * 
 *
 * MPCS0266 - 12/14/18. Updated packet summary for CFDP tracking
 *
 */
public class PacketMessageFactory implements IPacketMessageFactory {

    /**
     * Private constructor to enforce static nature.
     */
    public PacketMessageFactory() {
        SystemUtilities.doNothing();
    }

    @Override
    public ITelemetryPacketMessage createTelemetryPacketMessage(final ITelemetryPacketInfo info) {
        return new TelemetryPacketMessage(info);
    }

    @Override
    public ITelemetryPacketMessage createTelemetryPacketMessage(final ITelemetryPacketInfo    info,
                                        final PacketIdHolder id,
                                        final HeaderHolder   header,
                                        final TrailerHolder  trailer,
                                        final FrameIdHolder  fid) {
        return new TelemetryPacketMessage(info, id, header, trailer, fid);
    }

    @Override
    public IPacketSummaryMessage createPacketSummaryMessage(final long numGaps,
            final long numRegressions, final long numRepeats, final long numFill,
            final long numInvalid, final long numValid, final long numStation, final long numCfdp,
            final Map<String, PacketSummaryRecord> summaries) {
        
        return new PacketSummaryMessage(numGaps, numRegressions, numRepeats, numFill, numInvalid, numValid, numStation,
                                        numCfdp, summaries);
    }
}
