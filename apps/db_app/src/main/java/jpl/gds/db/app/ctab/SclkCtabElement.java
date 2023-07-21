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
import jpl.gds.shared.time.ISclk;


/**
 * CTAB is a way of printing channel values. The user specifies a template like:
 *
 * SCET C1(DN) SCLK C2(EU)
 *
 * and channel values queried from the database are formatted that way.
 *
 * This class does SCLK times.
 *
 */
public class SclkCtabElement extends TimeCtabElement
{
    /**
     * Build CTAB element with channel list. The list comes from the
     * CtabSequence.
     *
     * @param channels
     *            Channel value list
     * @throws CtabException
     *             Ctab exception
     */
    public SclkCtabElement(final List<IDbChannelSampleProvider> channels)
        throws CtabException
    {
        super(channels);
    }


    @Override
    /**
     * Convert element to string and append to string builder.
     *
     * @param sb
     *            the StringBuffer to append
     *
     * @throws CtabException
     */
    public void appendAsString(final StringBuilder sb) throws CtabException
    {
        super.appendAsString(sb);

        final ISclk sclk = getActiveChannelValue().getSclk();

        if (sclk == null)
        {
            sb.append(NULL);
        }
        else
        {
            sb.append(sclk);
        }
    }


    @Override
    /**
     * Convert header to string and append to string builder.
     *
     * @param sb
     *
     * @throws CtabException
     */
    public void appendAsHeader(final StringBuilder sb) throws CtabException
    {
        super.appendAsString(sb);

        sb.append("SCLK");
    }
}
