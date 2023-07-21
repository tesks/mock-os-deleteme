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
/**
 * Project:	AMMOS Mission Data Processing and Control System (MPCS)
 * Package:	jpl.gds.dictionary.impl.impl.impl.channel.app
 * File:	ChannelDictionaryParserApp.java
 *
 * Author:	Josh Choi (joshchoi)
 * Created:	Jun 26, 2012
 *
 */
package jpl.gds.app.tools.dictionary;

import static jpl.gds.shared.exceptions.ExceptionTools.rollUpMessages;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import jpl.gds.dictionary.api.ICategorySupport;
import org.apache.commons.cli.ParseException;
import org.apache.velocity.Template;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.dictionary.options.DictionaryCommandOptions;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDictionary;
import jpl.gds.dictionary.api.channel.IChannelDictionaryFactory;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.OutputFormatOption;
import jpl.gds.shared.cli.options.ShowColumnsOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.template.ApplicationTemplateManager;
import jpl.gds.shared.template.MissionConfiguredTemplateManagerFactory;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.template.TemplateManager;

/**
 * This is the main application class for the command line tool
 * chill_parse_chan_dict. It reads the default or the specified channel
 * dictionary using the mission-configured dictionary parser. It will then
 * output the loaded channel definitions to either the standard output or a
 * specified output file, using a template.
 * 
 * The primary purpose of this tool is to support chill_get_drf. So any changes
 * made to the behavior of this class should verify that the functionality of
 * chill_get_drf is not affected as a result.
 */
public class ChannelDictionaryParserApp extends AbstractCommandLineApp {

    /** Short filename option */
    public static final String OUTPUT_FILE_SHORT = "f";

    /** Long filename option */
    public static final String OUTPUT_FILE_LONG = "outputFilename";

    /** Output file append mode flag */
    public static final String APPEND_LONG = "append";

    /** Full path of FSW dictionary file to parse */
    public static final String FSW_DICTIONARY_FILE_LONG = "fswDictFile";    

    /** Full path of SSE dictionary file to parse */
    public static final String SSE_DICTIONARY_FILE_LONG = "sseDictFile";    

    /** Long option for FSW channel dictionary exclusion */
    public static final String EXCLUDE_FSW_LONG = "noFSW";

    /** Long option for SSE channel dictionary exclusion */
    public static final String EXCLUDE_SSE_LONG = "noSSE";

    /** Long option for header and monitor channel dictionaries exclusion */
    public static final String EXCLUDE_HEADER_AND_MONITOR_LONG = "noHM";

    /** Output template type */
    static final String OUTPUT_TEMPLATE_TYPE = "ChannelDictionary";

    private static final Tracer log = TraceManager.getDefaultTracer();


    /** Velocity template for output */
    private Template template;

    private String templateName;

    /** The output filename for converted dictionary */
    private String outputFilename;

    /** The FSW dictionary filename to parse */
    private String fswDictFilename;

    /** The SSE dictionary filename to parse */
    private String sseDictFilename;

    /** Flag to show column headings */
    private boolean showColumnHeaders;

    /** Flag that indicates FSW channel dictionary should be excluded */
    private boolean excludeFsw;

    /** Flag that indicates SSE channel dictionary should be excluded */
    private boolean excludeSse;

    /** Flag that indicates the output file should be appended, not overwritten */
    private boolean append;

    /**
     * Flag that indicates miscellaneous channel dictionaries, namely header and
     * monitor, should be excluded
     */
    private boolean excludeHeaderAndMonitor;
    
    private DictionaryCommandOptions dictOptions;
    
    private final FileOption outputFileOption = new FileOption(OUTPUT_FILE_SHORT, OUTPUT_FILE_LONG, "filename",
            "Output file for converted channel dictionary", false, false);
    private final FileOption fswFileOption = new FileOption(
                    null,
                    FSW_DICTIONARY_FILE_LONG,
                    "filename",
                    "Filename of the FSW dictionary to parse. If specified, the default FSW dictionary will be ignored and not parsed. " +
                    "Note: Only FSW dictionary (not SSE) can be specified using this option.",
                    false, true);

