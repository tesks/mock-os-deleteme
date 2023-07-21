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
 * AbstractAlgorithmMHHMMSSFromSecs is an abstract M-channel algorithmic
 * derivation class that derives one ASCII channel: HH:MM:SS from
 * parent channel that specifies seconds in a day. It mimics the
 * algorithm in mon158.ccl.
 *
 */
abstract public class AbstractAlgorithmMHHMMSSFromSecs extends DerivationAlgorithmBase
{

	/**
     * Perform derivation
     *
	 * @param inputSecsOfDay seconds in a day to convert
	 * @param chanName channel name of the derived channel
	 *
	 * @return The output channel contained in a ACVMap
	 */
	protected Map<String, IChannelValue> deriveMHHMMSSChannelFromSecs(final int inputSecsOfDay, final String chanName) {
		int secsOfDay = inputSecsOfDay;
		
		
    	final Map<String, IChannelValue> result = new HashMap<String, IChannelValue>();

		int hour = secsOfDay / 3600;
		secsOfDay = secsOfDay % 3600;	/* secs is now secs of hour */

		/* HH MM and SS all need a leading zero if 1 digit */

		StringBuilder sb = new StringBuilder(8);

		if (hour < 10) {
			sb.append("0");
		}

		sb.append(hour); 	// sb = hour

		int minutes = secsOfDay / 60;
		secsOfDay = secsOfDay % 60;	/* secs is now secs of minute */

		if (minutes < 10) {
			sb.append(":0");
		} else {
			sb.append(":");
		}

		sb.append(minutes);	// sb = hour : minutes

		if (secsOfDay < 10) {
			sb.append(":0");
		} else {
			sb.append(":");
		}

		sb.append(secsOfDay);	// sb = hour : minutes : secs

		/* derive child channel now */
		final IChannelValue child = createChannelValue(chanName, sb.toString());
		result.put(chanName, child);

		return result;
	}

}
