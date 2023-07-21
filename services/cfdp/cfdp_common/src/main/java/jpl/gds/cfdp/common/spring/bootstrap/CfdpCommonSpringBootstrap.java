/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.cfdp.common.spring.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.cfdp.common.config.CfdpCommonProperties;

/**
 * Spring configuration class for beans in the CFDP common project.
 * 
 *
 */
@Configuration
public class CfdpCommonSpringBootstrap {
	/** Bean name for CfdpCommonProperties */
	public static final String CFDP_COMMON_PROPERTIES = "CFDP_COMMON_PROPERTIES";
	
	/**
	 * Gets the singleton CfdpCommonProperties
	 * 
	 * @return the CfdpCommonProperties bean
	 */
	@Bean(name = CFDP_COMMON_PROPERTIES)
	@Scope("singleton")
	@Lazy(value = true)
	public CfdpCommonProperties getCfdpCommonProperties() {
		return new CfdpCommonProperties();
	}
}