    private final FileOption sseFileOption = new FileOption(
                    null,
                    SSE_DICTIONARY_FILE_LONG,
                    "filename",
                    "Filename of the SSE dictionary to parse. If specified, the default SSE dictionary will be ignored and not parsed. " +
                    "Note: Only SSE dictionary (not FSW) can be specified using this option.",
                    false, true);

    private final FlagOption appendOption = new FlagOption(null, APPEND_LONG,
                        "If specified, the output file will be appended, not overwritten.", false);
    private final FlagOption excludeFswOption = new FlagOption(null, EXCLUDE_FSW_LONG,
            "Exclude FSW channel definitions flag", false);
    private final FlagOption excludeSseOption = new FlagOption(null, EXCLUDE_SSE_LONG,
            "Exclude SSE channel definitions flag", false);
    private final OutputFormatOption formatOption = new OutputFormatOption(false);
    private final ShowColumnsOption showColsOption = new ShowColumnsOption(false);
    private final FlagOption excludeHeaderAndMonitorOption = new FlagOption(null, EXCLUDE_HEADER_AND_MONITOR_LONG, 
            "Exclude header and monitor channel definitions flag", false);
    
    private final Map<String, IChannelDefinition> loadedChannelMap = new TreeMap<>();
    
    private final MissionProperties missionProps;

    private final ApplicationContext appContext;

    public ChannelDictionaryParserApp() {
        appContext = SpringContextFactory.getSpringContext(true);
        missionProps = appContext.getBean(MissionProperties.class);
    }
    
    /**
     * Main entry point for execution.
     *
     * @param args	command line arguments
     */
    public static void main(final String[] args) {

        final ChannelDictionaryParserApp app = new ChannelDictionaryParserApp();

        /*
         * First, parse the command-line arguments.
         */

        try {
            final ICommandLine cl = app.createOptions().parseCommandLine(args, true);
            app.configure(cl);
        } catch (final ParseException e) {
            System.err.println("Exception encountered while interpreting arguments: " + e.getMessage());
            System.exit(1);
        }

        try {
            app.loadChannelDictionaries();
        } catch (final DictionaryException de) {
            System.err.println(de.getMessage() + ": " + de.getCause().getMessage());
            System.exit(1);
        }

        try {
            app.openOutputTranslateWriteAllChannelDictionaries();
        } catch (final IllegalStateException ise) {
            System.err.println(ise.getMessage() + ":" + ise.getCause().getMessage());
            System.exit(1);
        }

    }

