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
package jpl.gds.cfdp.clt.action.clear;

import jpl.gds.cfdp.clt.action.AActionClt;
import jpl.gds.cfdp.common.GenericRequest;
import jpl.gds.shared.config.GdsSystemProperties;

import static jpl.gds.cfdp.common.action.EActionCommandType.CLEAR;

/**
 * {@code ClearClt} is the CFDP command-line tool's Clear subcommand handler.
 *
 * @since 8.2.0
 */
public class ClearClt extends AActionClt {

	/**
	 * Constructor
	 */
    public ClearClt() {
        super(CLEAR);
    }

    @Override
    public void run() {
        final GenericRequest req = new GenericRequest();
        req.setRequesterId(GdsSystemProperties.getSystemUserName());
        postAndPrint(actionType.getRelativeUri(), req);
    }

}