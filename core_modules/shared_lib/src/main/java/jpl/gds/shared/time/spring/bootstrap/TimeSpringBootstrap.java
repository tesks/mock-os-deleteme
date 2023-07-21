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
package jpl.gds.shared.time.spring.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.shared.algorithm.AlgorithmConfig;
import jpl.gds.shared.time.SclkExtractorManager;
import jpl.gds.shared.time.TimeProperties;

/**
 * Spring bootstrap configuration for common time capabilities. Contains configuration for
 * time configuration and time object management and processing beans. 
 *  
 *
 * @since R8
 */
@Configuration
public class TimeSpringBootstrap {
	
	/**
	 * Bean name for the SclkExtractorManager bean.
	 */
	public static final String SCLK_EXTRACTOR_MANAGER = "SCLK_EXTRACTOR_MANAGER";

	/**
     * Creates or returns the SCLK extractor manager bean. Owing to autowiring,
     * also causes the shared ALGORITHM_CONFIG bean to be instantiated and
     * loaded.
     * 
     * @param algoConfig
     *            AlgorithConfig, autowired
     * @param timeConfig
     *            TimeProperties instance
     * @return SclkExtractorManager bean
     */
	@Bean(name=SCLK_EXTRACTOR_MANAGER) 
	@Scope("singleton")
	@Lazy(value = true)
	public SclkExtractorManager getSclkExtractorManager(final AlgorithmConfig algoConfig) {
		return new SclkExtractorManager(algoConfig, TimeProperties.getInstance());
	}
}
