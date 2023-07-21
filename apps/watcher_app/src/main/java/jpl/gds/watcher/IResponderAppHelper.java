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
package jpl.gds.watcher;

import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;

/**
 * This interface must be implemented by classes that cooperate with the
 * generic MessageReponderApp to build customized message handlers. It allows
 * command line options to be added to the customized application.
 */
public interface IResponderAppHelper {
    /**
     * Adds additional command line options to the customized message handler
     * beyond those supplied by the generic message responder application.
     * @param opt
     *            the BaseCommandOptions object to add additional command line options to
     */
    void addAppOptions(BaseCommandOptions opt);

    /**
     * Parses additional command line options beyond those parsed by the
     * generic message responder application.
     * @param commandLine
     *            the parsed CommandLine object
     * @throws ParseException
     *             if errors occur in command line processing
     */
    void configure(final ICommandLine commandLine) throws ParseException;

    /**
     * Gets additional help text to be displayed following the detailed
     * command line option help.
     * @return help text, or the empty string if none
     */
    String getAdditionalHelpText();

    /**
     * Gets a list of message types that should override subscribed types in
     * the config file.
     * @return an array of message types, or null if no overrides defined
     */
    String[] getOverrideTypes();

    /**
     * Gets the initial context configuration, as configured from the command
     * line.
     * @return context configuration object
     */
    IContextConfiguration getContextConfiguration();

    /**
     * Get usage text customized for the specific message responder application.
     * @return the usage text (not command line options) for the customized
     *         applications
     */
    String getUsageText();

    /**
     * Sets the initial session configuration, as configured from the command
     * line.
     * @param session
     *            object to configure
     */
    void setContextConfiguration(IContextConfiguration session);
    
    /**
     * Gets the ApplicationContext in use by this application helper.
     * @return  ApplicationContext
     */
    ApplicationContext getApplicationContext();
}
