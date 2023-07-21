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
 * This interface is implemented by TrackerType enumerations, to allow instances
 * to have both a specific ordinal map index and a display name for use in
 * reports.
 * 
 */
public interface ITrackerType {

	/**
	 * Gets the map type associated with the enum value. (Used as an index into
	 * the UsageTrackerMap table).
	 * 
	 * @return integer value
	 */
	public int getMapType();

	/**
	 * Gets the display name associated with the enum value. (Used in reports.)
	 * 
	 * @return descriptive name
	 */
	public String getDisplayName();
}
