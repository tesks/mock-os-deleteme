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
package jpl.gds.app.tools.dictionary;

import static jpl.gds.shared.exceptions.ExceptionTools.rollUpMessages;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.ParseException;
import org.apache.velocity.Template;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.dictionary.options.DictionaryCommandOptions;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.dictionary.api.evr.IEvrDictionary;
import jpl.gds.dictionary.api.evr.IEvrDictionaryFactory;
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
 * chill_parse_evr_dict. It reads the default or the specified EVR dictionary
 * using the mission-configured dictionary parser. It will then output the
 * loaded channel definitions to either the standard output or a specified
 * output file, using a template.
 */
public class EvrDictionaryParserApp extends AbstractCommandLineApp {
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

    /** Long option for FSW EVR dictionary exclusion */
    public static final String EXCLUDE_FSW_LONG = "noFSW";

    /** Long option for SSE evr dictionary exclusion */
    public static final String EXCLUDE_SSE_LONG = "noSSE";

    /** Output template type */
    static final String OUTPUT_TEMPLATE_TYPE = "EvrDictionary";

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

    /** Flag that indicates FSW EVR dictionary should be excluded */
    private boolean excludeFsw;

    /** Flag that indicates SSE EVR dictionary should be excluded */
    private boolean excludeSse;

    /** Flag that indicates the output file should be appended, not overwritten */
    private boolean append;
    
    private IEvrDictionary fswDict;
    private IEvrDictionary sseDict;
    
    private DictionaryCommandOptions dictOptions;
    
