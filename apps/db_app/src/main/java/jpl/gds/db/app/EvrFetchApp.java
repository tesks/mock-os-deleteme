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

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;

import jpl.gds.cli.legacy.options.DssVcidOptions;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.order.IEvrOrderByType;
import jpl.gds.db.api.sql.store.ldi.IEvrLDIStore;
import jpl.gds.db.mysql.impl.sql.order.EvrOrderByType;
import jpl.gds.evr.api.config.EvrProperties;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;

/**
 * The EvrFetchApp is the command line application used to query EVRs
 * from the database.
 *
 */
public class EvrFetchApp extends AbstractFetchApp
{
    private static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_get_evrs");
    private static final int NUM_QUERY_PARAMS = 11;

    /** Long option for EVR types. Used by GetEverythingApp. */
    public static final String EVR_TYPES_LONG = "evrTypes";
    
    private String level;
    private String name;
    private Long eventId;
    private String module;

    /** EVR types desired */
    private EvrTypeSelect evrTypeSelect = new EvrTypeSelect();

    /** VCIDs to query for */
    private Set<Integer> vcids = null;

    /** DSS ids to query for */
    private Set<Integer> dssIds = null;

    /**
     * Constructor.
     *
     */
    public EvrFetchApp()
    {
        super(IEvrLDIStore.DB_EVR_DATA_TABLE_NAME,
              APP_NAME,
              "EvrQuery");

        suppressInfo();
        
        this.level = null;
        this.name = null;
        this.eventId = null;
        this.module = null;
    }
    
    /**
     * Get event id.
     *
     * @return Event id
     */
    public Long getEventId()
    {
        return this.eventId;
    }

    /**
     * Get name.
     *
     * @return Name
     */
    public String getName()
    {
    	return this.name;
    }


    /**
     * Get level.
     *
     * @return Level
     */
    public String getLevel()
    {
        return this.level;
    }
    
