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

import java.util.*;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.types.*;
import jpl.gds.db.api.types.cfdp.IDbCfdpIndicationProvider;
import jpl.gds.db.api.types.cfdp.IDbCfdpPduSentProvider;
import jpl.gds.db.mysql.impl.sql.fetch.cfdp.CfdpIndicationFetch;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.ParseException;

import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.order.ICommandOrderByType;
import jpl.gds.db.mysql.impl.sql.order.CommandOrderByType;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.tc.api.CommandStatusType;
import org.apache.commons.lang.StringUtils;

/**
 * The command message fetch application is the command line application used
 * to query command message out of the database.
 *
 */
public class CommandFetchApp extends AbstractFetchApp
{
    private static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_get_commands");
    private static final int NUM_QUERY_PARAMS = 8;

    /**
     * Constant for 'final'
     */
    public static final String FINAL_LONG            = "final";

    /**
     * Constant for 'nonfinal'
     */
    public static final String NONFINAL_LONG         = "nonfinal";

    /**
     * Constant for 'lastStatusOnly'
     */
    public static final String LAST_STATUS_ONLY_LONG = "lastStatusOnly";

    /**
     * Constant for 'uplinkTypes'
     */
    public static final String UPLINK_TYPES = "uplinkTypes";

    /** For usage */
    public static final String RETRIEVE = "Retrieve selected types: " + "c=command " + "t=CFDP transaction " + " p=CFDP PDU";

    /**
     * The command string to query for (format varies depending on command message type)
     */
    protected String commandString;

    /**
     * The type of command message to query for (e.g. FileLoad)
     */
    protected CommandType       commandType;

    /**
     * The type of command status to query for (e.g. Radiated)
     */
    private CommandStatusType statusType = null;

    /** If true, final only; if false nonfinal only */
    private Boolean finl = null;

    /** Specific request-id to qury for */
    private String requestId = null;

    /** If true, do last status only */
    private boolean lastStatusOnly = false;

    /** If true, get the standard commands for the query */
    private boolean includeCommands = false;

    /** If true, get the CFDP PDUs for the query */
    private boolean includeCfdpPdus = false;

    /** if true, include CFDP uplink transactions in the query */
    private boolean includeUplinkTransactions = false;

    /** fetch for getting the CFDP Indications */
    private IDbSqlFetch transactionFetch = null;

    /** fetch for getting the CFDP PDUs sent */
    private IDbSqlFetch pduFetch = null;

    /** The full list of retrieved records from all 3 fetches*/
    private List<IDbAccessItem> fullSet = new ArrayList<>();
    private int fullSetOffset = 0;


    /**
     * Creates an instance of CommandMessageFetchApp.
     */
    public CommandFetchApp()
    {
        super(IDbTableNames.DB_COMMAND_MESSAGE_DATA_TABLE_NAME,
              APP_NAME,
              "CommandQuery");

        suppressInfo();
        
        this.commandString = null;
        this.commandType = null;
    }

    /**
     * Get command string.
     *
     * @return Returns the commandString.
     */
    public String getCommandString()
    {
        return this.commandString;
    }

