/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.product.automation.checkers;

import jpl.gds.product.automation.hibernate.checkers.AbstractAutomationChecker;
import jpl.gds.product.automation.hibernate.checkers.IProductAutomationChecker;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct;
import org.springframework.context.ApplicationContext;

/**
 * Simple dummy checker to use as a reference
 */
public class ConsoleLoggerChecker extends AbstractAutomationChecker implements IProductAutomationChecker {

    /**
     * @param actionName the action mnemonic for logging products
     * @param appContext
     */
    public ConsoleLoggerChecker(String actionName, ApplicationContext appContext) {
        super(actionName, appContext);
    }

    /**
     * Assesses whether PDPP should process this product. In the simple case, the answer is always yes!
     * @param product the action mnemonic for logging products
     */
    @Override
    public boolean isProcessingRequired(ProductAutomationProduct product) {
        return true;
    }
}