    private void printEvrLevels()
    {
        try
        {
        	final Set<String> evrLevelProps = appContext.getBean(EvrProperties.class).getCombinedEvrLevels();

			System.out.println("\nValid values for the --level option are:");
			
			final StringBuffer sb = new StringBuffer();
			int i = 1;
		    for (final String level: evrLevelProps) {		        
		        sb.append(level);
		        if (i++ != evrLevelProps.size()) {
	                sb.append(',');
	            }
		    }
            System.out.println(sb);
        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }
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
        this.requiredOptions.add(EVENT_ID_LONG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configureApp(final CommandLine cmdline) throws ParseException
    {
        super.configureApp(cmdline);

        if (cmdline.hasOption(MODULE_SHORT))
        {
            final String moduleString = cmdline.getOptionValue(MODULE_SHORT);
            if(moduleString == null)
            {
                throw new MissingArgumentException("-" + MODULE_SHORT + " requires a command line value");
            }
            this.module = moduleString.trim();
        }

        if(cmdline.hasOption(EVENT_ID_SHORT))
        {
            final String eventIdStr = cmdline.getOptionValue(EVENT_ID_SHORT);
            if(eventIdStr == null)
            {
                throw new MissingArgumentException("The option -" + EVENT_ID_SHORT + " requires a value.");
            }

            try
            {
                this.eventId = Long.parseLong(eventIdStr.trim());
            }
            catch(final NumberFormatException e1)
            {
                throw new MissingOptionException("Event ID must be a valid integer");
            }
        }

        if(cmdline.hasOption(EVR_NAME_SHORT))
        {
            final String nameString = cmdline.getOptionValue(EVR_NAME_SHORT);
            if(nameString == null)
            {
                throw new MissingArgumentException("The option -" + EVR_NAME_SHORT + " requires a value");
            }
            this.name = nameString.trim();
        }

        if(cmdline.hasOption(LEVEL_SHORT))
        {
            final String levelString = cmdline.getOptionValue(LEVEL_SHORT);
            if(levelString == null)
            {
                throw new MissingArgumentException("The option -" + LEVEL_SHORT + " requires a value");
            }
            this.level = levelString.trim();
        }

        // Process EVR type selection arguments
        getEvrTypes(dbProperties, cmdline, evrTypeSelect);

        vcids  = DssVcidOptions.parseVcid(missionProps, cmdline,
                                          evrTypeSelect.sse ? 'S' : null,
                                          null,
                                          EVR_TYPES_LONG);

        dssIds = DssVcidOptions.parseDssId(cmdline,
                                           evrTypeSelect.sse ? 'S' : null,
                                           EVR_TYPES_LONG);
    }


    /**
     * Set type select. Called by GetEverythingApp.
     *
     * @param types Selected types to query
     */
    public void setEvrTypes(final EvrTypeSelect types)
    {
        if (types != null)
        {
            evrTypeSelect = types;
        }
    }


    /**
     * Set VCIDs.
     *
     * @param vcids VCID collection
     */
    public void setVcid(final Collection<Integer> vcids)
    {
        if (vcids != null)
        {
            this.vcids = new TreeSet<Integer>(vcids);
        }
        else
        {
            this.vcids = null;
        }
    }


    /**
     * Set DSS ids.
     *
     * @param dssIds DSS id collection
     */
    public void setDssId(final Collection<Integer> dssIds)
    {
        if (dssIds != null)
        {
            this.dssIds = new TreeSet<Integer>(dssIds);
        }
        else
        {
            this.dssIds = null;
        }
    }




    /**
     * {@inheritDoc}
     */
    @Override
    public IDbSqlFetch getFetch(final boolean sqlStmtOnly)
    {
        fetch = appContext.getBean(IDbSqlFetchFactory.class).getEvrFetch(sqlStmtOnly);
    	return fetch;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getFetchParameters()
    {
    	final Object[] params = new Object[NUM_QUERY_PARAMS];

        Arrays.fill(params, null);
    	
    	IEvrOrderByType orderType = null;
		if(this.orderByString != null)
		{
			try
			{
				orderType = new EvrOrderByType(this.orderByString.trim());
			}
			catch(final IllegalArgumentException iae)
			{
				throw new IllegalArgumentException("The value \"" + this.orderByString + "\" is not a legal ordering value for this application.",iae);
			}
		}
		else if (this.times != null) {
		   switch(this.times.getTimeType().getValueAsInt()) {
		   case DatabaseTimeType.ERT_TYPE:
			   orderType = EvrOrderByType.ERT;
			   break;
		   case DatabaseTimeType.SCET_TYPE:
			   orderType = EvrOrderByType.SCET;
			   break;
		   case DatabaseTimeType.LST_TYPE:
			   orderType = EvrOrderByType.LST;
			   break;
		   case DatabaseTimeType.SCLK_TYPE:
			   orderType = EvrOrderByType.SCLK;
				   break;
		   case DatabaseTimeType.RCT_TYPE:
			   orderType = EvrOrderByType.RCT;
				   break;
           default:
               break;
		   }
		}
		else {
			orderType = EvrOrderByType.DEFAULT;
		}
		
    	params[0]  = this.name;
    	params[1]  = this.eventId;
    	params[2]  = this.level;
    	params[3]  = this.module;

        // Set fromSSE status
        if (evrTypeSelect.sse)
        {
            if (evrTypeSelect.fswRealtime || evrTypeSelect.fswRecorded)
            {
                params[5] = null; // Do union
            }
            else
            {
                params[5] = Boolean.TRUE;
            }
        }
        else
        {
            params[5] = Boolean.FALSE;
        }

        // Set isRealtime status (FSW only now, does not affect SSE)

        if (evrTypeSelect.fswRealtime == evrTypeSelect.fswRecorded)
        {
            params[6] = null; // Not needed
        }
        else
        {
            params[6] = evrTypeSelect.fswRealtime;
        }

    	params[7]  = orderType;
        params[8]  = Boolean.FALSE; // Second half if true
        params[9]  = vcids;
        params[10] = dssIds;

        if (((vcids  != null) && ! vcids.isEmpty()) ||
            ((dssIds != null) && ! dssIds.isEmpty()))
        {
            // No DSS or VCID in SseEvr
            params[5] = Boolean.FALSE;
        }

    	return(params);
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
            case DatabaseTimeType.SCLK_TYPE:
            case DatabaseTimeType.RCT_TYPE:
            case DatabaseTimeType.ERT_TYPE:
            case DatabaseTimeType.LST_TYPE:

                break;

            default:

                throw new ParseException("TimeType is not one of: SCET, ERT, RCT, SCLK, LST");
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
        return(EvrOrderByType.orderByTypes);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getUsage()
    {
        return APP_NAME + " " +
        "[--" + TIME_TYPE_LONG + " <" + TIME_TYPE_ARG +"> " +
        "--" + BEGIN_TIME_LONG + " <time>\n" + "               " +
        "--" + END_TIME_LONG + " <time> " +
        "--" + EVENT_ID_LONG + " <int> " +
        "--" + EVR_NAME_LONG + " <name> " +
        "--" + LEVEL_LONG + " <level>\n" + "               " +
        "--" + MODULE_LONG + " <module-name>]\n" +
        "               [Session search options - Not required]\n";
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
        addOption(EVENT_ID_SHORT,EVENT_ID_LONG, "int", "EVR Event ID");
        addOption(EVR_NAME_SHORT,EVR_NAME_LONG, "string", "EVR name or pattern to match multiple names.");
        addOption(LEVEL_SHORT,LEVEL_LONG, "string", "EVR Level string");
        addOption(TIME_TYPE_SHORT,TIME_TYPE_LONG, TIME_TYPE_ARG, "Time Type should be one of: (SCLK,ERT,SCET,RCT,LST).  The default time type is " + getDefaultTimeType().getValueAsString());
        addOption(BEGIN_TIME_SHORT,BEGIN_TIME_LONG, "Time", "Begin time of range");
        addOption(END_TIME_SHORT,END_TIME_LONG, "Time", "End time of range");

        addOption(MODULE_SHORT, MODULE_LONG, "name", "FSW/SSE module name");
        addOrderOption();

		addOption(null,
                  EVR_TYPES_LONG,
                  "string",
                  "Retrieve selected types: " +
                      "s=SSE "                +
                      "f=FSW-realtime "       +
                      "r=FSW-recorded");

        DssVcidOptions.addVcidOption(options);
        DssVcidOptions.addDssIdOption(options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showHelp()
    {
        super.showHelp();
        System.out.println("\nMetadata for selected Evrs will be written to standard output.");
        System.out.println("Format of this output can be specified using the -" + OUTPUT_FORMAT_SHORT + " option.");
        printTemplateStyles();
        printEvrLevels();
    }


    /**
     * Process arguments for selecting the EVR types. Used by GetEverythingApp.
     * @param dbProperties the current database configuration
     *
     * @param cl  Command line
     * @param ets Object to populate with selections
     *
     * @throws ParseException           Thrown on parameter error
     * @throws MissingArgumentException Thrown on missing value
     */
    public static void getEvrTypes(final IMySqlAdaptationProperties dbProperties,
    		                       final CommandLine   cl,
                                   final EvrTypeSelect ets)
        throws ParseException,
               MissingArgumentException
    {
        // --evrTypes (or default) MUST contain at least one
        // valid flag and CANNOT contain unknown flags

        String evrTypes =
            StringUtil.safeCompressAndUppercase(
                cl.hasOption(EVR_TYPES_LONG)
                    ? cl.getOptionValue(EVR_TYPES_LONG)
                    : dbProperties.getEvrTypesDefault());

        if (evrTypes.isEmpty())
        {
            throw new MissingArgumentException(
                          "You must provide a value for --" +
                          EVR_TYPES_LONG);
        }

        if (evrTypes.isEmpty())
        {
            throw new MissingArgumentException(
                          "You must provide a value for --" +
                          EVR_TYPES_LONG);
        }

        ets.sse         = evrTypes.contains("S");
        ets.fswRealtime = evrTypes.contains("F");
        ets.fswRecorded = evrTypes.contains("R");

        evrTypes = evrTypes.replaceAll("[SFR]", "");

        if (! evrTypes.isEmpty())
        {
            throw new ParseException("Unknown flags '" +
                                     evrTypes          +
                                     "' in --"         +
                                     EVR_TYPES_LONG);
        }
    }


    /**
     * Main entry point to run application.
     *
     * @param args Arguments from command-line.
     */    
    public static void main(final String[] args)
    {
        final EvrFetchApp app = new EvrFetchApp();
        app.runMain(args);
    }


    /** Holder class for the three EVR types. Used by GetEverythingApp. */
    public static class EvrTypeSelect extends Object
    {
        /** Set true for FSW realtime evrs */
        public boolean fswRealtime = false;

        /** Set true for FSW recorded evrs */
        public boolean fswRecorded = false;

        /** Set true for SSE evrs */
        public boolean sse         = false;


        /**
         * Constructor.
         */
        public EvrTypeSelect()
        {
            super();
        }
    }
}
