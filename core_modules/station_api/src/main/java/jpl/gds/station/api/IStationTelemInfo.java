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
package jpl.gds.station.api;

import org.xml.sax.SAXException;

import jpl.gds.context.api.filtering.IStationFilterable;
import jpl.gds.serialization.station.Proto3StationTelemInfo;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.xml.stax.StaxSerializable;

import java.util.Map;

/**
 * An interface to be implemented by station telemetry information objects.
 * 
 *
 * @since R8
 *
 */
public interface IStationTelemInfo extends IStationFilterable, Templatable, StaxSerializable{

    /**
     * Get the deep space station source ID (DSS ID). 
     * @return the DSS ID
     */
    @Override
    public Integer getDssId();

    /**
     * Set the deep space station source ID (DSS ID). 
     * @param dssId the DSS ID
     */
    public void setDssId(Integer dssId);

    /**
     * Sets the relay spacecraft ID
     * @param relayScid the numeric ID of the data relay spacecraft
     */
    public void setRelayScid(int relayScid);

    /**
     * Gets the Earth Receive Time.
     * @return the ERT
     */
    public IAccurateDateTime getErt();

    /**
     * Sets the Earth Receive Time.
     * @param ert the ERT to set
     */
    public void setErt(IAccurateDateTime ert);

    /**
     * @return Returns the bitRate.
     */
    public double getBitRate();

    /**
     * Sets the bitRate
     *
     * @param bitRate The bitRate to set.
     */
    public void setBitRate(double bitRate);

    /**
     * @return Returns the numBits.
     */
    public int getNumBitsReceived();

    /**
     * Sets the numBits
     *
     * @param numBits The numBits to set.
     */
    public void setNumBitsReceived(int numBits);

    /**
     * 
     * @return A formatted string representation of the Earth Receive Time
     */
    public String getErtString();

    /**
     * 
     * @return the relay spacecraft ID
     */
    public int getRelayScid();

    /**
     * Parses and sets a field value from the XML element value with the given
     * name.
     * 
     * @param elementName the name of the XML element in the XML message
     * @param text the value of the element
     * @throws SAXException if there is an issue parsing the XML
     */
    public void parseFromElement(String elementName, String text)
            throws SAXException;

    /**
     * De-serializes the content of the IStationTelemInfo object from
     * a byte array and sets all object attributes.
     * 
     * @param buff
     *            byte array containing object content
     * @param startOff
     *            starting offset of the IStationTelemInfo data in the given byte
     *            array.
     * @return the next offset in the byte array after parsing          
     */
	public int parseFromBinary(byte[] buff, int startOff);
	
    /**
     * Serializes the content of the IStationTelemInfo object to a
     * byte array.
     * 
     * @return byte array representing object content
     */	
	public byte[] toBinary();

    /**
     * Serializes the content of the IStationTelemInfo object to the
     * given byte array, starting at the given offset, and return the ending
     * offset.
     * 
     * @param buff
     *            byte array to write content to
     * @param startOff
     *            starting offset in the array
     * @return new offset after the write
     */
	public int toBinary(byte[] buff, int startOff);

    /**
     * Returns the binary size of the current instance, i.e., the length of the
     * array that would be returned by the next toBinary(). call Note that if
     * any attribute in the object changes, the resulting binary length may
     * change, so do not cache this value.
     * 
     * @return byte length required to serialize this instance
     */
    public int getBinarySize();	
    
	/**
	 * Transforms the content of the IStationTelemInfo object to a Protobuf
	 * message
	 * 
	 * @return the Protobuf message representing this object
	 */
    public Proto3StationTelemInfo build();
    
	/**
	 * Populated this IStationTelemInfo with data from the supplied Protobuf
	 * message
	 * 
	 * @param msg
	 *            the Protobuf containing the station telemetry info
	 */
    public void load(Proto3StationTelemInfo msg);

    /**
     * Sets the SLE metadata in the current object.
     *
     * @param sleMetadata the SLE metadata object to set
     */
    public void setSleMetadata(Map<String, String> sleMetadata);
}
