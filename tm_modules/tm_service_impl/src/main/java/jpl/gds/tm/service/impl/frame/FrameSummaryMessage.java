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
package jpl.gds.tm.service.impl.frame;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.dictionary.api.frame.EncodingType;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.message.BaseXmlMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.message.PublishableLogMessage;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.xml.XmlUtility;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.EncodingSummaryRecord;
import jpl.gds.tm.service.api.frame.FrameSummaryRecord;
import jpl.gds.tm.service.api.frame.IFrameSummaryMessage;

/**
 * FrameSummaryMessage is used to report the status of the frame synchronization
 * and includes statistics accumulated by framesync.
 * 
 */
public class FrameSummaryMessage extends PublishableLogMessage implements IFrameSummaryMessage {

	/**
	 * A Summary Map that maps a string to the FrameSummaryRecord
	 */
	protected Map<String, FrameSummaryRecord> summaryMap;
	/**
	 * A summary map that maps a string to the EncodingSumaryRecord
	 */
	protected Map<String, EncodingSummaryRecord> encodingMap;
	/**
	 * A long that stores the number of frames.
	 */
	protected long numFrames;
	/**
	 * A long that stores the number of idle frames.
	 */
	protected long idleFrames;
	/**
	 * A long that stores the number of dead frames.
	 */
	protected long deadFrames;
	/**
	 * A long that stores the number of badFrames.
	 */
	protected long badFrames;
	/**
	 * A long that stores the out of sync bytes.
	 */
	protected long outOfSyncBytes;
	/**
	 * A long that stores the number of out of sync bytes.
	 */
	protected long outOfSyncCount;
	/**
	 * A long that stores the frame bytes.
	 */
	protected long frameBytes;
	/**
	 * An inSync flag.
	 */
	protected boolean inSync;
	
	/**
	 * Incoming frame bitrate, bps.
	 * 
	 */
	private final double bitrate;

	/**
	 * Creates an instance of FrameSummaryMessage.
	 * 
	 * @param isInSync
	 *            indicates whether state is IN SYNC
	 * @param _numFrames
	 *            total number of frames processed
	 * @param _frameBytes
	 *            total bytes in all frames
	 * @param _outSyncBytes
	 *            total bytes out of sync
	 * @param _outSyncCount
	 *            number of times out-of-sync occurred
	 * @param _numIdle
	 *            total number of idle frames
	 * @param _numDead
	 *            total number of deadc0de frames
	 * @param _numBad
	 *            total number of bad frames
	 * @param _bitrate
	 *            frame incoming bitrate, bps  
	 */
	protected FrameSummaryMessage(final boolean isInSync, final long _numFrames,
			final long _frameBytes, final long _outSyncBytes,
			final long _outSyncCount, final long _numIdle, final long _numDead,
			final long _numBad, final double _bitrate) {
		super(TmServiceMessageType.TelemetryFrameSummary, TraceSeverity.INFO,
				LogMessageType.FRAME_SUMMARY);
		inSync = isInSync;
		numFrames = _numFrames;
		frameBytes = _frameBytes;
		outOfSyncBytes = _outSyncBytes;
		outOfSyncCount = _outSyncCount;
		idleFrames = _numIdle;
		deadFrames = _numDead;
		badFrames = _numBad;
		bitrate = _bitrate;
	}


