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
package jpl.gds.session.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;


/**
 * Enum for parameter modes. They represent applications, essentially.
 *
 * INTEGRATED will be switched to one of the other four depending upon
 * whether SSE and/or uplink are allowed. That's the mode you will usually use
 * to set up the state. If you use the others they will be checked for
 * compatibility.
 *
 * The reason for the split is so that the validity sets can specify more
 * precisely how the parameter is to be used. Since INTEGRATED will never be
 * an active mode, there is no reason (or harm) to put it in a validity set.
 *
 * Each constant sets exactly one flag.
 *
 * @see jpl.gds.session.validation.State
 *
 */
public enum ModeEnum
{
    /** Integrated Chill with FSW and SSE and uplink enabled */
    INTEGRATED_FSW_SSE_UP(false, false, false, true, false),

    /** Integrated Chill with FSW and SSE enabled */
    INTEGRATED_FSW_SSE(false, false, false, true, false),

    /** Integrated Chill with FSW and uplink enabled */
    INTEGRATED_FSW_UP(false, false, false, true, false),

    /** Integrated Chill with FSW but neither SSE nor uplink enabled */
    INTEGRATED_FSW_ONLY(false, false, false, true, false),

    /**
     * Integrated Chill, undifferentiated. Switched to one of the other
     * four based upon the enable flags.
     */
    INTEGRATED(false, false, false, true, false),

    /** FSW downlink */
    FSW_DOWNLINK(true, false, false, false, false),

    /** SSE downlink */
    SSE_DOWNLINK(false, false, true, false, false),

    /** Uplink */
    UPLINK(false, true, false, false, false),

    /** Monitor */
    MONITOR(false, false, false, false, true);


    /** Does this constant represent a FSW downlink application? */
    private final boolean _isFswDownlink;

    /** Does this constant represent an uplink application? */
    private final boolean _isUplink;

    /** Does this constant represent a SSE downlink application? */
    private final boolean _isSseDownlink;

    /** Does this constant represent an integrated application? */
    private final boolean _isIntegrated;

    /** Does this constant represent a monitor application? */
    private final boolean _isMonitor;


    /**
     * Private constructor.
     *
     * @param isFswDownlink True for FSW downlink application
     * @param isUplink      True for uplink application
     * @param isSseDownlink True for SSE downlink application
     * @param isIntegrated  True for integrated application
     * @param isMonitor     True for monitor application
     */
    private ModeEnum(final boolean isFswDownlink,
                     final boolean isUplink,
                     final boolean isSseDownlink,
                     final boolean isIntegrated,
                     final boolean isMonitor)
    {
        _isFswDownlink = isFswDownlink;
        _isUplink      = isUplink;
        _isSseDownlink = isSseDownlink;
        _isIntegrated  = isIntegrated;
        _isMonitor     = isMonitor;

        final int count = ((_isFswDownlink ? 1 : 0) +
                           (_isUplink      ? 1 : 0) +
                           (_isSseDownlink ? 1 : 0) +
                           (_isIntegrated  ? 1 : 0) +
                           (_isMonitor     ? 1 : 0));
        if (count != 1)
        {
            throw new IllegalArgumentException("ModeEnum must set one mode");
        }
    }


    /** Values as a sorted set */
    public static final Set<ModeEnum> MODES =
        Collections.unmodifiableSet(
            EnumSet.<ModeEnum>copyOf(Arrays.asList(values())));


    /**
     * Get number of elements.
     *
     * @return Size
     */
    public static int size()
    {
        return MODES.size();
    }


    /**
     * Does this constant represent a FSW downlink application?
     *
     * @return True if a FSW downlink application
     */
    public boolean isFswDownlink()
    {
        return _isFswDownlink;
    }


    /**
     * Does this constant represent an uplink application?
     *
     * @return True if a uplink application
     */
    public boolean isUplink()
    {
        return _isUplink;
    }


    /**
     * Does this constant represent a SSE downlink application?
     *
     * @return True if a SSE downlink application
     */
    public boolean isSseDownlink()
    {
        return _isSseDownlink;
    }


    /**
     * Does this constant represent an integrated application?
     *
     * @return True if an integrated application
     */
    public boolean isIntegrated()
    {
        return _isIntegrated;
    }


    /**
     * Does this constant represent a monitor application?
     *
     * @return True if a monitor application
     */
    public boolean isMonitor()
    {
        return _isMonitor;
    }
}