    /**
     * Parse the desired channel dictionaries and load into the table.
     * 
     * @throws DictionaryException
     *             Indicates an error encountered while locating or parsing a
     *             channel dictionary
     */
    public void loadChannelDictionaries() throws DictionaryException {

        /*
         * Obtain a DictionaryConfiguration object and determine the desired
         * channel dictionary path.
         */

        final DictionaryProperties dictConfig = this.dictOptions.getDictionaryConfiguration();
        final IChannelDictionaryFactory dictFact = appContext.getBean(IChannelDictionaryFactory.class);

        if (!excludeFsw) {
            String fswDictFile = null;

            if (fswDictFilename == null) {

                /*
                 * Use the mission-configured dictionary structure to locate the
                 * proper FSW dictionary to parse. User has not specified any
                 * custom path for the dictionary.
                 */

                try {
                    fswDictFile = dictConfig
                            .findFileForSystemMission(DictionaryType.CHANNEL);
                } catch (final DictionaryException e) {
                    throw new DictionaryException(
                            "Dictionary exception while looking up "
                                    + dictConfig.getDictionaryFileName(DictionaryType.CHANNEL),
                                    e);
                }

                if (fswDictFile == null) {
                    throw new DictionaryException("Cannot determine FSW channel dictionary file (null file path)");
                }

            } else {

                /*
                 * Dictionary file was specified by the user
                 */

                fswDictFile = fswDictFilename;
            }

            try {
                final IChannelDictionary fswChanDict = dictFact.getNewInstance(
                        dictOptions.getDictionaryConfiguration(), fswDictFile);
                loadedChannelMap.putAll(fswChanDict.getChannelDefinitionMap());
            } catch (final DictionaryException e) {
                throw new DictionaryException(
                        "Exception encountered while adding channel definitions to internal table from dictionary file "
                                + fswDictFile, e);
            }

        }

        if (!excludeSse && missionProps.missionHasSse()) {
            String sseDictFile = null;

            if (sseDictFilename == null) {

                /*
                 * Use the mission-configured dictionary structure to locate the
                 * proper SSE dictionary to parse. User has not specified any
                 * custom path for the dictionary.
                 */

                try {
                    sseDictFile = dictConfig.findSseFileForSystemMission(DictionaryType.CHANNEL);
                } catch (final DictionaryException e) {
                    throw new DictionaryException(
                            "Dictionary exception while looking up SSE "
                                    + dictConfig.getSseDictionaryFileName(DictionaryType.CHANNEL),
                                    e);
                }

                if (sseDictFile == null) {
                    throw new DictionaryException(
                            "Cannot determine SSE channel dictionary file (null file path)");
                }

            } else {

                /*
                 * Dictionary file was specified by the user
                 */

                sseDictFile = sseDictFilename;
            }

            try {
                final IChannelDictionary sseChanDict = dictFact.getNewSseInstance(
                        dictOptions.getDictionaryConfiguration(), sseDictFile);
                loadedChannelMap.putAll(sseChanDict.getChannelDefinitionMap());
            } catch (final DictionaryException e) {
                throw new DictionaryException(
                        "Exception encountered while adding channel definitions to internal table from SSE dictionary file "
                                + sseDictFile, e);
            }

        }

        if (!excludeHeaderAndMonitor) {

            /*
             * Add header and monitor channels.
             */
            try {
                final IChannelDictionary monChanDict = dictFact.getNewMonitorInstance(dictConfig);
                loadedChannelMap.putAll(monChanDict.getChannelDefinitionMap());
                final IChannelDictionary headChanDict = dictFact.getNewHeaderInstance(dictConfig);
                loadedChannelMap.putAll(headChanDict.getChannelDefinitionMap());
            } catch (final DictionaryException e) {
                throw new DictionaryException(
                        "Exception encountered while adding channel definitions to internal table from header/monitor dictionary file",
                        e);
            }

        }

    }


    /**
     * With the parsed parameters, translate the loaded channel definition table
     * and output.
     * 
     * @throws IllegalStateException
     *             Indicates an error encountered while opening the specified
     *             output file
     */
    public void openOutputTranslateWriteAllChannelDictionaries()
            throws IllegalStateException {

        PrintStream ps = null;

        if (outputFilename != null) {
            FileOutputStream fos = null;

            try {
                fos = new FileOutputStream(outputFilename, append);
                ps = new PrintStream(fos);

                translateWriteAllChannelDictionaries(ps);

                ps.flush();

            } catch (final FileNotFoundException e) {
                throw new IllegalStateException("Error writing to output file "
                        + outputFilename, e);

            } finally {

                if (ps != null) {

                    try {
                        ps.close();

                    } catch (final Exception e) {
                        // do nothing
                    }

                }

                if (fos != null) {

                    try {
                        fos.close();

                    } catch (final Exception e) {
                        // do nothing
                    }

                }

            }

        } else {
            translateWriteAllChannelDictionaries(System.out);

        }

    }


    private void translateWriteAllChannelDictionaries(final PrintStream ps) {
        final TemplatableChannelDefinition tcd = new TemplatableChannelDefinition();

        if (showColumnHeaders) {
            tcd.setHeader();
            ps.println(TemplateManager.createText(template, tcd.getTemplateContext()));
        }

        for (final IChannelDefinition chanDef :  loadedChannelMap.values()) {     
            tcd.set(chanDef);
            ps.println(TemplateManager.createText(template, tcd.getTemplateContext()));
        }

    }

