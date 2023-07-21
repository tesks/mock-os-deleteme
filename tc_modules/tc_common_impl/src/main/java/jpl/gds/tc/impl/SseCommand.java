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
package jpl.gds.tc.impl;

import org.springframework.context.ApplicationContext;

import jpl.gds.tc.api.command.ISseCommand;
import jpl.gds.tc.api.config.CommandProperties;

/**
 * 
 * A command to be transmitted to the Simulation & Support Equipment (SSE).
 * Currently SSE has no command dictionary, so no real processing or validation
 * is done on SSE commands. There is no associated command definition object.
 * They are simply transmitted as an ASCII string.
 * 
 *
 * 6/23/14 - MPCS-6304. Implement ICommand instead of extending
 *          AbstractCommand.
 */
public class SseCommand implements ISseCommand {
    /** The ASCII string representing the SSE command */
    protected String commandString;
    protected ApplicationContext appContext;


    /**
     * Default constructor
     *
     * This constructor should not be used, even within this class.
     * Use the CommandObjectFactory instead. If you feel you really must
     * use it, justify it and get permission.
     *
     * It's protected so that CommandObjectFactory can use it, but not
     * everybody. That's only partial protection, hence these comments.
     *
     * @param appContext App context
     */
    protected SseCommand(final ApplicationContext appContext)
    {
        /*
         * 11/5/13 - MPCS-5521. Must now set the command definition type
         * in the superclass.
         */
        super();
        
        this.appContext = appContext;

        this.commandString = "";
    }


    /**
     * Overloaded constructor
     *
     * This constructor should not be used, even within this class.
     * Use the CommandObjectFactory instead. If you feel you really must
     * use it, justify it and get permission.
     *
     * It's protected so that CommandObjectFactory can use it, but not
     * everybody. That's only partial protection, hence these comments.
     *
     * @param appContext App context
     * @param cmdString
     *            The string representing the command to send
     *
     */
    protected SseCommand(final ApplicationContext appContext, final String cmdString) {

        this(appContext);

        setCommandString(cmdString);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setCommandString(final String cmdString) {

        if (cmdString == null) {
            throw new IllegalArgumentException("Null input command string");
        } else if (cmdString.length() < 1) {
            throw new IllegalArgumentException("Empty input command string");
        } else if (ISseCommand.isSseCommand(appContext.getBean(CommandProperties.class), cmdString) == false) {
            throw new IllegalArgumentException(
                    "The input string does not start with the proper SSE prefix: "
                            + appContext.getBean(CommandProperties.class).getSseCommandPrefix()
                            + "(this may also indicate that the"
                            + " SSE command prefix is not properly defined in the configuration)");
        }

        this.commandString = cmdString;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCommandString(final boolean includePrefix) {

        if (includePrefix == true) {
            return (this.commandString);
        }

        return (this.commandString.substring(appContext.getBean(CommandProperties.class)
                .getSseCommandPrefix().length()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tc.api.command.IDatabaseArchivableCommand#getDatabaseString()
     */
    @Override
    public String getDatabaseString() {

        return (getCommandString(false));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        return (getCommandString(true));
    }

}
