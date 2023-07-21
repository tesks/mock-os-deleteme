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

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.EnableLstContextFlag;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.IProductPartProvider;
import jpl.gds.product.api.builder.AssemblyTrigger;
import jpl.gds.product.api.builder.IProductBuilderObjectFactory;
import jpl.gds.product.api.builder.IProductStorageMetadata;
import jpl.gds.product.api.builder.IProductTransactionProvider;
import jpl.gds.product.api.builder.IReceivedPartsTracker;
import jpl.gds.product.api.builder.ITransactionLogStorage;
import jpl.gds.product.api.builder.ProductStorageException;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * AbstractTransactionLogStorage is the base class on which all mission-specific product
 * transaction log storage classes should be built. Transaction log storage handles
 * the reading and writing of the product builder's transaction log file, which records
 * all received parts and product assembly events for each received data product.
 *
 */
public abstract class AbstractTransactionLogStorage implements ITransactionLogStorage {

	/** Trace logger shares with subclasses */
    protected final Tracer log;

	/** Version of event **/
	protected static final String EVENT_FORMAT_VERSION = "B";
	/** Event field delimiter **/
	protected static final String EVENT_FIELD_SEPARATOR = ",";
	/** Log line delimiter **/
	protected static final String LOG_LINE_SEPARATOR = "\n";

	/** Flag indicating whether local solar times should be used in the product metadata.**/
	protected final boolean setSolTimes;
	
	/** Current application context */
	protected final ApplicationContext appContext;
	
	/** Received parts tracker object */
	protected final IReceivedPartsTracker tracker;
	
	/** Product builder object instance factory */
	protected final IProductBuilderObjectFactory instanceFactory;
	
	/**
	 * Creates an instance of AbstractTransactionLogStorage.
	 * @param appContext the current application context
	 * @param instanceFactory Product builder object instance factory
	 */
	public AbstractTransactionLogStorage(final ApplicationContext appContext, final IProductBuilderObjectFactory instanceFactory) {
		this.appContext = appContext;
        log = TraceManager.getTracer(appContext, Loggers.PRODUCT_DECOM);

	  	setSolTimes = appContext.getBean(EnableLstContextFlag.class).isLstEnabled();
	  	
	  	this.tracker = appContext.getBean(IReceivedPartsTracker.class);
	  	this.instanceFactory = instanceFactory;
	}

	/**
	 * Writes an OPEN event to the transaction log. An OPEN event means that an
	 * MPDU packet has been received.
	 * 
	 * @param writer the transaction log Writer object
	 * @param genericPart the ProductPart whose receipt triggered the OPEN
	 * @throws IOException if there is an error writing to the transaction log file
	 * @throws ProductStorageException if there is any other type of error
	 */
	@Override
	public abstract void writeOpenEvent(Writer writer, IProductPartProvider genericPart)
	throws IOException, ProductStorageException;

	/**
	 * Writes a PART RECEIVED event to the transaction log. This event means that a 
	 * DPDU packet has been received.
	 * 
	 * @param writer the transaction log Writer object
	 * @param genericPart the Product Part that was received
	 * @throws IOException if there is an error writing to the transaction log file
	 * @throws ProductStorageException  if there is any other type of error
	 */
	@Override
	public abstract void writePartEvent(Writer writer, IProductPartProvider genericPart)
	throws IOException, ProductStorageException;
	
	/**
	 * Writes a PART RECEIVED event to the log and includes the offset as reported by the storageMetadata input.
	 *
	 * @param writer the transaction log Writer object
	 * @param genericPart the Product Part that was received
	 * @param storageMetadata storage object.
	 * @throws IOException if there is an error writing to the transaction log file
	 * @throws ProductStorageException  if there is any other type of error
	 */
	@Override
	public abstract void writePartEvent(Writer writer, IProductPartProvider genericPart, IProductStorageMetadata storageMetadata)
	throws IOException, ProductStorageException;	

	/**
	 * Writes a END event to the log. An END event implies that an EPDU packet
	 * has been received.
	 * @param writer the log Writer object
	 * @param genericPart the Product Part that was received
	 * @throws IOException if there is an error writing to the transaction log file
	 * @throws ProductStorageException  if there is any other type of error
	 */
	@Override
	public void writeEndEvent(final Writer writer, final IProductPartProvider genericPart)
	throws IOException, ProductStorageException {}

