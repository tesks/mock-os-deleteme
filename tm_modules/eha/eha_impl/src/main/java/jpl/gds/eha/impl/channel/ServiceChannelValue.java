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
package jpl.gds.eha.impl.channel;

import java.util.Map;

import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.eha.api.channel.ChannelCategoryEnum;
import jpl.gds.eha.api.channel.IAlarmValueSet;
import jpl.gds.eha.api.channel.IChannelValueFactory;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;

/**
 * ServiceChannelValue is the read/write base class for channel value classes. A
 * channel value is a single channel sample, which may be created from realtime
 * telemetry, recorded telemetry, data products, or ground data systems. A
 * channel value consists of a DN, or Data Number, which is essentially the
 * original, or raw measurement value of the channel at a point in time. From
 * this value, two other values may be developed. 1) A status value, which is
 * the result of translating an integer to a value in a list, or enumeration, of
 * display strings. 2) An EU, or Engineering Unit value, which is traditionally
 * defined to mean a floating point value derived from the DN via an algorithm,
 * lookup, or equation. <br>
 * A channel value is time-stamped, both with time information from the DN
 * source, and with times from the telemetry receiver. <br>
 * It is important to know the source of a channel value. It will behave
 * differently depending upon its source. The source/type of the channel should
 * be determined from its dictionary definition. <br>
 * A channel value must have an associated channel definition, which contains
 * information read from the project telemetry dictionary about the channel,
 * including its name, identifier, data type, size, unit, description, and
 * EU/Status translations.
 * 
 */
public abstract class ServiceChannelValue extends ClientChannelValue implements IServiceChannelValue {
	/**
	 * The Channel Definition reference for this channel.
	 */
	//private IChannelDefinition chanDef;
	
	private PacketIdHolder packetId = PacketIdHolder.UNSUPPORTED;
	private Long frameId;

	/**
	 * Category of channel value; extended for header channels.
	 * Should always be set to non-null. Not populated properly for
	 * non-header channels yet. Eventually, LOST should never be used
	 * for new values, just old values from database.
	 */
	private ChannelCategoryEnum category = ChannelCategoryEnum.LOST_HEADER;


	/**
	 * Creates an instance of AbstractChannelValue.
	 */
	public ServiceChannelValue()
	{
	    super();
	}


	@Override
    public void setRct(final IAccurateDateTime postTime) {
        rct = new AccurateDateTime(postTime.getTime());
	}

	@Override
	public void setDssId(final int dss) {
		dssId = dss;
	}

	@Override
	public void setVcid(final Integer vcid) {
		this.vcid = vcid;
	}

	@Override
	public void setScet(final IAccurateDateTime scet) {
		this.scet = scet;
	}

	@Override
	public void setLst(final ILocalSolarTime sol) {
		this.lst = sol;
	}

	@Override
	public void setEu(final double euVal)
	{
		eu = euVal;
		this.hasEu = true;
	}

	@Override
    public void setSclk(final ISclk sclk) {
		this.sclk = sclk;
	}

	@Override
	public void setErt(final IAccurateDateTime utc) {
		ert = utc;
	}

	@Override
	public void setDn(final Object dn)
	{
		this.dn = dn;
	}

	@Override
	public IChannelDefinition getChannelDefinition()
	{
		return super.getChannelDefinition();
	}

	@Override
	public void setChannelDefinition(final IChannelDefinition chanDef)
	{
		super.setChannelDefintion(chanDef);

		// compute status before building the object
		getStatus();

		//super.copyValuesFrom(chanDef);
//		this.chanId = chanDef.getId();
//		this.categories = chanDef.getCategories();
//		this.dataType = chanDef.getChannelType();
//		this.title = chanDef.getTitle();
//		this.definitionType = chanDef.getDefinitionType();
	}

	@Override
	public String getStatus()
	{	 
		final ChannelType ct = chanDef.getChannelType();

		if (ct.equals(ChannelType.STATUS) || ct.equals(ChannelType.BOOLEAN))
		{
			/* 
			 * Use computeStatus() instead of
			 * getStatus() in the channel definition.
			 */
			status = computeStatus();
		}

		return status;
	}


	@Override
	public void setDnFromBytes(final byte[] stuff, final int len) {
	    super.setDnFromBytes(stuff, len);
	}

