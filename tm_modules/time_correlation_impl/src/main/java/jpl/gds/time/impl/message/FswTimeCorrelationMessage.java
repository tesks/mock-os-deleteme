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
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.dictionary.api.frame.EncodingType;
import jpl.gds.shared.message.BaseXmlMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.xml.XmlUtility;
import jpl.gds.time.api.message.IFswTimeCorrelationMessage;
import jpl.gds.time.api.message.TimeCorrelationMessageType;

/**
 * This message is published whenever the downlink processor sees a time
 * correlation packet from the flight software, if time correlation handling is
 * enabled. It includes information from the TC packet and the reference frame
 * that goes with it.
 * 
 *
 */
public class FswTimeCorrelationMessage extends Message implements IFswTimeCorrelationMessage {

    private ISclk frameSclk;
	private ISclk packetSclk;
	private IAccurateDateTime frameErt;
	private IAccurateDateTime packetErt;
	private long frameVcfc;
	private int frameVcid;
	private double bitrate;
	private double frameLength;
	private boolean referenceFrameFound;
	// 7/16/13 - Added new fields per MPCS-5055.
	private long bitRateIndex;
	private EncodingType frameEncoding;

	/**
	 * Basic Constructor.
	 */
	private FswTimeCorrelationMessage() {

		super(TimeCorrelationMessageType.FswTimeCorrelation, System.currentTimeMillis());
	}
	
