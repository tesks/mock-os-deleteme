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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.dictionary.api.channel.PacketHeaderFieldName;
import jpl.gds.serialization.packet.Proto3TelemetryPacketInfo;
import jpl.gds.serialization.packet.Proto3TelemetryPacketInfo.HasDssIdCase;
import jpl.gds.serialization.packet.Proto3TelemetryPacketInfo.HasErtCase;
import jpl.gds.serialization.packet.Proto3TelemetryPacketInfo.HasScetCase;
import jpl.gds.serialization.packet.Proto3TelemetryPacketInfo.HasScidCase;
import jpl.gds.serialization.packet.Proto3TelemetryPacketInfo.HasSclkCase;
import jpl.gds.serialization.packet.Proto3TelemetryPacketInfo.HasSolCase;
import jpl.gds.serialization.packet.Proto3TelemetryPacketInfo.HasVcidCase;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTime;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;

/**
 * TelemetryPacketInfo stores metadata about packets that can be passed around the
 * system. It implements the ITelemetryPacketInfo interface for use in adaptations.
 * <p>
 * <br>
 * CAVEAT: This class is NOT thread safe for performance reasons. If the
 * attributes of the instance are changed during a toBinary() or
 * parseFromBinary() call on the same instance, results are unpredictable. The
 * only alternative seems to be to synchronize every set method and the to/from
 * binary methods. Developers take care.
 * 
 *
 * MPCS-7289 - 4/30/15. Huge refactor. Added members from
 *          IFrameInfo and DSNInfo objects, so this object and not those can be
 *          carried downstream. Added packet header members that were not
 *          represented. Removed unused members and XML serialization methods.
 *          No longer implements Templatable or StaxSerializable. Added binary
 *          serialization and deserialization methods. Changed handling of
 *          static header length members. Other than new methods, changes are
 *          not individually marked.
 * MPCS-4599 - 10/30/17 - Refactored binary functions to utilize
 * 			Proto3TelemetryPacketInfo messages instead of home-grown arbitrary
 * 			packed binary messages. Added build and load functions for creating
 * 			and parsing Proto3TelemetryPacketInfo messages.
 */
class TelemetryPacketInfo implements ITelemetryPacketInfo
{
    
    /** Caches the flag indicating whether LST times are used */
    private static final boolean USES_SOL = TimeProperties.getInstance().usesLst();
    
    private static final String UNKNOWN_APID_NAME = "Unknown";


    /** Default secondary header length from the configuration. */

    private boolean fromSse;
    private int apid = 0;
    private int seqCount = 0;
    private int size = 0;
    private byte groupingFlags = 0;
    private boolean isFill;
    private ISclk                sclk;
    private IAccurateDateTime scet;
    private IAccurateDateTime ert;
    private ILocalSolarTime sol;
    private boolean secondaryHeaderFlag = false;
    private List<Long> sourceVcfcs = null;
    private Integer vcid = null;    
    private Integer scid = Integer.valueOf(0);
    private double bitrate;
    private int relayScid;
    private String frameType = null;
    private byte version;
    private byte type;
    private String apidName;

    /** Default primary header length from the configuration. */
 
    private int primaryHeaderLength; 
    // MPCS-8198  - 05/17/2016: Removed default secondaryHeaderLength.
    private int secondaryHeaderLength; 


    private Integer dssId = Integer.valueOf(StationIdHolder.UNSPECIFIED_VALUE);
    private ISpacePacketHeader header;

    /**
     * Creates an instance from a packet header and packet length.
     * <p><br>
     * CAVEAT: This method should be used only via the ITelemetryPacketInfoFactory, and
     * only for test or deserialization purposes.
     * 
     * @param inHeader the packet header containing the parsed packet header information
     * 
     * @param entirePacketLength the length of the entire packet in bytes, including header
     * 
     */
    protected TelemetryPacketInfo(final ISpacePacketHeader inHeader, final int entirePacketLength)
    {
        super();

        this.header = inHeader;
        fromSse = false;
        apid = header.getApid();
        seqCount = header.getSourceSequenceCount();
        size = entirePacketLength;
        groupingFlags = header.getGroupingFlags();
        isFill = header.isFill();
        secondaryHeaderFlag = header.getSecondaryHeaderFlag() != 0;
        type = header.getPacketType();
        version = header.getVersionNumber();
        primaryHeaderLength = header.getPrimaryHeaderLength();
    }

