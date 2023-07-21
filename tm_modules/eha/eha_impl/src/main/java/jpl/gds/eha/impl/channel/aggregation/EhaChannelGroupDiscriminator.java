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
package jpl.gds.eha.impl.channel.aggregation;

import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.eha.api.channel.ChannelCategoryEnum;
import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaGroupDiscriminator;
import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;
import jpl.gds.eha.api.message.aggregation.IEhaChannelGroupDiscriminator;
import jpl.gds.shared.time.CoarseFineEncoding;

/**
 * This class controls the "Uniqueness" of EHA Channel Groups
 * 
 * @TODO R8 Refactor TODO - add javadoc when this capability is mature
 *
 */
public class EhaChannelGroupDiscriminator implements IEhaChannelGroupDiscriminator {
    
    private final Integer vcid;
    private final int apid;
    private final int dssId;
    private final boolean isRealtime;
    private final boolean isSSE;
    private final ChannelDefinitionType chanType;
    private final CoarseFineEncoding sclkEncoding;
    private ChannelCategoryEnum chanCatEnum;

    /**
     * This class' purpose is to create an object composed of only those fields
     * that need to remain constant within a GroupMetadata collection of
     * channels
     * 
     * @param m
     *            The message to be added to an appropriate Channel Group.
     */
    public EhaChannelGroupDiscriminator(final IAlarmedChannelValueMessage m) {
        /*
         * Set invariant fields. Note: Invariant fields are represented by group
         * metadata, and do not need to be stored with the group member data.
         */
        final IClientChannelValue value = m.getChannelValue();
        final IServiceChannelValue serviceChan = (IServiceChannelValue) m.getChannelValue();
        this.chanCatEnum = serviceChan.getChannelCategory();
        this.vcid = value.getVcid();
        this.dssId = value.getDssId();
        this.apid = 0;
        this.isRealtime = value.isRealtime();
        this.isSSE = m.isFromSse();
        this.chanType = value.getDefinitionType();
        this.sclkEncoding = value.getSclk().getEncoding();
    }

    /**
     * Create an EhaChannelGroupDiscriminator out of a Proto3EhaGroupDiscriminator
     * 
     * @param proto
     */
    public EhaChannelGroupDiscriminator(final Proto3EhaGroupDiscriminator proto) {
    	this.vcid = proto.getVcid();
    	this.apid = proto.getApid();
    	this.dssId = proto.getDssId();
    	this.isRealtime = proto.getIsRealtime();
    	this.isSSE = proto.getIsFromSSE();
    	this.chanType = ChannelDefinitionType.valueOf(proto.getChanType().name().substring("CHAN_DEF_TYPE_".length()));
    	this.sclkEncoding = new CoarseFineEncoding(proto.getSclkEncoding());
    }
 
    /*
     * (non-Javadoc)
     * 
     * @see
     * jpl.gds.eha.api.channel.aggregation.IEhaChannelGroupDiscriminator#
     * getVcid()
     */
    @Override
    public int getVcid() {
        return vcid;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * jpl.gds.eha.api.channel.aggregation.IEhaChannelGroupDiscriminator#
     * getVcid()
     */
    @Override
    public int getApid() {
        return apid;
    }

    /**
     * @return the isRealtime
     */
    @Override
    public boolean isRealtime() {
        return isRealtime;
    }

    /**
     * @return the isSSE
     */
    @Override
    public boolean isSSE() {
        return isSSE;
    }

    /**
     * @return the chanType
     */
    @Override
    public ChannelDefinitionType getChanType() {
        return chanType;
    }

    /**
     * @return the sclkEncoding
     */
    @Override
    public CoarseFineEncoding getSclkEncoding() {
        return sclkEncoding;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + apid;
        result = prime * result + dssId;
        result = prime * result + ((chanType == null) ? 0 : chanType.hashCode());
        result = prime * result + (isRealtime ? 1231 : 1237);
        result = prime * result + (isSSE ? 1231 : 1237);
        result = prime * result + ((sclkEncoding == null) ? 0 : sclkEncoding.hashCode());
        result = prime * result + ((vcid == null) ? 0 : vcid);
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final EhaChannelGroupDiscriminator other = (EhaChannelGroupDiscriminator) obj;
        if (apid != other.apid)
            return false;
        if (dssId != other.dssId)
            return false;
        if (chanType != other.chanType)
            return false;
        if (isRealtime != other.isRealtime)
            return false;
        if (isSSE != other.isSSE)
            return false;
        if (sclkEncoding == null) {
            if (other.sclkEncoding != null)
                return false;
        }
        else if (!sclkEncoding.equals(other.sclkEncoding))
            return false;
        if (vcid != other.vcid)
            return false;
        if (chanCatEnum != other.chanCatEnum)
        	return false;
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "EhaChannelGroupDiscriminator [dssId=" + dssId + ", vcid=" + vcid + ", apid=" + apid + ", isRealtime="
                + isRealtime + ", isSSE=" + isSSE + ", chanType=" + chanType + ", sclkEncoding=" + sclkEncoding + "]";
    }

	@Override
	public Integer getDssId() {
		 return dssId;
	}

	@Override
	public ChannelCategoryEnum getChanCatEnum() {
		return chanCatEnum;
	}
}
