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

import jpl.gds.context.api.filtering.IFilterableDataItem;
import jpl.gds.shared.metadata.IMetadataHeaderProvider;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.template.Templatable;



/**
 * An interface to be implemented objects that represent spacecraft filter information
 * in the context configuration.
 * 
 *
 * @since R8
 */
public interface IContextFilterInformation extends IMetadataHeaderProvider, Templatable {

	/**
	 * Sets the current downlink virtual channel ID. Used for
	 * filtering and telemetry labeling.
	 * 
	 * @param vcid VC to set; may be null
	 */
	public void setVcid(Integer vcid);

	/**
	 * Gets the current downlink virtual channel ID. Used for
	 * filtering and telemetry labeling.
	 * 
	 * @return vcid; may be null
	 */
	public Integer getVcid();

	/**
	 * Sets the current downlink station ID. Used for
	 * filtering and telemetry labeling.
	 * 
	 * @param dssId station ID to set; may be null
	 */
	public void setDssId(Integer dssId);

	/**
	 * Gets the current downlink station ID. Used for
	 * filtering and telemetry labeling.
	 * 
	 * @return dssId; may be null
	 */
	public Integer getDssId();

	/**
	 * Copies members from the supplied ISpacecraftFilterInformation
	 * object to this one.
	 * 
	 * @param toCopy object to copy data from
	 */
	public void copyValuesFrom(IContextFilterInformation toCopy);
	
	/**
     * Indicates whether the object metadata has changed since the last time the
     * metadata header object was fetched.
     * 
     * @return true if the object has been modified, false if not
     */
    public boolean isDirty();
    
    /**
     * Generates the XML for this object.
     * 
     * @param writer XML stream to write to
     * @throws XMLStreamException if there is a problem generating the XML
     */
    public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException;

    /**
     * Determines if the supplied data item should be accepted given the current
     * configured context filters.
     * 
     * @param toFilter
     *            the data item to check
     * @param sseFlag
     *            the current SSE context flag
     * @return true if the given data item passes the filter, false if not
     */
    public boolean accept(IFilterableDataItem toFilter, SseContextFlag sseFlag);

}
