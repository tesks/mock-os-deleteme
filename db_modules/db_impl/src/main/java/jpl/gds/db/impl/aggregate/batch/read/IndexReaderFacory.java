package jpl.gds.db.impl.aggregate.batch.read;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.apache.commons.io.LineIterator;

import jpl.gds.db.api.sql.fetch.aggregate.AggregateFetchException;
import jpl.gds.db.api.sql.fetch.aggregate.IBatchFileIndexProvider;
import jpl.gds.db.api.sql.fetch.aggregate.IEhaAggregateQueryCoordinator;
import jpl.gds.db.api.sql.fetch.aggregate.IIndexReaderFactory;
import jpl.gds.db.api.sql.fetch.aggregate.ProcessedBatchInfo;

/**
 * Factory class used to create the batch Index reader
 *
 */
public class IndexReaderFacory implements IIndexReaderFactory {
	
	@Override
	public IBatchFileIndexProvider getBatchIndexProvider(final IEhaAggregateQueryCoordinator aggQueryQoordinator, final String batchId) throws AggregateFetchException {
	    
	    final ProcessedBatchInfo batchIndex = aggQueryQoordinator.getBatch(batchId);
        final String indexFileName = batchIndex.getIndexFilename();
        final BufferedReader indexReader;

        try {
            indexReader = new BufferedReader(new FileReader(indexFileName));
        } catch (final FileNotFoundException e) {
            throw new AggregateFetchException(
                    "Temporary Batch Index File: '" + indexFileName + "' not found for Batch ID: '" + batchId, e);
        }

        return new BatchFileIndexReader(new LineIterator(indexReader), batchId);
	}
}
