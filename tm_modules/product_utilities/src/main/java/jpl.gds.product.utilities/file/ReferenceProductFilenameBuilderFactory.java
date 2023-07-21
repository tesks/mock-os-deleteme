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
package jpl.gds.product.utilities.file;

import jpl.gds.product.api.ProductApiBeans;
import org.springframework.context.ApplicationContext;

import jpl.gds.product.api.file.IProductFilenameBuilder;
import jpl.gds.product.api.file.IProductFilenameBuilderFactory;

/**
 * 
 * Product filename builder factory for the reference product builder.
 * 
 *
 * @since R8
 */
public class ReferenceProductFilenameBuilderFactory implements IProductFilenameBuilderFactory {

    private final ApplicationContext appContext;

    /**
     * Constructor.
     * 
     * @param appContext the current application context
     */
    public ReferenceProductFilenameBuilderFactory(final ApplicationContext appContext) {
        this.appContext = appContext;
    }

    @Override
    public IProductFilenameBuilder createBuilder() {
        return new ReferenceProductFilenameBuilder(appContext);
    }

}
