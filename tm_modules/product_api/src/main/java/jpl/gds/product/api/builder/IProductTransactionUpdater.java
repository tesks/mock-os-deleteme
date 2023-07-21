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
package jpl.gds.product.api.builder;

import jpl.gds.product.api.IPduType;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.ILocalSolarTime;


/**
 * A writer interface to be implemented by product transaction classes.
 * 
 *
 * @since R8
 */
public interface IProductTransactionUpdater extends IProductTransactionProvider {

	/**
	 * Sets the receivedMetadata flag, indicating whether a metadata packet has been received for
	 * the product.
	 * @param receivedMetadata the receivedMetadata to set
	 */
	void setReceivedMetadata(boolean receivedMetadata);

	/**
	 * Sets the unique transaction ID for this product builder transaction.
	 * @param string the ID to set
	 */
	void setId(String string);

	/**
	 * Sets the filename of the product being built.
	 * @param string the filename, not including the directory path
	 */
	void setFilename(String string);

	/**
	 * Sets the number of total expected parts in the product being 
	 * assembled. The total parts is only set in the product metadata if 
	 * it currently has a 0 value for total parts. This prevents a 0 total parts 
	 * value in a late PDU from overwriting any value set upon receipt of an EPDU.
	 * @param totalParts the total number of expected parts 
	 */
	void setTotalParts(int totalParts);

	/**
	 * Sets the expected data file size including all parts. The value will
	 * only be set if the file size member currently has a -1 value. This prevents
	 * a files size value from a late PDU from overwriting any value from an EPDU. 
	 * @param fileBytes the expected number of bytes in the file.
	 */
	void setFileSize(long fileBytes);

	/**
	 * Adds a part definition to the transaction.  This calls the createProductStorageMetadata method to add the part to the internal hashmap.
	 * 
	 * @param number the part number; starts at 1
	 * @param offset the part data offset into the product file
	 * @param localOffset the offset into the local temporary packet file
	 * @param length the part data length in bytes
	 * @param ert the part receive time
	 * @param sclk the part spacecraft clock time
	 * @param scet the part spacecraft event time
	 * @param sol the part local solar time
	 * @param pktSequence the sequence number of the part source packet
	 * @param relayScid the numeric id of the relay spacecraft
	 * @param groupingFlags the record grouping flags from the source packet
	 * @param partPduType The PDU type of the source packet
	 * @throws ProductStorageException if there is an issue adding the part
	 */
	public void addPart (final int number,
			final long offset, 
			final long localOffset,
			final int length,
			final IAccurateDateTime ert, 
			final ISclk sclk, 
			final IAccurateDateTime scet, 
			final ILocalSolarTime sol,
			final int pktSequence, 
			final int relayScid, 
			final int groupingFlags,
			final IPduType partPduType
			) throws ProductStorageException;
	
    /**
	 * Sets the active (temporary product builder) directory path for this transaction.
	 * 
	 * @param activeDir directory path to set
	 */
	void setActiveDir(String activeDir);

}