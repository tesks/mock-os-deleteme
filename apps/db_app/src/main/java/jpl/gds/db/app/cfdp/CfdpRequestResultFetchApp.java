package jpl.gds.db.app.cfdp;

import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import org.apache.commons.cli.ParseException;

public class CfdpRequestResultFetchApp extends ACfdpSubFetchApp {

	private static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_get_cfdp reqresult");

	private static final int NUM_QUERY_PARAMS = 1;

	public CfdpRequestResultFetchApp() {
		super(IDbTableNames.DB_CFDP_REQUEST_RESULT_DATA_TABLE_NAME, APP_NAME, "CfdpRequestResultQuery");

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
		fetch = appContext.getBean(IDbSqlFetchFactory.class).getCfdpRequestResultFetch(sqlStmtOnly);
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
		return(APP_NAME + " " + CfdpFetchApp.AvailableCommandsForHelp.REQRESULT.toString() + " [options]\n");
	}

}
