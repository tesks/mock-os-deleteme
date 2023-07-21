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
package jpl.gds.db.mysql.impl.sql.store.ldi;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.ApplicationContext;

import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader;
import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.db.api.sql.store.IAggregateStoreMonitor;
import jpl.gds.db.api.sql.store.IStoreMonitor;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.api.sql.store.ldi.IInserter;
import jpl.gds.db.api.sql.store.ldi.ILDIStore;
import jpl.gds.db.mysql.impl.sql.store.AbstractMySqlStore;
import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.performance.IPerformanceData;
import jpl.gds.shared.performance.QueuePerformanceData;
import jpl.gds.shared.thread.SleepUtilities;

/**
 * This is the abstract superclass that is extended by all database classes that
 * are responsible for inserting data into the database through LOAD DATA
 * INFILE. The main work is done by static class StaticLDIStore, and this class
 * passes through methods to that class.
 *
 */
public abstract class AbstractLDIStore extends AbstractMySqlStore implements ILDIStore {
	/** Maximum Frame body size in bytes */
	public static final int MAX_FRAME = ITelemetryFrameHeader.MAX_FRAME;

	public static final int MAX_HEADER = 1024;
	public static final int MAX_TRAILER = 255;
	public static final int MAX_PACKET = ISpacePacketHeader.MAX_PACKET;

	//  MPCS-7135 - Added members for async serialization below.

	/**
	 * Indicates whether this store performs LDI serialization asynchronously on a
	 * separate thread.
	 */
	protected boolean doAsyncSerialization = false;

	/**
	 * MPCS-7168 - Added performance reporter
	 */
	private PerformanceReporter _perf_reporter = null;

	/**
	 * Time to wait for join of the serializer thread upon shutdown (millis).
	 */
	private static final long SERIALIZER_JOIN = 5000L;

	/** Time to wait between checks for serializer queue idle-down (millis) */
	private static final long SERIALIZER_SHUTDOWN_SLEEP = 250L;

	/**
	 * Time to wait to queue new objects to the serializer queue (millis)
	 * 
	 * MPCS-7376 - Update value, which was inadvertently left as 0
	 * and can cause the offer to spin if the queue is full.
	 */
	private static final long SERIALIZATION_QUEUE_OFFER_WAIT = 1000;

	/** Indicates whether this store supports async serialization at all. */
	private final boolean supportsAsyncSerialization;

	/** Length of serialization queue, if enabled. */
	private int maxSerializationQueueLen = 0;

	/** Thread that will perform async serialization, if enabled. */
	private Thread serializationThread = null;

	/** Async serialization queue, if enabled. */
	private BlockingQueue<Object> serializationQueue = null;

	/** Flag used to signal the serialization queue it is time to idle-down. */
	private final AtomicBoolean resourceStarted = new AtomicBoolean(false);

	/**
	 * Flag indicating whether this store is in the process of idling down. We do
	 * not use the flush flag because the serialization worker resets it.
	 */
	protected AtomicBoolean inIdleDown = new AtomicBoolean(false);

	/** Serialization queue performance data */
	private QueuePerformanceData _queuePerformance = null;

	/** Serialization queue high water length */
	private long _highWaterMark = 0;

	/**
	 * Serialization queue size tracker.
	 * 
	 *  MPCS-7376 - Track queue size without calling queue.size().
	 */
	private final AtomicInteger queueSize = new AtomicInteger(0);

	/**
	 * Monitor for this store
	 */
	private IStoreMonitor monitor;

	/**
	 * Creates an instance of AbstractLDIStore.
	 *
	 * @param supportsAsync
	 *            indicates whether this store supports asynchronous LDI
	 *            serialization.
	 * 
	 * @version MPCS-7135 -  Added supportsAsync parameter.
	 * @param appContext
	 *            The current application context
	 */
	public AbstractLDIStore(final ApplicationContext appContext, final StoreIdentifier si,
			final boolean supportsAsync) {
		super(appContext, si, false); // LDI supplies its own connections

		if (contextConfig.getContextId().getNumber() == null) {
			contextConfig.getContextId().setNumber(0L);
		}
		supportsAsyncSerialization = supportsAsync;

		// MPCS-7135 -  Added call to init() method.
		localInit();
	}

