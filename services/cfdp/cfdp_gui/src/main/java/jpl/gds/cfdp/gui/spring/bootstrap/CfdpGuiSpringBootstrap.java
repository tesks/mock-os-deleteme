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
package jpl.gds.cfdp.gui.spring.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.cfdp.gui.config.CfdpGuiProperties;
import jpl.gds.cfdp.gui.up.factory.CfdpGuiUplinkTabFactory;
import jpl.gds.tcapp.app.gui.factory.IUplinkTabFactory;
import jpl.gds.tcapp.spring.bootstrap.TcAppSpringBootstrap;

/**
 * Spring configuration class for beans in the CFDP GUI project.
 * 
 *
 */
@Configuration
//This bootstrap directly depends on TcAppSpringBootstrap. This makes sure it's loaded after it
@Import({TcAppSpringBootstrap.class})
public class CfdpGuiSpringBootstrap {
	
	public static final String SEND_FILE_CFDP = "SEND_FILE_CFDP";
	public static final String CFDP_GUI_PROPERTIES = "CFDP_GUI_PROPERTIES";

	
	/**
	 * Get the CFDP upgraded uplink tab factory
	 * @param appContext the current application context
	 * @return the factory that makes uplink tabs
	 */
	@Bean(name = TcAppSpringBootstrap.UPLINK_TAB_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IUplinkTabFactory getCfdpUplinkTabFactory() {
		return new CfdpGuiUplinkTabFactory();
	}
	
	/**
	 * Get the CFDP GUI properties
	 * @param appContext the current application context
	 * @return the CfdpGuiProperties object
	 */
	@Bean(name = CFDP_GUI_PROPERTIES)
	@Scope("singleton")
	@Lazy(value = true)
	public CfdpGuiProperties getCfdpGuiProperties() {
		return new CfdpGuiProperties();
	}

}
