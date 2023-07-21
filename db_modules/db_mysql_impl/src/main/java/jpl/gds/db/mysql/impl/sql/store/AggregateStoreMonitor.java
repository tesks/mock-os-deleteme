package jpl.gds.db.mysql.impl.sql.store;

import jpl.gds.db.api.sql.store.IAggregateStoreMonitor;
import jpl.gds.db.api.sql.store.IDbSqlStore;
import jpl.gds.db.api.sql.store.StoreIdentifier;

/**
 * This class is used to keep track of the Aggregate record counts.
 * 
 * Note: The Gatherer and Inserter operate asynchronously, so the 
 * inProgressRecordCount is used to keep track of the aggregate records
 * while the LDI file is still being written to. The readyForInsertRecorCount
 * should be set to the inProgressRecordCount when the Gatherer closes the
 * LDI file and hands it off to the Inserter.
 *
 *
 */
public class AggregateStoreMonitor extends StoreMonitor implements IAggregateStoreMonitor {

    private long inProgressRecordCount = 0L;
    private long readyForInsertRecordCount = 0L;

    public AggregateStoreMonitor(final IDbSqlStore store, final StoreIdentifier si) {
        super(store, si);
    }

    @Override
    public void incInProgressRecordCount(final int incCount) {
        this.inProgressRecordCount += incCount;
    }

    @Override
    public void clearInProgressRecordCount() {
        this.inProgressRecordCount = 0L;
    }

    @Override
    public void setReadyForInsertCount(final long count) {
        readyForInsertRecordCount = count;
    }

    @Override
    public long getReadyForInsertCount() {
        return readyForInsertRecordCount;
    }

    @Override
    public long getInProgressRecordCount() {
        return inProgressRecordCount;
    }
}
