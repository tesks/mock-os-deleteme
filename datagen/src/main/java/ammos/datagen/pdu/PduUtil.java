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
package ammos.datagen.pdu;

import cfdp.engine.Data;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;

/**
 * PDU utilities
 * 
 *
 */
public final class PduUtil {
    /**
     * Examines the given PDU, and returns its type.
     * 
     * @param pdu PDU as Data object
     * @return PduType enum
     */
    public static PduType getType(final Data pdu) {
        byte dirCode;
        int lengthOfHeader;
        int lengthOfId;
        int lengthOfTransSum;
        PduType type;
        int value;

        type = PduType.DONT_KNOW;

        value = pdu.content[0];
        value = value & 0x10;
        if (value > 0) {
            return PduType.FILEDATA;
        }

        // It's a File Directive, but which one?
        lengthOfId = ((pdu.content[3] & 0x70) >> 4) + 1;
        lengthOfTransSum = (pdu.content[3] & 0x07) + 1;
        lengthOfHeader = 4 + lengthOfId + lengthOfTransSum + lengthOfId;
        if (pdu.length <= lengthOfHeader + 1) {
            TraceManager.getTracer(Loggers.DATAGEN).warn("Invalid PDU; length %d, hdr-len %d.",
                                                                         pdu.length, lengthOfHeader);
            System.exit(1);
            return (type);
        }

        dirCode = pdu.content[lengthOfHeader];

        if (dirCode == 4) {
            type = PduType.EOF;
        }
        else if (dirCode == 5) {
            type = PduType.FIN;
        }
        else if (dirCode == 6) {
            type = PduType.ACK;
        }
        else if (dirCode == 7) {
            type = PduType.METADATA;
        }
        else if (dirCode == 8) {
            type = PduType.NAK;
        }
        else {
            // PDU is not recognized
            TraceManager.getTracer(Loggers.DATAGEN).warn("PDU not recognized ");
        }

        return type;
    }

    private PduUtil() {

    }

}
