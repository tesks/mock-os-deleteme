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
package jpl.gds.sleproxy.server.chillinterface.downlink;

import com.lsespace.sle.user.service.data.TmProductionData;
import com.lsespace.sle.user.util.ByteBlock;
import com.lsespace.sle.user.util.JavaTimeTag;
import jpl.gds.sleproxy.server.chillinterface.config.ChillInterfaceConfigManager;
import jpl.gds.sleproxy.server.chillinterface.downlink.ampcsutil.BitOutputStream;
import jpl.gds.sleproxy.server.chillinterface.downlink.ampcsutil.LeotHead;
import jpl.gds.sleproxy.server.chillinterface.downlink.ampcsutil.SleHead;
import jpl.gds.sleproxy.server.chillinterface.internal.config.ChillInterfaceInternalConfigManager;
import jpl.gds.sleproxy.server.sleinterface.rtn.SLEInterfaceReturnService;
import jpl.gds.sleproxy.server.websocket.MessageDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * ChillInterfaceDownlinkClient is the main interactor with AMPCS's chill_down.
 * It is responsible for making the socket connection to chill_down and
 * forwarding the frame data in <code>frameQueue</code> to chill_down.
 *
 */
public class ChillInterfaceDownlinkClient implements Runnable {

	/**
	 * <code>BlockingQueue</code> that this client object will read from, to
	 * obtain the frame data it needs to forward to the chill_down connection.
	 */
	private final BlockingQueue<TmProductionData> frameQueue;

	/**
	 * Holding variable for the currently open output stream to chill_down.
	 * Wrapper for the data output stream.
	 */
	private BitOutputStream currentBitOutputStream;

	/**
	 * Holding variable for the currently open socket connection to chill_down.
	 */
	private Socket socket;

	/**
	 * Holding variable for the currently open data output stream to chill_down.
	 */
	private DataOutputStream dataOutputStream;

	/**
	 * A flag that will be set when the downlink client should just abandon the
	 * remaining frames in the queue and not attempt to transfer them, i.e. when
	 * the sending pipe has already been broken.
	 */
	private boolean doNotTransferRemainingFrames;

	/**
	 * The logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ChillInterfaceDownlinkClient.class);

	/**
	 * Output frame format
	 */
	private static final String downlinkOutputFormat = ChillInterfaceInternalConfigManager.INSTANCE.getDownlinkOutputFormat();

	/**
	 * Expected output format for downlink - LEOT
	 */
	private final String LEOT = "LEOT";

	/**
	 * Expected output format for downlink - SLE
	 */
	private final String SLE = "SLE";

	/**
	 * Constructor. The <code>BlockingQueue</code> object that will serve to
	 * feed this client with frame data needs to be provided.
	 *
	 * @param frameQueue
	 *            The <code>BlockingQueue</code> that will transfer frame data
	 *            from the SLE interface to this client.
	 */
	public ChillInterfaceDownlinkClient(final BlockingQueue<TmProductionData> frameQueue) {
		logger.trace("Entered constructor. frameQueue: {}", frameQueue);
		this.frameQueue = frameQueue;
		this.doNotTransferRemainingFrames = false;
	}

