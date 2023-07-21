/*
 * Copyright 2006-2021. California Institute of Technology.
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
package jpl.gds.station.api.sle;

import jpl.gds.station.api.IStationTelemHeader;
import jpl.gds.station.api.sle.annotation.ISlePrivateAnnotation;

import java.util.Map;

/**
 * Interface defining the methods available for the SLE Header
 *
 */
public interface ISleHeader extends IStationTelemHeader {

    /** Defined fixed SLE header size in bytes */
    int SLE_HEADER_SIZE = 31;

    /** Antenna ID string from header */
    String getAntennaId();

    /**
     * Antenna ID as integer
     * @return
     */
    int getIntAntennaId();

    /** Per SLE RAF Spec, dataLinkContinuity will be an integer in the range (-1 .. 16777215) */
    int getDatalinkContinuity();

    /** Per SLE RAF Spec, frame quality will be an integer from the following list
     *
     * good (0)
     * erred (1)
     * undetermined (2)
     *
     * */
    ESleFrameQuality getFrameQuality();

    /**
     * Get the Private Annotation. If no SLE Private Annotation is present, expect the NoOp SLE Private Annotation.
     * @return
     *      the SLE Private Annotation
     */
    ISlePrivateAnnotation getPrivateAnnotation();

    /**
     * Get metadata formatted as key value in readable format
     * @return KeyValueAttributes SLE metadata
     */
    Map<String, String> getMetadata();
}