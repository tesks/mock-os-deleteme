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
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.ApplicationContext;

import jpl.gds.product.api.builder.IProductBuilderManager;
import jpl.gds.product.api.builder.IProductBuilderService;
import jpl.gds.shared.performance.IPerformanceData;
import jpl.gds.shared.performance.PerformanceSummaryPublisher;

/**
 * This class is a Downlink Service that is a container for all ProductBuilder
 * service instances. It exists so that one performance data provider can be
 * established for all Product Builders.
 * 
 */
public class ProductBuilderManager implements IProductBuilderManager {

	/** Performance provider name */
	private static final String THIS_PROVIDER = "Product Builders";

	/** Product Builder instances. */
	private final CopyOnWriteArrayList<IProductBuilderService> builders = new CopyOnWriteArrayList<IProductBuilderService>();

	private final ApplicationContext appContext;
	
	/**
	 * Constructor.
	 * 
	 * @param context the current application context.
	 */
	public ProductBuilderManager(final ApplicationContext context) {
		appContext = context;
		
	}
	/**
     * {@inheritDoc}
     */
	@Override
    public void addProductBuilder(final IProductBuilderService builder) {
		assert builder != null : "product builder cannot be null";
		builders.add(builder);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.performance.IPerformanceProvider#getPerformanceData()
	 */
	@Override
	public List<IPerformanceData> getPerformanceData() {
		final List<IPerformanceData> perfList = new LinkedList<IPerformanceData>();
		for (final IProductBuilderService b : builders) {
			perfList.addAll(b.getPerformanceData());
		}
		return perfList;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.performance.IPerformanceProvider#getProviderName()
	 */
	@Override
	public String getProviderName() {
		return THIS_PROVIDER;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.interfaces.IService#startService()
	 */
	@Override
	public boolean startService() {
		/**
		 * Changed to use the session based publisher.
		 */
		appContext.getBean(PerformanceSummaryPublisher.class).registerProvider(this);
		return true;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.interfaces.IService#stopService()
	 */
	@Override
	public void stopService() {
		/**
		 * Changed to use the session based publisher.
		 */
		appContext.getBean(PerformanceSummaryPublisher.class).deregisterProvider(this);
		builders.clear();

	}

}
