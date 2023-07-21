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

import jpl.gds.dictionary.api.mapper.IFswToDictionaryMapper;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct;

/**
 * Checker to tell if the product needs to have the dictionary corrected.  This is MSL specific.  Checks to see if the given
 * product's dictionary version is equal to the unmacthed dict version (R0).
 * 
 * MPCS-6758  10/2014 - The dict version will never be an R0 so must check if the the version id and dict string match.
 * 
 * @version - MPCS-8180 - 07/20/2016 - Imported to and updated for AMPCS M20 adaptation
 */
public class CorrectChecker extends AbstractAutomationChecker {

	private IFswToDictionaryMapper mapper;

	/**
	 * @param actionName
	 */
	public CorrectChecker(String actionName, ApplicationContext appContext) {
		super(actionName, appContext);
		this.mapper = appContext.getBean(IFswToDictionaryMapper.class);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.product.automation.hibernate.checkers.IProductAutomationChecker#isProcessingRequired(jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct, jpl.gds.product.AbstractProductMetadata)
	 */
	@Override
	public boolean isProcessingRequired(ProductAutomationProduct product) {
		/**
		 * MPCS-7333 -  5/2015 - Chill down will set the dictionary version to R0 for mismatches.  We only 
		 * need to check if the string is set to that version.
		 */
		return mapper.getUnmatchedDictionaryVersion().equals(product.getDictVersion());
	}
}
