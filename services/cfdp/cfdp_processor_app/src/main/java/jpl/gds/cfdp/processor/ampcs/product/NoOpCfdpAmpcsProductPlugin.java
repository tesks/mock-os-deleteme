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
package jpl.gds.cfdp.processor.ampcs.product;

import cfdp.engine.TransStatus;
import cfdp.engine.ampcs.ICfdpAmpcsProductPlugin;
import org.springframework.stereotype.Service;

/**
 * Class {@code NoOpCfdpAmpcsProductPlugin} is is no-operation AMPCS Products Plugin. When CFDP Processor uses this class to produce AMPCS Products-related artifacts, nothing is produced.
 *
 * @since CFDP R3
 */
@Service
public class NoOpCfdpAmpcsProductPlugin implements ICfdpAmpcsProductPlugin {

    @Override
    public void productGeneration(final TransStatus status, final String effectiveMetadataFilename) {
        // Empty stub
    }

    @Override
    public void publishProductStartedMessage(TransStatus transStatus) {
        // Empty stub
    }

}
