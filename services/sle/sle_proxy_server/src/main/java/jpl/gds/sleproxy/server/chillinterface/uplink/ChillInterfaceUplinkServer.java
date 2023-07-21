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
package jpl.gds.sleproxy.server.chillinterface.uplink;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.bouncycastle.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jpl.gds.sleproxy.server.chillinterface.config.ChillInterfaceConfigManager;
import jpl.gds.sleproxy.server.chillinterface.internal.config.ChillInterfaceInternalConfigManager;
import jpl.gds.sleproxy.server.sleinterface.fwd.SLEInterfaceForwardService;
import jpl.gds.sleproxy.server.websocket.MessageDistributor;

/**
 * This server runs in a separate thread, handling the actual socket-level
 * connection and raw data transferring with an AMPCS telecommanding application
 * that connects. When no application is connected, the server blocks listening
 * to a socket, waiting for a new application to connect.
 * 
 */
public class ChillInterfaceUplinkServer implements Runnable {

	/*
	 * MPCS-8702: Need to make a reference to the server socket accessible by
	 * another thread, so that when the server thread is interrupted, the server
	 * socket is closed and the server promptly goes away.
	 */
	/**
	 * Current server socket being used to listen for an incoming connection.
	 */
	private volatile ServerSocket currentServerSocket;

	/**
	 * CLTU queue to push incoming CLTU data to.
	 */
	private volatile BlockingQueue<byte[]> cltuQueue;

	/**
	 * Lock object for the CLTU queue.
	 */
	private volatile Object cltuQueueLock;

