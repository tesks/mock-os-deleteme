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
package jpl.gds.telem.process.app.mc.rest.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import jpl.gds.common.service.telem.ITelemetryProcessorSummary;
import jpl.gds.common.service.telem.ITelemetrySummary;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.telem.common.ITelemetrySummaryDelegateResource;
import jpl.gds.telem.common.state.WorkerState;
import jpl.gds.telem.process.IProcessWorker;
import jpl.gds.telem.process.IProcessWorkerStatus;

/**
 * A POJO delegate to deliver Telemetry Summary information to RESTful M&C Service
 * 
 */
public class TelemetryProcessorSummaryDelegateResource implements ITelemetrySummaryDelegateResource {
    private final        IProcessWorker             app;
    private static final ITelemetryProcessorSummary EMPTY_TM_SUMMARY = new EmptyDelegate();
    private static final String                     UNKNOWN_TIME     = "UNKNOWN TIME";

    /** Status: Telemetry Summary Object */
    private ITelemetryProcessorSummary              tmSummary;

    /**
     * @param app
     *            the IProcessWorker that is running
     */
    public TelemetryProcessorSummaryDelegateResource(final IProcessWorker app) {
        super();
        this.app = app;
        setTelemetrySummary(EMPTY_TM_SUMMARY);
    }

    /**
     * Gets the control state of the Telemetry Process worker
     *
     * @return the control state of the worker
     */
    @Override
    @JsonProperty("state")
    public WorkerState getControlState() {
        return app.getState();
    }

    @Override
    public IProcessWorkerStatus getStatus() {
        return app.getStatus();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    @Override
    public long getContextKey() {
        return tmSummary.getContextKey();
    }

    @Override
    public String getFullName() {
        return tmSummary.getFullName();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getPartialProducts() {
        return tmSummary.getPartialProducts();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getProductDataBytes() {
        return tmSummary.getProductDataBytes();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getProducts() {
        return tmSummary.getProducts();
    }

   @Override
    public String getStartTime() {
        final IAccurateDateTime t = tmSummary.getStartTime();
        return (t != null) ? TimeUtility.getFormatterFromPool().format(t) : UNKNOWN_TIME;
    }

   @Override
    public String getStopTime() {
        final IAccurateDateTime t = tmSummary.getStopTime();
        return (t != null) ? TimeUtility.getFormatterFromPool().format(t) : UNKNOWN_TIME;
    }

    @Override
    public String getOutputDirectory() {
        return tmSummary.getOutputDirectory();
    }

    /**
     * @return the current TimeComparisonStrategy
     */
    public String getTimeComparisonStrategy() {
        return app.getTimeComparisonStrategy().getDisplayName();
    }

    /**
     * @param summary
     *            the telemetry summary object to assign as delegate for this class
     */
    public void setTelemetrySummary(final ITelemetrySummary summary) {
        this.tmSummary = (null == summary) ? EMPTY_TM_SUMMARY : (ITelemetryProcessorSummary) summary;
    }

    private static class EmptyDelegate implements ITelemetryProcessorSummary {
        public EmptyDelegate() {
            super();
        }

        @Override
        public long getContextKey() {
            return 0;
        }

        @Override
        public void setContextKey(long dbKey) { }

        @Override
        public String getFullName() {
            return null;
        }

        @Override
        public void setFullName(String name) { }

        @Override
        public long getPartialProducts() {
            return 0;
        }

        @Override
        public long getProductDataBytes() {
            return 0;
        }

        @Override
        public long getProducts() {
            return 0;
        }

        @Override
        public IAccurateDateTime getStartTime() {
            return null;
        }

        @Override
        public IAccurateDateTime getStopTime() {
            return null;
        }

        @Override
        public String getOutputDirectory() {
            return null;
        }

        @Override
        public void setOutputDirectory(String dir) {  }

        @Override
        public void populateBasicSummary(final IAccurateDateTime start, final IAccurateDateTime end, final String name,
                                         final String outputDir, final Long key) {
            return;
        }

        @Override
        public String toStringNoBlanks(String in) { return ""; }

        @Override
        public long getEhaPackets() {
            return 0;
        }

        @Override
        public long getEvrPackets() {
            return 0;
        }

    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getEhaPackets() {
        return tmSummary.getEhaPackets();
    }

    /**
     * Delegate POJO method for ITelemetrySummary
     * 
     * @return @See ITelemetrySummary
     */
    public long getEvrPackets() {
        return tmSummary.getEvrPackets();
    }

}
