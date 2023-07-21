package jpl.gds.db.app.cfdp;

import java.util.ArrayList;

import org.apache.commons.cli.ParseException;

import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;

public class CfdpRequestReceivedFetchApp extends ACfdpSubFetchApp {

	private static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_get_cfdp reqrecv");

	private static final int NUM_QUERY_PARAMS = 1;

	public CfdpRequestReceivedFetchApp() {
		super(IDbTableNames.DB_CFDP_REQUEST_RECEIVED_DATA_TABLE_NAME, APP_NAME, "CfdpRequestReceivedQuery");

		suppressInfo();
	}

	@Override
	protected void addAppOptions() {
		super.addAppOptions();
    	addOption(SHOW_COLUMNS_SHORT,SHOW_COLUMNS_LONG, null, SHOW_COLUMNS_DESC);
		addOption(CONTEXT_ID_SHORT,CONTEXT_ID_LONG, "contextId", "The unique numeric identifier for a context.");
	}

	@Override
	public IDbSqlFetch getFetch(boolean sqlStmtOnly) {
		fetch = appContext.getBean(IDbSqlFetchFactory.class).getCfdpRequestReceivedFetch(sqlStmtOnly);
		return fetch;
	}

	@Override
	public Object[] getFetchParameters() {
		final Object[] params = new Object[NUM_QUERY_PARAMS];
		return params;
	}

	@Override
	public void checkTimeType(DatabaseTimeRange range) throws ParseException {
		// TODO Auto-generated method stub

	}

	@Override
	public DatabaseTimeType getDefaultTimeType() {
        return(DatabaseTimeType.EVENT_TIME);
	}

	@Override
	public String[] getOrderByValues() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUsage() {
		return(APP_NAME + " " + CfdpFetchApp.AvailableCommandsForHelp.REQRECV + " [options]\n");
	}

}
