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
package jpl.gds.cfdp.processor.ampcs.product;

import cfdp.engine.ampcs.ICfdpAmpcsProductPlugin;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.CfdpProcessorSpringConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Class CfdpAmpcsProductPluginHolder
 *
 */
@Service
public class CfdpAmpcsProductPluginHolder {

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private ConfigurationManager configurationManager;

    private ICfdpAmpcsProductPlugin plugin;

    @PostConstruct
    public void init() {
        plugin = configurationManager.isAmpcsProductPluginEnabled() ?
                appContext.getBean(CfdpProcessorSpringConfiguration.CFDP_AMPCS_PRODUCT_PLUGIN, ICfdpAmpcsProductPlugin.class)
                : appContext.getBean(CfdpProcessorSpringConfiguration.NO_OP_CFDP_AMPCS_PRODUCT_PLUGIN, ICfdpAmpcsProductPlugin.class);
    }

    public ICfdpAmpcsProductPlugin getPlugin() {
        return plugin;
    }

}