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
package jpl.gds.dictionary.api.evr;

/**
 * An enumeration that identifies the possible data types for EVR arguments.
 * 
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * 
 *
 * 
 */
public enum EvrArgumentType {
    /** Unsigned 64-bit integer */
    U64,
    /** Unsigned 32-bit integer */
    U32,
    /** Unsigned 16-bit integer */
    U16,
    /** Unsigned 8-bit integer */
    U8,
    /** Signed 64-bit integer */
    I64,
    /** Signed 32-bit integer */
    I32,
    /** Signed 16-bit integer */
    I16,
    /** Signed 8-bit integer */
    I8,
    /** 32-bit floating point number (IEEE) */
    F32,
    /** 64-bit floating point number (IEEE) */
    F64,
    /** Boolean */
    BOOL,
    /** Fixed length string */
    FIX_STRING,
    /** Variable length string */
    VAR_STRING,
    /** Enumeration value */
    ENUM,
    /** OPCODE replacement */
    OPCODE,
    /** Sequence ID replacement */
    SEQID;

    /**
     * Gets the byte length associated with the data type, if it has an inherent
     * length
     * 
     * @return length in bytes for this data type, or 0 if no length in inherent
     *         in the type
     */
    public int getByteLength() {

        switch (this) {
        case I8:
        case U8:
        case BOOL:
            return 1;
        case I16:
        case U16:
            return 2;
        case I32:
        case U32:
        case F32:
            return 4;
        case I64:
        case U64:
        case F64:
            return 8;
        default:
            return 0;
        }
    }

}
