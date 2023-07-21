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

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader;
import jpl.gds.ccsds.api.tm.frame.TelemetryFrameHeaderFactory;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.serialization.frame.Proto3TelemetryFrameMessage;
import jpl.gds.serialization.messages.Proto3AbstractMessage;
import jpl.gds.shared.holders.FrameIdHolder;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.HolderException;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.BaseBinaryMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.station.api.IStationInfoFactory;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.station.api.dsn.chdo.IChdoSfdu;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfoFactory;
import jpl.gds.tm.service.api.frame.ITelemetryFrameMessage;
import org.springframework.context.ApplicationContext;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * TransferFrameMessage is the message sent when a transfer frame
 * is detected. The header and trailer may be null or of zero-length
 * as well as populated.
 *
 *
 * MPCS-4599 - 10/30/17 - Refactored binary functions and
 *          BinaryParseHandler to utilize Proto3TelemetryFrameMessage
 *          messages instead of home-grown arbitrary packed binary
 *          messages. Added constructor.
 * MPCS-9461 - 03/08/18 - Added setTemplateContext function
 */
public class TelemetryFrameMessage extends Message implements ITelemetryFrameMessage {

    /**
     * A DSNInfo object to store the dsn information.
     */
    private final IStationTelemInfo   dsnInfo;
    /**
     * A IFrameInfo object to store the transfer frame info.
     */
    private final ITelemetryFrameInfo tfInfo;
    private final int                 numBytes;
    private final byte[]              tf;

    // Zero-length and NULL are OK here (inside; these are never null).
    private final HeaderHolder  header;
    private final TrailerHolder trailer;
      
    private IChdoSfdu chdoObj;

    private final FrameIdHolder       frameId = FrameIdHolder.getNextFrameId();

    /**
     * A constructor that takes various parameters to set the basic fields.
     *
     * Zero-length and NULL headers and trailers are OK.
     *
     * @param dsnInfo2 The dsn information
     * @param tfI The transfer frame info
     * @param bodySize The number of bytes
     * @param buff The buffer
     * @param off The offset
     * @param hdr The header 
     * @param tr  The trailer 
     */
    protected TelemetryFrameMessage(final IStationTelemInfo       dsnInfo2,
                                final ITelemetryFrameInfo    tfI,
                                final int           bodySize,
                                final byte[]        buff,
                                final int           off,
                                final HeaderHolder  hdr,
                                final TrailerHolder tr)
    {
        super(TmServiceMessageType.TelemetryFrame, java.lang.System.currentTimeMillis());
        dsnInfo = dsnInfo2;
        tfInfo = tfI;
        numBytes = bodySize;
        tf = new byte[numBytes];
        System.arraycopy(buff, off, tf, 0, numBytes);

        header = HeaderHolder.getSafeHolder(hdr);
        trailer = TrailerHolder.getSafeHolder(tr);
    }

    /**
     * Constructor that requires a Proto3TelemetryFrameMEssage. The supplied
     * IStationTelemInfo and ITelemetryFrameInfo should be constructed from data
     * contained by the supplied message
     *
     * @param msg
     *            the received protobuf message
     * @param dsnInfo
     *            the received IStationTelemInfo
     * @param tfI
     *            the received ITelemetryFrameInfo
     * @param trace
     *            the current application tracer
     * @throws InvalidProtocolBufferException
     *             an error occurred while parsing the message
     */
    protected TelemetryFrameMessage(final Proto3TelemetryFrameMessage msg, final IStationTelemInfo dsnInfo,
            final ITelemetryFrameInfo tfI, final Tracer trace) throws InvalidProtocolBufferException {
        super(TmServiceMessageType.TelemetryFrame, msg.getSuper());

        this.dsnInfo = dsnInfo;

        this.tfInfo = tfI;

        this.tf = msg.getTf().toByteArray();
        this.numBytes = tf.length;

        HeaderHolder tmpHeader = HeaderHolder.NULL_HOLDER;
        try {
            tmpHeader = new HeaderHolder(msg.getHeader());
        }
        catch (final HolderException e) {
            trace.error("Unable to instantiate frame header holder in Proto3TelemetryFrameMessage");
        }
        this.header = tmpHeader;

        TrailerHolder tmpTrailer = TrailerHolder.NULL_HOLDER;
        try {
            tmpTrailer = new TrailerHolder(msg.getTrailer());
        }
        catch (final HolderException e) {
            trace.error("Unable to instantiate frame trailer holder in Proto3TelemetryFrameMessage");
        }
        this.trailer = tmpTrailer;
    }

    /**
     * Basic function to get the number of bytes.
     *
     * @return Returns the number of bytes.
     */
    @Override
    public int getNumBodyBytes() {
        return numBytes;
    }

    /**
     * Return frame header.
     *
     * @return Frame header (never null)
     */
    @Override
    public HeaderHolder getRawHeader() {
        return header;
    }

    /**
     * Return frame trailer.
     *
     * @return Frame trailer (never null)
     */
    @Override
    public TrailerHolder getRawTrailer() {
        return trailer;
    }

