package jpl.gds.db.impl.aggregate.batch.write;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import jpl.gds.db.api.sql.fetch.aggregate.AggregateFetchException;
import jpl.gds.db.api.sql.fetch.aggregate.IAggregateFetchConfig;
import jpl.gds.db.api.sql.fetch.aggregate.IStringWriter;
import jpl.gds.db.api.sql.fetch.aggregate.IBatchWriterFactory;

/**
 * Factory class used to create batch writer objects based
 * on fetch configuration.
 *
 */
public class BatchWriterFactory implements IBatchWriterFactory {
	
    private IAggregateFetchConfig config;
    
	/**
	 * Constructs a Batch Writer Factory
	 * @param config 
	 * 
	 * @param trace
	 */
	public BatchWriterFactory(final IAggregateFetchConfig config) {
	    this.config = config;
	}
		
	/* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.aggregate.IBatchWriterFactory#getBatchWriter(java.lang.String)
	 */
	public IStringWriter getBatchWriter(final String batchFileName) throws AggregateFetchException {
		
		IStringWriter writer = null;
		
		if (config.isTemplateSpecified()) {
			try {
				final FileOutputStream fos = new FileOutputStream(new File(batchFileName));
				writer = new TemplateBatchRecordWriter(fos);
			} catch (FileNotFoundException e) {
                throw new AggregateFetchException(
                        "Unable to create template Batch File for Template based records: '" + batchFileName + "'",e);
			}
		} else {
			try {
				final BufferedWriter buffWriter = new BufferedWriter(new FileWriter(batchFileName));
				writer = new CsvBatchRecordWriter(buffWriter);
			} catch (IOException e) {
                throw new AggregateFetchException(
                        "Unable to create temporary Batch File for CSV records: '" + batchFileName + "'",e);
			}
		}
		
		return writer;
	}
}
