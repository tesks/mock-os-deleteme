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

import org.springframework.context.ApplicationContext;

import jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct;

/**
 * Check to see if this needs to be extracted.  This checker must be after all of the other checkers that
 * could result in a new product being created.  There is no way to know that in this class so special care needs 
 * to be taken when setting up the checkers.  
 * 
 * If the parent of this product is not null we will need to extract.  If it is null, it was created by chill down and we will only 
 * extract if the real time extraction property of the product is 0, meaning it was not on.
 * 
 * MPCS-8180 - 07/20/2016 - Imported to and updated for AMPCS M20 adaptation
 */
public class ExtractChecker extends AbstractAutomationChecker {

	/**
	 * @param actionName The action mnemonic 
	 */
	public ExtractChecker(String actionName, ApplicationContext appContext) {
		super(actionName, appContext);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.product.automation.hibernate.checkers.IProductAutomationChecker#isProcessingRequired(jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct, jpl.gds.product.AbstractProductMetadata)
	 */
	@Override
	public boolean isProcessingRequired(ProductAutomationProduct product) {
		if (product.getParent() == null) {
			// This product was created by chill down so just check if the real time extraction flag was set.
			return product.getRealTimeExtraction() == 0;
		} else {
			// This product was a PDPP created product so it needs to be worked on.
			return true;
		}
	}
}
