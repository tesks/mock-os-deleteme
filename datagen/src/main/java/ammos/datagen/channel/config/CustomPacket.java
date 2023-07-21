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
package ammos.datagen.channel.config;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jpl.gds.shared.types.Pair;

/**
 * This class holds the configuration data for a custom channel packet. A custom
 * packet definition includes the requested packet APID and the requested list
 * of channel ID/DN value pairs to put into the packet.
 * 
 *
 */
public class CustomPacket {
	private final int apid;
	private final List<Pair<String, String>> dnValues;

	/**
	 * Constructor.
	 * 
	 * @param apid
	 *            APID for this custom packet
	 */
	public CustomPacket(final int apid) {

		this.apid = apid;
		this.dnValues = new LinkedList<Pair<String, String>>();
	}

	/**
	 * Adds a channel ID and DN value to the list of DN values that should be
	 * generated in this custom packet.
	 * 
	 * @param channelId
	 *            Channel ID in the dictionary
	 * @param dnValue
	 *            the DN value as a string; value must be parseable using the
	 *            dictionary channel type
	 */
	public void addDn(final String channelId, final String dnValue) {

		this.dnValues.add(new Pair<String, String>(channelId, dnValue));
	}

	/**
	 * Gets the APID for this custom packet.
	 * 
	 * @return packet APID
	 */
	public int getApid() {

		return this.apid;
	}

	/**
	 * Gets the list of Channel ID/DN value pairs that should be used to
	 * populate the packet.
	 * 
	 * @return a List of Pairs, where the first element in the Pair is the
	 *         channel ID and the second is the DN value; non-modifiable
	 */
	public List<Pair<String, String>> getDnValues() {

		return Collections.unmodifiableList(this.dnValues);
	}

	/**
	 * Gets a list of the channel IDs in this custom packet.
	 * 
	 * @return List of String channel IDs
	 */
	public List<String> getChannelIds() {

		final List<String> result = new LinkedList<String>();
		for (final Pair<String, String> pair : this.dnValues) {
			result.add(pair.getOne());
		}
		return result;
	}
}
