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


import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.SAXException;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader;
import jpl.gds.dictionary.api.frame.EncodingType;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.dictionary.api.frame.TransferFrameDefinitionFactory;
import jpl.gds.serialization.frame.Proto3InvalidFrameCode;
import jpl.gds.serialization.frame.Proto3TelemetryFrameInfo;
import jpl.gds.serialization.frame.Proto3TelemetryFrameInfo.HasScidCase;
import jpl.gds.serialization.frame.Proto3TelemetryFrameInfo.HasVcidCase;
import jpl.gds.serialization.frame.Proto3TelemetryFrameInfo.Proto3TelemetryFrameHeader;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.xml.stax.StaxSerializable;
import jpl.gds.shared.xml.stax.StaxStreamWriterFactory;
import jpl.gds.station.api.InvalidFrameCode;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;

/**
 * 
 * TransferFrameInfo contains metadata about a single instance of an 
 * AOS transfer frame.
 *
 *
 * MPCS-4599 - 10/30/17 - Refactored binary functions to utilize
 * 			Proto3TelemetryFrameInfo messages instead of home-grown arbitrary
 * 			packed binary messages. Added build and load functions for creating
 * 			and parsing Proto3TelemetryFrameInfo messages.
 *
 */
class TelemetryFrameInfo implements Templatable, StaxSerializable, ITelemetryFrameInfo 
{
	private static final Tracer log = TraceManager.getDefaultTracer();

	private String name; // transfer frame type
	private Integer vcid;
	private int seqCount = 0;
	private int version = 1;
	private int size = 0; // does not include sync mark and encoding
	private int packetStoreSize = 0; // packet storage size
	private Integer spacecraftId = Integer.valueOf(0);
	private int firstPacketPointer = 0;
	private int hdrSize = 0; // includes tf header
	private boolean deadcode = false;
	private boolean isFrameIdle = false;
	private boolean isFrameBad = false;
	private InvalidFrameCode badReason;
	private ITelemetryFrameHeader header;
	private ITransferFrameDefinition format;

	private int maxSeqCount;

	/**
	 * Creates an empty instance of TransferFrameInfo.
	 */
	protected TelemetryFrameInfo() {
	  // do nothing
	}

	/**
	 * Creates an instance of TransferFrameInfo with the given default values.
	 * @param scid the numeric spacecraft ID
	 * @param vc the virtual channel ID
	 * @param seq the virtual channel frame counter
	 */
	protected TelemetryFrameInfo(final int scid, final int vc, final int seq) {
		this.vcid = vc;
		this.seqCount = seq;
		this.spacecraftId = scid;
	}

