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
package jpl.gds.tc.impl.message;

import jpl.gds.shared.message.IMessageType;
import jpl.gds.tc.api.message.ICommandMessage;

/**
 * Abstract superclass for all uplink message types that are commands.  This goes for
 * the following types of uplink:
 * 
 *  FSW Commands
 *  HW Commands
 *  Sequence Directives
 *  SSE Commands
 *  
 *
 */
public abstract class AbstractCommandMessage extends AbstractUplinkMessage implements ICommandMessage
{
	/** The string representation of this command as entered by the user */
	protected String commandString;
	
	/**
	 * Initialize this message
	 * 
	 * @param type The internal message type
	 */
	public AbstractCommandMessage(final IMessageType type)
	{
		super(type);
		
		this.commandString = null;
	}

	/**
	 * Initialize this message
	 * 
	 * @param type The internal message type 
	 * @param commandString The string representation of the command as entered by the user
	 */
	public AbstractCommandMessage(final IMessageType type, final String commandString)
	{
		this(type);
		
		this.commandString = commandString;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.message.ICommandMessage#setCommandString(java.lang.String)
     */
	@Override
    public void setCommandString(final String commandString)
	{
		this.commandString = commandString;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.message.ICommandMessage#getCommandString()
     */
	@Override
    public String getCommandString()
	{
		return commandString;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.message.ICommandMessage#getDatabaseString()
     */
    @Override
    public String getDatabaseString()
	{
		return(this.commandString != null ? this.commandString : "");
	}
}
