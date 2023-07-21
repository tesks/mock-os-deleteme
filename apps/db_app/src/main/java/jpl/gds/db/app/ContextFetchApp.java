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

import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.impl.types.DatabaseContextConfig;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.metadata.MetadataKey;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;

import java.io.IOException;
import java.util.ListIterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

/**
 *
 * This is the command line application to query context configurations from the database.
 *
 */
public class ContextFetchApp extends AbstractFetchApp{

    private static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_get_contexts");

    /**
     * Constructor
     */
    public ContextFetchApp() {
        super(IDbTableNames.DB_CONTEXT_CONFIG_TABLE_NAME, APP_NAME, "ContextConfigQuery");
        suppressInfo();
        this.setCheckRequiredOptions(false);

        useContext = true;
    }

    @Override
    public void showHelp(){
        super.showHelp();

        System.out.println("\nMetadata for selected Context Configurations will be written to standard");
        System.out.println("output.  Format of this output can be specified using the -" + OUTPUT_FORMAT_SHORT + " option.");
        printTemplateStyles();
    }

    @Override
    public void configureApp(final CommandLine cmdline) throws ParseException {
        super.configureApp(cmdline);
    }

    @Override
    public IDbSqlFetch getFetch(final boolean sqlStmtOnly) {
        fetch =  appContext.getBean(IDbSqlFetchFactory.class).getContextConfigFetch(sqlStmtOnly);
        return fetch;
    }

    @Override
    public Object[] getFetchParameters() {
        return new Object[0];
    }

    @Override
    public void checkTimeType(final DatabaseTimeRange range) throws ParseException {
        //do nothing
    }

    @Override
    public DatabaseTimeType getDefaultTimeType() {
        return null;
    }

    @Override
    public String[] getOrderByValues() {
        return new String[0];
    }

    @Override
    public String getUsage() {
        return APP_NAME + " " + "[--" + CONTEXT_ID_LONG + " <number>] \n";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addAppOptions() {
        createCommonOptions(options);
        addOption(OUTPUT_FORMAT_SHORT,OUTPUT_FORMAT_LONG,"format", OUTPUT_FORMAT_DESC);
        addOption(SHOW_COLUMNS_SHORT, SHOW_COLUMNS_LONG, null, SHOW_COLUMNS_DESC);
        addOption(CONTEXT_ID_SHORT,CONTEXT_ID_LONG, "contextId", "The unique numeric identifier for a context.");
        final Option sqlStmtOpt = ReservedOptions.createOption(SQL_STATEMENT_ONLY_LONG, null,
                                                               "Instead of executing the database query, print the SQL statement (useful for debugging)");
        options.addOption(sqlStmtOpt);
    }

    /**
     * main entry to run application.
     *
     * @param args Command-line arguments
     */
    public static void main(final String[] args)
    {
        final ContextFetchApp app = new ContextFetchApp();
        app.runMain(args);
    }

    @Override
    protected void writeBody() throws IOException, DatabaseException {

        long byteOffset = 0;

        while (!out.isEmpty() && !shutdown) {
            final ListIterator<DatabaseContextConfig> iter = (ListIterator<DatabaseContextConfig>) out.listIterator();
            while (iter.hasNext() && !shutdown) {
                final DatabaseContextConfig dq = iter.next();

                //skip record if it does not match session ID filter
                if(cmdline.hasOption(ReservedOptions.TESTKEY_SHORT_VALUE) && !isSessionIdInMetadata(dq)){
                    continue;
                }

                //write header since we will output data
                super.writeHeader();

                dq.setRecordOffset(byteOffset);
                if (pw != null) {
                    writeMetaData(pw, dq);
                }

                if (dos != null) {
                    final byte[] bytes = dq.getRecordBytes();

                    byteOffset += writeData(dos, bytes, bytes.length);
                }

                recordCount++;
            }
            checkPrintWriter(pw);

            if (!shutdown) {
                getNextFetch();
            }
        }
    }

    @Override
    protected void writeHeader() throws IOException {
        //do nothing since we don't know if there is data to output
    }

    // check if the specified session IDs match the list of IDs we store in DB
    private boolean isSessionIdInMetadata(DatabaseContextConfig info){
        for(Long sessionId: dbSessionInfo.getSessionKeyList()){
            if(info.getValue(MetadataKey.SESSION_IDS.toString()).contains(String.valueOf(sessionId))){
                return true;
            }
        }

        return false;
    }
}