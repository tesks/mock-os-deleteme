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
package jpl.gds.evr.impl.service.notify;

import java.util.ArrayList;
import java.util.List;

import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.service.IEvrNotificationTrigger;
import jpl.gds.shared.types.Pair;

/**
 * This class defines the EVR trigger that is based on EVR's event ID numbers.
 * It can specify a set of them, either single ID numbers or ranges of them. For
 * ID ranges, the matches are done inclusively.
 * 
 *
 */
public class EvrIdTrigger implements IEvrNotificationTrigger {
	private static final int ID_LIST_SIZE = 8;
	private static final int ID_RANGE_LIST_SIZE = 1;

	private final List<Integer> ids;

	/**
	 * Note: Each pair's left value is always smaller than or equal to the right
	 * value.
	 */
	private final List<Pair<Integer, Integer>> ranges;

	/**
	 * Default constructor.
	 */
	public EvrIdTrigger() {
		ids = new ArrayList<Integer>(ID_LIST_SIZE);
		ranges = new ArrayList<Pair<Integer, Integer>>(ID_RANGE_LIST_SIZE);
	}

	/**
	 * Add an event ID to the set of ID numbers to look for in incoming EVRs.
	 * 
	 * @param id
	 *            event ID to monitor for
	 */
	public void addId(final int id) {
		ids.add(id);
	}

	/**
	 * Add a range of event ID to the set of ID ranges to look for in incoming
	 * EVRs.
	 * 
	 * @param a
	 *            lower value of the ID range, inclusive
	 * @param b
	 *            upper value of the ID range, inclusive
	 */
	public void addIdRange(int a, int b) {

		/*
		 * Technically we don't want range's end values to be supplied out of
		 * order, but just accept it. It's not the most difficult thing in the
		 * world.
		 */
		if (a > b) {
			int tmp = a;
			a = b;
			b = tmp;
		}

		ranges.add(new Pair<Integer, Integer>(a, b));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.evr.api.service.IEvrNotificationTrigger#evrTriggersNotification(jpl.gds.evr.api.IEvr)
	 */
	@Override
	public boolean evrTriggersNotification(final IEvr evr) {

		/*
		 * Almost certain that ID is not big enough to merit a 'long'
		 * comparison.
		 */
		int evrId = (int) evr.getEventId();

		for (int id : ids) {

			if (evrId == id) {
				return true;
			}

		}

		for (Pair<Integer, Integer> range : ranges) {

			if (evrId >= range.getOne() && evrId <= range.getTwo()) {
				return true;
			}

		}

		return false;
	}

	/**
	 * Get method for obtaining the list of singly-defined EVR IDs, that the
	 * trigger looks for.
	 * 
	 * @return list of single IDs that have been defined for this trigger
	 */
	public List<Integer> getSingleIDs() {
		return ids;
	}

	/**
	 * Get method for obtaining the list of EVR ID ranges that the trigger looks
	 * for.
	 * 
	 * @return list of ID ranges that have been defined for this trigger
	 */
	public List<Pair<Integer, Integer>> getIDRanges() {
		return ranges;
	}

}
