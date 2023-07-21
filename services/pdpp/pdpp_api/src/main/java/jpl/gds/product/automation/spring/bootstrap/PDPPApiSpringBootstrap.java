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

package jpl.gds.product.automation.spring.bootstrap;


import jpl.gds.product.PdppApiBeans;
import jpl.gds.product.automation.hibernate.AutomationLogger;
import jpl.gds.product.automation.hibernate.IAutomationLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

import jpl.gds.product.automation.ProductAutomationProperties;
import jpl.gds.product.automation.hibernate.AutomationSessionFactory;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationActionDAO;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationClassMapDAO;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationLogsDAO;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationProcessDAO;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationProductDAO;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationStatusDAO;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationUserDAO;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * PDPPApi Spring Bootstrap
 *
 */
@Configuration
@Lazy
public class PDPPApiSpringBootstrap {

	/**
	 * Hold onto things we want to close with context closed events.  Reason being we don not 
	 * want to get from the context if they were never created because we would have to create them.
	 */
	
	private AutomationSessionFactory sessionFactory = null;
	
	@Autowired
	private ApplicationContext appContext;

    /**
     * Gets Product Automation Properties
     * 
     * @param sseFlag
     *            The SSE context Flag
     * @return ProductAutomationProperties
     */
	@Bean(name=PdppApiBeans.PRODUCT_AUTOMATION_PROPERTIES)
	@Scope("singleton")
	@Lazy(value = true)
    public ProductAutomationProperties getProductAutomationProperties(final SseContextFlag sseFlag) {
        return new ProductAutomationProperties(sseFlag);
	}

	@Bean(name=PdppApiBeans.AUTOMATION_SESSION_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public AutomationSessionFactory getAutomationSessionFactory() {
		sessionFactory = new AutomationSessionFactory(appContext);
		return sessionFactory;
	}
	
	@EventListener
	public void shutdownAutomationSessionFactory(final ContextClosedEvent evt) {
		if (sessionFactory != null) {
			sessionFactory.closeSessionFactory();
		}
	}
	

	@Bean(name=PdppApiBeans.PRODUCT_DAO)
	@Scope("singleton")
	@Lazy(value = true)
	public ProductAutomationProductDAO getProductDAO(final AutomationSessionFactory sessionFactory) {
		return new ProductAutomationProductDAO(appContext, sessionFactory);
	}

	@Bean(name=PdppApiBeans.ACTION_DAO)
	@Scope("singleton")
	@Lazy(value = true)
	public ProductAutomationActionDAO getActionDAO(final AutomationSessionFactory sessionFactory) {
		return new ProductAutomationActionDAO(appContext, sessionFactory);
	}

	@Bean(name=PdppApiBeans.STATUS_DAO)
	@Scope("singleton")
	@Lazy(value = true)
	public ProductAutomationStatusDAO getStatusDAO(final AutomationSessionFactory sessionFactory) {
		return new ProductAutomationStatusDAO(appContext, sessionFactory);
	}

	@Bean(name=PdppApiBeans.PROCESS_DAO)
	@Scope("singleton")
	@Lazy(value = true)
	public ProductAutomationProcessDAO getProcessDAO(final AutomationSessionFactory sessionFactory) {
		return new ProductAutomationProcessDAO(appContext, sessionFactory);
	}


	@Bean(name=PdppApiBeans.CLASS_MAP_DAO)
	@Scope("singleton")
	@Lazy(value = true)
	public ProductAutomationClassMapDAO getClassMapDAO(final AutomationSessionFactory sessionFactory) {
		return new ProductAutomationClassMapDAO(appContext, sessionFactory);
	}

	@Bean(name=PdppApiBeans.LOGS_DAO)
	@Scope("singleton")
	@Lazy(value = true)
	public ProductAutomationLogsDAO getLogsDAO(final AutomationSessionFactory sessionFactory) {
		return new ProductAutomationLogsDAO(appContext, sessionFactory);
	}

	@Bean(name=PdppApiBeans.USER_DAO)
	@Scope("singleton")
	@Lazy(value = true)
	public ProductAutomationUserDAO getUserDAO(final AutomationSessionFactory sessionFactory) {
		return new ProductAutomationUserDAO(appContext, sessionFactory);
	}

	@Bean(name=PdppApiBeans.AUTOMATION_LOGGER)
	@Lazy
	@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
	public IAutomationLogger getAutomationLogger() {
		return new AutomationLogger(appContext);
	}


}
