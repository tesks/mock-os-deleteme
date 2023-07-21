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
package jpl.gds.telem.input.impl.service;

import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.connection.IDownlinkConnection;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.context.api.EnableRemoteDbContextFlag;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.config.RawDataFormat;
import jpl.gds.telem.input.api.config.StreamType;
import jpl.gds.telem.input.api.connection.IRawInputConnection;
import jpl.gds.telem.input.api.connection.IRemoteConnectionSupport;
import jpl.gds.telem.input.api.data.IRawDataProcessor;
import jpl.gds.telem.input.api.data.helper.IDataProcessorHelper;
import jpl.gds.telem.input.api.message.RawInputMetadata;
import jpl.gds.telem.input.api.service.ITelemetryInputService;
import jpl.gds.telem.input.api.stream.IRawInputStream;
import jpl.gds.telem.input.api.stream.IRawStreamProcessor;
import jpl.gds.telem.input.impl.message.RawInputMessenger;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RawInputHandler handles the processing of raw input into internal MPCS
 * messages consumed by various MPCS components
 *
 * NB: An EOFException is a subclass of IOException, so you MUST
 * catch it first if you care about the difference.
 *
 * MPCS-5131 08/01/13
 * Note that "stopping" is both synchronized and atomic.
 * It's synchronized so that it cannot be detected as true until
 * all of the shutdown is performed in stopReading.
 * It's atomic so we can pass it to the input connection as an object.
 *
 * I did not mark every change of stopping to getStopping.
 *
 */
public class TelemetryInputService implements ITelemetryInputService {
	private final Tracer logger;

    /** MPCS-6806 01/07/15 New */
    private static final int EXCEPTION_LIMIT = 1000;
    
    private static final int READ_JOIN_WAIT = 5000;

    // MPCS-5013 07/30/13 Made final
    private final RawInputMessenger messenger;
	private boolean paused;

    // MPCS-5131 07/31/13 Access synchronized
    private final AtomicBoolean stopping = new AtomicBoolean(true);
    private final AtomicBoolean runError = new AtomicBoolean(false);
    private final AtomicBoolean reading = new AtomicBoolean(false);

    /* MPCS-9569 - 4/18/18 - Add flag for read thread stopped */
    private final AtomicBoolean readThreadStopped = new AtomicBoolean(false);

    private final AtomicBoolean connected = new AtomicBoolean(false);
    
	private IRawInputConnection conn;
	private IRawStreamProcessor streamProc;
	private IRawDataProcessor dataProc;
	private IRawInputStream inputStream;

    // MPCS-5013 07/24/13 Made final; moved metadata to constructor
	private final TelemetryInputType rawInputType;
	private final TelemetryConnectionType connectionType;
	
	private final boolean isRemoteMode;
	
	private final ApplicationContext serviceContext;
	private Thread readThread;

	/**
	 * Constructor
	 * 
	 * @param serveContext the current application context
	 */
	public TelemetryInputService(final ApplicationContext serveContext)  {
		if (serveContext == null) {
			throw new IllegalArgumentException("Service context is null");
		}

		this.serviceContext = serveContext;
        this.logger = TraceManager.getTracer(serveContext, Loggers.TLM_INPUT);
		
		final IDownlinkConnection dc = serveContext.getBean(IConnectionMap.class).getDownlinkConnection();
				
		this.rawInputType = dc.getInputType();
		this.connectionType = dc.getDownlinkConnectionType();
        this.messenger = serviceContext.getBean(RawInputMessenger.class);
        
		this.isRemoteMode = serviceContext.getBean(EnableRemoteDbContextFlag.class).isRemoteDbEnabled();
	}
	
