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

import java.util.ArrayList;
import java.util.List;

/**
 * UsageTracker tracks usage of seed data. Each data value in the seed table or
 * configured list of seed values has a slot in the usage tracker. When a seed
 * value is used, its slot is marked in the tracker. The percentage of total
 * seed values used out of the complete table can then be computed.
 * 
 *
 */
public class UsageTracker {

	private List<Boolean> slots = new ArrayList<Boolean>(0);

	/**
	 * Allocates slots for seed values in the UsageTracker and marks them all as
	 * "not used".
	 * 
	 * @param numSlots
	 *            number of slots to allocate
	 */
	public void allocateSlots(final int numSlots) {

		this.slots = new ArrayList<Boolean>(numSlots);
		for (int i = 0; i < numSlots; i++) {
			this.slots.add(false);
		}
	}

	/**
	 * Marks a slot as used.
	 * 
	 * @param slotNum
	 *            slot number, indexed from 0, of the slot to mark used.
	 */
	public void markSlot(final int slotNum) {

		if (slotNum < 0 || slotNum >= this.slots.size()) {
			throw new IllegalArgumentException("slot number is out of range");
		}

		this.slots.set(slotNum, true);
	}

	/**
	 * Gets the percentage of slots marked as used out of the total number of
	 * slots.
	 * 
	 * @return percentage, from 0.0 to 100.0
	 */
	public double getPercentFilled() {

		if (this.slots.isEmpty()) {
			return 0.0;
		}
		int marked = 0;
		for (final Boolean b : this.slots) {
			if (b.booleanValue()) {
				marked++;
			}
		}
		return ((double) marked / (double) this.slots.size()) * 100.0;
	}
}
