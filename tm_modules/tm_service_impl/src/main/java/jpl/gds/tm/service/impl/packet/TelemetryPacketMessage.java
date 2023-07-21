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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.context.ApplicationContext;

import com.google.protobuf.ByteString;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.serialization.messages.Proto3AbstractMessage;
import jpl.gds.serialization.packet.Proto3TelemetryPacketMessage;
import jpl.gds.serialization.packet.Proto3TelemetryPacketMessage.HasPacketIdCase;
import jpl.gds.serialization.packet.Proto3TelemetryPacketMessage.HasRctCase;
import jpl.gds.serialization.primitives.time.Proto3Adt;
import jpl.gds.shared.holders.FrameIdHolder;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.HolderException;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.BaseBinaryMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.FastDateFormat;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.dsn.chdo.IChdoSfdu;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfoFactory;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * PacketMessage carries a telemetry packet and its metadata on the
 * internal message bus. The metadata includes all information previously
 * parsed from the packet header, plus frame information if available,
 * and is encapsulated in an IPacketInfo object.
 *
 * MPCS-7289 - 4/30/15. major refactor to adhere to
 *          adjusted IPacketInfo interface, decouple from DSNInfo
 *          and IFrameInfo classes, add binary serialization and
 *          de-serialization, allow setting of RCT. Changes not 
 *          individually marked.
 *
 * MPCS-5932 09/02/15 Added Frame id support
 * MPCS-4599 - 10/30/17 - Refactored binary functions and
 * 			BinaryParseHandler to utilize Proto3TelemetryPacketMessage
 * 			messages instead of home-grown arbitrary packed binary
 * 			messages. Added constructor.
 */
public class TelemetryPacketMessage extends Message implements ITelemetryPacketMessage
{	

    private final ITelemetryPacketInfo pktInfo;
    private byte pkt[];

    private final HeaderHolder  header;
    private final TrailerHolder trailer;

    /** May be created when needed. Access through getter ONLY */
    private PacketIdHolder packetId = null;

    private FrameIdHolder frameId = FrameIdHolder.UNSUPPORTED;
    
    private IChdoSfdu chdoObj;

    private IAccurateDateTime          rct;

    /**
     * Creates an instance of PacketMessage. If the packet id is null,
     * we wait until we need it to create it.
     * 
     * @param pktI the IPacketInfo containing packet metadata
     * @param pi   Packet id or null
     * @param hdr  Header holder; may be null
     * @param tr   Trailer holder; may be null
     * @param fi   Frame id, never null
     * 
     */
    protected TelemetryPacketMessage(final ITelemetryPacketInfo    pktI,
                            final PacketIdHolder pi,
                            final HeaderHolder   hdr,
                            final TrailerHolder  tr,
                            final FrameIdHolder  fi)
    {
        super(TmServiceMessageType.TelemetryPacket, System.currentTimeMillis());

        pktInfo  = pktI;
        fromSse  = pktI.isFromSse();
        packetId = pi;
        header   = HeaderHolder.getSafeHolder(hdr);
        trailer  = TrailerHolder.getSafeHolder(tr);
        frameId  = fi;

        if (frameId == null)
        {
            throw new IllegalArgumentException("PacketMessage frame id cannot be null");
        }
    }


    /**
     * Creates an instance of packet message with the given packet
     * metadata information. There is no attached packet data yet.
     * 
     * @param pktI IPacketInfo object containing packet metadata
     * 
     */
    protected TelemetryPacketMessage(final ITelemetryPacketInfo pktI) {
        this(pktI, null, null, null, FrameIdHolder.UNSUPPORTED);
    }
    
    /**
     * 
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketMessage#setRct(java.util.Date)
     */
    @Override
    public void setRct(final IAccurateDateTime rctToSet) {
        this.rct = rctToSet;
    }

    /**
     * 
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketMessage#getRct()
     */
    @Override
    public IAccurateDateTime getRct() {
        if (rct == null) {
            rct = getEventTime();
        } 
        return rct;
    }

    /**
     * 
     * 
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketMessage#setPacket(byte[], int) 
     */
    @Override
    public synchronized void setPacket(final byte[] pktData, final int numBytes) {
        pkt = new byte[numBytes];
        System.arraycopy(pktData, 0, pkt, 0, numBytes);
    }

