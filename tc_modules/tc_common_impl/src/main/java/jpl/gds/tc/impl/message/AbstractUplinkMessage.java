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
package jpl.gds.tc.impl.message;

import java.util.Map;

import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.tc.api.UplinkFailureReason;
import jpl.gds.tc.api.message.ICpdUplinkMessage;
import jpl.gds.tc.api.message.IUplinkMessage;

/**
 * Abstract superclass for all the various classes that represent messages
 * related to uplink.
 * 
 */
public abstract class AbstractUplinkMessage extends Message
		implements IUplinkMessage {
	/** Sequence file inserted into messages */
	protected String sequenceFile;
	private String commandedSide = DEFAULT_COMMANDED_SIDE;

	/**
	 * Initialize the uplink message
	 * 
	 * @param type The internal message type
	 */
	public AbstractUplinkMessage(final IMessageType type) {
        this(type, new AccurateDateTime(System.currentTimeMillis()));
	}

	/**
	 * Constructor
	 * @param type The internal message type
	 * @param eventTime the event time
	 */
    public AbstractUplinkMessage(final IMessageType type, final IAccurateDateTime eventTime) {
		super(type);
		setEventTime(eventTime);
		this.sequenceFile = null;
	}
	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.message.IMessage#toString()
	 */
	@Override
	public String toString() {
		return (getOneLineSummary());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see 
	 *      jpl.gds.shared.message.IMessage#setTemplateContext(Map<String,Object>)
	 */
	@Override
	public void setTemplateContext(final Map<String, Object> map) {
		super.setTemplateContext(map);

		map.put("type", getType());
		map.put("commandType", getType());

		map.put("commandString", getDatabaseString());

		if (this.sequenceFile != null) {
			map.put("sequenceFile", this.sequenceFile);
		}

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
	 */
	@Override
	public String getOneLineSummary() {
		final String dbString = getDatabaseString();

		if (dbString == null) {
			return "";
		}

		if (this instanceof ICpdUplinkMessage) {
			final ICpdUplinkMessage self = (ICpdUplinkMessage) this;
			return dbString
					+ " uplink request submitted: Status="
					+ self.getICmdRequestStatus()
					+ ((self.getICmdRequestFailureReason() != null && !self
							.getICmdRequestFailureReason().equalsIgnoreCase(
									UplinkFailureReason.NONE.toString())) ? (" ("
							+ self.getICmdRequestFailureReason() + ")")
							: "") + " (Request ID=" + self.getICmdRequestId()
					+ ")";
		}

		return dbString;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.message.IUplinkMessage#getCommandedSide()
     */
	@Override
    public String getCommandedSide() {
		return commandedSide;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.message.IUplinkMessage#setCommandedSide(java.lang.String)
     */
	@Override
    public void setCommandedSide(final String cs) {
		commandedSide = ((cs != null) ? cs.trim() : DEFAULT_COMMANDED_SIDE);
	}

}
