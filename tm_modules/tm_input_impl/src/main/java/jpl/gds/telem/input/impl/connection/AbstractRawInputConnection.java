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

import java.io.File;
import java.io.IOException;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.connection.INetworkConnection;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.util.HostPortUtility;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.config.TelemetryInputProperties;
import jpl.gds.telem.input.api.connection.IRawInputConnection;
import jpl.gds.telem.input.api.connection.RawInputConnectionStatusListener;
import jpl.gds.telem.input.api.message.RawInputMetadata;
import jpl.gds.telem.input.impl.message.RawInputMessenger;

/**
 * This is an abstract class that implements <code>IRawInputConnection</code>
 * and provides common implementations for various methods
 * 
 *
 */
public abstract class AbstractRawInputConnection implements IRawInputConnection, IQuitSignalHandler {
	/** Shared trace logger */
    protected final Tracer            logger;
	/** Messenger object for sending input-related messages */
	protected RawInputMessenger messenger;
	/** Current TelemetryInputConfig object */
	protected TelemetryInputProperties rawConfig;
	/** Message publication bus */
	protected IMessagePublicationBus bus;
	/** The current telemetry input type */
	protected TelemetryInputType rawInputType;
	/** The current telemetry connection type */
	protected TelemetryConnectionType connectionType;
	/** The current application context */
	protected ApplicationContext appContext;
    /** Filter station ID */
	protected int station;
	/** Stopping service flag */
	protected boolean stopping = true;
	/** Connection string for messages */
	protected String connectionString;
	/** Output directory for telemetry buffer */
	protected String bufferDir;
	/** Host we are connecting to if network connection */
	protected String connectHost;
	/** Port we are connecting to if network connection */
	protected int connectPort;
	/** GDS host - IS THIS EVEN NEEDED ? - Used only for DSN-E connections */
	protected String gdsHost;
    /** The SSE context flag */
    protected SseContextFlag           sseFlag;
	
	/**
	 * Constructor.
	 * 
	 * @param serveContext the current application context
	 */
	protected AbstractRawInputConnection(final ApplicationContext serveContext) {
        this.appContext = serveContext;
		this.bus = serveContext.getBean(IMessagePublicationBus.class);
        this.logger = TraceManager.getTracer(serveContext, Loggers.TLM_INPUT);
        
	    this.rawConfig = serveContext.getBean(TelemetryInputProperties.class);
	    this.messenger = serveContext.getBean(RawInputMessenger.class);
	    
	    final IConnectionMap connectConfig = appContext.getBean(IConnectionMap.class);
        this.sseFlag = appContext.getBean(SseContextFlag.class);
	    
	    this.rawInputType = connectConfig.getDownlinkConnection().getInputType();
	    this.connectionType = connectConfig.getDownlinkConnection().getDownlinkConnectionType();
	    		
	    final Integer dssid = appContext.getBean(IContextFilterInformation.class).getDssId();
	    station = dssid == null ? StationIdHolder.UNSPECIFIED_VALUE : dssid.intValue();
        this.bufferDir = appContext.getBean(IGeneralContextInformation.class).getOutputDir();
        final Long sessionId = appContext.getBean(IContextIdentification.class).getNumber();
        if (sessionId != null) {
            this.bufferDir = this.bufferDir + File.separator + sessionId;
        }
        
        if (connectionType.isNetwork()) {
        	this.connectHost = ((INetworkConnection)connectConfig.getDownlinkConnection()).getHost();
        	this.connectPort = ((INetworkConnection)connectConfig.getDownlinkConnection()).getPort();
        }
        
        /* R8 Refactor TODO - GDS host no longer in the config file. For SMAP it was always configured
         * to localhost, but I am having a very hard time understanding how and why this is used.  If we are
         * the server, this would always have to be localhost.  If we are the client, then DS-SSL commands
         * seems to expect the name of the emulator host, which must be configured as an environment variable
         * and is taken from the DSN Emulator property file. So I just don't understand why GDS host was used
         * here at all. This variable does not seem to be used at all if the connection is not to the DSN
         * emulator.
         */
        this.gdsHost = HostPortUtility.LOCALHOST;

        /** MPCS-9695 5/18/18: Add shutdown hook to all RawInputConnections */
        Runtime.getRuntime().addShutdownHook(new Thread(new ConnectionQuitSignalHandler(this)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void exitCleanly() {
		logger.debug("Ctrl+C detected. Stopping ", getClass().getName());
		stopping = true;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#getMetadata()
	 */
	@Override
	public RawInputMetadata getMetadata() {
		return new RawInputMetadata();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#closeConnection()
	 */
	@Override
	public void closeConnection() throws IOException {
		this.stopping = true;
		String cs = getConnectionString();

		if (cs == null) {
			cs = connectionString;
		}

		messenger.sendDisconnectMessage(cs);
	}


	/**
	 * {@inheritDoc}
	 * 
	 * @see
	 * jpl.gds.telem.input.api.connection.IRawInputConnection#addConnectionListener
	 * (jpl.gds.telem.input.api.connection.RawInputConnectionStatusListener)
	 */
	@Override
	public void addConnectionListener(final RawInputConnectionStatusListener listener)    {
        // Overridden by subclasses; nothing to do here.
	}


	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#
	 * removeConnectionListener
	 * (jpl.gds.telem.input.api.connection.RawInputConnectionStatusListener)
	 */
	@Override
	public void removeConnectionListener(
	        final RawInputConnectionStatusListener listener)
    {
        // Overridden by subclasses; nothing to do here.
	}


	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.telem.input.api.connection.IRawInputConnection#reconnect()
	 */
	@Override
	public void reconnect() throws RawInputException
    {
        // Overridden by subclasses; nothing to do here.
	}


    /**
     * Overridden by subclasses if necessary.
     *
     * @return True if EOF on stream
     */
    @Override
    public boolean needsEofOnStreamStatus()
    {
        return false;
    }
    
    /**                                                                                                                                                               
     *
     * @see jpl.gds.telem.input.api.connection.IRawInputConnection#shouldReconnectOnIOException(boolean)                                                                                
     *                                                                                                                                                                
     * MPCS-4632 - 10/13/16 - Added. By default connection should
     *          still have the same logic as when this choice was made by RawInputHandler                                                                             
     */
    @Override
    public boolean shouldReconnectOnIOException(final boolean processorEofStatus){

        // MPCS-5013  07/29/13 EOF trumps connected
        // Lost server, must reconnect.                                                                                                                                   
        // Used for CLIENT_SOCKET connections to detect lost server.                                                                                                      

        //Reversed true/false from RawInputHandler. Same logic, but easier to read.                                                                                       

        final boolean eof = processorEofStatus && needsEofOnStreamStatus();

        if(eof || !isConnected()){
            return true;
        }
        else{
            return false;
        }
    }

	/**
	 * Shutdown hook for the use of this class and its inheritors
	 *
	 * We cannot use QuitSignalHandler as apps have their own shutdown hook and we can't control the order in which
	 * these run, so we can run the risk of the Spring context being alrady closed
	 *
	 * MPCS-10617 - 06/04/19 - Added class
	 */
	 protected class ConnectionQuitSignalHandler implements Runnable{

		private final IQuitSignalHandler handler;

		/**
		 * Constructor
		 * @param handler The application IQuitSignalHandler
		 */
		ConnectionQuitSignalHandler(final IQuitSignalHandler handler) {
			this.handler = handler;
		}

		@Override
		public void run() {
			handler.exitCleanly();
		}
	}

}
