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
package ammos.datagen.generators.util;

/**
 * TrackerType is an enumeration used as an index by the UsageTrackerMap class,
 * to keep track of which UsageTracker is for which capability. This enumeration
 * lists the trackers common to all data generators. All enumerations that
 * implement the ITrackerType interface will assign a unique map index to every
 * value in the enumeration. The map type is in place because enums cannot be
 * extended, but I wanted to be able to add additional TrackerTypes for specific
 * data generators. If they all have a unique index, I can add them to a common
 * map.
 * 
 */
public enum TrackerType implements ITrackerType {
	/**
	 * TrackerType for the 8 bit integer table.
	 */
	INTEGER_8(1, "Integer 8-bit Table"),
	/**
	 * TrackerType for the 16 bit integer table.
	 */
	INTEGER_16(2, "Integer 16-bit Table"),
	/**
	 * TrackerType for the 32 bit integer table.
	 */
	INTEGER_32(3, "Integer 32-bit Table"),
	/**
	 * TrackerType for the 64 bit integer table.
	 */
	INTEGER_64(4, "Integer 64-bit Table"),
	/**
	 * TrackerType for the 8 bit unsigned table.
	 */
	UNSIGNED_8(5, "Unsigned 8-bit Table"),
	/**
	 * TrackerType for the 16 bit unsigned table.
	 */
	UNSIGNED_16(6, "Unsigned 16-bit Table"),
	/**
	 * TrackerType for the 32 bit unsigned table.
	 */
	UNSIGNED_32(7, "Unsigned 32-bit Table"),
	/**
	 * TrackerType for the 64 bit unsigned table.
	 */
	UNSIGNED_64(8, "Unsigned 64-bit Table"),
	/**
	 * TrackerType for the 32 bit float table.
	 */
	FLOAT_32(9, "Float 32-bit Table"),
	/**
	 * TrackerType for the 64 bit float table.
	 */
	FLOAT_64(10, "Float 64-bit Table"),
	/**
	 * TrackerType for the Packet SCLK table.
	 */
	SCLK(11, "SCLK Table"),
	/**
	 * Tracker type for the BOOLEAN table.
	 */
	BOOLEAN(12, "Boolean Table");

	private int mapType;
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
	private TrackerType(final int ordinal, final String displayName) {

		this.mapType = ordinal;
		this.name = displayName;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.util.ITrackerType#getMapType()
	 */
	@Override
	public int getMapType() {

		return this.mapType;
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

	/**
	 * Gets the maximum map type value from this enumeration class. Used to
	 * allow other ITrackerType enumerations to artificially extend this one.
	 * 
	 * @return maximum type found among all the possible values of this
	 *         enumeration
	 */
	public static int getMaxMapType() {

		int max = Integer.MIN_VALUE;
		for (final TrackerType t : TrackerType.values()) {
			max = Math.max(max, t.getMapType());
		}
		return max;
	}
}