    /**
     * 
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketMessage#setPacket(byte[], int, int)
     */
    @Override
    public synchronized void setPacket(final byte[] pktData, final int off, final int numBytes) {
        pkt = new byte[numBytes];
        System.arraycopy(pktData, off, pkt, 0, numBytes);
    }

    /**
     * 
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketMessage#getNumBytes()
     */
    @Override
	public synchronized int getNumBytes() {
        return (pkt == null) ? 0 : pkt.length;
    }

    /**
     * 
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketMessage#getPacketInfo()
     */
    @Override
	public ITelemetryPacketInfo getPacketInfo() {
        return pktInfo;
    }

    /**
     * 
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketMessage#getPacket()
     */
    @Override
	@SuppressWarnings("EI_EXPOSE_REP")
    public synchronized byte[] getPacket() {
        return pkt;
    }

    /**
     * 
     * @see jpl.gds.shared.message.IMessage#toString()
     */
    @Override
    public String toString() {
        return "MSG:" + getType() + " " + getEventTimeString() 
                + (pkt == null ? "0" : pkt.length) 
                + " pktInfo=" + pktInfo;
    }
    
    /**
     * 
     * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
	public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException
    {
    	writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));
    }

    /**
     * 
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        return "Packet: " + this.pktInfo.getIdentifierString();
    }

    /**
     * 
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketMessage#getPacketId()
     */
    @Override
    public synchronized PacketIdHolder getPacketId()
    {
        if (packetId == null)
        {
            if (! fromSse)
            {
                packetId = PacketIdHolder.getNextFswPacketId();
            }
            else
            {
                packetId = PacketIdHolder.getNextSsePacketId();
            }
        }

        return packetId;
    }


    /**
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketMessage#getFrameId()
     *
     */
    @Override
    public FrameIdHolder getFrameId()
    {
        return frameId;
    }


    /**
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketMessage#getHeader()
     */
    @Override
    public HeaderHolder getHeader()
    {
        return header;
    }

    /**
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketMessage#getTrailer()
     */
    @Override
    public TrailerHolder getTrailer()
    {
        return trailer;
    }

    @Override
    public void setTemplateContext(final Map<String, Object> map) {
        // MPCS-9461 - 03/08/18 - updated to set everything that should be set
        super.setTemplateContext(map);
        if (pktInfo != null) {
            pktInfo.setTemplateContext(map);
        }

        map.put("HeaderLength", header != null ? header.getLength() : 0);
        map.put("TrailerLength", trailer != null ? trailer.getLength() : 0);
        map.put("length", getNumBytes());
        map.put("rct", FastDateFormat.format(rct, null, null));
        map.put("rctExact", rct.getTime());
    }
    
    /**
     * @see jpl.gds.shared.message.IMessage#toBinary()
     *
     */
    @Override
    public synchronized byte[] toBinary() {
    	
        return this.build().toByteArray();
    }
    
    @Override
    public Proto3TelemetryPacketMessage build(){
    	final Proto3TelemetryPacketMessage.Builder retVal = Proto3TelemetryPacketMessage.newBuilder();
    	
    	retVal.setSuper((Proto3AbstractMessage)super.build());
    	retVal.setPktInfo(this.getPacketInfo().build());
    	retVal.setPkt(ByteString.copyFrom(this.getPacket()));
    	retVal.setHeader(this.getHeader().build());
    	retVal.setTrailer(this.getTrailer().build());
    	if(this.getPacketId() != null){
    		retVal.setPacketId(this.getPacketId().build());
    	}
    	retVal.setFrameId(this.getFrameId().build());
    	if(this.getRct() != null){
    		retVal.setRct(Proto3Adt.newBuilder().setMilliseconds(this.getRct().getTime())
				.setNanoseconds(this.getRct().getNanoseconds()));
    	}
    	
    	return retVal.build();
    	
    }
    
