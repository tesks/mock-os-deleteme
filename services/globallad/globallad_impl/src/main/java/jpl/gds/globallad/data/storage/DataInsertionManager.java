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
package jpl.gds.globallad.data.storage;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.json.Json;
import javax.json.JsonObject;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.IGlobalLadJsonable;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.container.GlobalLadUtilities;
import jpl.gds.globallad.data.container.IGlobalLadContainer;
import jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm;
import jpl.gds.globallad.disruptor.AbstractGlobalLadInserterEventHandler;
import jpl.gds.globallad.disruptor.GlobalLadDataEvent;
import jpl.gds.globallad.disruptor.GlobalLadInserterEventHandler;
import jpl.gds.globallad.disruptor.IDisruptorProducer;
import jpl.gds.globallad.disruptor.NoOpEventHandler;
import jpl.gds.globallad.io.IBinaryLoadHandler;
import jpl.gds.globallad.spring.beans.GlobalLadBinaryLoadHandlerProvider;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;

/**
 * Disruptor data producer that will manage the inserters responsible for
 * inserting data into the global lad data store.
 * 
 * Moved the data insertion out of the global lad store and into a separate class.
 * Added start method so the disruptor can be started when the spring context is ready.
 */
public class DataInsertionManager implements IDisruptorProducer<IGlobalLADData>, IGlobalLadJsonable {
	/**
	 * Event handlers to be registered with the disruptor and used to store data in the global lad.
	 */
	private final AbstractGlobalLadInserterEventHandler[] handlers;
	
	/**
	 * Disruptor that handles global lad data events and dispatches to the event handlers.
	 */
	private final Disruptor<GlobalLadDataEvent> disruptor;
	
	/**
	 * Executor for the disruptor.  Must keep a reference to it so we can shut it down.  Calling 
	 * disruptor.shutdown() does not stop the executor and the process will hang if a worker thread is
	 * still running.
	 */
	private final ExecutorService executor;
	
	private final IGlobalLadContainer masterContainer;
	private final Tracer log;

	private final long startTime;

	private final String                             socketHost;
	private final int                                socketPort;
	private final GlobalLadBinaryLoadHandlerProvider binaryParseHandlerProvider;

	/**
	 * @param config
	 * @param log
	 * @param masterContainer
	 * @param binaryParseHandlerProvider
	 */
	public DataInsertionManager(final GlobalLadProperties config, final Tracer log,
								final IGlobalLadContainer masterContainer,
								GlobalLadBinaryLoadHandlerProvider binaryParseHandlerProvider) {
		this.masterContainer = masterContainer;
		this.log = log;

		this.socketHost = config.getServerHost();
		this.socketPort = config.getSocketServerPort();
		this.binaryParseHandlerProvider = binaryParseHandlerProvider;

		/**
		 * Use guava to create a thread factory with descriptive thread names.  This way all disruptor
		 * event handlers will be named in a way that makes sense.
		 */
		executor = Executors.newCachedThreadPool(GlobalLadUtilities.createThreadFactory("glad-inserter-%d")); 

        /**
         * Set up the disruptor that will be used instead of a queue.  
         * MPCS-7926 - triviski 2/4/2016 - Creating the wait strategy here so the 
         * global lad configuration does not need to depend on the disruptor.
         */
        WaitStrategy inserterWaitStrategy;
        
        switch(GlobalLadProperties.getGlobalInstance().getClientWaitStrategy()) {
        case SLEEP:
            inserterWaitStrategy = new SleepingWaitStrategy();
            break;
        case SPIN:
            inserterWaitStrategy = new BusySpinWaitStrategy();
            break;
        case YIELD:
            inserterWaitStrategy = new YieldingWaitStrategy();
            break;
        case BLOCK:
        default:
            inserterWaitStrategy = new BlockingWaitStrategy();
            break;
        }

        disruptor = new Disruptor<GlobalLadDataEvent>(GlobalLadDataEvent.DATA_EVENT_FACTORY, // eventFactory, 
				config.getGlobalLadRingBufferSize(), //ringBufferSize, 
                executor, //executor, 
                ProducerType.MULTI, // producerType, 
                inserterWaitStrategy
                );

		final int numInserters = config.getNumberInserters();
		handlers = createInsertHandlers(numInserters, config.isTestRingBufferInsertOnly());
		
		disruptor.handleEventsWith(handlers);

		/**
		 * MPCS-8126 triviski 4/21/2016 - Starting the disruptor in the constructor made the application
		 * hang in the event spring had a start up exception because there were non-daemon threads open.  
		 * Adding a start method and will call it from the application listener.
		 */

		startTime = System.currentTimeMillis();
	}