    @Override
    public BaseCommandOptions createOptions() {
        
        if (optionsCreated.get()) {
            return options;
        }
        
        super.createOptions(appContext.getBean(BaseCommandOptions.class, this));

        dictOptions = new DictionaryCommandOptions(appContext.getBean(DictionaryProperties.class));
        
        options.addOptions(dictOptions.getFswOptions());

        if (missionProps.missionHasSse()) {
            options.addOptions(dictOptions.getSseOptions());
        }
        
        options.addOption(outputFileOption);
        
        options.addOption(excludeFswOption);

        if (missionProps.missionHasSse()) {
            options.addOption(excludeSseOption);
        }

        options.addOption(formatOption);
        
        options.addOption(fswFileOption);
     
        if (missionProps.missionHasSse()) {
            options.addOption(sseFileOption);
        }

        options.addOption(appendOption);
        options.addOption(showColsOption);
        options.addOption(excludeHeaderAndMonitorOption);
        
        return options;
    }


    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        super.configure(commandLine);
        
        this.dictOptions.parseAllOptionsAsOptional(commandLine);
        
        this.outputFilename = this.outputFileOption.parse(commandLine, false);

        this.dictOptions.FSW_DICTIONARY_DIRECTORY.parseWithDefault(commandLine, false, true);
        this.dictOptions.FSW_VERSION.parseWithDefault(commandLine, false, true);
        this.dictOptions.SSE_DICTIONARY_DIRECTORY.parseWithDefault(commandLine, false, true);
        this.dictOptions.SSE_VERSION.parseWithDefault(commandLine, false, true);

        /*
         * This app's output is always templated. Default output is CSV.
         */
        ApplicationTemplateManager tm = null;

        try {
            tm = MissionConfiguredTemplateManagerFactory.getNewApplicationTemplateManager(this,
                                                                                          appContext.getBean(SseContextFlag.class));
        } catch (final TemplateException e) {
            log.warn("Error generating template manager. Defaulting to no formatting: "
                    + rollUpMessages(e));
        }

        if (commandLine.hasOption(formatOption.getLongOpt())) {
            templateName = formatOption.parse(commandLine);

            try {
                this.template =
                        tm.getTemplateForStyle(OUTPUT_TEMPLATE_TYPE, templateName);

            } catch (final TemplateException e) {
                log.warn("Error retrieving template format "
                        + templateName
                        + " in "
                        + Arrays.toString(tm.getTemplateDirectories(
                                OUTPUT_TEMPLATE_TYPE).toArray())
                                + ". Defaulting to no formatting: " + rollUpMessages(e));

            }

        }

        if (this.template == null) {
            /*
             * Either no template was specified or the specified template does
             * not exist. Default to CSV.
             */
            final String format = "csv";

            try {
                this.template =
                        tm.getTemplateForStyle(OUTPUT_TEMPLATE_TYPE, format);

            } catch (final TemplateException e) {
                log.warn("Error retrieving template format "
                        + format
                        + " in "
                        + Arrays.toString(tm.getTemplateDirectories(
                                OUTPUT_TEMPLATE_TYPE).toArray())
                                + ". Cannot display output: " + rollUpMessages(e));
                throw new RuntimeException();
            }

        }

        this.showColumnHeaders = this.showColsOption.parse(commandLine);

        if (commandLine.hasOption(EXCLUDE_FSW_LONG)) {

            if (commandLine
                    .hasOption(dictOptions.FSW_DICTIONARY_DIRECTORY.getLongOpt())
                    || commandLine
                    .hasOption(dictOptions.FSW_VERSION.getLongOpt())
                    || commandLine.hasOption(FSW_DICTIONARY_FILE_LONG)) {
                throw new ParseException("--" + EXCLUDE_FSW_LONG
                        + " cannot be used with --"
                        + dictOptions.FSW_DICTIONARY_DIRECTORY.getLongOpt() + ", --"
                        + dictOptions.FSW_VERSION.getLongOpt() + ", and/or --"
                        + FSW_DICTIONARY_FILE_LONG + " arguments.");
            }

            this.excludeFsw = true;
        }

