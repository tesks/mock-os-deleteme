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
import jpl.gds.sleproxy.server.sleinterface.internal.config.SLEInterfaceInternalConfigManager;
import jpl.gds.sleproxy.server.sleinterface.profile.ReturnSLEInterfaceProfile;
import jpl.gds.sleproxy.server.sleinterface.profile.SLEInterfaceProfileManager;
import jpl.gds.sleproxy.server.sleinterface.rtn.SLEInterfaceReturnService;
import jpl.gds.sleproxy.server.sleinterface.rtn.action.EReturnActionType;

/**
 * Restlet resource for the SLE interface "return/action" API.
 * 
 *
 */
public class SLEInterfaceReturnActionServerResource extends ServerResource implements IActionResource {

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

		EReturnActionType action = null;

		try {
			action = EReturnActionType.valueOf(actionStr.toUpperCase());
		} catch (IllegalArgumentException iae) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, actionStr + " is not a valid action type");
			return;
		}

		switch (action) {
		case BIND:
			String profileName = getQuery().getValues("profile");

			if (profileName == null) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Profile name is missing in request");
				return;
			}

			if (!SLEInterfaceProfileManager.INSTANCE.contains(profileName)) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Profile " + profileName + " does not exist");
				return;
			}

			ReturnSLEInterfaceProfile profile = (ReturnSLEInterfaceProfile) SLEInterfaceProfileManager.INSTANCE
					.getProfile(profileName);

			try {

				// In case another thread is accessing the same profile object
				synchronized (profile) {
					// We have selected a SLE interface profile
					SLEInterfaceReturnService.INSTANCE.bind(profile);
				}

			} catch (Throwable e) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
						"BIND using profile " + profileName + " failed: " + e.getMessage());
				return;
			}

			break;
		case START:

			try {
				SLEInterfaceReturnService.INSTANCE.start();
			} catch (Throwable e) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, "START failed: " + e.getMessage());
				return;
			}

			break;
		case STOP:

			try {
				SLEInterfaceReturnService.INSTANCE.stop();
			} catch (Throwable e) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, "STOP failed: " + e.getMessage());
				return;
			}

			break;
		case UNBIND:

			try {
				SLEInterfaceReturnService.INSTANCE.unbind();
			} catch (Throwable e) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, "UNBIND failed: " + e.getMessage());
				return;
			}

			break;
		case ABORT:

			try {
				SLEInterfaceReturnService.INSTANCE.peerAbort();
			} catch (Throwable e) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, "PEER ABORT failed: " + e.getMessage());
				return;
			}

			break;
		default:
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
					"Cannot handle SLE interface return service action of " + action);
		}

	}

}