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
import jpl.gds.shared.formatting.SprintfFormat;


/**
 * CTAB is a way of printing channel values. The user specifies a template like:
 *
 * SCET C1(DN) SCLK C2(EU)
 *
 * and channel values queried from the database are formatted that way.
 *
 * ValueCtabElement is the base class for the value elements of the template.
 *
 */
abstract public class ValueCtabElement extends AbstractCtabElement
{
    private final int    _index;
    private final String _channelName;

    /**
     * true if formatting is desired for channel values
     */
    protected boolean formatChanVals;

    /**
     * the formatter to use for formatting channel values
     */
    protected SprintfFormat formatter;


    /**
     * Build CTAB element with channel list. The list comes from the
     * CtabSequence.
     *
     * We cannot validate the size completely at this time.
     *
     * @param channels
     *            Channel value list
     * @param index
     *            Our channel index in the list
     * @param name
     *            Our channel name for header purposes
     * @param formatChannelValues
     *            true if formatting is desired for channel values
     * @param formatter
     *            the formatter to use for formatting channel values
     *
     * @throws CtabException
     *             Ctab exception
     */
    public ValueCtabElement(final List<IDbChannelSampleProvider> channels,
                            final int                        index,
                            final String                     name,
                            final boolean formatChannelValues,
                            final SprintfFormat formatter)
        throws CtabException
    {
        super(channels);
        this.formatChanVals = formatChannelValues;
        this.formatter = formatter;
        
        if (index < 0)
        {
            throw new CtabException(getName() + " Negative index");
        }

        if ((name == null) || (name.length() == 0))
        {
            throw new CtabException(getName() + " Empty channel name");
        }

        _index       = index;
        _channelName = name;
    }


    /**
     * Get our channel value.
     *
     * @return DatabaseChannelValue
     *
     * @throws CtabException CTab exception
     */
    public IDbChannelSampleProvider getChannelValue() throws CtabException
    {
        return getChannelValue(_index);
    }


    /**
     * Get our channel name.
     *
     * @return String
     */
    protected String getChannelName()
    {
        return _channelName;
    }
}
