package jpl.gds.db.api.sql.fetch.aggregate;

/**
 * An interface implemented by the Index Reader Factory
 *
 */
public interface IIndexReaderFactory {
    public IBatchFileIndexProvider getBatchIndexProvider(final IEhaAggregateQueryCoordinator aggQueryQoordinator,
            final String batchId) throws AggregateFetchException;
}
