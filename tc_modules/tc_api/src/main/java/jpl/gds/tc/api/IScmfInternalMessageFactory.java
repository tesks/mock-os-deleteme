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

package jpl.gds.tc.api;

import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.exception.UnblockException;
import jpl.gds.tc.api.message.IUplinkMessage;

import java.util.List;

/**
 * Factory interface for creating internal uplink messages from an SCMF
 *
 */
public interface IScmfInternalMessageFactory {

    /**
     * Create internal uplink messages for an SCMF
     * @param scmf input SCMF
     * @return internal uplink messages
     * @throws CltuEndecException an issue with CLTU format
     * @throws UnblockException an issue with binary data conversion
     */
    List<IUplinkMessage> createInternalUplinkMessages(IScmf scmf) throws CltuEndecException, UnblockException;
}
