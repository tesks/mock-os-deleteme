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
package jpl.gds.globallad.spring.cli;

import static jpl.gds.globallad.spring.beans.BeanNames.GLAD_COMMAND_LINE_OVERRIDES;

import java.io.PrintWriter;

import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.globallad.io.IGlobalLadDataSource;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.options.MessageServiceCommandOptions;
import jpl.gds.shared.types.UnsignedInteger;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.options.GlobalLadOptions;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.context.cli.app.mc.AbstractRestfulServerCommandLineApp;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Tracer;

/**
 * Command line parser and storage used in the global lad application when 
 * started from the command line.    
 */
@Component(value=GLAD_COMMAND_LINE_OVERRIDES)
public class GlobalLadCommandLineParser extends AbstractRestfulServerCommandLineApp {
	/**
	 * MPCS-7879 triviski 2/10/2016 - Spring does it's own command line parsing
	 * and it is not compatible with the apache CLI that is used for MPCS.  This 
	 * turned into a middle ware that will use the application arguments and 
	 * removes all of the Spring arguments to be processed.  This will then 
	 * process the args with the MPCS method and set the overrides.  This 
	 * bean can then be injected into any other bean that needs these 
	 * override values.
	 * 
	 */

    /** The socket server port for chill down to connect to. */
    private Integer                   socketServerPort;

    public String backupFile;
    
    public boolean restoreFromBackup;

    private final GlobalLadProperties gladConfig;
    private final MessageServiceCommandOptions msgCmdOpts;

    /**
     * Autowired for Spring bean creation.
     * 
     * @param args
     *            Spring application arguments.
     * @param gladConfig
     *            GlobalLad configuration
     * @param msgSvcConfig
     *            Message service configuration
     * @throws Exception
     */
    @Autowired
    public GlobalLadCommandLineParser(final ApplicationArguments args, final GlobalLadProperties gladConfig,
                                      final MessageServiceConfiguration msgSvcConfig)
            throws Exception {
    		/**
    		 * MPCS-8064 triviski 4/22/2016 - I attempted to merge both the spring option parsing 
    		 * with the MPCS option parsing, and I failed.  I tried to use the args.getNonOptionArgs, but this
    		 * only got the short arguments.  So now I am just passing in the raw command line arguments
    		 * and the MPCS version will barf if there are any unrecognized arguments.  What does this mean?
    		 * No spring arguments can be given to this application. 
    		 */
        this(args.getSourceArgs(), gladConfig.getTracer(), gladConfig, msgSvcConfig);
    }

