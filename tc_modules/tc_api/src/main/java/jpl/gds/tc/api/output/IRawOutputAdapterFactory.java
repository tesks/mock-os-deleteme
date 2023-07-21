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
package jpl.gds.tc.api.output;

import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.tc.api.exception.RawOutputException;

public interface IRawOutputAdapterFactory {

    /**
     * Creates and returns an uplink output adapter for the given connection
     * type.
     *
     * @param connType the UplinkConnectionType to get an adapter for
     * 
     * @return RawOutput object
     *
     * @throws RawOutputException
     *             if no output adapter could be created for the current
     *             connection type
     */
    IRawOutputAdapter getUplinkOutput(UplinkConnectionType connType)
            throws RawOutputException;

}