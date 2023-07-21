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

import jpl.gds.shared.string.StringUtil;


/**
 * Conditions that apply for parameter analysis: context.
 *
 * The enableUp and enableSse specify whether or not uplink
 * or SSE is configured. That's mainly for integrated mode.
 *
 * The mode is really just the application type.
 *
 */
public final class State extends AbstractParameter
{

    private static final String ME             = "State: ";
    private static final String MODE_CANNOT_BE = "Mode cannot be ";
    private static final String ENABLED        = "enabled";

    private final String               _mission;
    private final ModeEnum             _mode;
    private final AllowUplinkBool      _enableUp;
    private final AllowSseDownlinkBool _enableSse;
    private final GuiBool              _gui;
    private final AutorunBool          _autorun;

    private final boolean _allowedForFswDownlink;
    private final boolean _allowedForSseDownlink;
    private final boolean _allowedForUplink;
    private final boolean _allowedForMonitor;


    /**
     * Constructor.
     *
     * @param mission   Mission
     * @param enableUp  Enable uplink?
     * @param enableSse Enable SSE downlink?
     * @param mode      Application
     * @param gui       True if a GUI is running
     * @param autorun   True if autorun is enabled
     *
     * @throws ParameterException On invalid state combination
     */
    public State(final String               mission,
                 final AllowUplinkBool      enableUp,
                 final AllowSseDownlinkBool enableSse,
                 final ModeEnum             mode,
                 final GuiBool              gui,
                 final AutorunBool          autorun)
        throws ParameterException
    {
        super();

        _mission   = checkNull(ME,
                               StringUtil.emptyAsNull(mission),
                               "Mission");
        _enableUp  = checkNull(ME, enableUp,  "Enable UP");
        _enableSse = checkNull(ME, enableSse, "Enable SSE");
        _gui       = checkNull(ME, gui,       "GUI");
        _autorun   = checkNull(ME, autorun,   "Autorun");

        ModeEnum tempMode = checkNull(ME, mode, "Mode");

        if (! _enableSse.get() && tempMode.isSseDownlink())
        {
            throw new ParameterException(ME             +
                                         MODE_CANNOT_BE +
                                         tempMode       +
                                         " because SSE is not enabled");
        }

        if (! _enableUp.get() && tempMode.isUplink())
        {
            throw new ParameterException(ME             +
                                         MODE_CANNOT_BE +
                                         tempMode       +
                                         " because uplink is not enabled");
        }

        // For every INTEGRATED* except INTEGRATED, see if it is compatible
        // with the enabled SSE and uplink states. For INTEGRATED, set it to
        // the correct mode based on those states.

        _mode = checkMode(tempMode, _enableUp, _enableSse);

        // From this point down, if we are SSE downlink or uplink that is OK

        _allowedForFswDownlink = (_mode.isFswDownlink() ||
                                  _mode.isIntegrated());

        _allowedForSseDownlink = (_mode.isSseDownlink() ||
                                  (_mode.isIntegrated() && _enableSse.get()));

        _allowedForUplink = (_mode.isUplink() ||
                             (_mode.isIntegrated() && _enableUp.get()));

        _allowedForMonitor = (_mode.isMonitor() || _mode.isIntegrated());
    }


