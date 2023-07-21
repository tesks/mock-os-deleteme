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

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.sql.fetch.aggregate.AggregateFetchMarkers;
import jpl.gds.db.api.sql.fetch.aggregate.IChannelStreamFilter;
import jpl.gds.db.api.sql.fetch.aggregate.IOutputConsumer;
import jpl.gds.db.api.sql.fetch.aggregate.IOutputController;
import jpl.gds.db.api.sql.fetch.aggregate.OutputRecordBatchContainer;
import jpl.gds.shared.log.Tracer;

/**
 * The class handles the aggregate query result output. The actual output write is 
 * delegated to the Output Consumer. It uses a Blocking Queue to throttle 
 * the output rate based on the Output Consumers write rate.
 *
 * @param <T>
 */
public class OutputController<T> implements IOutputController<T> {
	
	private AtomicBoolean running;
	private final BlockingQueue<List<T>> outputRecordQueue;
	private final Tracer trace;
	private final IOutputConsumer<T> outputConsumer;
	private List<IChannelStreamFilter<T>> outputStreamFilters;
	
	/**
	 * Constructor
	 * 
	 * @param appContext the Spring Application Context
	 * @param dataRecordQueue the output record queue
	 * @param outputConsumer the output consumer
	 * @param trace the tracer
	 */
	public OutputController(final ApplicationContext appContext, 
			final BlockingQueue<List<T>> dataRecordQueue, 
			final IOutputConsumer<T> outputConsumer, final Tracer trace) {
		this.outputRecordQueue = dataRecordQueue;
		this.outputConsumer = outputConsumer;
		this.trace = trace;
	}
	
	@Override
	public void run() {
	    
		running = new AtomicBoolean(true);
		
		while (running.get()) {
			
			List<T> recordList = null;
			try {
				recordList = outputRecordQueue.take();
			} catch (final InterruptedException e) {
				if (!outputRecordQueue.isEmpty()) {
					trace.error(AggregateFetchMarkers.OUTPUT_CONTROLLER, 
					        "Thread Interupted but queue still has "
							+ outputRecordQueue.size() + " batches to process!!");
				}
				running.set(false);
			}
			
			trace.debug(AggregateFetchMarkers.OUTPUT_CONTROLLER, 
			        "Output Record Queue take, QUEUE size = " + outputRecordQueue.size());
			
			if (recordList != null && !recordList.isEmpty()) {
			    
			    if (outputStreamFilters != null && !outputStreamFilters.isEmpty()) {
			        for (final IChannelStreamFilter<T> filter : outputStreamFilters) {
			            recordList = filter.filterRecordList(recordList);
			        }
			    }
			    
				outputConsumer.handleRecordBatch(new OutputRecordBatchContainer<T>(recordList));
			
			} else {
				trace.debug(AggregateFetchMarkers.OUTPUT_CONTROLLER, "Got end of processing message");
				// we should shut down
				running.set(false);
			}
		}
	}

	@Override
	public boolean isRunning() {
		return running.get();
	}

    @Override
    public void setStreamFilters(final List<IChannelStreamFilter<T>> outputStreamFilters) {
        this.outputStreamFilters = outputStreamFilters;
    }
}