        if (commandLine.hasOption(EXCLUDE_SSE_LONG)) {

            if (commandLine
                    .hasOption(dictOptions.SSE_DICTIONARY_DIRECTORY.getLongOpt())
                    || commandLine
                    .hasOption(dictOptions.SSE_VERSION.getLongOpt())
                    || commandLine.hasOption(SSE_DICTIONARY_FILE_LONG)) {
                throw new ParseException("--" + EXCLUDE_SSE_LONG
                        + " cannot be used with --"
                        + dictOptions.SSE_DICTIONARY_DIRECTORY.getLongOpt() + ", --"
                        + dictOptions.SSE_VERSION.getLongOpt() + ", and/or --"
                        + SSE_DICTIONARY_FILE_LONG + " arguments.");
            }

            this.excludeSse = true;
        }

        if (commandLine.hasOption(FSW_DICTIONARY_FILE_LONG)) {

            if (commandLine
                    .hasOption(dictOptions.FSW_DICTIONARY_DIRECTORY.getLongOpt())
                    || commandLine
                    .hasOption(dictOptions.FSW_VERSION.getLongOpt())
                    || commandLine.hasOption(EXCLUDE_FSW_LONG)) {
                throw new ParseException("--" + FSW_DICTIONARY_FILE_LONG
                        + " cannot be used with --"
                        + dictOptions.FSW_DICTIONARY_DIRECTORY.getLongOpt() + ", --"
                        + dictOptions.FSW_VERSION.getLongOpt()+ ", and/or --"
                        + EXCLUDE_FSW_LONG + " arguments.");
            }

            this.fswDictFilename = this.fswFileOption.parse(commandLine, true);

        }

        if (commandLine.hasOption(SSE_DICTIONARY_FILE_LONG)) {

            if (commandLine
                    .hasOption(dictOptions.SSE_DICTIONARY_DIRECTORY.getLongOpt())
                    || commandLine
                    .hasOption(dictOptions.SSE_VERSION.getLongOpt())
                    || commandLine.hasOption(EXCLUDE_SSE_LONG)) {
                throw new ParseException("--" + SSE_DICTIONARY_FILE_LONG
                        + " cannot be used with --"
                        + dictOptions.SSE_DICTIONARY_DIRECTORY.getLongOpt() + ", --"
                        + dictOptions.SSE_VERSION.getLongOpt() + ", and/or --"
                        + EXCLUDE_SSE_LONG + " arguments.");
            }

            this.sseDictFilename = this.sseFileOption.parse(commandLine, true);
        }

        if (commandLine.hasOption(APPEND_LONG)) {

            if (this.outputFilename == null) {
                throw new ParseException("--" + APPEND_LONG
                        + " cannot be used without --" + OUTPUT_FILE_LONG
                        + " argument");
            }

            this.append = true;
        }
        
