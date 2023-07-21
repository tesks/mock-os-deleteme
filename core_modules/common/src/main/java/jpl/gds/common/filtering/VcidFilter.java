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

import jpl.gds.shared.types.UnsignedInteger;

/**
 * A filtering class for VCIDs.
 * 
 *
 * @since R8
 *
 */
public class VcidFilter {
    
    private final Collection<UnsignedInteger> vcids = new TreeSet<>();   
    private final boolean allowNone;
    
    /**
     * Constructor.
     * 
     * @param vcids list of vcids allowed by the filter
     * @param allowNone true if the undefined VCID (null) should be allowed through
     */
    public VcidFilter(final Collection<UnsignedInteger> vcids, final boolean allowNone) {

        this.allowNone = allowNone;
        if (vcids != null) {
            this.vcids.addAll(vcids);
        }
    }
    
    /**
     * Indicates if the NONE (undefined or NULL) VCID should pass the filter.
     * @return true or false
     */
    public boolean allowsNone() {
        return this.allowNone;
    }
    
    /**
     * Gets the collection of VCIDs allowed by this filter.
     * 
     * @return collection of VCIDs as unsigned integers
     */
    public Collection<UnsignedInteger> getVcids() {
        return new TreeSet<>(vcids);
    }
    
    /**
     * Indicates if the given VCID passes the filter.
     * 
     * @param vcid the VCID to check
     * 
     * @return true if the value passes the filter, false if not
     */
    public boolean accept(final UnsignedInteger vcid) {
        if (vcid == null && this.allowNone) {
            return true;
        }
        if (vcid == null) {
            return false;
        }
        return vcids.isEmpty() || vcids.contains(vcid);
    }

}
