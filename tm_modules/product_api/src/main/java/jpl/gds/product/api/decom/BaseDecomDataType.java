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

import jpl.gds.dictionary.api.channel.ChannelType;

/**
 * BaseDecomType is an enumeration that defines all the primitive data types
 * supported by decommutation.
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * 
 *
 */
public enum BaseDecomDataType {

    /** Field data type is unknown */
	UNKNOWN,
	/** Field data type is unsigned integer. */
	UNSIGNED_INT,
	/** Field data type is signed integer. */
	SIGNED_INT,
	/** Field data type is floating point. */
	FLOAT,
	/** Field data type is text string. */
	STRING,
	/** Field is a filler field that has no specific type. */
	FILL,
	/** Field is an enumerated value. */
	ENUMERATION,
	/** Field data type is digital (unsigned) */
	DIGITAL,
	/** Field data type is boolean. */
	BOOLEAN,
	/** Field data type is time. */
	TIME;

    /**
     * Indicates whether this data dictionary type can be used as the type for a
     * channel time.
     * 
     * @return true if data type can be used for length fields; false if not
     */
    public boolean isChannelTimeType() {
        switch (this) {
            case UNSIGNED_INT:
            case SIGNED_INT:
            case TIME:
                return true;
            default:
                return false;
        }
    }

    
    /**
     * Gets the channel type that is compatible with the given decom field
     * type.
     * 
     * @return the channel type
     */
    protected ChannelType getChannelType() {
        switch (this) {
        case UNSIGNED_INT:
            return ChannelType.UNSIGNED_INT;
        case SIGNED_INT:
            return ChannelType.SIGNED_INT;
        case FLOAT:
            return ChannelType.FLOAT;
        case ENUMERATION:
            return ChannelType.STATUS;
        case TIME:
            return ChannelType.TIME;
        default:
            throw new IllegalStateException(
                    "Unsupported data type for channel field: " + this.toString());
        }
    }
}