	/**
     * Constructor that requires a Proto3TelemetryPacketMessage. The supplied
     * ITelemetryPacketInfo should be constructed from data contained by the
     * supplied message
     * 
     * @param msg
     *            the received protobuf message
     * @param pktInfo
     *            the corresponding ITelemetryPacketInfo
     * @param trace
     *            the tracer being used by the current application
     * @throws IOException
     *             an error occurs during parsing of the message
     */
    public TelemetryPacketMessage(final Proto3TelemetryPacketMessage msg, final ITelemetryPacketInfo pktInfo,
            final Tracer trace)
            throws IOException {
		super(TmServiceMessageType.TelemetryPacket, msg.getSuper());

		this.setFromSse(pktInfo.isFromSse());

		this.pktInfo = pktInfo;

		this.pkt = msg.getPkt().toByteArray();

		HeaderHolder tmpHeader = HeaderHolder.NULL_HOLDER;
		try {
			tmpHeader = new HeaderHolder(msg.getHeader());
		} catch (final HolderException e) {
            trace.error("Unable to instantiate frame header holder in Proto3TelemetryPacketMessage");
		}
		this.header = tmpHeader;

		TrailerHolder tmpTrailer = TrailerHolder.NULL_HOLDER;
		try {
			tmpTrailer = new TrailerHolder(msg.getTrailer());
		} catch (final HolderException e) {
            trace.error("Unable to instantiate frame trailer holder in Proto3TelemetryPacketMessage");
		}
		this.trailer = tmpTrailer;

		if (!msg.getHasPacketIdCase().equals(HasPacketIdCase.HASPACKETID_NOT_SET)) {
			try {
				this.packetId = new PacketIdHolder(msg.getPacketId());
			} catch (final HolderException e) {
                trace.error("Unable to instantiate frame packet ID holder in Proto3TelemetryPacketMessage");
			}
		}

		FrameIdHolder tmpFrameId = FrameIdHolder.UNSUPPORTED;
		try {
			tmpFrameId = new FrameIdHolder(msg.getFrameId());
		} catch (final HolderException e) {
            trace.error("Unable to instantiate frame frame ID holder in Proto3TelemetryPacketMessage");
		}
		this.frameId = tmpFrameId;

		if (!msg.getHasRctCase().equals(HasRctCase.HASRCT_NOT_SET)) {
			final Proto3Adt msgRct = msg.getRct();
			this.rct = new AccurateDateTime(msgRct.getMilliseconds(), msgRct.getNanoseconds());
		}

	}
    
    /**
     * BinaryParseHandler is the message-specific SAX parse handler for creating
     * this Message from its binary representation.
     * 
     */
    public static class BinaryParseHandler extends BaseBinaryMessageParseHandler {

    	 private final ITelemetryPacketInfoFactory pktInfoFactory;

        private final Tracer                      trace;
         
        /**
         * Constructor.
         * 
         * @param context
         *            current application context
         */
        public BinaryParseHandler(final ApplicationContext context) {
            this.trace = TraceManager.getTracer(context, Loggers.UTIL);
            pktInfoFactory = context.getBean(ITelemetryPacketInfoFactory.class);
         }

        /**
         * @{inheritDoc}
         * @see jpl.gds.shared.message.BaseBinaryMessageParseHandler#parse(byte[], int)
         */
        @Override
        public IMessage[] parse(final List<byte[]> content) throws IOException {
            for (final byte[] msgBytes : content) {

                final Proto3TelemetryPacketMessage pktMsg = Proto3TelemetryPacketMessage.parseFrom(msgBytes);

                final ITelemetryPacketInfo info = pktInfoFactory.create();
                info.load(pktMsg.getPktInfo());

                final ITelemetryPacketMessage message = new TelemetryPacketMessage(pktMsg, info, trace);

                addMessage(message);
            }
            
            return getMessages();
        }
    }

    /**
     * 
     * 
     * @see jpl.gds.common.types.IFillSupport#isFill()
     */
    @Override
    public boolean isFill() {
    	return getPacketInfo().isFill();
    }


    @Override
    public Integer getScid() {
        return getPacketInfo().getScid();
    }


    @Override
    public Integer getVcid() {
        return getPacketInfo().getVcid();
    }


    @Override
    public Integer getDssId() {
        return getPacketInfo().getDssId();
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
