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
/**
 * 
 */
package jpl.gds.globallad.spring.bootstrap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.globallad.data.factory.GenericGlobalLadDataFactory;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory;
import jpl.gds.globallad.feature.GlobalLadFeature;
import jpl.gds.globallad.feature.IGlobalLadFeatureManager;
import jpl.gds.globallad.utilities.CoreGlobalLadQuery;
import jpl.gds.globallad.utilities.ICoreGlobalLadQuery;
import jpl.gds.globallad.utilities.LadChannelValueConverter;
import jpl.gds.globallad.utilities.LadConsumer;

/**
 * Used for the core global lad connector class that gets data from the glad.
 */
@Configuration
public class GlobalLadConnectorSpringBootstrap {
	public static final String CORE_GLOBAL_LAD_QUERY = "CORE_GLOBAL_LAD_QUERY";
	public static final String GLOBAL_LAD_CONVERTER = "GLOBAL_LAD_CONVERTER";
	public static final String GLOBAL_LAD_DATA_FACTORY = "GLOBAL_LAD_DATA_FACTORY";
	public static final String GLOBAL_LAD_CONSUMER = "GLOBAL_LAD_CONSUMER";
    public static final String GLOBAL_LAD_FEATURE = "GLOBAL_LAD_FEATURE";
	
	@Autowired
	ApplicationContext appContext;

    @Bean(name=GLOBAL_LAD_CONSUMER) 
    @Scope("prototype")
    @Lazy
    public LadConsumer getLadConsumer(final ICoreGlobalLadQuery query, final TimeComparisonStrategyContextFlag timeStrategy) throws Exception {
    	return new LadConsumer(timeStrategy, query);
    }

    @Bean(name=CORE_GLOBAL_LAD_QUERY) 
    @Scope("prototype")
    @Lazy
    public ICoreGlobalLadQuery getCoreGlobalLadQuery(
    		final IGlobalLadDataFactory dataFactory, 
    		final LadChannelValueConverter valueConverter) throws Exception {
    	return new CoreGlobalLadQuery(appContext, dataFactory, valueConverter);
    }

    @Bean(name=GLOBAL_LAD_DATA_FACTORY) 
    @Scope("singleton")
    @Lazy
    public IGlobalLadDataFactory getGlobalLadDataFactory() throws Exception {
    	return new GenericGlobalLadDataFactory();
    }

    @Bean(name=GLOBAL_LAD_CONVERTER) 
    @Scope("singleton")
    @Lazy(value = true)
    public LadChannelValueConverter getGlobalLadConverter() {
    	return new LadChannelValueConverter(appContext);
    	
    }

    @Bean(name=GLOBAL_LAD_FEATURE, destroyMethod="") 
    @Scope("singleton")
    @Lazy(value = true)
    public IGlobalLadFeatureManager getGlobalLadFeature() {
        return new GlobalLadFeature();       
    }
}
