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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.context.ApplicationContext;

import jpl.gds.product.api.IPduType;
import jpl.gds.product.api.IProductMetadataUpdater;
import jpl.gds.product.api.builder.IProductStorageMetadata;
import jpl.gds.product.api.builder.IProductTransactionUpdater;
import jpl.gds.product.api.builder.ProductStorageException;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;

/**
 * This class represents the transaction conducted by the product
 * builder when constructing a product. It should be used as the
 * base class for mission-specific transaction classes.
 */
public abstract class AbstractProductTransaction implements IProductTransactionUpdater, Templatable {

    /**
     * Trace logger instance shared with all instances.
     */
    protected static final Tracer                       productLogger    = TraceManager.getTracer(Loggers.TLM_PRODUCT);


	/** Invalid grouping flags **/
	public static final int RECORD_INVALID = 0;

	/** Grouping flag: part starts a new record **/
	public static final int RECORD_START = 1;

	/** Grouping flag: part is a continuation of a record **/
	public static final int RECORD_CONTINUED = 2;

	/** Grouping flag: part ends a record **/
	public static final int RECORD_END = 3;

	/** Grouping flag: part not in any record **/
	public static final int RECORD_NOT = 4;

	/** Transaction ID */
	protected String id;
	/** Product filename */
	protected String filename;
	/** Total expected product bytes */
	protected long totalBytes;
	/** Product metadata */
	protected IProductMetadataUpdater metadata;
	/** Map of product parts */
	protected HashMap<Integer, IProductStorageMetadata> parts;
	/** List of record offsets */
	protected List<Long> recordOffsets;
	/** product file size */
	protected long fileSize;
	/** Indicates if MPDU received */
	protected boolean receivedMetadata;
	/** Indicates if EPDU received */
	protected boolean receivedEnd;
	/** Product active directory */
	protected String activeDir;
    /** Current application context */
	protected final ApplicationContext appContext;

	/**
	 * Creates an instance of a product transaction.
	 * @param appContext the current application context
	 * @param md a mission-specific product metadata object
	 */
	public AbstractProductTransaction(final ApplicationContext appContext, final IProductMetadataUpdater md) {
		this.appContext = appContext;
        productLogger.setAppContext(appContext);
		parts = new HashMap<Integer, IProductStorageMetadata>();
		receivedMetadata = false;
		receivedEnd = false;
		fileSize = -1;
		metadata = md;
	}

