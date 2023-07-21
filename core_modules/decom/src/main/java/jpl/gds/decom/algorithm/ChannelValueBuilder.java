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
package jpl.gds.decom.algorithm;

import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.Sclk;

/**
 * Builder object used to hold data needed to recreate a channel value
 * in a generic fashion.
 *
 */
public class ChannelValueBuilder {
	private ISclk sclk;
	private String channelId;
	
	private Object dn;
	private double eu;
	private boolean euSet;

	/**
	 * Create a new instance of the channel value builder.
	 */
	public ChannelValueBuilder() {
		 sclk = new Sclk(0, 0);
		 channelId = "";

	}
	
	/**
	 * Create a new instance with its fields initially copied from
	 * another instance.
	 * @param other builder containing parameters to copy
	 */
	public ChannelValueBuilder(ChannelValueBuilder other) {
		sclk = other.getSclk();
		dn = other.getDn();
		euSet = other.isEuSet();
		eu = other.getEu();
		channelId = other.getChannelId();
		
	}

	/**
	 * Get the channel ID string for the channel value.
	 * @return channel ID string
	 */
	public String getChannelId() {
		return channelId;
	}

	/**
	 * Get the EU value for the channel value. Call {@link #isEuSet()}
	 * to determine whether the EU is intentionally set. If the EU
	 * is not intentionally set, this EU is some arbitrary defaul value.
	 * @return the EU of the channel value
	 */
	public double getEu() {
		return eu;
	}

	/**
	 * Determine whether an EU has been set on this channel value
	 * @return true if an EU has been set
	 */
	public boolean isEuSet() {
		return euSet;
	}

	/**
	 * Get the data number (DN) for the channel value
	 * @return the DN
	 */
	public Object getDn() {
		return dn;
	}

	/**
	 * Get the SCLK for the channel value
	 * @return the SCLK
	 */
	public ISclk getSclk() {
		return sclk;
	}

	/**
	 * Set the SCLK for the channel value
	 * @param sclk the SCLK to associate with the channel value
	 * @return the builder
	 */
	public ChannelValueBuilder setSclk(Sclk sclk) {
		this.sclk = sclk;
		return this;
	}
	
	/**
	 * Set the channel ID for the channel value.  This will be used
	 * by channelization services to lookup the channel definition in
	 * a dictionary
	 * @param channelId the channel ID string
	 * @return the builder
	 */
	public ChannelValueBuilder setChannelId(String channelId) {
		this.channelId = channelId;
		return this;
	}
	
	/**
	 * Set the data number (DN) for the channel value
	 * @param dn the DN object for the channel value
	 * @return the builder
	 */
	public ChannelValueBuilder setDn(Object dn) {
		this.dn = dn;
		return this;
	}
	
	/**
	 * Set the EU for the channel value. Subsequent
	 * calls to {@link #isEuSet()} will return true.
	 * @param eu the EU value for the channel value
	 * @return the builder
	 */
	public ChannelValueBuilder setEu(double eu) {
		this.eu = eu;
		euSet = true;
		return this;
	}
	
}