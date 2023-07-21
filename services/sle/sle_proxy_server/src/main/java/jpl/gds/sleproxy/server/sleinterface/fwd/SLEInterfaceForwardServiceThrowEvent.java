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

/**
 * Data structure to hold a throw event's parts: (1) Event ID and (2) qualifier
 * bytes.
 * 
 */
public class SLEInterfaceForwardServiceThrowEvent {

	/**
	 * Event ID of the throw event.
	 */
	private final int eventId;

	/**
	 * Qualifier bytes of the throw event.
	 */
	private final byte[] qualifier;

	/**
	 * Constructor.
	 * 
	 * @param eventId
	 *            Event ID of the throw event
	 * @param qualifier
	 *            Qualifier bytes of the throw event
	 */
	public SLEInterfaceForwardServiceThrowEvent(final int eventId, final byte[] qualifier) {
		this.eventId = eventId;
		this.qualifier = qualifier;
	}

	/**
	 * Returns the event ID of the throw event.
	 * 
	 * @return The event ID of the throw event
	 */
	public final int getEventId() {
		return eventId;
	}

	/**
	 * Returns the qualifier bytes of the throw event.
	 * 
	 * @return The qualifier bytes
	 */
	public final byte[] getQualifier() {
		return qualifier;
	}

}
