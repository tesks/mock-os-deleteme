/*
 * Copyright 2006-2021. California Institute of Technology.
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
package jpl.gds.globallad.io.socket;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

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
import jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory;
import jpl.gds.globallad.disruptor.ByteBufferEvent;
import jpl.gds.globallad.disruptor.ByteBufferEventFactory;
import jpl.gds.globallad.disruptor.ByteBufferEventProducerWithTranslator;
import jpl.gds.globallad.disruptor.IDisruptorProducer;
import jpl.gds.globallad.io.GlobalLadDataMessageConstructor;
import jpl.gds.shared.log.Tracer;

/**
 * Reads data from the client socket and passes the raw data to a data message constructor via a 
 * LMAX disruptor.
 */
public class GlobalLadSocketClient extends Thread implements IGlobalLadJsonable {
	private static final Tracer log = GlobalLadProperties.getTracer();
	
	private final InputStream clientInputStream;
	private final Disruptor<ByteBufferEvent> disruptor;
	
	/**
	 * Need to hold this if it is a socket because we need to be able to close it.
	 */
	private Socket clientSocket;
	
	private final AtomicBoolean shutdown;
	
	/** Number of consecutive errors before shutting down. **/
	private final int maxErrors;
	
	/** The size of the buffer used to read.  Each read will be at most bufferSize bytes long. **/ 
	private final int bufferSize;
	
	/** 
	 * The handler responsible for consuming the raw byte[] from 
	 * the raw input.
	 */
	private final GlobalLadDataMessageConstructor dataConstructor;
	
	/** Stats objects */
	private long totalBytesRead;
	private long numberReads;
	private final long totalOfferTimeNS;
	private long readTime;
	private long emptyRead;
	private long runTimeMS;
	
	/**
	 * @param clientSocket open socket to read data
	 * @param name thread name
	 * @param ringBufferSize size of the disruptor ring buffer.  Must be a power of 2.
	 * @param maxErrors max number errors before shutting down this client
	 * @param bufferSize read buffer size
	 * @param factory global lad data factory passed to the data message constructor to create data objects
	 * @param executor executor to handle the inserters into the disruptor
	 * @param dataProducer global lad data producer to publish the raw data to the disruptor
	 * @throws IOException
	 */
	public GlobalLadSocketClient(final Socket clientSocket, 
			final String name, 
			final int ringBufferSize,
			final int maxErrors, 
			final int bufferSize, 
			final IGlobalLadDataFactory factory, 
			final ExecutorService executor,
			final IDisruptorProducer<IGlobalLADData> dataProducer) throws IOException {
		this(clientSocket.getInputStream(), name, ringBufferSize, maxErrors, bufferSize, factory, executor, dataProducer);
		this.clientSocket = clientSocket;
	}

	
	/**
	 * @param clientInputStream stream to read data from
	 * @param name thread name
	 * @param ringBufferSize size of the disruptor ring buffer.  Must be a power of 2.
	 * @param maxErrors max number errors before shutting down this client
	 * @param bufferSize read buffer size
	 * @param factory global lad data factory passed to the data message constructor to create data objects
	 * @param executor executor to handle the inserters into the disruptor
	 * @param dataProducer global lad data producer to publish the raw data to the disruptor
	 * 	 
	 */
	@SuppressWarnings("unchecked")
	public GlobalLadSocketClient(final InputStream clientInputStream, 
			final String name, 
			final int ringBufferSize,
			final int maxErrors, 
			final int bufferSize, 
			final IGlobalLadDataFactory factory,
			final ExecutorService executor,
			final IDisruptorProducer<IGlobalLADData> dataProducer) {
		super(name);
		clientSocket = null;
		this.clientInputStream = clientInputStream;
		
		this.maxErrors = maxErrors;
		this.bufferSize = bufferSize;
		this.shutdown = new AtomicBoolean(false);
		
		this.dataConstructor = new GlobalLadDataMessageConstructor(factory, dataProducer);

		/**
		 * Set up the disruptor that will be used instead of a queue.  
		 * MPCS-7926 - triviski 2/4/2016 - Creating the wait strategy here so the 
		 * global lad configuration does not need to depend on the disruptor.
		 */
		WaitStrategy clientWaitStrategy;
		
		switch(GlobalLadProperties.getGlobalInstance().getClientWaitStrategy()) {
		case SLEEP:
			clientWaitStrategy = new SleepingWaitStrategy();
			break;
		case SPIN:
			clientWaitStrategy = new BusySpinWaitStrategy();
			break;
		case YIELD:
			clientWaitStrategy = new YieldingWaitStrategy();
			break;
		case BLOCK:
		default:
			clientWaitStrategy = new BlockingWaitStrategy();
			break;
		}
		
		this.disruptor = new Disruptor<ByteBufferEvent>(
				new ByteBufferEventFactory(), 
				ringBufferSize, 
				executor,
				ProducerType.SINGLE, 
				clientWaitStrategy);
		
		this.disruptor.handleEventsWith(this.dataConstructor);
		
		this.totalBytesRead = 0;
		this.numberReads = 0;
		this.totalOfferTimeNS = 0;
		this.readTime = 0;
		this.emptyRead = 0;
	}

