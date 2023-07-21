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
package jpl.gds.product.impl;

import java.io.IOException;
import java.io.RandomAccessFile;

import jpl.gds.product.api.IPduType;
import jpl.gds.product.api.IProductPartProvider;
import jpl.gds.product.api.builder.IProductStorageMetadata;
import jpl.gds.product.api.builder.ProductStorageException;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ICoarseFineTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.types.ByteArraySlice;

/**
 * Abstract class used by the product builder to find PPDU user data that has been stored in the product on disk cache.
 * This class is made to be immutable.  
 * 
 * The mission adaptation needs to handle the compare method as well.
 * 
 *
 */
public abstract class AbstractProductStorageMetadata implements Comparable<IProductStorageMetadata>, IProductStorageMetadata {
	// Not sure if it makes sense to have this class be comparable.  This may come out later.
	private final int partNumber;
	private final long offset;
	private final int length;
	private final IAccurateDateTime ert;
    private final ICoarseFineTime  sclk;
	private final IAccurateDateTime scet;
	private final ILocalSolarTime sol;
	private final int pktSequence;
	private final int relayScid;
	private final int groupingFlags;
	private final IPduType pduType;
	
	private ByteArraySlice buffer;

	/**
	 * Temporary packet file values.
	 */
	public final long localOffset;
	
	/**
	 * Used for non-data parts.
	 */
	public AbstractProductStorageMetadata() {
		this(-1, -1, -1, -1, null, null, null, null, -1, -1, -1, null);
	}
	
	/**
	 * Creates an AbstractProductStorageMetadata.
	 * @param partNumber the part number; starts at 1
	 * @param offset the part data offset into the product file
	 * @param localOffset the offset into the temporary packet file.
	 * @param length the part data length in bytes.  This is the length of the user data without any packet headers.
	 * @param ert the part receive time
	 * @param sclk the part spacecraft clock time
	 * @param scet the part spacecraft event time
	 * @param sol the part local solar time
	 * @param pktSequence the sequence number of the part source packet
	 * @param relayScid the numeric id of the relay spacecraft
	 * @param groupingFlags the record grouping flags from the source packet
	 * @param pduType part PDU type
	 */
	public AbstractProductStorageMetadata(final int partNumber, final long offset, final long localOffset, final int length,
			final IAccurateDateTime ert, final ICoarseFineTime sclk, final IAccurateDateTime scet, final ILocalSolarTime sol,
			final int pktSequence, final int relayScid, final int groupingFlags, final IPduType pduType)
	{
		this.partNumber = partNumber;
		this.offset = offset;
		this.localOffset = localOffset;
		this.length = length;
		this.ert = ert;
		this.sclk = sclk;
		this.scet = scet;
		this.sol = sol;
		this.pktSequence = pktSequence;
		this.relayScid = relayScid;
		this.groupingFlags = groupingFlags;
		this.pduType = pduType;
	}
	
	/**
	 * Constructor.
	 * 
	 * @param localOffset offset of the user data into the temporary product file in the disk cache.
	 * @param part product part provider
	 */
	public AbstractProductStorageMetadata(final long localOffset, final IProductPartProvider part) {
		this(part.getPartNumber(), 
				part.getPartOffset(), 
				localOffset, 
				part.getPartLength(), 
				part.getMetadata().getErt(), 
				part.getMetadata().getSclk(),
				part.getMetadata().getScet(),
				part.getMetadata().getSol(),
				part.getPacketSequenceNumber(),
				part.getRelayScid(),
				part.getGroupingFlags(),
				part.getPartPduType()
				);
	}

	
	// Abstract methods.
	/**
	 * Indicates if this object has associated part data.
	 * 
	 * @return true if this AbstractProductStorageMetadata has data.
	 */
	@Override
	public abstract boolean hasData();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ByteArraySlice getData(final RandomAccessFile raf) throws ProductStorageException {
		return getData(raf, 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ByteArraySlice getData(final RandomAccessFile raf, final Integer startOffsetAdjust) throws ProductStorageException {
		if (hasData() && null != buffer) {
			return buffer;
		}else if (hasData()) {
			// Seek to the proper place in the file.
			try {
				raf.seek(getLocalOffset());
			} catch (final IOException e) {
				throw new ProductStorageException(String.format("Could not seek to location in the file for product: %s (%s)",
						toString(), e.getMessage()));
			}
			
			final byte[] data = new byte[getLength()];
			int bytesRead = -1;
			try {
				bytesRead = raf.read(data);
			} catch (final IOException e) {
				throw new ProductStorageException(String.format("Could not get data from file for product: %s (%s)",
						toString(), e.getMessage()));
			}
			
			if (bytesRead != getLength()) {
				throw new ProductStorageException(String.format("Unable to read the necessary number of bytes from the file: dataLength=%d bytesRead=%d", 
						data.length, bytesRead));
			}

			buffer = new ByteArraySlice(data, 0, getLength());
			buffer.offset += startOffsetAdjust;
			
			return buffer;
		} else {
			return new ByteArraySlice();
		}
	}

	@Override
	public int getGroupingFlag() {
		return groupingFlags;
	}
	
	@Override
	public int getNumber() {
		return partNumber;
	}
	
	@Override
	public int getPartNumber() {
		return partNumber;
	}

	@Override
	public long getOffset() {
		return offset;
	}

	@Override
	public int getLength() {
		return length;
	}

	@Override
	public long getLocalOffset() {
		return localOffset;
	}
	
	@Override
	public IAccurateDateTime getErt() {
		return ert;
	}
	
	@Override
	public String getErtString() {
		if (ert == null) {
			return null;
		}
		final String result = ert.getFormattedErt(true);
		return result;
	}

	@Override
	public ICoarseFineTime getSclk() {
		return sclk;
	}


	@Override
	public String getScetStr()
    {
		if (scet == null)
        {
			return null;
		}

        return scet.getFormattedScet(true);
	}

	@Override
	public ILocalSolarTime getSol() {
		return sol;
	}

	@Override
	public String getSolStr() {
		if (sol == null) {
			return null;
		}
		return sol.getFormattedSol(true);
	}

	@Override
	public int getPktSequence() {
		return pktSequence;
	}


	@Override
	public int getRelayScid() {
		return relayScid;
	}

	/**
	 * Method to compare times.  
	 * 
	 * a is null and b is null => 0
	 * a is null and b is not null => -1
	 * a is not null and b is null => 1
	 * a is not null and b is not null => a.compareto(b)
	 * @param a first time
	 * @param b second time
	 * @return 0, 1, or -1
	 */
	protected int compareTimes(final IAccurateDateTime a, final IAccurateDateTime b) {
		if (null == a && null == b) {
			return 0;
		} else if (null == a && null != b) {
			return -1;
		} else if (null != a && null == b) {
			return 1;
		} else {
			return a.compareTo(b);
		}
	}

	@Override
	public String toString() {
		return "AbstractProductStorageMetadata [partNumber=" + partNumber
				+ ", offset=" + offset + ", length=" + length + ", ert=" + getErtString() 
				+ ", sclk=" + (sclk == null ? 0 : sclk)
				+ ", scet=" + getScetStr() + ", sol=" + getSolStr() 
				+ ", pktSequence=" + pktSequence + ", relayScid=" + relayScid
				+ ", groupingFlags=" + groupingFlags + ", localOffset="
				+ localOffset + "]";
	}
	
}

