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
package jpl.gds.tc.api.command;

import jpl.gds.tc.api.config.CommandProperties;

public interface ISseCommand extends ICommand {

    /**
     * Sets the string value of the command to send. If the command does not
     * begin with the configured SSE prefix, an exception will be thrown.
     * 
     * @param cmdString
     *            The commandString to set.
     */
    void setCommandString(String cmdString);

    /**
     * Accessor for the command string
     * 
     * @param includePrefix
     *            True if the return value should have the configured SSE prefix
     *            prepended (normally "sse:"), false otherwise.
     * 
     * @return Returns the string representation of the SSE command
     */
    String getCommandString(boolean includePrefix);

    /**
     * Determine whether or not the input command string is an SSE command. This
     * is determined by checking whether or not the input string begins with the
     * configured SSE prefix.
     * 
     * @param cmdString
     *            The command string to check
     * 
     * @return True if the input string is an SSE command, false otherwise
     */
    static boolean isSseCommand(final CommandProperties cmdConfig, final String cmdString) {
    
        if (cmdString == null) {
            throw new IllegalArgumentException("Null input command string");
        }
    
        final String ssePrefix = cmdConfig
                .getSseCommandPrefix();
        if (ssePrefix == null) {
            return (false);
        }
    
        return (cmdString.startsWith(ssePrefix));
    }

}