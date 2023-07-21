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
import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.db.api.sql.fetch.FrameQueryOptions;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.fetch.VcfcRanges;
import jpl.gds.db.api.sql.order.IFrameOrderByType;
import jpl.gds.db.mysql.impl.sql.order.FrameOrderByType;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;


/**
 * The FrameFetchApp is the command line application used to query
 * frames and frame metadata out of the database.
 *
 */
public class FrameFetchApp extends AbstractFetchApp
{
    private static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_get_frames");

    private static final int NUM_QUERY_PARAMS = 6;

    /** The frame type to query for */
    private String type;

    /** The frame relay ID to query for */
    private Long relayId;

    /** Flag indicating the app should restore the ASM to the frame body in the database if it is not present (TURBO frames only) */
    private boolean addAsm;

    /** Flag for bad frames only */
    private boolean badOnly;

    /** Flag for good frames only */
    private boolean goodOnly;

    /** VCFCs (and ranges) to query for */
    private final VcfcRanges vcfcs;

    /** VCIDs to query for */
    private Set<Integer> vcids = null;

    /** DSS ids to query for */
    private Set<Integer> dssIds = null;

    /** True if we must restore headers and trailers to bodies */
    private boolean attachHeadersAndTrailers = false;

    /** true if showing all header metadata */
    private boolean showAllMetadata = false;

    /** Long option for showing all header metadata */
    public static final String SHOW_ALL_META_LONG = "showAllMetadata";


    /**
     * Creates an instance of FrameFetchApp.
     */
    public FrameFetchApp()
    {
        super(IDbTableNames.DB_FRAME_DATA_TABLE_NAME,
              APP_NAME,
              "FrameQuery");

        suppressInfo();

        this.type = null;
        this.relayId = null;
        this.addAsm = false;
        this.badOnly = false;
        this.goodOnly = false;
        this.vcfcs = new VcfcRanges();
    }

    /**
     * Get type.
     *
     * @return Type
     */
    public String getType()
	{
		return this.type;
	}


    /**
     * Get relay id.
     *
     * @return Relay id
     */
	public Long getRelayId()
	{
		return this.relayId;
	}


    /**
     * Get add-asm state.
     *
     * @return Add-asm state
     */
	public boolean isAddAsm()
	{
		return this.addAsm;
	}

    /**
     * Get bad-only state.
     *
     * @return Bad-only state
     */
	public boolean isBadOnly()
	{
		return this.badOnly;
	}

    /**
     * Get good-only state.
     *
     * @return Good-only state
     */
	public boolean isGoodOnly()
	{
		return this.goodOnly;
	}

    /**
     * Get VCFCS ranges.
     *
     * @return VCFCS ranges
     */
	public VcfcRanges getVcfcs()
	{
		return this.vcfcs;
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
        this.requiredOptions.add(BAD_ONLY_LONG);
        this.requiredOptions.add(GOOD_ONLY_LONG);
        this.requiredOptions.add(FRAME_TYPE_LONG);
        this.requiredOptions.add(VCFCS_LONG);
        this.requiredOptions.add(RELAY_SCID_LONG);
    }

