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
package jpl.gds.telem.input.impl.connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.connection.IDownlinkConnection;
import jpl.gds.common.config.connection.IFileConnectionSupport;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.util.HostPortUtility;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.stream.IRawInputStream;
import jpl.gds.telem.input.impl.PVLInfo;
import jpl.gds.telem.input.impl.stream.BufferedRawInputStream;
import jpl.gds.telem.input.impl.stream.RawInputStream;

/**
 * This class is responsible for establishing a connection to a TDS server for
 * telemetry acquisition
 * 
 *
 * MPCS-4632 - 10/13/16 - Added override of shouldReconnectOnIOException.
 */
public class TdsInputConnection extends AbstractRawInputConnection {
	private DataOutputStream tdsOutStream;
	private DataInputStream tdsInStream;
	private Socket socket;
	private final String pvlFileString;
	private PVLInfo pvlInfoObj;
	
	/**
     * Constructor.
     * 
     * @param serveContext the current application context
     */
	public TdsInputConnection(final ApplicationContext serveContext) {
		super(serveContext);
		final IDownlinkConnection dc = serveContext.getBean(IConnectionMap.class).getDownlinkConnection();
		this.pvlFileString =  ((IFileConnectionSupport)dc).getFile();
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#openConnection()
	 */
	@Override
	public boolean openConnection() throws RawInputException {
		this.stopping = false;

		if (connectHost == null) {
			throw new IllegalArgumentException("Null input host");
		} else if (!HostPortUtility.isPortValid(connectPort)) {
			throw new IllegalArgumentException("Invalid port number " + connectPort
			        + "given");
		}

		this.tdsInStream = getDataInputStream();

		return tdsInStream != null;
	}

	/**
	 * Attempts to open a socket connection with a TDS server at the given
	 * hostname and port. If successful, the PVL information is transmitted and
	 * the InputStream from the socket is returned
	 * 
	 * @param host the hostname of the TDS server
	 * @param port the port number to be used to connect to the TDS server
	 * @return the input stream from the server
	 */
	private DataInputStream getDataInputStream() {
		int retryCount = rawConfig.getSocketRetries();
		DataInputStream tdsInStream = null;
		boolean closed = true;

		// loop and keep trying to open the socket for as many
		// times as specified by the retry count
		while (closed == true && !stopping) {
			try {
				// if retryCount < 0; retry forever
				if (retryCount > 0) {
					retryCount--;
				}

				// get the socket
				logger.debug(getClass().getName()
				        + " is about to open a socket connection to TDS");
				socket = new Socket(connectHost, connectPort);
				logger.debug(getClass().getName()
				        + " has created client socket");

				// get the data input stream
				tdsInStream = new DataInputStream(socket.getInputStream());
				logger.debug(getClass().getName()
				        + " has opened input stream from TDS socket");

				// if it's null, it's our first time connecting...send the PVL
				if (this.tdsOutStream == null) {
					this.tdsOutStream = new DataOutputStream(socket.getOutputStream());
					logger.debug(getClass().getName()
					        + " has opened output stream to TDS socket");
					
					/*
					 * MPCS-8512 12/28/16 - added. put the PVL file info
					 * into this object first time and send the info to the TDS
					 * server. If the PVL file uses ERT times and the TIME_RANGE
					 * start time is not NOW, the object subscribes to SFDU_TF
					 * and SFDU_PKT messages and updates the start time
					 * dynamically. After the first population verify the
					 * subscriber has been restarted and send the current
					 * version of the PVL info.
					 */
					if(pvlInfoObj == null){
						pvlInfoObj = new PVLInfo(pvlFileString, appContext);
					}
					else{
						pvlInfoObj.restartSubscriber();
					}

					final InputStream pvlInStream = pvlInfoObj.toDataStream();
					logger.debug(getClass().getName()
					        + " has opened input stream from PVL file");

					int totalBytes = 0;
					final byte[] buffer = new byte[800];

					logger.debug(TdsInputConnection.class.getName()
					        + " is sending the PVL file...");
					int readCount = pvlInStream.read(buffer);
					while (readCount != -1) {
						if (readCount > 0) {
							totalBytes += readCount;
							this.tdsOutStream.write(buffer, 0, readCount);
						}
						readCount = pvlInStream.read(buffer);
					}
					this.tdsOutStream.flush();
					logger.debug(getClass().getName()
					        + " has successfully transmitted the PVL file of length " + totalBytes);

					pvlInStream.close();
				}

				// If we got this far the socket is open
				closed = false;
			} catch (final IOException ie) {
				logger.debug("Error connected to socket port " + connectPort
				        + " on host " + connectHost, ie);

				// if we've finished our number of retries
				if (retryCount == 0) {
					logger.debug("IOException " + ie
					        + " reading from socket port " + connectPort + " on host "
					        + connectHost, ie);

					final IPublishableLogMessage edm = appContext.getBean(IStatusMessageFactory.class).createEndOfDataMessage();
					this.bus.publish(edm);
                    this.logger.log(edm);
					this.stopping = true;
				}

				if (rawConfig.getSocketRetryInterval() > 0) {
					SleepUtilities.fullSleep(rawConfig.getSocketRetryInterval(), logger, "AbstractTdsInputAdapter."
					        + "getDataInputStreamFromSocket Error sleeping");
				}
			}
		}

		return tdsInStream;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#loadData(java.util.concurrent.atomic.AtomicBoolean)
	 */
	@Override
	public boolean loadData(final AtomicBoolean handlerStopping)
        throws RawInputException, IOException
    {
        // MPCS-5131 08/01/13 Add atomic
		// the initial (and only) DataInputStream is loaded in openConnection
		if (this.tdsInStream != null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#getRawInputStream()
	 */
	@Override
	public IRawInputStream getRawInputStream() {
		if (this.tdsInStream == null) {
			return null;
		}

		final DataInputStream tempDis = this.tdsInStream;
		this.tdsInStream = null;

		logger.debug("TIC: getRawInputStream()");

		if (tempDis == null) {
			logger.debug("TIC:  - dis == null, so returning null");
			return null;
		}

		// MPCS-7766 12/29/15 - Update to use buffered input stream only when specified
        if (rawConfig.isBufferedInputAllowed(connectionType, sseFlag)) {

			// MPCS-7449 DE 10/07/15 changed to BufferedRawInputStream
			return ((tempDis != null) ? new BufferedRawInputStream(this.appContext, tempDis, this.bufferDir) : null);
		}
		else{
			return ((tempDis != null) ? new RawInputStream(tempDis) : null);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#getConnectionString()
	 */
	@Override
	public String getConnectionString() {
		return this.rawInputType + ":" + connectHost + ":"
		        + connectPort;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.impl.connection.AbstractRawInputConnection#closeConnection()
	 */
	@Override
	public void closeConnection() throws IOException {
		super.closeConnection();

		if (tdsInStream != null) {
			tdsInStream.close();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.impl.connection.AbstractRawInputConnection#reconnect()
	 */
	@Override
	public void reconnect() throws RawInputException {
		logger.debug("TIC: reconnect()");
		
		if (socket != null) {
			try {
				socket.close();
			} catch (final IOException e) {
				logger.debug("TIC:  - IOException: " + e.getMessage());
				throw new RawInputException("Error closing socket", e);
			}
			messenger.sendDisconnectMessage(getConnectionString());
		}

		this.tdsInStream = getDataInputStream();
		
		/* 
		 * MPCS-7610 11/16/16 - Without restarting the output stream we won't send a PVL file on reconnect.
		 * Without sending a PVL file on reconnect we won't get data.
		 */
		this.tdsOutStream = null;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#isConnected()
	 */
	@Override
	public boolean isConnected() {
		return this.socket.isConnected();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#isDataStream()
	 */
	@Override
	public boolean isDataStream() {
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.impl.connection.AbstractRawInputConnection#shouldReconnectOnIOException(boolean)
	 */
	@Override
	public boolean shouldReconnectOnIOException(final boolean processorEofStatus){
		// if we got an EOF, the TDS input connection is done. If not, it needs to reconnect
		//otherwise we need to get more data, BUT(!!!) we need to reconnect the port, since it's most likely dead
		return !processorEofStatus;
	}

}
