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
package jpl.gds.db.impl.aggregate.batch.output;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import jpl.gds.db.api.sql.fetch.aggregate.IOutputConsumer;
import jpl.gds.db.api.sql.fetch.aggregate.OutputRecordBatchContainer;
import jpl.gds.shared.log.Tracer;

/**
 * This class functions as a String record output writer which uses
 * a specific instance of a Writer sub-class for the actual write.
 *
 */
public class StringRecordOutputWriter implements IOutputConsumer<String>{
	
	private final Writer writer;
	private final Tracer trace;

	/**
	 * Constructs an output writer with specified Writer object
	 * 
	 * @param writer the Writer used to write output records
	 * @param trace the tracer
	 */
	public StringRecordOutputWriter(final Writer writer, final Tracer trace) {
		this.writer = writer;
		this.trace = trace;
	}
	
	@Override
	public void handleRecordBatch(final OutputRecordBatchContainer<String> recordBatch) {
		final List<String> recordList = recordBatch.getRecordBatch();
		for (final String record : recordList) {
			try {
				writer.write(record);
			} catch (final IOException e) {
				trace.error("Encountered error during write: " + e.getMessage());
			}
		}
		try {
			writer.flush();
		} catch (final IOException e) {
			trace.error("Encountered error doing writer.flush(): " + e.getMessage());
		}
	}
}
