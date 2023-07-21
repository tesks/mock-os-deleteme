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
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.mapper.IFswToDictionaryMapper;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.processors.PostDownlinkProductProcessingException;
import jpl.gds.product.processors.PostDownlinkProductProcessorOptions;
import jpl.gds.product.processors.descriptions.IPdppDescription;
import jpl.gds.shared.spring.context.SpringContextFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Default implementation of IPdppContextContainerCreator. Used to make new sessions for PDPP processing.
 * MPCS-11847 - removed use of mapper, but leaving the interface the same. This is part of moving away from using a mapper
 * file in the Reference PDPP implementation (MPCS-11779)
 */
@Component
public class ReferencePdppContextContainerCreator extends AbstractPdppContextContainerCreator implements IPdppContextContainerCreator {

    public ReferencePdppContextContainerCreator(ApplicationContext applicationContext){
        super(applicationContext);
    }

    /**
     * Creates a container for a PDPP processing, including product metadata and session to use.
     * The session is either drawn from the session cache, if the cache contains one that meets the criteria, otherwise
     * a new session is created.
     * @param childKey
     * @param md
     * @param description
     * @param options
     * @param mapper
     * @return PdppContextContainer
     */
    public IPdppContextContainer createContextContainer(final String childKey,
                                                       final IProductMetadataProvider md,
                                                       final IPdppDescription description,
                                                       final PostDownlinkProductProcessorOptions options,
                                                       final IFswToDictionaryMapper mapper) throws PostDownlinkProductProcessingException, DatabaseException {

        String fswVersion = options.getOverridingDictionary();
        String fswDictionaryDir = options.getFswDictionaryDirectory();
        StoreIdentifier[] ldiStores = options.getLdiStores();
        boolean useDatabase = options.getUseDatabase();
        boolean useJms = options.getUseJMS();

        // First get the parent, that will fail if it does not exist.
        final IContextConfiguration parent = getParentSessionConfig(md);
        final ApplicationContext childContext = SpringContextFactory.getSpringContext(true);
        ((GenericApplicationContext) childContext).setClassLoader(new URLClassLoader(new URL[0], ClassLoader.getSystemClassLoader()));

        String childFswVersion = fswVersion == null ? md.getEmdDictionaryVersion() : fswVersion;
        String childDictDir = fswDictionaryDir == null ? md.getEmdDictionaryDir() : fswDictionaryDir;

        final DictionaryProperties dictConfig = childContext.getBean(DictionaryProperties.class);
        dictConfig.setFswVersion(childFswVersion);
        dictConfig.setFswDictionaryDir(childDictDir);

        // Get or create the child session.
        final IContextConfiguration child = this.sessionFetcher.getOrCreateChildSession(parent,
                childContext,
                childFswVersion,
                description);

        return new PdppContextContainer(parent,
                childContext,
                child,
                ldiStores,
                useJms,
                useDatabase
        );
    }
}
