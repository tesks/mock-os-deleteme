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
import jpl.gds.sleproxy.server.sleinterface.fwd.SLEInterfaceForwardService;
import jpl.gds.sleproxy.server.sleinterface.fwd.action.EForwardActionType;
import jpl.gds.sleproxy.server.sleinterface.internal.config.ESLEInterfaceForwardThrowEventScheme;
import jpl.gds.sleproxy.server.sleinterface.internal.config.SLEInterfaceInternalConfigManager;
import jpl.gds.sleproxy.server.sleinterface.profile.ISLEInterfaceProfile;
import jpl.gds.sleproxy.server.sleinterface.profile.SLEInterfaceProfileManager;

/**
 * Restlet resource for the SLE interface "forward/action" API.
 * 
 */
public class SLEInterfaceForwardActionServerResource extends ServerResource implements IActionResource {

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

		EForwardActionType action = null;

		try {
			action = EForwardActionType.valueOf(actionStr.toUpperCase());
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

			ISLEInterfaceProfile profile = SLEInterfaceProfileManager.INSTANCE.getProfile(profileName);

			try {

				// In case another thread is accessing the same profile object
				synchronized (profile) {
					SLEInterfaceForwardService.INSTANCE.bind(profile);
				}

			} catch (Throwable e) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
						"BIND using profile " + profileName + " failed: " + e.getMessage());
				return;
			}

			break;
		case START:

			try {
				SLEInterfaceForwardService.INSTANCE.start();
			} catch (Throwable e) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, "START failed: " + e.getMessage());
				return;
			}

			break;
		case STOP:

			try {
				SLEInterfaceForwardService.INSTANCE.stop();
			} catch (Throwable e) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, "STOP failed: " + e.getMessage());
				return;
			}

			break;
		case UNBIND:

			try {
				SLEInterfaceForwardService.INSTANCE.unbind();
			} catch (Throwable e) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, "UNBIND failed: " + e.getMessage());
				return;
			}

			break;
		case ABORT:

			try {
				SLEInterfaceForwardService.INSTANCE.peerAbort();
			} catch (Throwable e) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, "PEER ABORT failed: " + e.getMessage());
				return;
			}

			break;
		case THROW:

			if (SLEInterfaceInternalConfigManager.INSTANCE
					.getForwardThrowEventScheme() == ESLEInterfaceForwardThrowEventScheme.DSN) {
				String newBitrate = getQuery().getValues("change-rate");
				String newModindexStr = getQuery().getValues("change-index");
				String newCommandModState = getQuery().getValues("set-command-mod");
				String newRangeModState = getQuery().getValues("set-range-mod");

				/*
				 * Check: If rate is being changed, so should index. Also vice
				 * versa.
				 */
				if ((newBitrate != null && newModindexStr == null) || (newBitrate == null && newModindexStr != null)) {
					getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
							"Both change-rate and change-index need to be provided");
					return;
				} else if (newBitrate != null && newModindexStr != null) {

					try {
						int newModindex = Integer.parseInt(newModindexStr);
						SLEInterfaceForwardService.INSTANCE.throwDSNBitrateModindexChangeEvent(newBitrate, newModindex);
					} catch (NumberFormatException nfe) {
						getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
								"change-index value " + newModindexStr + " is invalid: " + nfe.getMessage());
						return;
					} catch (IllegalArgumentException iae) {
						getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, iae.getMessage());
						return;
					} catch (IllegalStateException ise) {
						getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, ise.getMessage());
						return;
					}

				} else if (newCommandModState != null) {

					try {
						SLEInterfaceForwardService.INSTANCE.throwDSNCommandModStateChangeEvent(newCommandModState);
					} catch (IllegalArgumentException iae) {
						getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, iae.getMessage());
						return;
					} catch (IllegalStateException ise) {
						getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, ise.getMessage());
						return;
					}

				} else if (newRangeModState != null) {

					try {
						SLEInterfaceForwardService.INSTANCE.throwDSNRangeModStateChangeEvent(newRangeModState);
					} catch (IllegalArgumentException iae) {
						getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, iae.getMessage());
						return;
					} catch (IllegalStateException ise) {
						getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, ise.getMessage());
						return;
					}

				} else {
					getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "No acceptable query parameter specified");
					return;
				}

			} else {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
						"chill_sle_proxy's SLE interface forward service has invalid throw event scheme configured: "
								+ SLEInterfaceInternalConfigManager.INSTANCE.getForwardThrowEventScheme());
				return;
			}

			break;
		default:
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
					"Cannot handle SLE interface forward service action of " + action);
		}

	}

}