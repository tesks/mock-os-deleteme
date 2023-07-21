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
package jpl.gds.db.impl.aggregate.batch.process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.sql.fetch.aggregate.AggregateFetchException;
import jpl.gds.db.api.sql.fetch.aggregate.AggregateFetchMarkers;
import jpl.gds.db.api.sql.fetch.aggregate.ComparableIndexItem;
import jpl.gds.db.api.sql.fetch.aggregate.IEhaAggregateDbRecord;
import jpl.gds.db.api.sql.fetch.aggregate.IStringWriter;
import jpl.gds.db.api.sql.fetch.aggregate.ProcessedBatchInfo;
import jpl.gds.db.api.sql.fetch.aggregate.RecordBatchContainer;

/**
 * This is File Based Sorting Processor which will be used to process
 * aggregates when sorting is requested.
 *
 */
public class FileBasedSortingProcessor extends AggregateBatchProcessor {
	
	private IStringWriter writer;
	private String batchRecordFile;
	private String batchIndexFile;
	private final String PATH_SEPARATOR = File.separator;

	/**
	 * Constructs a file based sorting batch processor
	 * 
	 * @param appContext 
	 * @param batchContainer
	 */
	public FileBasedSortingProcessor(final ApplicationContext appContext, 
			final RecordBatchContainer<IEhaAggregateDbRecord> batchContainer) {
		super(appContext, batchContainer);
	}

	@Override
	protected void postAggregateProcess() throws AggregateFetchException, IOException {
		
		sortRecordIndexList();
		batchRecordFile = outputDir + PATH_SEPARATOR + "BatchProcessor_" + batchProcessorNumber + "_" + batchId + ".tcf.sorted";
		batchIndexFile = outputDir + PATH_SEPARATOR + "BatchProcessor_" + batchProcessorNumber + "_" + batchId + ".tcif.sorted";
		
        batchInfo = new ProcessedBatchInfo(batchRecordFile, batchIndexFile);
        
        
        final long indexfileWriteStart = System.nanoTime();
        
        try (final BufferedWriter bw = new BufferedWriter(new FileWriter(batchIndexFile))) {
            for (final ComparableIndexItem<String> cindex : compList) {
                bw.write(cindex.getComparable() + System.lineSeparator());
            }
        } catch (final IOException e) {
            throw new IOException("Unable to write index to batch index file: " + batchIndexFile, e);
        }
        
        final long totalIndexBatchFileWriteTime = System.nanoTime() - indexfileWriteStart;
        trace.debug(AggregateFetchMarkers.FILE_BASED_SORTING_PROCESSOR, 
                "BatchProcessor # " + batchProcessorNumber + " for: " + batchId 
                + " Batch Index File Write time = "
                + (totalIndexBatchFileWriteTime / 1000000.0) + " msecs");
		
		final long fileWriteStart = System.nanoTime();
		writer = batchWriterFactory.getBatchWriter(batchRecordFile);
		
		for (final ComparableIndexItem<String> cw2 : compList) {				
			try {
				writer.write(recordList.get(cw2.getIndex()));
			} catch (final IOException e) {
				throw new IOException("Unable to write record to batch file: " + batchRecordFile, e);
			}		
		}
		
		try {
			writer.close();
		} catch (final IOException e) {
            throw new IOException("Unable to close batch file: " + batchRecordFile, e);
		}
		
		totalBatchFileWriteTime = System.nanoTime() - fileWriteStart;
		trace.debug(AggregateFetchMarkers.FILE_BASED_SORTING_PROCESSOR,
		        "BatchProcessor # " + batchProcessorNumber + " for: " + batchId + " Batch File Write time = " 
				+ (totalBatchFileWriteTime / 1000000.0) + " msecs");
		
		trace.debug(AggregateFetchMarkers.FILE_BASED_SORTING_PROCESSOR,
		        "BatchProcessor # " + batchProcessorNumber + " finished writing temp file for: " + batchId
				+ " record count: " + compList.size() + " Write Time: " + (System.nanoTime() - fileWriteStart) / 1000000.0
				+ " msecs");
		
		compList.clear();
		compList = null;
	}
}

