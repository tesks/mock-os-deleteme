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
package jpl.gds.watcher.responder.app;

import jpl.gds.common.options.querycommand.EvrTypeSelect;
import jpl.gds.common.options.querycommand.EvrTypesOption;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.evr.api.message.EvrMessageType;
import jpl.gds.session.message.SessionMessageType;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.CsvStringOption;
import jpl.gds.shared.cli.options.ExitWithSessionOption;
import jpl.gds.shared.cli.options.OutputFormatOption;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.template.MessageTemplateManager;
import jpl.gds.watcher.IResponderAppHelper;
import jpl.gds.watcher.responder.handlers.EvrMessageHandler;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.Collection;

/**
 * This is the application helper class for a message responder that displays
 * Evrs from the message bus.
 */
public class EvrWatcherApp implements IResponderAppHelper {
    private static final String APP_NAME = ApplicationConfiguration.getApplicationName(ResponderAppName.EVR_WATCHER_APP_NAME.getAppName());

    /**
     * EVR Event ID; separate multiple values by commas - short option.
     */
    public static final String EVENT_ID_SHORT = "i";
    /**
     * EVR Event ID; separate multiple values by commas - long option.
     */
    public static final String EVENT_ID_LONG = "eventId";

    /**
     * EVR name or pattern to match multiple names; pattern must be a POSIX
     * regular expression - short option.
     */
    public static final String LEVEL_SHORT = "l";
    /**
     * EVR name or pattern to match multiple names; pattern must be a POSIX
     * regular expression - long option.
     */
    public static final String LEVEL_LONG = "level";
    /**
     * EVR Level string; separate multiple values by commas - short option.
     */
    public static final String MODULE_SHORT = "u";
    /**
     * EVR Level string; separate multiple values by commas - long option.
     */
    public static final String MODULE_LONG = "module";
    /**
     * EVR Event ID; separate multiple values by commas - short option.
     */
    public static final String EVR_NAME_SHORT = "z";
    /**
     * EVR Event ID; separate multiple values by commas - long option.
     */
    public static final String EVR_NAME_LONG = "namePattern";

    private boolean exitWithSession  = false;
    private String[] levels;
    private String[] ids;
    private String[] modules;
    private String evrName;
    private String templateName = EvrMessageHandler.DEFAULT_TEMPLATE;
    private boolean wantRealtimeOnly = false;
    private boolean wantRecordedOnly = false;
    private boolean wantSseOnly      = false;
    private boolean wantFswOnly      = false;
    
	private final ApplicationContext appContext;
	
	private final ExitWithSessionOption exitOption = new ExitWithSessionOption();
	private final OutputFormatOption formatOption = new OutputFormatOption(false);
	private final CsvStringOption levelOption = new CsvStringOption(LEVEL_SHORT, LEVEL_LONG, "level-list",
                "EVR Level string; separate multiple values by commas", true, true, false);
	private final CsvStringOption moduleOption = new CsvStringOption(MODULE_SHORT, MODULE_LONG, "module-list",
                "FSW/SSE module name; separate multiple values by commas", true, true, false);
    private final StringOption nameOption = new StringOption(EVR_NAME_SHORT, EVR_NAME_LONG, "string",
                "EVR name or pattern to match multiple names; pattern must be"
                        + " a POSIX regular expression", false);
    private final CsvStringOption idOption = new CsvStringOption(EVENT_ID_SHORT, EVENT_ID_LONG, "id-list",
                "EVR Event ID; separate multiple values by commas", true, true, false);
    private final EvrTypesOption evrTypesOption = new EvrTypesOption(false);
    
	/**
	 * Constructor.
	 * 
	 * @param appContext the current application context
	 */
	public EvrWatcherApp(final ApplicationContext appContext) {
		this.appContext = appContext;
	}
	

    @Override
    public void addAppOptions(final BaseCommandOptions opt) {
        opt.addOption(exitOption);
        formatOption.setDefaultValue(EvrMessageHandler.DEFAULT_TEMPLATE);
        opt.addOption(formatOption);
        opt.addOption(levelOption);
        opt.addOption(moduleOption);
        opt.addOption(nameOption);
        opt.addOption(idOption);
        opt.addOption(evrTypesOption);
    }

