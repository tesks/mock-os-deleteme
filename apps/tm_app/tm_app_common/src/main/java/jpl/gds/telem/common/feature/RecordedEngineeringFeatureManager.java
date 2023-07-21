/*
 * Copyright 2006-2018. California Institute of Technology.
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
package jpl.gds.telem.common.feature;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.service.telem.AbstractTelemetryFeatureManager;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager; 

/**
 * RecordedEngineeringFeatureManager manages the objects required to launch the a
 * recorded engineering processor process.
 *
 *
 */
public class RecordedEngineeringFeatureManager extends AbstractTelemetryFeatureManager {

	/**
	 * {@inheritDoc}
	 */
	@Override
    public boolean init(final ApplicationContext springContext) {
		setValid(false);
		if (!this.isEnabled()) {
			return true;
		}
        log = TraceManager.getTracer(springContext, Loggers.DOWNLINK);

		// MP: RecordedEngineeringLauncher::startService() already checks for enabled
		// message service, but returns false.  Decided to not add it to the Feature list to begin with.
      
		if ( springContext.getBean(MessageServiceConfiguration.class).getUseMessaging() && 
                springContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class).getUseDatabase())
		{
		    addService(new RecordedEngineeringLauncher(springContext));
		}

		else // it's supposed to come up but the message service & DB options are not correct for this process
		{
		    log.warn("Recorded Telemetry Processing (RecordedEngineeringLauncher) will not be started due to cmd line parameters");
		}

        
        final boolean ok = startAllServices();

        setValid(ok);

		if (ok)
        {
			log.debug("Recorded engineering feature successfully initialized");
		}

		return ok;
	}


}
