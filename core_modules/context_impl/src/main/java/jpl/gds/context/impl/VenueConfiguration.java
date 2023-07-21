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
package jpl.gds.context.impl;

import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.shared.metadata.ISerializableMetadata;
import jpl.gds.shared.metadata.MetadataKey;
import jpl.gds.shared.metadata.MetadataMap;
import jpl.gds.shared.xml.XmlUtility;

/**
 * An class that holds venue configuration information in a context configuration.
 * 
 *
 * @since R8
 */
public class VenueConfiguration implements IVenueConfiguration {
	
    private VenueType venueType = VenueType.TESTSET;
    private String testbedName;
    private DownlinkStreamType streamType = DownlinkStreamType.NOT_APPLICABLE;
    private boolean dirty = true;
    private  ISerializableMetadata header;
    
    
    /**
     * Constructor.
     * 
     * @param props the current MissionProperties object, from which default venue will
     *        be fetched
     */
    public VenueConfiguration(final MissionProperties props) {
    	this.venueType = props.getDefaultVenueType();
    }
    
	@Override
	public synchronized void setVenueType(final VenueType type) {
		this.venueType = type;
		this.dirty = true;
		
	}
	@Override
	public VenueType getVenueType() {
		return this.venueType;
	}
	
	@Override
	public synchronized void setTestbedName(final String tbName) {
		this.testbedName = tbName;
		this.dirty = true;
		
	}
	
	@Override
	public String getTestbedName() {
		return this.testbedName;
	}
	
	@Override
	public DownlinkStreamType getDownlinkStreamId() {
		return this.streamType;
	}
	
	@Override
	public synchronized void setDownlinkStreamId(final DownlinkStreamType type) {
		this.streamType = (type != null)
				? type : DownlinkStreamType.NOT_APPLICABLE;
		this.dirty = true;
	}
	
	@Override
	public synchronized void copyValuesFrom(final IVenueConfiguration config) {
		this.streamType = config.getDownlinkStreamId();
		this.venueType = config.getVenueType();
		this.testbedName = config.getTestbedName();	
		this.dirty = true;
	}
	
	@Override
	public synchronized ISerializableMetadata getMetadataHeader() {
	    if (header == null || isDirty()) {
	        header = new MetadataMap();


	        header.setValue(MetadataKey.VENUE_TYPE, getVenueType());
	        if (getDownlinkStreamId() != null) {
	            header.setValue(MetadataKey.TELEMETRY_STREAM_TYPE, getDownlinkStreamId());
	        }
	        if (getTestbedName() != null) {
	            header.setValue(MetadataKey.TESTBED_NAME, getTestbedName());
	        }
	        this.dirty = false;
	    }
	    return header;
	}
	
    @Override
    public boolean isDirty() {
        return this.dirty ;
    }

    @Override
    public synchronized void setTemplateContext(final Map<String, Object> map) {
        if (getDownlinkStreamId() != null) {
            map.put("downlinkStreamId",
                DownlinkStreamType.convert(getDownlinkStreamId()));
        }
        if (getTestbedName() != null) {
            map.put("testbedName", getTestbedName());
        }

        if (getVenueType() != null) {
            map.put("venueType", getVenueType().toString());
        }
        
    }

    @Override
    public synchronized void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("VenueInformation");
        
        if (getVenueType() != null) {
            XmlUtility.writeSimpleElement(writer, "VenueType",
                getVenueType());
        }
        
        if (getVenueType() != null
                && getVenueType().hasTestbedName()
                && getTestbedName() != null) {
            XmlUtility.writeSimpleElement(writer, "TestbedName",
                    getTestbedName());
        }
        
        if (getVenueType() != null
                && getVenueType().hasStreams() && getDownlinkStreamId() != null) {
            XmlUtility.writeSimpleElement(writer, "DownlinkStreamId",
                DownlinkStreamType.convert(getDownlinkStreamId()));
        }
        
        writer.writeEndElement();
        
    }	
}
