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
package jpl.gds.globallad.feature;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.service.telem.AbstractTelemetryFeatureManager;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.service.GlobalLadDownlinkService;
import jpl.gds.globallad.service.IGlobalLadDownlinkService;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;

/**
 * Feature responsible for loading data into the global lad.
 */
public class GlobalLadFeature extends AbstractTelemetryFeatureManager implements IGlobalLadFeatureManager {

	@Override
    public boolean init(final ApplicationContext springContext) {
        log = TraceManager.getTracer(springContext, Loggers.DOWNLINK);
		setValid(false);
		
		/* R8 Refactor TODO - should the global LAD config be in the context? */
		if (!this.isEnabled() || !GlobalLadProperties.getGlobalInstance().isEnabled()) {
		    setValid(true);
			return true;
		}
		
		addService(new GlobalLadDownlinkService(springContext));
		setValid(startAllServices());
		
		if (isValid()) {
			log.debug("Global LAD feature successfully initialized");
		}
		
		return isValid();
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    public IGlobalLadDownlinkService getGlobalLadService() {
		return (IGlobalLadDownlinkService)getService(GlobalLadDownlinkService.class);
	}
}
