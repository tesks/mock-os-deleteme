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
package jpl.gds.product.api.decom;

import java.io.PrintStream;
import java.util.List;

import jpl.gds.product.api.decom.formatter.IProductDecomOutputFormatter;
import jpl.gds.shared.message.IMessagePublicationBus;

/**
 * A writer interface to be implemented by product decommutation classes.
 * 
 *
 */
public interface IProductDecomUpdater extends IProductDecom {

	/**
	 * Sets the result status. 
	 * @param status completion status: must be SUCCESS, FAILURE, NO_PROD_VIEWER, NO_DPO_VIEWER
	 */
	void setReturnCode(int status);

	/**
	 * Sets the flag indicating whether text output is suppressed.
	 * @param suppress true if text output is suppressed
	 */
	void setSupressText(boolean suppress);

	/**
	 * Overrides the dictionary directory in the product EMD file.
	 * @param dictDirOverride the directory to set
	 */
	void setDictDirOverride(String dictDirOverride);

	/**
	 * Overrides the dictionary version in the product EMD file.
	 * @param dictVersionOverride the version to set
	 */
	void setDictVersionOverride(String dictVersionOverride);

	/**
	 * Sets the Output Formatter for formatting product decom output.
	 * @param of the Output Formatter to set
	 */
	void setOutputFormatter(IProductDecomOutputFormatter of);

	/**
	 * Sets the stream for product decom output. This is the stream that will be
	 * used by the output formatter.
	 * @param printStream the PrintStream to set
	 */
	void setPrintStream(PrintStream printStream);

	/**
	 * Sets the message context for subscription purposes.
	 * @param messageContext the MessageContext to set
	 */
	void setMessageContext(IMessagePublicationBus messageContext);

	/**
	 * Sets the flag indicating whether to process products with an invalid checksum
	 * @param check true to ignore checksum
	 */
	void setIgnoreChecksum(boolean check);

	/**
	 * Sets the flag indicating whether to launch product viewers.
	 * @param enable true to launch product viewers
	 */
	void setShowProductViewer(boolean enable);

	/**
	 * Sets the flag indicating whether to launch data product object (DPO) viewers.
	 * @param enable true to launch DPO viewers
	 */
	void setShowDpoViewer(boolean enable);

	/**
	 * Sets the flag indicating whether to output detailed messages about external viewers
	 * as they are launched.
	 * 
	 * @param enable true to enable detailed output
	 */
	void setShowLaunchInfo(boolean enable);

	/**
	 * Sets a specific list of DPOs to be viewed.
	 * @param dpos list of DPO VIDs or Names
	 */
	void setDpoList(List<String> dpos);

    /**
     * Causes this object to subscribe to "product arrived" messages.
     * Product arrival messages are generated by StoredProductInput
     * as it reads products from product storage, where they were written
     * by the product generator.
     *
     */
	void subscribeToArrivedProducts();

    /**
     * Causes this object to subscribe to "product assembled" messages.
     * Product assembled messages are generated by the product generator
     * as it writes complete products to disk.
     */
	void subscribeToAssembledProducts();

    /**
     * Causes this object to subscribe to "partial product" messages.
     * Partial product messages are generated by the product generator
     * as it writes partial products to disk.
     */
	void subscribeToPartialProducts();

}