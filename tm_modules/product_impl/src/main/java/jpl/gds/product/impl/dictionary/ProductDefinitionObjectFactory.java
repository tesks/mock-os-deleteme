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
package jpl.gds.product.impl.dictionary;

import jpl.gds.product.api.dictionary.IProductDefinitionKey;
import jpl.gds.product.api.dictionary.IProductDefinitionObjectFactory;
import jpl.gds.product.api.dictionary.IProductObjectDefinition;

/**
 * ProductDefinitionObjectFactory is used to create mission-specific instances of
 * classes that are used in product definitions.
 */
public class ProductDefinitionObjectFactory implements IProductDefinitionObjectFactory {

	/**
     * {@inheritDoc}
     */
	@Override
    public IProductDefinitionKey createDefinitionKey(final int type, final int version) {
	    return new ReferenceProductDefinitionKey(type, version);
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public IProductObjectDefinition createProductObjectDefinition() {
        return new ProductObjectDefinition();
    }
}
