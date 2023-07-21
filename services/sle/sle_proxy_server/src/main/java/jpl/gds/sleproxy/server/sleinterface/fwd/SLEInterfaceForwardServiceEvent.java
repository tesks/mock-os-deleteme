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

import com.lsespace.sle.user.service.FcltuStatusReport;
import com.lsespace.sle.user.service.SLEUserFcltuInstance;
import com.lsespace.sle.user.service.UserServiceState;
import com.lsespace.sle.user.service.data.FcltuAsyncNotification;

/**
 * An individual event received from the SLE foward service.
 * 
 * This single class is a union of all the fields that are possible by the SLE
 * forward service event. So for each individual event, only some of the fields
 * will be valid.
 * 
 */
public class SLEInterfaceForwardServiceEvent {

	/**
	 * Enumerates the different types of events possible.
	 * 
	 */
	enum Type {

		/**
		 * Asynchronous notification event type.
		 */
		ASYNC_NOTIFICATION,

		/**
		 * Start event type.
		 */
		START,

		/**
		 * State change event type.
		 */
		STATE_CHANGE,

		/**
		 * Status report event type.
		 */
		STATUS_REPORT,

		/**
		 * Stop event type.
		 */
		STOP;
	};

	/**
	 * The event type.
	 */
	private final Type type;

	/**
	 * The underlying SLE FCLTU service.
	 */
	private final SLEUserFcltuInstance service;

	/**
	 * The notification received through the SLE FCLTU service.
	 */
	private final FcltuAsyncNotification notif;

	/**
	 * User service state received.
	 */
	private final UserServiceState state;

	/**
	 * The FCLTU service status report received.
	 */
	private final FcltuStatusReport report;

	/**
	 * Construct a new event based on just the type and the FCLTU service.
	 * 
	 * @param type
	 *            The event type
	 * @param service
	 *            The SLE FCLTU service
	 */
	SLEInterfaceForwardServiceEvent(final Type type, final SLEUserFcltuInstance service) {
		this.type = type;
		this.service = service;
		notif = null;
		state = null;
		report = null;
	}

	/**
	 * Construct a new event for the FCLTU async notification.
	 * 
	 * @param type
	 *            The event type
	 * @param service
	 *            The SLE FCLTU service
	 * @param notif
	 *            The asynchronous notification received
	 */
	SLEInterfaceForwardServiceEvent(final Type type, final SLEUserFcltuInstance service,
			final FcltuAsyncNotification notif) {
		this.type = type;
		this.service = service;
		this.notif = notif;
		state = null;
		report = null;
	}

	/**
	 * Construct a new state change event.
	 * 
	 * @param type
	 *            The event type
	 * @param service
	 *            The SLE FCLTU service
	 * @param state
	 *            The new state received
	 */
	SLEInterfaceForwardServiceEvent(final Type type, final SLEUserFcltuInstance service, final UserServiceState state) {
		this.type = type;
		this.service = service;
		notif = null;
		this.state = state;
		report = null;
	}

	/**
	 * Construct a new status report event.
	 * 
	 * @param type
	 *            The event type
	 * @param service
	 *            The SLE FCLTU service
	 * @param report
	 *            The status report received
	 */
	SLEInterfaceForwardServiceEvent(final Type type, final SLEUserFcltuInstance service,
			final FcltuStatusReport report) {
		this.type = type;
		this.service = service;
		notif = null;
		state = null;
		this.report = report;
	}

	/**
	 * Get the type of the event.
	 * 
	 * @return The event type
	 */
	public final Type getType() {
		return type;
	}

	/**
	 * Get the underlying SLE FCLTU service instance.
	 * 
	 * @return The FCLTU service instance
	 */
	public final SLEUserFcltuInstance getService() {
		return service;
	}

	/**
	 * Get the asynchronous notification.
	 * 
	 * @return The async notification
	 */
	public final FcltuAsyncNotification getNotif() {
		return notif;
	}

	/**
	 * Get the service state.
	 * 
	 * @return The service state
	 */
	public final UserServiceState getState() {
		return state;
	}

	/**
	 * Get the status report.
	 * 
	 * @return The status report
	 */
	public final FcltuStatusReport getReport() {
		return report;
	}

}