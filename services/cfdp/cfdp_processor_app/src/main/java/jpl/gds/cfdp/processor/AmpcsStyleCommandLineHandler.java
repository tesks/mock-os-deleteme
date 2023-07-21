/*
 * Copyright 2006-2020. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */
package jpl.gds.cfdp.processor;

import jpl.gds.cfdp.common.config.EConfigurationPropertyKey;
import jpl.gds.cfdp.processor.ampcs.properties.CfdpProcessorAmpcsProperties;
import jpl.gds.cfdp.processor.config.ConfigurationLoader;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.util.CfdpFileUtil;
import jpl.gds.cfdp.processor.error.MissingRequiredPropertiesException;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.common.options.DownlinkStreamTypeOption;
import jpl.gds.common.options.SubtopicOption;
import jpl.gds.common.options.TestbedNameOption;
import jpl.gds.common.options.VenueTypeOption;
import jpl.gds.context.cli.app.mc.AbstractRestfulServerCommandLineApp;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.cli.options.CommandLineOption;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.cli.options.numeric.UnsignedLongOption;
import jpl.gds.shared.config.OrderedProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.types.UnsignedLong;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import static jpl.gds.context.api.options.RestCommandOptions.REST_PORT_LONG;

/**
 * Command line handler
 *
 * MPCS-11566: Moved from CfdpProcessorApp and extended AbstractRestfulServerCommandLineApp
 */
public class AmpcsStyleCommandLineHandler extends AbstractRestfulServerCommandLineApp {

    /** config file short option */
    public static final String CONFIG_FILE_SHORT = "c";

    /** config file long option */
    public static final String CONFIG_FILE_LONG = "configFile";

    /** local entity ID short option */
    public static final String LOCAL_CFDP_ENTITY_ID_SHORT = "l";

    /** local entity ID long option */
    public static final String LOCAL_CFDP_ENTITY_ID_LONG = "localCfdpEntityId";

    /** cfdp original port long option */
    public static final String CFDP_OLD_PORT_LONG = "port";

    private final VenueTypeOption          venueTypeOption          = new VenueTypeOption(false);
    private final TestbedNameOption        testbedNameOption        = new TestbedNameOption(false);
    private final DownlinkStreamTypeOption downlinkStreamTypeOption = new DownlinkStreamTypeOption(false);
    private final SubtopicOption           subtopicOption           = new SubtopicOption(false);

    private final Map<String, CommandLineOption<?>> optionMap;

    private final CfdpProcessorAmpcsProperties ampcsProperties;
    private final Tracer                       log;
    private String     configFile;
    private Properties configProperties;
    private boolean    configLoadOk;

    // The four members below will be populated only if user provided their respective command-line options
    private  VenueType venueType   = null;
    private  String             testbedName        = null;
    private  DownlinkStreamType downlinkStreamType = null;
    private  String             subtopic           = null;

    /** Constructor */
    public AmpcsStyleCommandLineHandler() {
        log = TraceManager.getTracer(Loggers.CFDP);
        ampcsProperties = new CfdpProcessorAmpcsProperties();
        optionMap = new HashMap<>();
        optionMap.put(CONFIG_FILE_LONG,
                      new FileOption(CONFIG_FILE_SHORT, CONFIG_FILE_LONG, "path", "configuration file", false, true));

        optionMap.put(LOCAL_CFDP_ENTITY_ID_LONG, new UnsignedLongOption(LOCAL_CFDP_ENTITY_ID_SHORT,
                                                                        LOCAL_CFDP_ENTITY_ID_LONG, "id", "local CFDP entity ID", false));
        optionMap.put(VenueTypeOption.LONG_OPTION, venueTypeOption);
        optionMap.put(TestbedNameOption.LONG_OPTION, testbedNameOption);
        optionMap.put(DownlinkStreamTypeOption.LONG_OPTION, downlinkStreamTypeOption);
        optionMap.put(SubtopicOption.LONG_OPTION, subtopicOption);
    }

    @Override
    public BaseCommandOptions createOptions() {
        if (optionsCreated.get()) {
            return options;
        }
        super.createOptions();
        restOptions.REST_PORT_OPTION.addAlias(CFDP_OLD_PORT_LONG);

        for (final CommandLineOption<?> clo : optionMap.values()) {
            options.addOption(clo);
        }

        return options;
    }

    /**
     * Create common options
     * @return Options object
     */
    public Options createCommonsCliOptions() {
        final Options options = new Options();
        final OptionSet optionSet = createOptions().getOptions();

        for (final ICommandLineOption<?> clo : optionSet.getAllOptions()) {
            options.addOption(clo.getOpt(), clo.getLongOpt(), clo.hasArg(), clo.getDescription());
        }

        return options;
    }

