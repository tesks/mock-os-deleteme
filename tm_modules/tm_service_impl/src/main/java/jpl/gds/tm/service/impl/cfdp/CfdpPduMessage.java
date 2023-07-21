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
package jpl.gds.tm.service.impl.cfdp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.context.ApplicationContext;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.ccsds.api.cfdp.ICfdpPdu;
import jpl.gds.ccsds.api.cfdp.ICfdpPduFactory;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.serialization.holder.Proto3FrameIdHolder;
import jpl.gds.serialization.messages.Proto3AbstractMessage;
import jpl.gds.serialization.tm_impl.Proto3CfdpPduMessage;
import jpl.gds.serialization.tm_impl.Proto3CfdpPduMessage.ContainerIdsCase;
import jpl.gds.serialization.tm_impl.Proto3CfdpPduMessageFrames;
import jpl.gds.shared.holders.FrameIdHolder;
import jpl.gds.shared.holders.HolderException;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.message.BaseBinaryMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTime;
import jpl.gds.shared.time.Sclk;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.cfdp.ICfdpPduMessage;
import jpl.gds.tm.service.api.frame.ITelemetryFrameMessage;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * A message class for carrying CFDP PDUs and their metadata.
 *
 *
 * @since R8
 *
 * MPCS-9048 - 01/08/18 - Added function for packets
 * MPCS-9150 - 01/23/18 - Added third constructor, updated other constructors,
 *          added build and toBinary functions, added BinaryParseHandler
 * MPCS-9950 - 07/16/18 - Added new required fields
 * MPCS-10002 - 09/25/18 - Make pdu not final and add setter to allow link simulator to manipulate
 *                                              PDU data while retaining all the metadata
 */
public class CfdpPduMessage extends Message implements ICfdpPduMessage {

    private ICfdpPdu pdu;
    private List<FrameIdHolder> frameIds = new ArrayList<>();
    private PacketIdHolder      packetId;

    // MPCS-9950 - 07/16/18 - Added fields
    private long sessionId;
    private int hostId;
    private int fragment;
    private String sessionName;
    private String host;
    private String user;
    private String fswDictionaryDir;
    private String fswVersion;
    private String venueType;
    private String testbedName;
    private String outputDir;
    private int scid;
    private int apid;
    private String apidName;
    private int vcid;
    private int relayScid;
    private int sequenceCount;

    private ISclk             sclk;
    private ILocalSolarTime   lst;
    private IAccurateDateTime scet;
    private IAccurateDateTime  ert;

    private boolean simulatorGenerated;

    /**
     * @param frames
     *            frames that contained this PDU
     * @param pdu
     *            the CFDP PDU object itself
     * @param  context
     *            Context Configuration object
     */
    protected CfdpPduMessage(final List<ITelemetryFrameMessage> frames,
                             final ICfdpPdu pdu, IContextConfiguration context) {
        this(frames, null, pdu, context);
    }

    /**
     * @param packet
     *            packet that contained this PDU
     * @param pdu
     *            the CFDP PDU object itself
     * @param  context
     *            Context Configuration object
     */
    protected CfdpPduMessage(final ITelemetryPacketMessage packet,
            final ICfdpPdu pdu, IContextConfiguration context) {
        this(null, packet, pdu, context);
    }

    /**
     * Constructor to tag the object as simulator-generated.
     *
     * @param packet
     *            packet that contained this PDU
     * @param pdu
     *            the CFDP PDU object itself
     * @param context
     *            Context Configuration object
     * @param simulatorGenerated
     *            Simulator-generated flag
     */
    protected CfdpPduMessage(final ITelemetryPacketMessage packet,
                             final ICfdpPdu pdu, IContextConfiguration context, final boolean simulatorGenerated) {
        this(null, packet, pdu, context);
        this.simulatorGenerated = simulatorGenerated;
    }

