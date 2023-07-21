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
package jpl.gds.globallad.spring.beans;

import static jpl.gds.globallad.spring.beans.BeanNames.GLAD_DATA_FACTORY;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.factory.GenericGlobalLadDataFactory;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory;
import jpl.gds.globallad.data.storage.GlobalLadDataStore;

/**
 * Spring boot bean configuration class.  All beans are configured in separate 
 * bean configuration files.  Nothing has been integrated into the individual 
 * class files.  This was done so that Spring could be used with as little impact 
 * to the MPCS code as possible.  In the future we may want to do further integration, 
 * but for now this is the method that is being employed.
 */
@Configuration
public class ComponentBeansConfig {
	/**
	 * @return new global lad configuration instance.
	 */
	@Bean(name=BeanNames.GLAD_CONFIG_NAME)
	public GlobalLadProperties globalLadProperties() {
		/**
		 * In a perfect world, this should return a new instance to be 
		 * managed by spring alone.  However we can not inject this in 
		 * all places that it is being used so we must use the global 
		 * singleton so everything in the process uses the same instnace.
		 */
		return GlobalLadProperties.getGlobalInstance();
	}

	/**
	 * @return the global lad storage instance.
	 */
	@Bean(name=BeanNames.GLAD_DATA_STORE)
	public GlobalLadDataStore globalLadDataStore() {
		return new GlobalLadDataStore();
	}
	
	/**
	 * @return The data factory instance
	 * @throws Exception
	 */
	@Bean(name=GLAD_DATA_FACTORY)
    @Primary
	public IGlobalLadDataFactory globalLadDataFactory() throws Exception {
		return new GenericGlobalLadDataFactory();
	}
	
}