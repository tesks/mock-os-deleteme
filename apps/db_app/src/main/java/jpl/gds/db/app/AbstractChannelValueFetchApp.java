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
package jpl.gds.db.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.ParseException;

import jpl.gds.cli.legacy.options.DssVcidOptions;
import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.db.api.sql.fetch.ChannelTypeSelect;
import jpl.gds.db.api.sql.fetch.IChannelValueFetch;
import jpl.gds.db.api.sql.order.IChannelValueOrderByType;
import jpl.gds.db.mysql.impl.sql.order.ChannelValueOrderByType;
import jpl.gds.shared.channel.ChannelIdUtility;
import jpl.gds.shared.channel.ChannelListRangeException;
import jpl.gds.shared.string.StringUtil;


/**
 * This intermediary class captures some things that are common between
 * ChannelValueFetchApp and EcdrFetchApp. Subclasses may or may not need to
 * use anything in AbstractSocketFetchApp.
 *
 */
public abstract class AbstractChannelValueFetchApp
extends AbstractSocketFetchApp
{

	/** Channel types desired */
	private final ChannelTypeSelect channelTypeSelect =
			new ChannelTypeSelect();

	/**
	 * The list of channel IDs to query for
	 */
	private String[] channelIds = null;

	/**
	 * The module to query for.
	 */
	private String module = null;

	/** VCIDs to query for */
	private Set<Integer> vcids = null;

	/** DSS ids to query for */
	private Set<Integer> dssIds = null;

    /**
     * True if all channel values are required
     */
    protected boolean               allChannelValues  = true;

	/**
	 * Creates an instance of AbstractChannelValueFetchApp.
	 *
	 * @param appName Application name
     * @param app     CSV application type
	 */
	protected AbstractChannelValueFetchApp(final String appName,
                                           final String app)
	{
        super(IDbTableNames.DB_CHANNEL_VALUE_TABLE_NAME, appName, app);
	}


	/**
	 * Get selected channel types.
	 *
	 * @return Channel type object
	 */
	protected ChannelTypeSelect getChannelTypeSelect()
	{
		return channelTypeSelect;
	}


	/**
	 * Get channel-ids.
	 *
	 * @return Channel ids
	 */
	protected String[] getChannelIds()
	{
		return (channelIds != null)
				? Arrays.copyOf(channelIds, channelIds.length)
						: new String[0];
	}


	/**
	 * Get module.
	 *
	 * @return Module
	 */
	protected String getModule()
	{
		return module;
	}


	/**
	 * Get VCIDs.
	 *
	 * @return Set of VCID
	 */
	protected Set<Integer> getVcids()
	{
		return vcids;
	}


	/**
	 * Set VCIDs.
	 *
	 * @param theVcids Set of VCID
	 */
	protected void setVcids(final Set<Integer> theVcids)
	{
		vcids = theVcids;
	}


	/**
	 * Get station ids.
	 *
	 * @return Set of station ids
	 */
	protected Set<Integer> getDssIds()
	{
		return dssIds;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void createRequiredOptions() throws ParseException
	{
		super.createRequiredOptions();

		requiredOptions.add(BEGIN_TIME_LONG);
		requiredOptions.add(END_TIME_LONG);

		requiredOptions.add(CHANNEL_ID_FILE_LONG);
		requiredOptions.add(CHANNEL_IDS_LONG);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void configureApp(final CommandLine cmdline)
			throws ParseException
			{
		super.configureApp(cmdline);

		// parse and store the module name (if it exists)
		if(cmdline.hasOption(MODULE_LONG))
		{
			final String moduleString = cmdline.getOptionValue(MODULE_LONG);
			if(moduleString == null)
			{
				throw new MissingArgumentException(
						"--"        +
								MODULE_LONG +
								" requires a command line value");
			}

			module = moduleString.trim();
		}

		// Parse and store the channel id list from the command line
		// (if it exists)

		String chanIdString = null;

		if (cmdline.hasOption(CHANNEL_IDS_LONG))
		{
			chanIdString = cmdline.getOptionValue(CHANNEL_IDS_LONG);

			if (chanIdString == null)
			{
				throw new MissingArgumentException(
						"--" + CHANNEL_IDS_LONG +
						" requires a command line value");
			}

			chanIdString = chanIdString.trim();
		}

		// Finish parsing command line list of channel IDs

		channelIds = new String[0];

		if (chanIdString != null)
		{
			channelIds = chanIdString.split(",{1}");

			for(int i = 0; i < channelIds.length; i++)
			{
				channelIds[i] = channelIds[i].trim();
				if (ChannelIdUtility.isChanIdString(channelIds[i]) == false)
				{
					throw new ParseException("The input channel ID '"       +
							channelIds[i]                  +
							"' is not a valid channel ID." +
							"  Channel IDs should follow " +
							"the regular expression "      +
							ChannelIdUtility.CHANNEL_ID_REGEX);
				}
			}
		}

		// retrieve the channel Ids from a file if necessary
		List<String> channelIdsFromFile = new ArrayList<String>(0);
		if (cmdline.hasOption(CHANNEL_ID_FILE_LONG))
		{
			final String channelIdFile =
					cmdline.getOptionValue(CHANNEL_ID_FILE_LONG);

			channelIdsFromFile = parseChannelIdFile(channelIdFile);

			if (channelIdsFromFile.isEmpty())
			{
				throw new ParseException("Found empty channel id file " +
						channelIdFile);
			}
		}

		// Now append the file channel ids to the ids from the command line if
		// necessary or build the id array from the file ids

		if (! channelIdsFromFile.isEmpty())
		{
			if (channelIds == null)
			{
				channelIds = new String[0];
			}

			if (channelIds.length == 0)
			{
				channelIds = channelIdsFromFile.toArray(channelIds);
			}
			else
			{
				for (int icount = 0; icount < channelIds.length; ++icount)
				{
					if (! channelIdsFromFile.contains(channelIds[icount]))
					{
						channelIdsFromFile.add(channelIds[icount]);
					}
				}

				channelIds = channelIdsFromFile.toArray(channelIds);
			}
		}

		// Now remove ranges and clean up to make fetch happy

		try
		{
            channelIds = IChannelValueFetch.purify(channelIds);
		}
		catch (final ChannelListRangeException clre)
		{
			throw new ParseException("Bad channel range: " +
					clre.getMessage());
		}

		dssIds = DssVcidOptions.parseDssId(cmdline,
				null,
				CHANNEL_TYPES_LONG);
			}


	/**
	 * {@inheritDoc}
	 */
	@Override
    protected void showHelp()
	{
		super.showHelp();
		System.out.println("\nMetadata and values for selected Channels Values will be written to standard");
		System.out.println("output. The -" + OUTPUT_FORMAT_LONG + " option can be used to select the metadata output style.");
		this.printTemplateStyles();
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void addAppOptions()
	{
		super.addAppOptions();

		addOption(OUTPUT_FORMAT_SHORT,
				OUTPUT_FORMAT_LONG,
				"format",
				OUTPUT_FORMAT_DESC);

		addOption(SHOW_COLUMNS_SHORT,
				SHOW_COLUMNS_LONG,
				null,
				SHOW_COLUMNS_DESC);

		addOption(CHANNEL_IDS_SHORT,
				CHANNEL_IDS_LONG,
				"string",
				"A comma-separated list of channel names or ranges " +
				"or wildcards (e.g. A-0051,B-%,C-0123..C-0200)");

		addOption(TIME_TYPE_SHORT, TIME_TYPE_LONG, TIME_TYPE_ARG,
				"Time Type must be one of: (SCLK,ERT,SCET,LST). Default time " +
						"type is "                                                 +
						getDefaultTimeType().getValueAsString()                    +
				". Monitor, however, can support only ERT.");

		addOption(BEGIN_TIME_SHORT, BEGIN_TIME_LONG, "Time","Begin time of range");
		addOption(END_TIME_SHORT, END_TIME_LONG, "Time", "End time of range");

		addOption(CHANNEL_ID_FILE_SHORT, CHANNEL_ID_FILE_LONG, "string","The name of a file containing a list of channel names");
		addOption(MODULE_SHORT, MODULE_LONG, "name","A FSW module name pattern.");

		addOrderOption();

		DssVcidOptions.addVcidOption(options);
		DssVcidOptions.addDssIdOption(options);
	}

    /**
     * Get is-all-channel-values state.
     *
     * @return Is-all-channel-values state
     */
    public boolean isAllChannelValues() {
        return allChannelValues;
    }

    /**
     * Process arguments for selecting the channel types.
     *
     * NB: Called from CTab, etc.
     *
     * @param cl
     *            Command line
     * @param cts
     *            Object to populate with selections
     * @param obt
     *            List of order-by types
     *
     * @throws ParseException
     *             Thrown on parameter error
     * @throws MissingArgumentException
     *             Thrown on missing value
     */
    public void getChannelTypes(final CommandLine cl, final ChannelTypeSelect cts,
                                final List<ChannelValueOrderByType> obt)
            throws ParseException, MissingArgumentException {
        computeChannelTypes(cl, cts);

        final boolean ob = ((obt != null) && !obt.isEmpty());
        final boolean tt = cl.hasOption(TIME_TYPE_LONG);

        if (cts.monitor && tt) {
            final String type = StringUtil.safeTrim(cl.getOptionValue(TIME_TYPE_LONG));

            if ((type.length() > 0) && !type.equalsIgnoreCase("ERT") && !type.equalsIgnoreCase("RCT")) {
                throwTimeTypeException("M", type);
            }
        }

        if (cts.monitor && ob) {
            for (final ChannelValueOrderByType cvobt : obt) {
                switch (cvobt.getValueAsInt()) {
                    case ChannelValueOrderByType.SCLK_TYPE:
                    case ChannelValueOrderByType.SCET_TYPE:
                    case ChannelValueOrderByType.LST_TYPE:
                    case ChannelValueOrderByType.MODULE_TYPE:

                        throwOrderException("M", cvobt);
                        break;

                    default:
                        break;
                }
            }
        }

        if (cts.header && ob) {
            for (final ChannelValueOrderByType cvobt : obt) {
                if (cvobt.getValueAsInt() == ChannelValueOrderByType.MODULE_TYPE) {
                    throwOrderException("H", cvobt);
                }
            }
        }

        if (cts.sseHeader && ob) {
            for (final ChannelValueOrderByType cvobt : obt) {
                if (cvobt.getValueAsInt() == IChannelValueOrderByType.MODULE_TYPE) {
                    throwOrderException("G", cvobt);
                }
            }
        }
    }

    /**
     * Process arguments for selecting the channel types.
     * 
     * @param cl
     *            Command line
     * @param cts
     *            Object to populate with selections
     *
     * @throws ParseException
     *             Thrown on parameter error
     * @throws MissingArgumentException
     *             Thrown on missing value
     */
    protected void computeChannelTypes(final CommandLine cl, final ChannelTypeSelect cts)
            throws ParseException, MissingArgumentException {
        // --channelTypes (or default) MUST contain at least one
        // valid flag and CANNOT contain unknown flags

        String channelTypes = StringUtil.safeCompressAndUppercase(
                cl.hasOption(CHANNEL_TYPES_LONG) ? cl.getOptionValue(CHANNEL_TYPES_LONG)
                        : dbProperties.getChannelTypesDefault());

        if (channelTypes.isEmpty()) {
            throw new MissingArgumentException("You must provide a value for --" + CHANNEL_TYPES_LONG);
        }

        cts.monitor = channelTypes.contains("M");
        cts.header = channelTypes.contains("H");
        cts.fswRealtime = channelTypes.contains("F");
        cts.fswRecorded = channelTypes.contains("R");
        cts.sse = channelTypes.contains("S");

        cts.sseHeader = channelTypes.contains("G");

        channelTypes = channelTypes.replaceAll("[MHFRSG]", "");

        if (!channelTypes.isEmpty()) {
            throw new ParseException("Unknown flags '" + channelTypes + "' in --" + CHANNEL_TYPES_LONG);
        }

        final boolean tt = cl.hasOption(TIME_TYPE_LONG);
        final boolean module = cl.hasOption(MODULE_LONG);

        if (cts.monitor && tt) {
            final String type = StringUtil.safeTrim(cl.getOptionValue(TIME_TYPE_LONG));

            if (!type.equalsIgnoreCase("ERT") && !type.equalsIgnoreCase("RCT")) {
                throwTimeTypeException("M", type);
            }
        }

        if (cts.monitor && module) {
            throwModuleException("M");
        }

        if (cts.header && module) {
            throwModuleException("H");
        }

        if (cts.sseHeader && module) {
            throwModuleException("G");
        }
    }
}
