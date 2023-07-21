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

import jpl.gds.common.service.telem.*;
import jpl.gds.product.api.IProductTrackingService;
import jpl.gds.product.api.builder.IProductBuilderManager;
import jpl.gds.product.api.builder.IProductBuilderService;
import jpl.gds.product.api.builder.IProductMissionAdaptor;
import jpl.gds.product.api.config.IProductPropertiesProvider;
import jpl.gds.shared.interfaces.IService;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import org.springframework.context.ApplicationContext;

import java.io.File;

/**
 * ProductGeneratorFeatureManager manages the objects required for building data products.
 *
 *
 */
public class ProductGeneratorFeatureManager extends AbstractTelemetryFeatureManager {

	private IProductMissionAdaptor adaptor;
	private IProductTrackingService meter;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
    public boolean init(final ApplicationContext springContext) {
        log = TraceManager.getTracer(springContext, Loggers.TLM_PRODUCT);
		setValid(false);
		if (!this.isEnabled()) {
			return true;
		}

		setValid(true);
		
		//Define the mission adaptation and initialize the product builder configuration
		setValid(createMissionAdaptation(springContext));
		if (!this.isValid()) {
			return false;
		}

		final IProductPropertiesProvider productConfig = springContext.getBean(IProductPropertiesProvider.class);
		adaptor.setBaseDirectory(new File(productConfig.getStorageDir()));

        final int[] vcids = productConfig.getSupportedVcids();
        
        // Setup the product builders
        // all product builder manager additions are in the spring bootstrap for products.
        
        /* Moved add of the manager service to
         * after the product builder services so the manager will be
         * shut down AFTER the product builders.
         */

        for (int i= 0; i < vcids.length; i++) {
        	final IProductBuilderService builder = springContext.getBean(IProductBuilderService.class, vcids[i]);
        	addService(builder);
        }
        addService(springContext.getBean(IProductBuilderManager.class));

        meter = springContext.getBean(IProductTrackingService.class);
        addService(meter);
        
        final boolean ok = startAllServices();

        setValid(ok);

		if (ok)
        {
			log.debug("Product Generation feature successfully initialized");
		}

		return isValid();
	}


	/**
     * Creates a MissionAdaptation matching the current mission configuration.
	 */
	private boolean createMissionAdaptation(final ApplicationContext springContext)
	{
	    try {
            adaptor = springContext.getBean(IProductMissionAdaptor.class);
            /**
             * Calling the init method.  The MSL adaptor
             * uses it to initialize all of the dictionary code that was moved out of the 
             * constructor.
             */
            adaptor.init();
	    } catch (final Exception e) {
	        e.printStackTrace();
            log.error("Unable to create product adaptation: " + e.toString());
	        return false;
	    } 
	    return true;
	}
	
	/**
	 * Gets a product builder service instance.
	 * 
	 * @param vcid the virtual channel ID of the product builder service to get
	 * @return product builder service instance, or null if none defined
	 */
	public IProductBuilderService getProductBuilder(final int vcid)
	{
		for (final IService service: services) {
		    if (!(service instanceof IProductBuilderService)) {
		        continue;
		    }
			final IProductBuilderService pe = (IProductBuilderService)service;
			if (pe.getVcid() == vcid) {
				return pe;
			}
		}
		return null;
	}
	
	/**
	 * Gets the product tracking service object.
	 * 
	 * @return service object, or null if none defined
	 */
	public IProductTrackingService getProductTrackingService() {
	    return this.meter;
	}
	
	 
    @Override
    public void populateSummary(final ITelemetrySummary summary) {
        if (summary != null && meter != null) {
            if (summary instanceof IDownlinkSummary) {
                final DownlinkSummary sum = (DownlinkSummary) summary;

                sum.setPartialProducts(meter.getPartialProductCount());
                sum.setProductDataBytes(meter.getDataByteCount());
                sum.setProducts(meter.getProductCount());
            }
            else if (summary instanceof ITelemetryProcessorSummary) {
                final TelemetryProcessorSummary sum = (TelemetryProcessorSummary) summary;

                sum.setPartialProducts(meter.getPartialProductCount());
                sum.setProductDataBytes(meter.getDataByteCount());
                sum.setProducts(meter.getProductCount());
            }

        }
    }
}
