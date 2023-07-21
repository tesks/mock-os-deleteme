/*
 * Copyright 2006-2020. California Institute of Technology.
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
import jpl.gds.common.filtering.DssIdFilter;
import jpl.gds.common.options.DssIdListOption;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.session.message.SessionMessageType;
import jpl.gds.shared.channel.ChannelListRange;
import jpl.gds.shared.channel.ChannelListRangeException;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.ChannelOption;
import jpl.gds.shared.cli.options.ExitWithSessionOption;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.OutputFormatOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.template.MessageTemplateManager;
import jpl.gds.watcher.IResponderAppHelper;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Abstract JMS watcher app for EHA messages, with concrete implementations for EHAs, EHA Changes, and Alarms.
 */
public abstract class AbstractEhaWatcherApp implements IResponderAppHelper {

    protected final String         appName;
    protected final EhaMessageType messageType;
    protected       String         templateName;

    /**
     * Short command line option for the channel ID list
     */
    public static final String CHANNEL_IDS_SHORT = "z";

    /**
     * Long command line option for the channel ID list
     */
    public static final String CHANNEL_IDS_LONG = "channelIds";

    /**
     * Short command line option for the recorded flag
     */
    public static final String RECORDED_SHORT = "a";

    /**
     * Long command line option for the recorded flag
     */
    public static final String RECORDED_LONG = "recordedOnly";

    /**
     * Short command line option for the realtime flag
     */
    public static final String REALTIME_SHORT = "r";

    /**
     * Long command line option for the realtime flag
     */
    public static final String REALTIME_LONG = "realtimeOnly";

    /**
     * Short command line option for the channel ID file
     */
    public static final String CHANNEL_ID_FILE_SHORT = "q";

    /**
     * Long command line option for the channel ID file
     */
    public static final String CHANNEL_ID_FILE_LONG = "channelIdFile";

    /**
     * Short command line option for the DSS ID list
     */
    public static final String DSS_ID_SHORT = "D";

    /**
     * Long command line option for the DSS ID list
     */
    public static final String DSS_ID_LONG = "dssId";

    private boolean  exitWithSession = false;
    private String[] channelIds;

    private boolean wantRecorded;

    /* DSS IDs to filter for */
    private DssIdFilter dssIds = null;

    private final ApplicationContext    appContext;
    private final ExitWithSessionOption exitOption        = new ExitWithSessionOption();
    private final ChannelOption         channelIdsOption  = new ChannelOption(CHANNEL_IDS_SHORT, CHANNEL_IDS_LONG,
            "channelIdList",
            "comma-separated list of Channel IDs and ranges to process (e.g. A-0001..A-0200,B-1234)",
            null, null, false);
    private final OutputFormatOption    formatOption      = new OutputFormatOption(false);
    private final FlagOption            recordedOption    = new FlagOption(RECORDED_SHORT, RECORDED_LONG,
            "watch recorded channels only", false);
    private final FlagOption            realtimeOption    = new FlagOption(REALTIME_SHORT, REALTIME_LONG,
            "watch realtime channels only (default)", false);
    private final FileOption            channelFileOption = new FileOption(CHANNEL_ID_FILE_SHORT, CHANNEL_ID_FILE_LONG,
            "filename",
            "name of a file containing a list of channel names", false, true);
    private       DssIdListOption       dssIdsOption;

    /**
     * Constructor
     *
     * @param appContext      spring application context
     * @param appName         application name
     * @param messageType     EHA message type
     * @param defaultTemplate default output template
     */
    public AbstractEhaWatcherApp(ApplicationContext appContext, String appName, EhaMessageType messageType,
                                 String defaultTemplate) {
        this.appContext = appContext;
        this.appName = appName;
        this.messageType = messageType;
        this.templateName = defaultTemplate;
    }