	/**
	 * Buffer for receiving raw data (CLTU data).
	 */
	private final ByteBuffer totalBuffer;

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ChillInterfaceUplinkServer.class);

	/**
	 * Default constructor.
	 */
	public ChillInterfaceUplinkServer() {
		cltuQueueLock = new Object();
		totalBuffer = ByteBuffer
				.allocateDirect(ChillInterfaceInternalConfigManager.INSTANCE.getUplinkTotalBufferSize());
		logger.debug("Constructor: totalBuffer (ByteBuffer) allocated with capacity {}", totalBuffer.capacity());
	}

	/**
	 * Set the server's CLTU queue with the provided one.
	 * 
	 * @param cltuQueue
	 *            The queue that the server should use to relay CLTUs
	 */
	public final void setCLTUQueue(final BlockingQueue<byte[]> cltuQueue) {

		synchronized (cltuQueueLock) {
			this.cltuQueue = cltuQueue;
			logger.debug("setCLTUQueue(cltuQueue: {})", Integer.toHexString(System.identityHashCode(cltuQueue)));
		}

	}

	/**
	 * If server socket (listening socket) is open, close it, ingesting the
	 * resulting exception.
	 */
	public final void stopListening() {

		if (currentServerSocket != null && !currentServerSocket.isClosed()) {
			logger.debug("stopListening(): Closing server socket");

			try {
				currentServerSocket.close();
			} catch (IOException e) {
				logger.debug("stopListening(): Exception from currentServerSocket.close() (expected)", e);
			}

		} else {
			logger.trace("stopListening(): currentServerSocket is either null ({}) or already closed, so doing nothing",
					currentServerSocket == null);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public final void run() {
		logger.debug("Entered run()");
		logger.debug("run(): Start listening on port {}.",
				ChillInterfaceConfigManager.INSTANCE.getUplinkListeningPort());

		try (ServerSocket serverSocket = new ServerSocket(
				ChillInterfaceConfigManager.INSTANCE.getUplinkListeningPort())) {
			/*
			 * MPCS-8702: Copy a reference to the server socket so that it may
			 * be closed by another thread.
			 */
			currentServerSocket = serverSocket;

			/*
			 * Split the try-with-resources block into three: Outer block for
			 * ServerSocket, middle block for (client) Socket, and inner block
			 * for DataInputStream. Reason for this is that ServerSocket and
			 * (client) Socket need to be made accessible from another thread in
			 * order to close them at any time.
			 */

			while (!(Thread.currentThread().isInterrupted())) {
				logger.info("{} chill interface uplink server starting listen on local port {}",
						SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
						ChillInterfaceConfigManager.INSTANCE.getUplinkListeningPort());

				try (Socket clientSocket = serverSocket.accept()) {
					String clientHostName = clientSocket.getInetAddress().getHostName();
					logger.info("{} Client connected from host {}",
							SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
							clientHostName);
					ChillInterfaceUplinkManager.INSTANCE.setConnectedHost(clientHostName);

					try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {
						logger.debug("run(): Obtained DataInputStream from client socket");
						byte[] readBuffer = new byte[ChillInterfaceInternalConfigManager.INSTANCE
								.getUplinkReadBufferSize()];
						long totalReadCount = 0;
						logger.debug("run(): readBuffer (byte[]) of size {} instantiated. Starting first read.",
								readBuffer.length);
						int readCount = dis.read(readBuffer);
						logger.debug(
								"run(): First read's count is {}. Entering DIS while loop. Loop until read count is -1 or thread is interrupted.",
								readCount);

						while (readCount != -1 && !(Thread.currentThread().isInterrupted())) {

							if (readCount > 0) {
								totalReadCount += readCount;
								logger.trace("run() DIS while loop: Total bytes read is now {}", totalReadCount);

								/*
								 * Append the bytes just read to the total
								 * buffer
								 */
								totalBuffer.put(readBuffer, 0, readCount);
								logger.trace(
										"run() DIS while loop: Appended new bytes to the total buffer. Calling extractCLTUs on it.");

								// See if we can extract CLTUs
								List<byte[]> cltus = extractCLTUs(totalBuffer);
								logger.trace("run() DIS while loop: Extracted {} CLTUs", cltus.size());

								if (cltus.size() > 0) {
									ChillInterfaceUplinkManager.INSTANCE.incrementCLTUsReceivedCount(cltus.size(),
											ZonedDateTime.now(ZoneOffset.UTC));
									/*
									 * Send CLTU received time and count info to
									 * clients
									 */
									MessageDistributor.INSTANCE.chillUpDataFlow(
											ChillInterfaceUplinkManager.INSTANCE
													.getLastCLTUsReceivedTimeSinceLastEnable(),
											ChillInterfaceUplinkManager.INSTANCE
													.getCLTUsReceivedCountSinceLastEnable());
									logger.trace(
											"run() DIS while loop: Adding {} new CLTUs to CLTU queue {}. Current queue size: {}, remaining: {}",
											cltus.size(), Integer.toHexString(System.identityHashCode(cltuQueue)),
											cltuQueue.size(), cltuQueue.remainingCapacity());

									int cltusPlacedInQueueCount = 0;

									synchronized (cltuQueueLock) {

										if (cltuQueue == null) {
											/*
											 * The queue has been taken away, so
											 * drop the CLTUs and interrupt this
											 * thread. The interrupt will result
											 * in exiting the DIS while loop,
											 * closing the client socket,
											 * closing the server socket, and
											 * exiting run(), making the uplink
											 * server go away.
											 */
											logger.error(
													"{} Failed to queue {} of new CLTUs received from client. These CLTUs have been dropped. SLE interface forward service must no longer be ACTIVE.",
													SLEInterfaceForwardService.INSTANCE
															.getCurrentConnectionNumberStringForLogging(),
													cltus.size());
											logger.warn(
													"{} Disconnecting from the chill uplink client and stopping the server because unable to queue additional CLTUs",
													SLEInterfaceForwardService.INSTANCE
															.getCurrentConnectionNumberStringForLogging());
											logger.debug("run() DIS while loop: Interrupting current thread");
											Thread.currentThread().interrupt();
										} else {
											// cltuQueue is still good

											try {
												/*
												 * Putting to the cltuQueue
												 * should block, so that reads
												 * from AMPCS uplink application
												 * is throttled.
												 */
												for (byte[] cltu : cltus) {
													cltuQueue.put(cltu);
													cltusPlacedInQueueCount++;
												}

											} catch (Exception e) {
												logger.error(
														"{} Failed to queue {} of new CLTUs received from client. These CLTUs have been dropped. Exception occurred: {}",
														SLEInterfaceForwardService.INSTANCE
																.getCurrentConnectionNumberStringForLogging(),
														cltus.size() - cltusPlacedInQueueCount,
														e.getClass().getSimpleName());
												logger.warn(
														"{} Disconnecting from the chill uplink client and stopping the server because of aforementioned exception",
														SLEInterfaceForwardService.INSTANCE
																.getCurrentConnectionNumberStringForLogging());
												logger.debug(
														"run() DIS while loop: Here's the exception in full and interrupting current thread",
														e);
												Thread.currentThread().interrupt();
											}

										}

									}

								}

							} else {
								logger.debug("run() DIS while loop: UNEXPECTED! Read bytes is {}", readCount);
							}

							readCount = dis.read(readBuffer);
							logger.trace("run() DIS while loop: Just read {} bytes", readCount);
							// End in the while loop block
						}

						logger.debug("run(): Exiting the DataInputStream try block");
					}

					logger.debug("run(): Exiting the client socket try block");
				} catch (IOException ioe) {
					logger.debug("run(): Caught client socket exception", ioe);
				}

				ChillInterfaceUplinkManager.INSTANCE.setConnectedHost(null);
				// End in server listening while loop
			}

			// End in the server socket try-with-resources block
		} catch (IOException ioe) {
			logger.error(
					"{} chill interface uplink server listening socket could not be established. Try disabling and re-enabling the chill uplink interface.",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(), ioe);
			return;
		}
		
		// MPCS-8702: serverSocket has been released by this point.
		currentServerSocket = null;

		logger.info("{} chill interface uplink server listening socket closed. Server is now going away.",
				SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging());
		logger.debug("Exiting run()");
	}

	/**
	 * This method requires that the byteBuffer carries data that starts with
	 * either the CLTU acqusition/idle sequence or the CLTU start sequence.
	 * Otherwise, the CLTU extraction will not work correctly.
	 * 
	 * @param byteBuffer
	 *            Buffer to scan for CLTU extraction opportunity
	 * @return List of CLTUs (in raw form) that have been extracted
	 */
	private List<byte[]> extractCLTUs(final ByteBuffer byteBuffer) {
		logger.trace("Entered extractCLTUs(byteBuffer position: {}, remaining:{})", byteBuffer.position(),
				byteBuffer.remaining());
		List<byte[]> cltus = new ArrayList<>();

		/*
		 * byteBuffer's 'position' should currently be at the next writing
		 * position, which means that the offset range 0 to 'position' carries
		 * data we're interested in. Create a byte array that will extract this
		 * data.
		 */
		byte[] workingCacheForReading = new byte[byteBuffer.position()];

		/*
		 * The flip will change the position to 0 and 'limit' set to the
		 * original 'position', ripe for our reading
		 */
		byteBuffer.flip();

		/*
		 * Extract the data from byteBuffer, storing it in
		 * workingCacheForReading
		 */
		byteBuffer.get(workingCacheForReading);

		int cltuScanPosition = 0;
		int cltuFirstStartSequenceIndex = -1;
		int cltuFirstTailSequenceIndex = -1;
		int indexAfterLastExtractedCLTU = 0;

		// Scan for the CLTU start sequence
		do {
			cltuFirstStartSequenceIndex = getFirstCLTUStartSequenceIndex(workingCacheForReading, cltuScanPosition);
			logger.trace("extractCLTUs() do loop: cltuFirstStartSequenceIndex: {}", cltuFirstStartSequenceIndex);

			if (cltuFirstStartSequenceIndex >= 0) {
				cltuScanPosition = cltuFirstStartSequenceIndex
						+ ChillInterfaceInternalConfigManager.INSTANCE.getCLTUStartSequence().length;

				if (cltuScanPosition >= workingCacheForReading.length) {
					// Passed the boundary
					logger.trace(
							"extractCLTUs() do loop: Exiting loop because remaining bytes not enough for full CLTU scan");
					break;
				} else {
					cltuFirstTailSequenceIndex = getFirstCLTUTailSequenceIndex(workingCacheForReading,
							cltuScanPosition);
					logger.trace("extractCLTUs() do loop: cltuFirstTailSequenceIndex: {}", cltuFirstTailSequenceIndex);

					if (cltuFirstTailSequenceIndex >= 0) {
						// Found a complete CLTU in the buffer. Extract it.
						logger.trace("extractCLTUs() do loop: Found a complete CLTU in the buffer so extracting it");
						int cltuLength = cltuFirstTailSequenceIndex
								+ ChillInterfaceInternalConfigManager.INSTANCE.getCLTUTailSequence().length
								- cltuFirstStartSequenceIndex;
						byte[] cltu = Arrays.copyOfRange(workingCacheForReading, cltuFirstStartSequenceIndex,
								cltuFirstStartSequenceIndex + cltuLength);
						cltus.add(cltu);
						cltuScanPosition = cltuFirstTailSequenceIndex
								+ ChillInterfaceInternalConfigManager.INSTANCE.getCLTUTailSequence().length;
						indexAfterLastExtractedCLTU = cltuScanPosition;
					}

				}

			}

			logger.trace(
					"extractCLTUs() do loop: {} bytes in cache, current scan position is {}, first CLTU start sequence index is {}, first CLTU tail sequence index is {}",
					workingCacheForReading.length, cltuScanPosition, cltuFirstStartSequenceIndex,
					cltuFirstTailSequenceIndex);
		} while (cltuScanPosition < workingCacheForReading.length && cltuFirstStartSequenceIndex >= 0
				&& cltuFirstTailSequenceIndex >= 0);

		logger.trace("extractCLTUs(): Exited do loop. Putting the remaining {} bytes in cache back into the buffer",
				workingCacheForReading.length - indexAfterLastExtractedCLTU);

		// Put the remaining bytes back into the buffer
		byteBuffer.clear();
		byteBuffer.put(workingCacheForReading, indexAfterLastExtractedCLTU,
				workingCacheForReading.length - indexAfterLastExtractedCLTU);
		logger.trace("Exiting extractCLTUs() with {} CLTUs", cltus.size());
		return cltus;
	}

	/**
	 * Scan the buffer to find the start of a CLTU. If more that one is found,
	 * only the offset for the first one is returned. So to locate all of them,
	 * this method will need to be called repeatedly.
	 * 
	 * @param buffer
	 *            Buffer to scan for a CLTU start sequence
	 * @param from
	 *            Offset to start scanning from
	 * @return If a CLTU start sequence is found, its starting offset. If not
	 *         found, -1.
	 */
	private int getFirstCLTUStartSequenceIndex(final byte[] buffer, final int from) {
		logger.trace("Entered getFirstCLTUStartSequenceIndex(buffer size is {}, index to start search from is {})",
				buffer.length, from);
		byte[] cltuStartSequence = ChillInterfaceInternalConfigManager.INSTANCE.getCLTUStartSequence();
		byte cltuAcquisitionOrIdleSequenceByte = ChillInterfaceInternalConfigManager.INSTANCE
				.getCLTUAcquisitionOrIdleSequenceByte();
		boolean foundCLTUStartSequence = false;
		int cltuStartSequenceIndex = -1;
		boolean previousBytesAllAcquisitionOrIdleSequence = true;

		for (int currentSearchIndex = from, cltuStartSequenceIndexToMatch = 0; !foundCLTUStartSequence
				&& previousBytesAllAcquisitionOrIdleSequence
				&& currentSearchIndex < buffer.length; currentSearchIndex++) {

			if (buffer[currentSearchIndex] == cltuStartSequence[cltuStartSequenceIndexToMatch]) {
				// We have a match, so increment the index for the next match
				cltuStartSequenceIndexToMatch++;

				// Mark this index if it's the first byte that matched
				if (cltuStartSequenceIndex < 0) {
					cltuStartSequenceIndex = currentSearchIndex;
				}

			} else {
				// No match. Reset.
				cltuStartSequenceIndexToMatch = 0;
				cltuStartSequenceIndex = -1;

				/*
				 * If we haven't found the start sequence yet, the bytes under
				 * examination should be acqusition/idle bytes
				 */
				if (buffer[currentSearchIndex] != cltuAcquisitionOrIdleSequenceByte) {
					previousBytesAllAcquisitionOrIdleSequence = false;
				}

			}

			if (cltuStartSequenceIndexToMatch == cltuStartSequence.length) {
				// We have a complete match
				foundCLTUStartSequence = true;
			}

		}

		if (foundCLTUStartSequence) {
			logger.trace("Exiting getFirstCLTUStartSequenceIndex() with ", cltuStartSequenceIndex);
			return cltuStartSequenceIndex;
		} else {
			logger.trace("Exiting getFirstCLTUStartSequenceIndex() with -1");
			return -1;
		}

	}

	/**
	 * Scan the buffer to find the tail of a CLTU. If more that one is found,
	 * only the offset for the first one is returned. So to locate all of them,
	 * this method will need to be called repeatedly.
	 * 
	 * @param buffer
	 *            Buffer to scan for a CLTU tail sequence
	 * @param from
	 *            Offset to start scanning from
	 * @return If a CLTU tail sequence is found, its starting offset. If not
	 *         found, -1.
	 */
	private int getFirstCLTUTailSequenceIndex(final byte[] buffer, final int from) {
		logger.trace("Entered getFirstCLTUTailSequenceIndex(buffer size is {}, index to start search from is {})",
				buffer.length, from);
		byte[] cltuTailSequence = ChillInterfaceInternalConfigManager.INSTANCE.getCLTUTailSequence();
		boolean foundCLTUTailSequence = false;
		int cltuTailSequenceIndex = -1;

		for (int currentSearchIndex = from, cltuTailSequenceIndexToMatch = 0; !foundCLTUTailSequence
				&& currentSearchIndex < buffer.length; currentSearchIndex++) {

			if (buffer[currentSearchIndex] == cltuTailSequence[cltuTailSequenceIndexToMatch]) {
				// We have a match, so increment the index for the next match
				cltuTailSequenceIndexToMatch++;

				// Mark this index if it's the first byte that matched
				if (cltuTailSequenceIndex < 0) {
					cltuTailSequenceIndex = currentSearchIndex;
				}

			} else {
				// No match. Reset.
				cltuTailSequenceIndexToMatch = 0;
				cltuTailSequenceIndex = -1;
			}

			if (cltuTailSequenceIndexToMatch == cltuTailSequence.length) {
				// We have a complete match
				foundCLTUTailSequence = true;
			}

		}

		if (foundCLTUTailSequence) {
			logger.trace("Exiting getFirstCLTUTailSequenceIndex() with ", cltuTailSequenceIndex);
			return cltuTailSequenceIndex;
		} else {
			logger.trace("Exiting getFirstCLTUTailSequenceIndex() with -1");
			return -1;
		}

	}

}
