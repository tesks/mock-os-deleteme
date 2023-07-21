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
package jpl.gds.telem.common.app.mc.rest.resources;

import jpl.gds.eha.api.channel.IClientChannelValue;

/**
 * Class ChannelValueResource
 */
public class ChannelValueResource {
    private static final String       NA  = "n/a";
    private static final String       NAN = "NaN";

    private final IClientChannelValue channelValue;

    /**
     * @param channelValue
     *            the channel value
     */
    public ChannelValueResource(final IClientChannelValue channelValue) {
        super();
        this.channelValue = channelValue;
    }

    /**
     * @return the channel's ID
     */
    public String getChannelId() {
        try {
            return channelValue.getChanId();
        }
        catch (final Throwable t) {
            return NA;
        }
    }

    /**
     * @return the channel's DN value
     */
    public String getChannelDn() {
        try {
            return channelValue.getDn().toString();
        }
        catch (final Throwable t) {
            return NA;
        }
    }

    /**
     * @return the channel's EU value
     */
    public String getChannelEu() {
        try {
            return Double.toString(channelValue.getEu());
        }
        catch (final Throwable t) {
            return NAN;
        }
    }
}