	@Override
    public synchronized boolean startService() {

		try {
			conn = serviceContext.getBean(IRawInputConnection.class);
			if (conn instanceof IRemoteConnectionSupport) {
			    ((IRemoteConnectionSupport)conn).setRemoteMode(this.isRemoteMode);
			}
		} catch (final Exception e) {
		    e.printStackTrace();
			logger.error("Unable to create RawInputConnection", e);
		}

		try {
			streamProc = serviceContext.getBean(IRawStreamProcessor.class);
			streamProc.init(rawInputType, isRemoteMode);
		} catch (final Exception e) {
		    e.printStackTrace();
			logger.error("Unable to create RawStreamProcessor", e);
		}

		if (conn == null || streamProc == null) {
			logger.error("Unable to initialize raw input handler");
			return false;
		}

		conn.addConnectionListener(this);

		try {
			dataProc = serviceContext.getBean(IRawDataProcessor.class);

            // MPCS-9947 6/29/18: Set the stream's data processor
            streamProc.setDataProcessor(dataProc);
		} catch (final Exception e) {
			logger.debug("Unable to create RawDataProcessor. This is expected if your raw input format does not require one.");
		} 

		IDataProcessorHelper helper = null;
		try {
			helper = serviceContext.getBean(IDataProcessorHelper.class);
		} catch (final Exception e) {
			logger.debug("Unable to create DataProcessorHelper. This is expected if your raw input format does not require one.");
		}

		if (dataProc != null && helper != null) {
			// start data processor
			try {
                dataProc.init(helper, StreamType.getStreamType(rawInputType), connectionType);
            } catch (final RawInputException e) {
                e.printStackTrace();
                logger.error("Unable to create RawDataProcessor", e);
                logger.error("Unable to initialize raw input handler");
                return false;
            }
		}
		
		return true;
	}
	
	@Override
    public synchronized void stopService() {
	
	    try {
	        stopReading();
	        /* MPCS-9569 - 4/18/18 - disconnect() now done in stopReading() */
            this.messenger.stop();
        } catch (final IOException e) {
            logger.error("There was an issue stopping the raw input handler: " + e.toString());
            e.printStackTrace();
        }
	
	}

	@Override
    public synchronized void startReading() throws RawInputException
    {
	    if (!connected.get()) {
	        throw new RawInputException("Telemetry source is not connected");
	    }
	    if (reading.getAndSet(true)) {
	        throw new RawInputException("Telemetry is already being read");
	    }
	    
	    if (this.streamProc != null) {
	        streamProc.init(rawInputType, isRemoteMode);
	    }
	    
	    if (this.dataProc != null) {
           dataProc.start();
	    }	   
	    try {
            this.readThread = new Thread(new InputReader(), "TLM Reader");
	        this.readThread.start();
	        /* MPCS-9569 - 4/18/18 - Reset read stop flag */
	        readThreadStopped.set(false);
	    } catch (final Exception e) {
	        e.printStackTrace();
	        this.reading.set(false);
	        throw new RawInputException("Unexpected error starting read thread: " + e.toString(), e);
	    }
	  
	}


    /**
     * On any kind of error (except IO), reset so processing
     * can continue.
     *
     * We cannot just duplicate the IOException logic because we
     * can get here on practically any kind of error in any part
     * of the underlying code. Something could be left in an unusable
     * state. So we recreate the connection and raw stream processor.
     *
     * Returning false means that the caller gives up. It doesn't
     * necessarily indicate an error.
     *
     * If the exception is thrown we cannot continue.
     *
     * @param soFar     Exception count
     * @param exception Exception received
     *
     * @return True if continuation is possible
     *
     * @throws RawInputException If unable to reset
     *
     * MPCS-6806 01/07/15 New method
     */
    private boolean reset(final int       soFar,
                          final Exception exception)
        throws RawInputException
    {
        if (getStopping())
        {
            return false;
        }

        if (soFar > EXCEPTION_LIMIT)
        {
            logger.error("Unable to reset RawInputHandler, " +
                             "too many exceptions ("         +
                             EXCEPTION_LIMIT                 +
                             ")",
                         exception);

            return false;
        }

        // Connection must be closed so it can be reopened

        try
        {
            if (conn != null)
            {
                conn.closeConnection();
            }
        }
        catch (final Exception e)
        {
            conn = null;
        }

        try
        {
            conn = serviceContext.getBean(IRawInputConnection.class);
            if (conn instanceof IRemoteConnectionSupport) {
                ((IRemoteConnectionSupport)conn).setRemoteMode(this.isRemoteMode);
            }
        }
        catch (final Exception rie)
        {
            logger.error("Unable to recreate raw input connection", rie);

            throw new RawInputException("Unable to recreate raw input connection", rie);
        }

		if (! conn.openConnection())
        {
            logger.error("Unable to reopen raw input connection");

            throw new RawInputException("Unable to reconnect raw input connection");
        }

        streamProc = null;

        try
        {
            streamProc = serviceContext.getBean(IRawStreamProcessor.class);
            streamProc.init(rawInputType, isRemoteMode);

            // MPCS-9947 6/29/18: Set the data processor again after resetting stream
            streamProc.setDataProcessor(dataProc);
        }
        catch (final Exception rie)
        {
            logger.error("Unable to recreate raw stream processor", rie);

            throw new RawInputException("Unable to recreate raw stream processor", rie);
        }

        streamProc.setAwaitingFirstData(true);

        if (! reconnect())
        {
            // NB: This is not necessarily a real failure. You cannot reconnect to a file,
            // for example.

            logger.error("Unable to reconnect raw input connection: " +
                         conn.getClass().getName());

            return false;
        }

        conn.addConnectionListener(this);

        messenger.sendConnectMessage(conn.getConnectionString());

        logger.debug("RawInputHandler reset: "       +
                     exception.getClass().getName()  +
                     "("                             +
                     exception.getLocalizedMessage() +
                     ")");

        return true;
    }


