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
package jpl.gds.time.api.service;

import jpl.gds.dictionary.api.frame.EncodingType;
import jpl.gds.shared.time.ISclk;

/**
 * This is the interface implemented by all flight Time Correlation packet parsers.
 * 
 *
 */
public interface ITimeCorrelationParser {
	/**
	 * Parses the time correlation packet data into member fields.
	 * 
	 * @param packetData byte array consisting of the entire time correlation packet
	 */
	public void parse(byte[] packetData);
	
	/**
	 * Gets the frame virtual channel that was in the TC packet.
	 * 
	 * @return VCID
	 */
	public int getVcid();
	
	/**
	 * Gets the frame virtual channel sequence counter that was in the TC packet.
	 * 
	 * @return VCFC
	 */
	public long getVcfc();
	
	/**
	 * Gets the SCLK that was in the TC packet.
	 * 
	 * @return SCLK
	 */
	public ISclk getSclk();
	
	/**
	 * Gets the frame encoding type that was in the TC packet.
	 * 
	 * @return EncodingType
	 */
	public EncodingType getEncType();
	
	/**
	 * Gets the bit rate index that was in the TC packet.
	 * 
	 * @return bit rate index (mission specific)
	 */
	public long getRateIndex();
}
