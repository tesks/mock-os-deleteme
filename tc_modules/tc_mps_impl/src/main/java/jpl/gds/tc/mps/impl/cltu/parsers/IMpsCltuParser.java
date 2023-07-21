/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.tc.mps.impl.cltu.parsers;

import gov.nasa.jpl.tcsession.TcSession;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.cltu.ICltuParser;
import jpl.gds.tc.api.exception.CltuEndecException;

public interface IMpsCltuParser extends ICltuParser {

    /**
     * Parse a CLTU from an MPS CLTU item
     *
     * @param cltuItem MPS CLTU item
     * @return CLTU object
     */
    ICltu parse(TcSession.cltuitem cltuItem) throws CltuEndecException;

}
