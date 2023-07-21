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

import jpl.gds.shared.types.Pair;

/**
 * This class stores the packet rotation count for one packet APID. The rotation
 * count indicates how many packets for this APID should be generated before the
 * data generator moves to the next APID. In addition to using the accessors
 * defined here, APID and count members of this object can also be accessed
 * using the getOne() and getTwo() methods, respectively, of the underlying Pair
 * class.
 * 
 */
@SuppressWarnings("serial")
public class ApidRotationCount extends Pair<Integer, Long> {

	/**
	 * Constructor.
	 * 
	 * @param theApid
	 *            the packet APID
	 * @param theCount
	 *            the rotation count for this packet
	 */
	public ApidRotationCount(final int theApid, final long theCount) {

		this.setOne(theApid);
		this.setTwo(theCount);
	}

	/**
	 * Gets the packet APID associated with this count.
	 * 
	 * @return apid
	 */
	public int getApid() {

		return this.getOne();
	}

	/**
	 * Sets the packet APID associated with this count.
	 * 
	 * @param apid
	 *            the APID value to set
	 */
	public void setApid(final int apid) {

		this.setOne(apid);
	}

	/**
	 * Gets the packet APID rotation count.
	 * 
	 * @return rotation count
	 */
	public long getCount() {

		return this.getTwo();
	}

	/**
	 * Sets the packet APID rotation count.
	 * 
	 * @param count
	 *            count value to set
	 */
	public void setCount(final long count) {

		this.setTwo(count);
	}

}
