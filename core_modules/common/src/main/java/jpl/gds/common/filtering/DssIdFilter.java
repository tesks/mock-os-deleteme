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
package jpl.gds.common.filtering;

import java.util.Collection;
import java.util.TreeSet;

import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * A filtering class for DSS IDs.
 * 
 * @since R8
 *
 */
public class DssIdFilter {
    
    private final Collection<UnsignedInteger> stations = new TreeSet<>();   
    private final boolean allowNone;
    
    /**
     * Constructor.
     * 
     * @param stations list of stations allowed by the filter
     * @param allowNone true if the undefined station (0 or null) should be allowed through
     */
    public DssIdFilter(final Collection<UnsignedInteger> stations, final boolean allowNone) {

        this.allowNone = allowNone;
        if (stations != null) {
            for (final UnsignedInteger s: stations) {
                if (s.intValue() > 0) {
                    this.stations.add(s);
                }
            }
        }
    }
    
    /**
     * Indicates if the NONE (undefined or NULL) station should pass the filter.
     * @return true or false
     */
    public boolean allowsNone() {
        return this.allowNone;
    }
    
    /**
     * Gets the collection of stations IDs allowed by this filter.
     * 
     * @return collection of station IDs as unsigned integers
     */
    public Collection<UnsignedInteger> getStations() {
        return new TreeSet<>(stations);
    }
    
    /**
     * Indicates if the given DSS ID passes the filter.
     * 
     * @param dssId station ID to check
     * 
     * @return true if the value passes the filter, false if not
     */
    public boolean accept(final UnsignedInteger dssId) {
        if ((dssId == null || dssId.intValue() == StationIdHolder.UNSPECIFIED_VALUE) && this.allowNone) {
            return true;
        }
        if (dssId == null) {
            return false;
        }
        return stations.isEmpty() || stations.contains(dssId);
    }

}
