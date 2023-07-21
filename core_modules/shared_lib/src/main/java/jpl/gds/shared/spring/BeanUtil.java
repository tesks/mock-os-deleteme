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
package jpl.gds.shared.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Helper class for interacting with Spring context beans. The current ApplicationContext is injected here
 * during spring startup initilization from <SharedSpringBootstrap>.
 * 
 *
 */
public class BeanUtil implements ApplicationContextAware {
    private static ApplicationContext context;

    /**
     * Default constructor for Spring
     */
    public BeanUtil() { }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        BeanUtil.context = applicationContext;
    }


    /**
     * Gets a bean from the current application context
     * 
     * @param beanClass
     *            class to get a bean for
     * @return Bean
     */
    public static <T> T getBean(final Class<T> beanClass) {
        return context.getBean(beanClass);
    }


}