	/**
	 * Sets the shutdown flag.
	 */
	public void shutDown() {
		shutdown.set(true);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		int numErrors = 0;
		runTimeMS = System.currentTimeMillis();
		
		/**
		 * Get the event producer for the disruptor. 
		 */
		final IDisruptorProducer<ByteBuffer> producer = new ByteBufferEventProducerWithTranslator(disruptor.getRingBuffer());
		disruptor.start();
		
		try {
			do {
				try {
					while (! shutdown.get()) {
						final byte[] buffer = new byte[bufferSize];
						
						final long st = System.nanoTime();
						final int bytesRead = clientInputStream.read(buffer);
						this.readTime += System.nanoTime()-st;
						
						if (bytesRead < 0) {
							// EOF, shutdown.
							shutDown();
							break;
						} else if (bytesRead == 0) {
							/**
							 *  Nothing was read, do nothing.  This should never happen unless
							 *  we use a non-blocking input.  Just covering all cases.
							 */
							this.emptyRead++;
						} else {
							final ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);
							producer.onData(byteBuffer);
							numberReads++;
							totalBytesRead += bytesRead;
						}
					}
				} catch (final IOException e) {
                    log.error(this.getName() + " encountered exception" + e.getMessage(), e.getCause());
					numErrors++;
				}
				
				if (numErrors >= maxErrors) {
					throw new Exception("Global Lad Client for Port %d exceeded the maximum number of errors and is shutting down.");
				}
			} while (numErrors < maxErrors && ! shutdown.get());
		} catch (final Exception e) {
			log.error(getName() + " is shutting down due to unknown exception" + e.getMessage(), e.getCause());
		} finally {
			disruptor.shutdown();
			if (clientSocket != null) {
				try {
					clientSocket.close();
				} catch (final IOException e) {
                    log.error(getName() + " cannot be closed due to IOException " + e.getMessage(), e.getCause());
                    e.printStackTrace();
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.IGlobalLadJsonable#getStats()
	 */
	@Override
	public JsonObject getStats() {
		final int size = disruptor.getRingBuffer().getBufferSize();
		final long remaining = disruptor.getRingBuffer().remainingCapacity();
		
		return Json.createObjectBuilder()
				.add("ringBufferSize", size)
				.add("remaining", remaining)
				.add("backlog", size - remaining)
				.add("reads", numberReads)
				.add("runTimeMS", System.currentTimeMillis()-runTimeMS)
				.add("bytesRead", totalBytesRead)
				.add("readTimeNS", readTime)
				.add("emptyReads", emptyRead)
				.add("readBufferSize", bufferSize)
				.add("offerTimeNS", totalOfferTimeNS)
				.add("dataConstructor", dataConstructor.getStats())
				.build();
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.IGlobalLadJsonable#getMetadata(jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm)
	 */
	@Override
	public JsonObject getMetadata(final IGlobalLadContainerSearchAlgorithm matcher) {
		return getStats();
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.IGlobalLadJsonable#getJsonId()
	 */
	@Override
	public String getJsonId() {
		return getName();
	}
}
