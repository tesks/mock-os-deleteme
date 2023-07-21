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
import java.util.List;
import java.util.TreeSet;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * A filtering class for SCIDs.
 * 
 *
 * @since R8
 *
 */
public class ScidFilter {
    
    private final Collection<UnsignedInteger> scids = new TreeSet<>();
    private final boolean allowNone;
    
    /**
     * Constructor.
     * 
     * @param list
     *            list of scids allowed by the filter
     */
    public ScidFilter(final List<UnsignedInteger> list) {
        this(list, false);
    }

    /**
     * Constructor.
     * 
     * @param scids
     *            list of scids allowed by the filter
     * @param allowNone
     *            true if the undefined SCID (null) should be allowed through
     */
    public ScidFilter(final Collection<UnsignedInteger> scids, final boolean allowNone) {

        this.allowNone = allowNone;
        if (scids != null) {
            for (final UnsignedInteger s : scids) {
                if (s.intValue() > 0) {
                    this.scids.add(s);
                }
            }
        }
    }
    
    /**
     * Indicates if the NONE (undefined or NULL) SCID should pass the filter.
     * @return true or false
     */
    public boolean allowsNone() {
        return this.allowNone;
    }
    
    /**
     * Gets the collection of SCIDs allowed by this filter.
     * 
     * @return collection of SCIDs as unsigned integers
     */
    public Collection<UnsignedInteger> getScids() {
        return new TreeSet<>(scids);
    }
    
    /**
     * Indicates if the given SCID passes the filter.
     * 
     * @param scid the SCID to check
     * 
     * @return true if the value passes the filter, false if not
     */
    public boolean accept(final UnsignedInteger scid) {
        if ((scid == null || scid.intValue() == MissionProperties.UNKNOWN_ID || scid.intValue() == 0) 
                && this.allowNone) {
            return true;
        }
        if (scid == null) {
            return false;
        }
        return scids.isEmpty() || scids.contains(scid);
    }

}
