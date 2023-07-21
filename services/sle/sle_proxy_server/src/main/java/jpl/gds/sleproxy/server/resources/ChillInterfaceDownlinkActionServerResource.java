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
package jpl.gds.sleproxy.server.resources;

import org.restlet.data.Status;
import org.restlet.resource.ServerResource;

import jpl.gds.sleproxy.common.resources.IActionResource;
import jpl.gds.sleproxy.server.chillinterface.downlink.ChillInterfaceDownlinkManager;
import jpl.gds.sleproxy.server.chillinterface.downlink.action.EDownlinkActionType;

/**
 * Restlet resource for the chill interface "downlink/action" API.
 *
 */
public class ChillInterfaceDownlinkActionServerResource extends ServerResource implements IActionResource {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.nasa.jpl.ammos.sle_proxy_common.resources.IActionResource#accept()
	 */
	@Override
	public final void accept() {
		String actionStr = getAttribute("action");

		if (actionStr == null) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Action type missing in request");
			return;
		}

		EDownlinkActionType action = null;

		try {
			action = EDownlinkActionType.valueOf(actionStr.toUpperCase());
		} catch (IllegalArgumentException iae) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, actionStr + " is not a valid action type");
			return;
		}

		switch (action) {
		case CONNECT:

			try {
				ChillInterfaceDownlinkManager.INSTANCE.connect();
			} catch (IllegalStateException ise) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
						"chill interface downlink CONNECT failed: " + ise.getMessage());
				return;
			}

			break;
		case DISCONNECT:

			try {
				ChillInterfaceDownlinkManager.INSTANCE.disconnect();
			} catch (IllegalStateException ise) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
						"chill interface downlink DISCONNECT failed: " + ise.getMessage());
				return;
			}

			break;
		default:
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
					"Cannot handle chill interface downlink action of " + action);
		}

	}

}