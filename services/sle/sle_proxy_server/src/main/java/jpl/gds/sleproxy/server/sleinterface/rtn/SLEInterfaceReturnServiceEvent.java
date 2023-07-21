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
package jpl.gds.sleproxy.server.sleinterface.rtn;

import java.util.List;

import com.lsespace.sle.user.service.ReturnStatusReport;
import com.lsespace.sle.user.service.SLEUserServiceInstance;
import com.lsespace.sle.user.service.UserServiceState;
import com.lsespace.sle.user.service.data.TransferBufferData;

/**
 * An individual event received from the SLE return service.
 * 
 * This single class is a union of all the fields that are possible by the SLE
 * return service event. So for each individual event, only some of the fields
 * will be valid.
 * 
 */
public class SLEInterfaceReturnServiceEvent {

	/**
	 * Enumerates the different types of events possible.
	 * 
	 */
	enum Type {

		/**
		 * Start event type.
		 */
		START,

		/**
		 * Stop event type.
		 */
		STOP,

		/**
		 * State change event type.
		 */
		STATE_CHANGE,

		/**
		 * Transfer buffer event type.
		 */
		TRANSFER_BUFFER,

		/**
		 * Status report event type.
		 */
		STATUS_REPORT;
	};

	/**
	 * The event type.
	 */
	private final Type type;

	/**
	 * The underlying SLE RAF or RCF service.
	 */
	private final SLEUserServiceInstance service;

	/**
	 * User service state received.
	 */
	private final UserServiceState state;

	/**
	 * Number of frames in the transfer buffer event.
	 */
	private final int numFrames;

	/**
	 * Number of good frames in the transfer buffer event.
	 */
	private final int numGoodFrames;

	/**
	 * Actual data of the transfer buffer event.
	 */
	private final List<TransferBufferData> data;

	/**
	 * The RAF or RCF service status report received.
	 */
	private final ReturnStatusReport report;

	/**
	 * Construct a new event based on just the type and the RAF or RCF service.
	 * 
	 * @param type
	 *            The event type
	 * @param service
	 *            The SLE RAF or RCF service
	 */
	SLEInterfaceReturnServiceEvent(final Type type, final SLEUserServiceInstance service) {
		this.type = type;
		this.service = service;
		state = null;
		numFrames = -1;
		numGoodFrames = -1;
		data = null;
		report = null;
	}

	/**
	 * Construct a new state change event.
	 * 
	 * @param type
	 *            The event type
	 * @param service
	 *            The SLE RAF or RCF service
	 * @param state
	 *            The new state received
	 */
	SLEInterfaceReturnServiceEvent(final Type type, final SLEUserServiceInstance service,
			final UserServiceState state) {
		this.type = type;
		this.service = service;
		this.state = state;
		numFrames = -1;
		numGoodFrames = -1;
		data = null;
		report = null;
	}

	/**
	 * Construct a new transfer buffer event.
	 * 
	 * @param type
	 *            The event type
	 * @param service
	 *            The SLE RAF or RCF service
	 * @param numFrames
	 *            Number of frames in the transfer buffer event
	 * @param numGoodFrames
	 *            Number of good frames in the transfer buffer event
	 * @param data
	 *            Actual data of the transfer buffer event
	 */
	SLEInterfaceReturnServiceEvent(final Type type, final SLEUserServiceInstance service, final int numFrames,
			final int numGoodFrames, final List<TransferBufferData> data) {
		this.type = type;
		this.service = service;
		state = null;
		this.numFrames = numFrames;
		this.numGoodFrames = numGoodFrames;
		this.data = data;
		report = null;
	}

	/**
	 * Construct a new status report event.
	 * 
	 * @param type
	 *            The event type
	 * @param service
	 *            The SLE RAF or RCF service
	 * @param report
	 *            The status report received
	 */
	SLEInterfaceReturnServiceEvent(final Type type, final SLEUserServiceInstance service,
			final ReturnStatusReport report) {
		this.type = type;
		this.service = service;
		state = null;
		numFrames = -1;
		numGoodFrames = -1;
		data = null;
		this.report = report;
	}

	/**
	 * Get the underlying SLE RAF or RCF service instance.
	 * 
	 * @return The RAF or RCF service instance
	 */
	public final Type getType() {
		return type;
	}

	/**
	 * Get the underlying SLE RAF or RCF service instance.
	 * 
	 * @return The RAF or RCF service instance
	 */
	public final SLEUserServiceInstance getService() {
		return service;
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
	 * Get the number of frames.
	 * 
	 * @return The number of frames
	 */
	public final int getNumFrames() {
		return numFrames;
	}

	/**
	 * Get the number of good frames.
	 * 
	 * @return The number of good frames
	 */
	public final int getNumGoodFrames() {
		return numGoodFrames;
	}

	/**
	 * Get the transfer buffer data.
	 * 
	 * @return The data
	 */
	public final List<TransferBufferData> getData() {
		return data;
	}

	/**
	 * Get the status report.
	 * 
	 * @return The status report
	 */
	public final ReturnStatusReport getReport() {
		return report;
	}

}