    /**
     * @param frames
     *            frames that contained this PDU
     * @param packet
     *            packetthat contained this PDU
     * @param pdu
     *            the CFDP PDU object itself
     * @param  context
     *            Context Configuration object
     */
    protected CfdpPduMessage(final List<ITelemetryFrameMessage> frames, final ITelemetryPacketMessage packet,
                             final ICfdpPdu pdu, final IContextConfiguration context) {
        super(TmServiceMessageType.CfdpPdu, System.currentTimeMillis());
        this.pdu = pdu;
        if (frames == null) {
            this.frameIds = new ArrayList<>();
        }
        if(frames != null) {
            for (ITelemetryFrameMessage frame : frames) {
                this.frameIds.add(frame.getFrameId());
            }
        }
        if(packet != null) {
            this.packetId = packet.getPacketId();
        }

        //from context
        if(context.getContextId().getNumber() != null) {
            this.sessionId = context.getContextId().getNumber();
        }
        if(context.getContextId().getHostId() != null) {
            this.hostId = context.getContextId().getHostId();
        }
        if(context.getContextId().getFragment() != null) {
            this.fragment = context.getContextId().getFragment();
        }
        this.sessionName = context.getContextId().getName();
        this.host = context.getContextId().getHost();
        this.user = context.getContextId().getUser();
        this.fswDictionaryDir = context.getDictionaryConfig().getFswDictionaryDir();
        this.fswVersion = context.getDictionaryConfig().getFswVersion();
        this.venueType = context.getVenueConfiguration().getVenueType().name();
        this.testbedName = context.getVenueConfiguration().getTestbedName();
        this.outputDir = context.getGeneralInfo().getOutputDir();
        if(context.getContextId().getSpacecraftId() != null) {
            this.scid = context.getContextId().getSpacecraftId();
        }

        //using frames - get info from first frame
        if(frames != null &&  !frames.isEmpty()) {
            ITelemetryFrameMessage frame = frames.get(0);
            if(frame.getVcid() != null) {
                this.vcid = frame.getVcid();
            }
            //no access to APID
            this.relayScid = frame.getStationInfo().getRelayScid();
            //only access to ERT via station info
            this.ert = frame.getStationInfo().getErt();
            this.sequenceCount = frame.getFrameInfo().getSeqCount();
        }
        //using packets
        else if(packet != null){
            this.apid = packet.getPacketInfo().getApid();
            this.apidName = packet.getPacketInfo().getApidName();
            if(packet.getPacketInfo().getVcid() != null) {
                this.vcid = packet.getPacketInfo().getVcid();
            }
            this.relayScid = packet.getPacketInfo().getRelayScid();
            this.sclk = packet.getPacketInfo().getSclk();
            this.lst = packet.getPacketInfo().getLst();
            this.ert = packet.getPacketInfo().getErt();
            this.scet = packet.getPacketInfo().getScet();
            this.sequenceCount = packet.getPacketInfo().getSeqCount();
        }
    }

    @Override
    public long getSessionId() {
        return sessionId;
    }

    @Override
    public long getHostId() {
        return hostId;
    }

    @Override
    public long getFragment() {
        return fragment;
    }

