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
 * TimeCtabElement is the base class for the time elements of the template.
 *
 */
abstract public class TimeCtabElement extends AbstractCtabElement
{
    /**
     * Build CTAB element with channel list. The list comes from the
     * CtabSequence.
     *
     * @param channels Channel value list
     *
     * @throws CtabException Ctab exception
     */
    public TimeCtabElement(final List<IDbChannelSampleProvider> channels)
        throws CtabException
    {
        super(channels);
    }


    /**
     * Locate the first non-null channel value in the list.
     *
     * @return DatabaseChannelValue
     *
     * @throws CtabException if none exist
     */
    public IDbChannelSampleProvider getActiveChannelValue() throws CtabException
    {
        final int size = getChannelValueListSize();

        for (int i = 0; i < size; ++i)
        {
            final IDbChannelSampleProvider dcv = getChannelValue(i);

            if (dcv != null)
            {
                return dcv;
            }
        }

        throw new CtabException(getName() + ".getChannelValue Index error");
    }
}
