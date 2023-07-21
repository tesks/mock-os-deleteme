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

import jpl.gds.product.api.file.IProductFilenameBuilder;
import jpl.gds.product.api.file.IProductFilenameBuilderFactory;
import org.springframework.context.ApplicationContext;

/**
 * MPCS-8337 9/26/2016 - Very simple builder implementation for the reference product builder factory.
 * MPCS-11863 - adding a PDPP version to use the ReferenceProductFilenameBuilder for PDPP
 */
public class PdppReferenceProductFilenameBuilderFactory implements IProductFilenameBuilderFactory {

    private final ApplicationContext appContext;

    /**
     * Constructor.
     *
     * @param appContext the current application context
     */
    public PdppReferenceProductFilenameBuilderFactory(final ApplicationContext appContext) {
        this.appContext = appContext;
    }

    @Override
    public IProductFilenameBuilder createBuilder() {
        return new PdppReferenceProductFilenameBuilder(appContext);
    }

}
