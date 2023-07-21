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
package jpl.gds.context.api;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.shared.metadata.IMetadataHeaderProvider;
import jpl.gds.shared.template.Templatable;

/**
 * An interface to be implemented by classes that carry venue information for
 * use in a context configuration.
 * 
 *
 * @since R8
 */
public interface IVenueConfiguration extends IMetadataHeaderProvider, Templatable {
	
	/**
	 * Sets the venue type.
	 * 
	 * @param type venue type to set; may not be null
	 */
	public void setVenueType(VenueType type);
	
	/**
	 * Gets the venue type.
	 * 
	 * @return venue type; never null
	 */
	public VenueType getVenueType();
	
	/**
	 * Sets the testbed name.
	 * 
	 * @param tbName testbed name to set
	 */
	public void setTestbedName(String tbName);
	
	/**
	 * Gets the testbed name; only valid when the current venue supports testbeds.
	 * 
	 * @return testbed name; may be null
	 */
	public String getTestbedName();
	
	/**
	 * Gets the downlink stream type; only valid when the current venue supports
	 * testbeds and when processing flight, as opposed to SSE, telemetry.
	 * 
	 * @return the downlink stream type; may be null
	 */
	public DownlinkStreamType getDownlinkStreamId();
	
	/**
	 * Sets the downlink stream type; only valid when the current venue supports
	 * testbeds and when processing flight, as opposed to SSE, telemetry.
	 * 
	 * @param type the downlink stream type to set
	 */
	public void setDownlinkStreamId(DownlinkStreamType type);
	
	/** 
	 * Copies members from the supplied IVenueConfiguration object to this one.
	 * 
	 * @param config object to copy data from
	 */
	public void copyValuesFrom(IVenueConfiguration config); 
	
	/**
     * Indicates whether the object metadata has changed since the last time the
     * metadata header object was fetched.
     * 
     * @return true if the object has been modified, false if not
     */
    public boolean isDirty();
    
    /**
     * Generates the XML for this connection.
     * 
     * @param writer XML stream to write to
     * @throws XMLStreamException if there is a problem generating the XML
     */
    public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException;

}
