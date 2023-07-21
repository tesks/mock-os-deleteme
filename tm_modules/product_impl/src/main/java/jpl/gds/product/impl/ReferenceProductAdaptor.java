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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.product.api.IPduType;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.IProductPartProvider;
import jpl.gds.product.api.IReferenceProductMetadataProvider;
import jpl.gds.product.api.builder.AssemblyTrigger;
import jpl.gds.product.api.builder.IProductStorageMetadata;
import jpl.gds.product.api.builder.ProductStorageConstants;
import jpl.gds.product.api.builder.ProductStorageException;
import jpl.gds.product.api.message.IProductStartedMessage;
import jpl.gds.product.impl.builder.ReferenceProductStorageMetadata;
import jpl.gds.shared.types.ByteArraySlice;

/**
 * This is the Reference Product Builder Adaptor class. It is the Product Mission Adapter 
 * implementation for the reference mission.
 * 
 */
public class ReferenceProductAdaptor extends AbstractProductAdapter {

	private final HashMap<Long,String> productFilenameMap = new HashMap<Long,String>();

	/**
	 * Creates an instance of ReferenceProductAdaptor.
	 * @param context the current application context
	 * @throws DictionaryException if there is a problem creating the APID dictionary
	 */
	public ReferenceProductAdaptor(final ApplicationContext context) throws DictionaryException {
		super(context);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IProductStorageMetadata storePartData(final File baseDirectory, final IProductPartProvider part)
	throws ProductStorageException
	{
     	/*
		 * Removed the updates to the received parts tracker.  This is handled
		 * in the storePart method in the abstract adaptor.
		 */
		final IPduType pduType = part.getPartPduType();
		if (pduType.isMetadata() || pduType.isEnd()) {
			return new ReferenceProductStorageMetadata(pduType);
		}
		else {
			final String id = part.getTransactionId();
			/*
			 * use product transaction ID rather than filename as directory name
			 */
			final File partDir = new File(baseDirectory,
					part.getTransactionId());
			if (!partDir.exists() && !partDir.mkdirs()) {
				throw new ProductStorageException("Could not create directory "
						+ partDir + " for " + id);
			}
			final File dataFile = new File(partDir, ProductStorageConstants.TEMP_DATA_FILE);

			final ByteArraySlice data = part.getData();
			RandomAccessFile raf = null;
			try {
				raf = new RandomAccessFile(dataFile, "rw");
				final long offset = part.getPartOffset();
				raf.seek(offset);
				raf.write(data.array, data.offset, data.length);
				raf.close();
			}
			catch (final FileNotFoundException e) {
				throw new ProductStorageException("Data file not found: "
						+ dataFile, e);
			}
			catch (final IOException e) {
				throw new ProductStorageException("Error writing data file "
						+ dataFile, e);
			}
			lastParts[part.getVcid()] = part;

			/**
			 * Reference will always have a local offset of -1 because we
			 * are not using the local offset.
			 */
			return new ReferenceProductStorageMetadata(-1, part, pduType);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAssemblyTrigger(final IProductPartProvider part) {
		if (productChange(part)) {
			return true;
		}
		if (part.getPartPduType().isEnd() || part.getPartPduType().isEndOfData()) {
			return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AssemblyTrigger getAssemblyTrigger(final IProductPartProvider part) {
		/**
		 *  Cleaning up this code.  Was calling the get transaction id method
		 * a bunch of times so just do it once and use it.  
		 */

		final String transactionId = part.getTransactionId();
		final int vcid = part.getVcid();

		synchronized (tracker) {

			// First, set the total parts value in the ReceivedPartsTracker.
			if (!tracker.isTotalPartsKnown(vcid, transactionId)) {

				// If total number of parts for this product is unknown, set it.
				// In CFDP, each part carries the total number of parts info.
				tracker.setTotalParts(vcid, transactionId, part.getMetadata().getTotalParts());
			}

			final IPduType pduType = part.getPartPduType();

			// If this is an END pdu, must adjust
			// the parts tracker total part count.
			if (pduType.isEnd()) {
				/**
				 * The set total parts method will not allow us to decrement the count.  Use the new
				 * method that will not do a check when setting the number of parts.
				 */
				tracker.setTotalPartsDirect(vcid, transactionId, part.getMetadata().getTotalParts()-1);
			}

			// Now for the real action. If there was a product change, assemble.
			if (productChange(part)) {
				return AssemblyTrigger.PROD_CHANGE;
			}

			if (productConfig.isForcePartialsOnEndPdu()
					&& (pduType.isEnd() || pduType.isEndOfData())) {

				// If configured to force out partials upon receiving an END
				// PDU, regardless of whether or not all parts have been
				// received, trigger assembly here.
				log.debug("Forcing END_PART for product " + transactionId);
				return AssemblyTrigger.END_PART;
			}

			if (tracker.allPartsReceived(vcid, transactionId)) {

				// If all parts have been received for this product, trigger an
				// assembly with END_PART, even if this is not the END PDU. This
				// situation will occur when packets are received out of order.
				log.debug("Determined all parts received for product " + transactionId);
				
				return AssemblyTrigger.END_PART;
			}

		}

		return AssemblyTrigger.NO_TRIGGER;
	}


	/**
	 * Publishes a product started message to the internal message context.
	 * 
	 * @param part the first product part received for a specific data product
	 */
	protected void publishStartMessage(final IProductPartProvider part) {
		final String type = getType(part.getApid());
		final String id = part.getTransactionId();
	    final IProductStartedMessage m = messageFactory.
	                createProductStartedMessage(type, part.getApid(), part.getVcid(),id, part.getMetadata().getTotalParts());
		bus.publish(m);
	}

	/**
	 * Gets the product type name/description given relevant
	 * product type identifier(s).
	 * @param apid the product APID
	 * @return the product type name/description
	 */
	protected String getType(final int apid) {
		/*  Go through APID definition to get name. */
		return apidReference.getApidDefinition(apid).getName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getProductType(final IProductPartProvider part) {
		return getType(part.getApid());
	}

	/**
	 * Indicates whether the given part belongs to a different part than the last one
	 * stored.  Note that this method operates by comparing the product directory +
	 * filename for the old part vs. the new one.  If this ever becomes an ambiguous
	 * product identification method, this method must be changed. Note also the
	 * previous part is set by a call to storePartData() or storePartReceivedEvent().
	 * If neither of these calls have been made for the previous part, this method will 
	 * return an invalid answer.
	 * 
	 * @param part the new part
	 * @return true if the part belongs to a different product than  the last oneO
	 * stored; false if not
	 * 
	 */
	protected boolean productChange(final IProductPartProvider part) {
		if (lastParts[part.getVcid()] == null) {
			return false;
		}
		if (!productConfig.isForcePartialsOnChange()) {
			return false;
		}   
		final long newTransId = ((IReferenceProductMetadataProvider)part.getMetadata()).getCfdpTransactionId();
		final IProductPartProvider tempPart = lastParts[part.getVcid()];
		final long oldTransId = ((IReferenceProductMetadataProvider)tempPart.getMetadata()).getCfdpTransactionId();
		return newTransId != oldTransId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getProductType(final IProductMetadataProvider md) {
		return getType(md.getApid());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void storePartReceivedEvent(final File activeDirectory, final IProductPartProvider part,
			final IProductStorageMetadata storageMetadata) throws ProductStorageException {
		/*
		 *  use product transaction ID rather than filename as directory name
		 */
		final File dir = new File(activeDirectory, part.getTransactionId());
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				throw new ProductStorageException("Couldn't create directory "
						+ dir + " to store event " + "for transaction "
						+ part.getTransactionId());
			}
		}
		final File file = new File(dir, ProductStorageConstants.TRANS_LOG_FILE);
		final boolean transactionAlreadyOpen = file.exists();
		FileWriter writer = null;
		try {
			writer = new FileWriter(file, true);
			if (!transactionAlreadyOpen || part.getPartPduType().isMetadata()) {

				transactionLogStorage.writeOpenEvent(writer, part);
				if (!transactionAlreadyOpen) {
					publishStartMessage(part);
				}
			}
			if (part.getPartPduType().isData() ||
					part.getPartPduType().isEndOfData() ||
					part.getPartPduType().isEnd()) {
				transactionLogStorage.writePartEvent(writer, part);
			}
		} catch (final IOException e) {
			throw new ProductStorageException("Can't write to file " + file);
		} finally {
			close(writer);
			setLastPart(part);
		}
		setLastPart(part);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addToFilenameMap(final long transactionId, final String filename) {
		productFilenameMap.put(transactionId, filename);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFromFilenameMap(final long transactionId) {
		return productFilenameMap.get(transactionId);
	}

}
