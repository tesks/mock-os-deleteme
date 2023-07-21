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

import java.util.Map;

import jpl.gds.dictionary.api.eu.EUGenerationException;
import jpl.gds.eha.channel.api.IChannelValue;
import jpl.gds.eha.channel.api.ParameterizedEuBase;

/**
 * This is an M-channel EU computation class that computes a floating
 * point seconds.nanoseconds from separate seconds and nanoseconds 
 * values. The nanoseconds value is the supplied DN. The seconds 
 * value is stored as the DN of the channel identified by the 
 * "secondsChildId" parameter. This mimics the algorithm in mon158.ccl.
 * 
 */
public class AlgorithmNanosecondEuMaker extends ParameterizedEuBase {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double eu(final String channelId, final Map<String, String> parameters, final double dn)
			throws EUGenerationException {
		final String secondsChannelId = parameters.get("secondsChannelId");
		if (secondsChannelId == null) {
			throw new 
			EUGenerationException("secondsChannelId parameter not defined in EU algorithm for channel " 
					+ channelId);
		}
		final IChannelValue secsChannel =  
				getMostRecentChannelValue(secondsChannelId, 
						true, getStation(parameters));
		if (secsChannel == null) {
			throw new 
			EUGenerationException("No LAD value found for parent channel " + secondsChannelId);
		}
		final double factor = Math.pow(10.0, 9.0);
		final double eu = secsChannel.doubleValue() + (dn / factor);

		return eu;
	}
}