    /**
     * Check the mode and perhaps change it to something else.
     *
     * For every INTEGRATED* except INTEGRATED, see if it is compatible
     * with the enabled SSE and uplink states. For INTEGRATED, set it to
     * the correct mode based on those states.
     *
     * @param mode      Application
     * @param enableUp  Enable uplink?
     * @param enableSse Enable SSE downlink?
     *
     * @return New mode (or original mode)
     *
     * @throws ParameterException On invalid state combination
     */
    private static ModeEnum checkMode(final ModeEnum             mode,
                                      final AllowUplinkBool      enableUp,
                                      final AllowSseDownlinkBool enableSse)
        throws ParameterException
    {
        ModeEnum result = mode;

        switch (result)
        {
            case INTEGRATED_FSW_ONLY:
                if (enableSse.get() || enableUp.get())
                {
                    throw new ParameterException(ME                 +
                                                 MODE_CANNOT_BE     +
                                                 result             +
                                                 " because SSE or " +
                                                 "uplink is enabled");
                }

                break;

            case INTEGRATED_FSW_SSE:
                if (! enableSse.get())
                {
                    throw new ParameterException(ME                     +
                                                 MODE_CANNOT_BE         +
                                                 result                 +
                                                 " because SSE is not " +
                                                 ENABLED);
                }

                if (enableUp.get())
                {
                    throw new ParameterException(ME                    +
                                                 MODE_CANNOT_BE        +
                                                 result                +
                                                 " because uplink is " +
                                                 ENABLED);
                }

                break;

            case INTEGRATED_FSW_UP:
                if (enableSse.get())
                {
                    throw new ParameterException(ME                 +
                                                 MODE_CANNOT_BE     +
                                                 result             +
                                                 " because SSE is " +
                                                 ENABLED);
                }

                if (! enableUp.get())
                {
                    throw new ParameterException(ME                        +
                                                 MODE_CANNOT_BE            +
                                                 result                    +
                                                 " because uplink is not " +
                                                 ENABLED);
                }

                break;

            case INTEGRATED_FSW_SSE_UP:
                if (! enableSse.get() || ! enableUp.get())
                {
                    throw new ParameterException(ME                     +
                                                 MODE_CANNOT_BE         +
                                                 result                 +
                                                 " because SSE and "    +
                                                 "uplink are not both " +
                                                 ENABLED);
                }

                break;

            case INTEGRATED:
                if (enableSse.get())
                {
                    result = (enableUp.get()
                                    ? ModeEnum.INTEGRATED_FSW_SSE_UP
                                    : ModeEnum.INTEGRATED_FSW_SSE);
                }
                else
                {
                    result = (enableUp.get()
                                    ? ModeEnum.INTEGRATED_FSW_UP
                                    : ModeEnum.INTEGRATED_FSW_ONLY);
                }

                break;

            default:
                break;
        }

        return result;
    }


    /**
     * Getter for mission.
     *
     * @return Mission
     */
    public String getMission()
    {
        return _mission;
    }


    /**
     * Getter for allow SSE downlink.
     *
     * @return SSE downlink bool
     */
    public AllowSseDownlinkBool getEnableSse()
    {
        return _enableSse;
    }


    /**
     * Getter for enabled uplink.
     *
     * @return Uplink bool
     */
    public AllowUplinkBool getEnableUplink()
    {
        return _enableUp;
    }


    /**
     * Getter for mode.
     *
     * @return Mode
     */
    public ModeEnum getMode()
    {
        return _mode;
    }


    /**
     * Getter for GUI.
     *
     * @return GUI
     */
    public GuiBool getGui()
    {
        return _gui;
    }


    /**
     * Getter for autorun.
     *
     * @return Autorun state
     */
    public AutorunBool getAutorun()
    {
        return _autorun;
    }


    /**
     * True if FSW downlink
     *
     * @return True if allowed
     */
    public boolean getAllowedForFswDownlink()
    {
        return _allowedForFswDownlink;
    }


    /**
     * True if SSE downlink
     *
     * @return True if allowed
     */
    public boolean getAllowedForSseDownlink()
    {
        return _allowedForSseDownlink;
    }


    /**
     * True if uplink is allowed
     *
     * @return True if allowed
     */
    public boolean getAllowedForUplink()
    {
        return _allowedForUplink;
    }


    /**
     * True if monitor is allowed.
     *
     * @return True if allowed
     */
    public boolean getAllowedForMonitor()
    {
        return _allowedForMonitor;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("State is");

        sb.append(" Mission:").append(getMission());
        sb.append(",Mode:").append(getMode());
        sb.append(",GUI:").append(getGui());
        sb.append(",SSE:").append(getEnableSse());
        sb.append(",Uplink:").append(getEnableUplink());
        sb.append(",FSW Downlink?:").append(getAllowedForFswDownlink());
        sb.append(",SSE Downlink?:").append(getAllowedForSseDownlink());
        sb.append(",Uplink?:").append(getAllowedForUplink());
        sb.append(",Monitor?:").append(getAllowedForMonitor());

        return sb.toString();
    }
}