	/**
     * @param args
     *            arguments to parse
     * @param logger
     *            the logger.
     * @param gladConfig
     *            GlobalLad configuration
     * @throws Exception
     */
    public GlobalLadCommandLineParser(final String[] args, final Tracer logger, final GlobalLadProperties gladConfig,
                                      final MessageServiceConfiguration msgSvcConfig)
            throws Exception {
		super();
        this.gladConfig = gladConfig;
        this.msgCmdOpts = new MessageServiceCommandOptions(msgSvcConfig);
        try {
            final ICommandLine commandLine = createOptions()
                    .parseCommandLine(args, true);
            configure(commandLine);
		} catch (final ParseException e) {
            logger.error("Failed to configure command line options ", ExceptionTools.getMessage(e));
			showHelp();
            System.exit(ICommandLineOption.COMMAND_LINE_ERROR);
		}
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public BaseCommandOptions createOptions() {
        /* MPCS-9376: Remove super.createOptions() to use glad legacy restPort option */
        if (optionsCreated.getAndSet(true)) {
            return options;
        }
        options = new BaseCommandOptions(this, true);
        options.addHelpOption();
        options.addVersionOption();

        GlobalLadOptions.REST_SERVER_PORT_OPTION.setDefaultValue(gladConfig.getRestPort());
        options.addOption(GlobalLadOptions.REST_SERVER_PORT_OPTION);

        GlobalLadOptions.SOCKET_SERVER_PORT_OPTION.setDefaultValue(gladConfig.getSocketServerPort());
        options.addOption(GlobalLadOptions.SOCKET_SERVER_PORT_OPTION);

        options.addOption(GlobalLadOptions.RESTORE_FILE_OPTION);
        options.addOption(GlobalLadOptions.DO_RESTORE_OPTION);
        options.addOption(restOptions.REST_INSECURE_OPTION);

        options.addOption(GlobalLadOptions.MODE_OPTION);
        GlobalLadOptions.JMS_TOPICS_OVERRIDE_OPTION.setDefaultValue(null);
        options.addOption(GlobalLadOptions.JMS_TOPICS_OVERRIDE_OPTION);
        options.addOption(GlobalLadOptions.JMS_VENUE_TYPE_OPTION);
        options.addOption(GlobalLadOptions.JMS_DOWNLINK_STREAM_TYPE_OPTION);
        options.addOption(GlobalLadOptions.JMS_TESTBED_NAME_OPTION);
        options.addOption(GlobalLadOptions.JMS_HOST_NAME_OPTION);
        options.addOption(msgCmdOpts.JMS_HOST);
        options.addOption(msgCmdOpts.JMS_PORT);

        return options;
    }
    
    private String getUsage() {
        /**
         * MPCS-7990 triviski 4/25/2016 - Fixed usage statements.
         */
        final StringBuilder b = new StringBuilder(ApplicationConfiguration.getApplicationName() + " ");
        b.append(String.format("[--%s <restPort>] ", GlobalLadOptions.REST_SERVER_PORT_OPTION.getLongOpt()));
        b.append(String.format("[--%s <socketServerPort>] ", GlobalLadOptions.SOCKET_SERVER_PORT_OPTION.getLongOpt()));
        b.append(String.format("[--%s] ", GlobalLadOptions.MODE_OPTION.getLongOpt()));
        b.append(String.format("[--%s <topic[,topic...]>] ", GlobalLadOptions.JMS_TOPICS_OVERRIDE_OPTION.getLongOpt()));
        b.append(String.format("[--%s <string>] ", GlobalLadOptions.RESTORE_FILE_OPTION.getLongOpt()));
        b.append(String.format("[--%s] ", GlobalLadOptions.DO_RESTORE_OPTION.getLongOpt()));
        b.append(String.format("[--%s] ", restOptions.REST_INSECURE_OPTION.getLongOpt()));
        b.append(String.format("[--%s] ", GlobalLadOptions.JMS_VENUE_TYPE_OPTION.getLongOpt()));
        b.append(String.format("[--%s] ", GlobalLadOptions.JMS_TESTBED_NAME_OPTION.getLongOpt()));
        b.append(String.format("[--%s] ", GlobalLadOptions.JMS_DOWNLINK_STREAM_TYPE_OPTION.getLongOpt()));
        b.append(String.format("[--%s] ", GlobalLadOptions.JMS_HOST_NAME_OPTION.getLongOpt()));
        b.append(String.format("[--%s] ", msgCmdOpts.JMS_HOST.getLongOpt()));
        b.append(String.format("[--%s] ", msgCmdOpts.JMS_PORT.getLongOpt()));

        return b.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        /* MPCS-9376: Remove super.configure() to use glad legacy restPort option */
        final BaseCommandOptions optionsParser = createOptions();

        optionsParser.getHelpOption().parseWithExit(commandLine, false, true);
        optionsParser.getVersionOption().parseWithExit(commandLine, false, true);
        setCommandLine(commandLine);

        /*
         * HTTPS must be enabled in the configuration and not disabled on the command line for it to be active.
         * Need to parse restInsecureOption explicitly because we are not calling super.configure()
         */
        restIsSecure = gladConfig.isHttpsEnabled() && !restOptions.REST_INSECURE_OPTION.parse(commandLine); // allow disabling HTTPS
        gladConfig.setHttpsEnabled(restIsSecure);
        if (restOptions.REST_INSECURE_OPTION.parse(commandLine)) {
            restIsSecure = false;
            gladConfig.setHttpsEnabled(false);
        }

        /**
         * This will get the arguments from the command line if set and override in the
         * global lad configuration bean.
         */
        restPort = GlobalLadOptions.REST_SERVER_PORT_OPTION.parseWithDefault(commandLine, false, true);
        socketServerPort = GlobalLadOptions.SOCKET_SERVER_PORT_OPTION.parseWithDefault(commandLine, false, true);
        restoreFromBackup = GlobalLadOptions.DO_RESTORE_OPTION.parse(commandLine);
        backupFile = restoreFromBackup ? 
    				GlobalLadOptions.RESTORE_FILE_OPTION.parse(commandLine) : null;

        /**
         * MPCS-12089  - Configure JMS topic, JMS or socket server mode overrides
         */
        final String jmsTopics = GlobalLadOptions.JMS_TOPICS_OVERRIDE_OPTION.parseWithDefault(commandLine, false, true);
        if (jmsTopics != null) {
            gladConfig.setJmsRootTopics(jmsTopics);
        }

        final IGlobalLadDataSource.DataSourceType serverMode = GlobalLadOptions.MODE_OPTION.parse(commandLine, false);
        if (serverMode != null) {
            gladConfig.setDataSource(serverMode);
        }

        final VenueType venueType = GlobalLadOptions.JMS_VENUE_TYPE_OPTION.parse(commandLine, false);
        if (venueType != null) {
            gladConfig.setVenueType(venueType);
        }
        final String testbedName = GlobalLadOptions.JMS_TESTBED_NAME_OPTION.parse(commandLine, false);
        if (testbedName != null) {
            gladConfig.setTestbedName(testbedName);
        }
        final DownlinkStreamType downlinkStreamType = GlobalLadOptions.JMS_DOWNLINK_STREAM_TYPE_OPTION
                .parse(commandLine, false);
        if (downlinkStreamType != null) {
            gladConfig.setDownlinkStreamType(downlinkStreamType);
        }
        final String jmsHostName = GlobalLadOptions.JMS_HOST_NAME_OPTION.parse(commandLine, false);
        if (jmsHostName != null) {
            gladConfig.setJmsHostName(jmsHostName);
        }

        msgCmdOpts.JMS_HOST.parse(commandLine, false);
        msgCmdOpts.JMS_PORT.parse(commandLine, false);

    }

    /**
     * Gets the configured server socket port for GlobalLad
     * 
     * @return server socket port
     */
    public Integer getSocketServerPort() {
        return socketServerPort;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        /* MPCS-7750 - 10/23/15. Get nested Options object */
        final OptionSet options = createOptions().getOptions();

        final PrintWriter pw = new PrintWriter(System.out);
        
        pw.println(getUsage());

        pw.println("                   ");
        options.printOptions(pw);
        pw.flush();
    }
}
