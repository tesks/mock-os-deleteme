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


/**
 * ReferenceProductDefinition is the product definition class for the reference mission.
 * The item of chief importance is the product definition key, which is
 * project-specific.
 *
 *
 */
public class ReferenceProductDefinition extends AbstractProductDefinition {

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.impl.dictionary.AbstractProductDefinition#getKey()
     */
    @Override
	public IProductDefinitionKey getKey() {
        if (key == null) {
            key = new ReferenceProductDefinitionKey(getApid(), Integer.parseInt(getVersion()));
        }
        return key;
    }
}
