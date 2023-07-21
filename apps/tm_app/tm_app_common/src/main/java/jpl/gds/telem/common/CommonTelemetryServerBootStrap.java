/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.telem.common;

import jpl.gds.common.config.service.ServiceConfiguration;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.shared.util.HostPortUtility;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *  Spring bootstrap configuration class for Down, TI, and TP apps ONLY.
 *  Do not place this class into any Spring bootstrap package to be automatically
 *  loaded. This bootstrap should be explicitly loaded by the chill_telem_ingest
 *  application only. Loading it automatically may result in the wrong
 *  perspective properties bean getting returned from the application context.
 *
 */
@Configuration
public class CommonTelemetryServerBootStrap {
    private static final String  SERVICE_CONFIGURATION         = "SERVICE_CONFIGURATION";

    @Bean(SERVICE_CONFIGURATION)
    protected ServiceConfiguration getServiceConfig(IMySqlAdaptationProperties db,
                                                    MessageServiceConfiguration messageConfig) {
        final ServiceConfiguration sc = new ServiceConfiguration();

        ServiceConfiguration.ServiceParams sp = new ServiceConfiguration.ServiceParams(
                ServiceConfiguration.ServiceType.LOMS, db.getUseDatabase(),
                HostPortUtility.cleanHostName(db.getHost()), db.getPort(),
                db.getDatabaseName());
        sc.addService(sp);

        sp = new ServiceConfiguration.ServiceParams(ServiceConfiguration.ServiceType.JMS,
                                                    messageConfig.getUseMessaging(),
                                                    HostPortUtility.cleanHostName(messageConfig.getMessageServerHost()),
                                                    messageConfig.getMessageServerPort(), null);
        sc.addService(sp);

        final GlobalLadProperties glad = GlobalLadProperties.getGlobalInstance();

        /* Should supply rest port to clients, not socket port */
        sp = new ServiceConfiguration.ServiceParams(ServiceConfiguration.ServiceType.GLAD, glad.isEnabled(),
                                                    HostPortUtility.cleanHostName(glad.getServerHost()),
                                                    glad.getRestPort(), null);
        sc.addService(sp);

        return sc;
    }
}