	/**
	 * Initialize the configuration of this store.
	 * 
	 * @version MPCS-7135 - Added method.
	 */

	protected void localInit() {
		if (dbProperties.getUseDatabase() == false) {
			return;
		}

		final int maxQueue = dbProperties.getAsyncQueueSize(getStoreIdentifier().name());
		if (supportsAsyncSerialization) {
			if (maxQueue <= 0) {
				trace.warn(Markers.DB, "Serialization queue size is not configured for store ",
						this.getStoreIdentifier(), " but this store does support asynchronous serialization");
			}
			maxSerializationQueueLen = maxQueue;
			doAsyncSerialization = maxSerializationQueueLen > 0;

			/*
			 *  MPCS-7168 - Set up queue performance object. MPCS-7198 -
			 *  Added units to call below
			 */
			if (doAsyncSerialization) {
				_queuePerformance = new QueuePerformanceData(appContext.getBean(PerformanceProperties.class),
						"LDI Serializer (" + this.getStoreIdentifier() + ")", maxSerializationQueueLen, false, true,
						"records");
			}
		} else if (maxQueue != 0) {
			trace.warn(Markers.DB, "Serialization queue size is configured for store ", this.getStoreIdentifier(),
					" but this store does not support asynchronous serialization");
		}
	}

	/**
	 * Perform our local initialization. Overridden by subclasses.
	 *
	 * Called to initialize the store. The first thing we do is to save the store so
	 * that we can close it at the end. Then, if this is the first time we are
	 * called, start the threads. Note that we have our own connection.
	 */
	@Override
	protected void startResource() {
		if (resourceStarted.getAndSet(true)) {
			return;
		}
		super.startResource();

		this.monitor = archiveController.getStoreMonitor(si);

		/*  MPCS-7168 - Register the performance reporter. */
		trace.debug("Starting Performance Reporter for ", this.getStoreIdentifier());
		_perf_reporter = new PerformanceReporter(appContext);

		/*
		 * MPCS-7135 - Added startup of async serialization thread if
		 * enabled.
		 */
		if (doAsyncSerialization) {
			serializationQueue = new ArrayBlockingQueue<Object>(this.maxSerializationQueueLen);
			archiveController.setSerializationFlush(false);
			serializationThread = new Thread(new SerializationQueueWorker(),
					"LDI Serializer [" + this.getStoreIdentifier() + "]");
			serializationThread.start();
			trace.debug("Serialization thread started for " + this.getStoreIdentifier());
		}
		synchronized (this.monitor.getSyncMonitor()) {
			startInserter();
			this.monitor.setActive(true);
		}
  
		// MPCS-9908 jy Marking the archive controller as active kicks off the daemon thread that
                //              monitors for any active stores. If none are active, it proceeds to shut things
                //              down - by moving this call below this.monitor.setActive(true), we ensure that
		//              at least one store is marked as active so things go as they should (kinda).
		// NOTE: this should be considered a temporary solution only, see MPCS-9952.
		//
		archiveController.getAndSetStarted(true);
	}

	/**
	 * Perform our local shutdown. Overridden by subclasses.
	 *
	 * Remember that stores are instances, but our processing here is static. We
	 * shut things down, but only when ALL stores have been shut down.
	 *
	 * The gatherer is told to flush. When it has flushed all tables, it resets that
	 * flag, which tells us to then notify the inserter.
	 *
	 * The inserter performs some important shut down actions.
	 */
	@Override
	protected void stopResource() {
		if (!resourceStarted.getAndSet(false)) {
			return;
		}

		/*
		 * MPCS-7135 - Added idle-down steps for the serialization queue.
		 * This is just in case the subclass did not do this, which it is supposed to
		 * before it sets the state to inactive. This call is just insurance.
		 */
		idleDownSerializer();

		/*
		 * Indicate that this store is no longer active.
		 */
		synchronized (this.monitor.getSyncMonitor()) {
			interruptInserter();
			this.monitor.setActive(false);
		}

		/*
		 * MPCS-7168 - De-register the performance reporter as the last
		 * thing
		 */
		_perf_reporter.deregister();
		super.stopResource();
	}