    private boolean reconnect() throws RawInputException {
		conn.reconnect();
		
		messenger.sendConnectMessage(conn.getConnectionString());

        /* MPCS-7939 02/12/16 - Changed, if the IDataInputStream doesn't reconnect (and the raw input
		* system isn't stopping), it throws a RawInputException. If it does reconnect, then there's no need
		* to check the getRawInputStream return value
		*/
		return true;
	}


    /**
     * Set stopping state to false. Used at startup
     * so we do it synchronized.
     */
    private synchronized void setStarting()
    {
        stopping.set(false);
        /* MPCS-9569 - 4/18/18 - Reset read stopped flag */
        readThreadStopped.set(false);
        messenger.setFlowing(true);
    }


    /**
     * Get stopping state.
     *
     * @return True if we are stopping
     * MPCS-9569 - 4/18/18 - No longer synchronized
     */
    private boolean getStopping()
    {
        return stopping.get();
    }

	@Override
    public synchronized void stopReading() throws IOException
    {
        /**
         * MPCS-9695 - 5/21/18: Updated to always set reading to false and stopping to true
         *                          instead of returning when reading was false or stopping was set.
         * The returns caused a shutdown problem in the case of a socket connection that never successfully connected.
         */
        reading.getAndSet(false);
        stopping.getAndSet(true);
		
		/* MPCS-9569 - 4/18/18 - Disconnect from the data source and shutdown the
		 * data processors. This should terminate any read in progress.
		 */
		disconnect();

		/* Now the read thread should stop. */
		if (this.readThread != null) {
            try {
                logger.info("Waiting for telemetry read thread to terminate");
                this.readThread.join(READ_JOIN_WAIT);
            } catch (final InterruptedException e) {
                logger.info("Join to telemetry read thread was interrupted");
            }

            logger.info("Read thread" + (this.readThreadStopped.get() ? " has terminated" : " has not terminated"));
            
            /* If the read thread has not terminated, resort to interrupting it in case it is stuck on some
             * read to the source. 
             */
            if (!this.readThreadStopped.get()) {
                try {
                    
                    logger.info("Interrupt and wait for telemetry read thread to terminate again - longer wait");
                    this.readThread.interrupt();
                    this.readThread.join(READ_JOIN_WAIT * 3);
                } catch (final InterruptedException e) {
                    logger.info("Join to telemetry read thread was interrupted");
                }
                logger.info("Read thread" + (this.readThreadStopped.get() ? " has terminated" : " has not terminated. Exiting in any case..."));
            }
		}

        // MPCS-5135 08/02/13 Moved down after first if
        logger.debug("RawInputHandler: stop reading");
        
        /* MPCS-9569 - 4/18/18 -  removed stop of stream and data processors */
        
        reading.set(false);
        
	}
    
	@Override
    public void disconnect() throws IOException {

	    /* MPCS-9569 - 4/18/18. Allow disconnect even if still reading.*/

	    if (conn != null) {
            conn.closeConnection();
        }

	    if (!connected.getAndSet(false)) {
	        return;
	    }

        if (dataProc != null) {
            dataProc.stop();
        }

        if (streamProc != null) {
            streamProc.stop();
        }
        
        //input stream needs to close after connection, otherwise we would think we lost the connection
        if (inputStream != null) {
            inputStream.close();
        }
        
        messenger.sendStopMessage();
	}

	@Override
    public void pause() {
		if (this.paused) {
			return;
		}

		streamProc.pause();
		this.paused = true;
	}

	@Override
    public void resume() {
		if (!this.paused) {
			return;
		}

		streamProc.resume();
		this.paused = false;
	}
	
	// MPCS-7832 01/07/16 - Added function
	@Override
    public void clearInputStreamBuffer() throws UnsupportedOperationException, IOException, IllegalStateException{
		if(inputStream != null){
			inputStream.clearInputStreamBuffer();
		}
		else{
		    // MPCS-8152 - 05/03/16 - updated message
		    throw new IllegalStateException("RawInputHandler: InputStream is null at this time. No data to be cleared.");
		}
	}

