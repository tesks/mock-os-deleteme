/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.product.context;

import jpl.gds.common.spring.context.IContextCache;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.processors.PostDownlinkProductProcessorOptions;
import jpl.gds.product.processors.descriptions.IPdppDescription;
import jpl.gds.product.processors.PostDownlinkProductProcessingException;

/**
 * Interface that defines how to get a context (ie Session) container for a PDPP processor
 */
public interface IPdppContextCache extends IContextCache {

    /**
     * Checks to see if a matching session exists in the cache, otherise creates a new one
     * @param md product metadata to use in the new session
     * @param description object containing strings used to construct the new session and file names
     * @param options additional PDPP options
     * @param startLdiStores
     * @return IPdppContextContainer
     */
    IPdppContextContainer getContextContainer(final IProductMetadataProvider md,
                                          IPdppDescription description,
                                          PostDownlinkProductProcessorOptions options,
                                          final boolean startLdiStores) throws PostDownlinkProductProcessingException;
}
