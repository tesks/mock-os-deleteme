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
package jpl.gds.watcher.app.handler;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.filtering.DssIdFilter;
import jpl.gds.common.filtering.VcidFilter;
import jpl.gds.shared.message.IMessage;

/**
 * Shared class for handling telemetry ingestion product (eg: packet and frame) messages.
 * These objects potentially still have additional wrapping information and can be filtered by downlink information
 *
 * @param <T>
 *            The interface class for messages being processed by this handler
 */
public abstract class TelemetryIngestionCaptureHandler<T extends IMessage> extends AbstractCaptureHandler<T>
        implements ITelemetryIngestionCaptureHandler {

    private boolean      restoreBodies;
    private VcidFilter vcFilter;
    private DssIdFilter dssFilter;

    /**
     * constructor
     * 
     * @param appContext
     *            the current application context
     * @param csvQueryList
     *            the csv query list properties to be retrieved
     */
    protected TelemetryIngestionCaptureHandler(final ApplicationContext appContext, final String csvQueryList) {
        super(appContext, csvQueryList);
    }

    @Override
    public boolean messageFilterCheck(final T msg) {
        // if there's a message, but nothing to check against, then accept. otherwise, don't accept at this level.
        return super.messageFilterCheck(msg) && vcFilter == null && dssFilter == null;
    }

    /**
     * Set to restore headers and trailers to bodies for stored data
     * 
     * @param restoreBodies
     *            TRUE if headers and trailers are to be stored with data, FALSE
     *            otherwise
     */
    @Override
    public void setRestoreBodies(final boolean restoreBodies) {
        this.restoreBodies = restoreBodies;
    }

    /**
     * Get if headers and trailers are to be restored with bodies for stored
     * data
     * 
     * @return TRUE if header and trailers are stored, FALSE otherwise
     */
    @Override
    public boolean isRestoreBodies() {
        return this.restoreBodies;
    }

    @Override
    public void setVcidFilter(final VcidFilter vcFilter) {
        this.vcFilter = vcFilter;
    }

    @Override
    public VcidFilter getVcidFilter() {
        return this.vcFilter;
    }

    @Override
    public void setDssIdFilter(final DssIdFilter dssFilter) {
        this.dssFilter = dssFilter;
    }

    @Override
    public DssIdFilter getDssIdFilter() {
        return this.dssFilter;
    }

}
