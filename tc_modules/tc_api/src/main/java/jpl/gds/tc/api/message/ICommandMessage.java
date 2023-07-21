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
package jpl.gds.tc.api.message;

public interface ICommandMessage extends IUplinkMessage {

    /**
     * Sets the command entered by the user that spawned this message.
     * @param commandString the command string to set
     */
    public void setCommandString(String commandString);

    /**
     * Gets the command entered by the user that spawned this message
     * 
     * @return command string
     */
    public String getCommandString();

    /**
     * {@inheritDoc}
     * @see jpl.gds.tc.api.command.IDatabaseArchivableCommand#getDatabaseString()
     */
    public String getDatabaseString();

}