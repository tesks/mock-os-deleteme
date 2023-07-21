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

import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.message.api.MessageUtility;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.MessageTypesOption;
import jpl.gds.shared.cli.options.OutputFormatOption;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.IMessageConfiguration;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.template.MissionConfiguredTemplateManagerFactory;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.template.TemplateManager;
import jpl.gds.watcher.IResponderAppHelper;
import jpl.gds.watcher.responder.handlers.ExecuteScriptMessageHandler;

/**
 * This is the application helper class for a message responder that triggers
 * a script.
 */
public class TriggerScriptApp implements IResponderAppHelper {
    private static final String APP_NAME = ApplicationConfiguration.getApplicationName(ResponderAppName.TRIGGER_SCRIPT_APP_NAME.getAppName());
    /**
     * Full path to script to execute (required) - short option.
     */
    public static final String SCRIPT_SHORT = "s";
    /**
     * Full path to script to execute (required) - long option.
     */
    public static final String SCRIPT_LONG = "script";
    /**
     * Show output of spawned script on console - short option.
     */
    public static final String CONSOLE_SHORT = "c";
    /**
     * Show output of spawned script on console - long option.
     */
    public static final String CONSOLE_LONG = "consoleOutput";
    /**
     * Specifies message types to subscribe to (required). Message types must
     * be specified as a quoted comma-separated list - short option.
     */
    public static final String TYPES_SHORT_OPTION = "t";
    /**
     * Specifies message types to subscribe to (required). Message types must
     * be specified as a quoted comma-separated list - long option.
     */
    public static final String TYPES_LONG_OPTION = "types";

    private String script;
    private String templateName =
            ExecuteScriptMessageHandler.DEFAULT_TEMPLATE;
    private boolean showToConsole = false;
    private Collection<IMessageType> messageTypes;
    
	private final ApplicationContext appContext;
	
	private final OutputFormatOption formatOption = new OutputFormatOption(false);
	private final StringOption scriptOption = new StringOption(SCRIPT_SHORT, SCRIPT_LONG, "path",
                "full path to script to execute (required)", true);
	private final FlagOption consoleOption = new FlagOption(CONSOLE_SHORT, CONSOLE_LONG,
                "show output of spawned script on console", false);
	private final MessageTypesOption typesOption = new MessageTypesOption(true);
	
	/**
	 * Constructor.
	 * 
	 * @param appContext the current application context
	 */
	public TriggerScriptApp(final ApplicationContext appContext) {
		this.appContext = appContext;
	}

    @Override
    public void addAppOptions(final BaseCommandOptions opt) {
        formatOption.setDefaultValue(ExecuteScriptMessageHandler.DEFAULT_TEMPLATE);
        opt.addOption(formatOption);
        opt.addOption(scriptOption);
        opt.addOption(consoleOption);
        opt.addOption(typesOption);
    }

    @Override
    public void configure(final ICommandLine commandLine)
            throws ParseException {
        
        templateName = formatOption.parseWithDefault(commandLine, false, true);
        script = scriptOption.parse(commandLine);
        showToConsole = consoleOption.parse(commandLine);
        messageTypes = typesOption.parse(commandLine);
     
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#getOverrideTypes()
     */
    @Override
    public String[] getOverrideTypes() {
        if (this.messageTypes != null) {
            final String[] result = new String[this.messageTypes.size()];
            int i = 0;
            for (final IMessageType t: this.messageTypes) {
                result[i++] = t.getSubscriptionTag();
            }
            return result;
        } else {
            return null;
        }
    }

    /**
     * Gets the name of the script to be executed when this application gets a
     * message.
     * @return the full path to the script
     */
    public String getScriptName() {
        return this.script;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#getContextConfiguration()
     */
    @Override
    public IContextConfiguration getContextConfiguration() {
        return null;
    }

    private String getTemplateDirectories() {
        TemplateManager templateManager = null;
        final StringBuilder result = new StringBuilder();
        try {
            templateManager = MissionConfiguredTemplateManagerFactory.getNewMessageTemplateManager(appContext.getBean(SseContextFlag.class));

            final List<String> directories =
                    templateManager.getTemplateDirectories();

            result.append("\nTemplate directories searched are:\n");
            for (final String d : directories) {
                result.append("   " + d + "\n");
            }
        } catch (final TemplateException e) {
            TraceManager.getDefaultTracer().warn(

                    "Unable to determine template directories\n");
        }
        return result.toString();
    }

    /**
     * Gets the name of the output template (format) for channel records.
     * @return the template name
     */
    public String getTemplateName() {
        return this.templateName;
    }

    @Override
    public String getAdditionalHelpText() {
        
        final List<IMessageConfiguration> availTypes = MessageRegistry.getAllMessageConfigs(true);
        final List<String> availStyles = MessageUtility.getMessageStyles(availTypes.toArray(new IMessageConfiguration[] {}));
        final StringBuilder types =
                new StringBuilder("Available message types are:\n   ");
        for (int i = 0; i < availTypes.size(); i++) {
            types.append(availTypes.get(i).getSubscriptionTag() + " ");
            if (i != 0 && i % 5 == 0) {
                types.append("\n   ");
            }
        }
        final StringBuilder styles =
                new StringBuilder(
                        "\n\nKnown available message styles are:\n   ");
        for (final String s: availStyles) {
            styles.append(s + " ");
        }
        styles.append("\n\nNote that not all styles apply to all message types.");
        styles.append("\n");
        
        return types.toString() + styles.toString() + getTemplateDirectories();
        
    }
    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#getUsageText()
     */
    @Override
    public String getUsageText() {
        
        
        final StringBuilder sb = new StringBuilder("Usage: " + APP_NAME + " [session options] [jms options] [database options] [--printLog\n");
        sb.append("                            --script <script-path> --types <type-list> --consoleOutput --outputFormat <format>]\n");
        sb.append("       " + APP_NAME + " --topics <topic-list> [jms options] [database options] [--printLog\n");
        sb.append("                            --script <script-path> --types <type-list> --consoleOutput --outputFormat <format>]\n");
        return sb.toString();
     }

    /**
     * Gets the flag indicating whether output from the spawned script should
     * be sent to the console.
     * @return true if script output should be piped to the console
     */
    public boolean isConsoleOutput() {
        return this.showToConsole;
    }

    /**
     * Sets the flag indicating whether output from the spawned script should
     * be sent to the console.
     * @param enable
     *            true if script output should be piped to the console
     */
    public void setConsoleOutput(final boolean enable) {
        this.showToConsole = enable;
    }

    /**
     * Sets the name of the script to be executed when this application gets a
     * message.
     * @param path
     *            the full path to the script
     */
    public void setScriptName(final String path) {
        this.script = path;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#setContextConfiguration(IContextConfiguration)
     */
    @Override
    public void setContextConfiguration(final IContextConfiguration session) {
        // do nothing
    }

	@Override
	public ApplicationContext getApplicationContext() {
		return this.appContext;
	}
}
