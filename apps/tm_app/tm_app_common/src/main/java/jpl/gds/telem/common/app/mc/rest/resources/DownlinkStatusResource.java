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
package jpl.gds.telem.common.app.mc.rest.resources;

import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.performance.HealthStatus;
import jpl.gds.shared.performance.HeapPerformanceData;
import jpl.gds.shared.performance.ProviderPerformanceSummary;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A status POJO to contain Downlink Status Information
 * 
 */
public class DownlinkStatusResource {
    private final PerformanceProperties perfProps;
    private HealthStatus heapHealth;
    private HeapPerformanceData heapPerfData;

    /** Map of health groups keyed by provider name. */
    private final Map<String, ProviderPerformanceSummary> healthGroups = new HashMap<>();

    /**
     * @param appContext the Spring Application Context
     */
    public DownlinkStatusResource(final ApplicationContext appContext) {
        this.perfProps = appContext.getBean(PerformanceProperties.class);
        this.heapHealth = HealthStatus.GREEN;
        this.heapPerfData = new HeapPerformanceData(perfProps);
    }

    /**
     * @return the heapPerfData
     */
    public HeapPerformanceData getHeapPerfData() {
        return heapPerfData;
    }

    /**
     * @return the heapHealth
     */
    public HealthStatus getHeapHealth() {
        return heapHealth;
    }

    /**
     * @return a list of Performance Summary Log Messages
     */
    public List<ProviderPerformanceSummary> getHealthGroups() {
        final List<ProviderPerformanceSummary> perfSummaryList = new ArrayList<>(healthGroups.size());
        for (final ProviderPerformanceSummary perfSummary : healthGroups.values()) {
            perfSummaryList.add(perfSummary);
        }
        return perfSummaryList;
    }

    /**
     * @param heapHealth
     *            the Heap Health Data
     */
    public void setHeapHealth(final HealthStatus heapHealth) {
        this.heapHealth = heapHealth;
    }

    /**
     * @param heapPerfData
     *            the heapPerfData to set
     */
    public void setHeapPerfData(final HeapPerformanceData heapPerfData) {
        this.heapPerfData = heapPerfData;
    }

    /**
     * @param perfSummary
     *            the Performance Summary Data
     */
    public void setPerformanceData(final ProviderPerformanceSummary perfSummary) {
        healthGroups.put(perfSummary.getProviderName(), perfSummary);
    }
}
