/*
 * Copyright 2006-2022. California Institute of Technology.
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

package ammos.datagen.pdu.cfdp;

import cfdp.engine.ampcs.IMetadataConfigurationLookup;

/**
 *  MetadataConfigurationLookup for datagen, that does not depend on other beans
 *  only defined for the CFDP processor app
 *
 */
public class DatagenMetadataConfigurationLookup implements IMetadataConfigurationLookup {
    @Override
    public boolean getDownlinkMetadataIncludePduLogEnabledProperty() {
        return false;
    }

    @Override
    public boolean getUplinkMetadataIncludePduLogEnabledProperty() {
        return false;
    }
}