    /**
     * Get command type.
     *
     * @return Returns the commandType.
     */
    public CommandType getCommandType()
    {
        return this.commandType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createRequiredOptions() throws ParseException
    {    	
    	super.createRequiredOptions();
        this.requiredOptions.add(BEGIN_TIME_LONG);
        this.requiredOptions.add(CMD_STRING_LONG);
        this.requiredOptions.add(END_TIME_LONG);
        this.requiredOptions.add(CMD_TYPE_LONG);
        this.requiredOptions.add(STATUS_TYPE_LONG);
        this.requiredOptions.add(CMD_REQUEST_ID_LONG);
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public void configureApp(final CommandLine cmdline) throws ParseException
    {
        super.configureApp(cmdline);

        //parse in and store the command string value (if there is one)
        if(cmdline.hasOption(CMD_STRING_SHORT))
        {
            final String cmdString = cmdline.getOptionValue(CMD_STRING_SHORT);
            if(cmdString == null)
            {
                throw new MissingArgumentException("The option -" + CMD_STRING_SHORT + " requires a value");
            }
            this.commandString = cmdString.trim();
        }

        //parse in and store the command type value (if there is one)
        if(cmdline.hasOption(CMD_TYPE_SHORT))
        {
            final String cmdType = cmdline.getOptionValue(CMD_TYPE_SHORT);
            if(cmdType == null)
            {
                throw new MissingArgumentException("The option -" + CMD_TYPE_SHORT + " requires a value");
            }

            try
            {
                this.commandType = new CommandType(cmdType.trim());
            }
            catch(final IllegalArgumentException iae)
            {
                throw new ParseException("The input command message type \"" + cmdType + "\" is not a valid type of command message");
            }
        }

        // Parse in and store the command status value (if there is one)

        if (cmdline.hasOption(STATUS_TYPE_LONG))
        {
            final String status = cmdline.getOptionValue(STATUS_TYPE_LONG);

            if (status == null)
            {
                throw new MissingArgumentException("The option --" + STATUS_TYPE_LONG + " requires a value");
            }

            try
            {
                statusType = CommandStatusType.valueOf(status.trim());
            }
            catch (final IllegalArgumentException iae)
            {
                throw new ParseException("The input command status type '" + status + "' is not a valid type of command status");
            }
        }

        // Parse in and store the request-id value (if there is one)

        if (cmdline.hasOption(CMD_REQUEST_ID_LONG))
        {
            requestId = StringUtil.safeTrim(
                            cmdline.getOptionValue(CMD_REQUEST_ID_LONG));

            if (requestId.isEmpty())
            {
                throw new MissingArgumentException(
                    "The option --" + CMD_REQUEST_ID_LONG + " requires a value");
            }
        }

        lastStatusOnly = cmdline.hasOption(LAST_STATUS_ONLY_LONG);

        final boolean fl  = cmdline.hasOption(FINAL_LONG);
        final boolean ufl = cmdline.hasOption(NONFINAL_LONG);

        if (fl && ufl)
        {
            throw new ParseException("Cannot set both --" +
                                     FINAL_LONG           +
                                     " and --"            +
                                     NONFINAL_LONG);
        }
        else if (fl)
        {
            finl = true;
        }
        else if (ufl)
        {
            finl = false;
        }
        else
        {
            finl = null;
        }


        getUplinkTypes(cmdline);
    }

    private void getUplinkTypes(final CommandLine cmdLine) throws ParseException {
        String uplinkTypes = StringUtil.safeCompressAndUppercase(cmdLine.hasOption(UPLINK_TYPES) ? cmdLine.getOptionValue(UPLINK_TYPES)
                : dbProperties.getUplinkTypesDefault());

        if(uplinkTypes.isEmpty()) {
            throw new MissingArgumentException("You must provide a value for --" + UPLINK_TYPES);
        }

        includeCommands = uplinkTypes.contains("C");
        includeUplinkTransactions = uplinkTypes.contains("T");
        includeCfdpPdus = uplinkTypes.contains("P");

        uplinkTypes = uplinkTypes.replaceAll("[CTP]", "");

        if (!uplinkTypes.isEmpty()) {
            throw new ParseException("Unknown flags '" + uplinkTypes + "' in --" + UPLINK_TYPES);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean setupFetch() {
        boolean connected = true;

        if (includeCommands) {
            fetch = getFetch(sqlStmtOnly);
            connected = connected && fetch.isConnected();
        }

        if (includeUplinkTransactions) {
            transactionFetch = getIndicationFetch(sqlStmtOnly);

            connected = connected && transactionFetch.isConnected();
        }

        if (includeCfdpPdus) {
            pduFetch = getPduSentFetch(sqlStmtOnly);

            connected = connected && pduFetch.isConnected();
        }

        return connected;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void teardownFetch() {
        if(fetch != null) { fetch.close(); }
        if(transactionFetch != null) { transactionFetch.close(); }
        if(pduFetch != null) { pduFetch.close(); }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void getInitialFetch() throws DatabaseException {

        /*
         * really, under the hood, each fetch really has all of the results
         * They just restrict how many we can get out at once. This allows
         * us to emulate that and have all of the disparate result types interleaved
         */

        List<IDbAccessItem> rawData = new ArrayList<>();

        if(includeCommands) {
            rawData.addAll(getAllResults(fetch));

        }
        if(includeUplinkTransactions) {
           rawData.addAll(getAllResults(transactionFetch));
        }
        if(includeCfdpPdus) {
            rawData.addAll(getAllResults(pduFetch));
        }

        if(includeUplinkTransactions || includeCfdpPdus) {
            fullSet = sort(rawData);
        } else {
            fullSet = rawData;
        }

        //since this is the first time, it'll load the first batch of results into "out"
        getNextFetch();
    }


    //use the supplied fetch, get all of the results, and concatenate them
    private List<IDbAccessItem> getAllResults(IDbSqlFetch dbFetch) throws DatabaseException {
        List<? extends IDbRecord> tmp;
        List<IDbAccessItem> castList = new ArrayList<>();
        tmp = dbFetch.get( useContext ? dbContextInfo: dbSessionInfo,
                times, defaultBatchSize, useContext,
                getFetchParameters());

        while(!tmp.isEmpty()) {

            for(IDbRecord tmpRec : tmp) {
                if(tmpRec instanceof IDbAccessItem) {
                    castList.add((IDbAccessItem) tmpRec);
                } else {
                    throw new DatabaseException("Received record was not of the class IDbAccessItem");
                }
            }

            tmp = dbFetch.getNextResultBatch();
        }

        return castList;
    }

    /*
     * while each type of entry may be sorted on its own, the list consists of each type concatenated onto
     * the list, meaning they're not sorted together. This would be easy if commands, CFDP indications, and
     * CFDP PDUs sent all had the same colummns, but they don't. So we need to give them some common ground
     * for sorting.
     */
    private List<IDbAccessItem> sort(List<IDbAccessItem> unsorted) throws DatabaseException {
        List<IDbAccessItem> sorted = new ArrayList<>();

        SortedMap<String,List<IDbAccessItem>> sortMap = new TreeMap<>();

        CommandOrderByType orderBy = (CommandOrderByType) getFetchParameters()[2];

        // none sorting doesn't use any column for sorting
        if(orderBy.getValueAsInt() == ICommandOrderByType.NONE_TYPE) {
            return unsorted;
        }

        String[] columns = orderBy.getOrderByColumns();

        for(IDbAccessItem unsortedEntry : unsorted) {
            String key = generateKey(unsortedEntry, columns);

            sortMap.putIfAbsent(key, new ArrayList<>());

            sortMap.get(key).add(unsortedEntry);
        }

        //now that the values are sorted into buckets, let's flatten it back out into a List
        sortMap.values().forEach(sorted::addAll);


        return sorted;
    }

    // helper function so the entries get safely casted
    private String generateKey(IDbAccessItem entry, String[] columns) throws DatabaseException {

        if(IDbCommandProvider.class.isInstance(entry)) {
            return makeCommandKey((IDbCommandProvider) entry, columns);
        } else if(IDbCfdpIndicationProvider.class.isInstance(entry)) {
            return makeIndicationKey((IDbCfdpIndicationProvider) entry, columns);
        } else if(IDbCfdpPduSentProvider.class.isInstance(entry)) {
            return makePduSentKey((IDbCfdpPduSentProvider) entry, columns);
        }

        throw new DatabaseException("Unknown database entry type encountered: " + entry.getClass().toString());
    }

    // used to make a sorting key for command entries
    private String makeCommandKey(IDbCommandProvider entry, String[] columns) {
        StringBuilder sb = new StringBuilder();
        for(String column : columns) {
            switch (column) {
                case "sessionId":
                    sb.append(entry.getSessionId());
                    break;
                case "hostId":
                    sb.append(entry.getSessionHostId());
                    break;
                case "requestId":
                    sb.append(entry.getRequestId());
                    break;
                case "eventTimeCoarse":
                    sb.append(entry.getEventTime().getTime());
                    break;
                case "eventTimeFine":
                    sb.append(StringUtils.leftPad(String.valueOf(entry.getEventTime().getNanoseconds()),6,"0"));
                    break;
                case "type":
                    sb.append(entry.getType());
                    break;
                case "rctCoarse":
                    sb.append(entry.getRct().getTime());
                    break;
                case "rctFine":
                    sb.append(StringUtils.leftPad(String.valueOf(entry.getRct().getNanoseconds()),6,"0"));
                    break;
            }
            sb.append("/");
        }

        return sb.toString();
    }

    // used to make a sorting key for CFDP indications
    private String makeIndicationKey(IDbCfdpIndicationProvider entry, String[] columns) {
        StringBuilder sb = new StringBuilder();
        for(String column : columns) {
            switch (column) {
                case "sessionId":
                    sb.append(entry.getSessionId());
                    break;
                case "hostId":
                    sb.append(entry.getSessionHostId());
                    break;
                case "requestId":
                    sb.append(entry.getTransactionSequenceNumber());
                    break;
                case "eventTimeCoarse":
                case "rctCoarse":
                    sb.append(entry.getIndicationTime().getTime());
                    break;
                case "eventTimeFine":
                case "rctFine":
                    sb.append(StringUtils.leftPad(String.valueOf(entry.getIndicationTime().getNanoseconds()),6,"0"));
                    break;
                case "type":
                    sb.append(entry.getType());
                    break;
            }
            sb.append("/");
        }

        return sb.toString();
    }

    // used to make a sorting key for CFDP PDU sent
    private String makePduSentKey(IDbCfdpPduSentProvider entry, String[] columns) {
        StringBuilder sb = new StringBuilder();
        for(String column : columns) {
            switch (column) {
                case "sessionId":
                    sb.append(entry.getSessionId());
                    break;
                case "hostId":
                    sb.append(entry.getSessionHostId());
                    break;
                case "requestId":
                    sb.append(entry.getPduId());
                    break;
                case "eventTimeCoarse":
                case "rctCoarse":
                    sb.append(entry.getPduTime().getTime());
                    break;
                case "eventTimeFine":
                case "rctFine":
                    sb.append(StringUtils.leftPad(String.valueOf(entry.getPduTime().getNanoseconds()),6,"0"));
                    break;
                case "type":
                    sb.append(entry.getMetadata("pduType"));
                    break;
            }
            sb.append("/");
        }

        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    protected void getNextFetch() {

        if(fullSetOffset == fullSet.size()) {
            out = new ArrayList<>();
        }

        int endOffset = (fullSetOffset + defaultBatchSize) > fullSet.size() ? fullSet.size() : (fullSetOffset + defaultBatchSize);

        out = fullSet.subList(fullSetOffset, endOffset);

        fullSetOffset = endOffset;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbSqlFetch getFetch(final boolean sqlStmtOnly)
    {
    	fetch = appContext.getBean(IDbSqlFetchFactory.class).getCommandFetch(sqlStmtOnly);
    	return fetch;
    }

    /**
     * Get the CfdpPduSent SQL statement
     * @param sqlStmtOnly TRUE if the SQL statement is to be fetched only (not executed), false otherwise
     * @see jpl.gds.db.app.cfdp.CfdpPduSentFetchApp#getFetch(boolean)
     */
    public IDbSqlFetch getPduSentFetch(final boolean sqlStmtOnly) {
        return appContext.getBean(IDbSqlFetchFactory.class).getCfdpPduSentFetch(sqlStmtOnly);
    }

    /**
     * Get the CfdpIndication SQL statement
     * @param sqlStmtOnly TRUE if the SQL statement is to be fetched only (not executed), false otherwise
     * @see jpl.gds.db.app.cfdp.CfdpIndicationFetchApp#getFetch(boolean)
     */
    public IDbSqlFetch getIndicationFetch(final boolean sqlStmtOnly) {
        return appContext.getBean(IDbSqlFetchFactory.class).getCfdpIndicationFetch(sqlStmtOnly);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getFetchParameters()
    {

    	final Object[] params = new Object[NUM_QUERY_PARAMS];
    	
    	ICommandOrderByType orderType = CommandOrderByType.DEFAULT;

        if (this.orderByString != null)
    	{
    		try
			{
				orderType = new CommandOrderByType(this.orderByString.trim());
			}
			catch(final IllegalArgumentException iae)
			{
                throw new IllegalArgumentException("The value '"      +
                                                   this.orderByString +
                                                   "' is not a legal ordering value for this application", iae);
			}
    	}
        else if (this.times != null)
        {
            switch (times.getTimeType().getValueAsInt())
            {
                case DatabaseTimeType.EVENT_TIME_TYPE:
                    orderType = CommandOrderByType.EVENT_TIME;
                    break;
                case DatabaseTimeType.RCT_TYPE:
                    orderType = CommandOrderByType.RCT;
                    break;
                default:
                    break;
            }
		}

        final List<String> requestIds = new ArrayList<String>(1);

        if (requestId != null)
        {
            requestIds.add(requestId);
        }

    	params[0] = this.commandString;
    	params[1] = this.commandType;
    	params[2] = orderType;
        params[3] = finl;
        params[4] = statusType;
        params[5] = requestIds;
        params[6] = lastStatusOnly;
        /*
         * param[7] allows us to only retrieve CFDP indications for
         * outbound (uplink) transactions only
         */
        params[7] = CfdpIndicationFetch.Direction.OUT;
    	
    	return(params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkTimeType(final DatabaseTimeRange range) throws ParseException
    {

        if ((range.getTimeType().getValueAsInt() != DatabaseTimeType.EVENT_TIME_TYPE) &&
            (range.getTimeType().getValueAsInt() != DatabaseTimeType.RCT_TYPE))
    	{
    		throw new ParseException("Time type must be "                   +
                                     DatabaseTimeType.EVENT_TIME.toString() +
                                     " or "                                 +
                                     DatabaseTimeType.RCT.toString()        +
                                     " for this application.");
    	}
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public DatabaseTimeType getDefaultTimeType()
    {
    	return(DatabaseTimeType.EVENT_TIME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getOrderByValues()
    {
        return(CommandOrderByType.orderByTypes);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getUsage()
    {
        return APP_NAME +" [--" + 
               CMD_STRING_LONG + " <string> --" + 
               CMD_TYPE_LONG + " <commandType> --" + 
               STATUS_TYPE_LONG + " <statusType> --" + 
               CMD_REQUEST_ID_LONG + " <string> \n" + 
               "                   --" + 
               BEGIN_TIME_LONG + " <time> --" +
               END_TIME_LONG + " <time>]\n" +
               "                   [Session search options - Not required]\n";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addAppOptions()
    {
        super.addAppOptions();

        addOption(null, LAST_STATUS_ONLY_LONG, null, "Last status only");

        addOption(null, FINAL_LONG,    null, "Final states only");
        addOption(null, NONFINAL_LONG, null, "Non-final states only");
        
        addOption(OUTPUT_FORMAT_SHORT,OUTPUT_FORMAT_LONG,"format", OUTPUT_FORMAT_DESC);
    	addOption(SHOW_COLUMNS_SHORT,SHOW_COLUMNS_LONG, null, SHOW_COLUMNS_DESC);
    	addOption(CMD_STRING_SHORT,CMD_STRING_LONG, "string", 
        		"Command Message String Pattern (used with an SQL LIKE clause)");

        addOption(TIME_TYPE_SHORT, TIME_TYPE_LONG, TIME_TYPE_ARG,
                  "Time type should be one of (EVENTTIME,RCT). Default is " +
                      getDefaultTimeType().getValueAsString());

        if (CommandType.messageTypes.length > 0)
        {
            final StringBuilder typeString = new StringBuilder(CommandType.messageTypes[0]);
            for(int i=1; i < CommandType.messageTypes.length; i++)
            {
                typeString.append(", ").append(CommandType.messageTypes[i]);
            }
            addOption(CMD_TYPE_SHORT,CMD_TYPE_LONG, "string", "The type of command message.  Valid values are: " + typeString);
        }

        if (CommandStatusType.values().length > 0)
        {
            final StringBuilder sb    = new StringBuilder();
            boolean             first = true;

            for (final CommandStatusType cst : CommandStatusType.values())
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    sb.append(',');
                }

                sb.append(cst);
            }

            addOption(null, STATUS_TYPE_LONG, "string", "The type of command status. Valid values are: " + sb);
        }

        addOption(null, CMD_REQUEST_ID_LONG, "string", "A request-id.");
        addOption(BEGIN_TIME_SHORT,BEGIN_TIME_LONG, "time", "Begin time of command event time range");
        addOption(END_TIME_SHORT,END_TIME_LONG, "time", "End time of command event time range");
        addOrderOption();

        addOption(null, UPLINK_TYPES, "string", RETRIEVE);
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
     * The main method to start the application
     *
     * @param args Arguments from command-line.
     */
    public static void main(final String[] args)
    {
        final CommandFetchApp app = new CommandFetchApp();
        app.runMain(args);
    }

}