	/**
	 * Idles down the asynchronous serialization queue.
	 */
	protected void idleDownSerializer() {
		if (this.doAsyncSerialization && !inIdleDown.getAndSet(true)) {
			/* Wait for any ongoing offer to the queue to complete. */
			SleepUtilities.checkedSleep(SERIALIZATION_QUEUE_OFFER_WAIT);

			/* Now tell the queue to flush. Nothing more will be queued. */
			trace.debug("Flushing items in the serialization queue for ", this.getStoreIdentifier());
			archiveController.setSerializationFlush(true);
			serializationThread.interrupt();

			// Wait until the serialization queue is done flushing.
			while (archiveController.isSerializationFlush()) {
				trace.debug("Waiting for serialization queue flush for ", this.getStoreIdentifier());
				SleepUtilities.checkedSleep(SERIALIZER_SHUTDOWN_SLEEP);
			}

			trace.debug("Serialization queue flushed for ", this.getStoreIdentifier());

			/*
			 * The serialization thread should be done. This should return immediately.
			 */
			SleepUtilities.checkedJoin(serializationThread, SERIALIZER_JOIN,
					"LDI Serializer [" + this.getStoreIdentifier() + "]", trace);
		}
	}

	/**
	 * Offers the given object to the serialization queue. Blocks until the item is
	 * accepted, the store begins idle-down (in which case, does nothing), or the
	 * calling thread is interrupted (in which case, logs an error and return.)
	 * 
	 * @param toQueue
	 *            the object to queue
	 * @throws UnsupportedOperationException
	 *             if the store does not support this operation
	 * @version MPCS-7135 - Added method.
	 */
	protected void queueForSerialization(final Object toQueue) {
		if (!this.supportsAsyncSerialization) {
			throw new UnsupportedOperationException("This store does not operate asynchronously");
		}

		if (doAsyncSerialization && !inIdleDown.get()) {
			boolean queued = false;
			try {
				while (!queued) {
					queued = serializationQueue.offer(toQueue, SERIALIZATION_QUEUE_OFFER_WAIT, TimeUnit.MILLISECONDS);
					if (!queued) {
						trace.debug("Serialization queue is blocked for store ", this.getStoreIdentifier());
					} else {
						/*
						 * MPCS-7376 - Track queue size without using queue.size()
						 */
						/*
						 * MPCS-7245 - Track high water mark on the add rather than the
						 * remove
						 */
						_highWaterMark = Math.max(queueSize.incrementAndGet(), _highWaterMark);
					}
				}
			} catch (final InterruptedException e) {
				// should not happen
				trace.error(getStoreIdentifier(), " queueObjectForSerialization was interrupted.. Size=",
						serializationQueue.size(), " Max=", maxSerializationQueueLen, " inIdleDown= ", inIdleDown, " ",
						ExceptionTools.getMessage(e), e);
				Thread.dumpStack();
			}
		} else {
			trace.warn("queueForSerialization was called while idling down; not queuing the object");
		}
	}