    /**
     * Return frame id.
     *
     * @return Frame id (never null)
     *
     */
    @Override
    public FrameIdHolder getFrameId() {
        return frameId;
    }

    /**
     * Basic function to get the transfer frame info
     *
     * @return Returns the transfer frame info
     */
    @Override
    public ITelemetryFrameInfo getFrameInfo() {
        return tfInfo;
    }

    /**
     * Basic function to get the DSN info
     *
     * @return Returns the dsn info.
     */
    @Override
    public IStationTelemInfo getStationInfo() {
        return dsnInfo;
    }

    /**
     * Basic function to get the transfer frame.
     *
     * @return Returns the transfer frame.
     */
    @Override
    public byte[] getFrame() {
        return tf;
    }

    @Override
    public String toString() {
        return getOneLineSummary();
    }

    /**
     * This generates the Stax Xml.
     *
     * @param writer
     *            The XML Stream Writer to write to.
     * @throws XMLStreamException
     *             If there are issues writing to the xml stream.
     */
    @Override
    public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));
    }

    @Override
    public String getOneLineSummary() {
        return getType().toString() + " " + getEventTimeString() + " numBytes=" + numBytes + " stationInfo=" + dsnInfo
                + " tfInfo=" + tfInfo;
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.shared.message.IMessage#toBinary()
     *
     */
    @Override
    public synchronized byte[] toBinary() {

        return this.build().toByteArray();
    }

    @Override
    public Proto3TelemetryFrameMessage build() {
        final Proto3TelemetryFrameMessage.Builder retVal = Proto3TelemetryFrameMessage.newBuilder();

        retVal.setSuper((Proto3AbstractMessage) super.build());
        retVal.setDsnInfo(this.dsnInfo.build());
        retVal.setTfInfo(this.tfInfo.build());
        retVal.setTf(ByteString.copyFrom(this.tf));
        retVal.setHeader(this.getRawHeader().build());
        retVal.setTrailer(this.getRawTrailer().build());

        return retVal.build();
    }

    /**
     * BinaryParseHandler is the message-specific protobuf parse handler for creating
     * this Message from its binary representation.
     */
    public static class BinaryParseHandler extends BaseBinaryMessageParseHandler {

        private final ITelemetryFrameInfoFactory frameInfoFactory;
        private final IStationInfoFactory        stationInfoFactory;
        private final MissionProperties          missionProps;

        private final Tracer                     trace;

        /**
         * Constructor.
         *
         * @param context
         *            current application context
         */
        public BinaryParseHandler(final ApplicationContext context) {
            frameInfoFactory = context.getBean(ITelemetryFrameInfoFactory.class);
            stationInfoFactory = context.getBean(IStationInfoFactory.class);
            missionProps = context.getBean(MissionProperties.class);
            this.trace = TraceManager.getTracer(context, Loggers.UTIL);

        }

        /**
         * @{inheritDoc}
         */
        @Override
        public IMessage[] parse(final List<byte[]> content) throws IOException {

            for (final byte[] msgBytes : content) {

                final Proto3TelemetryFrameMessage tlmMsg = Proto3TelemetryFrameMessage.parseFrom(msgBytes);

                final IStationTelemInfo stationInfo = stationInfoFactory.create(tlmMsg.getDsnInfo());
                final ITelemetryFrameInfo info = frameInfoFactory.create();
                info.load(tlmMsg.getTfInfo());
                if (tlmMsg.getTfInfo().hasHeader()) {
                    ITelemetryFrameHeader header = TelemetryFrameHeaderFactory.create(missionProps, info.getFrameFormat());
                    header.load(tlmMsg.getTfInfo().getHeader().getHeaderBytes().toByteArray(), 0);

                    info.setHeader(header);
                }

                final ITelemetryFrameMessage message = new TelemetryFrameMessage(tlmMsg, stationInfo, info, trace);

                addMessage(message);
            }

            return getMessages();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.common.types.IIdleSupport#isIdle()
     */
    @Override
    public boolean isIdle() {
        return getFrameInfo().isIdle();
    }

    @Override
    public Integer getScid() {
        return getFrameInfo().getScid();
    }

    @Override
    public Integer getDssId() {
        return getStationInfo().getDssId();
    }

    @Override
    public Integer getVcid() {
        return getFrameInfo().getVcid();
    }

    @Override
    public void setTemplateContext(final Map<String, Object> map) {
        super.setTemplateContext(map);

        tfInfo.setTemplateContext(map);
        dsnInfo.setTemplateContext(map);

        map.put("rawHeaderLength", header != null ? header.getLength() : 0);
        map.put("rawTrailerLength", trailer != null ? trailer.getLength() : 0);
        map.put("length", numBytes);
        map.put("rct", getEventTimeString());
        map.put("rctExact", eventTime);
    }


    @Override
    public IChdoSfdu getChdoObject() {
        return chdoObj;
    }


    @Override
    public void setChdoObject(final IChdoSfdu chdo) {
        chdoObj = chdo;       
    }

}
