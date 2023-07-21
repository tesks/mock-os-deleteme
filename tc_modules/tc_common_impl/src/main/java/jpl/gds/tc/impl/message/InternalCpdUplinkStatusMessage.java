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

import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.tc.api.ICpdUplinkStatus;
import jpl.gds.tc.api.message.CommandMessageType;
import jpl.gds.tc.api.message.IInternalCpdUplinkStatusMessage;

/**
 * This class represents the message of ICMD-based uplink status. It carries a
 * list of <code>CpdUplinkStatus</code> objects, which has been polled from CPD.
 * This message is only for use on the AMPCS internal message context
 * 
 * @since AMPCS R3
 */
public class InternalCpdUplinkStatusMessage extends Message implements IInternalCpdUplinkStatusMessage {

	/** The statuses */
	private final List<ICpdUplinkStatus> statuses;

	/** The delta status from previous poll */
	private final List<ICpdUplinkStatus> deltas;

	/**
	 * Constructs a new InternalCpdUplinkStatusMessage.
	 */
	public InternalCpdUplinkStatusMessage(final List<ICpdUplinkStatus> statuses,
			final List<ICpdUplinkStatus> deltas) {
		super(CommandMessageType.InternalCpdUplinkStatus);
		this.statuses = statuses;
		this.deltas = deltas;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public List<ICpdUplinkStatus> getStatuses() {
		return statuses;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public List<ICpdUplinkStatus> getDeltas() {
		return deltas;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
	 */
	@Override
	public void generateStaxXml(final XMLStreamWriter writer)
			throws XMLStreamException {
        writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
	 */
	@Override
	public String getOneLineSummary() {
		final StringBuilder sb = new StringBuilder();

		for (final ICpdUplinkStatus us : this.statuses) {
			sb.append("Uplink request status update: Status=");
			sb.append(us.getStatus().toString());
			sb.append(" (Request ID=");
			sb.append(us.getId());
			sb.append(")");
			sb.append(";");
		}

		return sb.toString();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.message.IMessage#toString()
	 */
	@Override
	public String toString() {
		return getOneLineSummary();
	}
}
