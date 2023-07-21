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
package jpl.gds.tc.api;

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.string.StringUtil;


/**
 * An enumeration of the different types of command statuses
 * that will be stored in the CommandStatus.status column.
 *
 * Any state marked final is one of the possible last states expected to be
 * received for any given request-id.
 *
 *
 * MPCS-10532 - 05/16/19 - Added Submitted for Send File CFDP Put requests
 */
public enum CommandStatusType
{
    /** Unknown catch-all state -- Orchid*/
    UNKNOWN(false),

    /** Requested -- Blue */
    Requested(false),
    
    /** Submitted -- Blue */
    Submitted(false),

    /** Standby -- Gray */
    Standby(false),

    /** Ready -- White */
    Ready(false),

    /** Radiating -- Light green */
    Radiating(false),

    /** Radiated (final) -- Green */
    Radiated(true),

    /** Failed -- Orange */
    Failed(false),

    /** Awaiting confirmation -- Honeydew */
    Awaiting_Confirmation(false),

    /** Windows expired (final) -- Red */
    Windows_Expired(true),

    /** Radiation attempts exceeded (final) -- Red */
    Rad_Attempts_Exceeded(true),

    /** Received (final) -- Green */
    Received(true),

    /** Deleted (final) -- Brown */
    Deleted(true),

    /** Corrupted (final) -- Red */
    Corrupted(true),

    /** Send failure (final) -- Red */
    Send_Failure(true);

    private static final Tracer logger = TraceManager.getDefaultTracer();


    private final boolean _final;


    /**
     * Constructor.
     *
     * @param isFinal True means a final state
     */
    private CommandStatusType(final boolean isFinal)
    {
        _final = isFinal;
    }


    /**
     * Get final status.
     *
     * @return True if a final state
     */
    public boolean isFinal()
    {
        return _final;
    }


    /**
     * Safely convert string to value.
     *
     * @param s String to convert
     *
     * @return String as enum value
     */
    public static CommandStatusType valueOfIgnoreCase(final String s)
    {
        final String candidate = StringUtil.safeTrim(s);

        for (final CommandStatusType type : CommandStatusType.values())
        {
            if (type.toString().equalsIgnoreCase(candidate))
            {
                return type;
            }

        }

        logger.warn("No "                             +
                    CommandStatusType.class.getName() +
                    " enum value matches '"           +
                    candidate                         +
                    "'. Using "                       +
                    CommandStatusType.UNKNOWN);

        return CommandStatusType.UNKNOWN;
    }
}
