package jpl.gds.db.api.sql.fetch.aggregate;

/**
 * An interface to be implemented by the Batch Writer Factory
 *
 */
public interface IBatchWriterFactory {

    /**
	 * Gets a batch writer object based on fetch configuration and passed in parameters
	 * 
	 * @param batchFileName The name of the batch file to create
	 * @return The batch writer object
	 * @throws AggregateFetchException
	 */
	public IStringWriter getBatchWriter(final String batchFileName) throws AggregateFetchException;
}