    private final FileOption outputFileOption = new FileOption(OUTPUT_FILE_SHORT, OUTPUT_FILE_LONG, "filename",
            "Output file for converted EVR dictionary", false, false);
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
            "Exclude FSW EVR definitions flag", false);
    private final FlagOption excludeSseOption = new FlagOption(null, EXCLUDE_SSE_LONG,
            "Exclude SSE EVR definitions flag", false);
    private final OutputFormatOption formatOption = new OutputFormatOption(false);
    private final ShowColumnsOption showColsOption = new ShowColumnsOption(false);
    
    private final MissionProperties  missionProps;

    private final ApplicationContext appContext;

    
    
    /**
     * Main entry point for execution.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {

        final EvrDictionaryParserApp app = new EvrDictionaryParserApp();

        /*
         * First, parse the command-line arguments.
         */

        try {
            final ICommandLine cl = app.createOptions().parseCommandLine(args, true);
            app.configure(cl);
        } catch (final ParseException e) {
            System.err
            .println("Exception encountered while interpreting arguments: "
                    + e.getMessage());
            System.exit(1);
        }

        try {
            app.loadEvrDictionaries();
        } catch (final DictionaryException de) {
            System.err.println(de.getMessage() + ": "
                    + de.getCause().getMessage());
            System.exit(1);
        }

        try {
            app.openOutputTranslateWriteAllEvrDictionaries();
        } catch (final IllegalStateException ise) {
            System.err.println(ise.getMessage() + ":"
                    + ise.getCause().getMessage());
            System.exit(1);
        }

    }

    public EvrDictionaryParserApp() {
        appContext = SpringContextFactory.getSpringContext(true);
        this.missionProps = appContext.getBean(MissionProperties.class);
    }
    
    /**
     * Parse the desired evr dictionaries and load into the table.
     *
     * @throws DictionaryException Indicates an error encountered while locating
     *             or parsing a EVR dictionary
     */
    public void loadEvrDictionaries() throws DictionaryException {

        /*
         * Obtain a DictionaryConfiguration object and determine the desired EVR
         * dictionary path.
         */

        final DictionaryProperties dictConfig = this.dictOptions.getDictionaryConfiguration();

        if (!this.excludeFsw) {
            String fswDictFile = null;

            if (this.fswDictFilename == null) {

                /*
                 * Use the mission-configured dictionary structure to locate the
                 * proper FSW dictionary to parse. User has not specified any
                 * custom path for the dictionary.
                 */

                try {
                    fswDictFile =
                            dictConfig
                            .findFileForSystemMission(DictionaryType.EVR);
                } catch (final DictionaryException e) {
                    throw new DictionaryException(
                            "Dictionary exception while looking up "
                                    + dictConfig.getDictionaryFileName(DictionaryType.EVR),
                                    e);
                }

                if (fswDictFile == null) {
                    throw new DictionaryException(
                            "Cannot determine FSW EVR dictionary file (null file path)");
                }

            } else {

                /*
                 * Dictionary file was specified by the user
                 */

                fswDictFile = this.fswDictFilename;
            }

            try {
                fswDict = appContext.getBean(IEvrDictionaryFactory.class).
                        getNewInstance(dictOptions.getDictionaryConfiguration(), fswDictFile);
            } catch (final DictionaryException e) {
                throw new DictionaryException(
                        "Exception encountered while adding EVR definitions to internal table from dictionary file "
                                + fswDictFile, e);
            }

        }

        if (!this.excludeSse && missionProps.missionHasSse()) {
            String sseDictFile = null;

            if (this.sseDictFilename == null) {

                /*
                 * Use the mission-configured dictionary structure to locate the
                 * proper SSE dictionary to parse. User has not specified any
                 * custom path for the dictionary.
                 */

                try {
                    sseDictFile =
                            dictConfig
                            .findSseFileForSystemMission(DictionaryType.EVR);
                } catch (final DictionaryException e) {
                    throw new DictionaryException(
                            "Dictionary exception while looking up SSE "
                                    + dictConfig.getSseDictionaryFileName(DictionaryType.EVR),
                                    e);
                }

                if (sseDictFile == null) {
                    throw new DictionaryException(
                            "Cannot determine SSE EVR dictionary file (null file path)");
                }

            } else {

                /*
                 * Dictionary file was specified by the user
                 */

                sseDictFile = this.sseDictFilename;
            }

            try {
                sseDict = appContext.getBean(IEvrDictionaryFactory.class).
                        getNewSseInstance(dictOptions.getDictionaryConfiguration(), sseDictFile);
              
            } catch (final DictionaryException e) {
                e.printStackTrace();
                throw new DictionaryException(
                        "Exception encountered while adding EVR definitions to internal table from SSE dictionary file "
                                + sseDictFile, e);
            }

        }

    }

    /**
     * With the parsed parameters, translate the loaded EVR definition table and
     * output.
     *
     * @throws IllegalStateException Indicates an error encountered while
     *             opening the specified output file
     */
    public void openOutputTranslateWriteAllEvrDictionaries()
            throws IllegalStateException {

        PrintStream ps = null;

        if (this.outputFilename != null) {
            FileOutputStream fos = null;

            try {
                fos = new FileOutputStream(this.outputFilename, this.append);
                ps = new PrintStream(fos);

                this.translateWriteAllEvrDictionaries(ps);

                ps.flush();

            } catch (final FileNotFoundException e) {
                throw new IllegalStateException("Error writing to output file "
                        + this.outputFilename, e);

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
            this.translateWriteAllEvrDictionaries(System.out);

        }

    }

    private void translateWriteAllEvrDictionaries(final PrintStream ps) {
        final TemplatableEvrDefinition tcd = new TemplatableEvrDefinition();

        if (this.showColumnHeaders) {
            tcd.setHeader();
            ps.println(TemplateManager.createText(this.template,
                    tcd.getTemplateContext()));
        }
        
        /* Get FSW and SSE entries separately. Fetch definitions rather than names. */
        if (fswDict != null) {
            for (final IEvrDefinition evrDef : fswDict.getEvrDefinitions()) {
                tcd.set(evrDef);
                ps.println(TemplateManager.createText(this.template, tcd.getTemplateContext()));
            }
        }

        if (sseDict != null) {
            for (final IEvrDefinition evrDef : sseDict.getEvrDefinitions()) {
                tcd.set(evrDef);
                ps.println(TemplateManager.createText(this.template, tcd.getTemplateContext()));
            }
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
    }

    /**
     * EVR definition representation for Velocity templating.
     */
    private static class TemplatableEvrDefinition implements Templatable {
        private final Map<String, Object> context;
        private IEvrDefinition evrDef;

        public TemplatableEvrDefinition() {
            this.context = new HashMap<String, Object>(9);
        }

        public void set(final IEvrDefinition cd) {
            this.context.clear();
            this.evrDef = cd;
            this.setTemplateContext(this.context);
            this.evrDef = null; // to release object reference
        }

        public void setHeader() {
            this.context.put("header", true);
        }

        /**
         * {@inheritDoc}
         *
         * @see jpl.gds.shared.template.Templatable#setTemplateContext(java.util.Map)
         */
        @Override
        public void setTemplateContext(final Map<String, Object> map) {
            map.put("id", this.evrDef.getId());
            map.put("title", this.evrDef.getName());
            map.put("level", this.evrDef.getLevel());
            /* New call to Categories. */
            map.put("module", this.evrDef.getCategory(IEvrDefinition.MODULE));
            // remove tabs and newlines from format
            final String format =
                    this.evrDef.getFormatString().replaceAll(
                            "[\\t\\n\\r\\f\\v]", "");
            map.put("format", format);
            map.put("nargs", this.evrDef.getNargs());
            map.put("opsCat", this.evrDef.getCategory(IEvrDefinition.OPS_CAT));
            map.put("subsystem", this.evrDef.getCategory(IEvrDefinition.SUBSYSTEM));
        }

        public Map<String, Object> getTemplateContext() {
            return this.context;
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
     * The FSW dictionary filename to parse
     * 
     * @return FSW file name
     */
    public String getFswDictFilename() {
        return fswDictFilename;
    }

    /**
     * The SSE dictionary filename to parse
     * 
     * @return SSE file name
     */
    public String getSseDictFilename() {
        return sseDictFilename;
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
     * Velocity template name for output
     * 
     * @return Template name
     */
    public String getTemplateName() {
        return templateName;
    }
}