    /**
     * Creates an empty instance. Primary and Secondary header lengths
     * will be loaded from the default configuration.
     * 
     * CAVEAT: This method should be used only via the ITelemetryPacketInfoFactory,
     * and only for test purposes or for deserialization of an instance
     * of this class.
     * 
     */
    protected TelemetryPacketInfo() {
        super();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getErt()
     */
    @Override
    public IAccurateDateTime getErt() {
        return ert;
    }

    /**
     * Sets the earth receive time for the packet (ERT).
     * @param ert ERT time to set
     */
    @Override
    public void setErt(final IAccurateDateTime ert) {
        this.ert = ert;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#addSourceVcfc(long)
     */
    @Override
    public void addSourceVcfc(final long vcfc) {
        if (sourceVcfcs == null) {
            sourceVcfcs = new CopyOnWriteArrayList<>();
        }
        if (!sourceVcfcs.contains(Long.valueOf(vcfc))) {
            sourceVcfcs.add(vcfc);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getSourceVcfcs()
     */
    @Override
    public List<Long> getSourceVcfcs() {
        if (sourceVcfcs == null) {
            return null;
        }
        return Collections.unmodifiableList(sourceVcfcs);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#clearSourceVcfcs()
     */
    @Override
    public void clearSourceVcfcs() {
        this.sourceVcfcs = null;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#setSclk(jpl.gds.shared.time.ISclk)
     */
    @Override
    public void setSclk(final ISclk theSclk)
    {
        sclk = theSclk;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#setScet(jpl.gds.shared.time.IAccurateDateTime)
     */
    @Override
    public void setScet(final IAccurateDateTime theScet)
    {
        scet = theScet;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#setLst(jpl.gds.shared.time.ILocalSolarTime)
     */
    @Override
    public void setLst(final ILocalSolarTime theSol) {
        sol = theSol;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#setSecondaryHeaderFlag(boolean)
     */
    @Override
    public void setSecondaryHeaderFlag(final boolean flag)
    {
        secondaryHeaderFlag = flag;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getApid()
     */
    @Override
    public int getApid()
    {
        return apid;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getApidName()
     */
    @Override
    public String getApidName() {
    	return apidName == null ? UNKNOWN_APID_NAME : apidName;
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#setApidName(java.lang.String)
     */
    @Override
    public void setApidName(final String name) {
    	this.apidName = name == null ?  UNKNOWN_APID_NAME  : name;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getSeqCount()
     */
    @Override
    public int getSeqCount()
    {
        return seqCount;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getSclk()
     */
    @Override
    public ISclk getSclk()
    {
        return sclk;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getScet()
     */
    @Override
    public IAccurateDateTime getScet()
    {
        return scet;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getLst()
     */
    @Override
    public ILocalSolarTime getLst()
    {
        return sol;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getSize()
     */
    @Override
    public int getSize()
    {
        return size;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getSecondaryHeaderFlag()
     */
    @Override
    public boolean getSecondaryHeaderFlag()
    {
        return secondaryHeaderFlag;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return getIdentifierString();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getIdentifierString()
     */
    @Override
    public String getIdentifierString()
    {
        final StringBuilder sb = new StringBuilder(1024);

        sb.append("Packet APID=");
        sb.append(getApid());
        sb.append(", ERT=");
        sb.append(getErtString());
        sb.append(", SCLK=");
        sb.append(getSclkString());
        sb.append(", SCET=");
        sb.append(getScetString());
        if (USES_SOL) {
            sb.append(", LST=");
            sb.append(getLstString());
        }
        sb.append(", Sequence Counter=");
        sb.append(getSeqCount());
        sb.append(", VCID=");
        sb.append(getVcid());
        sb.append(", DSSID=");
        sb.append(getDssId());

        return(sb.toString());
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#isFromSse()
     */
    @Override
    public boolean isFromSse()
    {
        return (fromSse);
    }

    /**
     * Sets the flag indicating this is an SSE packet.
     *
     * @param fromSse true of the packet is an SSE packet, false for flight packets
     */
    @Override
    public void setFromSse(final boolean fromSse)
    {
        this.fromSse = fromSse;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getGroupingFlags()
     */
    @Override
    public byte getGroupingFlags()
    {
        return (groupingFlags);
    }

     /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getVcid()
     */
    @Override
    public void setGroupingFlags(final byte groupingFlags)
    {
        this.groupingFlags = groupingFlags;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#isFill()
     */
    @Override
    public boolean isFill()
    {
        return (isFill);
    }

    /**
     * Sets the flag indicating this is a fill packet.
     *
     * @param isIdle true to set this as a fill packet, false if not
     */
    @Override
    public void setFill(final boolean isIdle)
    {
        this.isFill = isIdle;
    }

    /**
     * Sets the APID from the primary packet header.
     *
     * @param apid The APID number to set.
     */
    @Override
    public void setApid(final int apid)
    {
        this.apid = apid;
    }

    /**
     * Sets the sequence counter from the primary header.
     *
     * @param seqCount the source sequence count to set
     */
    @Override
    public void setSeqCount(final int seqCount)
    {
        this.seqCount = seqCount;
    }

    /**
     * Sets the total size of the packet, including the header, in bytes.
     *
     * @param size The size to set.
     */
    @Override
    public void setSize(final int size)
    {
        this.size = size;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getVcid()
     */
    @Override
    public Integer getVcid() {
        if (vcid == null) {
            return 0;
        } else {
            return vcid;
        }
    }

    /**
     * Sets the source virtual channel ID (VCID) for this packet.
     * 
     * @param vcid The source VCID to set
     */
    @Override
    public void setVcid(final Integer vcid) {
        this.vcid = vcid;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getDssId()
     */
    @Override
    public Integer getDssId() {
        return dssId;
    }

    /**
     * Sets the source virtual channel ID (VCID) for this packet.
     * 
     * @param dssId The dssId to set
     */
    @Override
    public void setDssId(final Integer dssId)
    {
        if (dssId == null) {
            this.dssId = Integer.valueOf(StationIdHolder.UNSPECIFIED_VALUE);
        } else {
            this.dssId = dssId;
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getPrimaryHeaderLength()
     */
    @Override
    public int getPrimaryHeaderLength() {
        return primaryHeaderLength;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getSecondaryHeaderLength()
     */
    @Override
    public int getSecondaryHeaderLength() {
        return secondaryHeaderLength;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#setPrimaryHeaderLength(int)
     */
    @Override
    public void setPrimaryHeaderLength(final int len) {
        primaryHeaderLength = len;
        
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#setSecondaryHeaderLength(int)
     */
    @Override
    public void setSecondaryHeaderLength(final int len) {
        secondaryHeaderLength = len;
        
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#setBitRate(double)
     */
    @Override
    public void setBitRate(final double rate) {
        this.bitrate = rate;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getBitRate()
     */
    @Override
    public double getBitRate() {
        return bitrate;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#setScid(Integer)
     */
    @Override
    public void setScid(final Integer scid) {
        if (scid == null) {
            this.scid = Integer.valueOf(0);
        } else {    
            this.scid = scid;
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getScid()
     */
    @Override
    public Integer getScid() {
        return this.scid;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#setRelayScid(int)
     */
    @Override
    public void setRelayScid(final int scid) {
        this.relayScid = scid;
        
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getRelayScid()
     */
    @Override
    public int getRelayScid() {
        return this.relayScid;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#setFrameType(java.lang.String)
     */
    @Override
    public void setFrameType(final String type) {
        this.frameType = type;        
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getFrameType()
     */
    @Override
    public String getFrameType() {
        return frameType;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getPacketVersion()
     */
    @Override
    public byte getPacketVersion() {
        return version;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#setPacketVersion(byte)
     */
    @Override
    public void setPacketVersion(final byte version) {
        this.version = version;
        
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getPacketType()
     */
    @Override
    public byte getPacketType() {
        return type;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#setPacketType(byte)
     */
    @Override
    public void setPacketType(final byte type) {
        this.type = type;
        
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getFieldValue(jpl.gds.dictionary.impl.impl.api.channel.PacketHeaderFieldName)
     */
    @Override
    public Object getFieldValue(final PacketHeaderFieldName name) {
        switch (name) {
        case PACKET_VERSION_NUMBER: return getPacketVersion();
        case PACKET_TYPE: return getPacketType();
        case SECONDARY_HEADER_FLAG: return getSecondaryHeaderFlag();
        case APID: return getApid();
        case SEQUENCE_FLAGS: return getGroupingFlags();
        case PACKET_SEQUENCE_COUNT: return getSeqCount();
        case PACKET_DATA_LENGTH: return this.size - this.primaryHeaderLength - 1;
        case COARSE_SPACECRAFT_TIME: return this.sclk == null ? 0 : this.sclk.getCoarse();
        case FINE_SPACECRAFT_TIME: return this.sclk == null ? 0 : this.sclk.getFine();
        default:
            break;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#getBinarySize()
     */
    @Override
    public int getBinarySize() {
        return toBinary().length;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#toBinary()
     */
    @Override
    public byte[] toBinary() {  

        return build().toByteArray();    
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#toBinary(byte[], int)
     */
    @Override
    public int toBinary(final byte[] buff, final int startOff) {
    	final byte [] binaryVal = toBinary();
        final int neededLen = binaryVal.length;
        
        if (buff == null || startOff + neededLen > buff.length) {
            throw new IllegalArgumentException("buffer is null or too small to serialize packet info");
        }
        
        int off = startOff;
        
        System.arraycopy(binaryVal, 0, buff, startOff, binaryVal.length);

        off += neededLen;
        
        return off;
        
        
    }
    
    @Override
    public Proto3TelemetryPacketInfo  build(){
    	final Proto3TelemetryPacketInfo.Builder retVal = Proto3TelemetryPacketInfo.newBuilder();
    	
        retVal.setUsesSol(USES_SOL);
        retVal.setSecondaryHeaderFlag(this.secondaryHeaderFlag);
        retVal.setIsFill(this.isFill);
        retVal.setFromSse(this.fromSse);

        retVal.setVersion(this.version);
        retVal.setType(this.type);
        retVal.setApid(this.apid);
        retVal.setSeqCount(this.seqCount);
        retVal.setSize(this.size);
        retVal.setBitrate(this.bitrate);
        retVal.setPrimaryHeaderLength(this.primaryHeaderLength);
        retVal.setSecondaryHeaderLength(this.secondaryHeaderLength);
        retVal.setRelayScid(this.relayScid);
        retVal.setGroupingFlags(this.groupingFlags);
        
        if(this.frameType != null){
        	retVal.setFrameType(this.frameType);
        }
        if(this.apidName != null){
        	retVal.setApidName(this.apidName);
        }
        if(this.scid != null){
        	retVal.setScid(this.scid);
        }
        if(this.ert != null){
        	retVal.setErt(ert.buildAccurateDateTime());
        }
        if(this.sclk != null){
        	retVal.setSclk(sclk.buildSclk());
        }
        if(this.scet != null){
        	retVal.setScet(scet.buildAccurateDateTime());
        }
        if(this.vcid != null){
        	retVal.setVcid(this.vcid);
        }
        if(this.dssId != null){
        	retVal.setDssId(this.dssId);
        }
        if(this.sol != null){
        	retVal.setSol(sol.buildLocalSolarTime());
        }
        
        if(this.sourceVcfcs != null){
        	retVal.addAllSourceVcfcs(this.sourceVcfcs);
        }
        
        return retVal.build();
    }
    
    @Override
    public void load(final Proto3TelemetryPacketInfo msg){
        
        this.fromSse = msg.getFromSse();
        this.isFill = msg.getIsFill();
        this.secondaryHeaderFlag = msg.getSecondaryHeaderFlag();
        //no need to get has_sol from here, already set at instantiation
        
        this.version = (byte)msg.getVersion();
        this.type = (byte)msg.getType();
        this.apid = msg.getApid();
        this.seqCount = msg.getSeqCount();
        this.size = msg.getSize();
        this.bitrate = msg.getBitrate();
        this.primaryHeaderLength = msg.getPrimaryHeaderLength();
        this.secondaryHeaderLength = msg.getSecondaryHeaderLength();
        this.relayScid = msg.getRelayScid();
        this.groupingFlags = (byte)msg.getGroupingFlags();
        
        if(!msg.getFrameType().isEmpty()){
        	this.frameType = msg.getFrameType();
        }
        if(!msg.getApidName().isEmpty()){
        	this.apidName = msg.getApidName();
        } else {
        	this.apidName = UNKNOWN_APID_NAME;
        }
        if(msg.getHasScidCase().equals(HasScidCase.SCID)){
        	this.scid = msg.getScid();
        }
        if(msg.getHasErtCase().equals(HasErtCase.ERT)){
        	this.ert = new AccurateDateTime(msg.getErt().getMilliseconds(), msg.getErt().getNanoseconds());
        }
        if(msg.getHasSclkCase().equals(HasSclkCase.SCLK)){
        	this.sclk = new Sclk(msg.getSclk().getSeconds(), msg.getSclk().getNanos());
        }
        if(msg.getHasScetCase().equals(HasScetCase.SCET)){
        	this.scet = new AccurateDateTime(msg.getScet().getMilliseconds(), msg.getScet().getNanoseconds());
        }
        if(msg.getHasVcidCase().equals(HasVcidCase.VCID)){
        	this.vcid = msg.getVcid();
        }
        if(msg.getHasDssIdCase().equals(HasDssIdCase.DSSID)){
        	this.dssId = msg.getDssId();
        }
        if(msg.getHasSolCase().equals(HasSolCase.SOL)){
        	this.sol = new LocalSolarTime(msg.getSol().getMilliseconds(), msg.getSol().getSol());
        }
        
        if(msg.getSourceVcfcsCount() > 0){
        	this.sourceVcfcs = msg.getSourceVcfcsList();
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfo#parseFromBinary(byte[], int)
     */
    @Override
    public int parseFromBinary(final byte[] buff, final int startOff) {
    
        Proto3TelemetryPacketInfo msg;
        
        try {
        	msg = Proto3TelemetryPacketInfo.parseFrom(buff);
        } catch (final InvalidProtocolBufferException e) {
        	TraceManager.getDefaultTracer().warn("Error parsing ITelemetryPacketInfo from buffer: " + ExceptionTools.getMessage(e));
        	return startOff;
        }
        load(msg);
        
        return (startOff + msg.toByteArray().length);
    
    }

    private String getSclkString()
    {
        if (sclk == null) {
            return "";
        }
        return sclk.toString();
    }

    private String getScetString()
    {
        if (scet == null) {
            return "";
        }
        return(scet.getFormattedScet(true));
    }

    private String getLstString()
    {
        if (sol == null) {
            return "";
        }
        return(sol.getFormattedSol(true));
    }

    private String getErtString()
    {
        if (ert == null) {
            return "";
        }
        return(ert.getFormattedErt(true));
    }

    @Override
    public void setTemplateContext(final Map<String, Object> map) {
        // MPCS-9461 - 03/08/18 - added a couple of items to the template context that weren't set before.
        map.put("ert", getErtString());
        map.put("scet", getScetString());
        map.put("sclk", getSclkString());
        map.put("lst", getLstString());
        map.put("vcid", vcid == null? "" : vcid);
        map.put("dssId", dssId);
        map.put("apid", apid);
        map.put("apidName", getApidName());
        map.put("spsc", seqCount);
        if (sourceVcfcs != null)
        {
            final StringBuilder sources = new StringBuilder(128);

            for (int i = 0; i < sourceVcfcs.size(); ++i)
            {
                if (i != 0)
                {
                    sources.append(';');
                }

                sources.append(sourceVcfcs.get(i));
            }

            map.put("sourceVcfcs", sources.toString());
        }
        map.put("length", size);
        map.put("packetType", type);
        map.put("relayScid", relayScid);
        map.put("vcfc", seqCount);
        map.put("bitRate", bitrate);

    }

}
