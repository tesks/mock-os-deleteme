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
package jpl.gds.cfdp.processor;

import cfdp.engine.ampcs.ICfdpAmpcsProductPlugin;
import jpl.gds.cfdp.processor.ampcs.product.CfdpAmpcsProductPlugin;
import jpl.gds.cfdp.processor.ampcs.product.CfdpMultimissionDvtExtractor;
import jpl.gds.cfdp.processor.ampcs.product.ICfdpDvtExtractor;
import jpl.gds.cfdp.processor.ampcs.product.NoOpCfdpAmpcsProductPlugin;
import jpl.gds.common.error.ErrorCode;
import jpl.gds.security.spring.bootstrap.SecuritySpringBootstrap;
import jpl.gds.shared.config.GdsSpringSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySources;

// MPCS-9865 - 6/22/2018 - Removed this configuration class from spring.bootstrap project so that other AMPCS
// projects don't error out trying to look for JavaCFDP's ICfdpAmpcsProductPlugin.

/**
 * Spring configuration class for beans in the CFDP Processor project.
 *
 * @since R8
 */
@Configuration
public class CfdpProcessorSpringConfiguration {
    @Autowired
    private             ApplicationContext appContext;

    @Autowired
    private ConfigurableEnvironment env;

    /** The name of the Downlink Spring Properties file */
    public static final String CFDP_SPRING_PROPERTIES_FILENAME = "cfdp_processor_spring.properties";

    /**
     * Name of Downlink Spring Property Sources
     */
    public static final String CFDP_SPRING_PROPERTY_SOURCES    = "CFDP_SPRING_PROPERTY_SOURCES";


    /**
     * Bean name for the CfdpAmpcsProductPlugin bean.
     */
    public static final String CFDP_AMPCS_PRODUCT_PLUGIN = "CFDP_AMPCS_PRODUCT_PLUGIN";

    /**
     * Bean name for the NoOpCfdpAmpcsProductPlugin bean.
     */
    public static final String NO_OP_CFDP_AMPCS_PRODUCT_PLUGIN = "NO_OP_CFDP_AMPCS_PRODUCT_PLUGIN";

    /**
     * Bean name for the ICfdpDvtExtractor bean.
     */
    public static final String CFDP_DVT_EXTRACTOR = "CFDP_DVT_EXTRACTOR";

    private static final String     INITIALIZATION_ERROR_PREAMBLE       = "\n*** Initialization Error: ";

    @Bean(name = CFDP_AMPCS_PRODUCT_PLUGIN)
    @Scope("singleton")
    @Lazy(value = true)
    @Autowired
    public ICfdpAmpcsProductPlugin getCfdpAmpcsProductPlugin() {
        return new CfdpAmpcsProductPlugin();
    }

    @Bean(name = NO_OP_CFDP_AMPCS_PRODUCT_PLUGIN)
    @Scope("singleton")
    @Lazy(value = true)
    @Autowired
    public ICfdpAmpcsProductPlugin getNoOpCfdpAmpcsProductPlugin() {
        return new NoOpCfdpAmpcsProductPlugin();
    }

    @Bean(name = CFDP_DVT_EXTRACTOR)
    @Scope("singleton")
    @Lazy(value = true)
    @Autowired
    public ICfdpDvtExtractor getCfdpDvtExtractor() {
        return new CfdpMultimissionDvtExtractor();
    }

    @Bean(name = CFDP_SPRING_PROPERTY_SOURCES)
    @Scope("singleton")
    @DependsOn({ SecuritySpringBootstrap.DEFAULT_SSL_PROPERTY_SOURCES })
    @Autowired
    @Primary
    public PropertySources loadAMPCSPropertySource() {
        try {
            return GdsSpringSystemProperties.loadAMPCSProperySources(env,
                                                                     TraceManager.getTracer(appContext,
                                                                                            Loggers.CFDP),
                                                                     CFDP_SPRING_PROPERTIES_FILENAME,
                                                                     appContext.getBean(SseContextFlag.class));
        }
        catch (final Exception e) {
            System.err.println(INITIALIZATION_ERROR_PREAMBLE + ExceptionTools.getMessage(e));
            System.exit(ErrorCode.UNKNOWN_ERROR_CODE.getNumber());
            return null;
        }
    }

}