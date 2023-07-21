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

import java.io.RandomAccessFile;

import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ICoarseFineTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.types.ByteArraySlice;

/**
 * An interface to be implemented by product storage metadata classes.
 * 
 *
 * @since R8
 */
public interface IProductStorageMetadata {

	/**
	 * Indicates if there is part data related to this metadata object.
	 * 
	 * @return true if this metadata object has data.
	 */
	boolean hasData();

    /**
     * Reads the data out of the given file input stream and returns a ByteArraySlice.  
     * 
     * @param raf RandomAccessFile for the temporary packet storage file.
     * @return the byte slice
     * @throws ProductStorageException
     */
	ByteArraySlice getData(RandomAccessFile raf) throws ProductStorageException;

	/**
	 * Reads the data out of the given file input stream and returns a ByteArraySlice.  
	 * 
	 * @param raf RandomAccessFile for the temporary packet storage file.
	 * @param startOffsetAdjust starting offset to adjust to before pulling data
	 * @return the byte slice
	 * @throws ProductStorageException
	 */
	ByteArraySlice getData(RandomAccessFile raf, Integer startOffsetAdjust) throws ProductStorageException;

	/**
	 * Gets the grouping (record) flags from this metadata.
	 * 
	 * @return grouping flag value
	 */
	int getGroupingFlag();

    /**
     * Gets the part number from this metadata.
     * 
     * For legacy reasons need to keep the old name to get the part number. The
     * emd templates are calling this method to create the emd files.
     * 
     * @return part number
     */
	int getNumber();

	/**
	 * Returns the part number from this metadata.
	 * 
     * @return part number
	 */
	int getPartNumber();

	/**
	 * Returns the part data offset from this metadata.
	 * 
	 * @return the byte offset.
	 */
	long getOffset();

	/**
	 * Returns the part data length from this metadata.
	 * 
	 * @return byte length.
	 */
	int getLength();

    /**
     * Returns the offset into the local temporary packet file that the user
     * data was stored in.
     * 
     * @return byte offset
     */
	long getLocalOffset();

	/**
	 * Gets the earth receive time from this metadata.
	 * 
	 * @return ERT
	 */
	IAccurateDateTime getErt();

	/**
	 * Returns the ert as an ISO-formatted time string.
	 * 
	 * @return ISO-formatted time string.
	 */
	String getErtString();

	/**
	 * Gets the SCLK from this metadata.
	 * 
	 * @return the sclk.
	 */
	ICoarseFineTime getSclk();

	/**
	 * Returns the scet from this metadata as an ISO-formatted string.
	 * @return an ISO-formatted string.
	 */
	String getScetStr();

	/**
	 * Returns the LST, or SOL time, from this metadata.
	 * 
	 * @return ILocalSolarTime.
	 */
	ILocalSolarTime getSol();

	/**
	 * Returns the LST, or SOL time, from this metadata as a formatted string.
	 * 
	 * @return formatted string.
	 */
	String getSolStr();

	/**
	 * Returns the packet sequence number from this metadata.
	 * 
	 * @return sequence number
	 */
	int getPktSequence();

	/**
	 * Returns the relay spacecraft ID from this metadata.
	 * 
	 * @return SCID
	 */
	int getRelayScid();
}