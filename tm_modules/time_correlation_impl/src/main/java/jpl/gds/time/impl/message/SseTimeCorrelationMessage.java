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
package jpl.gds.time.impl.message;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.shared.message.BaseXmlMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.types.Pair;
import jpl.gds.time.api.message.ISseTimeCorrelationMessage;
import jpl.gds.time.api.message.TimeCorrelationMessageType;

/**
 * This message is published whenever an SSE time correlation message is
 * received by the downlink processor, if time correlation capability is
 * enabled. This message contain the TC packet SCLK and an ERT, and a series of
 * SCLK/ERT pairs used for correlation. This capability functions only with the
 * JPL SSE.
 * 
 */
public class SseTimeCorrelationMessage extends Message implements ISseTimeCorrelationMessage {

    private List<Pair<ISclk, IAccurateDateTime>> timeEntries = new ArrayList<Pair<ISclk, IAccurateDateTime>>(
			32);
	private IAccurateDateTime packetErt;
    private ISclk                               packetSclk;

	/**
     * Constructor that initializes members with the given input values and a
     * current timestamp.
     * 
     * @param correlations
     *            SCLK/ERT pairs from the SSE TC packet
     * @param packetErt
     *            ERT of the TC Packet
     * @param packetSclk
     *            SCLK of the TC packet
     */
	SseTimeCorrelationMessage(
            final List<Pair<ISclk, IAccurateDateTime>> correlations, final IAccurateDateTime packetErt,
            final ISclk packetSclk) {


        super(TimeCorrelationMessageType.SseTimeCorrelation, System.currentTimeMillis());

        this.timeEntries = correlations;
        this.packetSclk = packetSclk;
        this.packetErt = packetErt;
	}

