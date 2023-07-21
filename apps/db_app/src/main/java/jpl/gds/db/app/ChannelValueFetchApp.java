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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.ParseException;

import jpl.gds.cli.legacy.options.DssVcidOptions;
import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.db.api.sql.fetch.AlarmControl;
import jpl.gds.db.api.sql.fetch.ChannelTypeSelect;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.order.IChannelValueOrderByType;
import jpl.gds.db.api.sql.order.OrderByType;
import jpl.gds.db.mysql.impl.sql.order.ChannelValueOrderByType;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.TimeUtility;


/**
 * The channel value fetch app is the command line application used to retrieve
 * channel values from the database.
 * 
 * NB: plotonly is just a variation of chartreport.
 *
 */
public class ChannelValueFetchApp extends AbstractChannelValueFetchApp
{
    private static final String  APP_NAME                 = ApplicationConfiguration.getApplicationName("chill_get_chanvals");

    /** Long option for includePacketInfo */
    public static final String   INCLUDE_PACKET_INFO_LONG = "includePacketInfo";

    /** Long option for fromUniquePackets */
    public static final String   FROM_UNIQUE_PACKETS_LONG = "fromUniquePackets";

    /** Size of query parameter array Increment */
    public static final int      NUM_QUERY_PARAMS         = 11;

    /**
     * Alarms to query for
     */
    protected final AlarmControl alarms                   = new AlarmControl();

    /** If true, join with packet */
    private boolean              includePacketInfo        = false;

    /** If true, remove duplicates */
    private boolean              fromUniquePackets        = false;

	/**
	 * Creates an instance of ChannelValueFetchApp.
     *
	 */
	public ChannelValueFetchApp()
	{
		super(APP_NAME, "ChanvalQuery");

        suppressInfo();
	}

    /**
     * 
     * @param appName
     *            the name of the applicaation
     * @param app
     *            CSV application type
     */
    protected ChannelValueFetchApp(final String appName, final String app) {
        super(appName, app);
    }