	@Override
	public String toString() {
		return "MSG:" + getType() + " " + getEventTimeString() + " isInSync="
				+ inSync + " numFrames=" + numFrames;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameSummaryMessage#setFrameSummaryMap(java.util.Map)
     */
	@Override
    public synchronized void setFrameSummaryMap(
			final Map<String, FrameSummaryRecord> map) {
	    if (map == null) {
	        summaryMap = null;
	        return;
	    }
		summaryMap = new HashMap<String, FrameSummaryRecord>(map);
		summaryMap.putAll(map);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameSummaryMessage#getFrameSummaryMap()
     */
	@Override
    public synchronized Map<String, FrameSummaryRecord> getFrameSummaryMap() {
		return summaryMap;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameSummaryMessage#setEncodingSummaryMap(java.util.Map)
     */
	@Override
    public synchronized void setEncodingSummaryMap(
			final Map<String, EncodingSummaryRecord> map) {
	    if (map == null) {
	        encodingMap = null;
	        return;
	    }
		encodingMap = new HashMap<String, EncodingSummaryRecord>(map);
		encodingMap.putAll(map);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameSummaryMessage#getEncodingSummaryMap()
     */
	@Override
    public synchronized Map<String, EncodingSummaryRecord> getEncodingSummaryMap() {
		return encodingMap;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.message.PublishableLogMessage#setTemplateContext(java.util.Map)
	 */
	@Override
	public synchronized void setTemplateContext(final Map<String, Object> map) {
		super.setTemplateContext(map);

		if (getEventTime() != null) {
			map.put(IMessage.EVENT_TIME_TAG, getEventTimeString());
		} else {
			map.put(IMessage.EVENT_TIME_TAG, "");
		}
		map.put("inSync", Boolean.valueOf(inSync));
		map.put("numFrames", Long.valueOf(numFrames));
		map.put("idleFrames", Long.valueOf(idleFrames));
		map.put("deadFrames", Long.valueOf(deadFrames));
		map.put("badFrames", Long.valueOf(badFrames));
		map.put("frameBytes", Long.valueOf(frameBytes));
		map.put("outOfSyncBytes", Long.valueOf(outOfSyncBytes));
		map.put("outOfSyncCount", Long.valueOf(outOfSyncCount));
		
		map.put("bitrate", bitrate);

		if (summaryMap != null) {
			final Collection<FrameSummaryRecord> sums = summaryMap.values();
			map.put("summaryList", new ArrayList<FrameSummaryRecord>(sums));
		}

		if (encodingMap != null) {
			final Collection<EncodingSummaryRecord> sums = encodingMap.values();
			map.put("encodingList", new ArrayList<EncodingSummaryRecord>(sums));
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.message.PublishableLogMessage#generateStaxXml(javax.xml.stream.XMLStreamWriter)
	 */
	@Override
	public synchronized void generateStaxXml(final XMLStreamWriter writer)
			throws XMLStreamException {
		final DateFormat df = TimeUtility.getFormatterFromPool();
		try {
			writer.writeStartElement(MessageRegistry.getDefaultInternalXmlRoot(getType())); // <FrameSyncSumMessage>
			writer.writeAttribute(IMessage.EVENT_TIME_TAG, getEventTimeString());
			
			super.generateXmlForContext(writer);

			writer.writeStartElement("inSync"); // <inSync>
			writer.writeCharacters(Boolean.toString(inSync));
			writer.writeEndElement(); // </inSync>

			writer.writeStartElement("numFrames"); // <numFrames>
			writer.writeCharacters(Long.toString(numFrames));
			writer.writeEndElement(); // </numFrames>

			writer.writeStartElement("idleFrames"); // <idleFrames>
			writer.writeCharacters(Long.toString(idleFrames));
			writer.writeEndElement(); // </idleFrames>

			writer.writeStartElement("deadFrames"); // <deadFrames>
			writer.writeCharacters(Long.toString(deadFrames));
			writer.writeEndElement(); // </deadFrames>

			writer.writeStartElement("badFrames"); // <badFrames>
			writer.writeCharacters(Long.toString(badFrames));
			writer.writeEndElement(); // </badFrames>

			writer.writeStartElement("frameBytes"); // <frameBytes>
			writer.writeCharacters(Long.toString(frameBytes));
			writer.writeEndElement(); // </frameBytes>

			writer.writeStartElement("outOfSyncBytes"); // <outOfSyncBytes>
			writer.writeCharacters(Long.toString(outOfSyncBytes));
			writer.writeEndElement(); // </outOfSyncBytes>

			writer.writeStartElement("outOfSyncCount"); // <outOfSyncCount>
			writer.writeCharacters(Long.toString(outOfSyncCount));
			writer.writeEndElement(); // </outOfSyncCount>

			writer.writeStartElement("bitrate"); // <bitrate>
			writer.writeCharacters(Double.toString(bitrate));
			writer.writeEndElement(); // </bitrate>

			if (summaryMap != null && !summaryMap.isEmpty()) {
				writer.writeStartElement("frameSummaries"); // <frameSummaries>
				for (final FrameSummaryRecord fts : summaryMap.values()) {
					writer.writeStartElement("frameSummary"); // <frameSummary>
					writer.writeAttribute("frameType", fts.getFrameType());
					writer.writeAttribute("vcid", Long.toString(fts.getVcid()));
					writer.writeAttribute("sequenceCount",
							Long.toString(fts.getSequenceCount()));
					writer.writeAttribute("instanceCount",
							Long.toString(fts.getCount()));
					writer.writeAttribute("lastErtTime", fts.getLastErtStr());
					writer.writeEndElement(); // </frameSummary>
				}
				writer.writeEndElement(); // </frameSummaries>
			}

			if (encodingMap != null && !encodingMap.isEmpty()) {
				writer.writeStartElement("encodingSummaries"); // <encodingSummaries>
				for (final EncodingSummaryRecord es : encodingMap.values()) {
					writer.writeStartElement("encodingSummary"); // <encodingSummary>
					writer.writeAttribute("encodingType", es.getType()
							.toString());
					writer.writeAttribute("instanceCount",
							Long.toString(es.getInstanceCount()));
					writer.writeAttribute("sequenceCount",
							Long.toString(es.getLastSequence()));
					writer.writeAttribute("vcid", Long.toString(es.getVcid()));
					writer.writeAttribute("lastErtTime", es.getLastErtStr());
					writer.writeAttribute("badFrameCount",
							Long.toString(es.getBadFrameCount()));
					writer.writeAttribute("errorCount",
							Long.toString(es.getErrorCount()));
					writer.writeEndElement(); // </encodingSummary>
				}
				writer.writeEndElement(); // </encodingSummaries>
			}

			writer.writeEndElement(); // </FrameSyncSumMessage>
		} finally {
			TimeUtility.releaseFormatterToPool(df);
		}
	}

	/**
     * ParseHandler is the message-specific SAX parse handler for
     * creating this Message from its XML representation.
     * 
	 */
	public static class XmlParseHandler extends BaseXmlMessageParseHandler {

		/**
		 * A long to store the number of frames
		 */
		protected long numFrames;
		/**
		 * A long to store the idle frames.
		 */
		protected long idleFrames;
        /**
         * A long to store the dead frames.
         */
		protected long deadFrames;
        /**
         * A long to store the bad frames
         */
		protected long badFrames;
        /**
         * A long to store the out of sync bytes.
         */
		protected long outOfSyncBytes;
        /**
         * A long to store the out of sync bytes count.
         */
		protected long outOfSyncCount;
        /**
         * A long to store the frame bytes.
         */
		protected long frameBytes;
		private boolean inSync;
		private IAccurateDateTime time;
		private Map<String, FrameSummaryRecord> summaryMap;
		private Map<String, EncodingSummaryRecord> encodingMap;
		private double bitrate;

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
		 * java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(final String uri, final String localName,
				final String qname, final Attributes attr) throws SAXException {
			super.startElement(uri, localName, qname, attr);

			if (qname.equals(MessageRegistry.getDefaultInternalXmlRoot(TmServiceMessageType.TelemetryFrameSummary))) {
				setInMessage(true);
				time = getDateFromAttr(attr, IMessage.EVENT_TIME_TAG);

			} else if (qname.equalsIgnoreCase("frameSummaries")) {
				summaryMap = new HashMap<String, FrameSummaryRecord>();

			} else if (qname.equalsIgnoreCase("frameSummary")) {

				final FrameSummaryRecord sum = new FrameSummaryRecord();
				sum.setFrameType(attr.getValue("frameType"));

				String aValue = attr.getValue("vcid");
				if (aValue != null) {
					sum.setVcid(Long.parseLong(aValue));
				}
				aValue = attr.getValue("sequenceCount");
				if (aValue != null) {
					sum.setSequenceCount(Long.parseLong(aValue));
				}
				aValue = attr.getValue("instanceCount");
				if (aValue != null) {
					sum.setCount(Long.parseLong(aValue));
				}
				aValue = attr.getValue("lastErtTime");
				if (aValue != null) {
					try {
						sum.setLastErt(new AccurateDateTime(aValue));
					} catch (final java.text.ParseException e) {
						e.printStackTrace();
					}
				}
				summaryMap.put(sum.getVcid() + "/" + sum.getFrameType(), sum);

			} else if (qname.equalsIgnoreCase("encodingSummaries")) {
				encodingMap = new HashMap<String, EncodingSummaryRecord>();

			} else if (qname.equalsIgnoreCase("encodingSummary")) {
				final EncodingSummaryRecord sum = new EncodingSummaryRecord();

				sum.setType(Enum.valueOf(EncodingType.class,
						attr.getValue("encodingType")));

				String aValue = attr.getValue("vcid");
				if (aValue != null) {
					sum.setVcid(Integer.parseInt(aValue));
				}
				aValue = attr.getValue("sequenceCount");
				if (aValue != null) {
					sum.setLastSequence(Long.parseLong(aValue));
				}
				aValue = attr.getValue("instanceCount");
				if (aValue != null) {
					sum.setInstanceCount(Long.parseLong(aValue));
				}
				aValue = attr.getValue("lastErtTime");
				if (aValue != null) {
					try {
						sum.setLastErt(new AccurateDateTime(aValue));
					} catch (final java.text.ParseException e) {
						e.printStackTrace();
					}
				}
				aValue = attr.getValue("badFrameCount");
				if (aValue != null) {
					sum.setBadFrameCount(Long.parseLong(aValue));
				}
				aValue = attr.getValue("errorCount");
				if (aValue != null) {
					sum.setErrorCount(Long.parseLong(aValue));
				}
				encodingMap.put(sum.getVcid() + "/" + sum.getType().toString(),
						sum);

			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
		 * java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(final String uri, final String localName,
				final String qname) throws SAXException {
			super.endElement(uri, localName, qname);

			if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(TmServiceMessageType.TelemetryFrameSummary))) {
			    final FrameSummaryMessage msg = new FrameSummaryMessage(inSync, numFrames,
			            frameBytes, outOfSyncBytes,
			            outOfSyncCount, idleFrames, deadFrames,
			            badFrames, bitrate);
				msg.setEncodingSummaryMap(encodingMap);
			    msg.setFrameSummaryMap(summaryMap);
			    msg.setEventTime(time);
			    addMessage(msg);
			    setInMessage(false);
			} else if (qname.equalsIgnoreCase("inSync")) {
				inSync = this.getBooleanFromBuffer();
			} else if (qname.equalsIgnoreCase("numFrames")) {
				numFrames = this.getLongFromBuffer();
			} else if (qname.equalsIgnoreCase("idleFrames")) {
				idleFrames = this.getLongFromBuffer();
			} else if (qname.equalsIgnoreCase("deadFrames")) {
				deadFrames = this.getLongFromBuffer();
			} else if (qname.equalsIgnoreCase("badFrames")) {
				badFrames = this.getLongFromBuffer();
			} else if (qname.equalsIgnoreCase("frameBytes")) {
				frameBytes = this.getLongFromBuffer();
			} else if (qname.equalsIgnoreCase("outOfSyncBytes")) {
				outOfSyncBytes = this.getLongFromBuffer();
			} else if (qname.equalsIgnoreCase("outOfSyncCount")) {
				outOfSyncCount = this.getLongFromBuffer();
			} else if (qname.equalsIgnoreCase("bitrate")) {
				bitrate = XmlUtility.getDoubleFromText(buffer.toString());
			}
		}
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameSummaryMessage#getMessage()
     */
	@Override
	public String getMessage() {
		return getOneLineSummary();
	}

	
	/**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameSummaryMessage#getOneLineSummary()
     */
	@Override
	public String getOneLineSummary() {
		final String retVal = (inSync ? "In Sync" : "Out of Sync") + ", Frame Bytes="
				+ frameBytes + ", Frames=" + numFrames + ", Out Of Sync Bytes="
				+ outOfSyncBytes + ", Out Of Sync Count=" + outOfSyncCount
				+ ", Idle Frames=" + idleFrames
				+ (deadFrames > 0 ? ", Dead Frames=" + deadFrames : ", Bitrate=" 
			    + bitrate);

		return retVal;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameSummaryMessage#getDeadFrames()
     */
	@Override
    public long getDeadFrames() {
		return deadFrames;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameSummaryMessage#getFrameBytes()
     */
	@Override
    public long getFrameBytes() {
		return frameBytes;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameSummaryMessage#getIdleFrames()
     */
	@Override
    public long getIdleFrames() {
		return idleFrames;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameSummaryMessage#isInSync()
     */
	@Override
    public boolean isInSync() {
		return inSync;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameSummaryMessage#getNumFrames()
     */
	@Override
    public long getNumFrames() {
		return numFrames;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameSummaryMessage#getOutOfSyncBytes()
     */
	@Override
    public long getOutOfSyncBytes() {
		return outOfSyncBytes;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameSummaryMessage#getBadFrames()
     */
	@Override
    public long getBadFrames() {
		return badFrames;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameSummaryMessage#getOutOfSyncCount()
     */
	@Override
    public long getOutOfSyncCount() {
		return outOfSyncCount;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameSummaryMessage#getBitrate()
     */

	@Override
    public double getBitrate() {
		return this.bitrate;
	}
	
}
