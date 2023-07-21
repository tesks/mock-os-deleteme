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
package jpl.gds.db.mysql.impl.sql.store.ldi;


/**
 * Fundamental information for channel-value-change processing.
 *
 */
abstract public class TypeInfo extends Object
{
    /**
     * String encoding.
     */
    public static final String ENCODING = "UTF-8";

    /**
     * Are we supporting surrogates?
     */
    public static final boolean UTF8_SURROGATES = false;

    /**
     * The maximum number of bytes into which a codepoint can expand.
     */
    public static final int UTF8_MAX_BYTES      = (UTF8_SURROGATES ? 4 : 3);

    /**
     * Size of SQL tinyint in bytes.
     */
    public static final int TINYINT_SIZE        = Byte.SIZE    / Byte.SIZE;

    /**
     * Size of SQL smallint in bytes.
     */
    public static final int SMALLINT_SIZE       = Short.SIZE   / Byte.SIZE;

    /**
     * Size of SQL int in bytes.
     */
    public static final int INT_SIZE            = Integer.SIZE / Byte.SIZE;

    /**
     * Size of SQL bigint in bytes.
     */
    public static final int BIGINT_SIZE         = Long.SIZE    / Byte.SIZE;

    /**
     * Size of SQL double in bytes.
     */
    public static final int DOUBLE_SIZE         = Double.SIZE  / Byte.SIZE;

    /**
     * Size of SQL float in bytes.
     */
    public static final int FLOAT_SIZE          = Float.SIZE   / Byte.SIZE;

    /**
     * A zero as a byte.
     */
    public static final byte ZERO_BYTE          = (byte) 0;

    /**
     * A one as a byte.
     */
    public static final byte ONE_BYTE           = (byte) 1;

    /**
     * A blank as a byte.
     */
    public static final byte BLANK_BYTE         = (byte) ' ';

    /**
     * Mark field as NULL.
     */
    public static final byte NULL_BYTE          = ONE_BYTE;

    /**
     * Mark field as NON-NULL.
     */
    public static final byte NONNULL_BYTE       = ZERO_BYTE;

    /**
     * Mark field as TRUE.
     */
    public static final byte TRUE_BYTE          = ONE_BYTE;

    /**
     * Mark field as FALSE.
     */
    public static final byte FALSE_BYTE         = ZERO_BYTE;

    /**
     * Mask to extract a byte.
     */
    public static final long BYTE_MASK          = (1L << Byte.SIZE) - 1L;
}
