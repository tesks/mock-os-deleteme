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

package jpl.gds.cfdp.clt.action.savestate;

import static jpl.gds.cfdp.common.action.EActionCommandType.SAVE_STATE;

import jpl.gds.cfdp.clt.action.AActionClt;
import jpl.gds.cfdp.common.GenericRequest;
import jpl.gds.shared.config.GdsSystemProperties;;

public class SaveStateClt extends AActionClt {

	public SaveStateClt() {
		super(SAVE_STATE);
	}

	@Override
	public void run() {
		final GenericRequest req = new GenericRequest();
        req.setRequesterId(GdsSystemProperties.getSystemUserName());
		postAndPrint(actionType.getRelativeUri(), req);
	}

}