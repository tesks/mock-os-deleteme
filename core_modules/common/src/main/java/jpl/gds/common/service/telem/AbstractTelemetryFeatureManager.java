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
package jpl.gds.common.service.telem;

import jpl.gds.shared.interfaces.IService;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.sys.SystemUtilities;

import java.util.ArrayList;
import java.util.List;

/**
 * AbstractTelemetryFeatureManager is the base class for telemetry processing features, 
 * providing common methods for the ITelemetryFeatureManager interface.
 * 
 *
 */
public abstract class AbstractTelemetryFeatureManager implements ITelemetryFeatureManager {
    /**
     * Trace logger to share with subclasses.
     */
	protected Tracer log;

	private boolean isFeatureValid;	
	private boolean isFeatureEnabled = true;
	
	/**
	 * List of Service instances attached to this feature manager.
	 */
	protected final List<IService> services = new ArrayList<>();

	/**
	 * {@inheritDoc}
	 */
	@Override
    public void enable(final boolean isEnabled) {
		this.isFeatureEnabled = isEnabled;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
    public synchronized void addService(final IService service) {
		services.add(service);
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean startAllServices() {
		boolean ok = true;

        for (final IService service : services)
        {
            if (! service.startService())
            {
                ok = false;

                /**
                 * This is somewhat weird but the caller does not necessarily
                 * care if there is a failure to start, and thus putting out
                 * an error or warning may fail a smoke test.
                 */
                TraceManager.getDefaultTracer().debug("Unable to start service ", service.getClass().getName());
                
            }
		}

		return ok;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
    public synchronized void stopAllServices() {
		for (final IService service: services) {
		    log.debug("Shutting down service: " + service);
			service.stopService();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public synchronized void clearAllServices() {
		services.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public synchronized IService getService(final Class<?> c) {
		for (final IService service: services) {
			if (c.isAssignableFrom(service.getClass())) {
				return service;
			}
		}
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
    public void shutdown() {
		if (!isFeatureEnabled) {
			return;
		}
		stopAllServices();
		clearAllServices();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public boolean isValid() {
		return isFeatureValid;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
    public void setValid(final boolean isValid) {
	    this.isFeatureValid = isValid;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
    public boolean isEnabled() {
		return isFeatureEnabled;
	}
	
	@Override
    public void populateSummary(final ITelemetrySummary summary) {
	    SystemUtilities.doNothing();
	}
	    
	
}
