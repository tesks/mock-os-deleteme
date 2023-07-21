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

package jpl.gds.tc.impl.icmd;

import gov.nasa.jpl.icmd.schema.BitRateAndModIndexType;
import jpl.gds.tc.api.icmd.datastructures.CpdConfiguration;
import jpl.gds.tc.api.icmd.datastructures.CpdConnectionStatus;

/**
 * A class representing the set of parameters retrieved fro CPD service.
 *
 * @since	AMPCS R3
 * @see CpdClient		
 */
public class CpdParameters {

	private CpdConfiguration cpdConfig;
	private CpdConnectionStatus connStatus;
	private BitRateAndModIndexType bitrateModindex;
	
	
	/**
	 * Default constructor.
	 */
	public CpdParameters() {
		super();
	}

	
	/**
	 * Copy constructor.
	 *
	 * @param src the original CPD parameters object to copy from
	 */
	public CpdParameters(final CpdParameters src) {
		super();
		this.cpdConfig = src.getCpdConfig();
		this.connStatus = src.getConnStatus();
		this.bitrateModindex = src.getBitrateModindex();
	}


	/**
	 * Returns the CPD configuration.
	 *
	 * @return the CPD configuration
	 */
	public CpdConfiguration getCpdConfig() {
		return cpdConfig;
	}
	
	
	/**
	 * Returns the connection status.
	 *
	 * @return the connection status
	 */
	public CpdConnectionStatus getConnStatus() {
		return connStatus;
	}
	
	
	/**
	 * Returns the bit-rate/mod-index.
	 *
	 * @return the bit-rate/mod-index
	 */
	public BitRateAndModIndexType getBitrateModindex() {
		return bitrateModindex;
	}


	/**
	 * Sets CPD configuration parameter.
	 *
	 * @param cpdConfig the CPD configuration parameter to set
	 */
	public void setCpdConfig(CpdConfiguration cpdConfig) {
		this.cpdConfig = cpdConfig;
	}


	/**
	 * Sets the connection status parameter.
	 *
	 * @param connStatus the connection status parameter to set
	 */
	public void setConnStatus(CpdConnectionStatus connStatus) {
		this.connStatus = connStatus;
	}


	/**
	 * Sets the bit-rate/mod-index parameter.
	 *
	 * @param bitrateModindex the bit-rate/mod-index parameter to set
	 */
	public void setBitrateModindex(BitRateAndModIndexType bitrateModindex) {
		this.bitrateModindex = bitrateModindex;
	}
	
}
