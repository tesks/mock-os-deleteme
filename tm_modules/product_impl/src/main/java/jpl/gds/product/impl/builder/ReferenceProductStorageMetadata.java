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
package jpl.gds.product.impl.builder;

import jpl.gds.product.api.IPduType;
import jpl.gds.product.api.IProductPartProvider;
import jpl.gds.product.api.builder.IProductStorageMetadata;
import jpl.gds.product.impl.AbstractProductStorageMetadata;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ICoarseFineTime;
import jpl.gds.shared.time.ILocalSolarTime;

/**
 * Implementation of the storage metadata used for the reference product builder.
 * 
 *
 */
public class ReferenceProductStorageMetadata extends AbstractProductStorageMetadata {
    
	/**
	 * Constructor.
	 * 
	 * @param pduType PDU type 
	 */
	public ReferenceProductStorageMetadata(final IPduType pduType) {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param partNumber product part number
	 * @param offset data offset of the part
	 * @param localOffset part data offset in the temporary data file
	 * @param length part data length
	 * @param ert part earth receive time
	 * @param sclk part SCLK
	 * @param scet part spacecraft event time
	 * @param sol part LST time
	 * @param pktSequence sequence number of packet that contained the part
	 * @param relayScid relay spacecraft ID
	 * @param groupingFlags part grouping (record) flags
	 * @param pduType part PDU type
	 */
	public ReferenceProductStorageMetadata(final int partNumber, final long offset, final long localOffset, final int length,
			final IAccurateDateTime ert, final ICoarseFineTime sclk, final IAccurateDateTime scet, final ILocalSolarTime sol,
			final int pktSequence, final int relayScid, final int groupingFlags, final IPduType pduType) {
		super(partNumber, offset, localOffset, length, ert, sclk, scet, sol, pktSequence, relayScid, groupingFlags, pduType);
	}

	/**
	 * Constructor
	 * 
	 * @param localOffset part data offset in the temporary data file
	 * @param part product part provider
	 * @param pduType part PDU type
	 */
	public ReferenceProductStorageMetadata(final long localOffset, final IProductPartProvider part, final IPduType pduType) {
		super(localOffset, part);

	}

	@Override
	public int compareTo(final IProductStorageMetadata aps) {
		if (null == aps) {
			return 1;
		} else if (getPartNumber() < aps.getPartNumber()) {
			return -1;
		} else if (getPartNumber() > aps.getPartNumber()) {
			return 1;
		} else {
			return compareTimes(getErt(), aps.getErt());
		}
	}
	
	@Override
	public boolean hasData() {
		/**
		 * Never has data.
		 */
		return false;
	}
}
