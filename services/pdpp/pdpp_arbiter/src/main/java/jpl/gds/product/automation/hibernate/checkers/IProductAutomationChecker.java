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
package jpl.gds.product.automation.hibernate.checkers;

import jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct;

/**
 * A checker that would be tied to a specific PDPP type and would take in a
 * product and check to see if that product should be handled by the paired PDPP
 * process.
 * 
 * MPCS-8180 - 07/20/2016 - Imported to and updated for AMPCS M20
 *          adaptation
 */
public interface IProductAutomationChecker {
	/**
	 * Do whatever checks are required to figure out if the product should be
	 * processed by the action this represents.
	 * 
	 * @param product
	 *            The product automation product to be tested.
	 * 
	 * @return true if the product requires processing by the action
	 *         implementing this interface, false otherwise.
	 */
	public boolean isProcessingRequired(ProductAutomationProduct product);
}
