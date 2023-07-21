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

import java.io.FileNotFoundException;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;

import jpl.gds.cli.legacy.options.DssVcidOptions;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.db.api.sql.fetch.ApidRanges;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.fetch.SpscRanges;
import jpl.gds.db.api.sql.order.IPacketOrderByType;
import jpl.gds.db.mysql.impl.sql.order.PacketOrderByType;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;


/**
 * The PacketFetchApp is the command line application used to
 * query packets and packet metadata out of the database.
 *
 */
public class PacketFetchApp extends AbstractSocketFetchApp
{
    /**  New parent class */

    private static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_get_packets");

    private static final int NUM_QUERY_PARAMS = 9;

    /** Long option for packet types. Used by GetEverythingApp. */
    public static final String PACKET_TYPES_LONG = "packetTypes";

    /** The APIDs to query by */
    private final ApidRanges apid;

    /** The SPSCs to query by */
    private final SpscRanges spsc;

    /** VCIDs to query for */
    private Set<Integer> vcids = null;

    /** DSS ids to query for */
    private Set<Integer> dssIds = null;

    /** Packet types desired */
    private final PacketTypeSelect packetTypeSelect =
        new PacketTypeSelect();

    /** True if we must restore headers and trailers to bodies */
    private boolean attachHeadersAndTrailers = false;

    
    /**
     * Creates an instance of PacketFetchApp.
     */
    public PacketFetchApp()
    {
        super(IDbTableNames.DB_PACKET_DATA_TABLE_NAME,
              APP_NAME,
              "PacketQuery");

        suppressInfo();
        
        this.apid = new ApidRanges();
        this.spsc = new SpscRanges();
    }


    /**
     * Get APID ranges.
     *
     * @return APID ranges
     */
    public ApidRanges getApid()
	{
		return this.apid;
	}

    /**
     * Get SPSC ranges.
     *
     * @return SPSC ranges
     */
	public SpscRanges getSpsc()
	{
		return this.spsc;
	}
	
