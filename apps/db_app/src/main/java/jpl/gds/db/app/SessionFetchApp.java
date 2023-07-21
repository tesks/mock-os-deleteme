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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import jpl.gds.cli.legacy.options.DssVcidOptions;
import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.order.ISessionOrderByType;
import jpl.gds.db.api.sql.order.OrderByType;
import jpl.gds.db.api.sql.store.ISessionStore;
import jpl.gds.db.mysql.impl.sql.order.SessionOrderByType;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.holders.HolderException;
import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;

/**
 * This is the command line applications to query test configurations out of the
 * test session table in the database.
 *
 */
public class SessionFetchApp extends AbstractFetchApp
{
    private static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_get_sessions");

    private static final int    NUM_QUERY_PARAMS     = 4;

    /** VCIDs to query for */
    private Set<Integer>        vcids                = null;

    /** DSS ids to query for */
    private Set<Integer>        dssIds               = null;

    /** Session fragment to filter for (1 by default) */
    private SessionFragmentHolder sessionFragment = SessionFragmentHolder.MINIMUM;

    /** Long sessionDssId option */
    private static final String  SESSION_DSS_ID_LONG  = "sessionDssId";
    /** Long sessionVcid option */
    private static final String  SESSION_VCID_LONG    = "sessionVcid";

    /** Long sessionDssId option */
    private static final String  SESSION_DSS_ID_SHORT = null;
    /** Long sessionVcid option */
    private static final String  SESSION_VCID_SHORT   = null;


