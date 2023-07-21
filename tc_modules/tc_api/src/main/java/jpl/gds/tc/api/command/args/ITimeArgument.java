/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.tc.api.command.args;

/**
 * This class is the interface for functionality that is specific to time arguments for commands
 *
 */
public interface ITimeArgument extends ICommandArgument {

    String SCLK = "SCLK";
    String SCET = "SCET";
    String LST  = "LST";
    /**
     * Get the time argument in a format that is accepted by the CTS library
     * @return a time value in a CTS acceptable format
     */
    String getUplinkValue();
}
