/*
 * Copyright 2006-2020. California Institute of Technology.
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
package jpl.gds.file;

import jpl.gds.product.api.file.IProductFilename;
import jpl.gds.product.api.file.IProductFilenameBuilder;
import jpl.gds.product.utilities.file.AbstractProductFilenameBuilder;
import jpl.gds.product.utilities.file.ReferenceProductFilename;
import org.springframework.context.ApplicationContext;

/**
 * MPCS-8337 9/26/2016 - Very simple builder implementation for the reference product builder factory.
 * MPCS-11863 - adding a PDPP version to use the specific ReferenceProductFilename constructor
 */
public class PdppReferenceProductFilenameBuilder extends AbstractProductFilenameBuilder implements IProductFilenameBuilder {


    /**
     * Constructor
     * @param appContext the ApplicationContext to get configuration defaults from
     */
    public PdppReferenceProductFilenameBuilder(final ApplicationContext appContext) {
        super(appContext);
    }

    @Override
    public IProductFilename build() {
        return new ReferenceProductFilename(isPartial, productPath, productName, getPathWithNoExtension());
    }
}
