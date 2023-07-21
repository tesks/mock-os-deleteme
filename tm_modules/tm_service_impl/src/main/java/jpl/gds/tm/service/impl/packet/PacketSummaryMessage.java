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
package jpl.gds.tm.service.impl.packet;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.context.api.IContextIdentification;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.message.BaseXmlMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.message.PublishableLogMessage;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.packet.IPacketSummaryMessage;
import jpl.gds.tm.service.api.packet.PacketSummaryRecord;

/**
 * This message summarizes statistics regarding packets and packet extraction.
 * 
 * MPCS-10266 - 12/14/18. Add CFDP packet counter.
 * 
 */

public class PacketSummaryMessage extends PublishableLogMessage implements
IPacketSummaryMessage {

    private long numFrameGaps;
    private long numFrameRegressions;
    private long numFrameRepeats;
    private long numValidPackets;
    private long numFillPackets;
    private long numInvalidPackets;

    /*
     * 12/14/18 - MPCS-10266. Add CFDP packet counter.
     */
    private long numCfdpPackets;
    
    /*
     * 11/25/13 - MPCS-5554. Add station packet counter.
     */
    private long numStationPackets;
    private Map<String, PacketSummaryRecord> packetSummaryMap = null;
    
    /**
     * Creates an instance of PacketSummaryMessage.
     * 
     * @param numGaps
     *            number of frame gaps
     * @param numRegressions
     *            number of frame regressions
     * @param numRepeats
     *            number of frame repeats
     * @param numFill
     *            number of fill packets
     * @param numInvalid
     *            number of invalid packets
     * @param numValid
     *            number of valid packets
     * @param numStation
     *            number of station packets
     * @param numCfdp
     *            number of cfdp packets
     * @param summaries
     *            map of packet APID to PacketSummaryRecord for each packet type
     */
    protected PacketSummaryMessage(final long numGaps, final long numRegressions, final long numRepeats, final long numFill, 
            final long numInvalid, final long numValid, final long numStation, final long numCfdp,
            final Map<String, PacketSummaryRecord> summaries) {
        this();
        setNumFrameGaps(numGaps);
        setNumFrameRegressions(numRegressions);
        setNumFrameRepeats(numRepeats);
        setNumFillPackets(numFill);
        setNumInvalidPackets(numInvalid);
        setNumValidPackets(numValid);
        setNumStationPackets(numStation);
        setNumCfdpPackets(numCfdp);
        setPacketSummaryMap(summaries);

    }
    
    /**
     * Constructor for use by the message parser.
     */
    protected PacketSummaryMessage() {
        super(TmServiceMessageType.TelemetryPacketSummary, TraceSeverity.INFO,
                LogMessageType.PACKET_SUMMARY);
    }

    @Override
    public String toString() {
        return getOneLineSummary();
    }

    @Override
    public String getMessage() {
        return getOneLineSummary();
    }

    /**
     * Sets the map of PacketSummaryObjects into the message.
     * 
     * @param map map of packet VCID/APID key to PacketSumamryObject
     */
    private synchronized void setPacketSummaryMap(final Map<String, PacketSummaryRecord> map) {
        packetSummaryMap = new HashMap<>(map);
    }

    @Override
    public synchronized Map<String, PacketSummaryRecord> getPacketSummaryMap() {
        return packetSummaryMap;
    }

    @Override
    public long getNumFrameGaps() {
        return numFrameGaps;
    }

    /**
     * Sets the number of Frame Gaps
     * 
     * @param numGaps
     *            The gap count to set.
     */
    private void setNumFrameGaps(final long numGaps) {
        this.numFrameGaps = numGaps;
    }

    @Override
    public long getNumCfdpPackets() {
        return numCfdpPackets;
    }

    /**
     * Sets the number of Frame Gaps
     * 
     * @param numGaps
     *            The gap count to set.
     */
    private void setNumCfdpPackets(final long numCfdp) {
        this.numCfdpPackets = numCfdp;
    }

    @Override
    public long getNumFrameRegressions() {
        return numFrameRegressions;
    }

    /**
     * Sets the number of Frame Regressions
     * 
     * @param numRegressions
     *            The regression count to set.
     */
    private void setNumFrameRegressions(final long numRegressions) {
        this.numFrameRegressions = numRegressions;
    }

    @Override
    public long getNumFrameRepeats() {
        return numFrameRepeats;
    }

    /**
     * Sets the number of Frame Repeats
     * 
     * @param numRepeats
     *            The repeat count to set.
     */
    private void setNumFrameRepeats(final long numRepeats) {
        this.numFrameRepeats = numRepeats;
    }

    @Override
    public long getNumFillPackets() {
        return numFillPackets;
    }

    /**
     * Sets the number of Fill Packets.
     * 
     * @param numFillPackets
     *            The fill count to set.
     */
    private void setNumFillPackets(final long numFillPackets) {
        this.numFillPackets = numFillPackets;
    }

    @Override
    public long getNumStationPackets() {
        return numStationPackets;
    }

    /**
     * Sets the number of Station Monitor Packets.
     * 
     * @param numStationPackets
     *            The station packet count to set.
     *            
     */
    private void setNumStationPackets(final long numStationPackets) {
        this.numStationPackets = numStationPackets;
    }

    @Override
    public long getNumInvalidPackets() {
        return numInvalidPackets;
    }

    /**
     * Sets the number of Invalid Packets.
     * 
     * @param numInvalidPackets
     *            The packet count to set.
     */
    private void setNumInvalidPackets(final long numInvalidPackets) {
        this.numInvalidPackets = numInvalidPackets;
    }

    @Override
    public long getNumValidPackets() {
        return numValidPackets;
    }

    /**
     * Sets the number of Valid Packets.
     * 
     * @param numPackets
     *            The packet count to set.
     */
    private void setNumValidPackets(final long numPackets) {
        numValidPackets = numPackets;
    }

    @Override
    public synchronized void setTemplateContext(final Map<String, Object> map) {
        super.setTemplateContext(map);

        // define the new summary information keys

        if (getEventTime() != null) {
            map.put(IMessage.EVENT_TIME_TAG, getEventTimeString());
        } else {
            map.put(IMessage.EVENT_TIME_TAG, "");
        }
        map.put("numFrameGaps", numFrameGaps);
        map.put("numFrameRegressions", numFrameRegressions);
        map.put("numFrameRepeats", numFrameRepeats);
        map.put("numIdlePackets", numFillPackets);
        map.put("numInvalidPackets", numInvalidPackets);
        map.put("numValidPackets", numValidPackets);
        /*
         * 11/25/13 - MPCS-5554. Add station packet counter.
         */
        map.put("numStationPackets", numStationPackets);

        /*
         * 12/14/18 - MPCS-10266. Add CFDP packet counter.
         */
        map.put("numCfdpPackets", numCfdpPackets);

        if (packetSummaryMap != null) {
            final Collection<PacketSummaryRecord> sums = packetSummaryMap.values();
            map.put("summaryList", new ArrayList<PacketSummaryRecord>(sums));
        }
    }

    @Override
    public synchronized void generateStaxXml(final XMLStreamWriter writer)
            throws XMLStreamException {
        final DateFormat df = TimeUtility.getFormatterFromPool();
        try {
            writer.writeStartElement(MessageRegistry.getDefaultInternalXmlRoot(getType())); // <PacketExtractSumMessage>
            writer.writeAttribute(IMessage.EVENT_TIME_TAG, getEventTimeString());
            
            super.generateXmlForContext(writer);

            writer.writeStartElement("fromSSE"); // <fromSSE>
            writer.writeCharacters(Boolean.toString(isFromSse()));
            writer.writeEndElement(); // </fromSSE>

            writer.writeStartElement("numFrameGaps"); // <numFrameGaps>
            writer.writeCharacters(Long.toString(numFrameGaps));
            writer.writeEndElement(); // </numFrameGaps>

            writer.writeStartElement("numFrameRegressions"); // <numFrameRegressions>
            writer.writeCharacters(Long.toString(numFrameRegressions));
            writer.writeEndElement(); // </numFrameRegressions>

            writer.writeStartElement("numFrameRepeats"); // <numFrameRepeats>
            writer.writeCharacters(Long.toString(numFrameRepeats));
            writer.writeEndElement(); // </numFrameRepeats>

            writer.writeStartElement("numValidPackets"); // <numValidPackets>
            writer.writeCharacters(Long.toString(numValidPackets));
            writer.writeEndElement(); // </numValiPackets>

            writer.writeStartElement("numIdlePackets"); // <numIdlePackets>
            writer.writeCharacters(Long.toString(numFillPackets));
            writer.writeEndElement(); // </numIdlePackets>

            writer.writeStartElement("numInvalidPackets"); // <numInvalidPackets>
            writer.writeCharacters(Long.toString(numInvalidPackets));
            writer.writeEndElement(); // </numInvalidPackets>


            writer.writeStartElement("numStationPackets"); // <numStationPackets>
            writer.writeCharacters(Long.toString(numStationPackets));
            writer.writeEndElement(); // </numStationPackets>

            /*
             *  12/14/18 - MPCS-10266. Add CFDP packet counter.
             */
            writer.writeStartElement("numCfdpPackets"); // <numCfdpPackets>
            writer.writeCharacters(Long.toString(numCfdpPackets));
            writer.writeEndElement(); // </numCfdpPackets>

            if (packetSummaryMap != null && !packetSummaryMap.isEmpty()) {
                writer.writeStartElement("packetSummaries"); // <packetSummaries>
                final Collection<PacketSummaryRecord> sums = packetSummaryMap.values();

                for (final PacketSummaryRecord pso : sums) {
                    writer.writeStartElement("packetSummary"); // <packetSummary>
                    writer.writeAttribute("vcid", Long.toString(pso.getVcid()));
                    writer.writeAttribute("apid", Long.toString(pso.getApid()));
                    writer.writeAttribute("apidName",
                            (pso.getApidName() == null ? "Unknown" : pso
                                    .getApidName()));
                    writer.writeAttribute("instanceCount", Long.toString(pso
                            .getInstanceCount()));
                    writer.writeAttribute("lastScetTime", pso.getLastScetStr());
                    if (pso.getLastLst() != null) {
                        writer.writeAttribute("lastLstTime", pso.getLastLstStr());
                    }
                    writer.writeAttribute("lastSclkTime", pso.getLastSclkStr());
                    writer.writeAttribute("lastErtTime", pso.getLastErtStr());
                    writer.writeAttribute("lastSequenceNum", Long.toString(pso
                            .getSeqCount()));
                    writer.writeEndElement(); // </packetSummary>
                }
                writer.writeEndElement(); // </packetSummaries>
            }

            writer.writeEndElement(); // </PacketExtractSumMessage>
        } finally {
            TimeUtility.releaseFormatterToPool(df);
        }
    }

    /**
     * 
     * ParseHandler is the message-specific SAX parse handler for creating this
     * Message from its XML representation.
     * 
     */
    public static class XmlParseHandler extends BaseXmlMessageParseHandler {

        private PacketSummaryMessage msg;
        private HashMap<String, PacketSummaryRecord> summaryMap;
		private final Integer scid;

        /**
         * Constructor.
         * 
         * @param appContext the current application context
         */
        public XmlParseHandler(final ApplicationContext appContext) {
        	scid = appContext.getBean(IContextIdentification.class).getSpacecraftId();
        }

        @Override
        public void startElement(final String uri, final String localName,
                final String qname, final Attributes attr) throws SAXException {
            super.startElement(uri, localName, qname, attr);

            if (qname.equals(MessageRegistry.getDefaultInternalXmlRoot(TmServiceMessageType.TelemetryPacketSummary))) {
            	setInMessage(true);
                msg = new PacketSummaryMessage();
                msg.setEventTime(getDateFromAttr(attr, IMessage.EVENT_TIME_TAG));
                addMessage(msg);
            } else if (qname.equals("packetSummaries")) {
                summaryMap = new HashMap<>();
            } else if (qname.equalsIgnoreCase("packetSummary")) {
                final PacketSummaryRecord sum = new PacketSummaryRecord();
                String aValue = attr.getValue("vcid");
                if (aValue != null) {
                    sum.setVcidValue(Long.valueOf(aValue));
                }
                aValue = attr.getValue("apid");
                if (aValue != null) {
                    sum.setApid(Long.valueOf(aValue));
                }
                aValue = attr.getValue("apidName");
                sum.setApidName(aValue);

                aValue = attr.getValue("instanceCount");
                if (aValue != null) {
                    sum.setInstanceCount(Long.valueOf(aValue));
                }
                aValue = attr.getValue("lastScetTime");
                if (aValue != null) {
                    try {
                        sum.setLastScet(new AccurateDateTime(aValue));
                    } catch (final java.text.ParseException e) {
                        e.printStackTrace();
                    }
                }
                aValue = attr.getValue("lastLstTime");
                if (aValue != null) {
                    try {
                        sum.setLastLst(LocalSolarTimeFactory.getNewLst(aValue, scid));
                    } catch (final java.text.ParseException e) {
                        e.printStackTrace();
                    }
                }
                aValue = attr.getValue("lastSclkTime");
                if (aValue != null) {
                    sum.setLastSclk(sclkFmt.valueOf(aValue));
                }
                aValue = attr.getValue("lastErtTime");
                if (aValue != null) {
                    try {
                        sum.setLastErt(new AccurateDateTime(aValue));
                    } catch (final java.text.ParseException e) {
                        e.printStackTrace();
                    }
                }
                aValue = attr.getValue("lastSequenceNum");
                if (aValue != null) {
                    sum.setSeqValue(Long.valueOf(aValue));
                }
                summaryMap.put(sum.getVcid() + "/" + sum.getApid(), sum);
            }
        }

        @Override
        public void endElement(final String uri, final String localName,
                final String qname) throws SAXException {
            super.endElement(uri, localName, qname);

            if (qname.equals(MessageRegistry.getDefaultInternalXmlRoot(TmServiceMessageType.TelemetryPacketSummary))) {
            	setInMessage(false);
            } else if (qname.equalsIgnoreCase("numFrameGaps")) {
                msg.setNumFrameGaps(this.getIntFromBuffer());
            } else if (qname.equalsIgnoreCase("numFrameRegressions")) {
                msg.setNumFrameRegressions(this.getIntFromBuffer());
            } else if (qname.equalsIgnoreCase("numFrameRepeats")) {
                msg.setNumFrameRepeats(this.getIntFromBuffer());
            } else if (qname.equalsIgnoreCase("numIdlePackets")) {
                msg.setNumFillPackets(this.getLongFromBuffer());
            } else if (qname.equalsIgnoreCase("numValidPackets")) {
                msg.setNumValidPackets(this.getLongFromBuffer());
            } else if (qname.equalsIgnoreCase("numInvalidPackets")) {
                msg.setNumInvalidPackets(this.getLongFromBuffer());
            } else if (qname.equalsIgnoreCase("numStationPackets")) {
                msg.setNumStationPackets(this.getLongFromBuffer());
            }
            else if (qname.equalsIgnoreCase("numCfdpPackets")) {
                /*
                 *  12/14/18 - MPCS-10266. Add CFDP packet counter.
                 */
                msg.setNumCfdpPackets(this.getLongFromBuffer());
            }
            else if (qname.equalsIgnoreCase("packetSummaries")) {
                msg.setPacketSummaryMap(summaryMap);
            } else if (qname.equalsIgnoreCase("packetSummary")) {

            }
        }
    }

    @Override
    public String getOneLineSummary() {
        return "Frame Gaps=" + numFrameGaps
                + ", Frame Regressions=" + numFrameRegressions
                + ", Repeat Frames=" + numFrameRepeats
                + ", Valid Packets=" + numValidPackets + 
                ", Bad Packets=" + numInvalidPackets + 
                ", Fill Packets=" + numFillPackets +
                ", Station Monitor Packets=" + numStationPackets + 
                ", CFDP Packets=" + numCfdpPackets;
    }
 
}