	@Override
    public void setMeterInterval(final long meterInterval) {
		streamProc.setMeterInterval(meterInterval);
	}
	
    @Override
	public void onConnectionLost() {
		streamProc.setConnectionLost(true);
	}

	@Override
	public void onConnectionGained() {
		streamProc.setConnectionLost(false);
	}

	@Override
    public boolean connect() throws RawInputException {
	    if (connected.get()) {
	        throw new RawInputException("Already connected");
	    }
	    
	    connected.set(conn.openConnection());
	    
	    final String connectString = conn.getConnectionString();

	    // M. DeMore - Adding check for "stopping" here. We do not want an error just because
	    // user chose to stop (MPCS-3420).
	    if (!connected.get() && !getStopping()) {
	        throw new RawInputException("Unable to connect to telemetry source: " + conn.getConnectionString());
	    }
	    
        if (connectString == null) {
            throw new RawInputException("Null input connect string");
        }   else if(!connected.get() && getStopping()){
            return false;
        }
              
        messenger.sendConnectMessage(connectString);
        return true;
	}
	
	private class InputReader implements Runnable {

        @Override
        public void run() {
            try {
                int exceptions = 0;

                // MPCS-5131 08/01/13
                setStarting();

                // MPCS-5013 07/24/13
                RawInputMetadata metadata = null;

                retryConnectionLoop:

                while (!getStopping()) {
                    try {
                        while (true)
                        {
                            /**
                             * MPCS-5131 08/01/13
                             * Check for stopping at top of loop
                             * (Not in while because we want to break out of both)
                             * Stopping object passed to loadData so he can detect
                             * it being set. Used for server socket input connections. 
                             */
                            if (getStopping() || ! conn.loadData(stopping))
                            {
                                break retryConnectionLoop;
                            }

                            inputStream = conn.getRawInputStream();
                            metadata = conn.getMetadata();

                            if (metadata == null) {
                                metadata = new RawInputMetadata();
                            }

                            metadata.setDataFormat(RawDataFormat.getRawDataFormat(rawInputType));

                            streamProc.processRawData(inputStream, metadata);

                            if (getStopping() || !conn.isDataStream()) {
                                break retryConnectionLoop;
                            }

                            // MPCS-5013 07/23/13 "if" statement

                            if (streamProc.getEofOnStreamStatus() &&
                                conn.needsEofOnStreamStatus())
                            {
                                // Lost server, must reconnect.
                                // Used for NEN_SN connections to detect
                                // lost server.

                                streamProc.setAwaitingFirstData(true);

                                if (! reconnect())
                                {
                                    break retryConnectionLoop;
                                }

                            }
                        }
                    }
                    catch (final IOException e) {
                        // Includes EOFException

                        if(getStopping()) {
                            break retryConnectionLoop;
                        }

                        // MPCS-5013 07/29/13 EOF trumps connected
                        // Lost server, must reconnect.
                        // Used for CLIENT_SOCKET connections to detect lost server.

                        final boolean eof = (streamProc.getEofOnStreamStatus() &&
                                             conn.needsEofOnStreamStatus());

                        if (! eof && conn.isConnected())
                        {
                            continue;
                        }

                        streamProc.setAwaitingFirstData(true);
                        final boolean reconnSuccess = reconnect();

                        if (reconnSuccess) {
                            continue;
                        } else {
                            break retryConnectionLoop;
                        }
                    }
                    catch (final Exception e)
                    {
                        e.printStackTrace();
                        /** MPCS-6806 01/07/15 New catch */

                        // See comments at reset below.

                        ++exceptions;

                        if (! reset(exceptions, e))
                        {
                            // We're done in any case

                            break retryConnectionLoop;
                        }
                    }
                }

                messenger.setFlowing(false);
                messenger.sendEndOfDataMessage();
            } catch (final RawInputException e) {
                logger.error("Error in telemetry read thread: " + e.toString(), e);
                runError.set(true);
            }
            
            /* MPCS-9569 - 4/18/18 - Set stop flag and issue log */
            readThreadStopped.set(true);
            logger.info("Telemetry read thread has now stopped");
        }
	    
	}

    @Override
    public synchronized boolean isConnected() {
        return this.connected.get();
    }

    @Override
    public synchronized boolean isReading() { return this.reading.get(); }
}
