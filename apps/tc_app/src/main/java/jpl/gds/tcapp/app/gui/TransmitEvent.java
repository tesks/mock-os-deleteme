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
package jpl.gds.tcapp.app.gui;

import jpl.gds.tc.api.ISendCompositeState;

public class TransmitEvent {
	private AbstractUplinkComposite transmitter;
	private ISendCompositeState transmitState;
	private boolean success;
	private long timestamp;
	private String message;

	public TransmitEvent() {
		//default
	}
	
	/**
	 * Constructor
	 * 
	 * @param transmitter the name of the widget that transmitted
	 * @param transmitItem the name of the item that was transmitted
	 */
	public TransmitEvent(final AbstractUplinkComposite transmitter, final ISendCompositeState transmitState, final boolean success, final String message) {
		this.transmitter = transmitter;
		this.transmitState = transmitState;
		this.success = success;
		this.timestamp = System.currentTimeMillis();
		this.message = message;
	}
	
	/**
	 * Sets all member variables
	 */
	public void setTransmitInfo(final AbstractUplinkComposite transmitter, final ISendCompositeState transmitState, final boolean success, final String message) {
		this.transmitter = transmitter;
		this.transmitState = transmitState;
		this.success = success;
		this.timestamp = System.currentTimeMillis();
		this.message = message;
	}
	
	/**
	 * Get the event message
	 * @return the event message
	 */
	public String getMessage() {
		return this.message;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "[" + transmitter.getDisplayName() + "] " + transmitState.getTransmitSummary();
	}

	/**
     * @return the transmitter
     */
    public AbstractUplinkComposite getTransmitter() {
    	return transmitter;
    }

	/**
     * @return the transmitItem
     */
    public ISendCompositeState getTransmitState() {
    	return transmitState;
    }

	/**
     * @return the success
     */
    public boolean isSuccessful() {
    	return success;
    }

	/**
     * @return the timestamp
     */
    public long getTimestamp() {
    	return timestamp;
    }
}
