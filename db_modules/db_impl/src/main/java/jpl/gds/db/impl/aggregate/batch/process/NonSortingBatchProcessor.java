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

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.sql.fetch.aggregate.IEhaAggregateDbRecord;
import jpl.gds.db.api.sql.fetch.aggregate.ProcessedBatchInfo;
import jpl.gds.db.api.sql.fetch.aggregate.RecordBatchContainer;
import jpl.gds.shared.sys.SystemUtilities;

public class NonSortingBatchProcessor extends AggregateBatchProcessor {
	
	public NonSortingBatchProcessor(final ApplicationContext appContext, 
			final RecordBatchContainer<IEhaAggregateDbRecord> batchContainer) {
		super(appContext, batchContainer);
	}
	
	@Override
	protected void perAggregateProcess() {
		SystemUtilities.doNothing();
	}
	
	@Override
	protected void postAggregateProcess() {
		batchInfo = new ProcessedBatchInfo(recordList);
	}
}
