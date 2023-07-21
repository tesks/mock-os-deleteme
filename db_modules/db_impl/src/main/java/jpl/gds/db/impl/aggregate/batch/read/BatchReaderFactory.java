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
package jpl.gds.db.impl.aggregate.batch.read;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.apache.commons.io.LineIterator;

import jpl.gds.db.api.sql.fetch.aggregate.AggregateFetchException;
import jpl.gds.db.api.sql.fetch.aggregate.IAggregateFetchConfig;
import jpl.gds.db.api.sql.fetch.aggregate.IBatchFileRecordProvider;
import jpl.gds.db.api.sql.fetch.aggregate.IBatchReaderFactory;
import jpl.gds.db.api.sql.fetch.aggregate.IEhaAggregateQueryCoordinator;
import jpl.gds.db.api.sql.fetch.aggregate.ProcessedBatchInfo;


/**
 * Factory class used to create batch reader objects based
 * on fetch configuration.
 *
 */
public class BatchReaderFactory implements IBatchReaderFactory {
	
	private final IAggregateFetchConfig config;
	
	/**
	 * Constructs a Batch Reader Factory with the specified 
	 * aggregate fetch configuration and tracer
	 * 
	 * @param config The aggregate fetch configuration
	 * @param trace The tracer
	 */
	public BatchReaderFactory(final IAggregateFetchConfig config) {
		this.config = config;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.aggregate.IBatchReaderFactory#getBatchRecordProvider(jpl.gds.db.api.sql.fetch.aggregate.IEhaAggregateQueryCoordinator, java.lang.String)
	 */
	@Override
    public IBatchFileRecordProvider getBatchRecordProvider(final IEhaAggregateQueryCoordinator aggQueryQoordinator, final String batchId) throws AggregateFetchException {
        	
        final ProcessedBatchInfo batchIndex = aggQueryQoordinator.getBatch(batchId);
        final String batchFileName = batchIndex.getRecordFilename();

        if (config.isTemplateSpecified()) {

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(new File(batchFileName));
            } catch (final FileNotFoundException e) {
                throw new AggregateFetchException(
                        "Temporary Template Batch File: '" + batchFileName + "' not found for Batch ID: '" + batchId,
                        e);
            }

            return new TemplateBatchFileRecordProvider(fis);
        } else {
            BufferedReader recordReader = null;

            try {
                recordReader = new BufferedReader(new FileReader(batchFileName));
            } catch (final FileNotFoundException e) {
                throw new AggregateFetchException(
                        "Temporary CSV Batch File: '" + batchFileName + "' not found for Batch ID: '" + batchId, e);
            }

            return new BatchFileRecordReader(new LineIterator(recordReader));
        }
		
	}
}