    /**
     * {@inheritDoc}
     */
	@Override
    public void configureApp(final CommandLine cmdline) throws ParseException
    {
        super.configureApp(cmdline);

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
                throw new ParseException("File "                +
                                         outFile                +
                                         " does not exist and " +
                                         "cannot be created");
            }
        }

        // If specified, restore headers and trailers to bodies
        attachHeadersAndTrailers = parseRestoreOption(cmdline);

        if (attachHeadersAndTrailers && (getOutputFilename() == null))
        {
            throw new ParseException("Cannot specify --" +
                                     RESTORE_LONG        +
                                     " without --"       +
                                     FILE_LONG);
        }

        if(cmdline.hasOption(FRAME_TYPE_SHORT))
        {
            final String typeString = cmdline.getOptionValue(FRAME_TYPE_SHORT);
            if(typeString == null)
            {
                throw new MissingArgumentException("The argument -" + FRAME_TYPE_SHORT + " requires a value");
            }
            this.type = typeString.trim();
        }


        if(cmdline.hasOption(RELAY_SCID_SHORT))
        {
            final String relayScidString = cmdline.getOptionValue(RELAY_SCID_SHORT);
            if(relayScidString == null)
            {
                throw new MissingArgumentException("The argument -" + RELAY_SCID_SHORT + " requires a value");
            }

            try
            {
                this.relayId = Long.valueOf(relayScidString.trim());
            }
            catch (final NumberFormatException e1)
            {
                throw new MissingOptionException("Relay Spacecraft ID is not a valid integer");
            }
            if (this.relayId < 0) {
                throw new ParseException("Invalid relay SCID value");
            }
        }
        
        // check for addAsm boolean
        this.addAsm = !cmdline.hasOption ( ADD_NO_ASM_SHORT );

        badOnly  = cmdline.hasOption(BAD_ONLY_SHORT);
        goodOnly = cmdline.hasOption(GOOD_ONLY_SHORT);

        if(badOnly && goodOnly)
        {
            throw new ParseException("Cannot specify both --badOnly and --goodOnly");
        }

        //see if we're supposed to be dumping to the console
        this.reportRows = false;
        this.reportRows = cmdline.hasOption(REPORT_SHORT) || !cmdline.hasOption(FILE_SHORT);

        // Check for VCFC values to query for
        if(cmdline.hasOption(VCFCS_SHORT))
        {
            vcfcs.processOptions(cmdline.getOptionValue(VCFCS_SHORT),VCFCS_SHORT);
        }

        vcids  = DssVcidOptions.parseVcid(missionProps, cmdline, (String) null);
        dssIds = DssVcidOptions.parseDssId(cmdline, (String) null);

        showAllMetadata = cmdline.hasOption(SHOW_ALL_META_LONG);
        if(showAllMetadata){
            attachHeadersAndTrailers = true;
            csvColumns.add("sleMetadata");
            csvHeaders.add("sleMetadata");
        }
    }


    /**
     * {@inheritDoc}
     */
	@Override
	public IDbSqlFetch getFetch(final boolean sqlStmtOnly)
	{
        fetch = appContext.getBean(IDbSqlFetchFactory.class).getFrameFetch(sqlStmtOnly);
		return fetch;
	}
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getFetchParameters()
    {
    	final Object[] params = new Object[NUM_QUERY_PARAMS];
    	
    	IFrameOrderByType orderType = FrameOrderByType.DEFAULT;
		if(this.orderByString != null)
		{
			try
			{
				orderType = new FrameOrderByType(this.orderByString.trim());
			}
			catch(final IllegalArgumentException iae)
			{
				throw new IllegalArgumentException("The value \"" + this.orderByString + "\" is not a legal ordering value for this application.",iae);
			}
		}
        else if (this.times != null)
        {
            switch (this.times.getTimeType().getValueAsInt())
            {
                case DatabaseTimeType.ERT_TYPE:
                    orderType = FrameOrderByType.ERT;
                    break;

                case DatabaseTimeType.RCT_TYPE:
                    orderType = FrameOrderByType.RCT;
                    break;

                default:
                    break;
            }
        }

		Boolean good = null;
        if(this.goodOnly)
        {
            good = Boolean.TRUE;
        }
        else if(this.badOnly)
        {
            good = Boolean.FALSE;
        }
		
		final FrameQueryOptions qo = new FrameQueryOptions();
		
        qo.setFrameType(this.type);
        qo.setVcid(vcids);
        qo.setDss(dssIds);
        qo.setRelayId(this.relayId);
        qo.setGood(good);
        qo.setVcfcs(this.vcfcs);

		params[0] = qo;
		params[1] = this.addAsm;
		params[2] = orderType;
        params[3] = null;

        // Need to join with FrameBody if showAllMetatada is true,
        // or we have an output file (with --restoreBodies)
		params[4] = (getOutputFilename() != null || showAllMetadata);

        params[5] = attachHeadersAndTrailers;
    	
    	return(params);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void checkTimeType(final DatabaseTimeRange range) throws ParseException
    {
        if((range.getTimeType().getValueAsInt() != DatabaseTimeType.ERT_TYPE) &&
           (range.getTimeType().getValueAsInt() != DatabaseTimeType.RCT_TYPE))
    	{
    		throw new ParseException("TimeType is not ERT or RCT");
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
        return(FrameOrderByType.orderByTypes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUsage()
    {

        return(APP_NAME + " [" +
                "--" + BEGIN_TIME_LONG + " <time> " +
                "--" + END_TIME_LONG + " <time> " +
                "--" + TIME_TYPE_LONG + " <" +TIME_TYPE_ARG + "> " +
                "--" + FILE_LONG + " <filename>\n" + "                 " +
                "--" + REPORT_LONG + " " +
                "--" + FRAME_TYPE_LONG + " <frameType> " +
                "--" + RELAY_SCID_LONG + " <scid>\n" + "                 " +
                "--" + GOOD_ONLY_LONG + " " +
                "--" + BAD_ONLY_LONG  + " " +
                "--" + REPORT_LONG    + " " +
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
        addOption(ADD_NO_ASM_SHORT,ADD_NO_ASM_LONG,null,"flag indicating NOT to reinstall ASM to TURBO frames");
        addOption(FRAME_TYPE_SHORT,FRAME_TYPE_LONG, FRAME_TYPE_LONG,"Frame Type (string)");
        addOption(RELAY_SCID_SHORT,RELAY_SCID_LONG, "scid", "Relay spacecraft ID");
        addOption(BEGIN_TIME_SHORT,BEGIN_TIME_LONG, "time", "Begin time of frame range (ERT/RCT) in the form YYYY-MM-DDThh:mm:sss.ttt or YYYY-DOYThh:mm:sss.ttt");
        addOption(END_TIME_SHORT,END_TIME_LONG, "time", "End time of frame range (ERT/RCT) in the form YYYY-MM-DDThh:mm:sss.ttt or YYYY-DOYThh:mm:sss.ttt");
        addOption(REPORT_SHORT,REPORT_LONG, null, "Report query results to console as text");
        addOption(FILE_SHORT,FILE_LONG, FILE_LONG, "Output file for frame data");
        addOption(BAD_ONLY_SHORT,BAD_ONLY_LONG,null,"Get bad Frames only");
        addOption(GOOD_ONLY_SHORT,GOOD_ONLY_LONG,null,"Get good Frames only");
        addOption(VCFCS_SHORT, VCFCS_LONG, "vcfc,...", "VCFCs and ranges");
        addOrderOption();

        addOption(TIME_TYPE_SHORT,TIME_TYPE_LONG, TIME_TYPE_ARG, "Time Type should be one of (ERT,RCT).  Default is " + getDefaultTimeType().getValueAsString());

        DssVcidOptions.addVcidOption(options);
        DssVcidOptions.addDssIdOption(options);

        addOption(null, RESTORE_LONG, null, "Restore headers and trailers to bodies");
        addOption(null, SHOW_ALL_META_LONG, null, "Show all header metadata");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void showHelp()
    {
        super.showHelp();

        System.out.println("\nMetadata for selected Frames will be written to standard output.");
        System.out.println("If -" + FILE_SHORT + " is specified, frame data will be written to an output file.");
        System.out.println("In this case, no metadata will be displayed unless the -" + REPORT_SHORT + " option is");
        System.out.println("supplied. The -" + OUTPUT_FORMAT_SHORT + " option can be used to select the metadata output style.");
        this.printTemplateStyles();
    }

    /**
     * Main entry point to run application.
     *
     * @param args Arguments from command-line.
     */    
    public static void main(final String[] args)
    {
        final FrameFetchApp app = new FrameFetchApp();
        app.runMain(args);
    }
}
