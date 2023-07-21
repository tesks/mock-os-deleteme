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
import java.io.FileWriter;
import java.io.IOException;

/**
 * Class used by the abstract product adaptor to store information about the
 * products that are in progress. The only members are an open file writer for
 * writing to the log and a boolean to indicate if the transaction was already
 * opened. Should be sub-classed by mission specific product adaptors to fit the
 * mission needs.
 * 
 *
 */
public abstract class AbstractProductInProgress {
    /** Transaction log file writer */
	protected final FileWriter logWriter;
	/** Transaction log file */
	protected final File txLogFile;
	/** Flag indicating whether a transaction has been opened */
	protected boolean transactionAlreadyOpen;
    
	/**
	 * @param filePath - File path to the transaction log file.  Used to detect when a cache has been deleted outside of the current process.
	 * @param txlogWriter - Open FileWriter for the transaction log.
	 * @param transactionAlreadyOpen - If the transaction was already opened.
	 */
	public AbstractProductInProgress(final String filePath, final FileWriter txlogWriter, final boolean transactionAlreadyOpen) {
		this.logWriter = txlogWriter;
		this.transactionAlreadyOpen = transactionAlreadyOpen;
		
		/**
		 * Keep a file object for the txlog file to make sure that the cache directory was not wiped
		 * out by anoher chill down because a complete product was created and the cache was cleaned.
		 */
		this.txLogFile = new File(filePath);
		
	}		
	
	/**
	 * Verify the cache is still intact. 
	 * 
	 * @return If the tx log file exists.
	 */
	public boolean validate() {
		return txLogFile.exists();
	}
	
	/**
	 * Gets the transaction log writer.
	 * 
	 * @return FileWriter
	 */
	public FileWriter getWriter() {
		return logWriter;
	}
	
	/**
	 * Indicates if the product transaction is open.
	 * 
	 * @return true if open, false if not
	 */
	public boolean isTransactionOpen() {
		return transactionAlreadyOpen;
	}
	
	/**
     * Sets the flag indicating if the product transaction is open.
     * 
     * @param txopen true if open, false if not
     */
	public void setTransactionOpened(final boolean txopen) {
		this.transactionAlreadyOpen = txopen;
	}
	
	/**
	 * Closes the log writer and calls closeExtraFiles.
	 * @throws IOException if there is a problem closing the files
	 */
	public void closeFiles() throws IOException {
		flushFiles();
		
		if (null != logWriter) {
			logWriter.close();
		}		
		
		closeExtraFiles();
	}
	
	/**
	 * Called by closeFiles.  All files added by the mission adaptation should be closed out
	 * in the implementation of this method.
	 * 
	 * @throws IOException if there is a problem closing the files
	 */
	public abstract void closeExtraFiles() throws IOException;
	
	/**
	 * Flushes the log writer and then calls flushExtraFiles.
	 * 
	 * @throws IOException if there is a problem flushing the files
	 */
	public void flushFiles() throws IOException {
		if (null != logWriter) {
			logWriter.flush();
		}
		
		flushExtraFiles();
	}
	
	/**
	 * Called by flushFiles.  This method should ensure that all IO added by the mission 
	 * adaptation is flushed.  
	 * 
	 * @throws IOException if there is a problem flushing the files
	 */
	public abstract void flushExtraFiles() throws IOException;

}