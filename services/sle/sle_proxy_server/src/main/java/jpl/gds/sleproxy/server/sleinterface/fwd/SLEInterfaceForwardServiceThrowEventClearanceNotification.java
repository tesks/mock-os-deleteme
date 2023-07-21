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
package jpl.gds.sleproxy.server.sleinterface.fwd;

import com.lsespace.sle.user.service.data.FcltuAsyncNotificationType;

/**
 * Data structure to hold a throw event clearance notification's parts: (1)
 * Event thrown ID and (2) notification type.
 * 
 */
public class SLEInterfaceForwardServiceThrowEventClearanceNotification {

	/**
	 * ID of the thrown event.
	 */
	private final long eventThrowId;

	/**
	 * Notification type.
	 */
	private final FcltuAsyncNotificationType notificationType;

	/**
	 * Constructor.
	 * 
	 * @param eventThrowId
	 *            ID of the thrown event
	 * @param notificationType
	 *            Notification type
	 */
	public SLEInterfaceForwardServiceThrowEventClearanceNotification(final long eventThrowId,
			final FcltuAsyncNotificationType notificationType) {
		this.eventThrowId = eventThrowId;
		this.notificationType = notificationType;
	}

	/**
	 * Returns the ID of the thrown event.
	 * 
	 * @return The ID of the thrown event
	 */
	public final long getEventThrowId() {
		return eventThrowId;
	}

	/**
	 * Returns the notification type of the thrown event.
	 * 
	 * @return The notification type
	 */
	public final FcltuAsyncNotificationType getNotificationType() {
		return notificationType;
	}

}