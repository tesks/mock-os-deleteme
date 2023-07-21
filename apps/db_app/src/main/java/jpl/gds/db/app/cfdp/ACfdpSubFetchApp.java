package jpl.gds.db.app.cfdp;

import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.db.app.AbstractFetchApp;
import org.apache.commons.cli.ParseException;

public abstract class ACfdpSubFetchApp extends AbstractFetchApp {

    public ACfdpSubFetchApp(String tableName, String appName, String app) {
        super(tableName, appName, app);
    }

    /**
     * Populates the requiredOptions member variable with the required option name
     * strings. Adds the long option name by default unless there is none, otherwise
     * it adds the short option name.
     *
     * @throws ParseException Parse error
     */
    @Override
    public void createRequiredOptions() throws ParseException {
        super.createRequiredOptions();
        requiredOptions.add(CONTEXT_ID_LONG);
    }

}
