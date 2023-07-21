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
package jpl.gds.db.mysql.impl.sql.store.ldi;

import java.util.LinkedList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.db.api.sql.store.IStoreMonitor;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.api.sql.store.ldi.IInserter;
import jpl.gds.shared.performance.IPerformanceData;
import jpl.gds.shared.performance.IPerformanceProvider;
import jpl.gds.shared.performance.PerformanceSummaryPublisher;

/**
 * A class that responds to the PerformanceSummaryPublisher by collecting
 * performance data from all Inserter threads and LDI stores.
 *
 * @version MCPS-7168 dded class,
 */
public class PerformanceReporter implements IPerformanceProvider {

    /** The performance provider name */
    private final String THIS_PROVIDER = "Database Stores";
    
    /**
     * Reference to Spring Application Context
     */
    private final ApplicationContext appContext;
    
    /**
     * Reference to store controller
     */
    private final IDbSqlArchiveController archiveController;

    /**
     * Constructor. Registers for performance data requests.
     */
    public PerformanceReporter(final ApplicationContext appContext) {
        /**
         * MPCS-7927 - Registering with the
         * SessionBasedPerformanceSummaryPublisher. This was not correctly
         * changed after refactoring and moving the base
         * PerformanceSummaryPublisher class to shared lib.
         */
        this.appContext = appContext;
        this.appContext.getBean(PerformanceSummaryPublisher.class).registerProvider(this);
        this.archiveController = appContext.getBean(IDbSqlArchiveController.class);
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.shared.performance.IPerformanceProvider#getPerformanceData()
     */
    @Override
    public List<IPerformanceData> getPerformanceData() {
        final List<IPerformanceData> perfList = new LinkedList<IPerformanceData>();

        /* Collect performance data from all running Inserters */
        for (final StoreIdentifier si : StoreIdentifier.values()) {
            final IStoreMonitor monitor = archiveController.getStoreMonitor(si);
            if (monitor == null) {
                continue;
            }
            final IInserter inserter = monitor.getInserter();

            /* MPCS-7649 -  Add check for shutdown flag */
            if (inserter.isStarted() && !inserter.getShutDown()) {
                perfList.addAll(inserter.getPerformanceData());
            }

            /* Collect performance data from all LDI stores if active */
            if (archiveController.isUp()) {
                perfList.addAll(monitor.getStore().getPerformanceData());
            }
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
     * De-registers for performance data requests.
     */
    public void deregister() {
        appContext.getBean(PerformanceSummaryPublisher.class).deregisterProvider(this);
    }
}
