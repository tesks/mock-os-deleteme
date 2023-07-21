/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.tc.impl.message;

import jpl.gds.shared.message.Message;
import jpl.gds.shared.metadata.context.ContextKey;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.types.UnsignedLong;
import jpl.gds.tc.api.message.CommandMessageType;
import jpl.gds.tc.api.message.ITransmittableCommandMessage;

/**
 * The representation of a File CFDP PutActionRequest as a notification sent on the message
 * bus. This message is sent out on the bus when a PutActionRequest is issued to the CFDP
 * server
 *
 */
public class FileCfdpMessage extends Message implements ITransmittableCommandMessage {

	private int transmitEventId;
	private boolean isSuccessful;
	private String commandString;
	
	/**
	 * Constructor
	 */
	public FileCfdpMessage(String message, final boolean success, final UnsignedLong sessionId) {
		super(CommandMessageType.FileCfdp);

		setEventTime(new AccurateDateTime());
		setSuccessful(success);

		// Make sure command gets logged with the provided session key
		// NOT the processor's context key
		if (sessionId != null && sessionId.longValue() > 0) {
			IContextKey k = new ContextKey();
			k.setNumber(sessionId.longValue());
			setContextKey(k);
		}

		this.commandString = message;
	}

	public String getDatabaseString() {
		return commandString;
	}

	@Override
	public String getCommandedSide() {
		return null;
	}

	@Override
	public void setCommandedSide(final String cs) {
		// doesn't go to a side
		
	}

	@Override
	public String getOneLineSummary() {
		return getDatabaseString();
	}

	@Override
	public String toString() {
		return getOneLineSummary();
	}
	

	@Override
	public void setCommandString(final String commandString) {
		this.commandString = commandString;
	}

	@Override
	public String getCommandString() {
		return this.commandString;
	}

	@Override
	public int getTransmitEventId() {
		return this.transmitEventId;
	}

	@Override
	public void setTransmitEventId(final int transmitEventId) {
		this.transmitEventId = transmitEventId;
		
	}

	@Override
	public boolean isSuccessful() {
		return this.isSuccessful;
	}

	@Override
	public void setSuccessful(final boolean isSuccessful) {
		this.isSuccessful = isSuccessful;
		
	}

}
