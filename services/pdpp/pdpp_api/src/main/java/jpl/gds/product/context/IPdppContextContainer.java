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
package jpl.gds.product.context;

import jpl.gds.common.spring.context.IContextContainer;
import jpl.gds.context.api.IContextConfiguration;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;

/**
 * Exposing an interface for PdppContextContainer to allow for Springification and possible future implementations.
 */
public interface IPdppContextContainer extends IContextContainer {

    /**
     * If the services are already started for this container.
     *
     * @return boolean
     */
    boolean isServicesStarted();

    /**
     * {@inheritDoc}
     * @see jpl.gds.common.spring.context.IContextContainer#getApplicationContext()
     */
    @Override
    ApplicationContext getApplicationContext();

    /**
     * @return the parent context
     */
    ApplicationContext getParentContext();

    /**
     * @return the child context
     */
    ApplicationContext getChildContext();

    /**
     * @return the configuration object describing the parent session
     */
    IContextConfiguration getParentSessionConfig();

    /**
     * @return the configuration object describing the child session
     */
    IContextConfiguration getChildSessionConfig();

    /**
     * {@inheritDoc}
     * @see jpl.gds.common.spring.context.IContextContainer#startChildDbStores()
     */
    @Override
    void startChildDbStores();

    /**
     * {@inheritDoc}
     * @see jpl.gds.common.spring.context.IContextContainer#stopChildDbStores()
     */
    @Override
    void stopChildDbStores();

    /**
     * {@inheritDoc}
     * @see jpl.gds.common.spring.context.IContextContainer#startChildMessagePortal()
     */
    @Override
    void startChildMessagePortal();

    /**
     * {@inheritDoc}
     * @see jpl.gds.common.spring.context.IContextContainer#stopChildMessagePortal()
     */
    @Override
    void stopChildMessagePortal();

    /**
     * @param instance
     * @param startupMethod
     * @param shutdownMethod
     */
    void registerShutdownMethod(Object instance, Method startupMethod, Method shutdownMethod);

    /**
     * @param clazz
     */
    void unregisterService(Class<?> clazz);

    /**
     * {@inheritDoc}
     * @see jpl.gds.common.spring.context.IContextContainer#startRequiredServices()
     */
    @Override
    void startRequiredServices();

    /**
     * {@inheritDoc}
     * @see jpl.gds.common.spring.context.IContextContainer#shutdownRequiredServices()
     */
    @Override
    void shutdownRequiredServices();
}