	/**
	 * Retrieves the receivedMetadata flag, indicating whether a metadata packet has been received for
	 * the product.
	 * @return the receivedMetadata true if metadata has been received
	 */
	@Override
    public boolean isReceivedMetadata() {
		return receivedMetadata;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public boolean isReceivedEnd() {
		return !(fileSize == -1);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
    public boolean allPartsReceived() {
		return isReceivedMetadata() && isReceivedEnd() && !hasGaps();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public void setReceivedMetadata(final boolean receivedMetadata) {
		this.receivedMetadata = receivedMetadata;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public IProductMetadataUpdater getMetadata() {
		return metadata;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public String getId() {
		if (id == null) {
			return "UNKNOWN";
		}
		return id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public void setId(final String string) {
		id = string;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public String getFilename() {
		if (filename == null) {
			return "UNKNOWN";
		}
		return filename;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public void setFilename(final String string) {
		filename = string;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public abstract boolean hasGaps();

	/**
	 * {@inheritDoc}
	 */
	@Override
    public int getMissingParts() {
		int total  = metadata.getTotalParts();
		int missing = 0;
		
		if (total == 0) {
			total = getReceivedPartCount();
		}

		// The part map only contains the data parts in it.  Parts are stared 
		// as their part numbers so we start at part 1.
		for (int i = 1; i <= total; ++i) {
			if (parts.get(i) == null) {
				++missing;
			}
		}
		return missing;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public void setTotalParts(final int totalParts) {
		if (metadata.getTotalParts() < totalParts) {
			metadata.setTotalParts(totalParts);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public void setFileSize(final long fileBytes) {
		if (fileSize == -1) {
			fileSize = fileBytes;
		}
	}

	/*
	 * 6/29/2014 - Made this abstract since an AbstractProductStorageMetadata class needs to
	 * be created and added to the internal container. 
	 * 
	 *  Adding logic to check if the current part we are trying to add is the same size to any duplicates we have
	 *  already received.  See the ProductConfig for specific configuration values.  This was previously done in the status
	 *  cache, but we do not keep that record anymore for the load method of the product part to use.  The transaction log is the only place
	 *  where product information is stored so this seems like the best place to do the check with the new product builder design.  
	 *  
	 *  When the transaction log is read in by the TransactionLogStorage class it reads from top to bottom and if there are duplicates they 
	 *  will get replaced in the map until only the latest of each part is left.  I am leveraging this fact so if there is a map collision
	 *  we check to see if we are supposed to check for different sizes.  If we are, we check that the value in the map has the same size as 
	 *  the value that is going to replace it.  If there is a problem we throw a storage exception and this product will not be assembled.
	 */
	

	@Override
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
			) throws ProductStorageException {
		
		final IProductStorageMetadata storageMetadata = createProductStorageMetadata(number,
				offset, 
				localOffset, 
				length, 
				ert, 
				sclk, 
				scet, 
				sol, 
				pktSequence, 
				relayScid, 
				groupingFlags, 
				partPduType);
		
		/**
		 * The key is the part number, no more of this less than
		 * stuff that made everything really confusing.  
		 */
		if (parts.containsKey(number) && 
			parts.get(number).getLength() != storageMetadata.getLength()) {
			
			throw new ProductStorageException(String.format("Early part [%s] !=  Later part [%s]", 
					parts.get(number), storageMetadata));
		}
		
		parts.put(number,  storageMetadata);
	}

	/**
	 * Creates a storage metadata object that is mission specific.  
	 * The addPart method calls this mission specific implementation
	 * to add a part object to the internal hashmap.
	 * 
	 * @param number part number
	 * @param offset part data offset
	 * @param localOffset the offset into the local temporary packet file
	 * @param length part data length
	 * @param ert part earth received time
	 * @param sclk part SCLK
	 * @param scet part spacecraft event time
	 * @param sol part LST time
	 * @param pktSequence packet sequence number of the packet the part came in
	 * @param relayScid relay spacecraft ID
	 * @param groupingFlags part grouping (record) flags
	 * @param partPduType part PDU type
	 * @return new metadata instance
	 */
	public abstract IProductStorageMetadata createProductStorageMetadata(final int number,
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
			final IPduType partPduType);
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IProductStorageMetadata getStorageMetadataForPart(final int number) {
		return parts.get(number);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public int getLastPartNumber() {
		int lastPartNum = 0;
		for (int i = 0; i < getReceivedPartCount(); i++) {
			final IProductStorageMetadata p = parts.get(i);
			if (p != null) {
				if (p.getPartNumber() > lastPartNum) {
					lastPartNum = p.getPartNumber();
				}
			}
		}
		return lastPartNum;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public int getFirstPartNumber() {
		int firstPartNum = 0;
		for (int i = 1; i <= getReceivedPartCount(); i++) {
			final IProductStorageMetadata p = parts.get(i);
			if (p != null) {
				firstPartNum = p.getPartNumber();
				break;
			}
		}
		return firstPartNum;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public int getReceivedPartCount() {
		return parts.size();
	}
		 	 
	/**
	 * {@inheritDoc}
	 */
	@Override
    public int getRecordCount() {
		findRecords();
		return recordOffsets.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public long getRecordOffset(final int n) {
		findRecords();
		return (recordOffsets.get(n-1)).longValue();
	}

	/** 
	 * Locates the records in the product being assembled, producing
	 * a count of records and a list of their offsets that is stored
	 * in this class.  Does not include records in parts not yet 
	 * received.
	 */
	protected void findRecords() {
		if (recordOffsets != null) {
			return;
		}
		recordOffsets = new ArrayList<Long>();
		/**
		 *  Start at part number 1.
		 */
		for (int i = 1; i <= metadata.getTotalParts(); ++i) {
			final IProductStorageMetadata part = parts.get(i);
			if (part == null) {
				// skip
			} else if (part.getGroupingFlag() == RECORD_START) {
				recordOffsets.add(new Long(part.getOffset()));
			} else if (part.getGroupingFlag() == RECORD_NOT) {
				recordOffsets.add(new Long(part.getOffset()));
			}
		}
		if (recordOffsets.size() == 0) {
			recordOffsets.add(new Long(0));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public String getActiveDir() {
		return activeDir;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public void setActiveDir(final String activeDir) {
		this.activeDir = activeDir;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setTemplateContext(final Map<String, Object> map) {
		map.put("numReceivedParts", getReceivedPartCount());

		/**
		 *  Parts are stored using their part number, not a minus one.  Start at 1.
		 *  Simplified this whole thing.  Creating a parts list of all the expected parts and then removing the 
		 *  parts that we have.  What is left is the missing parts.
		 */

		/**
		 *  The total parts from the metadata will either be the highest received part number in the event 
		 *  that the EPDU was not received or the proper number of data parts if the EPDU was received.  This will 
		 *  work for cases where products have a separate EPDU or when the EPDU is included in the the last data part.
		 */
		final TreeSet<Integer> missingParts = new TreeSet<Integer>(); 
		for (int partNumber = 1; partNumber <= this.metadata.getTotalParts(); partNumber++) {
			missingParts.add(partNumber);
		}

		// Get the missing list by removing all of the parts we have received.
		missingParts.removeAll(parts.keySet());

		// Get the sorted list of part objects we received. This sorts based on the part number and collects the 
		// results into a set.
		final TreeSet<IProductStorageMetadata> receivedParts = new TreeSet<IProductStorageMetadata>(parts.values());

		map.put("partList", receivedParts);
		map.put("missingPartList", missingParts);

		/**
		 *  Call the abstract method to add extra data to the map.
		 */
		setExtraTemplateContext(map);

	}
	
	/**
	 *  Copies all of the storage metadata objects into a sorted set
	 * and will order them by ....
	 * 
	 * @return sorted set of metadata objects
	 */
	public SortedSet<IProductStorageMetadata> getStorageMetadataSet() {
		final SortedSet<IProductStorageMetadata> ss = new TreeSet<IProductStorageMetadata>();
		ss.addAll(parts.values());
		
		return ss;
	}
	
	/**
	 * Get the sorted set of the part numbers that are part of this transaction.  This is a better way to get the
	 * parts in sorted order instead of using the comparator of the storage metadata objects.
	 * 
	 * @return sorted set of the part numbers in this transaction.
	 */
	public SortedSet<Integer> getStorageMetadataPartNumberSet() {
		return new TreeSet<Integer>(parts.keySet());
	}
	
	/**
	 * setTemplateContext calls this method in case the mission adaptation needs to set any extra data in the map
	 * to be used when writing a transaction to a metadata file.
	 * 
	 * @param map template map to update
	 */
	public abstract void setExtraTemplateContext(final Map<String, Object> map);
}