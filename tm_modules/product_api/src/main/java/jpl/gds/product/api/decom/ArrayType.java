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
package jpl.gds.product.api.decom;

/**
 * ArrayType is an enumeration that defines all the valid types of
 * data arrays that can be decommutated.
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * <p>
 * ArrayType is an enumeration that defines all the valid types of data
 * arrays that can be decommutated.
 * 
 *
 * @see IArrayField
 */
public enum ArrayType {
    /**
     *  Length of array is fixed and is known at definition time.
     */
    FIXED_LENGTH,
    /**
     * Variable length array that continues to the end of the data stream.
     */
    VARIABLE_LENGTH_UNKNOWN,
    /**
     * Variable length array in which the length is in data right before 
     * the array data in an implicit prefix field.
     */
    VARIABLE_LENGTH_IN_DATA,
    /**
     * Variable length array in which the length is in a previous data field 
     * with the given name.
     */
    VARIABLE_LENGTH_IN_FIELD
}
