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
 * ProductDecomFieldType is an enumeration that defines all the valid types of
 * data fields that can be decommutated.
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * <p>
 * ProductDecomFieldType is an enumeration that defines all the valid types of
 * data fields that can be decommutated. The ProductDecomFieldType is set when
 * the IProductDecomField it applies to is created via the
 * ProductDecomFieldFactory.
 * 
 *
 * @see IProductDecomField
 */
public enum ProductDecomFieldType {
    /**
     * A decom field that is an array of bit fields.
     */
    BIT_ARRAY_FIELD,
    /**
     * A decom field that consists of bits extracted from a larger
     * BIT_ARRAY_FIELD field.
     */
    BIT_FIELD,

    /**
     * A decom field that represents a primitive data type that may be
     * channelized.
     */
    SIMPLE_FIELD,

    /**
     * A decom field which consists of a stream of bytes, either of known length
     * or extending to the end of the data stream, which can be displayed in
     * decimal, hex, or text format.
     */
    STREAM_FIELD,

    /**
     * A decom field that contains a list of other decom fields, the set of
     * which repeat over and over until the array limit is reached. The array
     * limit may be fixed or variable.
     */    
    ARRAY_FIELD,
    /**
     * A decom field that contains a list of other decom fields.
     */
    STRUCTURE_FIELD,

    /**
     * A decom field that represents a data product object. A data product
     * object has a unique dictionary ID and contains a list of other decom
     * fields.
     */
    DPO;
}
