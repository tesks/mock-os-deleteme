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

import jpl.gds.db.api.DatabaseException;
import jpl.gds.dictionary.api.mapper.IFswToDictionaryMapper;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.processors.PostDownlinkProductProcessingException;
import jpl.gds.product.processors.PostDownlinkProductProcessorOptions;
import jpl.gds.product.processors.descriptions.IPdppDescription;

/**
 * Interface used to make new sessions for PDPP processing.
 * The default implementation is ReferencePdppContextContainerCreator, but if a mission has specific logic
 * for a given processor type, the mission can create its own implementation of this interface.
 */

public interface IPdppContextContainerCreator {

    /**
     * Creates a new context container (session) for PDPP processing
     * @param childKey
     * @param md
     * @param description
     * @param options
     * @param mapper
     * @return IPdppContextContainer
     */
    IPdppContextContainer createContextContainer(final String childKey,
                                                       final IProductMetadataProvider md,
                                                       final IPdppDescription description,
                                                       final PostDownlinkProductProcessorOptions options,
                                                       final IFswToDictionaryMapper mapper) throws PostDownlinkProductProcessingException, DatabaseException;
}
