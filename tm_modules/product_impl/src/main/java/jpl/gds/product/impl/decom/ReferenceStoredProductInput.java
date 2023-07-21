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
package jpl.gds.product.impl.decom;


import java.io.IOException;

import org.springframework.context.ApplicationContext;

import jpl.gds.product.api.IProductMetadataUpdater;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.builder.IProductBuilderObjectFactory;
import jpl.gds.product.api.decom.IStoredProductInput;

/**
 *  This component reads a generic product object that has been stored by the
 *  product generator and publishes a "product arrived" message.
 *  
 */
public class ReferenceStoredProductInput extends StoredProductInput {
     
    /**
     * Creates an instance of ReferenceStoredProductInput.
     * 
     * @param appContext the current application context
     */
    public ReferenceStoredProductInput(final ApplicationContext appContext) {
		super(appContext);
	}


	/**
     * {@inheritDoc}
     * @see jpl.gds.product.impl.decom.StoredProductInput#read(java.lang.String)
     */
    @Override
    public void read(final String filename) throws IOException, ProductException {
        final String[] filenames = IStoredProductInput.getFilenames(filename);
        checkFiles(filenames);
        final String dataFilename = filenames[0];
        final String metadataFilename = filenames[1];

        final IProductMetadataUpdater metadata = appContext.getBean(IProductBuilderObjectFactory.class).createMetadataUpdater();
        metadata.loadFile(metadataFilename);

        publishProduct(dataFilename, metadata);
    }
}