    private void createAndPopulateNewConfigFile(final File f) throws IOException {
        final Properties properties = new OrderedProperties();

        EnumSet.allOf(EConfigurationPropertyKey.class)
               .forEach(key -> properties.setProperty(key.getFullPropertyKeyStr(),
                                                      ampcsProperties.getConfigFileInitProperty(key) != null ?
                                                              ampcsProperties.getConfigFileInitProperty(key) : ""));

        // MPCS-10634 4/9/19 Create directories if missing
        new CfdpFileUtil().createParentDirectoriesIfNotExist(f.getAbsolutePath());

        try (OutputStream fos = new FileOutputStream(f)) {
            final DateFormat dateFormatter = TimeUtility.getFormatterFromPool();
            properties.store(fos, "Auto-created " + dateFormatter.format(new Date())
                    + " using default application properties");
            TimeUtility.releaseFormatterToPool(dateFormatter);
        }

    }

    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        //will also parse REST port / restInsecure
        super.configure(commandLine);

        configFile = ampcsProperties.getWritableConfigFile();

        final String overriddenConfigFile = (String) optionMap.get(CONFIG_FILE_LONG).parse(commandLine);

        if (overriddenConfigFile != null) {
            configFile = overriddenConfigFile;
        }

        // MPCS-10233 - 10/1/2018 - If configuration file doesn't exist, create it
        final File f = new File(configFile);

        if (!f.exists() || !f.isFile()) {
            log.warn("Writable configuration file " + configFile + " doesn't exist");

            try {
                createAndPopulateNewConfigFile(f);
                log.info("Created a new writable configuration file " + configFile
                                 + " using default application properties");
            } catch (final IOException ie) {
                log.error("Failed to create new writable configuration file: " + configFile);
                log.error("Is the directory hierarchy writable? Or change the config file location via AMPCS " +
                                  "property 'cfdpProcessor.writable.config.file'");
                System.exit(1);
            }

        }

        // Load all properties from the writable configuration file

        try {

            configProperties = new ConfigurationLoader().load(configFile)
                                                        .confirmRequiredPropertiesLoaded(EConfigurationPropertyKey.getAllFullKeyStrings()).getProperties();
            configLoadOk = true;

            // Apply rest of user overrides
            if(commandLine.hasOption(REST_PORT_LONG)) {
                configProperties.put(EConfigurationPropertyKey.PORT_PROPERTY.toString(), restPort);
                configProperties.put(ConfigurationManager.USER_OVERRIDES_APPLIED_PROPERTY_NAME, true);
            }
            else{
                //set REST port from config
                setRestPort(Integer.parseInt(configProperties.getProperty(EConfigurationPropertyKey.PORT_PROPERTY.toString())));
            }

            final UnsignedLong overriddenLocalCfdpEntityId = (UnsignedLong) optionMap.get(LOCAL_CFDP_ENTITY_ID_LONG)
                                                                                     .parse(commandLine);

            if (overriddenLocalCfdpEntityId != null) {
                configProperties.put(EConfigurationPropertyKey.LOCAL_CFDP_ENTITY_ID_PROPERTY.toString(),
                                     overriddenLocalCfdpEntityId.toString());
                configProperties.put(ConfigurationManager.USER_OVERRIDES_APPLIED_PROPERTY_NAME, true);
            }

        } catch (final MissingRequiredPropertiesException mrpe) {
            TraceManager.getTracer(Loggers.CFDP).error(ExceptionTools.getMessage(mrpe));
        }

        venueType = venueTypeOption.parse(commandLine);
        testbedName = testbedNameOption.parse(commandLine);
        downlinkStreamType = downlinkStreamTypeOption.parse(commandLine);
        subtopic = subtopicOption.parse(commandLine);
    }

    /**
     * @return the configLoadOk
     */
    public boolean configLoadedOk() {
        return configLoadOk;
    }

    /**
     * @return the configFile
     */
    public String getConfigFile() {
        return configFile;
    }

    /**
     * @return the configProperties
     */
    public Properties getConfigProperties() {
        return configProperties;
    }

    @Override
    public void showHelp() {
        /*
         * MPCS-8798 caused help text to be displayed twice, so applying the new check
         * required to make sure it doesn't do that.
         */

        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        super.showHelp();
    }

    /**
     * Get venue type
     * @return venue type
     */
    public VenueType getVenueType() {
        return venueType;
    }

    /**
     * Get testbed name
     * @return testbed name
     */
    public String getTestbedName() {
        return testbedName;
    }

    /**
     * Get Downlink Stream Type
     * @return Downlink Stream Type
     */
    public DownlinkStreamType getDownlinkStreamType() {
        return downlinkStreamType;
    }

    /**
     * Get subtopic
     * @return subtopic
     */
    public String getSubtopic() {
        return subtopic;
    }
}