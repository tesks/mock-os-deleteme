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
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.util.HostPortUtility;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.stream.IRawInputStream;
import jpl.gds.telem.input.impl.stream.BufferedRawInputStream;
import jpl.gds.telem.input.impl.stream.RawInputStream;


/**
 * This class is responsible for establishing inbound socket connections for
 * telemetry acquisition.
 * 
 * @since	AMPCS R3
 */
public class ServerSocketInputConnection
        extends AbstractRawInputConnection
{
    // MPCS-5135 08/02/13 Change parent class

	private Thread listenerThread;
	private ConnectionListenerWorker listenerWorker;
	private volatile Socket socket;
	private volatile DataInputStream dis;
	private int listenPort;
    
	
    /**
     * Constructor.
     * 
     * @param context the current application context
     */
    public ServerSocketInputConnection(final ApplicationContext context) {
        super(context);
    }

	private synchronized void setDis(final DataInputStream d) {
		logger.trace("SSIC: setDis(" + d + ")");
		dis = d;
	}
	
	
	private synchronized void setSocket(final Socket s) {
		logger.trace("SSIC: setSocket(" + s + ")");
		socket = s;
	}

	
	private Socket getSocket() {
		return socket;
	}
	
	
	private void startConnectionListener() {
		logger.debug("SSIC: startConnectionListener()");

		logger.trace("SSIC:  - calling stopConnectionListener()");
		stopConnectionListener();

		logger.debug("SSIC:  - creating new listener worker (port=" + listenPort + ")");
		listenerWorker = new ConnectionListenerWorker(listenPort, this);
		/* MPCS-7135 - 3/17/15. Name the thread. */
		listenerThread = new Thread(listenerWorker, "Connection Listener Worker");
		listenerThread.setDaemon(true);

		logger.debug("SSIC:  - starting listener thread");
		listenerThread.start();
	}

	
	private void waitForInputStream() {
		logger.debug("SSIC: waitForInputStream()");
		
		while (dis == null && !stopping) {

			if (rawConfig.getSocketRetryInterval() > 0) {
				logger.trace("SSIC:  - dis still null and not stopping yet; sleep "
						+ rawConfig.getSocketRetryInterval()
						+ "ms");
				SleepUtilities
						.fullSleep(rawConfig
								.getSocketRetryInterval(), logger,
								"SSIC.waitForInputStream sleep error on retry");
			}

		}

		if (dis != null) {
			logger.debug("SSIC:  - dis obtained (" + dis + "); exiting waitForInputStream()");
		} else if (stopping) {
			logger.debug("SSIC:  - stopping; exiting waitForInputStream()");
		}

	}
	
	
	private void stopConnectionListener() {
		logger.debug("SSIC: stopConnectionListener()");

		if (listenerThread != null) {
			logger.trace("SSIC:  - interrupting listenerThread");
			listenerThread.interrupt();
			logger.debug("SSIC:  - stopping listenerWorker's ServerSocket");
			listenerWorker.stopServer();

			while (listenerThread.isAlive()) {

				if (rawConfig.getSocketRetryInterval() > 0) {
					/*
					 * Here, we're reusing the SocketRetryInterval value even
					 * though that config property is not specifically meant for
					 * this purpose. I see no harm of doing it, and it avoids
					 * adding yet another config property to the bloated GDS
					 * config. - Josh Choi
					 */
					logger.trace("SSIC:  - listenerThread is still alive; sleep "
							+ rawConfig
									.getSocketRetryInterval() + "ms");
					SleepUtilities.fullSleep(rawConfig
							.getSocketRetryInterval(), logger,
							"SSIC.stopConnectionListener sleep error on retry");
				}

			}
			
			logger.trace("SSIC:  - listenerThread now being set to null");
			listenerThread = null;
			
		} else {
			logger.debug("SSIC:  - listenerThread is already null");
			
		}
		
	}
	
	
	private void stopCurrentConnection() throws IOException
    {
		logger.trace("SSIC: stopCurrentConnection()");
		
		/*
		 * Below are specialized code blocks just for DSN Emulator. When NEN/SN
		 * mode is being used for DSN Emulator, we need to initiate a directive
		 * to stop the data flow. We also need to log the DSN Emulator's status
		 * of NEN/SN data transmission.
		 */

        // MPCS-5135 08/02/13 Refactor if

		logger.trace("SSIC:  - sending out disconnect message");
		messenger.sendDisconnectMessage(getConnectionString());

		if (dis != null) {
			logger.trace("SSIC:  - closing dis and setting it to null");
			dis.close();
			setDis(null);
		} else {
			logger.trace("SSIC:  - dis is already null");
		}

		if (socket != null) {
			logger.trace("SSIC:  - closing socket and setting it to null");
			socket.close();
			setSocket(null);
		} else {
			logger.trace("SSIC:  - socket is already null");
		}
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#openConnection()
	 */
	@Override
	public boolean openConnection()
        throws RawInputException
    {
		logger.trace("SSIC: openConnection(...)");
		this.stopping = false;


		/*
		 * Use the downlink port as the listening port.
		 */
		listenPort = connectPort;

		if (!HostPortUtility.isPortValid(listenPort)) {
			throw new IllegalArgumentException("Invalid port number " + listenPort
			        + "given");
		}

		/*
		 * Start the connection listening thread.
		 */
		startConnectionListener();
		
		/*
		 * Wait around for a DataInputStream to be obtained. 
		 */
		waitForInputStream();
		
		/*
		 * Below should return true.
		 */
		return dis != null;
	}

	
	/**
	 * {@inheritDoc}
	 * 
	 * @see
	 * jpl.gds.telem.input.api.connection.IRawInputConnection#closeConnection()
	 */
	@Override
	public void closeConnection() throws IOException {
		logger.trace("SSIC: closeConnection()");
		super.closeConnection();
		
		/*
		 * Stop the current socket connection with the station, if any.
		 */
		stopCurrentConnection();
		
		/*
		 * Stop the connection listening thread.
		 */
		stopConnectionListener();
		
	}
	

	/**
	 * {@inheritDoc}
	 * 
	 * @see
	 * jpl.gds.telem.input.api.connection.IRawInputConnection#getConnectionString()
	 */
	@Override
	public String getConnectionString()
    {
        /** MPCS-5013 07/17/13 Whole thing */

        final Socket        socket = getSocket();
        final StringBuilder sb     = new StringBuilder();

        sb.append(this.rawInputType);
        sb.append(" with client at ");

        if ((socket != null) && socket.isConnected())
        {
            sb.append(socket.getInetAddress().getHostAddress()).append(':');
            sb.append(socket.getPort());
            sb.append(" on local ").append(socket.getLocalAddress().getHostAddress());
            sb.append(':').append(socket.getLocalPort());
        }
        else
        {
            sb.append("no connection");
        }

        /*
         * 11/25/13 - MPCS-5552. Do not add station to message if there
         * isn't one defined.
         */
        if (station != StationIdHolder.UNSPECIFIED_VALUE) {
            sb.append(" from station ").append(station);
        }

        return sb.toString();
	}

	
	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#reconnect()
	 */
	@Override
	public void reconnect() throws RawInputException {
		logger.debug("SSIC: reconnect()");

		try {
			stopCurrentConnection();
		} catch (final IOException e) {
			logger.debug("SSIC:  - IOException: " + e.getMessage());
			throw new RawInputException(e);
		}

		/*
		 * Wait for a DataInputStream to be obtained again.
		 */
		waitForInputStream();
		
	}

	
	/**
	 * {@inheritDoc}
	 * 
	 * @see
	 * jpl.gds.telem.input.api.connection.IRawInputConnection#getRawInputStream
	 * ()
	 * 
	 * MPCS-7939 - 02/12/16 - refactored to remove reconnect logic
	 */
	@Override
	public IRawInputStream getRawInputStream() {

		logger.debug("SSIC: getRawInputStream()");

		if (dis == null) {
			logger.debug("SSIC:  - dis == null, so returning null");
			return null;
		}

		final DataInputStream tempDis = dis;
		setDis(null);
		// MPCS-7766 12/29/15 - Update to use buffered input stream only when specified
        if (this.rawConfig.isBufferedInputAllowed(connectionType, sseFlag)) {

			// MPCS-7449 DE 10/07/15 changed to BufferedRawInputStream
			return ((tempDis != null) ? new BufferedRawInputStream(this.appContext, tempDis, this.bufferDir) : null);
		}
		else{
			return ((tempDis != null) ? new RawInputStream(tempDis) : null);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#loadData(java.util.concurrent.atomic.AtomicBoolean)
	 */
	@Override
	public boolean loadData(final AtomicBoolean handlerStopping)
        throws IOException
    {
		if (this.dis != null) {
			logger.trace("SSIC: loadData() returning true");
			return true;
		} else {
			
			/*
			 * Why are we triggering a reconnect() here? Because RawInputHandler
			 * won't, once we return a false. RawInputHandler will just keep
			 * looping, as it continues to retrieve "false" from this method,
			 * and nothing will happen. So we have to trigger the reconnect here
			 * before we return a value. (RawInputHandler is not doing things
			 * right.)
			 */
			try {
				logger.trace("SSIC: loadData() attempting reconnect");

				reconnect();

                // MPCS-5131 08/01/13 See if the reconnect caused the
                // raw input handler to stop. We don't want to light
                // Connected on the GUI.

                if ((handlerStopping != null) && handlerStopping.get())
                {
                    return false;
                }

                messenger.sendConnectMessage(getConnectionString());

                // MPCS-5131 08/01/13 Make Waiting light go oute
                messenger.sendStartOfDataMessage();

				return true;
			} catch (final RawInputException e) {
				logger.trace("SSIC: loadData() returning false (reconnect attempt failed): " + e.getMessage());
				return false;
			}
			
		}
		
	}

	
	/**
	 * {@inheritDoc}
	 * 
	 * @see
	 * jpl.gds.telem.input.api.connection.IRawInputConnection#isConnected()
	 */
	@Override
	public boolean isConnected() {

//		if (getSocket() == null) {
//			logger.trace("SSIC: isConnected() returning false");
//			return false;
//		}
//		
//		logger.trace("SSIC: isConnected() returning " + getSocket().isConnected());
//		return getSocket().isConnected();
		
		/*
		 * The above commented out code is the way this method is _supposed to_
		 * work. But sockets don't close immediately. And as long as we're
		 * waiting for the socket timeout, RawInputHandler will cause the
		 * downlink process to loop infinitely doing nothing. So we wire this
		 * method to return false, so that reconnect logic will kick in.
		 */
		logger.trace("SSIC: isConnected() returning false");
		return false;

	}

	
	/**
	 * {@inheritDoc}
	 * 
	 * @see
	 * jpl.gds.telem.input.api.connection.IRawInputConnection#isDataStream()
	 */
	@Override
	public boolean isDataStream() {
		return true;
	}

	
	/**
	 * Class that maintains the connection listening ServerSocket. Runs in a
	 * thread.
	 * 
	 */
	private class ConnectionListenerWorker implements Runnable
    {
        // MPCS-5135 08/02/13 Make non-static

		private ServerSocket server;

        // MPCS-5013 07/18/13
		private final ServerSocketInputConnection ssic;
		private final int port;

		
		/**
		 * Default constructor.
		 * 
		 * @param port
		 *            Port number of the listening socket.
		 * @param ssic
		 *            The ServerSocketInputConnection object using this worker.
		 */
		ConnectionListenerWorker(final int port, final ServerSocketInputConnection ssic) {
			this.port = port; 
			this.ssic = ssic;
		}

		
		private void stopServer() {
			logger.debug("SSIC->CLW: stopServer()");

			if (server != null) {
				
				try {
					logger.debug("SSIC->CLW:  - listening server is not null; closing it");
					server.close();
					server = null;
				} catch (final IOException e) {
					logger.error("Closing ServerSocket produced exception: " + e.getMessage());
				}
				
			} else {
				logger.trace("SSIC->CLW:  - listening server is already null");
			}

		}

		
		/**
		 * {@inheritDoc}
		 *
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			logger.debug("SSIC->CLW: run()");

			try {
				server = new ServerSocket(port);
				logger.debug("SSIC->CLW:  - new listening server on port " + port);

			} catch (final IOException e) {
				logger.error("Creating ServerSocket on port " + port
						+ " produced exception: " + e.getMessage());

                                return;
			}

			logger.trace("SSIC->CLW:  - entering connection accepting while loop");

			while(!Thread.interrupted()) {
				Socket newSocket = null;
				
				try {
					newSocket = server.accept();
				} catch (final IOException e) {
					logger.debug("SSIC->CLW:  -- IOException on server.accept(): " + e.getMessage());
					return;
				}

				logger.debug("SSIC->CLW:  -- accepted a new inbound connection");
				
				try {
					
					if (ssic.getSocket() != null) {
						logger.warn("Rejected additional connection to data source "
								+ ssic.rawInputType
								+ ":localPort="
								+ ssic.getSocket().getLocalPort()
								+ ":clientIP="
								+ newSocket.getInetAddress().toString()
								+ ":clientPort="
								+ newSocket.getPort()
								+ " at "
								+ TimeUtility.getFormatter().format(new AccurateDateTime()));
						newSocket.close();
					} else {
						logger.debug("SSIC->CLW:  -- no connection existed; accepting it");
						ssic.setSocket(newSocket);
						logger.trace("SSIC->CLW:  -- opening a new DataInputStream from the new connection");
						ssic.setDis(new DataInputStream(newSocket
								.getInputStream()));

					}

				} catch (final IOException e) {
					logger.warn("Exception with new socket: " + e.getMessage());
				}

			}
			
			logger.debug("SSIC->CLW:  - exited connection accepting while loop; close listening server");
			
			try {
				server.close();
			} catch (final IOException e) {
				logger.warn("Exception trying to close connection listening server: " + e.getMessage());
			}
		}
	}
}