    public String getUsageText() {
        final StringBuilder sb = new StringBuilder(
                "Usage: " + appName + " [session options] [jms options] [database options] [--printLog\n");
        sb.append(
                "                             --channelIds <channelIdList> --channelIdFile <filename> --dssId <integer,...>\n");
        sb.append(
                "                             --outputFormat <format> --exitWithSession --realtimeOnly --recordedOnly]\n");
        sb.append("       " + appName + " --topics <topic-list> [jms options] [database options] [--printLog\n");
        sb.append(
                "                             --channelIds <channelIdList> --channelIdFile <filename> --dssId <integer,...>\n");
        sb.append(
                "                             --outputFormat <format> --exitWithSession --realtimeOnly --recordedOnly]\n");
        return sb.toString();
    }

    @Override
    public void addAppOptions(final BaseCommandOptions opt) {

        opt.addOption(this.exitOption);
        opt.addOption(this.channelIdsOption);
        opt.addOption(this.channelFileOption);
        this.formatOption.setDefaultValue(templateName);
        opt.addOption(this.formatOption);
        opt.addOption(this.recordedOption);
        opt.addOption(this.realtimeOption);

        this.dssIdsOption = new DssIdListOption(DSS_ID_SHORT, DSS_ID_LONG, "integer,...",
                "A comma-separated list of station IDs for filtering station monitor channels only. Any --sessionDssId filter will be applied first if also supplied",
                false, false, appContext.getBean(MissionProperties.class));
        opt.addOption(this.dssIdsOption);
    }

    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {

        final Collection<String> channelIdsAsList = this.channelIdsOption.parse(commandLine);

        if (channelIdsAsList != null && !channelIdsAsList.isEmpty()) {
            channelIds = channelIdsAsList.toArray(new String[]{});
        }

        final String chanIdFile = this.channelFileOption.parse(commandLine);

        // parse and store the list of channel IDs from a file (if it exists)
        if (chanIdFile != null) {
            List<String> channelIdsFromFile = null;
            try {
                channelIdsFromFile = retrieveChannelIds(chanIdFile);
            } catch (final FileNotFoundException e) {
                throw new ParseException("Could not find channel ID file " + chanIdFile);
            } catch (final IOException e) {
                throw new ParseException("IO error reading channel ID file " + chanIdFile);
            } catch (final ChannelListRangeException e) {
                throw new ParseException("Channel ID file " + chanIdFile + " contains illegal channel range(s)");
            }

            if (channelIdsFromFile.isEmpty()) {
                throw new ParseException("Found empty channel id file "
                        + chanIdFile);
            }

            // now append the file channel ids to the ids from the command line if
            // necessary
            // or build the id array from the file ids
            if (!channelIdsFromFile.isEmpty()) {
                if ((channelIds == null)) {
                    channelIds = new String[0];
                }
                if (channelIds.length == 0) {
                    channelIds = channelIdsFromFile.toArray(channelIds);
                } else {
                    for (int icount = 0; icount < channelIds.length; ++icount) {
                        if (!channelIdsFromFile.contains(channelIds[icount])) {
                            channelIdsFromFile.add(channelIds[icount]);
                        }
                    }

                    channelIds = channelIdsFromFile.toArray(channelIds);
                }
            }
        }

        exitWithSession = exitOption.parse(commandLine);
        templateName = formatOption.parseWithDefault(commandLine, false, true);

        // realtime only
        if (commandLine.hasOption(realtimeOption.getLongOpt())) {
            wantRecorded = false;
        } else if (commandLine.hasOption(recordedOption.getLongOpt())) {
            wantRecorded = true;
        }

        /* Parse DSS ID command line option */
        dssIds = this.dssIdsOption.parse(commandLine);
    }

