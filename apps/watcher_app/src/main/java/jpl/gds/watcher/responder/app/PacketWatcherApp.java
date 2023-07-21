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

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.options.VcidOption;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.session.message.SessionMessageType;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.*;
import jpl.gds.shared.template.MessageTemplateManager;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.watcher.IResponderAppHelper;
import jpl.gds.watcher.responder.handlers.PacketExtractSumMessageHandler;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import java.util.Collection;

/**
 * This is the application helper class for a message responder that displays
 * PacketExtractSumMessages from the message bus.
 */
public class PacketWatcherApp implements IResponderAppHelper {

    private static final String APP_NAME =
            ApplicationConfiguration.getApplicationName(ResponderAppName.PACKET_WATCHER_APP_NAME.getAppName());

    /**
     * Single VCID - short option.
     */
    public static final String VCID_SHORT = "w";
    /**
     * Single VCID - long option.
     */
    public static final String VCID_LONG = "vcid";
    /**
     * APIDs; separate multiple values by commas - short option.
     */
    public static final String APID_SHORT = "a";
    /**
     * APIDs; separate multiple values by commas - long option.
     */
    public static final String APID_LONG = "apid";
    /**
     * APID Names; separate multiple values by commas - short option.
     */
    public static final String APID_NAME_SHORT = "A";
    /**
     * APID Names; separate multiple values by commas - long option.
     */
    public static final String APID_NAME_LONG = "apidName";
    /**
     * Toggles minification of output. Minified output displays header only once
     * - short option.
     */
    public static final String MINIFY_SHORT = "M";
    /**
     * Toggles minification of output. Minified output displays header only once
     * - long option.
     */
    public static final String MINIFY_LONG = "minify";

    private boolean exitWithContext = false;
    private boolean minify = false;
    private boolean csvHeaders = false;
    private String[] apids;
    private String[] apidNames;
    private UnsignedInteger vcid;
    private String templateName =
            PacketExtractSumMessageHandler.DEFAULT_TEMPLATE;
    
	private final ApplicationContext appContext;
	
	private final ExitWithSessionOption exitOption = new ExitWithSessionOption();
	private final ShowColumnsOption showColsOption = new ShowColumnsOption(false);
	private VcidOption vcidOption;
	private final CsvStringOption apidOption = new CsvStringOption(APID_SHORT, APID_LONG, "apid-list",
            "APIDs; separate multiple values by commas", true, true, false);
	private final CsvStringOption apidNameOption = new CsvStringOption(APID_NAME_SHORT, APID_NAME_LONG, "apid-name-list",
            "APID Names; separate multiple values by commas", true, true, false);
	private final OutputFormatOption formatOption = new OutputFormatOption(false);
	private final FlagOption minifyOption = new FlagOption(MINIFY_SHORT, MINIFY_LONG,
            "Toggles minification of output. Minified output only displays the header once in the beginning", false);
	
	/**
	 * Constructor.
	 * 
	 * @param appContext the current application context
	 */
	public PacketWatcherApp(final ApplicationContext appContext) {
		this.appContext = appContext;
	}
	
    @Override
	public void addAppOptions(final BaseCommandOptions opt) {
        opt.addOption(exitOption);
        opt.addOption(showColsOption);
        vcidOption = new VcidOption("w", appContext.getBean(MissionProperties.class), false);
        opt.addOption(vcidOption);
        opt.addOption(apidOption);
        opt.addOption(apidNameOption);
        formatOption.setDefaultValue(PacketExtractSumMessageHandler.DEFAULT_TEMPLATE);
        opt.addOption(formatOption);
        opt.addOption(minifyOption);
    }


    @Override
	public void configure(final ICommandLine commandLine)
            throws ParseException {
        
        exitWithContext = exitOption.parse(commandLine);
        templateName = formatOption.parseWithDefault(commandLine, false, true);
        minify = minifyOption.parse(commandLine);
        vcid = vcidOption.parse(commandLine);
        csvHeaders = showColsOption.parse(commandLine);
        
        Collection<String> tempList = apidOption.parse(commandLine);
        if (tempList != null) {
            apids = tempList.toArray(new String[] {});
        }
      
        tempList = apidNameOption.parse(commandLine);
        if (tempList != null) {
            apidNames = tempList.toArray(new String[] {});
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#getAdditionalHelpText()
     */
    @Override
	public String getAdditionalHelpText() {
        final String[] styles = MessageTemplateManager.getTemplateStylesForHelp(TmServiceMessageType.TelemetryPacketSummary);
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
                result.append("\n   ");
            }
            result.append(styles[i] + " ");
        }
        result.append('\n');
        result.append(MessageTemplateManager.getTemplateDirectoriesForHelp(TmServiceMessageType.TelemetryPacketSummary));
        return result.toString();
    }

    /**
     * @return array of apid names
     */
    public String[] getApidNames() {
    	if (this.apidNames != null) {
    		final String[] result = new String[apidNames.length];
    		System.arraycopy(apidNames, 0, result, 0, apidNames.length);
    		return result;
    	}
        return null;
    }

    /**
     * @return array of apid
     */
    public String[] getApids() {
    	if (this.apids != null) {
    		final String[] result = new String[apids.length];
    		System.arraycopy(apids, 0, result, 0, apids.length);
    		return result;
    	}
        return null;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#getOverrideTypes()
     */
    @Override
	public String[] getOverrideTypes() {
        return new String[] { TmServiceMessageType.TelemetryPacketSummary.getSubscriptionTag(),
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
        sb.append("                            --apid <apid-list> --apidName <apidName-list> --vcid <vcid> --minify --outputFormat <format<]\n");
        sb.append("       " + APP_NAME + " --topics <topic-list> [jms options] [database options] [--printLog\n");
        sb.append("                            --apid <apid-list> --apidName <apidName-list> --vcid <vcid> --minify --outputFormat <format>]\n");
        return sb.toString();
    }
    
    /**
     * @return vcid
     */
    public UnsignedInteger getVcid() {
        return vcid;
    }

    /**
     * Gets the flag indicating whether the application should exit when the
     * context ends.
     * @return true if the application should exit when an end of context
     *         message is received
     */
    public boolean isExitContext() {
        return exitWithContext;
    }

    /**
     * Gets the flag indicating weather output is in minified (condensed)
     * format. Minified output only displays the header once in the beginning
     * and subsequent chunks are delimited by a horizontal line
     * @return boolean true if minification is turned on, false otherwise
     */
    public boolean isMinified() {
        return minify;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#setContextConfiguration(IContextConfiguration)
     */
    @Override
	public void setContextConfiguration(final IContextConfiguration config) {
         // Not needed for this application
    }

    /**
     * Returns whether or not csv headers should be displayed.
     * @return boolean whether or not to display csv headers
     */
    public boolean showCSVHeaders() {
        return csvHeaders;
    }
    

	@Override
	public ApplicationContext getApplicationContext() {
		return this.appContext;
	}

}
