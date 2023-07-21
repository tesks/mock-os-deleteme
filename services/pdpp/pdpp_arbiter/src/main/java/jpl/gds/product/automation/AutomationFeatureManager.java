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
package jpl.gds.product.automation;

import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.service.telem.AbstractTelemetryFeatureManager;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.product.PdppApiBeans;
import jpl.gds.shared.interfaces.IService;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

/**
 * This class creates and makes available the AutomationDownlinkService
 *
 * 06/15/16 - MPCS-8179 - Imported from MPCS for MSL. Updated to work
 *          in current dependency structure. Service is instantiated through Reflection
 */
public class AutomationFeatureManager extends AbstractTelemetryFeatureManager {

	@Override
	public boolean init(final ApplicationContext springContext) {
		setValid(false);

        log = TraceManager.getTracer(springContext, Loggers.PDPP);

		/**
		 *  - 11/14/2012 - MPCS-4358 - If this is SSE, don't do anything just like if not enabled.
		 * 
		 * MPCS-6798 -  - Check to see if the session ID is null which means this is a no database run.  In that case do 
		 * not init just return true that we have init'd.
		 * 
		 */

		if (!isEnabled() || // Automation disabled
                !springContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class).getUseDatabase() || // No database run
                springContext.getBean(SseContextFlag.class).isApplicationSse()) { // Is SSE chill down.

            log.debug("AutomationFeatureManager feature was not initialized because it is not needed.");

			return true;
		}

		IService servInstance = null;
		try { 
            servInstance = springContext.getBean(IAutomationDownlinkService.class);
		} catch(final BeansException e) { 
            log.warn("Couldn't load " + PdppApiBeans.AUTOMATION_DOWNLINK_SERVICE, e.getCause());
		}

		// Check that the AutomationDownlinkService is not null before adding it 
		if (servInstance != null) {
			addService(servInstance);
			setValid(startAllServices());
			}


		if (isValid()) {
			log.debug("AutomationFeatureManager feature successfully initialized");
		}

		return isValid();
	}
}
