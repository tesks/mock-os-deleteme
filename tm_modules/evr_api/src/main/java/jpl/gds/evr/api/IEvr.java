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
package jpl.gds.evr.api;

import java.util.List;

import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.types.Pair;

/**
 * The <code>IEvr</code> interface is to be implemented by all EVR classes,
 * which represent processed EVRs.
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 * An <code>IEvr</code> object is the multi-mission representation of an EVR.
 * EVR extraction adapter implementations must create <code>IEvr</code> objects
 * via the <code>EvrFactory</code>. An <code>IEvr</code> object must have an
 * associated <code>IEvrDefinition</code>, which will be automatically created
 * or fetched from the currently loaded EVR definitions and assigned to the
 * <code>IEvr</code> objects created by the factory. Other objects should
 * interact with <code>IEvr</code> objects only through the factory class and
 * the <code>IEvr</code> interface. Interaction with the actual
 * <code>IEvr</code> implementation classes in EVR processing adaptation is
 * contrary to multi-mission development standards.
 * 
 *
 * @see IEvrDefinition
 * @see IEvrFactory
 */
public interface IEvr extends Templatable
{
	/**
	 * Returns the SCLK.
	 * 
	 * @return SCLK of the EVR
	 */
    public ISclk getSclk();

	/**
	 * Returns the event ID.
	 * 
	 * @return event ID of the EVR
	 */
	public long getEventId();

	/**
	 * Sets the EVR definition for this EVR.
	 * 
	 * @param evrDef
	 *            the IEvrDefinition to set
	 * 
	 */
	public void setEvrDefinition(final IEvrDefinition evrDef);

	/**
	 * Returns the SCET.
	 * 
	 * @return SCET of the EVR
	 */
	public IAccurateDateTime getScet();

	/**
	 * Returns the record creation time.
	 * 
	 * @return RCT of the EVR
	 */
    public IAccurateDateTime getRct();

	/**
	 * Returns the ERT.
	 * 
	 * @return ERT of the EVR
	 */
	public IAccurateDateTime getErt();

	/**
	 * Sets all metadata from lists of strings.
	 * 
	 * @param keys
	 *            list of metadata keywords
	 * @param values
	 *            list of values corresponding to the keywords provided
	 */
	public void setMetadataKeyValuesFromStrings(
			final List<EvrMetadataKeywordEnum> keys, final List<String> values);

	/**
	 * Returns the EVR message.
	 * 
	 * @return message of the EVR
	 */
	public String getMessage();

	/**
	 * Returns the local solar time.
	 * 
	 * @return LST of the EVR
	 */
	public ILocalSolarTime getSol();

	/**
	 * Returns the EVR level.
	 * 
	 * @return level of the EVR
	 */
	public String getLevel();

	/**
	 * Sets the message of the EVR.
	 * 
	 * @param message
	 *            to set
	 */
	public void setMessage(final String message);

	/**
	 * Sets the SCLK of the EVR.
	 * 
	 * @param sclk
	 *            spacecraft clock value to set
	 */
    public void setSclk(final ISclk sclk);

	/**
	 * Sets the local solar time of the EVR.
	 * 
	 * @param sol
	 *            LST value to set
	 */
	public void setSol(ILocalSolarTime sol);

	/**
	 * Sets the SCET of the EVR.
	 * 
	 * @param scet
	 *            SCET value to set
	 */
	public void setScet(final IAccurateDateTime scet);

	/**
	 * Sets the record creation time of the EVR.
	 * 
	 * @param rct
	 *            RCT value to set
	 */
    public void setRct(final IAccurateDateTime rct);

	/**
	 * Sets the earth received time of the EVR.
	 * 
	 * @param ert
	 *            ERT value to set
	 */
	public void setErt(final IAccurateDateTime ert);

	/**
	 * Sets whether or not this message was generated in response to an SSE
	 * event or a FSW event.
	 * 
	 * @param fromSse
	 *            true should be passed if EVR is from SSE, false otherwise
	 */
	public void setFromSse(final boolean fromSse);

	/**
	 * Sets whether or not this message was marked bad.
	 * 
	 * @param status
	 *            true should be passed if EVR is bad, false otherwise
	 */
	public void setBadEvr(final boolean status);
	
	/**
	 * Tells whether or not this message was generated in response to an SSE
	 * event or a FSW event.
	 * 
	 * @return true if EVR is from SSE, false otherwise
	 */
	public boolean isFromSse();

	/**
	 * Tells whether or not this EVR is a real-time one or recorded.
	 * 
	 * @return true if EVR is real-time, false otherwise
	 */
	public boolean isRealtime();

	/**
	 * Tells whether or not this EVR has been marked bad.
	 * 
	 * @return true if EVR is bad, false otherwise
	 */
	public boolean isBadEvr();
	
	/**
	 * Returns the definition of this EVR.
	 * 
	 * @return the EVR definition
	 */
	public IEvrDefinition getEvrDefinition();

	/**
	 * Sets the real-time flag of this EVR.
	 * 
	 * @param realtime
	 *            true if EVR is real-time, false otherwise
	 */
	public void setRealtime(final boolean realtime);

	/**
	 * Gets the EVR metadata.
	 * 
	 * @return metadata of the EVR
	 */
	public EvrMetadata getMetadata();

	/**
	 * Returns the name of the EVR.
	 * 
	 * @return EVR name
	 */
	public String getName();
	

    /**
     * Retrieves the category value from the category name.
     * 
     * @param name  The category name.
     *
     * @return  The category value.
     */
    public String getCategory(String name);
    
	/**
	 * Returns the metadata as a key-value list.
	 * 
	 * @return metadata key-values
	 */
	public List<Pair<EvrMetadataKeywordEnum, String>> getMetadataKeyValueStrings();

	/**
	 * Returns the metadata value for the provided metadata key.
	 * 
	 * @param key
	 *            metadata keyword to look up the value for
	 * @return value of the metadata looked up, null if not found
	 */
	public String getMetadataValue(final EvrMetadataKeywordEnum key);


    /**
     * Gets the DSS (receiving station) ID for this channel value.
     * @return the DSS ID; 0 for no station
     */
    public abstract int getDssId();

    /**
     * Sets the DSS (receiving station) ID for this channel value.
     * @param dss the DSS ID; 0 for no station
     */
    public abstract void setDssId(final int dss);

    /**
     * Gets the VCID (virtual channel id) for this channel value.
     * @return the VCID; null for unknown
     */
    public abstract Integer getVcid();

    /**
     * Sets the VCID (virtual channel id) for this channel value.
     * @param vcid null for unknown
     */
    public abstract void setVcid(final Integer vcid);


    /**
     * Gets the packet id for this channel value.
     *
     * @return the packet id; null for unknown
     */
    public abstract PacketIdHolder getPacketId();


    /**
     * Sets the packet id for this channel value.
     *
     * @param packetId Packet id, null for unknown
     */
    public abstract void setPacketId(final PacketIdHolder packetId);
    
}