    @Override
    public String getSessionName() {
        return sessionName;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getFswDictionaryDir() {
        return fswDictionaryDir;
    }

    @Override
    public String getFswVersion() {
        return fswVersion;
    }

    @Override
    public String getVenueType() {
        return venueType;
    }

    @Override
    public String getOutputDir() {
        return outputDir;
    }

    @Override
    public String getTestbedName() {
        return testbedName;
    }

    @Override
    public int getScid() {
        return scid;
    }

    @Override
    public int getApid() {
        return apid;
    }

    @Override
    public String getApidName() {
        return apidName;
    }

    @Override
    public int getVcid() {
        return vcid;
    }

    @Override
    public int getRelayScid() {
        return relayScid;
    }

    @Override
    public int getSequenceCount() {
        return sequenceCount;
    }

    @Override
    public ISclk getSclk() {
        return sclk;
    }

    @Override
    public ILocalSolarTime getLst() {
        return lst;
    }

    @Override
    public IAccurateDateTime getScet() {
        return scet;
    }

    @Override
    public IAccurateDateTime getErt() {
        return ert;
    }

    /**
     * Sets the session ID
     * @param sessionId Session ID
     */
    public void setSessionId(final long sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Sets the hist ID
     * @param hostId Host ID
     */
    public void setHostId(final int hostId) {
        this.hostId = hostId;
    }


    /**
     * Sets the fragment
     * @param fragment Fragment
     */
    public void setFragment(final int fragment) {
        this.fragment = fragment;
    }

    /**
     * Sets the session name
     * @param sessionName Session name
     */
    public void setSessionName(final String sessionName) {
        this.sessionName = sessionName;
    }

    /**
     * Sets the host name
     * @param host Host name
     */
    public void setHost(final String host) {
        this.host = host;
    }

    /**
     * Sets the user name
     * @param user User name
     */
    public void setUser(final String user) {
        this.user = user;
    }

    /**
     * Sets the flight dictionary directory
     * @param fswDictionaryDir FSD firectory
     */
    public void setFswDictionaryDir(final String fswDictionaryDir) {
        this.fswDictionaryDir = fswDictionaryDir;
    }

    /**
     * Sets the flight dictionary version
     * @param fswVersion FSW version
     */
    public void setFswVersion(final String fswVersion) {
        this.fswVersion = fswVersion;
    }

    /**
     * Sets the venue type
     * @param venueType venue type
     */
    public void setVenueType(final String venueType) {
        this.venueType = venueType;
    }

    /**
     * Sets the output directory
     * @param outputDir Output directory
     */
    public void setOutputDir(final String outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * Sets the spacecraft ID
     * @param scid Spacecraft ID
     */
    public void setScid(final int scid) {
        this.scid = scid;
    }

    /**
     * Sets the Application process ID
     * @param apid Application process ID
     */
    public void setApid(final int apid) {
        this.apid = apid;
    }

    /** Sets the product type (Apid name)
     *
     * @param apidName Product type
     */
    public void setApidName(final String apidName) {
        this.apidName = apidName;
    }

    /**
     * Sets the Virtual Channel ID
     * @param vcid VC ID
     */
    public void setVcid(final int vcid) {
        this.vcid = vcid;
    }

    /**
     * Sets the testbed name
     * @param testbedName Testbed name
     */
    public void setTestbedName(final String testbedName) {
        this.testbedName = testbedName;
    }

    /**
     * Sets the relay spacecraft ID
     * @param relayScid Relay SCID
     */
    public void setRelayScid(final int relayScid) {
        this.relayScid = relayScid;
    }

    /**
     * Sets the sequence count
     * @param sequenceCount Sequence Count
     */
    public void setSequenceCount(final int sequenceCount) {
        this.sequenceCount = sequenceCount;
    }

    /**
     * Sets the spacecraft clock (SCLK)
     * @param sclk SCLK
     */
    public void setSclk(final ISclk sclk) {
        this.sclk = sclk;
    }

    /**
     * Sets the local solar time (LST)
     * @param  lst LST
     */
    public void setLst(final ILocalSolarTime lst) {
        this.lst = lst;
    }

    /**
     * Sets the spacecraft event time (SCET)
     * @param scet SCET
     */
    public void setScet(final IAccurateDateTime scet) {
        this.scet = scet;
    }

    /**
     * Sets the sthe earth receive time (ERT)
     * @param ert ERT
     */
    public void setErt(final IAccurateDateTime ert) {
        this.ert = ert;
    }

    /**
     * Constructor for reconstituting a protobuf message
     *
     * @param msg
     *            the CFDP PDU message in protobuf format
     * @param pdu
     *            the CFDP PDU that was contained in the protobuf message
     * @throws InvalidProtocolBufferException
     *             an error was encountered with the context map of the protobuf message
     */
    protected CfdpPduMessage(final Proto3CfdpPduMessage msg, final ICfdpPdu pdu) throws InvalidProtocolBufferException {
        super(TmServiceMessageType.CfdpPdu, msg.getSuper());

        this.pdu = pdu;
        sessionId = msg.getSessionId();
        hostId = msg.getHostId();
        fragment = msg.getFragment();
        sessionName = msg.getSessionName();
        host = msg.getHost();
        user = msg.getUser();
        fswDictionaryDir = msg.getFswDictionaryDir();
        fswVersion = msg.getFswVersion();
        venueType = msg.getVenueType();
        testbedName = msg.getTestbedName();
        outputDir = msg.getOutputDir();
        scid = msg.getScid();
        apid = msg.getApid();
        if(!msg.getHasApidNameCase().equals(Proto3CfdpPduMessage.HasApidNameCase.HASAPIDNAME_NOT_SET)) {
            apidName = msg.getApidName();
        }
        vcid = msg.getVcid();
        relayScid = msg.getRelayScid();
        sequenceCount = msg.getSequenceCount();
        if(!msg.getHasSclkCase().equals(Proto3CfdpPduMessage.HasSclkCase.HASSCLK_NOT_SET)) {
            sclk = new Sclk(msg.getSclk());
        }
        if(!msg.getHasLstCase().equals(Proto3CfdpPduMessage.HasLstCase.HASLST_NOT_SET)) {
            lst = new LocalSolarTime(scid);
            lst.loadLocalSolarTime(msg.getLst());
        }

        if(!msg.getHasScetCase().equals(Proto3CfdpPduMessage.HasScetCase.HASSCET_NOT_SET)) {
            scet = new AccurateDateTime(msg.getScet());
        }
        ert = new AccurateDateTime(msg.getErt());

        if (msg.getContainerIdsCase().equals(ContainerIdsCase.PACKETID)) {
            try {
                packetId = new PacketIdHolder(msg.getPacketId());
            }
            catch (final HolderException e) {
                // shouldn't be a bad holder at this point, just drop exception and ignore
            }
        }
        else if (msg.getContainerIdsCase().equals(ContainerIdsCase.FRAMEIDS)
                && !msg.getFrameIds().getSingleFrameIdList().isEmpty()) {

            frameIds = new ArrayList<>();

            for (final Proto3FrameIdHolder frameId : msg.getFrameIds().getSingleFrameIdList()) {
                try {
                    frameIds.add(new FrameIdHolder(frameId));
                }
                catch (final HolderException e) {
                    // shouldn't be a bad holder at this point, just drop exception and ignore
                }
            }
        }

        simulatorGenerated = msg.getSimulatorGenerated();
    }

    @Override
    public ICfdpPdu getPdu() {
        return pdu;
    }

    @Override
    public void generateStaxXml(final XMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));
    }

    @Override
    public String getOneLineSummary() {
        return pdu.toString() + ", fromSimulator=" + fromSimulator();
    }

    @Override
    public String toString() {
        return getOneLineSummary();
    }

    @Override
    public Proto3CfdpPduMessage build() {
        final Proto3CfdpPduMessage.Builder retVal = Proto3CfdpPduMessage.newBuilder();

        retVal.setSuper((Proto3AbstractMessage)super.build());

        retVal.setPduData(ByteString.copyFrom(this.pdu.getData()));

        retVal.setSessionId(sessionId);
        retVal.setHostId(hostId);
        retVal.setFragment(fragment);
        retVal.setSessionName(sessionName);
        retVal.setUser(user);
        retVal.setHost(host);
        retVal.setFswDictionaryDir(fswDictionaryDir);
        if(fswVersion != null) {
            retVal.setFswVersion(fswVersion);
        }
        retVal.setVenueType(venueType);
        if(testbedName != null) {
            retVal.setTestbedName(testbedName);
        }
        retVal.setOutputDir(outputDir);
        retVal.setScid(scid);
        retVal.setApid(apid);
        if(apidName != null){
            retVal.setApidName(apidName);
        }
        retVal.setVcid(vcid);
        retVal.setRelayScid(relayScid);
        retVal.setSequenceCount(sequenceCount);
        if(sclk != null){
            retVal.setSclk(sclk.buildSclk());
        }
        if(lst != null){
            retVal.setLst(lst.buildLocalSolarTime());
        }
        if(scet != null){
            retVal.setScet(scet.buildAccurateDateTime());
        }
        if(ert != null) {
            retVal.setErt(ert.buildAccurateDateTime());
        }

        if (packetId != null) {
            retVal.setPacketId(packetId.build());
        }
        else if (frameIds != null) {
            final Proto3CfdpPduMessageFrames.Builder frameIdsHolder = Proto3CfdpPduMessageFrames.newBuilder();

            for (final FrameIdHolder frameId : frameIds) {
                frameIdsHolder.addSingleFrameId(frameId.build());
            }
        }

        retVal.setSimulatorGenerated(simulatorGenerated);

        return retVal.build();
    }

    @Override
    public byte[] toBinary() {
        return build().toByteArray();
    }

    /**
     * BinaryParseHandler is the message-specific protobuf parse handler for creating
     * this Message from its binary representation.
     */
    public static class BinaryParseHandler extends BaseBinaryMessageParseHandler {


        private final ICfdpPduFactory             pduFactory;

        /**
         * Constructor.
         *
         * @param context
         *            current application context
         */
        public BinaryParseHandler(final ApplicationContext context) {
            pduFactory = context.getBean(ICfdpPduFactory.class);

        }

        @Override
        public IMessage[] parse(final List<byte[]> content) throws IOException {
            for (final byte[] msgBytes : content) {
                final Proto3CfdpPduMessage pduMsg = Proto3CfdpPduMessage.parseFrom(msgBytes);

                final ICfdpPdu pdu = pduFactory.createPdu(pduMsg.getPduData().toByteArray());

                final ICfdpPduMessage message = new CfdpPduMessage(pduMsg, pdu);

                addMessage(message);
            }
            return getMessages();
        }

    }

    /**
     * Check if message object was generated by a simulator.
     *
     * @return true if a simulator generated this message, false otherwise
     */
    @Override
    public boolean fromSimulator() {
        return simulatorGenerated;
    }

    /**
     * Set the flag to indicate whether this message is generated from a simulator or not.
     *
     * @param fromSimulator
     */
    @Override
    public void setFromSimulator(boolean fromSimulator) {
        this.simulatorGenerated = fromSimulator;
    }

    /**
     * Set the PDU for this message.
     *
     * @param pdu
     */
    @Override
    public void setPdu(ICfdpPdu pdu) {
        this.pdu = pdu;
    }

}