	/**
	 * Creates an instance of TransferFrameInfo with the given IFrameHeader
	 * object and ITransferFrameDefinition objects. If either of these objects 
	 * is non-null, then the other member fields in this instance will be 
	 * initialized from the fields in those objects, where possible. It is
	 * important to note that the header size and packet store size will
	 * be initialized using the "Actual" ASM length in the ITransferFrameDefinition
	 * object, as opposed to the vanilla ASM length. If the vanilla ASM
	 * length is desired, then the caller must override the packet store
	 * size and header size fields after this constructor completes.
	 * 
	 * @param header the IFrameHeader object (populated) associated with this frame
	 *               info instance; may not be null
	 * @param format the ITransferFrameDefinition object (populated) associated with 
	 *               this frame info instance; may not be null
	 */
	protected TelemetryFrameInfo(final ITelemetryFrameHeader header, final ITransferFrameDefinition format) {
	    /* MPCS-7039 - 7/9/15. Both arguments now required */
	    if (header == null) {
	        throw new IllegalArgumentException("frame header cannot be null");
	    }
	    if (format == null) {
	        throw new IllegalArgumentException("frame format cannot be null");
	    }
	    this.header = header;
	    this.format = format;
	    setFieldsFromHeader();
	    setFieldsFromFormat();
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void setFrameFormat(final ITransferFrameDefinition format) {
		this.format = format;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#getFrameFormat()
	 */
	@Override
	public ITransferFrameDefinition getFrameFormat() {
		return format;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#getBadReason()
	 */
	@Override
	public InvalidFrameCode getBadReason() {
		return this.badReason;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#getHeader()
	 */
	@Override
	public ITelemetryFrameHeader getHeader() {
		return this.header;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#setHeader(jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader)
	 */
	@Override
	public void setHeader(final ITelemetryFrameHeader header) {
		this.header = header;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#setBadReason(jpl.gds.station.api.InvalidFrameCode)
	 */
	@Override
	public void setBadReason(final InvalidFrameCode badReason) {
		this.badReason = badReason;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#setType(java.lang.String)
	 */
	@Override
	public void setType(final String typeName) {
		this.name = typeName;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#getType()
	 */
	@Override
	public String getType() {
		return this.name;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#setCADUSize(int)
	 */
	@Override
	public void setCADUSize(final int sz) {
		this.size = sz;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#setHdrSize(int)
	 */
	@Override
	public void setHdrSize(final int sz) {
		this.hdrSize = sz;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#setDataAreaSize(int)
	 */
	@Override
	public void setDataAreaSize(final int pss) {
		this.packetStoreSize = pss;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#setDataPointer(int)
	 */
	@Override
	public void setDataPointer(final int fpp) {
		this.firstPacketPointer = fpp;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#setVersion(int)
	 */
	@Override
	public void setVersion(final int version) {
		/* 
		 * MPCS-3923 - 11/3/14. Just set the version number from the argument.
		 * No more AOS V1 vs V2 hanky-panky.
		 */
		this.version = version;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#getVersion()
	 */
	@Override
	public int getVersion() {
		return this.version;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#setIdle(boolean)
	 */
	@Override
	public void setIdle(final boolean idle) {
		this.isFrameIdle = idle;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#setDeadCode(boolean)
	 */
	@Override
	public void setDeadCode(final boolean dc) {
		this.deadcode = dc;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#isDeadCode()
	 */
	@Override
	public boolean isDeadCode() {
		return this.deadcode;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#getVcid()
	 */
	@Override
	public Integer getVcid() {
        if (vcid == null) {
            return 0;
        }
        else {
            return vcid;
        }
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#setVcid(Integer)
	 */
	@Override
	public void setVcid(final Integer vcid) {
	    this.vcid = vcid;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#getSeqCount()
	 */
	@Override
	public int getSeqCount() {
		return this.seqCount;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#setSeqCount(int)
	 */
	@Override
	public void setSeqCount(final int seq) {
		this.seqCount = seq;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#getMaxDataPointer()
	 */
	@Override
	public int getMaxDataPointer() {
		return this.packetStoreSize;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#getMaxSeqCount()
	 */
	@Override
	public int getMaxSeqCount() {
		/* 
		 * MPCS-3923 - 11/3/14. Just return the member variable.
		 * No more AOS V1 vs V2 hanky-panky.
		 */
		return this.maxSeqCount;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#setMaxSeqCount(int)
	 */
	@Override
	public void setMaxSeqCount(final int max) {
		this.maxSeqCount = max;
	}


	/**
	 * @{inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#getCADUSize()
	 */
	@Override
	public int getCADUSize() {
		return this.size;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#getHdrSize()
	 */
	@Override
	public int getHdrSize() {
		return this.hdrSize;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#getDataAreaSize()
	 */
	@Override
	public int getDataAreaSize() {
		return this.packetStoreSize;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#getScid()
	 */
	@Override
	public Integer getScid() {
		return this.spacecraftId;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#setScid(Integer)
	 */
	@Override
	public void setScid(final Integer scid) {
	    if (scid == null) {
	        this.spacecraftId = Integer.valueOf(0);
	    } else {
	        this.spacecraftId = scid;
	    }
	}


	/**
	 * @{inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#getDataPointer()
	 */
	@Override
	public int getDataPointer() {
		return this.firstPacketPointer;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#isIdle()
	 */
	@Override
	public boolean isIdle() {
		return this.isFrameIdle;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#setBad(boolean)
	 */
	@Override
	public void setBad(final boolean bad) {
		this.isFrameBad = bad;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#isBad()
	 */
	@Override
	public boolean isBad() {
		return this.isFrameBad;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TFInfo scid=" + this.spacecraftId + " name=" + this.name + " vcid="
				+ this.vcid + " seqCount=" + this.seqCount + " version="
				+ this.version + " size=" + this.size + " headerSize=" + this.hdrSize
				+ " fpp=" + this.firstPacketPointer + " Idle=" + this.isFrameIdle  
				+ " Bad=" + this.isFrameBad + " BadReason=" + this.badReason;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.template.Templatable#setTemplateContext(java.util.Map)
	 */
	@Override
	public void setTemplateContext(final Map<String,Object> map) {
		map.put("scid", this.spacecraftId);
        map.put("name", (this.name == null ? "" : this.name)); // deprecated for R8
        map.put("frameType", (this.name == null ? "" : this.name));

		//null value is checked in velocity template
		map.put("vcid", vcid);

        map.put("seqCount", this.seqCount); // deprecated for R8
        map.put("vcfc", seqCount);
		map.put("size", this.size);
		map.put("version", this.version);
		map.put("fpp", this.firstPacketPointer);
		map.put("idle", this.isFrameIdle);
		map.put("bad", this.isFrameBad);
        map.put("isBad", isFrameBad); // deprecated for R8
		map.put("badReason", this.badReason);
		if (this.format != null) {
			map.put("encodingType", this.format.getEncoding().toString());
		} else {
			map.put("encodingType", EncodingType.UNENCODED);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
	 */
	@Override
	public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException
	{
		writer.writeStartElement("TransferFrameInfo"); // <TransferFrameInfo>
		writer.writeAttribute("scid",Long.toString(this.spacecraftId));
		writer.writeAttribute("name",this.name != null ? this.name : "");

		writer.writeStartElement("virtualChannel"); // <virtualChannel>
		writer.writeCharacters(Long.toString(this.vcid));
		writer.writeEndElement(); // </virtualChannel>

		writer.writeStartElement("seqCount"); // <seqCount>
		writer.writeCharacters(Long.toString(this.seqCount));
		writer.writeEndElement(); // </seqCount>

		writer.writeStartElement("version"); // <version>
		writer.writeCharacters(Long.toString(this.version));
		writer.writeEndElement(); // </version>

		writer.writeStartElement("size"); // <size>
		writer.writeCharacters(Long.toString(this.size));
		writer.writeEndElement(); // </size>

		writer.writeStartElement("firstPacketPointer"); // <firstPacketPointer>
		writer.writeCharacters(Long.toString(this.firstPacketPointer));
		writer.writeEndElement(); // </firstPacketPointer>

		writer.writeStartElement("isIdle"); // <isIdle>
		writer.writeCharacters(Boolean.toString(this.isFrameIdle));
		writer.writeEndElement(); // </isIdle>

		writer.writeStartElement("isBad"); // <isBad>
		writer.writeCharacters(Boolean.toString(this.isFrameBad));
		writer.writeEndElement(); // </isBad>

		if(this.badReason != null)
		{
			writer.writeStartElement("badReason"); // <badReason>
			writer.writeCharacters(this.badReason.toString());
			writer.writeEndElement(); // </badReason>
		}

		writer.writeStartElement("encodingType"); // <encodingType>
		writer.writeCharacters(this.format != null ? this.format.getEncoding().toString() : EncodingType.UNENCODED.toString());
		writer.writeEndElement(); // </encodingType>

		writer.writeEndElement(); // </TransferFrameInfo>
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#parseFromElement(java.lang.String, java.lang.String)
	 */
	@Override
	public void parseFromElement(final String elementName, final String text)
			throws SAXException {
		if (elementName.equalsIgnoreCase("virtualChannel")) {
			this.vcid = Integer.parseInt(text);
		} else if (elementName.equalsIgnoreCase("seqCount")) {
			this.seqCount = Integer.parseInt(text);
		} else if (elementName.equalsIgnoreCase("version")) {
			this.version = Integer.parseInt(text);
		} else if (elementName.equalsIgnoreCase("size")) {
			this.size = Integer.parseInt(text);
		} else if (elementName.equalsIgnoreCase("firstPacketPointer")) {
			this.firstPacketPointer = Integer.parseInt(text);
		} else if (elementName.equalsIgnoreCase("isIdle")) {
			this.isFrameIdle = Boolean.valueOf(text);
		} else if (elementName.equalsIgnoreCase("isBad")) {
			this.isFrameBad = Boolean.valueOf(text);
		} else if (elementName.equalsIgnoreCase("badReason")) {
			this.badReason = InvalidFrameCode.valueOf(text);
		} else if (elementName.equalsIgnoreCase("encodingType")) {
			this.format = TransferFrameDefinitionFactory.createTransferFrame();
			this.format.setEncoding(Enum.valueOf(EncodingType.class, text));
		}
	}


	/**
	 * @{inheritDoc}
	 * @see jpl.gds.shared.xml.stax.StaxSerializable#toXml()
	 */
	@Override
	public String toXml()
	{
		String output = "";
		try
		{
			output = StaxStreamWriterFactory.toXml(this);
		}
		catch(final XMLStreamException e)
		{
			log.error("Could not transform TransferFrameInfo object to XML: " + e.getMessage(), e);
		}

		return(output);
	}

	@SuppressWarnings("deprecation")
	private void setFieldsFromHeader() {

	    /* MPCS-7039 - 7/9/15. Header can no longer be null at this point */
	    this.setDataPointer(this.header.getDataPointer());
	    this.setIdle(this.header.isIdle());
	    this.setMaxSeqCount(this.header.getMaxSeqCount());	
	    this.setSeqCount(this.header.getVirtualChannelFrameCount());
	    this.setScid(this.header.getScid());
	    this.setVersion(this.header.getVersion());
	    this.setVcid(this.header.getVirtualChannelId());
	}

	private void setFieldsFromFormat() {

	    /* MPCS-7039 - 7/9/15. Header and format can no longer be null at this point.
	     */
	    this.setDataAreaSize(this.format.getDataAreaSizeBytes());
	    this.setType(this.format.getName());
	    this.setCADUSize(this.format.getCADUSizeBytes());
	    this.setHdrSize(this.format.getTotalHeaderSizeBytes() + (this.format.arrivesWithASM() ? this.format.getASMSizeBytes() : 0));
	}
	
    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#getBinarySize()
     */
    @Override
    public int getBinarySize() {
        return toBinary().length;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#toBinary()
     */
    @Override
    public byte[] toBinary() {
    	return build().toByteArray();
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#toBinary(byte[], int)
     */
    @Override
    public int toBinary(final byte[] buff, final int startOff) {
    	
    	final byte[] binaryVal = toBinary();
        final int neededLen = binaryVal.length;

        if (buff == null || startOff + neededLen > buff.length) {
            throw new IllegalArgumentException("buffer is null or too small to serialize frame info");
        }

        int off = startOff;
        
        System.arraycopy(binaryVal, 0, buff, startOff, binaryVal.length);

        off += neededLen;
        
        return off;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tm.service.api.frame.ITelemetryFrameInfo#parseFromBinary(byte[], int)
     */
    @Override
    public int parseFromBinary(final byte[] buff, final int startOff) {

        Proto3TelemetryFrameInfo msg;
        try {
            msg = Proto3TelemetryFrameInfo.parseFrom(buff);
        }
        catch (final InvalidProtocolBufferException e) {
            log.warn("Error parsing ITelemetryFrameInfo from buffer: " + ExceptionTools.getMessage(e));
            return startOff;
        }
        load(msg);

        return (startOff + msg.toByteArray().length);
    }

    @Override
    public Proto3TelemetryFrameInfo build() {
        final Proto3TelemetryFrameInfo.Builder retVal = Proto3TelemetryFrameInfo.newBuilder();

        if (getScid() != null) {
            retVal.setSpacecraftId(getScid());
        }
        if (getType() != null && !getType().isEmpty()) {
            retVal.setName(getType());
        }
        if (getVcid() != null) {
            retVal.setVcid(getVcid());
        }
        if (getFrameFormat() != null) {
            retVal.setFormat(getFrameFormat().build());
        }
        retVal.setSeqCount(getSeqCount());
        retVal.setVersion(getVersion());
        retVal.setSize(getCADUSize());
        retVal.setFirstPacketPointer(getDataPointer());
        retVal.setIsFrameIdle(isIdle());
        retVal.setIsDeadCode(isDeadCode());
        retVal.setIsFrameBad(isBad());
        if (getBadReason() != null) {
            retVal.setBadReason(Proto3InvalidFrameCode.valueOf(getBadReason().toString()));
        }

		if (getHeader() != null) {
        	Proto3TelemetryFrameHeader.Builder tfhBuild = Proto3TelemetryFrameHeader.newBuilder();
			tfhBuild.setHeaderBytes(ByteString.copyFrom(getHeader().getAllHeaderBytes()));
			retVal.setHeader(tfhBuild.build());
		}

        return retVal.build();
    }

    @Override
    public void load(final Proto3TelemetryFrameInfo msg) {
        if (!msg.getHasScidCase().equals(HasScidCase.HASSCID_NOT_SET)) {
            this.spacecraftId = msg.getSpacecraftId();
        }
        if (!msg.getName().isEmpty()) {
            this.name = msg.getName();
        }
        if (!msg.getHasVcidCase().equals(HasVcidCase.HASVCID_NOT_SET)) {
            this.vcid = msg.getVcid();
        }
        if (msg.hasFormat()) {
            this.format = TransferFrameDefinitionFactory.createTransferFrame();
            this.format.load(msg.getFormat());
        }
        this.seqCount = msg.getSeqCount();
        this.version = msg.getVersion();
        this.size = msg.getSize();
        this.firstPacketPointer = msg.getFirstPacketPointer();
        this.isFrameIdle = msg.getIsFrameIdle();
        this.isFrameBad = msg.getIsFrameBad();
        if (msg.getBadReason().ordinal() > 0) {
            this.badReason = InvalidFrameCode.valueOf(msg.getBadReason().toString());
        }
        this.deadcode = msg.getIsDeadCode();
        
        // header must be set from class that has access to Mission Properties or Application Context.
    }
}
