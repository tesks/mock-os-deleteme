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

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;

import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.IProductPartProvider;

/**
 * An interface to be implemented by product builder transaction log storage 
 * classes.
 * 
 *
 */
public interface ITransactionLogStorage {

    /** OPEN event type **/
    String OPEN_EVENT = "OPEN";
    /** PART RECEIVED event type **/
    String PART_EVENT = "PART";
    /** END event type **/
    String END_EVENT = "END";
    /** ASSEMBLY event type **/
    String ASSEMBLE_EVENT = "ASSEMBLE";
    /** Grouping flags: part starts a new record **/
    String RECORD_START = "START";
    /** Grouping flags: part is a continuation of a record **/
    String RECORD_CONTINUED = "CONT";
    /** Grouping flags: part ends a record **/
    String RECORD_END = "END";
    /** Grouping flags: part not in any record **/
    String RECORD_NOT = "NOT";
    /** Grouping flags: invalid grouping flags **/
    String RECORD_INVALID = "INVALID";

    /**
     * Writes an OPEN event to the transaction log. An OPEN event means that an
     * MPDU packet has been received.
     * 
     * @param writer the transaction log Writer object
     * @param genericPart the product part whose receipt triggered the OPEN
     * @throws IOException if there is an error writing to the transaction log file
     * @throws ProductStorageException if there is any other type of error
     */
    void writeOpenEvent(Writer writer, IProductPartProvider genericPart) throws IOException, ProductStorageException;

    /**
     * Writes a PART RECEIVED event to the transaction log. This event means that a 
     * DPDU packet has been received.
     * 
     * @param writer the transaction log Writer object
     * @param genericPart the product part that was received
     * @throws IOException if there is an error writing to the transaction log file
     * @throws ProductStorageException  if there is any other type of error
     */
    void writePartEvent(Writer writer, IProductPartProvider genericPart) throws IOException, ProductStorageException;

    /**
     * Writes a END event to the log. An END event implies that an EPDU packet
     * has been received.
     * @param writer the log Writer object
     * @param genericPart the product part that was received
     * @throws IOException if there is an error writing to the transaction log file
     * @throws ProductStorageException  if there is any other type of error
     */
    void writeEndEvent(Writer writer, IProductPartProvider genericPart) throws IOException, ProductStorageException;

    /**
     * Writes an ASSEMBLY event to the log. This means that generation of a full or
     * partial data product has been requested.
     * 
     * @param writer the transaction log Writer object
     * @param cause the trigger (reason) for the assembly of the product
     * @throws IOException if there is an error writing to the transaction log file
     */
    void writeAssemblyTriggeredEvent(Writer writer, AssemblyTrigger cause) throws IOException;

    /**
     * Creates a product transaction by loading the transaction log file.
     * @param id the product transaction ID
     * @param reader the transaction log Reader object
     * @return the populated, mission-specific product transaction object
     * @throws ProductStorageException if there is an error loading the transaction log file
     */
    IProductTransactionProvider loadTransactionLog(String id, LineNumberReader reader) throws ProductStorageException;

    /**
     * Writes the metadata in the given product transaction object to the product metadata 
     * (EMD) file in response to an ASSEMBLE event.
     * 
     * @param w the transaction log Writer object
     * @param tx the product transaction to store to the EMD
     * @throws ProductStorageException if there is any problem writing the product
     * metadata file
     */
    void storeTransactionToMetadataFile(Writer w, IProductTransactionProvider tx) throws ProductStorageException;

	/**
	 * Writes a PART RECEIVED event to the log and includes the offset as reported by the storageMetadata input.
	 * Adding storage metadata support.
	 * 
	 * @param writer the transaction log Writer object
	 * @param genericPart the product part that was received
	 * @param storageMetadata storage object.
	 * @throws IOException if there is an error writing to the transaction log file
	 * @throws ProductStorageException  if there is any other type of error
	 */
	public abstract void writePartEvent(Writer writer, IProductPartProvider genericPart, IProductStorageMetadata storageMetadata)
	throws IOException, ProductStorageException;	

	/**
	 * Populates and returns a product metadata object from transaction information.
	 * @param mtx the transaction object for the product
	 * @return the product metadata; paths/filename are not populated
	 */
	public abstract IProductMetadataProvider getProductMetadata(IProductTransactionProvider mtx);
}