        this.excludeHeaderAndMonitor = this.excludeHeaderAndMonitorOption.parse(commandLine);
    }

    /**
     * Channel definition representation for Velocity templating.
     */
    private static class TemplatableChannelDefinition implements Templatable {
        private final Map<String, Object> context;
        private IChannelDefinition chanDef;


        public TemplatableChannelDefinition() {
            context = new HashMap<String,Object>(14);
        }


        public void set(final IChannelDefinition cd) {
            context.clear();
            chanDef = cd;
            setTemplateContext(context);
            chanDef = null; // to release object reference
        }


        public void setHeader() {
            context.put("header", true);
        }


        /**
         * {@inheritDoc}
         *
         * @see jpl.gds.shared.template.Templatable#setTemplateContext(java.util.Map)
         */
        @Override
        public void setTemplateContext(final Map<String, Object> map) {
            map.put("id", chanDef.getId());
            map.put("title", chanDef.getTitle());
            map.put("type", chanDef.getChannelType().toString());
            map.put("typeDrf", formatType(chanDef.getChannelType()));
            map.put("dnUnits", chanDef.getDnUnits());
            /* Use hasEu() instead of doDnToEu(). They have the same effect */
            map.put("dnToEu", chanDef.hasEu());
            map.put("euUnits", chanDef.getEuUnits());
            map.put("channelFormat", constructChannelFormat(chanDef));
            map.put("enumDefs", formatEnums(chanDef.getLookupTable()));
            map.put("ioFormat", chanDef.getDnFormat());
            map.put("euIoFormat", chanDef.getEuFormat());
            map.put("module", chanDef.getCategory(ICategorySupport.MODULE));
            map.put("subsystem", chanDef.getCategory(ICategorySupport.SUBSYSTEM));
            map.put("opsCategory", chanDef.getCategory(ICategorySupport.OPS_CAT));
        }


        public Map<String, Object> getTemplateContext() {
            return context;
        }

        /**
         * ChannelType's enumerations are not DRF-tools friendly. Translate on
         * our own.
         * 
         * @param type ChannelType to translate for DRF-tool ingestion
         * @return DRF-tool friendly channel type String 
         */
        private String formatType(final ChannelType type) {

            switch (type) {
            case SIGNED_INT:
            case UNSIGNED_INT:
            case TIME:
            case DIGITAL:
                return "INTEGER";
            case STATUS:
                return "ENUM";
            case BOOLEAN:
                return "BOOL";
            default:
                return type.toString();
            }

        }

        /**
         * DRF-tool requires the original dictionary's channel format string.
         * Since we lose that information upon parsing to IChannelDefinition
         * objects, must reconstruct.
         * 
         * @param chanDef
         *            channel definition to reconstruct the channel format from
         * @return channel format String
         */
        private String constructChannelFormat(final IChannelDefinition chanDef) {

            switch (chanDef.getChannelType()) {
            case SIGNED_INT:
            case UNSIGNED_INT:
            case TIME:
            case FLOAT:
                return chanDef.getChannelType().getBriefChannelType()
                        + chanDef.getSize();
            case STATUS:
                return ChannelType.SIGNED_INT.getBriefChannelType()
                        + chanDef.getSize();
            case DIGITAL:
                return ChannelType.UNSIGNED_INT.getBriefChannelType()
                        + chanDef.getSize();
            default:
                return chanDef.getChannelType().toString();
            }
        }

        /**
         * EnumerationDefinition's toString() is not well-suited for DRF-tools
         * ingestion. So make our own.
         * 
         * @param enumDef
         *            EnumerationDefinition to format to String
         * @return formatted enumeration definition String object
         * 
         * @version EnumerationDefinition key
         */
        private String formatEnums(final EnumerationDefinition enumDef) {

            if (enumDef == null) {
                return null;
            }

            final long min = enumDef.getMinValue();
            final long max = enumDef.getMaxValue();
            final StringBuilder text = new StringBuilder();
            String delim = "";

            for (long i = min; i <= max; i++) {
                final String value = enumDef.getValue(i);

                if (value != null) {
                    text.append(delim);
                    text.append(i);
                    text.append("=");
                    text.append(value);
                    delim = ",";
                }

            }

            return text.toString();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }
        super.showHelp();
    }

    /**
     * Getter for Spring Application Context - package private
     * 
     * @return ApplicationContext object
     */
    ApplicationContext getAppContext() {
        return appContext;
    }

    /**
     * Flag that indicates FSW channel dictionary should be excluded
     * 
     * @return excludeFsw as boolean
     */

    public boolean isExcludeFsw() {
        return excludeFsw;
    }

    /**
     * Flag that indicates SSE channel dictionary should be excluded
     * 
     * @return excludeSse as boolean
     */
    public boolean isExcludeSse() {
        return excludeSse;
    }

    /**
     * Flag that indicates the output file should be appended, not overwritten
     * 
     * @return isAppend as boolean
     */
    public boolean isAppend() {
        return append;
    }

    /**
     * Flag to show column headings
     * 
     * @return showColumnHeaders as boolean
     */
    public boolean isShowColumnHeaders() {
        return showColumnHeaders;
    }

    /**
     * Flag that indicates miscellaneous channel dictionaries, namely header and monitor, should be excluded
     * 
     * @return excludeHeaderAndMonitor as boolean
     */
    public boolean isExcludeHeaderAndMonitor() {
        return excludeHeaderAndMonitor;
    }

    /**
     * Velocity template name for output
     * 
     * @return Template name
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * The FSW dictionary filename to parse
     * 
     * @return FSW dictionary file name
     */
    public String getFswDictFilename() {
        return fswDictFilename;
    }

    /**
     * The SSE dictionary filename to parse
     * 
     * @return SSE dictionary file name
     */
    public String getSSeDictFilename() {
        return sseDictFilename;
    }

}
