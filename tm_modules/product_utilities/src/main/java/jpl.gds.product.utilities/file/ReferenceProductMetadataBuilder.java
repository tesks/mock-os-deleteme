/*
 * Copyright 2006-2020. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */
package jpl.gds.product.utilities.file;

import jpl.gds.product.api.file.IProductMetadata;
import jpl.gds.product.api.file.IProductMetadataBuilder;
import org.springframework.context.ApplicationContext;

/**
 * Reference implementation of a Metadata Builder, intended for use by PDPP to populate the mission-specific metadata class
 */
public class ReferenceProductMetadataBuilder implements IProductMetadataBuilder {

    private ApplicationContext appContext;

    /**
     * Constructor
     * @param appContext the ApplicationContext to get configuration defaults from
     */
    public ReferenceProductMetadataBuilder(final ApplicationContext appContext) {

        this.appContext = appContext;
    }


    /**
     * Builds a new metadata object.
     * @return reference product metadata instance
     */
    @Override
    public IProductMetadata build() {
        return new ReferenceProductMetadata(appContext);
    }
}
