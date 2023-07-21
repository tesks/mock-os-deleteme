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
package jpl.gds.globallad.service.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;

import com.lmax.disruptor.EventHandler;

import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.AbstractGlobalLadData;
import jpl.gds.globallad.disruptor.GlobalLadDataEvent;
import jpl.gds.globallad.service.GlobalLadDownlinkServiceConnectionException;
import jpl.gds.serialization.globallad.data.Proto3GlobalLadTransport;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.thread.SleepUtilities;

/**
 * Event handler for global lad messages to be used in a data producer like the global lad downlink service.
 */
public class GlobalLadMessageEventHandler extends Thread implements EventHandler<GlobalLadDataEvent> {
	private static final Tracer tracer = GlobalLadProperties.getTracer();

	private static final int MAX_SOCKET_RETRY = GlobalLadProperties.getGlobalInstance().getMaxSocketRetryCount();
	private static final int SOCKET_RETRY_DELAY_MILLIS = GlobalLadProperties.getGlobalInstance().getMaxSocketRetryDelyMillis();

    private Socket              globalLadSocket;
    private final int           gladPort;
    private final String        gladHost;
    private int                 numErrors;
    private DataOutputStream    output;
    private boolean             sigtermHandle;

	/**
	 * Store the last sequence.  This is used by the performance publishing to figure out the backlog
	 * in in the disruptor being written to the global lad.  The sequence is just a count of the number of events
	 * the disruptor has handled.  Each event handler will be given the sequence number so we can set this 
	 * value every time an event is processed.
	 */
	public final AtomicLong lastSequence;

	/**
	 * @throws IOException IO error connecting to the global lad.
	 */
	public GlobalLadMessageEventHandler() throws IOException {
		lastSequence = new AtomicLong();

		/**
		 * Moved the glad connection logic to this handler so that reconnection can be attempted.
		 */
		numErrors = 0;
        sigtermHandle = false;
		gladPort = GlobalLadProperties.getGlobalInstance().getSocketServerPort();
		gladHost = GlobalLadProperties.getGlobalInstance().getServerHost();

        // MPCS-9450 3/1/18 - Ctrl+C shutdown hook to stop reconnection attempts
		Runtime.getRuntime().addShutdownHook(this);

		output = connectToGlobalLad();
	}
	
	@Override
	public void onEvent(final GlobalLadDataEvent event, final long sequence, final boolean endOfBatch) throws Exception {
		/**
		 * If there was an exception with translating the data to the event, event.data will be null.  
		 * Check and throw if necessary as to avoid the completely useless NullPointerException.
		 */
		try {
			if (event.data != null) {
				/**
				 * Changing this to be a no-op.  There are a million checks on the data before we get here and
				 * this just adds noise. If the data is null means it is not an event that needs to be handled.
				 */
				
				output.write(event.data.toPacketByteArray());
			}
			
		} catch (final IOException e) {
			/**
			 * Try to reconnect. If this fails this will throw a GlobalLadServiceException
			 */
			reconnect();
			
		} finally {
			if (endOfBatch) {
				output.flush();
			}
			
			lastSequence.set(sequence);
		}
	}
	
	/**
	 * Closes the global lad socket and then attempts to connect to the global lad.
	 * 
	 * @throws GlobalLadDownlinkServiceConnectionException Can not connect to the global lad.
	 */
	private void reconnect() throws GlobalLadDownlinkServiceConnectionException {
		// Attempt to close the socket if it is not null.
		try {
			if (globalLadSocket != null) {
				globalLadSocket.close();
			}
		} catch (final Exception e) {
			// Don't care
		}
		
		/**
		 * Reconnect to the socket by calling the connect method. 
		 */
		try {
			this.output = this.connectToGlobalLad();
		} catch (final Exception e) {
			/**
			 * Attempts were made to reconnect but we must shut down now.
			 */
			throw new GlobalLadDownlinkServiceConnectionException(e);
		}

	}
	
	@Override
	public void run() { 
        sigtermHandle = true;
	}
	
	/**
	 * Moving socket connection logic to this handler class so that there
	 * can be some re-connect logic in the event we drop the connection.  If we have an error and it is 
	 * an IOException, we will attempt to reconnect.
	 */

	/**
	 * Connects to the global lad.
	 * 
	 * @throws IOException Fail to connect to the global lad server
	 */
	private DataOutputStream connectToGlobalLad() throws IOException {
		while (null == globalLadSocket || globalLadSocket.isClosed()) {
			try {
				globalLadSocket = new Socket(gladHost, gladPort);
			} catch (final IOException e) {
				numErrors++;
                tracer.info(String.format("Failed to connect to global lad on %s port %d.  Attempt %d of %d: ",
                        gladHost, gladPort, numErrors, MAX_SOCKET_RETRY) + e.getMessage(), e.getCause());
                if (numErrors >= MAX_SOCKET_RETRY || sigtermHandle) {
					throw e;
				}
				
				tracer.info(String.format("Pausing for %d milliseconds before retrying...", SOCKET_RETRY_DELAY_MILLIS));
				if (SleepUtilities.checkedSleep(SOCKET_RETRY_DELAY_MILLIS)) {
					throw e;
				}
				tracer.info("...retrying...");
			} 
		}
		
		return globalLadSocket == null ? null : new DataOutputStream(globalLadSocket.getOutputStream());
	}
}