    /**
     * {@inheritDoc}
     */
    @Override
    public void createRequiredOptions() throws ParseException
    {    	
    	super.createRequiredOptions();
        this.requiredOptions.add(BEGIN_TIME_LONG);
        this.requiredOptions.add(END_TIME_LONG);
        this.requiredOptions.add(PACKET_SPSC_LONG);
        this.requiredOptions.add(PACKET_APID_LONG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configureApp(final CommandLine cmdline) throws ParseException
    {
        super.configureApp(cmdline);

        /**
         * Do this before the file can be created
         */
        checkFilename(cmdline, false);

        if (cmdline.hasOption(FILE_SHORT))
        {

            final String outFile =
                StringUtil.emptyAsNull(cmdline.getOptionValue(FILE_SHORT));

        	if (outFile == null)
        	{
        		throw new MissingArgumentException("--" + FILE_LONG + " requires a filename argument");
        	}

            try
            {
                setOutputFilename(outFile);
            }
            catch (final FileNotFoundException fnfe)
            {
                throw new ParseException("File " +
                                         outFile +
                                         " cannot be created");
            }
        }

        // If specified, restore headers and trailers to bodies
        attachHeadersAndTrailers = parseRestoreOption(cmdline);

        if (attachHeadersAndTrailers &&
            (getOutputFilename() == null) &&
            ! cmdline.hasOption(SOCKET_HOST_LONG))
        {
            throw new MissingOptionException("Cannot specify --"  +
                                             RESTORE_LONG         +
                                             " without either --" +
                                             FILE_LONG            +
                                             " or --"             +
                                             SOCKET_HOST_LONG);
        }

        if(cmdline.hasOption(PACKET_APID_SHORT))
        {
            apid.processOptions(cmdline.getOptionValue(PACKET_APID_SHORT),PACKET_APID_SHORT);
        }

        if(cmdline.hasOption(PACKET_SPSC_SHORT))
        {
            spsc.processOptions(cmdline.getOptionValue(PACKET_SPSC_SHORT),PACKET_SPSC_SHORT);
        }

        // See if we're supposed to print out to the console

        this.reportRows = (! cmdline.hasOption(SOCKET_HOST_LONG) &&
                           (cmdline.hasOption(REPORT_LONG) ||
                            ! cmdline.hasOption(FILE_LONG)));

        // Process packet type selection arguments
        PacketFetchApp.getPacketTypes(dbProperties, cmdline, packetTypeSelect);

		if (packetTypeSelect.fsw && packetTypeSelect.sse)
		{
			throw new ParseException("Cannot set both F and S for --" +
					                 PACKET_TYPES_LONG);
		}

        fromSse = packetTypeSelect.sse;

        vcids  = DssVcidOptions.parseVcid(missionProps, cmdline,
                                          packetTypeSelect.sse ? 'S' : null,
                                          null,
                                          PACKET_TYPES_LONG);

        dssIds = DssVcidOptions.parseDssId(cmdline,
                                           packetTypeSelect.sse ? 'S' : null,
                                           PACKET_TYPES_LONG);

        checkOutputFormat(cmdline);

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public IDbSqlFetch getFetch(final boolean sqlStmtOnly)
    {
        fetch = appContext.getBean(IDbSqlFetchFactory.class).getPacketFetch(sqlStmtOnly);
    	return fetch;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getFetchParameters()
    {
    	final Object[] params = new Object[NUM_QUERY_PARAMS];
    	
    	IPacketOrderByType orderType = PacketOrderByType.DEFAULT;
		if(this.orderByString != null)
		{
			try
			{
				orderType = new PacketOrderByType(this.orderByString.trim());
			}
			catch(final IllegalArgumentException iae)
			{
				throw new IllegalArgumentException("The value \"" + this.orderByString + "\" is not a legal ordering value for this application.",iae);
			}
		} 	else if (this.times != null) {
			switch(this.times.getTimeType().getValueAsInt()) {
			case DatabaseTimeType.ERT_TYPE:
				orderType = PacketOrderByType.ERT;
				break;
			case DatabaseTimeType.SCET_TYPE:
				orderType = PacketOrderByType.SCET;
				break;
			case DatabaseTimeType.LST_TYPE:
				orderType = PacketOrderByType.LST;
				break;
			case DatabaseTimeType.SCLK_TYPE:
				orderType = PacketOrderByType.SCLK;
				break;
			case DatabaseTimeType.RCT_TYPE:
				orderType = PacketOrderByType.RCT;
				break;
            default:
                break;
			}
		}
		
		params[0] = this.apid;
		params[1] = vcids;
		params[2] = this.spsc;
		params[3] = this.fromSse;
		params[4] = null;
		params[5] = orderType;

		params[6] = ((getOutputFilename() != null) ||
                     usingSocket());

        params[7] = dssIds;
    	params[8] = attachHeadersAndTrailers;

    	return params;
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
            case DatabaseTimeType.RCT_TYPE:
            case DatabaseTimeType.LST_TYPE:

                break;

            default:

                throw new ParseException("TimeType is not one of: SCET, ERT, SCLK, RCT, LST");
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public DatabaseTimeType getDefaultTimeType()
    {
        return(DatabaseTimeType.ERT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getOrderByValues()
    {
        return(PacketOrderByType.orderByTypes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUsage()
    {
        return(APP_NAME + " " +
                "[--" + BEGIN_TIME_LONG + " <time> " +
                "--" + END_TIME_LONG + " <time>\n" + "                  " +
                "--" + TIME_TYPE_LONG + " <" +TIME_TYPE_ARG + "> " +
                "--" + FILE_LONG + " <filename> " +
                "--" + REPORT_LONG + "\n" + "                  " +
                "--" + PACKET_SPSC_LONG + " <int,...>\n                  " +
                "--" + PACKET_APID_LONG + " <int,...>\n" +
                "                  " +
                getExtraUsage() +
                "]\n" +
                "                  " +
                "[Session search options - Not required]\n");
    }

    /**
     * {@inheritDoc}
     */
	@Override
    protected void addAppOptions()
    {
        super.addAppOptions();

        addOption(OUTPUT_FORMAT_SHORT,OUTPUT_FORMAT_LONG,"format", OUTPUT_FORMAT_DESC);
    	addOption(SHOW_COLUMNS_SHORT,SHOW_COLUMNS_LONG, null, SHOW_COLUMNS_DESC);
        addOption(PACKET_APID_SHORT,PACKET_APID_LONG, "int,...", "Packet Application ID");
        addOption(TIME_TYPE_SHORT,TIME_TYPE_LONG, TIME_TYPE_ARG, "Time Type should be one of (SCLK,ERT,SCET,RCT,LST).  Default is " + getDefaultTimeType().getValueAsString());
        addOption(BEGIN_TIME_SHORT,BEGIN_TIME_LONG, "Time", "Begin time of packet range");
        addOption(END_TIME_SHORT,END_TIME_LONG, "Time", "End time of packet range");
        addOption(REPORT_SHORT,REPORT_LONG, null, "Report query results to console as text");
        addOption(FILE_SHORT,FILE_LONG, FILE_LONG, "Output file for packet data");
        addOption(PACKET_SPSC_SHORT,PACKET_SPSC_LONG,"int,...","Source Packet Seq Counter");
        addOrderOption();

		addOption(null,
                  PACKET_TYPES_LONG,
                  "string",
                  "Retrieve selected types: " +
                      "s=SSE "                +
                      "f=FSW");

        DssVcidOptions.addVcidOption(options);
        DssVcidOptions.addDssIdOption(options);

        addOption(null, RESTORE_LONG, null, "Restore headers and trailers to bodies");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void showHelp()
    {
        super.showHelp();
        
        System.out.println("\nMetadata for selected Packets will be written to standard output.");
        System.out.println("If -" + FILE_SHORT + " is specified, packet data will be written to an output file.");
        System.out.println("In this case, no metadata will be displayed unless the -" + REPORT_SHORT + " option is");
        System.out.println("supplied. The -" + OUTPUT_FORMAT_SHORT + " option can be used to select the metadata output style.");
        this.printTemplateStyles();
    }


    /**
     * Process arguments for selecting the packet types.
     * Used by GetEverythingApp.
     * 
     * @param dc the current DatabaseConfiguration
     *
     * @param cl  Command line
     * @param pts Object to populate with selections
     *
     * @throws ParseException           Thrown on parameter error
     * @throws MissingArgumentException Thrown on missing value
     */
    public static void getPacketTypes(final IMySqlAdaptationProperties dc,
    		                          final CommandLine      cl,
                                      final PacketTypeSelect pts)
        throws ParseException,
               MissingArgumentException
    {
        // --packetTypes (or default) MUST contain at least one
        // valid flag and CANNOT contain unknown flags

        String packetTypes =
            StringUtil.safeCompressAndUppercase(
                cl.hasOption(PACKET_TYPES_LONG)
                    ? cl.getOptionValue(PACKET_TYPES_LONG)
                    : dc.getPacketTypesDefault());

        if (packetTypes.isEmpty())
        {
            throw new MissingArgumentException(
                          "You must provide a value for --" +
                          PACKET_TYPES_LONG);
        }

        pts.sse = packetTypes.contains("S");
        pts.fsw = packetTypes.contains("F");

        packetTypes = packetTypes.replaceAll("[SF]", "");

        if (! packetTypes.isEmpty())
        {
            throw new ParseException("Unknown flags '" +
                                     packetTypes       +
                                     "' in --"         +
                                     PACKET_TYPES_LONG);
        }
    }


    /**
     * Main entry point to run application.
     *
     * @param args Arguments from command-line.
     */    
    public static void main(final String[] args)
    {
        final PacketFetchApp app = new PacketFetchApp();
        app.runMain(args);
    }


    /** Holder class for the two packet types. Used by GetEverythingApp. */
    public static class PacketTypeSelect extends Object
    {
        /** Set true for FSW packets */
        public boolean fsw = false;

        /** Set true for SSE packets */
        public boolean sse = false;


        /**
         * Constructor.
         */
        public PacketTypeSelect()
        {
            super();
        }
    }
}
