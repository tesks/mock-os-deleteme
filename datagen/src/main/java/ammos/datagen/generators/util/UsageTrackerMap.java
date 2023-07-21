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

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jpl.gds.shared.log.TraceManager;


/**
 * This class tracks seed usage statistics common to multiple types of data
 * generators by keeping a map of UsageTracker objects. This map is indexed by a
 * "tracker map type", implemented by any of the TrackerType enumerations.
 * <p>
 * This class also contains a static reference to the global UsageTrackerMap
 * object established for each data generator run.
 * 
 *
 */
public class UsageTrackerMap {

	private static UsageTrackerMap globalTrackers = new UsageTrackerMap();

	/**
	 * The actual map of UsageTracker objects. The key used is the map type of
	 * an ITrackerType enumeration value. Accessible to subclasses.
	 */
	protected final Map<Integer, UsageTracker> trackers = new HashMap<Integer, UsageTracker>();

	/**
	 * Adds a UsageTracker to the map of trackers.
	 * 
	 * @param type
	 *            the key for the tracker. Must be the map type taken from one
	 *            of the ITrackerType enumeration classes.
	 * 
	 * @param tracker
	 *            the UsageTracker object to associate with the given type.
	 */
	public synchronized void addTracker(final Integer type,
			final UsageTracker tracker) {

		this.trackers.put(type, tracker);
	}

	/**
	 * Fetches the UsageTracker with the given type from the map.
	 * 
	 * @param type
	 *            the key for the tracker. Must be the map type taken from one
	 *            of the ITrackerType enumeration classes.
	 * @return matching UsageTracker object, or null if none in the map
	 */
	public synchronized UsageTracker getTracker(final Integer type) {

		return this.trackers.get(type);
	}

	/**
	 * Writes summary data for all UsageTracker object in the map to the
	 * specified file.
	 * 
	 * @param theFile
	 *            path to the output file
	 * @param append
	 *            true to append to the file, false to overwrite
	 */
	public void writeToFile(final String theFile, final boolean append) {

		try {
			final FileWriter fw = new FileWriter(theFile, append);
			fw.write(toString());
			fw.close();
		} catch (final IOException e) {
			TraceManager.getDefaultTracer().error("I/O Error writing file:", e);

		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public synchronized String toString() {

		final StringBuilder sb = new StringBuilder(
				"Usage Percentages for Seed Data:\n");
		/*
		 * Loops through all tracker types. Could as easily iterate over the map
		 * and would avoid a null check, but the TrackerType ordering is desired
		 * in the output.
		 */
		for (final TrackerType t : TrackerType.values()) {
			final UsageTracker ut = this.trackers.get(t.getMapType());
			if (ut == null) {
				continue;
			}
			sb.append("  " + t.getDisplayName() + ": "
					+ String.format("%.1f", ut.getPercentFilled()) + "%\n");
		}
		return sb.toString();
	}

	/**
	 * Gets the global UsageTrackerMap object for the current run. The global
	 * object is initialized to an empty UsageTrackerMap object, but may be
	 * overridden with a sub-classed object by a specific data generator.
	 * 
	 * @return UsageTrackerMap object
	 */
	public synchronized static UsageTrackerMap getGlobalTrackers() {

		return globalTrackers;
	}

	/**
	 * Sets the global UsageTrackerMap object for the current run. The global
	 * object is initialized to an empty UsageTrackerMap object, but may be
	 * overridden with a sub-classed object by a specific data generator.
	 * 
	 * @param trackers
	 *            UsageTrackerMap object to set
	 */
	public synchronized static void setGlobalTrackers(
			final UsageTrackerMap trackers) {

		if (trackers == null) {
			throw new IllegalArgumentException(
					"GlobalTrackers object cannot be set to null. It will screw up everything.");
		}
		globalTrackers = trackers;
	}

}
