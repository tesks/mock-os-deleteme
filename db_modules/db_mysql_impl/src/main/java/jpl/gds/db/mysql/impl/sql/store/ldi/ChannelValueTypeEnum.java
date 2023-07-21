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
 * Specifies the types needed for a ChannelValue table row as used by
 * channel-value-change. We also specify the default sizes.
 *
 */
public enum ChannelValueTypeEnum
{
    /**
     * SQL TINYINT UNSIGNED
     */
    sql_utinyint( TypeInfo.TINYINT_SIZE,  true),

    /**
     * SQL SMALLINT UNSIGNED
     */
    sql_usmallint(TypeInfo.SMALLINT_SIZE, true),

    /**
     * SQL BIGINT SIGNED
     */
    sql_bigint(   TypeInfo.BIGINT_SIZE,   false),

    /**
     * SQL BIGINT UNSIGNED
     */
    sql_ubigint(  TypeInfo.BIGINT_SIZE,   true),

    /**
     * SQL DOUBLE SIGNED
     */
    sql_double(   TypeInfo.DOUBLE_SIZE,   false),

    /**
     * SQL VARCHAR
     */
    sql_char(     0,                      false);


    // In bytes; zero means variable (for sql_char)
    private final int     _defaultSize;
    private final boolean _unsigned;


    /**
     * Constructor.
     *
     * @param defaultSize
     * @param unsigned
     *
     * @throws IllegalArgumentException
     */
    private ChannelValueTypeEnum(final int     defaultSize,
                                 final boolean unsigned)
        throws IllegalArgumentException
    {
        if (defaultSize < 0)
        {
            throw new IllegalArgumentException("Size must be nonnegative");
        }

        _defaultSize = defaultSize;
        _unsigned    = unsigned;
    }


    /**
     * Get default size.
     *
     * @return int
     */
    public int getDefaultSize()
    {
        return _defaultSize;
    }


    /**
     * Return true if unsigned.
     *
     * @return boolean
     */
    public boolean getUnsigned()
    {
        return _unsigned;
    }
}
