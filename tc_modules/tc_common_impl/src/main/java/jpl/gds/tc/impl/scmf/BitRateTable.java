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

package jpl.gds.tc.impl.scmf;

import jpl.gds.tc.api.config.ScmfProperties;


/**
 * This class represents the indexing table for uplink bit rates as defined in
 * the 820-013 0198-Telecomm for SCMFs.
 * 
 */
public class BitRateTable
{
	public static String units = "Bits/Second";
	
	/**
	 * The various allowable bit rate values.
	 */
	private static double[] bitRateTable = 
	{
		2000.0000,
		1000.0000,
		500.0000,
		250.0000,
		125.0000,
		62.5000,
		31.2500,
		15.6250,
		7.8125,
		256.0000,
		128.0000,
		64.0000,
		32.0000,
		16.0000,
		8.0000,
		4.0000,
		2.0000,
		1.0000
	};
	
	/**
	 * Given an enum index, find the actual bit rate value it corresponds to.
	 * 
	 * @param scmfConfig the SCMF configuration in use when this function is being called
	 * 
	 * @param index The numeric value stored in an SCMF indicating a particular bit rate.
	 * 
	 * @return The actual floating point bit rate corresponding to the input index value.
	 */
	public static double getBitRateFromIndex(ScmfProperties scmfConfig, int index)
	{
		boolean strictBitRateIndex = scmfConfig.isBitRateStrict();
		if(index <= 0)
		{
			if(strictBitRateIndex) {
				throw new IllegalArgumentException("Bitrate index must be greater than one.  Input was: " + index);				
			} else {
				index = 1;
			}
		}
		else if(index > bitRateTable.length)
		{
			if(strictBitRateIndex) {
				throw new IllegalArgumentException("Bitrate index must be less than " + bitRateTable.length + ".  Input was: " + index);
			} else {
				index = bitRateTable.length;
			}
		}
		
		//Shift the index by one since arrays are zero-indexed, but
		//the table is one-indexed. Real engineers count from zero. 
		return(bitRateTable[index-1]);
	}
}