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

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.dictionary.api.mapper.IFswToDictMapping;
import jpl.gds.dictionary.api.mapper.IFswToDictionaryMapper;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.processors.PostDownlinkProductProcessingException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Common tools for container creation
 */
@Component
public class AbstractPdppContextContainerCreator {

    protected ApplicationContext appContext;

    protected IPdppSessionFetcher sessionFetcher;

    /** Constructor
     * @param appContext ApplicationContext
     * */
    public AbstractPdppContextContainerCreator(ApplicationContext appContext) {
        this.appContext = appContext;
        this.sessionFetcher = appContext.getBean(IPdppSessionFetcher.class);
    }

    /**
     * @param md IProductMetadataProvider
     * @return IContextConfiguration
     * @throws PostDownlinkProductProcessingException
     */
    protected IContextConfiguration getParentSessionConfig(final IProductMetadataProvider md) throws PostDownlinkProductProcessingException {
        try {
            final IContextConfiguration pc = sessionFetcher.getSession(md);

            if (pc == null) {
                throw new IllegalStateException("Parent session does not exist");
            } else {
                return pc;
            }
        } catch (final Exception e) {
            throw new PostDownlinkProductProcessingException("Failed to get context container", e);
        }
    }

    /**
     * @param fswBuildId
     * @param fswVersion
     * @param fswDictionaryDirectory
     * @return
     */
    protected IFswToDictMapping getAndOrAddMapperEntry(final IFswToDictionaryMapper mapper, final Long fswBuildId, final String fswVersion, final String fswDictionaryDirectory) {

        // Check if these are already mapped and good to go.
        if (!mapper.isSameFlightDictionary(fswBuildId, fswVersion)) {
            // They are not the same, so an entry must be made.
            mapper.addFswToDictMapping(fswBuildId, fswVersion, fswVersion, null, fswDictionaryDirectory, null, null, 127);
        }

        final IFswToDictMapping entry = mapper.getDictionary(fswBuildId);
        return entry;
    }
}
