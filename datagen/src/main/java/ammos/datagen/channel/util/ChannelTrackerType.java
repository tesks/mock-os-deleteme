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
package ammos.datagen.channel.util;

import ammos.datagen.generators.util.ITrackerType;
import ammos.datagen.generators.util.TrackerType;

/**
 * This enumeration defines the list of UsageTracker object unique to channel
 * generation. It is an artificial extension of the general TrackerType
 * enumeration (since enums cannot be extended). As such, it assigns map types
 * to its members starting at 1 + the maximum map index in the TrackerType
 * class.
 * 
 *
 */
public enum ChannelTrackerType implements ITrackerType {
	/**
	 * ChannelTrackerType for invalid channel indices.
	 */
	INVALID_INDEX(TrackerType.getMaxMapType() + 1,
			"Invalid Channel Index Table");

	private int type;
	private String name;

	/**
	 * Constructor
	 * 
	 * @param mapType
	 *            the map index value for the enum value; must be unique among
	 *            all enumerations that implement ITrackerType
	 * @param displayName
	 *            a display name for the specific tacker the enum value maps to
	 */
	private ChannelTrackerType(final int mapType, final String displayName) {

		this.type = mapType;
		this.name = displayName;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.util.ITrackerType#getMapType()
	 */
	@Override
	public int getMapType() {

		return this.type;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.util.ITrackerType#getDisplayName()
	 */
	@Override
	public String getDisplayName() {

		return this.name;
	}
}