    @Override
    public void configure(final ICommandLine commandLine)
            throws ParseException {

        exitWithSession = exitOption.parse(commandLine);
        templateName = formatOption.parseWithDefault(commandLine, false, true);
        final EvrTypeSelect evrTypeSelect = evrTypesOption.parse(commandLine);
     
        if (evrTypeSelect.sse)
        {
            wantFswOnly = false;
            wantSseOnly = (! (evrTypeSelect.fswRealtime ||
                            evrTypeSelect.fswRecorded));
        }
        else
        {
            wantFswOnly = true;
            wantSseOnly = false;
        }

        if (evrTypeSelect.fswRealtime)
        {
            wantRecordedOnly = false;
            wantRealtimeOnly = ! evrTypeSelect.fswRecorded;
        }
        else
        {
            wantRecordedOnly = true;
            wantRealtimeOnly = false;
        }

        evrName = nameOption.parse(commandLine);
        Collection<String> tempList = levelOption.parse(commandLine);
        if (!tempList.isEmpty()) {
            levels = tempList.toArray(new String[] {});
        }

        tempList = moduleOption.parse(commandLine);
        if (!tempList.isEmpty()) {
            modules = tempList.toArray(new String[] {});
        }
        
        tempList = idOption.parse(commandLine);      
        if (!tempList.isEmpty()) {
            ids = tempList.toArray(new String[] {});
        }
        
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#getAdditionalHelpText()
     */
    @Override
    public String getAdditionalHelpText() {
        final String[] styles = MessageTemplateManager.getTemplateStylesForHelp(EvrMessageType.Evr);
        if (styles.length == 0) {
            // OK to system.out this rather than trace; it's part of the help
            // text
            return ("\nA list of formatting styles in not currently"
                    + " available.");
        }
        final StringBuilder result =
                new StringBuilder("\nAvailable formatting styles are:");
        for (int i = 0; i < styles.length; i++) {
            if (i % 4 == 0) {
                result.append("\n");
                result.append("   ");
            }
            result.append(styles[i] + " ");
        }
        result.append("\n");
        result.append( MessageTemplateManager.getTemplateDirectoriesForHelp(EvrMessageType.Evr));
        return result.toString();
    }

    /**
     * Get EVR name pattern.
     *
     * @return evr name pattern
     */
    public String getEvrNamePattern() {
        return evrName;
    }

    /**
     * Get event ids.
     *
     * @return array of event id
     */
    public String[] getIds()
    {
        return ((ids != null) ? Arrays.copyOf(ids, ids.length) : null);
    }

    /**
     * Get all levels.
     *
     * @return array of levels
     */
    public String[] getLevels()
    {
        return ((levels != null) ? Arrays.copyOf(levels, levels.length) : null);
    }

    /**
     * Get all modules.
     *
     * @return array of modules
     */
    public String[] getModules()
    {
        return ((modules != null)
                   ? Arrays.copyOf(modules, modules.length) : null);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#getOverrideTypes()
     */
    @Override
    public String[] getOverrideTypes() {
        return new String[] { EvrMessageType.Evr.getSubscriptionTag(), 
                SessionMessageType.SessionHeartbeat.getSubscriptionTag(),
                SessionMessageType.StartOfSession.getSubscriptionTag(),
                SessionMessageType.EndOfSession.getSubscriptionTag() };
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#
     *      getContextConfiguration()
     */
    @Override
    public IContextConfiguration getContextConfiguration() {
        return null;
    }

    /**
     * Gets the name of the output template (format) for channel records.
     * @return the template name
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#getUsageText()
     */
    @Override
    public String getUsageText() {
        
        final StringBuilder sb = new StringBuilder("Usage: " + APP_NAME + " [session options] [jms options] [database options] [--printLog\n");
        sb.append("                         --eventId <id-list> --module <module-list> --level <level-list>\n");
        sb.append("                         --namePattern <name> --evrTypes <types> --exitWithSession]\n");
        sb.append("      " + APP_NAME + " --topics <topic-list> [jms options] [database options] [--printLog\n");
        sb.append("                        --eventId <id-list> --module <module-list> --level <level-list>\n");
        sb.append("                        --namePattern <name> --evrTypes <types> --exitWithSession]\n");
        return sb.toString();
    }

    /**
     * Gets the flag indicating whether the application should exit when the
     * session ends.
     * @return true if the application should exit when an end of session
     *         message is received
     */
    public boolean isExitSession() {
        return exitWithSession;
    }

    /**
     * Get FSW-only state.
     *
     * @return whether fsw is enabled
     */
    public boolean isFswOnly() {
        return wantFswOnly;
    }

    /**
     * Get realtime-only state.
     *
     * @return whether the application is in realtime mode and not recorded
     */
    public boolean isRealtime() {
        return wantRealtimeOnly;
    }

    /**
     * Get recorded-only state.
     *
     * @return whether the application is in recorded mode and not realtime
     */
    public boolean isRecorded() {
        return wantRecordedOnly;
    }

    /**
     * Get SSE-only state.
     *
     * @return whether sse is enabled
     */
    public boolean isSseOnly() {
        return wantSseOnly;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#setContextConfiguration(IContextConfiguration)
     */
    @Override
    public void setContextConfiguration(final IContextConfiguration context) {
        // do nothing
    }
        
 
	@Override
	public ApplicationContext getApplicationContext() {
		return this.appContext;
	}
}
