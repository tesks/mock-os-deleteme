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
package jpl.gds.db.impl.types;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.types.IDbInfoProvider;
import jpl.gds.db.api.types.IDbQueryableFactory;

/**
 * 
 * @param <T>
 *            the Type of IDbQueryable Provider generated by this factory
 * @param <Q>
 *            the Type of IDbQueryable Updater generated by this factory
 *
 */
public abstract class AbstractDbQueryableFactory<T extends IDbInfoProvider, Q extends T>
        implements IDbQueryableFactory<T, Q> {
    /**
     * The Spring Application Context
     */
    protected final ApplicationContext appContext;

    /**
     * the class object of the Class
     * <Q>that is an updater to the interface Class<T>
     */
    protected final Class<Q>           updaterInterface;

    /**
     * @param appContext
     *            the Spring Application Context
     * @param updaterInterface
     *            the class object of the Class
     *            <Q>that is an updater to the interface Class<T>
     */
    public AbstractDbQueryableFactory(final ApplicationContext appContext, final Class<Q> updaterInterface) {
        super();
        this.appContext = appContext;
        this.updaterInterface = updaterInterface;
    }

    /**
     * @return an IDbQueryable object
     */
    @Override
    public T createQueryableProvider() {
        throw new UnsupportedOperationException("Invalid factory method called for : "
                + this.getClass().getSimpleName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T createQueryableProvider(final Object... args) {
        throw new UnsupportedOperationException("Invalid factory method called for : "
                + this.getClass().getSimpleName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Q createQueryableUpdater() {
        return convertProviderToUpdater(createQueryableProvider());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Q createQueryableUpdater(final Object... args) {
        return convertProviderToUpdater(createQueryableProvider(args));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Q convertProviderToUpdater(final T provider) {
        return updaterInterface.cast(provider);
    }
}
