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
package jpl.gds.context.api.filtering;

/**
 * An interface to be implemented by classes whose instances are filterable
 * by spaceccraft ID.
 * 
 *
 * @since R8
 */
public interface IScidFilterable extends IFilterableDataItem {
    /**
     * Gets the spacecraft ID for filtering.  May be null, a non-null value of 0, 
     * or a non-null value of MissionProperties.UNKNOWN_ID to indicate an undefined
     * or unknown spacecraft ID.
     * 
     * @return spacecraft ID; may be null
     */
    public Integer getScid();
}
