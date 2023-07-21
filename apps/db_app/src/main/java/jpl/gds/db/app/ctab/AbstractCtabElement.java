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
package jpl.gds.db.app.ctab;

import java.util.List;

import jpl.gds.db.api.types.IDbChannelSampleProvider;


/**
 * CTAB is a way of printing channel values. The user specifies a template like:
 *
 * SCET C1(DN) SCLK C2(EU)
 *
 * and channel values queried from the database are formatted that way.
 *
 * AbstractCtabElement is the base class for the elements of the template.
 *
 */
abstract public class AbstractCtabElement extends Object
{
    /** "Not applicable" notation */
    public static final String NA   = "N/A";

    /** "NULL" notation */
    public static final String NULL = "NULL";

    private final List<IDbChannelSampleProvider> _channels;


    /**
     * Build CTAB element with channel list. The list comes from the
     * CtabSequence.
     *
     * @param channels Channel value list
     *
     * @throws CtabException Ctab exception
     */
    public AbstractCtabElement(final List<IDbChannelSampleProvider> channels)
        throws CtabException
    {
        super();
        if (channels == null)
        {
            throw new CtabException(getName() + " Null channels");
        }

        _channels = channels;
    }


    /**
     * Get channel value from index.
     *
     * @param index Channel desired
     *
     * @return DatabaseChannelValue
     *
     * @throws CtabException Ctab exception
     */
    public IDbChannelSampleProvider getChannelValue(final int index)
        throws CtabException
    {
        try
        {
            return _channels.get(index);
        }
        catch (final IndexOutOfBoundsException ioobe)
        {
            throw new CtabException(getName() + ".getChannelValue Index error",
                                    ioobe);
        }
    }


    /**
     * Return the number of channel values
     *
     * @return int
     */
    public int getChannelValueListSize()
    {
        return _channels.size();
    }


    /**
     * Convert element to string and append to string builder.
     *
     * @param sb String builder
     *
     * @throws CtabException Ctab exception
     */
    public void appendAsString(final StringBuilder sb) throws CtabException
    {
        if (sb == null)
        {
            throw new CtabException(getName() +
                                    ".appendAsString Null string builder");
        }
    }


    /**
     * Convert header to string and append to string builder.
     *
     * @param sb String builder
     *
     * @throws CtabException Ctab exception
     */
    abstract public void appendAsHeader(final StringBuilder sb)
        throws CtabException;


    /**
     * Class name for diagnostic purposes.
     *
     * @return String
     */
    public String getName()
    {
        return getClass().getSimpleName();
    }
}