	/**
	 * Writes an ASSEMBLY event to the log. This means that generation of a full or
	 * partial data product has been requested.
	 * 
	 * @param writer the transaction log Writer object
	 * @param cause the trigger (reason) for the assembly of the product
	 * @throws IOException if there is an error writing to the transaction log file
	 */
	@Override
	public void writeAssemblyTriggeredEvent(final Writer writer, final AssemblyTrigger cause)
	throws IOException {
		// Version of event log format
		writer.write(EVENT_FORMAT_VERSION + EVENT_FIELD_SEPARATOR);
		// Type of event (ASSEMBLE)
		writer.write(ASSEMBLE_EVENT + EVENT_FIELD_SEPARATOR);
		// Wall clock
		writer.write(String.valueOf(System.currentTimeMillis()) + EVENT_FIELD_SEPARATOR);
		// Cause of assembly
		writer.write(cause.toString() + "\n");
	}

	/**
	 * Creates a ProductTransaction by loading the transaction log file.
	 * @param id the product transaction ID
	 * @param reader the transaction log Reader object
	 * @return the populated, mission-specific ProductTransaction object
	 * @throws ProductStorageException if there is an error loading the transaction log file
	 */
	@Override
	public abstract IProductTransactionProvider loadTransactionLog(
			String id, LineNumberReader reader) throws ProductStorageException;

	/**
	 * Populates and returns a product metadata object from transaction information.
	 * @param mtx the transaction object for the product
	 * @return the product metadata; paths/filename are not populated
	 */
	@Override
	public abstract IProductMetadataProvider getProductMetadata(IProductTransactionProvider mtx);

	/**
	 * Writes the metadata in the given ProductTransaction object to the product metadata 
	 * (EMD) file in response to an ASSEMBLE event.
	 * 
	 * @param w the transaction log Writer object
	 * @param tx the ProductTransaction to store to the EMD
	 * @throws ProductStorageException if there is any problem writing the product
	 * metadata file
	 */
	@Override
	public abstract void storeTransactionToMetadataFile(Writer w, IProductTransactionProvider tx)
	throws ProductStorageException;

	/**
	 * Parses an integer from a string and catches any error.
	 * @param s the string to parse
	 * @return the integer value, or 0 if the input cannot be parsed
	 */
	protected int parseInt(final String s) {
		try {
			return Integer.parseInt(s);
		} catch (final NumberFormatException e) {
			e.printStackTrace();
            log.warn("Error parsing integer '", s, "'");
			return 0;
		}
	}

	/**
	 * Parses a long value from a string and catches any error.
	 * @param s the string to parse
	 * @return the long value, or 0 if the input cannot be parsed
	 */
	protected long parseLong(final String s) {
		try {
			return Long.parseLong(s);
		} catch (final NumberFormatException e) {
            log.warn("Error parsing long '", s, "'");
			return 0;
		}
	}

	/**
	 * Parses a float value from a string and catches any error..
	 * @param s the string to parse
	 * @return the float value, or 0 if the input cannot be parsed
	 */
	protected float parseFloat(final String s) {
		try {
			return Float.parseFloat(s);
		} catch (final NumberFormatException e) {
            log.warn("Error parsing float '", s, "'");
			return 0;
		}
	}

	/**
	 * Parses the grouping flags for an incoming part and returns an enumerated
	 * value represented the found flag. 
	 * 
	 * @param s the flag string to parse
	 * @return one of the RECORD grouping flag constants in the
	 * ProductTransaction class
	 */
	protected int parseGroupingFlags(final String s) {
		if (s.equals(RECORD_CONTINUED)) {
			return IProductTransactionProvider.RECORD_CONTINUED;
		}
		if (s.equals(RECORD_START)) {
			return IProductTransactionProvider.RECORD_START;
		}
		if (s.equals(RECORD_END)) {
			return IProductTransactionProvider.RECORD_END;
		}
		if (s.equals(RECORD_NOT)) {
			return IProductTransactionProvider.RECORD_NOT;
		}
		return IProductTransactionProvider.RECORD_INVALID;
	}
}
