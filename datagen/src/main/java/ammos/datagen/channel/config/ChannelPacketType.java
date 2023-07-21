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

/**
 * This class encapsulates the APID and pre-channelized indicator for one
 * defined channel packet type.
 * 
 *
 */
public class ChannelPacketType {

	private final int apid;
	private final boolean isPrechan;

	/**
	 * Constructor.
	 * 
	 * @param apid
	 *            APID for this packet type
	 * @param isPrechan
	 *            true if the packet is pre-channelized, false if it is
	 *            associated with a decom map
	 */
	public ChannelPacketType(final int apid, final boolean isPrechan) {

		this.apid = apid;
		this.isPrechan = isPrechan;
	}

	/**
	 * Retrieves the packet APID.
	 * 
	 * @return APID number
	 */
	public int getApid() {

		return this.apid;
	}

	/**
	 * Gets the flag indicating whether the packet is pre-channelized.
	 * 
	 * @return true if this packet type pre-channelized, false if not
	 */
	public boolean isPrechannelized() {

		return this.isPrechan;
	}
}
