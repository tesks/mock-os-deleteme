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

import jpl.gds.common.filtering.DssIdFilter;
import jpl.gds.common.filtering.VcidFilter;

/**
 * Interface for all capture handlers extending TelemetryIngestionCaptureHandler.
 */
public interface ITelemetryIngestionCaptureHandler extends ICaptureHandler {

    public void setRestoreBodies(final boolean restoreBodies);

    public boolean isRestoreBodies();

    public void setVcidFilter(final VcidFilter vcFilter);

    public VcidFilter getVcidFilter();

    public void setDssIdFilter(DssIdFilter dssFilter);

    public DssIdFilter getDssIdFilter();

}