    /**
     * retrieves the channel ids from a file and returns them in an array of strings. This array is empty( not null ) if
     * a problem was found.
     *
     * @param channelIdFile -- the file containing the channel ids to be processed
     * @return fileChannelIds -- the array of channel ids which were found in the channel Id file. This array is
     * concatenated with channelIds to form the complete set of channelIds to fetch
     * @throws RuntimeException
     */
    private List<String> retrieveChannelIds(final String channelIdFile)
            throws IOException, ChannelListRangeException, ParseException {
        final List<String> channelIdCollection = new ArrayList<>();
        long               lineCounter         = 0;

        BufferedReader myReader = null;

        try {
            // open the file
            myReader = new BufferedReader(new FileReader(
                    channelIdFile));
            String currentLine;

            // loop over the lines in the file
            do {
                lineCounter++;

                try {
                    currentLine = myReader.readLine();
                } catch (final IOException ioException) {
                    throw new ParseException("Failed to read line "
                            + lineCounter + " of channel id file "
                            + channelIdFile);
                }

                if (currentLine == null || currentLine.isEmpty() || currentLine.trim().startsWith("#")) {
                    continue;
                }

                final String[] currentIds = currentLine.split(",{1}");
                for (int i = 0; i < currentIds.length; i++) {
                    // Strip off comment at end of line
                    if (currentIds[i].trim().indexOf('#') != -1) {
                        currentIds[i] = currentIds[i].trim().substring(0, currentIds[i].indexOf('#'));
                    }
                }
                final String[] rangeSplitCurrentIds = (new ChannelListRange()).genChannelListFromRange(currentIds);

                for (int icount = 0; icount < rangeSplitCurrentIds.length; ++icount) {
                    if (rangeSplitCurrentIds[icount].length() > 0) {
                        channelIdCollection.add(rangeSplitCurrentIds[icount].trim());
                    }
                }
            } while (currentLine != null);

        } finally {
            myReader.close();
        }

        return channelIdCollection;
    }

    /**
     * Gets the flag indicating whether the application should exit when the session ends.
     *
     * @return true if the application should exit when an end of session message is received
     */
    public boolean isExitSession() {
        return exitWithSession;
    }

    /**
     * Gets the list of channel IDs to watch for
     *
     * @return an array of channel IDs
     */
    public String[] getChannelIds() {
        if (channelIds == null) {
            return null;
        } else {
            return channelIds.clone();
        }
    }

    /**
     * Gets the name of the output template (format) for channel records
     *
     * @return the template name
     */
    public String getTemplateName() {
        return templateName;
    }

    @Override
    public String[] getOverrideTypes() {
        List<String> overrideTypes      = getOverrideTypesList();
        String[]     overrideTypesArray = new String[overrideTypes.size()];
        return overrideTypes.toArray(overrideTypesArray);
    }

    /**
     * Get the override types List
     *
     * @return override types list
     */
    protected List<String> getOverrideTypesList() {
        List<String> overrideTypes = new ArrayList<>();
        overrideTypes.add(SessionMessageType.StartOfSession.getSubscriptionTag());
        overrideTypes.add(SessionMessageType.EndOfSession.getSubscriptionTag());
        overrideTypes.add(SessionMessageType.SessionHeartbeat.getSubscriptionTag());
        return overrideTypes;
    }

    @Override
    public IContextConfiguration getContextConfiguration() {
        return null;
    }

    @Override
    public void setContextConfiguration(final IContextConfiguration config) {
        SystemUtilities.doNothing();
    }

    /**
     * Get recorded flag
     *
     * @return true if recorded is wanted, false otherwise
     */
    public boolean isRecorded() {
        return wantRecorded;
    }

    /**
     * Get station id filter object.
     *
     * @return Station ID filter.
     */
    public DssIdFilter getDssIdFilter() {
        return dssIds;
    }

    @Override
    public String getAdditionalHelpText() {
        final String[] styles = MessageTemplateManager.getTemplateStylesForHelp(messageType);
        if (styles.length == 0) {
            // OK to system.out this rather than trace; it's part of the help text
            return ("\nA list of formatting styles in not currently available.");
        }
        final StringBuilder result = new StringBuilder("\nAvailable formatting styles are:");
        for (int i = 0; i < styles.length; i++) {
            if (i % 4 == 0) {
                result.append("\n");
                result.append("   ");
            }
            result.append(styles[i] + " ");
        }
        result.append("\n");
        result.append(MessageTemplateManager.getTemplateDirectoriesForHelp(messageType));
        return result.toString();
    }

    @Override
    public ApplicationContext getApplicationContext() {
        return this.appContext;
    }
}