	@Override
	public void setTemplateContext(final Map<String,Object> map) {
	    super.setTemplateContext(map);

		if (chanDef != null) {
			map.put("channelDef", true);
			((Templatable)chanDef).setTemplateContext(map);
		}

		/* R8 refactor TODO - taking a risk and commenting this out. It is difficult to do here
		 * because we lack the config object to do so. I can find no template using these variables
		 * that would also be used for output using a class using this class.  
		 */
//		if (dn != null) {
//			map.put("preFormatted", USE_FORMATTERS);
//			if (chanDef != null && hasEu()) {
//				/*
//				 *  Formatting EU in templates causes
//				 * template errors. Changed this to add two variables to the template for
//				 * EU: one formatted and the other unformatted, so templates can
//				 * choose which one to use. 
//				 */
//				if (USE_FORMATTERS && this.chanDef.getEuFormat() != null) {
//					map.put("formattedEu", new SprintfFormat().anCsprintf(chanDef.getEuFormat(), getEu()).trim());
//				} else {
//					map.put("formattedEu", getEu());
//				}
//			}
//
//			/*
//			 * Formatting data number in templates causes
//			 * template errors. Changed this to add two variables to the template for
//			 * data number: one formatted and the other unformatted, so templates can
//			 * choose which one to use. 
//			 */
//			if (USE_FORMATTERS && this.chanDef != null && this.chanDef.getDnFormat() != null) {
//			    
//			    // R8 Refactor TODO - Technically, if the DN formatting can be SCET, we need SCID here
//			    // to pass to the SprintfFormat constructor and there is just no non-global way to get that at this time
//				map.put("formattedDataNumber", new SprintfFormat().anCsprintf(chanDef.getDnFormat(), dn).trim());
//			} else {
//				map.put("formattedDataNumber", dn);
//			}
//
//		}
	}

	@Override
	public IServiceChannelValue copy(final IChannelValueFactory chanFactory) {
		final ServiceChannelValue ecdr = (ServiceChannelValue)chanFactory.createServiceChannelValue(chanDef);
		super.copyMembersTo(ecdr);
		if (alarms != null) {
		    ecdr.alarms = alarms.copy();	
		}
		//ecdr.chanDef = chanDef;
		ecdr.packetId = packetId;
		ecdr.frameId = frameId;

		return ecdr;
	}

	@Override
	public void setAlarms(final IAlarmValueSet alarms) {
		this.alarms = alarms;
	}

	@Override
	public abstract void setDnFromString(String value, int componentLength);

	@Override
	public void setRealtime(final boolean realtime) {
		this.realtime = realtime;
	}

//	@Override
//	public void setDefinition(final IChannelDefinition def) {
//		setChannelDefinition(def);
//	}

	@Override
	public PacketIdHolder getPacketId()
	{
		return packetId;
	}

	@Override
	public void setPacketId(final PacketIdHolder pid)
	{
		this.packetId = ((pid != null) ? pid : PacketIdHolder.UNSUPPORTED);
	}

	@Override
	public Long getFrameId()
	{
		return frameId;
	}

	@Override
	public void setFrameId(final Long frameId)
	{
		this.frameId = frameId;
	}

	@Override
	public ChannelCategoryEnum getChannelCategory()
	{
		return category;
	}

	@Override
	public void setChannelCategory(final ChannelCategoryEnum cce)
	{
		category = ((cce != null) ? cce : ChannelCategoryEnum.LOST_HEADER);
	}
	
    /**
     * Gets the status value from the state table, for enum/status and boolean
     * channels. This is used for mapping enumerated values from the flight
     * software to string values on the ground.
     * 
     * @return the string that the key maps to in the state table; if no entry
     *         for the id exists, the string "status=id" will be returned
     *
     * 8/31/17. Changed the return for boolean channels to false/true string
     * 6/18/20. Update return logic for boolean channels
     *          to correctly lookup map definition. 0/1 (false/true) can map to an
     *          (optional) defined string in the dictionary.
     */
    protected String computeStatus() {
        if (chanDef.getLookupTable() == null) {
        	if (chanDef.getChannelType().equals(ChannelType.BOOLEAN)) {
				return intValue() == 0 ? "False" : "True";
	        } else {
        		return "status=" + intValue();
	        }
        }
        final String s = chanDef.getLookupTable().getValue(intValue());
        if (s == null) {
	        if (chanDef.getChannelType().equals(ChannelType.BOOLEAN)) {
		        return intValue() == 0 ? "False" : "True";
	        } else {
		        return "status=" + intValue();
	        }
        }
        return s;
    }


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((alarms == null) ? 0 : alarms.hashCode());
		result = prime * result + ((category == null) ? 0 : category.hashCode());
		result = prime * result + ((chanDef == null) ? 0 : chanDef.hashCode());
		result = prime * result + ((frameId == null) ? 0 : frameId.hashCode());
		result = prime * result + ((packetId == null) ? 0 : packetId.hashCode());
		return result;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ServiceChannelValue other = (ServiceChannelValue) obj;
		if (alarms == null) {
			if (other.alarms != null)
				return false;
		} else if (!alarms.equals(other.alarms))
			return false;
		if (category != other.category)
			return false;
		if (chanDef == null) {
			if (other.chanDef != null)
				return false;
		} else if (!chanDef.equals(other.chanDef))
			return false;
		if (frameId == null) {
			if (other.frameId != null)
				return false;
		} else if (!frameId.equals(other.frameId))
			return false;
		if (packetId == null) {
			if (other.packetId != null)
				return false;
		} else if (!packetId.equals(other.packetId))
			return false;
		return true;
	}
}
