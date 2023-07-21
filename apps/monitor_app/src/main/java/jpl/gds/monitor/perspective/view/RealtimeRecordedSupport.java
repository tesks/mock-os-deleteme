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
package jpl.gds.monitor.perspective.view;


/**
 * This interface should be implemented by view configuration classes that
 * and preferences shells that support a realtime/recorded filter.
 */
public interface RealtimeRecordedSupport {
    /**
     * Indicates whether a GUI view is to display recorded data, realtime data,
     * or both.
     * 
     * @param filterType
     *            one of the RealtimeRecordedFilterType enum values
     */
    public void setRealtimeRecordedFilterType(
            final RealtimeRecordedFilterType filterType);

    /**
     * Gets the flag indicating if GUI a view is to display recorded data,
     * realtime data, or both.
     * 
     * @return one of the RealtimeRecordedFilterType enum values
     */
    public RealtimeRecordedFilterType getRealtimeRecordedFilterType();

}