	/**
	 * Default constructor.
	 */
	private SseTimeCorrelationMessage() {

		super(TimeCorrelationMessageType.SseTimeCorrelation, System.currentTimeMillis());
	}


	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
	 */
	@Override
	public String getOneLineSummary() {

		return "SSE Time Correlation Packet Received at ERT="
				+ this.packetErt
				+ ", "
				+ (this.timeEntries.isEmpty() ? ("no correlations in message")
                        : ("first correlation SCLK="
								+ this.timeEntries.get(0).getOne().toString()
								+ " for ERT=" + this.timeEntries.get(0).getTwo()
								.getFormattedErt(true)));
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setTemplateContext(final Map<String, Object> map) {

		super.setTemplateContext(map);
		if (getEventTime() != null) {
			map.put(IMessage.EVENT_TIME_TAG, getEventTimeString());
		} else {
			map.put(IMessage.EVENT_TIME_TAG, "");
		}

        if (this.packetSclk != null) {
            map.put("packetSclk", this.packetSclk);
		}

		if (this.packetErt != null) {
			map.put("packetErt", this.packetErt.getFormattedErt(true));
		}

		map.put("correlations", this.timeEntries);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
	 */
	@Override
	public void generateStaxXml(final XMLStreamWriter writer)
			throws XMLStreamException {

		writer.writeStartElement(MessageRegistry.getDefaultInternalXmlRoot(getType())); // <SseTimeCorrelationMessage>
		writer.writeAttribute(IMessage.EVENT_TIME_TAG, getEventTimeString());
		
		super.generateStaxXml(writer);

        if (this.packetSclk != null) {
            writer.writeStartElement("packetSclk"); // <packetSclk>
            writer.writeCharacters(this.packetSclk.toString());
            writer.writeEndElement(); // </packetSclk>
		}

		if (this.packetErt != null) {
			writer.writeStartElement("packetErt"); // <packetErt>
			writer.writeCharacters(this.packetErt.getFormattedErt(true));
			writer.writeEndElement(); // </packetErt>
		}

		writer.writeStartElement("correlations"); // <correlations>

        for (final Pair<ISclk, IAccurateDateTime> pair : this.timeEntries) {
			writer.writeStartElement("correlation"); // <correlation>
            writer.writeAttribute("sclk", pair.getOne().toString());
			writer.writeAttribute("ert", pair.getTwo().getFormattedErt(true));
			writer.writeEndElement(); // </correlation>
		}

		writer.writeEndElement(); // </correlations>

		writer.writeEndElement(); // </SseTimeCorrelationMessage>

	}


	/**
     * @{inheritDoc}
     * @see jpl.gds.time.api.message.ISseTimeCorrelationMessage#getTimeEntries()
     */
	@Override
    public List<Pair<ISclk, IAccurateDateTime>> getTimeEntries() {
		return this.timeEntries;
	}

	/**
     * Sets the whole list of SCLK/ERT correlation pairs, wiping out all
     * previous entries.
     * 
     * @param timeEntries
     *            List of Pairs
     */
    private void setTimeEntries(final List<Pair<ISclk, IAccurateDateTime>> timeEntries) {

		this.timeEntries = timeEntries;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.time.api.message.ISseTimeCorrelationMessage#getPacketErt()
     */
	@Override
    public IAccurateDateTime getPacketErt() {

		return this.packetErt;
	}

	/**
	 * Sets the TC packet ERT.
	 * 
	 * @param sseErt
	 *            ERT to set
	 */
	private void setPacketErt(final IAccurateDateTime sseErt) {

		this.packetErt = sseErt;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.time.api.message.ISseTimeCorrelationMessage#getPacketSclk()
     */
	@Override
    public ISclk getPacketSclk() {

        return this.packetSclk;
	}

	/**
     * Sets the TC packet SCLK.
     * 
     * @param sseSclk
     *            SCLK to set
     */
    private void setPacketSclk(final ISclk sseSclk) {

        this.packetSclk = sseSclk;
	}

	/**
	 * ParseHandler is the message-specific SAX parse handler for creating this
	 * Message from its XML representation.
	 * 
	 */
	public static class XmlParseHandler extends BaseXmlMessageParseHandler {
		private SseTimeCorrelationMessage msg;
        private List<Pair<ISclk, IAccurateDateTime>> correlations;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void startElement(final String uri, final String localName,
				final String qname, final Attributes attr) throws SAXException {

			super.startElement(uri, localName, qname, attr);

			if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(TimeCorrelationMessageType.SseTimeCorrelation))) {
				setInMessage(true);
				this.msg = new SseTimeCorrelationMessage();
				addMessage(this.msg);
				this.msg.setEventTime(getDateFromAttr(attr,
						IMessage.EVENT_TIME_TAG));
			} else if (qname.equalsIgnoreCase("correlations")) {
                this.correlations = new ArrayList<Pair<ISclk, IAccurateDateTime>>();
				this.msg.setTimeEntries(this.correlations);
			} else if (qname.equalsIgnoreCase("correlation")) {
                final String sclkStr = attr.getValue("sclk");
                if (sclkStr == null) {
					throw new SAXException(
                                           "No SCLK value in correlation element");
				}
                final ISclk s = sclkFmt.valueOf(sclkStr);
				final String ertStr = attr.getValue("ert");
				if (ertStr == null) {
					throw new SAXException(
							"No ERT value in correlation element");
				}
				IAccurateDateTime e = null;
				try {
					e = new AccurateDateTime(ertStr);
				} catch (final ParseException ex) {
					throw new SAXException(
							"Unparseable ERT in correlation element: " + ertStr);
				}
                final Pair<ISclk, IAccurateDateTime> pair = new Pair<ISclk, IAccurateDateTime>(
						s, e);
                this.correlations.add(pair);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void endElement(final String uri, final String localName,
				final String qname) throws SAXException {

			super.endElement(uri, localName, qname);

			if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(TimeCorrelationMessageType.SseTimeCorrelation))) {
				setInMessage(false);
            }
            else if (qname.equalsIgnoreCase("packetSclk")) {
                this.msg.setPacketSclk(getSclkFromBuffer());
			} else if (qname.equalsIgnoreCase("packetErt")) {
				try {
					this.msg.setPacketErt(new AccurateDateTime(getBufferText()));
				} catch (final ParseException ex) {
					throw new SAXException("Cannot parse packet ERT time: "
							+ getBufferText());
				}
			}
		}
	}

}
