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
package jpl.gds.station.impl;

import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.SAXException;

import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.serialization.primitives.time.Proto3Adt;
import jpl.gds.serialization.station.Proto3StationTelemInfo;
import jpl.gds.serialization.station.Proto3StationTelemInfo.HasDssIdCase;
import jpl.gds.serialization.station.Proto3StationTelemInfo.HasErtCase;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.xml.stax.StaxSerializable;
import jpl.gds.shared.xml.stax.StaxStreamWriterFactory;
import jpl.gds.station.api.IStationTelemInfo;
/**
 * 
 * DSNInfo is used to carry critical information about a chunk of telemetry received from
 * the DSN or a similar data source.
 * 
 *
 * Refactored binary functions to utilize Proto3StationTelemInfo messages
 * instead of home-grown arbitrary packed binary messages. Added build and
 * load functions for creating and parsing Proto3StationTelemInfo messages.
 */
public class StationTelemInfo implements Templatable, StaxSerializable, IStationTelemInfo{
	private static final Tracer log = TraceManager.getDefaultTracer();

    private IAccurateDateTime ert;
    private double bitRate;
    private int numBits; 
    private int relayScid;
    private Integer dssId = 0;
    private Map<String, String> sleMetadata = new LinkedHashMap<>();

    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemInfo#getDssId()
     */
    @Override
    public Integer getDssId() {
    	return dssId;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemInfo#setDssId(Integer)
     */
    @Override
    public void setDssId(final Integer dssId) {
        if (dssId == null) {
            this.dssId = Integer.valueOf(StationIdHolder.UNSPECIFIED_VALUE);
        } else {
            this.dssId = dssId;
        }
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemInfo#setRelayScid(int)
     */
    @Override
    public void setRelayScid(final int relayScid) {
        this.relayScid = relayScid;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemInfo#getErt()
     */
    @Override
    public IAccurateDateTime getErt() {
        return this.ert;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemInfo#setErt(jpl.gds.shared.time.IAccurateDateTime)
     */
    @Override
    public void setErt(final IAccurateDateTime theErt) {
        this.ert = theErt;
    }
    
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemInfo#getBitRate()
     */
    @Override
    public double getBitRate() {
        return this.bitRate;
    }

    
    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemInfo#setBitRate(double)
     */
    @Override
    public void setBitRate(final double bitRate) {
        this.bitRate = bitRate;
    }

    
    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemInfo#getNumBitsReceived()
     */
    @Override
    public int getNumBitsReceived() {
        return this.numBits;
    }

    
    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemInfo#setNumBitsReceived(int)
     */
    @Override
    public void setNumBitsReceived(final int numBits) {
        this.numBits = numBits;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemInfo#getErtString()
     */
    @Override
    public String getErtString() {
        if (this.ert == null) {
            return null;
        }
        return this.ert.getFormattedErt(true);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemInfo#getRelayScid()
     */
    @Override
    public int getRelayScid() {
        return relayScid;
    }

    @Override
    public String toString() {
        // MPCS-12387: Display SLE metadata, if present
        return "ert=" + getErtString() + " bitRate=" + bitRate + " numBits=" + numBits + " dssId=" + dssId +
                (sleMetadata.isEmpty() ? "" : " sleMetadata: " + sleMetadata);
    }


    @Override
    public void setTemplateContext(final Map<String,Object> map) {

        map.put("ert", getErtString());
        map.put("bitRate", bitRate);
        map.put("numBits", numBits);
        map.put("relayScid", relayScid);
        map.put("dssId", dssId);
        map.put("relayScftId", relayScid); // - deprecated for R8
        map.put("relaySpacecraftId", relayScid); // - deprecated for R8

    }
    
    @Override
    public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException
    {
    	writer.writeStartElement("StationInfo");
		
		writer.writeStartElement("ert"); // <ert>
		writer.writeCharacters(getErtString());
		writer.writeEndElement(); // </ert>
		
		writer.writeStartElement("bitRate"); // <bitRate>
		writer.writeCharacters(Double.toString(this.bitRate));
		writer.writeEndElement(); // </bitRate>
		
		writer.writeStartElement("numBitsReceived"); // <numBits>
		writer.writeCharacters(Long.toString(this.numBits));
		writer.writeEndElement(); // </numBits>
		
		writer.writeStartElement("relayScid"); // <relayScid>
		writer.writeCharacters(Long.toString(this.relayScid));
		writer.writeEndElement(); // </relayScid>
		
		writer.writeStartElement("dssId"); // <dssId>
		writer.writeCharacters(this.dssId >= 0 ? Long.toString(this.dssId) : "0");
		writer.writeEndElement(); // </dssId>
		
		writer.writeEndElement(); // </StationInfo>
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.IStationTelemInfo#parseFromElement(java.lang.String, java.lang.String)
     */
    @Override
    public void parseFromElement(final String elementName, final String text)
    throws SAXException {
        if (elementName.equalsIgnoreCase("ert")) {
            try {
                this.ert = new AccurateDateTime(text);
            } catch (final ParseException e) {
                throw new SAXException(e.getMessage());
            }
        } else if (elementName.equalsIgnoreCase("bitRate")) {
            this.bitRate = Double.parseDouble(text);
        } else if (elementName.equalsIgnoreCase("numBitsReceived")) {
            this.numBits = Integer.parseInt(text);
        } else if (elementName.equalsIgnoreCase("relayScid")) {
            this.relayScid = Integer.parseInt(text);
        } else if (elementName.equalsIgnoreCase("dssId")) {
            this.dssId = Integer.parseInt(text);
        }
    }
    
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
			log.error("Could not transform DSNInfo object to XML: " + e.getMessage(), e);
		}
		
		return(output);
	}
    
    @Override
    public int toBinary(final byte[] buff, final int startOff) {

    	final byte[] binaryVal = toBinary();
        final int neededLen = binaryVal.length;

        if (buff == null || startOff + neededLen > buff.length) {
            throw new IllegalArgumentException("buffer is null or too small to serialize station info");
        }

        int off = startOff;
        
        System.arraycopy(binaryVal, 0, buff, startOff, binaryVal.length);

        off += neededLen;
        
        return off;
        
    }
    
    @Override
    public int parseFromBinary(final byte[] buff, final int startOff) {

    	Proto3StationTelemInfo msg;
    	try {
			msg = Proto3StationTelemInfo.parseFrom(buff);
		} catch (final InvalidProtocolBufferException e) {
			log.warn("Error parsing IStationTelemInfo from buffer: " + ExceptionTools.getMessage(e));
			return startOff;
		}
    	load(msg);
    	
    	return (startOff + msg.toByteArray().length);
    }


    @Override
    public byte[] toBinary() {
    	return build().toByteArray();
    }
    

    @Override
    public int getBinarySize() {
        return toBinary().length;
    }
    
    @Override
    public Proto3StationTelemInfo build() {
    	final Proto3StationTelemInfo.Builder retVal = Proto3StationTelemInfo.newBuilder();
    	
    	if(getErt() != null){
    		retVal.setErt(Proto3Adt.newBuilder()
    				.setMilliseconds(this.getErt().getTime())
    				.setNanoseconds(this.getErt().getNanoseconds()));
    	}
    	retVal.setBitRate(this.getBitRate());
    	retVal.setNumBits(this.getNumBitsReceived());
    	retVal.setRelayScid(this.getRelayScid());
    	if(getDssId() != null){
    		retVal.setDssId(this.getDssId());
    	}

        retVal.putAllSleMetadata(sleMetadata);
    	
    	return retVal.build();
    }
    
    @Override
    public void load(final Proto3StationTelemInfo msg){
    	if(!msg.getHasErtCase().equals(HasErtCase.HASERT_NOT_SET)){
    		setErt(new AccurateDateTime(msg.getErt().getMilliseconds(), msg.getErt().getNanoseconds()));
    	}
    	setBitRate(msg.getBitRate());
    	setNumBitsReceived(msg.getNumBits());
    	setRelayScid(msg.getRelayScid());
    	if(!msg.getHasDssIdCase().equals(HasDssIdCase.HASDSSID_NOT_SET)){
    		setDssId(msg.getDssId());
    	}

        this.sleMetadata = new LinkedHashMap<>();
        final Map<String, String> kv = msg.getSleMetadataMap();
        this.sleMetadata.putAll(kv);
    }

    @Override
    public void setSleMetadata(Map<String, String> map) {
        sleMetadata = new LinkedHashMap<>(map);
    }
}
