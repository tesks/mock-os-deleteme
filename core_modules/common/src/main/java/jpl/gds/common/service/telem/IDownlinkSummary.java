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
package jpl.gds.common.service.telem;

/**
 * The LEGACY Interface for a POJO to return IDownlinkSummary Status via RESTful interface
 * 
 * @since R8.1
 */
public interface IDownlinkSummary
        extends ITelemetryIngestorSummary, ITelemetryProcessorSummary {

}