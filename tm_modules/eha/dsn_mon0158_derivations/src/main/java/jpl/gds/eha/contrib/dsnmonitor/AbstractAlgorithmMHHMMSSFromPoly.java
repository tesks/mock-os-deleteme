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
package jpl.gds.eha.contrib.dsnmonitor;

import java.util.HashMap;
import java.util.Map;

import jpl.gds.eha.channel.api.DerivationAlgorithmBase;
import jpl.gds.eha.channel.api.IChannelValue;


/**
 * AbstractAlgorithmMHHMMSSFromPoly is an abstract M-channel algorithmic
 * derivation class that derives one ASCII channel: HH:MM:SS from
 * parent channel that specifies an integer value of formula:
 * 
 * value = HH * 10000 + MM * 100 + SS
 * 
 * where  HH is 0 to 23, MM is 0 to 59, and SS is 0 to 60. It mimics
 * the algorithm in mon158.ccl.
 *
 */
abstract public class AbstractAlgorithmMHHMMSSFromPoly extends DerivationAlgorithmBase
{

	/**
     * Perform derivation
     *
	 * @param inPolyValue value of HH * 10000 + MM * 100 + SS, where
	 * 					HH is 0 to 23, MM is 0 to 59, and SS is 0 to 60
	 * @param chanName channel name of the derived channel
	 *
	 * @return The output channel contained in a ACVMap
	 */
	protected Map<String, IChannelValue> deriveMHHMMSSChannelFromPoly(final long inPolyValue, final String chanName) {
		long polyValue = inPolyValue;
		
    	final Map<String, IChannelValue> result = new HashMap<String, IChannelValue>();

		int hour = (int) (polyValue / 10000);
		polyValue = polyValue - (hour * 10000);
		
		int minutes = (int) (polyValue / 100);
		int secs = (int) (polyValue - (minutes * 100));

		/* HH MM and SS all need a leading zero if 1 digit */

		StringBuilder sb = new StringBuilder(8);

		if (hour < 10) {
			sb.append("0");
		}

		sb.append(hour); 	// sb = hour

		if (minutes < 10) {
			sb.append(":0");
		} else {
			sb.append(":");
		}

		sb.append(minutes);	// sb = hour : minutes

		if (secs < 10) {
			sb.append(":0");
		} else {
			sb.append(":");
		}

		sb.append(secs);	// sb = hour : minutes : secs

		/* derive child channel now */
		final IChannelValue child = createChannelValue(chanName, sb.toString());
		result.put(chanName, child);

		return result;
	}

}
