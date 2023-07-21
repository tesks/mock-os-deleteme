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
package jpl.gds.product.impl.builder;

import java.util.LinkedList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.product.api.builder.IProductBuilderService;
import jpl.gds.product.api.builder.IProductMissionAdaptor;
import jpl.gds.product.api.builder.IProductStorage;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.performance.IPerformanceData;
import jpl.gds.shared.thread.SleepUtilities;

/**
 * 
 * ProductBuilder creates and manages the objects needed to generate data
 * products from packet messages for a single virtual channel. It is the top
 * level object responsible for starting the product scheduler, the packet input
 * receiver, and the product writer. It is a downlink service, meaning it is
 * invoked by the downlink application's service management infrastructure.
 * 
 *
 * 3/25/15 Changes throughout to replace the backlog
 *          summary publishing with the provision of performance data to the
 *          PerformanceSummaryPublisher.
 */
public class ProductBuilder implements IProductBuilderService {

    private final Tracer log;


	/*
	 * Removed the product scheduler and added as a variable in
	 * AbstractDiskProductStorage.
	 */
	private final int vcid;
	private ProductBuilderInput input;
	private final IProductMissionAdaptor adaptor;

	/* keep a reference to our disk storage object */
	private IProductStorage productStorage;
	
	private final ApplicationContext appContext;

	/**
	 * Creates an instance of ProductBuilder for processing packets with the given VCID
	 * @param appContext the current application context
	 * @param vcid the virtual channel ID of the packets to process
	 */
	public ProductBuilder(final ApplicationContext appContext, final int vcid) {
        log = TraceManager.getTracer(appContext, Loggers.TLM_PRODUCT);
		this.vcid = vcid;
		adaptor = appContext.getBean(IProductMissionAdaptor.class);
		this.appContext = appContext;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.interfaces.IService#startService()
	 */
	@Override
	public boolean startService() {
		addProductStorage(adaptor);

		input = new ProductBuilderInput(appContext, vcid);
		input.subscribeToPackets(adaptor);
	
		log.debug("Product builder started for vcid " + vcid);

		return true;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public int getVcid() {
		return this.vcid;
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.interfaces.IService#stopService()
	 */
	@Override
	public void stopService() {
		log.debug("Shutting down product builder for vcid " + vcid);
		if (input != null) {
			input.shutdown();
		}
		
		try {
			SleepUtilities.checkedSleep(1000);
		} catch (final Exception e) {
		    // do nothing
		}

		/* Removed a bunch of stuff related to backlog
		 * status publication.
		 */

		if (productStorage != null) {

			productStorage.shutdown();

			while (!productStorage.isDone()) {
				SleepUtilities.checkedSleep(250);
			}

			productStorage.closeSubscriptions();
		}
	}

	/**
	 * Creates a product storage object for this builder. The storage writes
	 * product data to files on a separate thread.
	 * 
	 * @param mission the ProductMissionAdaptor to add product storage to.
	 */
	private void addProductStorage(final IProductMissionAdaptor mission)
	{
		productStorage = appContext.getBean(IProductStorage.class, vcid);
		productStorage.setDirectory(mission.getBaseDirectory());
		productStorage.setMissionAdaptation(mission);
		productStorage.start();
		productStorage.startSubscriptions();
	}


	@Override
    public List<IPerformanceData> getPerformanceData() {
	    /* Fix NPE */
	    if (productStorage != null) {
	        return productStorage.getPerformanceData();
	    } else {
	        return new LinkedList<>();
	    }
	}

}