    /**
     * Get alarms.
     *
     * @return Alarms
     */
	public AlarmControl getAlarms()
	{
		return alarms;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	protected void addGlobalContext()
	{
		DateFormat df = null;
		try
		{
			df = TimeUtility.getFormatterFromPool();

			final int scid = missionProps.getDefaultScid();
			final String name =  missionProps.mapScidToName(scid);

			globalContext.put("spacecraftID", scid);
			if (name != null) {
				globalContext.put("spacecraftName", name);
			} else {
				globalContext.put("spacecraftName", "");
			}
			globalContext.put("productCreationTime",df.format(new AccurateDateTime()));
			globalContext.put("missionName", missionProps.getMissionLongName());
			if (beginTimeString != null) {
				globalContext.put("startTime",beginTimeString);
			} else {
				globalContext.put("startTime","");	
			}
			if (endTimeString != null) {
				globalContext.put("stopTime",endTimeString);
			} else {
				globalContext.put("stopTime","");
			}

            final String[] channelIds = getChannelIds();

			final StringBuilder channelList = new StringBuilder(1024);
			channelList.append("[");
			if(channelIds.length > 0)
			{
				channelList.append(channelIds[0]);
				for(int i=1; i < channelIds.length; i++)
				{
					channelList.append(" ");
					channelList.append(channelIds[i]);
				}
			}
			channelList.append("]");
			globalContext.put("channelValues",channelList.toString());
		}
		finally
		{
			if (df != null)
			{
				TimeUtility.releaseFormatterToPool(df);
			}
		}
	}



    /**
     * {@inheritDoc}
     */
	@Override
	public void createRequiredOptions() throws ParseException
    {
		super.createRequiredOptions();

		requiredOptions.add(ALARM_ONLY_LONG);
	}


    /**
     * {@inheritDoc}
     */
	@Override
	public void configureApp(final CommandLine cmdline) throws ParseException {

		super.configureApp(cmdline);

		if (orderByString != null)
        {
            IChannelValueOrderByType cvobt = null;

			try
			{
                cvobt = new ChannelValueOrderByType(orderByString.trim());
			}
			catch (final IllegalArgumentException iae)
			{
				throw new ParseException("The value '" +
                                         orderByString +
                                         "' is not a legal ordering value for this application");
			}

            if (! cmdline.hasOption(INCLUDE_PACKET_INFO_LONG) &&
                cvobt.getPacketRequired())
            {
				throw new ParseException("The order-by "          +
                                         cvobt                    +
                                         " is only valid when --" +
                                         INCLUDE_PACKET_INFO_LONG  +
                                         " is specified");
            }
		}

		// Are all channel values required or just changes?
		allChannelValues = ! cmdline.hasOption(CHANGE_VALUES_LONG);

        if (! allChannelValues      &&
            (orderByString != null) &&
            orderByString.equalsIgnoreCase(ChannelValueOrderByType.NONE.getValueAsString()))
        {
            throw new ParseException("--"                                        +
                                     CHANGE_VALUES_LONG                         +
                                     " does not work properly with order-by of " +
                                     ChannelValueOrderByType.NONE);
        }

		if(cmdline.hasOption(ALARM_ONLY_LONG))
		{
			alarms.processOptions(cmdline.getOptionValue(ALARM_ONLY_LONG),ALARM_ONLY_LONG);
		}

        final List<ChannelValueOrderByType> obt =
           new ArrayList<ChannelValueOrderByType>();

        if (cmdline.hasOption(ORDER_BY_LONG))
        {
            String obString = cmdline.getOptionValue(ORDER_BY_SHORT);

            if (obString == null)
            {
                throw new MissingArgumentException("--" +
                                                   ORDER_BY_LONG +
                                                   " requires an argument");
            }

            obString = obString.trim();

            try
            {
                obt.add(new ChannelValueOrderByType(obString));
            }
            catch (final IllegalArgumentException iae)
            {
                throw new MissingArgumentException("Bad value '" +
                                                   obString      +
                                                   "' for --"    +
                                                   ORDER_BY_LONG);
            }
        }
        else
        {
            obt.add(ChannelValueOrderByType.ERT);
        }

        includePacketInfo = cmdline.hasOption(INCLUDE_PACKET_INFO_LONG);


        if (includePacketInfo)
        {
        	resetApplicationType("ChanvalPacketQuery");
        }

        fromUniquePackets = cmdline.hasOption(FROM_UNIQUE_PACKETS_LONG);

        // Process channel type selection arguments

        final ChannelTypeSelect cts = getChannelTypeSelect();

        getChannelTypes(cmdline, cts, obt);

        setVcids(DssVcidOptions.parseVcid(
                     missionProps, 
                     cmdline,
                     cts.monitor ? 'M' : null,
                     null,
                     CHANNEL_TYPES_LONG));

        if (fromUniquePackets   &&
            ! includePacketInfo &&
            (cts.fswRealtime ||
             cts.fswRecorded ||
             cts.header      ||
             cts.sse         ||
             cts.sseHeader))
        {
                throw new ParseException("--"                     +
                                         FROM_UNIQUE_PACKETS_LONG +
                                         " requires --"           +
                                         INCLUDE_PACKET_INFO_LONG +
                                         " for f, r, h, s, and g channels");
        }


        processIndexHints(cmdline);
	}




    /**
     * {@inheritDoc}
     */
	@Override
	public String getUsage()
	{

        final String NL = "\n[--";

		return (APP_NAME + " --" + CHANNEL_TYPES_LONG + " <hmsfrg>" +
                NL + CHANNEL_IDS_LONG + " <string,...> |"           +
                " --" + CHANNEL_ID_FILE_LONG + " <string>]"         +
                NL + MODULE_LONG + " <name>]"                       +
                NL + CHANGE_VALUES_LONG + "]"                       +
                NL + INCLUDE_PACKET_INFO_LONG + "]"                 +
                NL + FROM_UNIQUE_PACKETS_LONG + "]"                 +
                "\n[" + getExtraUsage() + "]"                       +
				"\n[Session search options - Not required]\n");
	}



    /**
     * {@inheritDoc}
     */
	@Override
    protected void showHelp()
	{
		super.showHelp();

        System.out.println("\nFor --alarmOnly, the possible alarm types are:\n" +
                           "    ANY\n"                                          +
                           "    DN DN-R DN-Y DN-RY DN-YR\n"                     +
                           "    EU EU-R EU-Y EU-RY EU-YR\n"                     +
                           "    RED R YELLOW Y\n");
	}


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
    @Override
	protected void addAppOptions()
	{
		super.addAppOptions();

		OptionBuilder.withLongOpt(CHANGE_VALUES_LONG);
		OptionBuilder.withDescription("Channel values should only be reported on change; default is all channel values");
		options.addOption(OptionBuilder.create(CHANGE_VALUES_SHORT));

		addOption(null,
                  CHANNEL_TYPES_LONG,
                  "string",
                  ChannelTypeSelect.RETRIEVE);

		addOption(null,
                  USE_INDEX_LONG,
                  "string,...",
                  "Use index");

		addOption(null,
                  FORCE_INDEX_LONG,
                  "string,...",
                  "Force index");

		addOption(null,
                  IGNORE_INDEX_LONG,
                  "string,...",
                  "Ignore index");

        options.addOption(ReservedOptions.createOption(
                              INCLUDE_PACKET_INFO_LONG,
                              null,
                              "Include info from Packet"));

        options.addOption(ReservedOptions.createOption(
                              FROM_UNIQUE_PACKETS_LONG,
                              null,
                              "Include info from unique Packets only" +
                                  " (also works for monitor)"));

		addOption(ALARM_ONLY_SHORT,ALARM_ONLY_LONG,"alarm,...","Only retrieve values in alarm");
		addOrderOption();
	}


    /**
     * {@inheritDoc}
     */
	@Override
	public IDbSqlFetch getFetch(final boolean sqlStmtOnly)
	{
		fetch = appContext.getBean(IDbSqlFetchFactory.class).getChannelValueFetch(isSqlStmtOnly(),
                        getChannelIds(),
                        (getModule() != null) ? new String[] {getModule()}
                                              : null,
                        includePacketInfo,
                        false,
                        fromUniquePackets);
		return fetch;
	}


    /**
     * {@inheritDoc}
     */
	@Override
	public Object[] getFetchParameters()
	{
		final Object[] params = new Object[NUM_QUERY_PARAMS];

		params[0]  = getChannelIds();
		params[1]  = getModule();
        params[2]  = getChannelTypeSelect();
		params[3]  = getOrderings();
		params[4]  = allChannelValues;
		params[5]  = alarms;
        params[6]  = Boolean.FALSE; // Descending
        params[7]  = getVcids();
        params[8]  = getDssIds();
        params[9]  = null; // APID holder
        params[10] = getIndexHints();

		return params;
	}


    /**
     * Get list of possible orderings.
     *
     * @return List of possible orderings
     */
	private List<IChannelValueOrderByType> getOrderings()
	{
		final List<IChannelValueOrderByType> orders = new ArrayList<IChannelValueOrderByType>();
        IChannelValueOrderByType orderType = (IChannelValueOrderByType) orderByTypeFactory.getOrderByType(OrderByType.CHANNEL_VALUE_ORDER_BY);
		if(orderByString != null)
		{
			try
			{
				orderType = new ChannelValueOrderByType(orderByString.trim());
			}
			catch(final IllegalArgumentException iae)
			{
				throw new IllegalArgumentException("The value \"" + orderByString + "\" is not a legal ordering value for this application.",iae);
			}
			orders.add(orderType);
		} else {
			if (times != null) {
				switch(times.getTimeType().getValueAsInt())
                {
				case DatabaseTimeType.ERT_TYPE:
					orderType = ChannelValueOrderByType.ERT;
					break;
				case DatabaseTimeType.SCET_TYPE:
					orderType = ChannelValueOrderByType.SCET;
					break;
				case DatabaseTimeType.SCLK_TYPE:
					orderType = ChannelValueOrderByType.SCLK;
					break;
				case DatabaseTimeType.LST_TYPE:
					orderType = ChannelValueOrderByType.LST;
					break;
				case DatabaseTimeType.RCT_TYPE:
					orderType = ChannelValueOrderByType.RCT;
					break;
				default:
					orderType = ChannelValueOrderByType.ERT;
					break;
				}
			}
			orders.add(orderType);
		}
		return(orders);
	}


    /**
     * {@inheritDoc}
     */
    @Override
    public DatabaseTimeType getDefaultTimeType()
    {
        return DatabaseTimeType.ERT;
    }


    /**
     * {@inheritDoc}
     */
	@Override
	public void checkTimeType(final DatabaseTimeRange range) throws ParseException
	{
		switch(range.getTimeType().getValueAsInt())
		{
		case DatabaseTimeType.SCET_TYPE:
		case DatabaseTimeType.ERT_TYPE:
		case DatabaseTimeType.SCLK_TYPE:
		case DatabaseTimeType.LST_TYPE:
		case DatabaseTimeType.RCT_TYPE:

			break;

		default:

			throw new ParseException("TimeType is not one of: SCET, ERT, SCLK, RCT");
		}
	}


    /**
     * {@inheritDoc}
     */
	@Override
	public String[] getOrderByValues()
	{
		return (ChannelValueOrderByType.orderByTypes);
	}


    /**
     * Process arguments for selecting the channel types.
     *
     * NB: Called from CTab, etc.
     *
     * @param cl  Command line
     * @param cts Object to populate with selections
     * @param obt List of order-by types
     *
     * @throws ParseException           Thrown on parameter error
     * @throws MissingArgumentException Thrown on missing value
     */
    @Override
    public void getChannelTypes(final CommandLine cl, final ChannelTypeSelect cts,
            final List<ChannelValueOrderByType> obt)
        throws ParseException,
               MissingArgumentException
    {
        super.getChannelTypes(cl, cts, obt);
		final boolean packet = cl.hasOption(INCLUDE_PACKET_INFO_LONG);
        if (cts.monitor && packet)
        {
            throw new ParseException("Cannot set both --" +
                                     CHANNEL_TYPES_LONG   +
                                     " M and --"          +
                                     INCLUDE_PACKET_INFO_LONG);
        }
    }


    /**
     * Perform extra writes to context as needed.
     *
     * @param context Context to write to
     */
    @Override
    protected void writeHeaderMetaDataExtra(final Map<String, Object> context)
    {
        context.put("hasPacket", includePacketInfo);
    }


	/**
	 * The main method to run the application
	 * 
	 * @param args The command line arguments
	 */
	public static void main(final String[] args)
	{
		final ChannelValueFetchApp app = new ChannelValueFetchApp();
		app.runMain(args);
	}
}
