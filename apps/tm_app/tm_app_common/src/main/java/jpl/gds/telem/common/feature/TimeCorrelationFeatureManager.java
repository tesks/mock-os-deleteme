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

import jpl.gds.common.service.telem.AbstractTelemetryFeatureManager;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinitionProvider;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.time.api.service.ITimeCorrelationService;

/**
 * TimeCorrelationFeatureManager handles everything required to initialize and shutdown the
 * time correlation capability in the downlink process.
 * 
 */
public class TimeCorrelationFeatureManager extends AbstractTelemetryFeatureManager 
{

	/**
	 * {@inheritDoc}
	 */
	@Override
    public boolean init(final ApplicationContext springContext) {
        log = TraceManager.getTracer(springContext, Loggers.DOWNLINK);
        setValid(false);
		if (!this.isEnabled()) {
			return true;
		}
	    
		setValid(true);
		
        try {
            springContext.getBean(ITransferFrameDefinitionProvider.class);
        } catch (final Exception e1) {
            log.error("Error loading transfer frame dictionary for time correlation service: " + e1.getMessage());
            setValid(false);
            return false;
        }
		
		try
		{
			addService(springContext.getBean(ITimeCorrelationService.class));
		}
		catch(final Exception e)
		{
			log.fatal("Time correlation adapter configuration error: " + e.getMessage());
			setValid(false);
			return false;
		}
		
	    setValid(startAllServices());
		
		if (this.isValid()) {
			log.debug("Time Correlation feature successfully initialized");
		}
		
		return isValid();
	}

	/**
	 * Gets the time correlation service object.
	 * 
	 * @return service object, or null if not initialized
	 */
	public ITimeCorrelationService getTimeCorrelator() {
		return (ITimeCorrelationService)getService(ITimeCorrelationService.class);
	}
}
