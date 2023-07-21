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
package jpl.gds.message.api.options;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.ParseException;

import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.FlagOptionParser;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.cli.options.ICommandLineOptionsGroup;
import jpl.gds.shared.cli.options.PortOption;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.StringOptionParser;
import jpl.gds.shared.cli.options.numeric.UnsignedIntOptionParser;
import jpl.gds.shared.holders.PortHolder;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * This class creates command line option objects used for parsing message topic
 * options and automatically setting the parsed values into a
 * MessageServiceConfiguration object. Once an instance of this class is
 * constructed, it provides public members for each defined option, which can be
 * individually added to a class that extends BaseCommandOptions and can be
 * individually parsed by an application. Alternatively, there are convenience
 * methods to get or parse collections of options.
 */
public class MessageServiceCommandOptions implements ICommandLineOptionsGroup {
    private static final UnsignedInteger MIN_PORT = UnsignedInteger.valueOf(
            PortHolder.MIN_VALUE);

    private static final UnsignedInteger MAX_PORT = UnsignedInteger.valueOf(
            PortHolder.MAX_VALUE);
    
    // constants for option names */
    /** Long name for database host option */
    public static final String JMS_HOST_LONG = "jmsHost";
    /** Long name for database port option */
    public static final String JMS_PORT_LONG = "jmsPort";
    /** Long name for no JMS option */
    public static final String NO_JMS_LONG = "noJMS";

    private final MessageServiceConfiguration jmsConfig;

    /**
     * The NO_JMS command option. Parsing this option sets the "use message service" flag in
     * the MessageServiceConfiguration member instance.
     */
    public final FlagOption NO_JMS = new FlagOption("J", NO_JMS_LONG,
            "execute without using the Messaging Service.", false);

    /**
     * The JMS_HOST command option. Parsing this option sets the "message service host"
     * property in the MessageServiceConfiguration member instance.
     */
    public final StringOption JMS_HOST = new StringOption(null, JMS_HOST_LONG,
            "hostname", "host where the message server is running.", false);

    /**
     * The JMS_PORT command option. Parsing this option sets the "message service port"
     * property in the MessageServiceConfiguration member instance.
     */
    public final PortOption JMS_PORT = new PortOption(null, JMS_PORT_LONG, "port",
            "port on which the message server is listening.", false);
    
    /**
     * Constructor that takes a unique instance of MessageServiceConfiguration. The
     * supplied MessageServiceConfiguration will be used both to determine defaults, and to
     * set parsed values into.
     * 
     * @param config
     *            the MessageServiceConfiguration instance to use
     */
    public MessageServiceCommandOptions(final MessageServiceConfiguration config) {
        this.jmsConfig = config;

        NO_JMS.setParser(new NoJmsOptionParser());
        NO_JMS.addAlias("noMessaging");
        JMS_HOST.setParser(new JmsHostOptionParser());
        JMS_HOST.addAlias("messagingHost");
        JMS_PORT.setParser(new JmsPortOptionParser());
        JMS_PORT.addAlias("messagingPort");
    }

    /**
     * Gets the MessageServiceConfiguration member object.
     * 
     * @return MessageServiceConfiguration; never null
     */
    public MessageServiceConfiguration getMessageServiceConfiguration() {
        return this.jmsConfig;
    }

    /**
     * Gets a Collection containing all command line options defined by this
     * class.
     * 
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllOptions() {
        final Set<ICommandLineOption<?>> result = new TreeSet<ICommandLineOption<?>>();
        result.add(JMS_HOST);
        result.add(JMS_PORT);
        result.add(NO_JMS);
        return result;
    }

    /**
     * Gets a Collection containing all command line options defined by this
     * class, less the NO_JMS option (which would be meaningless for monitoring
     * utilities, for instance).
     * 
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllOptionsWithoutNoJms() {
        final Set<ICommandLineOption<?>> result = new TreeSet<ICommandLineOption<?>>();
        result.add(JMS_HOST);
        result.add(JMS_PORT);

        return result;
    }

    /**
     * Parses all the options defined by this class from the supplied command
     * line object. Does not require any of the options, and supplies no
     * defaults. Result values are set into the MessageServiceConfiguration member. Any
     * option not present on the supplied command line is effectively ignored.
     * 
     * @param commandLine
     *            the parsed command line options
     * @throws ParseException
     *             if there is a parse error
     */
    public void parseAllOptionsAsOptional(final ICommandLine commandLine)
            throws ParseException {
        JMS_HOST.parse(commandLine);
        JMS_PORT.parse(commandLine);
        NO_JMS.parse(commandLine);
    }

    /**
     * An option parser class for the NO_JMS option. The parsed value is set
     * into the MessageServiceConfiguration.
     */
    protected class NoJmsOptionParser extends FlagOptionParser {

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.FlagOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public Boolean parse(final ICommandLine commandLine,
                final ICommandLineOption<Boolean> opt) throws ParseException {

            final Boolean noJms = super.parse(commandLine, opt);

            if (noJms != null && noJms) {
                jmsConfig.setUseMessaging(false);
            }

            return noJms;
        }
    }

    /**
     * An option parser class for the JMS_HOST option. The parsed value is set
     * into the MessageServiceConfiguration.
     */
    protected class JmsHostOptionParser extends StringOptionParser {

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {

            final String name = super.parse(commandLine, opt);

            if (name != null) {
                jmsConfig.setMessageServerHost(name);
            }

            return name;
        }
    }

    /**
     * An option parser class for the JMS_PORT option. The parsed value is set
     * into the MessageServiceConfiguration.
     */
    protected class JmsPortOptionParser extends UnsignedIntOptionParser {

        /**
         * Constructor
         */
        public JmsPortOptionParser() {
            super(MIN_PORT, MAX_PORT);
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.numeric.UnsignedIntOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public UnsignedInteger parse(final ICommandLine commandLine,
                final ICommandLineOption<UnsignedInteger> opt) throws ParseException {

            final UnsignedInteger port = super.parse(commandLine, opt);

            if (port != null) {
                jmsConfig.setMessageServerPort(port.intValue());
            }

            return port;

        }

    }

}