    /**
     * Constructor.
     */
    public SessionFetchApp()
    {
        super(ISessionStore.DB_SESSION_DATA_TABLE_NAME,
              APP_NAME,
              "SessionQuery");

        suppressInfo();

        this.setCheckRequiredOptions(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public IDbSqlFetch getFetch(final boolean sqlStmtOnly)
	{
    	fetch =  appContext.getBean(IDbSqlFetchFactory.class).getSessionFetch(sqlStmtOnly);
    	return fetch;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public Object[] getFetchParameters()
	{
		final Object[] params = new Object[NUM_QUERY_PARAMS];
		
		ISessionOrderByType orderType = null;
		if(this.orderByString != null)
		{
			try
			{
                List<String> list = new ArrayList<>();
                for(String ord : SessionOrderByType.orderByTypes){
                    list.add(ord.toLowerCase());
                }
			    int ordinal = list.indexOf(this.orderByString.toLowerCase().trim());
                orderType = (ISessionOrderByType) orderByTypeFactory.getOrderByType(OrderByType.SESSION_ORDER_BY, ordinal);
			}
			catch(final IllegalArgumentException iae)
			{
				throw new IllegalArgumentException("The value \"" + this.orderByString + "\" is not a legal ordering value for this application.",iae);
			}
		}
		else
		{
			orderType = SessionOrderByType.DEFAULT;
		}
		
		params[0] = orderType;
        params[1] = sessionFragment;
        params[2] = vcids;
        params[3] = dssIds;
		
		return(params);
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void checkTimeType(final DatabaseTimeRange range) throws ParseException
	{
		//do nothing
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public DatabaseTimeType getDefaultTimeType()
	{
		return(null);
	}


    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getOrderByValues()
    {
        return(SessionOrderByType.orderByTypes);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getUsage()
    {
        return
        (
                APP_NAME + " " +
                "[--" + ReservedOptions.TESTKEY_LONG_VALUE + " <number> " +
                "--" + ReservedOptions.TESTNAME_LONG_VALUE + "Pattern <name>\n" + "                   " +
                "--" + ReservedOptions.TESTHOST_LONG_VALUE + "Pattern <host>" +
                "--" + ReservedOptions.TESTUSER_LONG_VALUE + "Pattern <username>\n" + "                   " +
                "--" + ReservedOptions.SSEVERSION_LONG_VALUE + "Pattern <version> " +
                "--" + ReservedOptions.FSWVERSION_LONG_VALUE + "Pattern version>\n" + "                   " +
                "--" + TEST_START_LOWER_LONG + " <time>\n" + "                   " +
                "--" + TEST_START_UPPER_LONG + " <time> " +
                "--" + ReservedOptions.TESTDESCRIPTION_LONG_VALUE + "Pattern <description>\n"+ "                   " +
                "--" + ReservedOptions.TESTTYPE_LONG_VALUE + "Pattern <type>]\n"
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addAppOptions()
    {
        super.addAppOptions();

        addOption(OUTPUT_FORMAT_SHORT,OUTPUT_FORMAT_LONG,"format", OUTPUT_FORMAT_DESC);
        addOption(SHOW_COLUMNS_SHORT, SHOW_COLUMNS_LONG, null, SHOW_COLUMNS_DESC);
        addOption(SESSION_DSS_ID_SHORT, SESSION_DSS_ID_LONG, "integer,...",
                  "deep space station id to filter for. Multiple values may be supplied in a comma-separatedvalue (CSV) format");
        addOption(SESSION_VCID_SHORT, SESSION_VCID_LONG, "integer,...",
                  "virtual channel ID to filter for.Multiple values may be supplied in a comma-separatedvalue (CSV) format");

        addOption(FRAGMENT_SHORT, FRAGMENT_LONG, "integer", FRAGMENT_DESC);
        addOption(null, SHOW_FRAGMENTS_LONG, null, SHOW_FRAGMENTS_DESC);

    	addOrderOption();
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public void showHelp()
    {
        super.showHelp();

        System.out.println("\nMetadata for selected Test Configurations will be written to standard");
        System.out.println("output.  Format of this output can be specified using the -" + OUTPUT_FORMAT_SHORT + " option.");
        printTemplateStyles();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configureApp(final CommandLine cmdline) throws ParseException {
        super.configureApp(cmdline);
        vcids = DssVcidOptions.parseSessionVcid(missionProps, cmdline, (String) null);
        dssIds = DssVcidOptions.parseSessionDssId(cmdline, (String) null);


        if(cmdline.hasOption(FRAGMENT_SHORT)) {
            try {
                sessionFragment = SessionFragmentHolder.valueOf((Integer.parseInt(cmdline.getOptionValue(FRAGMENT_SHORT))));
                dbSessionInfo.setSessionFragment(sessionFragment);
            }
            catch (final HolderException | NumberFormatException e) {
                throw new ParseException("Session Fragment '" + cmdline.getOptionValue(FRAGMENT_SHORT) + "' is not a valid session fragment");
            }
        }

        if( cmdline.hasOption(SHOW_FRAGMENTS_LONG)){
            if(cmdline.hasOption(FRAGMENT_SHORT)){
                throw new ParseException(String.format("Cannot specify both %s and %s parameters", FRAGMENT_LONG,
                                                       SHOW_FRAGMENTS_LONG));
            }

            //show all fragments
            sessionFragment = null;
        }
    }

    /**
     * Set VCIDs.
     *
     * @param vcids
     *            VCID collection
     */
    public void setVcid(final Collection<Integer> vcids) {
        if (vcids != null) {
            this.vcids = new TreeSet<>(vcids);
        }
        else {
            this.vcids = null;
        }
    }

    /**
     * Set DSS ids.
     *
     * @param dssIds
     *            DSS id collection
     */
    public void setDssId(final Collection<Integer> dssIds) {
        if (dssIds != null) {
            this.dssIds = new TreeSet<>(dssIds);
        }
        else {
            this.dssIds = null;
        }
    }


    /**
     * main entry to run application.
     *
     * @param args Command-line arguments
     */    
    public static void main(final String[] args)
    {
        final SessionFetchApp app = new SessionFetchApp();
        app.runMain(args);
    }

    @Override
    public CommandLine parseCommandLine(final String[] args) throws ParseException {
        final CommandLine commandLine = super.parseCommandLine(args);

        if (!(this instanceof SessionFetchApp) && (args.length == 0)) {
            throw new ParseException(
                    "Empty command line input. See help menu (-" + ReservedOptions.HELP_SHORT_VALUE + ") for usage.");
        }
        return commandLine;
    }

    @Override
    public void createRequiredOptions() throws ParseException {
        super.createRequiredOptions();
        this.requiredOptions.add(FRAGMENT_LONG);
        this.requiredOptions.add(SHOW_FRAGMENTS_LONG);
    }
}