	/**
	 * Creates the insert handlers and adds them to an array.  This is an entry point for testing
	 * of the handlers.
	 * 
	 * @param numInserters number of inserters to create
	 * @return array of configured inserters
	 */
	/** package **/ AbstractGlobalLadInserterEventHandler[] createInsertHandlers(final int numInserters, final boolean insertTestMode) {
		final AbstractGlobalLadInserterEventHandler[] eventHandlers = new AbstractGlobalLadInserterEventHandler[numInserters];
		
		for (int i = 0; i < numInserters; i++) {
			eventHandlers[i] = insertTestMode ? 
					new NoOpEventHandler(masterContainer, i, numInserters) : // If in test mode use the no-op handler.  This will stop just short of inserting into the global lad.
					new GlobalLadInserterEventHandler(masterContainer, i, numInserters);
		}
		
		return eventHandlers;
	}
	
	/**
	 * Stops the disruptor.
	 */
	public void stop() {
		disruptor.shutdown();
		executor.shutdown();
	}
	
	/**
	 * Starts the disruptor.
	 */
	public void start()	{
		disruptor.start();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onData(final IGlobalLADData data) {
		disruptor.getRingBuffer().publishEvent(GlobalLadDataEvent.DATA_TRANSLATOR, data);
	}

	/**
	 * Reads the contents of dumpFile into the lad. If it is null or doesnt
	 * exist attempts to load from the persisted dump files at the configured
	 * backup location.
	 * 
	 * @param dumpFile
	 *            data file to restore
	 * @param doClear
	 *            If true clears the lad before loading.
	 * @return boolean indicating a successful load.
	 */
	public boolean initializeLadFromBackup(final File dumpFile, final boolean doClear) {
		if (doClear) {
			this.masterContainer.clear();
		}
		/**
		 * Possible there is no dump file to work with.
		 */
		if (dumpFile != null) {
			log.info("Attempting to initialized from restore file " , dumpFile);
		} else {
			return false;
		}

		/**
		 * Connect to the global lad server as a client and pass the contents of
		 * the restore file like any client would.
		 */
        TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.ERROR);

		try (FileInputStream input = new FileInputStream(dumpFile)) {
			final IBinaryLoadHandler binaryLoadHandler = binaryParseHandlerProvider.getBinaryLoadHandler(input, this);
			binaryLoadHandler.execute();
		} catch (final Exception e) {
			log.error("Failed to initialize from backup file: " + e.getMessage(), e.getCause());
		} finally {
			TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.INFO);
		}
		
		log.info("Successfully initialized from restore file " , dumpFile);
		return true;
	}
	
	/**
	 * @return the max lastSequence from the event handlers
	 */
	public long getLastHandlerSequence() {
		long seq = -1;
		for (final AbstractGlobalLadInserterEventHandler handler : handlers) {
			if (handler.lastSequence > seq) {
				seq = handler.lastSequence;
			}
		}
		
		return seq;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JsonObject getStats() {
		final int size = disruptor.getRingBuffer().getBufferSize();
		final long remaining = disruptor.getRingBuffer().remainingCapacity();
		
		return Json.createObjectBuilder()
				.add("globalLadRunTimeMS", System.currentTimeMillis()-startTime)
				.add("lastDisruptorSequence", this.getLastHandlerSequence()+1)
				.add("ringBufferSize", size)
				.add("remainingEvents", remaining)
				.add("backlog", size - remaining)
				.build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JsonObject getMetadata(final IGlobalLadContainerSearchAlgorithm matcher) {
		return Json.createObjectBuilder().build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getJsonId() {
		return "inserterManager";
	}
}