	/**
	 * Should invoke the store-specific method to serialize the object and write it
	 * to the LDI file. The default implementation throws and must be overridden by
	 * stores that support the asynchronous serialization capability.
	 * 
	 * @param toInsert
	 *            the object to process
	 * @return true if the object was successfully serialized, false if not
	 * 
	 * @throws UnsupportedOperationException
	 *             if the store does not support this operation
	 * 
	 * @version MPCS-7135 - Added method.
	 */
	protected boolean serializeToLDIFile(final Object toInsert) {
		throw new UnsupportedOperationException("This store does not operate asynchronously");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jpl.gds.db.api.sql.store.ldi.ILDIStore#getPerformanceData()
	 */
	@Override
	public List<IPerformanceData> getPerformanceData() {
		if (_queuePerformance != null) {
			/*
			 * MPCS-7376 - Track queue size without using queue.size()
			 */
			/*
			 * MPCS-7649 - Make sure queue size is reported >= 0. Because
			 * this is being loosely tracked without using a call to the queue itself, it
			 * can get slightly out of sync, and may be < 0.
			 */
			_queuePerformance.setCurrentQueueSize(Math.max(0, queueSize.get()));
			_queuePerformance.setHighWaterMark(_highWaterMark);
			return Arrays.asList((IPerformanceData) _queuePerformance);
		} else {
			return new LinkedList<IPerformanceData>();
		}
	}

	/**
	 * 
	 * This Runnable works off the serialization queue by calling the store-specific
	 * method that serializes them to the LDI file.
	 *
	 *
	 * @version MPCS-7135 - Added class.
	 */
	private class SerializationQueueWorker implements Runnable {

		private long insertCount;

		/**
		 * Run method for thread that catches everything.
		 */
		@Override
		public void run() {
			try {
				/* MPCS-7376 - Add temp list to drain queue to */
				final List<Object> tempObjs = new LinkedList<Object>();
				Object toInsert = null;
				boolean stopping = false;

				/* Loop until told to stop and the queue is empty. */
				while (!stopping || (stopping && !serializationQueue.isEmpty())) {

					try {
						toInsert = serializationQueue.take();
					} catch (final InterruptedException e) {
						trace.debug("SerializationQueueWorker interrupted for ", getStoreIdentifier(), " ",
								ExceptionTools.getMessage(e), " QueueSize=", serializationQueue.size());

						/* If the flush flag is set, we are stopping. */
						if (archiveController.isSerializationFlush()) {
							trace.debug("Stopping serialization... flush has been triggered. Queuesize=",
									serializationQueue.size());
							stopping = true;
						}
						toInsert = null;
					}

					if (toInsert != null) {

						tempObjs.add(toInsert);
						/*
						 * MPCS-7376 - If there is anything in the queue, drain the
						 * entire thing. Reduces lock usage.
						 */
						serializationQueue.drainTo(tempObjs);

						/*
						 * MPCS-7376 - Track high water mark without calling
						 * queue.size().
						 */
						/*
						 * MPCS-7245 - Track high water mark on the add rather than the
						 * remove
						 */
						queueSize.addAndGet(-tempObjs.size());

						for (final Object o : tempObjs) {
							if (serializeToLDIFile(o)) {
								insertCount++;
							}
						}
						toInsert = null;
						tempObjs.clear();
					}
				}
				trace.debug("LDI serialization queue for ", getStoreIdentifier(), " stopped. Total number of inserts: ",
						insertCount);

				archiveController.setSerializationFlush(false);
			} catch (final Throwable t) {
				trace.error("serialization queue worked dies: ", t.getCause());

				t.printStackTrace();
			}
		}

	}

	/**
	 * @param ie
	 * @param bb1
	 */
	public void writeToStream(final BytesBuilder bb1) {
		writeToStream(bb1, (BytesBuilder) null);
	}

	/**
	 * @param ie
	 * @param bb1
	 * @param bb2
	 */
	public void writeToStream(final BytesBuilder bb1, final BytesBuilder bb2) {
		switch (this.monitor.getSi()) {
		case ChannelValue:
		case HeaderChannelValue:
		case MonitorChannelValue:
		case SseChannelValue:
			writeChannelValueToStream(bb1, bb2);
			break;
		case CommandMessage:
			writeCommandMessageToStream(bb1, bb2);
			break;
		case Packet:
		case SsePacket:
		case Frame:
			writePacketOrFrameToStream(bb1, bb2);
			break;
		case Product:
		case LogMessage:
			writeLogMessageToStream(bb1);
			break;
		case CfdpIndication:
		case CfdpFileGeneration:
		case CfdpFileUplinkFinished:
		case CfdpRequestReceived:
		case CfdpRequestResult:
		case CfdpPduReceived:
		case CfdpPduSent:
			writeCfdpDataToStream(bb1);
			break;
		case None:
		default:
			break;
		}
	}

	public void writeToStream(final BytesBuilder bb1, final BytesBuilder[] bb2) {
	    writeToStream(bb1, bb2, null);
	}
	
	/**
	 * @param ie
	 * @param bb1
	 * @param bb2
	 */
	public void writeToStream(final BytesBuilder bb1, final BytesBuilder[] bb2, final Integer aggRecCount) {
		switch (this.monitor.getSi()) {
		case Evr:
		case SseEvr:
			writeEvrToStream(bb1, bb2);
			break;
        case ChannelAggregate:
        case HeaderChannelAggregate:
        case SseChannelAggregate:
        case MonitorChannelAggregate:
            writeChannelAggregateToStream(bb1, bb2, aggRecCount);
            break;	
		case CommandMessage:
		case LogMessage:
		case Product:
		case Packet:
		case SsePacket:
		case Frame:
		case ChannelValue:
		case HeaderChannelValue:
		case MonitorChannelValue:
		case SseChannelValue:
		case None:
		default:
			break;
		}
	}

	/**
	 * @param ie
	 * @param channelValue
	 * @param channelMetadata
	 */
	protected void writeChannelValueToStream(final BytesBuilder channelValue, final BytesBuilder channelMetadata) {
		final StoreIdentifier si = this.monitor.getSi();
		synchronized (monitor.getSyncMonitor()) {
			if (!monitor.isActive()) {
				trace.warn("Ignoring ", si, "(s) sent while inactive");
				return;
			}

			if (monitor.getValueStream() == null) {
				monitor.clearValuesInStream();
				monitor.setValueStream(archiveController.openStream(storeConfig.getValueTableName()));

				if (monitor.getValueStream() == null) {
					// Unable to write
					return;
				}
			}

			try {
				monitor.incValuesInStream();
				monitor.incValuesProcessed();
				channelValue.write(monitor.getValueStream().getTwo());
			} catch (final IOException ioe) {
				trace.error(IDbSqlArchiveController.WRITE_ERROR, storeConfig.getValueTableName(), ": ", ioe.getCause());
			}

			if (channelMetadata == null) {
				/** MPCS-7714 Added check */
				checkStreamCount(monitor.getValuesInStream(), storeConfig.getValueTableName(), 0L,
						storeConfig.getMetadataTableName());
				return;
			}

			if (channelMetadata.isEmpty()) {
				trace.error(IDbSqlArchiveController.WRITE_ERROR, storeConfig.getMetadataTableName(),
						": empty bytes builder");
				return;
			}

			if (monitor.getMetadataStream() == null) {
				monitor.clearMetadataInStream();
				monitor.setMetadataStream(archiveController.openStream(storeConfig.getMetadataTableName()));
				if (monitor.getMetadataStream() == null) {
					// Unable to write
					return;
				}
			}

			try {
				monitor.incMetadataInStream();
				monitor.incMetadataProcessed();
				channelMetadata.write(monitor.getMetadataStream().getTwo());
			} catch (final IOException ioe) {
				trace.error(IDbSqlArchiveController.WRITE_ERROR, storeConfig.getMetadataTableName(), ": ",
						ioe.getCause());
			}

			/** MPCS-7714 Added check */
			checkStreamCount(monitor.getValuesInStream(), storeConfig.getValueTableName(),
					monitor.getMetadataInStream(), storeConfig.getMetadataTableName());
		}
	}

	/**
	 * @param ie
	 * @param commandMessage
	 * @param commandStatus
	 */
	protected void writeCommandMessageToStream(final BytesBuilder commandMessage, final BytesBuilder commandStatus) {
		final StoreIdentifier si = this.monitor.getSi();
		synchronized (monitor.getSyncMonitor()) {
			if (!monitor.isActive()) {
				trace.warn("Ignoring ", si, "(s) sent while inactive");
				return;
			}

			boolean wroteCommand = false;
			if ((commandMessage != null) && (!commandMessage.isEmpty())) {
				if (monitor.getValueStream() == null) {
					monitor.clearValuesInStream();
					monitor.setValueStream(archiveController.openStream(storeConfig.getValueTableName()));

					if (monitor.getValueStream() == null) {
						// Unable to write
						return;
					}
				}

				try {
					monitor.incValuesInStream();
					monitor.incValuesProcessed();
					commandMessage.write(monitor.getValueStream().getTwo());
					wroteCommand = true;
				} catch (final IOException ioe) {
					trace.error(IDbSqlArchiveController.WRITE_ERROR, storeConfig.getValueTableName(), ": ",
							ioe.getCause());
				}
			}

			if ((commandStatus == null) || (commandStatus.isEmpty())) {
				if (wroteCommand) {
					checkStreamCount(monitor.getValuesInStream(), storeConfig.getValueTableName(), 0L,
							storeConfig.getMetadataTableName());
				}
				return;
			}

			if (monitor.getMetadataStream() == null) {
				monitor.clearMetadataInStream();
				monitor.setMetadataStream(archiveController.openStream(storeConfig.getMetadataTableName()));
				if (monitor.getMetadataStream() == null) {
					// Unable to write
					return;
				}
			}

			try {
				monitor.incMetadataInStream();
				monitor.incMetadataProcessed();
				commandStatus.write(monitor.getMetadataStream().getTwo());
			} catch (final IOException ioe) {
				trace.error(IDbSqlArchiveController.WRITE_ERROR, storeConfig.getMetadataTableName(), ": ",
						ioe.getCause());
			}

			/** MPCS-7714 Added check */
			checkStreamCount(monitor.getValuesInStream(), storeConfig.getValueTableName(),
					monitor.getMetadataInStream(), storeConfig.getMetadataTableName());
		}
	}

	/**
	 * @param ie
	 * @param bodyOrValue
	 * @param metadata
	 */
	protected void writePacketOrFrameToStream(final BytesBuilder metadata, final BytesBuilder body) {
		final StoreIdentifier si = this.monitor.getSi();
		synchronized (monitor.getSyncMonitor()) {
			if (!monitor.isActive()) {
				trace.warn("Ignoring ", si, "(s) sent while inactive");
				return;
			}

			if (monitor.getMetadataStream() == null) {
				monitor.clearMetadataInStream();
				monitor.setMetadataStream(archiveController.openStream(storeConfig.getMetadataTableName()));
				if (monitor.getMetadataStream() == null) {
					// Unable to write
					return;
				}
			}

			try {
				monitor.incMetadataInStream();
				monitor.incMetadataProcessed();
				metadata.write(monitor.getMetadataStream().getTwo());
			} catch (final IOException ioe) {
				trace.error(IDbSqlArchiveController.WRITE_ERROR, storeConfig.getMetadataTableName(), ": ",
						ioe.getCause());
			}

			if (monitor.getValueStream() == null) {
				monitor.clearValuesInStream();
				monitor.setValueStream(archiveController.openStream(storeConfig.getValueTableName()));

				if (monitor.getValueStream() == null) {
					// Unable to write
					return;
				}
			}

			try {
				monitor.incValuesInStream();
				monitor.incValuesProcessed();
				body.write(monitor.getValueStream().getTwo());
			} catch (final IOException ioe) {
				trace.error(IDbSqlArchiveController.WRITE_ERROR, storeConfig.getValueTableName(), ": ", ioe.getCause());
			}

			/** MPCS-7714 Added check */
			checkStreamCount(monitor.getValuesInStream(), storeConfig.getValueTableName(),
					monitor.getMetadataInStream(), storeConfig.getMetadataTableName());
		}
	}

	/**
	 * @param ie
	 * @param evrValue
	 * @param evrMetadata
	 */
	protected void writeEvrToStream(final BytesBuilder evrValue, final BytesBuilder[] evrMetadata) {
		final StoreIdentifier si = this.monitor.getSi();
		synchronized (this.monitor.getSyncMonitor()) {
			if (!this.monitor.isActive()) {
				trace.warn("Ignoring ", si, "(s) sent while inactive");
				return;
			}

			if (this.monitor.getValueStream() == null) {
				this.monitor.clearValuesInStream();
				this.monitor.setValueStream(archiveController.openStream(storeConfig.getValueTableName()));

				if (this.monitor.getValueStream() == null) {
					// Unable to write
					return;
				}
			}

			try {
				this.monitor.incValuesInStream();
				this.monitor.incValuesProcessed();
				evrValue.write(this.monitor.getValueStream().getTwo());
			} catch (final IOException ioe) {
				trace.error(IDbSqlArchiveController.WRITE_ERROR, storeConfig.getValueTableName(), ": ", ioe.getCause());
				return;
			}

			if ((evrMetadata == null) || (evrMetadata.length == 0)) {
				/** MPCS-7714 Added check */
				checkStreamCount(this.monitor.getValuesInStream(), storeConfig.getValueTableName(), 0L,
						storeConfig.getMetadataTableName());
				return;
			}

			if (this.monitor.getMetadataStream() == null) {
				this.monitor.clearMetadataInStream();
				this.monitor.setMetadataStream(archiveController.openStream(storeConfig.getMetadataTableName()));
				if (this.monitor.getMetadataStream() == null) {
					// Unable to write
					return;
				}
			}

			final FileOutputStream fos = this.monitor.getMetadataStream().getTwo();
			boolean crossed = false;

			for (final BytesBuilder nextMetadata : evrMetadata) {
				try {
					this.monitor.incMetadataInStream();
					this.monitor.incMetadataProcessed();
					if (this.monitor.getMetadataInStream() == archiveController.getLdiRowExceed()) {
						crossed = true;
					}
					nextMetadata.write(fos);
				} catch (final IOException ioe) {
					trace.error(IDbSqlArchiveController.WRITE_ERROR, storeConfig.getMetadataTableName(), ": ",
							ioe.getCause());
				}
			}

			/** MPCS-7714 Added check */
			checkStreamCount(this.monitor.getValuesInStream(), storeConfig.getValueTableName(),
					crossed ? archiveController.getLdiRowExceed() : 0L, storeConfig.getMetadataTableName());
		}
	}

    /**
     * @param ie
     * @param caValue
     * @param caMetadata
     */
    protected void writeChannelAggregateToStream(final BytesBuilder   caValue,
                                                 final BytesBuilder[] caMetadata,
                                                 final int aggRecCount) {
        final StoreIdentifier si = this.monitor.getSi();
        synchronized (this.monitor.getSyncMonitor()) {
            if (!this.monitor.isActive()) {
                trace.warn("Ignoring " + si + "(s) sent while inactive");
                return;
            }

            if (this.monitor.getValueStream() == null) {
                this.monitor.clearValuesInStream();
                ((IAggregateStoreMonitor)this.monitor).clearInProgressRecordCount();
                this.monitor.setValueStream(archiveController.openStream(storeConfig.getValueTableName()));

                if (this.monitor.getValueStream() == null) {
                    // Unable to write
                    return;
                }
            }

            try {
                ((IAggregateStoreMonitor)this.monitor).incInProgressRecordCount(aggRecCount);
                
                this.monitor.incValuesInStream();
                this.monitor.incValuesProcessed();
                caValue.write(this.monitor.getValueStream().getTwo());
            }
            catch (final IOException ioe) {
                trace.error(IDbSqlArchiveController.WRITE_ERROR + storeConfig.getValueTableName() + ": ",
                        ioe.getCause());
                return;
            }

            if ((caMetadata == null) || (caMetadata.length == 0)) {
                /** MPCS-7714 Added check */
                checkStreamCount(this.monitor.getValuesInStream(), storeConfig.getValueTableName(), 0L, storeConfig.getMetadataTableName());
                return;
            }

            if (this.monitor.getMetadataStream() == null) {
                this.monitor.clearMetadataInStream();
                this.monitor.setMetadataStream(archiveController.openStream(storeConfig.getMetadataTableName()));
                if (this.monitor.getMetadataStream() == null) {
                    // Unable to write
                    return;
                }
            }

            final FileOutputStream fos = this.monitor.getMetadataStream().getTwo();
            boolean crossed = false;

            for (final BytesBuilder nextMetadata : caMetadata) {
                try {
                    this.monitor.incMetadataInStream();
                    this.monitor.incMetadataProcessed();
                    if (this.monitor.getMetadataInStream() == archiveController.getLdiRowExceed()) {
                        crossed = true;
                    }
                    nextMetadata.write(fos);
                }
                catch (final IOException ioe) {
                    trace.error(
                            IDbSqlArchiveController.WRITE_ERROR + storeConfig.getMetadataTableName() + ": ",
                            ioe.getCause());
                }
            }

            /** MPCS-7714 Added check */
            checkStreamCount(this.monitor.getValuesInStream(), storeConfig.getValueTableName(), crossed ? archiveController.getLdiRowExceed() : 0L, storeConfig.getMetadataTableName());
        }
    }	
	
	/**
	 * @param ie
	 * @param log
	 */
	protected void writeLogMessageToStream(final BytesBuilder log) {
		final StoreIdentifier si = this.monitor.getSi();
		synchronized (monitor.getSyncMonitor()) {
			if (!monitor.isActive()) {
				trace.debug("Ignoring " + si + "(s) sent while inactive");
				return;
			}

			if (monitor.getValueStream() == null) {
				monitor.clearValuesInStream();
				monitor.setValueStream(archiveController.openStream(storeConfig.getValueTableName()));

				if (monitor.getValueStream() == null) {
					// Unable to write
					return;
				}
			}

			try {
				monitor.incValuesInStream();
				monitor.incValuesProcessed();
				log.write(monitor.getValueStream().getTwo());
				/** MPCS-7714 Added check */
				checkStreamCount(monitor.getValuesInStream(), storeConfig.getValueTableName(), 0L,
						storeConfig.getMetadataTableName());
			} catch (final IOException ioe) {
				trace.error(IDbSqlArchiveController.WRITE_ERROR, storeConfig.getValueTableName(), ": ", ioe);
			}
		}
	}

	/**
	 * @param cfdpData
	 */
	protected void writeCfdpDataToStream(final BytesBuilder cfdpData) {
		final StoreIdentifier si = this.monitor.getSi();
		synchronized (monitor.getSyncMonitor()) {
			if (!monitor.isActive()) {
				trace.debug("Ignoring " + si + "(s) sent while inactive");
				return;
			}

			if (monitor.getValueStream() == null) {
				monitor.clearValuesInStream();
				monitor.setValueStream(archiveController.openStream(storeConfig.getValueTableName()));

				if (monitor.getValueStream() == null) {
					// Unable to write
					return;
				}
			}

			try {
				monitor.incValuesInStream();
				monitor.incValuesProcessed();
				cfdpData.write(monitor.getValueStream().getTwo());
				checkStreamCount(monitor.getValuesInStream(), storeConfig.getValueTableName(), 0L,
						storeConfig.getMetadataTableName());
			} catch (final IOException ioe) {
				trace.error(IDbSqlArchiveController.WRITE_ERROR, storeConfig.getValueTableName(), ": ", ioe);
			}
		}
	}

	/**
	 * Interrupt the gatherer if the streams are large enough. Do interrupt once for
	 * either or both.
	 *
	 * @param streamCount1
	 *            Stream row size
	 * @param what1
	 *            Table type
	 * @param streamCount2
	 *            Stream row size
	 * @param what2
	 *            Table type
	 *
	 * @version MPCS-7714
	 */
	protected void checkStreamCount(final long streamCount1, final String what1, final long streamCount2,
			final String what2) {
		boolean doInterrupt = false;

		if (streamCount1 == archiveController.getLdiRowExceed()) {
			doInterrupt = true;

			gathererTracer.trace("Gatherer interrupted for ", what1, " at ", archiveController.getLdiRowExceed());

		}

		if (streamCount2 == archiveController.getLdiRowExceed()) {
			doInterrupt = true;

			gathererTracer.trace("Gatherer interrupted for ", what2, " at ", archiveController.getLdiRowExceed());

		}

		if (doInterrupt) {
			archiveController.stopGatherer();
		}
	}

	/**
	 * Start the inserter thread for this store.
	 *
	 * @param ie
	 *            Enum of desired inserter
	 */
	protected void startInserter() {
		final IInserter inserter = this.monitor.getInserter();
		if (!inserter.isStarted()) {
			inserter.startInserter();
		}

	}

	/**
	 * Interrupt the inserter thread for this store.
	 *
	 * @param ie
	 *            Enum of desired inserter
	 */
	protected void interruptInserter() {
		final IInserter inserter = this.monitor.getInserter();
		if (inserter.isStarted()) {
			inserter.interruptInserter();
		}
	}

	protected void insertValOrNull(final BytesBuilder _bb, final Long val){
		if(val != null){
			_bb.insert(val);
		}
		else{
			_bb.insertNULL();
		}
	}

	protected void insertValOrNull(final BytesBuilder _bb, final Integer val){
		if(val != null){
			_bb.insert(val);
		}
		else{
			_bb.insertNULL();
		}
	}

}