	/**
	 * Constructor that initializes fields with the supplied input values and a
	 * current timestamp.
	 * 
	 * @param desiredSclk
	 *            the "expected" SCLK from inside the TC packet
	 * @param pSclk
	 *            the SCLK from the TC packet header
	 * @param fErt
	 *            the ERT of the reference frame
	 * @param pErt
	 *            the ERT of the TC packet
	 * @param vcfc
	 *            the reference frame sequence counter
	 * @param vcid
	 *            the reference frame and TC packet virtual channel ID
	 * @param fLength
	 *            the length of the reference frame in bytes
	 * @param refFrameFound true if the reference frame was located, false if not
     * @param encoding encoding type of the reference frame
     * @param rate  bitrate at which the reference frame was received
     * @param rateIndex flight rate index the rate corresponds to
	 */
    FswTimeCorrelationMessage(final ISclk desiredSclk, final ISclk pSclk,
			final IAccurateDateTime fErt, final IAccurateDateTime pErt, final long vcfc, final int vcid,
			final int fLength, final boolean refFrameFound, final EncodingType encoding, final double rate, final long rateIndex) {

	    super(TimeCorrelationMessageType.FswTimeCorrelation);
	    setEventTime(new AccurateDateTime());

      this.frameSclk = desiredSclk;
      this.packetSclk = pSclk;
      this.frameErt = fErt;
      this.packetErt = pErt;
      this.frameVcfc = vcfc;
      this.frameVcid = vcid;
      this.frameLength = fLength;
      this.referenceFrameFound = refFrameFound;
      this.frameEncoding = encoding;
      this.bitrate = rate;
      this.bitRateIndex = rateIndex;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.time.api.message.IFswTimeCorrelationMessage#getExpectedSclk()
     */
	@Override
    public ISclk getExpectedSclk() {

		return this.frameSclk;
	}

	/**
	 * Sets the expected SCLK (from inside the TC packet).
	 * 
	 * @param sclkFromPacket
	 *            SCLK to set
	 */
    private void setExpectedSclk(final ISclk sclkFromPacket) {

		this.frameSclk = sclkFromPacket;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.time.api.message.IFswTimeCorrelationMessage#getPacketSclk()
     */
	@Override
    public ISclk getPacketSclk() {

		return this.packetSclk;
	}

	/**
	 * Sets the packet SCLK (from the TC packet header).
	 * 
	 * @param packetSclk
	 *            SCLK to set
	 */
	private void setPacketSclk(final ISclk packetSclk) {

		this.packetSclk = packetSclk;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.time.api.message.IFswTimeCorrelationMessage#getBitrate()
     */
	@Override
    public double getBitrate() {

		return this.bitrate;
	}

	/**
	 * Sets the bit rate at which the TC reference frame was received.
	 * 
	 * @param bitrate
	 *            the bit rate as a double.
	 */
	private void setBitrate(final double bitrate) {

		this.bitrate = bitrate;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.time.api.message.IFswTimeCorrelationMessage#getFrameErt()
     */
	@Override
    public IAccurateDateTime getFrameErt() {

		return this.frameErt;
	}

	/**
	 * Sets the ERT of the TC reference frame.
	 * 
	 * @param frameErt
	 *            ERT to set
	 */
	private void setFrameErt(final IAccurateDateTime frameErt) {

		this.frameErt = frameErt;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.time.api.message.IFswTimeCorrelationMessage#getPacketErt()
     */
	@Override
    public IAccurateDateTime getPacketErt() {

		return this.packetErt;
	}

	/**
	 * Sets the ERT of the TC packet.
	 * 
	 * @param packetErt
	 *            ERT to set
	 */
	private void setPacketErt(final IAccurateDateTime packetErt) {

		this.packetErt = packetErt;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.time.api.message.IFswTimeCorrelationMessage#getFrameVcfc()
     */
	@Override
    public long getFrameVcfc() {

		return this.frameVcfc;
	}

	/**
	 * Sets the virtual channel sequence count of the TC reference frame.
	 * 
	 * @param frameVcfc
	 *            the VCFC to set
	 */
	private void setFrameVcfc(final long frameVcfc) {

		this.frameVcfc = frameVcfc;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.time.api.message.IFswTimeCorrelationMessage#getFrameVcid()
     */
	@Override
    public int getFrameVcid() {

		return this.frameVcid;
	}

	/**
	 * Sets the virtual channel identifier of the TC reference frame.
	 * 
	 * @param vcid
	 *            the VCID to set
	 */
	private void setFrameVcid(final int vcid) {

		this.frameVcid = vcid;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.time.api.message.IFswTimeCorrelationMessage#isReferenceFrameFound()
     */
	@Override
    public boolean isReferenceFrameFound() {

		return this.referenceFrameFound;
	}

	/**
	 * Sets the flag indicating whether the TC reference frame was found in
	 * recently received frames. If it was not found, frame-related fields in
	 * this message will be undefined.
	 * 
	 * @param referenceFrameFound
	 *            true if the frame was found, false if not
	 */
	private void setReferenceFrameFound(final boolean referenceFrameFound) {

		this.referenceFrameFound = referenceFrameFound;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.time.api.message.IFswTimeCorrelationMessage#getFrameLength()
     */
	@Override
    public double getFrameLength() {

		return this.frameLength;
	}

	/**
	 * Sets the length in bytes of the TC reference frame.
	 * 
	 * @param frameLength
	 *            byte length to set
	 */
	private void setFrameLength(final double frameLength) {

		this.frameLength = frameLength;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.time.api.message.IFswTimeCorrelationMessage#getBitRateIndex()
     */
	@Override
    public long getBitRateIndex() {
	
		return bitRateIndex;
	}

	/**
	 * Sets the bit rate index from the TC packet.
	 * The interpretation of this value is mission specific.
	 * 
	 * @param bitRateIndex bit rate index value
	 */
	private void setBitRateIndex(final long bitRateIndex) {
	
		this.bitRateIndex = bitRateIndex;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.time.api.message.IFswTimeCorrelationMessage#getFrameEncoding()
     */
	@Override
    public EncodingType getFrameEncoding() {
	
		return frameEncoding;
	}

	/**
	 * Sets the frame encoding type from the TC packet.
	 * 
	 * @param frameEncoding EncodingType
	 * 
	 */
	private void setFrameEncoding(final EncodingType frameEncoding) {
	
		this.frameEncoding = frameEncoding;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
	 */
	@Override
	public String getOneLineSummary() {

		// 7/16/2013 - Output changed to add new fields per MPCS-5055.
		return "FSW Time Correlation Packet Received, Frame VCID="
				+ this.frameVcid + ", Frame VCFC=" + this.frameVcfc
				+ ", Frame ERT=" + (this.referenceFrameFound ? 
						this.frameErt.getFormattedErt(true) : "NOT FOUND")
						+ ", Frame SCLK=" + this.frameSclk + ", Bit Rate="
						+ this.bitrate + ", Frame Encoding=" + (frameEncoding == null ? "UNKNOWN" : 
							this.frameEncoding) +
							", Bit Rate Index = " + this.bitRateIndex;
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

		map.put(IMessage.EVENT_TIME_TAG,
				getEventTime() != null ? getEventTimeString() : "");

		if (this.packetErt != null) {
			map.put("packetErt", this.packetErt.getFormattedErt(true));
		}

		if (this.packetSclk != null) {
			map.put("packetSclk", this.packetSclk);
		}

		map.put("referenceFrameFound", this.referenceFrameFound);
		map.put("bitRate", this.bitrate);
		map.put("frameVcid", this.frameVcid);
		map.put("frameVcfc", this.frameVcfc);
		map.put("frameLength", this.frameLength);

		if (this.frameSclk != null) {
			map.put("frameSclk", this.frameSclk.toString());
		}

		if (this.frameErt != null) {
			map.put("frameErt", this.frameErt.getFormattedErt(true));
		}
		
		// 7/16/13 - Added this field per MPCS-5055.
		if (this.frameEncoding != null) {
			map.put("frameEncoding", this.frameEncoding.toString());
		}
		
		// 7/16/13 - Added this field per MPCS-5055.
		map.put("bitRateIndex", this.bitRateIndex);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
	 */
	@Override
	public void generateStaxXml(final XMLStreamWriter writer)
			throws XMLStreamException {

		writer.writeStartElement(MessageRegistry.getDefaultInternalXmlRoot(getType())); // <FswTimeCorrelationMessage>
		writer.writeAttribute(IMessage.EVENT_TIME_TAG, getEventTimeString());
		
		super.generateStaxXml(writer);

		writer.writeStartElement("packetSclk"); // <packetSclk>
		writer.writeCharacters(this.packetSclk == null ? "" : this.packetSclk
				.toString());
		writer.writeEndElement(); // </packetSclk>

		writer.writeStartElement("packetErt"); // <packetErt>
		writer.writeCharacters(this.packetErt == null ? "" : this.packetErt
				.getFormattedErt(true));
		writer.writeEndElement(); // </packetErt>

		writer.writeStartElement("referenceFrameFound"); // <referenceFrameFound>
		writer.writeCharacters(String.valueOf(this.referenceFrameFound));
		writer.writeEndElement(); // </referenceFrameFound>

		writer.writeStartElement("bitRate"); // <bitRate>
		writer.writeCharacters(String.valueOf(this.bitrate));
		writer.writeEndElement(); // </bitRate>

		writer.writeStartElement("frameVcid"); // <frameVcid>
		writer.writeCharacters(String.valueOf(this.frameVcid));
		writer.writeEndElement(); // </frameVcid>

		writer.writeStartElement("frameVcfc"); // <frameVcfc>
		writer.writeCharacters(String.valueOf(this.frameVcfc));
		writer.writeEndElement(); // </frameVcfc>

		writer.writeStartElement("frameLength"); // <frameLength>
		writer.writeCharacters(String.valueOf(this.frameLength));
		writer.writeEndElement(); // </frameLength>

		writer.writeStartElement("frameSclk"); // <frameSclk>
		writer.writeCharacters(this.frameSclk == null ? "" : this.frameSclk
				.toString());
		writer.writeEndElement(); // </frameSclk>

		// this is the only optional element
		if (this.frameErt != null) {
			writer.writeStartElement("frameErt"); // <frameErt>
			writer.writeCharacters(this.frameErt.getFormattedErt(true));
			writer.writeEndElement(); // </frameErt>
		}

		// 7/16/13 - Added this field per MPCS-5055.
		if (this.frameEncoding != null) {
			writer.writeStartElement("frameEncodingType");
			writer.writeCharacters(this.frameEncoding.toString());
			writer.writeEndElement(); // <frameEncodingType>
		}
		
		// 7/16/13 - Added this field per MPCS-5055.
		writer.writeStartElement("bitRateIndex");
		writer.writeCharacters(String.valueOf(this.bitRateIndex));
		writer.writeEndElement(); // <bitRateIndex>
		
		writer.writeEndElement(); // </FswTimeCorrelationMessage>
	}

	/**
	 * 
	 * ParseHandler is the message-specific SAX parse handler for creating this
	 * Message from its XML representation.
	 * 
	 */
	public static class XmlParseHandler extends BaseXmlMessageParseHandler {
		private FswTimeCorrelationMessage msg;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void startElement(final String uri, final String localName,
				final String qname, final Attributes attr) throws SAXException {

			super.startElement(uri, localName, qname, attr);

			if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(TimeCorrelationMessageType.FswTimeCorrelation))) {
				setInMessage(true);
				this.msg = new FswTimeCorrelationMessage();
				addMessage(this.msg);
				this.msg.setEventTime(getDateFromAttr(attr,
						IMessage.EVENT_TIME_TAG));
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void endElement(final String uri, final String localName,
				final String qname) throws SAXException {

			super.endElement(uri, localName, qname);

			if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(TimeCorrelationMessageType.FswTimeCorrelation))) {
				setInMessage(false);
			} if (qname.equalsIgnoreCase("packetSclk")) {
				this.msg.setPacketSclk(getSclkFromBuffer());
			} else if (qname.equalsIgnoreCase("packetErt")) {
				try {
					this.msg.setPacketErt(new AccurateDateTime(getBufferText()));
				} catch (final ParseException ex) {
					throw new SAXException("Cannot parse packet ERT time: "
							+ getBufferText());
				}
			} else if (qname.equalsIgnoreCase("referenceFrameFound")) {
				this.msg.setReferenceFrameFound(getBooleanFromBuffer());
			} else if (qname.equalsIgnoreCase("bitRate")) {
				this.msg.setBitrate(XmlUtility
						.getDoubleFromText(getBufferText()));
			} else if (qname.equalsIgnoreCase("frameVcid")) {
				this.msg.setFrameVcid(this.getIntFromBuffer());
			} else if (qname.equalsIgnoreCase("frameVcfc")) {
				this.msg.setFrameVcfc(getLongFromBuffer());
			} else if (qname.equalsIgnoreCase("frameLength")) {
				this.msg.setFrameLength(XmlUtility
						.getDoubleFromText(getBufferText()));
			} else if (qname.equalsIgnoreCase("frameSclk")) {
				this.msg.setExpectedSclk(getSclkFromBuffer());
			} else if (qname.equalsIgnoreCase("frameErt")) {
				try {
					this.msg.setFrameErt(new AccurateDateTime(getBufferText()));
				} catch (final ParseException ex) {
					throw new SAXException("Cannot parse frame ERT time: "
							+ getBufferText());
				}
			} else if (qname.equalsIgnoreCase("frameEncodingType")) {
				// 7/16/13 - Added parsing of this field per MPCS-5055.
				try {
					final EncodingType t = EncodingType.valueOf(getBufferText());
					this.msg.setFrameEncoding(t);
				} catch (final IllegalArgumentException e) {
					throw new SAXException("Cannot parser frame encoding type: " + getBufferText());
				}
			} else if (qname.equalsIgnoreCase("bitRateIndex")) {
				// 7/16/13 - Added parsing of this field per MPCS-5055.
				this.msg.setBitRateIndex(XmlUtility.getLongFromText(getBufferText()));
			}
		}
	}
	
}