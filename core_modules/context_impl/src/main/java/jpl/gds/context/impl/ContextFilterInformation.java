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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.common.filtering.DssIdFilter;
import jpl.gds.common.filtering.VcidFilter;
import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.context.api.filtering.IFilterableDataItem;
import jpl.gds.context.api.filtering.IStationFilterable;
import jpl.gds.context.api.filtering.IVcidFilterable;
import jpl.gds.shared.metadata.ISerializableMetadata;
import jpl.gds.shared.metadata.MetadataKey;
import jpl.gds.shared.metadata.MetadataMap;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.shared.xml.XmlUtility;

/**
 * An object containing data filtering information for use in a context configuration.
 * 
 *
 * @since R8
 */
public class ContextFilterInformation implements IContextFilterInformation {
	
	private Integer vcid;
	private Integer dssId;
	private boolean dirty = true;
	private ISerializableMetadata header;
	private VcidFilter vcidFilter = new VcidFilter(null, true);
	private DssIdFilter stationFilter = new DssIdFilter(null, true);
    private boolean enableScidCheck;
	
	@Override
	public void setVcid(final Integer vcid) {
		this.vcid = vcid;
		this.dirty = true;
		if (vcid == null) {
		    vcidFilter = new VcidFilter(null, true);
		} else {
		     final List<UnsignedInteger> vcids = new ArrayList<>();
		     vcids.add(UnsignedInteger.valueOf(vcid));
		     this.vcidFilter = new VcidFilter(vcids, true);
		}
	}

	@Override
	public Integer getVcid() {
		return this.vcid;
	}

	@Override
	public void setDssId(final Integer dssId) {
	    this.dssId = dssId;
	    this.dirty = true;
	    if (dssId == null) {
	        stationFilter = new DssIdFilter(null, true);
	    } else {
	        final List<UnsignedInteger> dssIds = new ArrayList<>();
	        dssIds.add(UnsignedInteger.valueOf(dssId));
	        this.stationFilter = new DssIdFilter(dssIds, true);
	    }
	}

	@Override
	public Integer getDssId() {
		return this.dssId;
	}

	@Override
	public void copyValuesFrom(final IContextFilterInformation toCopy) {
		this.vcid = toCopy.getVcid();
		this.dssId = toCopy.getDssId();
		this.dirty = true;
		
	}

	@Override
	public ISerializableMetadata getMetadataHeader() {
		
	    if (header == null || isDirty()) {
	        header =  new MetadataMap();

	        if (getDssId() != null) {
	            header.setValue(MetadataKey.CONFIGURED_DSSID, getDssId());
	        }

	        if (getVcid() != null) {
	            header.setValue(MetadataKey.CONFIGURED_VCID, getVcid());
	        }
	        this.dirty = false;

	    }
		return header;
	}

    @Override
    public boolean isDirty() {

        return this.dirty;
    }

    @Override
    public void setTemplateContext(final Map<String, Object> map) {
        
        if (getVcid() != null) {
            map.put("contextVcid", getVcid());
        }

        if (getDssId() != null) {
            map.put("contextDssId", getDssId());
        }
    }

    @Override
    public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("FilterInformation");
        
        if (getDssId() != null) {
            XmlUtility.writeSimpleElement(writer, "DssId", getDssId());
        }
        
        if (getVcid() != null) {
            XmlUtility.writeSimpleElement(writer, "Vcid", getVcid());
        }

        writer.writeEndElement();
    }
    
    @Override
    public boolean accept(final IFilterableDataItem data, final SseContextFlag sseFlag) {
        // These checks are expensive. If a filter vcid was never set, don't do
        // the rest
        if (this.vcid != null) {
        if (data instanceof IVcidFilterable && !sseFlag.isApplicationSse()) {
                final Integer vc = ((IVcidFilterable)data).getVcid();
                if (!vcidFilter.accept(vc == null ? null : UnsignedInteger.valueOf(vc.intValue()))) {
                    return false;
                }
            }
        }
        // These checks are expensive. If a filter dssid was never set, don't do
        // the rest
        if (this.dssId != null) {
        if (data instanceof IStationFilterable && !sseFlag.isApplicationSse()) {
                final Integer dss = ((IStationFilterable)data).getDssId();
                if (!stationFilter.accept((dss == null || dss.intValue() == -1 || dss.intValue() == 0) ? 
                        null : UnsignedInteger.valueOf(dss.intValue()))) {
                    return false;
                }
            }
        }
        return true;
    }
}