	/**
	 * Trigger the client to open the connection to a chill_down server.
	 *
	 * @throws UnknownHostException
	 *             Thrown when the configured chill_down host name is not
	 *             reachable/unknown
	 * @throws IOException
	 *             Thrown when an error occurs during the socket onnection
	 *             establishment
	 */
	public final void connect() throws UnknownHostException, IOException {
		logger.debug("Entered connect()");

		// Establish downlink connection to chill_down
		String downlinkHost = ChillInterfaceConfigManager.INSTANCE.getDownlinkHost();
		int downlinkPort = ChillInterfaceConfigManager.INSTANCE.getDownlinkPort();
		logger.debug("Creating new socket to {}:{}", downlinkHost, downlinkPort);
		socket = new Socket(downlinkHost, downlinkPort);
		dataOutputStream = new DataOutputStream(socket.getOutputStream());
		logger.info("{} Connected to chill_down at {}:{}",
				SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(), downlinkHost,
				downlinkPort);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public final void run() {
		logger.debug("Entered run()");

		currentBitOutputStream = new BitOutputStream(dataOutputStream);

		while (socket != null && !socket.isClosed() && !(Thread.currentThread().isInterrupted())) {

			try {
				logger.trace(
						"run()'s while loop (means socket is open and thread is not interrupted): Executing frameQueue.take()");
				TmProductionData frame = frameQueue.take();

				if (frame != null) {
					transfer(frame);
				}

			} catch (InterruptedException ie) {
				logger.debug("run()'s while loop: Caught InterruptedException. Raising interrupt again.", ie);
				/*
				 * Raise an interrupt again. Reason here:
				 * http://stackoverflow.com/questions/4906799/why-invoke-thread-
				 * currentthread-interrupt-when-catch-any-interruptexception
				 */
				Thread.currentThread().interrupt();
			}

		}

		/*
		 * Thread is interrupted so we'll be exiting run(). But handle all
		 * remaining frames in the queue before we go. But if socket is already
		 * closed, skip all of this.
		 */
		final List<TmProductionData> remainingFrames = new LinkedList<>();
		frameQueue.drainTo(remainingFrames);
		logger.debug("run(): Drained remaining {} frames in queue", remainingFrames.size());

		if (socket != null && !socket.isClosed()) {

			if (doNotTransferRemainingFrames) {
				logger.warn(
						"{} chill interface downlink client is now stopping. Abandoning {} frames left in the queue because connection to chill_down seems to have been lost",
						SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
						remainingFrames.size());

			} else {
				logger.info(
						"{} chill interface downlink client is now stopping. Draining the frames queue and transferring the remaining {} frames.",
						SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
						remainingFrames.size());

				int remainingFramesTransferred = 0;

				for (TmProductionData frame : remainingFrames) {
					transfer(frame);
					remainingFramesTransferred++;

					if (doNotTransferRemainingFrames) {
						logger.warn("{} Frame transfer is failing. Abandoning {} frames left in the queue.",
								SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
								remainingFrames.size() - remainingFramesTransferred);
						break;
					}

				}

			}

			if (dataOutputStream != null) {
				logger.trace("run(): Finished transferring so flushing and closing DataOutputStream");

				try {
					dataOutputStream.flush();
					dataOutputStream.close();
				} catch (IOException ie) {
					logger.error("{} DataOutputStream flush or close caused exception",
							SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(), ie);
				}

			} else {
				logger.debug("run(): Just finished transferring but DataOutputStream is already null!");
			}

			if (!socket.isClosed()) {
				logger.debug("run(): Closing socket");

				try {
					socket.close();
				} catch (IOException ie) {
					logger.error("{} Socket close caused exception",
							SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(), ie);
				}

			}

		} else {
			logger.warn(
					"{} Unable to transfer {} frames that were remaining in the queue because the socket is already closed. Must abandon them.",
					SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
					remainingFrames.size());
		}

		logger.info("{} chill interface downlink has ended",
				SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging());
		logger.debug("Exiting run()");
	}

	/**
	 * Utility to serialize a frame in the SLE SDU format. Uses the SLEHead header, adds ASM header, then writes
	 * the data.
	 * @param frame TmProductionData frame from SLE
	 */
	private void writeSleFrame(final TmProductionData frame) {
		JavaTimeTag ert = frame.getErt();
		ByteBlock data = frame.getData();

		int size = data.getLength() + ChillInterfaceInternalConfigManager.INSTANCE.getDownlinkASMHeader().length;
		logger.trace("transfer(frame): Creating new SleHead. ASM + frame size: {}, ERT(ms): {}", size, ert.getMilliseconds());

		SleHead sleHeader = new SleHead();
		try {
			sleHeader.load(frame);

			sleHeader.serialize(currentBitOutputStream);

			logger.trace("transfer(frame): Write ASM to stream");
			currentBitOutputStream.write(ChillInterfaceInternalConfigManager.INSTANCE.getDownlinkASMHeader());

			logger.trace("transfer(frame): Write data to stream");
			currentBitOutputStream.write(data.getBytes(), data.getStartOffset(), data.getLength());
			ChillInterfaceDownlinkManager.INSTANCE.incrementFramesTransferredCount(ZonedDateTime.now(ZoneOffset.UTC));

			// Send written frame time and count information to GUI clients
			MessageDistributor.INSTANCE.chillDownDataFlow(
					ChillInterfaceDownlinkManager.INSTANCE.getLastFramesTransferredTimeSinceLastConnection(),
					ChillInterfaceDownlinkManager.INSTANCE.getFramesTransferredCountSinceLastConnection());

		} catch (Exception e) {
			handleFrameException(e);
		}


	}

	/**
	 * Utility to serialize a frame in the LEO-T format. Uses the LeotHead header, adds ASM header, then writes
	 * the data.
	 * @param frame TmProductionData frame from SLE
	 */
	private void writeLeotFrame(final TmProductionData frame) {
		JavaTimeTag ert = frame.getErt();
		ByteBlock data = frame.getData();
		int size = data.getLength() + ChillInterfaceInternalConfigManager.INSTANCE.getDownlinkASMHeader().length;
		logger.trace("transfer(frame): Creating new LeotHead. ASM + frame size: {}, ERT(ms): {}", size, ert.getMilliseconds());

		LeotHead leotHeader = new LeotHead(size, new Date(ert.getMilliseconds()));
		try {
			logger.trace("transfer(frame): Write LeotHead to stream");
			leotHeader.write(currentBitOutputStream);
			// Add ASM header
			logger.trace("transfer(frame): Write ASM to stream");
			currentBitOutputStream.write(ChillInterfaceInternalConfigManager.INSTANCE.getDownlinkASMHeader());
			logger.trace("transfer(frame): Write data to stream");
			currentBitOutputStream.write(data.getBytes(), data.getStartOffset(), data.getLength());
			ChillInterfaceDownlinkManager.INSTANCE.incrementFramesTransferredCount(ZonedDateTime.now(ZoneOffset.UTC));

			// Send written frame time and count information to GUI clients
			MessageDistributor.INSTANCE.chillDownDataFlow(
					ChillInterfaceDownlinkManager.INSTANCE.getLastFramesTransferredTimeSinceLastConnection(),
					ChillInterfaceDownlinkManager.INSTANCE.getFramesTransferredCountSinceLastConnection());

		} catch (IOException ie) {
			handleFrameException(ie);
		}

	}

	/**
	 * If something goes wrong in writing a frame, handle logging and stop further frame transfer
	 *
	 * @param ex the exception
	 */
	private void handleFrameException(Exception ex) {
		logger.error("{} Error writing to output stream, downlink connection seems to have been lost",
				SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(), ex);
		logger.debug("transfer(frame): Raising interrupt");
		Thread.currentThread().interrupt();
		logger.debug("transfer(frame): Setting SLEInterfaceReturnService's frame queue to null");
		SLEInterfaceReturnService.INSTANCE.setFrameQueue(null);
		doNotTransferRemainingFrames = true;
	}

	/**
	 * Transfer the frame data over the socket connection to chill_down.
	 *
	 * @param frame
	 *            The frame data to send
	 */
	private void transfer(final TmProductionData frame) {
		logger.trace("Entered transfer(frame: {})", frame);
		switch (downlinkOutputFormat) {
			case LEOT:
				writeLeotFrame(frame);
				break;
			case SLE:
				writeSleFrame(frame);
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + downlinkOutputFormat);
		}
	}

	/**
	 * Closes this client's socket connection. This method wouldn't normally be
	 * called, unless the client is blocked on an I/O operation on the socket,
	 * in which case closing the socket may be necessary in order for the client
	 * thread to be finally interrupted.
	 *
	 * @throws IOException
	 *             Thrown when socket closure causes an exception
	 *
	 */
	public final void closeSocket() throws IOException {
		logger.debug("Entered closeSocket()");

		if (socket != null && !socket.isClosed()) {
			logger.debug("closeSocket(): Closing socket");
			socket.close();
		} else {
			logger.debug("closeSocket(): Socket was already closed!");
		}

	}

}