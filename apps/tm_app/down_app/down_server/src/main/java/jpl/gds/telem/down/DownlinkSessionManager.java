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
package jpl.gds.telem.down;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import jpl.gds.telem.common.feature.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.GeneralProperties;
import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.service.ServiceConfiguration;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.service.telem.DownlinkSummary;
import jpl.gds.common.service.telem.ITelemetryFeatureManager;
import jpl.gds.common.service.telem.ITelemetrySummary;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.context.api.EnableRemoteDbContextFlag;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.SseDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinitionProvider;
import jpl.gds.eha.api.feature.IEhaFeatureManager;
import jpl.gds.eha.api.service.alarm.IAlarmNotifierService;
import jpl.gds.eha.api.service.channel.ISuspectChannelService;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.portal.IMessagePortal;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.session.message.EndOfSessionMessage;
import jpl.gds.session.message.SessionHeartbeatMessage;
import jpl.gds.session.message.StartOfSessionMessage;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.email.EmailCenter;
import jpl.gds.shared.exceptions.ApplicationException;
import jpl.gds.shared.file.FileUtility;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.metadata.InvalidMetadataException;
import jpl.gds.shared.performance.PerformanceSummaryPublisher;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.SclkScetConverter;
import jpl.gds.shared.time.SclkScetUtility;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.telem.common.app.mc.IRestfulTelemetryApp;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.service.ITelemetryInputService;

/**
 * DownlinkSessionManager handles the setup, execution, and tear down of a
 * downlink processing session. It is responsible for starting the database
 * stores, the session heartbeat, all of the telemetry processing features
 * required by the downlink configuration, the frame, packet, and product
 * meters, and the performance summary publisher. Then it is responsible for
 * starting the processing of the telemetry input stream. When the session is
 * terminated, it stops all of the publishers, features, and data stores it
 * started.
 * 
 * 01/07/16 - Added ability to clear the buffer containing unprocessed (raw) data.
 */
public class DownlinkSessionManager implements MessageSubscriber {

    private final Tracer                       log;

    private final IContextConfiguration contextConfig;
    private ITelemetryInputService rawInputHandler;
    private IAccurateDateTime startTime;
    private final IMessagePublicationBus context;
    private DownlinkSummary                      summary;
    private Timer heartbeatTimer;
    private final boolean standalone;
    private final IMessagePortal jmsPortal;
    private final AtomicBoolean endOfDataReceived = new AtomicBoolean(false);
    private ServiceConfiguration              serviceConfiguration;
    
    /*
     * Feature managers are now
     * created as local variables (except the EHA manager) 
     * and added to a list of feature managers which is a member 
     * variable.
     */
    private final List<ITelemetryFeatureManager> allFeatureManagers = new LinkedList<>();
    private IEhaFeatureManager ehaManager;
    private final DownlinkAppProperties featureSet;
    private final long meterInterval;
    private boolean ended = true;
    private PerformanceSummaryPublisher performancePublisher;
    private final ApplicationContext appContext;
    private IDbSqlArchiveController archiveController;
    private final IStatusMessageFactory statusMessageFactory;
    private final SseContextFlag                 sseFlag;

    /**
     * Creates an instance of DownlinkSessionManager.
     * 
     * @param inputContext the current application context
     * @param contextConfig the IContextConfiguration object configured by the user's
     *        input
     * @param runningStandalone true if this session is NOT running as part of
     *        the integrated GUI
     */
    public DownlinkSessionManager(final ApplicationContext inputContext, final IContextConfiguration contextConfig, final boolean runningStandalone) {
        appContext = inputContext;
        this.contextConfig = contextConfig;
        this.meterInterval = inputContext.getBean(DownConfiguration.class).getMeterInterval();
        featureSet = inputContext.getBean(DownConfiguration.class).getFeatureSet();
        context = inputContext.getBean(IMessagePublicationBus.class);
        standalone = runningStandalone;
        statusMessageFactory = inputContext.getBean(IStatusMessageFactory.class);
        jmsPortal = inputContext.getBean(IMessagePortal.class);
        context.subscribe(CommonMessageType.EndOfData, this);       
        log = TraceManager.getTracer(inputContext, Loggers.DOWNLINK);
        sseFlag = inputContext.getBean(SseContextFlag.class);
    }

    /**
     * Sets up everything needed to run the session. Initializes the feature
     * managers, output directory, statistics meters, debugging objects, and Raw
     * Input adapter.
     * 
     * @return true if initialization was successful
     * @throws DictionaryException 
     * @throws ApplicationException 
     * @throws InvalidMetadataException 
     */
    private boolean initSession() throws ApplicationException {
        
        try {
            archiveController = appContext.getBean(IDbSqlArchiveController.class);
        } catch (final Exception e) {
            log.error("Error instantiating archive controller: " + e.toString());
            throw new ApplicationException(e);
        }
        
        
        final TelemetryInputType inputType = 
        		contextConfig.getConnectionConfiguration().getDownlinkConnection().getInputType();
        
        if (inputType.needsFrameSync() && !featureSet.isEnableFrameSync()) {
            log.warn("The input data type requires framesync but it is not enabled in your configuration; no telemetry will be processed");
        }

        if (inputType.needsPacketExtract() && !featureSet.isEnablePacketExtract()) {
            log.warn("The input data type requires packet extraction but it is not enabled in your configuration; no packets will be processed");
        }
        
        /*
         * Feature managers are now
         * created as local variables and added to a list of
         * feature managers which is a member variable.
         */
        // Create and enable/disable feature managers based upon the feature set
        // for this session
        final FrameFeatureManager frameManager = new FrameFeatureManager();
        allFeatureManagers.add(frameManager);
        final PacketFeatureManager packetManager = new PacketFeatureManager();
        allFeatureManagers.add(packetManager);
        final HeaderChannelizationFeatureManager headerChannelManager = new HeaderChannelizationFeatureManager();
        allFeatureManagers.add(headerChannelManager);
        final ITelemetryFeatureManager evrManager = new EvrFeatureManager();
        allFeatureManagers.add(evrManager);
        final ITelemetryFeatureManager pduManager = new PduExtractionFeatureManager();
        allFeatureManagers.add(pduManager);
        
        /* Do not add EHA manager to the list of feature
         * managers here. It must be done last.
         */
        ehaManager = appContext.getBean(IEhaFeatureManager.class);
    
        final ITelemetryFeatureManager productGenManager = new ProductGeneratorFeatureManager();
        allFeatureManagers.add(productGenManager);
        final ITelemetryFeatureManager timeCorrManager = new TimeCorrelationFeatureManager();
        allFeatureManagers.add(timeCorrManager);

        frameManager.setEnableFrameSync(featureSet.isEnableFrameSync() && inputType.needsFrameSync());
        frameManager.setEnableFrameTracking(inputType.hasFrames());
        frameManager.enable(frameManager.isEnableFrameSync() || frameManager.isEnableFrameTracking());
        
        packetManager.setEnablePacketExtract(featureSet.isEnablePacketExtract() && inputType.needsPacketExtract());
        packetManager.setEnablePacketTracking(true);
        packetManager.enable(packetManager.isEnablePacketExtract() || packetManager.isEnablePacketTracking());
        headerChannelManager.enable(featureSet.isEnableAnyHeaderChannelizer());
        headerChannelManager.setFrameHeaderChannelsEnabled(featureSet.isEnableFrameHeaderChannelizer());
        headerChannelManager.setPacketHeaderChannelsEnabled(featureSet.isEnablePacketHeaderChannelizer());
        
        /** 
         * Set SFDU header channelization flag into the
         * header channelization feature manager. 
         */
        headerChannelManager.setSfduHeaderChannelsEnabled(featureSet.isEnableSfduHeaderChannelizer() && inputType.hasSfdus());
        evrManager.enable(featureSet.isEnableEvrDecom());
        pduManager.enable(featureSet.isEnablePduExtract());
        ehaManager.enable(featureSet.isEnablePreChannelizedDecom());
        ehaManager.enableGenericDecom(featureSet.isEnableGenericChannelDecom());
        ehaManager.enableGenericEvrDecom(featureSet.isEnableGenericEvrDecom());
        ehaManager.enableAlarmProcessing(featureSet.isEnableAlarms());
        productGenManager.enable(featureSet.isEnableProductGen() && !sseFlag.isApplicationSse());
        timeCorrManager.enable(featureSet.isEnableTimeCorr() && inputType.hasFrames());

        // Starting with AMPCS R3, we now allow additional feature managers that
        // can simply be configured (need arose from supporting de-ssl's NEN/SN
        // interface, or directive issuance).

        
        /**
         * check if the monitor needs to be loaded if the NEN or DSN misc
         * features are set up and enabled.
         */
        boolean enableMonitorDict = false;

        if(featureSet.getMiscFeatures() != null) {
        	// Begin factory-style instantiations of misc features.
	        for (final String miscFeatureClassName : featureSet.getMiscFeatures()) {
	            Class<?> c = null;
	            ITelemetryFeatureManager dfm = null;
	
	            /*
	             * Added exception to the error
	             * log messages below. Error is unanticipated and cannot be caused by
	             * any incorrect user action. The exception should be dumped.
	             */		     
	            try {
	                c = Class.forName(miscFeatureClassName);
	                dfm = (ITelemetryFeatureManager) c.newInstance();
	                log.trace("Successfully instantiated downlink feature " + miscFeatureClassName);
	                
	                /**
	                 * check if the monitor needs to be loaded if the NEN or DSN misc
	                 * features are set up and enabled.
	                 */
	                enableMonitorDict = enableMonitorDict 
	                		|| dfm instanceof NenStatusDecomFeatureManager 
	                		|| dfm instanceof DsnMonitorChannelizationFeatureManager;
	
	            } catch (final ClassNotFoundException |InstantiationException | IllegalAccessException e) { 
	                log.error("Class " + miscFeatureClassName
	                        + " could not be instantiated: " + e.getMessage(), e);
	                continue;
	
	            } 
	            allFeatureManagers.add(dfm);
                if (dfm instanceof RecordedEngineeringFeatureManager) {
                    dfm.enable(featureSet.isEnableMiscFeatures() && productGenManager.isEnabled());
                } else {
                    dfm.enable(featureSet.isEnableMiscFeatures());
                }
	        }
        }    
        

        /**
         * Now add the EHA feature manager. This
         * must be added after all the feature managers that may load channel
         * dictionaries, because this manager loads the alarm dictionary.
         *
         * TODO IT IS LESS THAN DESIREABLE THAT THE ALARM CAPABILITY DOES NOT HAVE
         * ITS OWN FEATURE MANAGER. If it was separate, we could just make
         * sure that one was added last.
         *
         */
        allFeatureManagers.add(ehaManager);
        
        /**
         * Enable all required dictionaries.  This should enable
         * everything that could be used and the feature managers will be responsible for loading
         * the required dictionaries.  Using the isSse property and the feature managers to enable and load
         * all the required dictionaries for the downlink up front before anything is initialized.
         * 
         * Note that the monitor enabling is not done here since the dsn channelizing is a misc feature.
         * 
         * TODO right now there are two load strategies, one for flight one for SSE.  Maybe these should
         * be merged at some point.
         */
        try {
            final boolean isSse = sseFlag.isApplicationSse();
            log.debug(getClass().getName(), " isSse ?", isSse);
			if (isSse) {
			    /* Enable alarm and decom dictionary. SSE users are people too. */
				appContext.getBean(SseDictionaryLoadingStrategy.class)
				.enableApid()
				.setHeader(headerChannelManager.isEnabled())
				.setEvr(evrManager.isEnabled())
				.setChannel(ehaManager.isEnabled())			
				.setAlarm(ehaManager.isEnabled())
                .setDecom(ehaManager.isEnabled())
				.loadAllEnabled(appContext, false);
			} else {
				appContext.getBean(FlightDictionaryLoadingStrategy.class)
				.enableApid()
				.setFrame(frameManager.isEnabled())
				.setHeader(headerChannelManager.isEnabled())
				.setEvr(evrManager.isEnabled())
				.setCommand(evrManager.isEnabled())
				.setSequence(evrManager.isEnabled())
				.setChannel(ehaManager.isEnabled())
				.setAlarm(ehaManager.isEnabled())
				.setDecom(ehaManager.isEnabled())
				.setMonitor(enableMonitorDict)
				.setProduct(productGenManager.isEnabled())
				.loadAllEnabled(appContext, false, true);
			}
		} catch (final Exception e) {
			throw new ApplicationException("Failed to load all required dictionaries", e);
		}
        
        // Create the session output directory. Products, reports, SCMFs, log,
        // and
        // debug files will go in this directory
        setupSessionOutputDirectory();

        /*
         * Start the performance publisher (replaces
         * backlog summary publisher).
         */
        startPerformancePublisher();
  
        /*
         * Feature managers are now
         * created all on one list, so they can be started by this
         * loop instead of one by one.
         */
        // Now initialize all the feature managers
        boolean ok = false;
        for (final ITelemetryFeatureManager fm: allFeatureManagers) {
            ok = fm.init(appContext);
            if (!ok) {
                log.error("Startup of downlink service " + fm + " failed");
                throw new ApplicationException("Startup of downlink service " + fm + "failed");
            } else {
                log.debug("Started feature manager: " + fm);
            }
        }

        /*
         * SCLK/SCET now initialized at start of session and version logged.
         * Failure to load SCLK/SCET file is now FATAL!
         */
        if (ok) {
            final int scid = contextConfig.getContextId().getSpacecraftId();
            SclkScetConverter sclkScetConverter = SclkScetUtility.getConverterFromSpacecraftId(scid, log); // add new
                                                                                                           // method to
                                                                                                           // pass in                                                                                                         // trace
            // remove print when sclkScetConverter is not null, now printed within getConverterFromSpacecraftId
            if (sclkScetConverter == null) {
            	/*
            	 * If the project sclkscet file did not load
            	 * try load the default one (0)
            	 */
                log.warn(Markers.TIME_CORR, "Could not open SCLK/SCET file "
                        + FileUtility.createFilePathLogMessage(GdsSystemProperties.getMostLocalPath("sclkscet."
                                + scid, sseFlag.isApplicationSse()))
                        + " (for SCID=" + scid + ") loading default sclkscet file...");
                sclkScetConverter = SclkScetUtility.getConverterFromSpacecraftId(0, log);
            }
            if (sclkScetConverter == null){
                log.error(Markers.TIME_CORR, "Could not open SCLK/SCET file "
                        + FileUtility.createFilePathLogMessage(GdsSystemProperties.getMostLocalPath("sclkscet."
                                + 0, sseFlag.isApplicationSse())));
                ok = false;
            }
            else {
                log.debug(Markers.TIME_CORR, " Successfully instantiated SCLK/SCET file ",
                          sclkScetConverter.getFilename(), " for scid ", scid);
            }
        }

        // Now start up the input adapter that will read the telemetry
        if (ok) {
			ok = startRawInput();
        }
        return ok;
    }

    /**
     * Cleans up after a session. Everything done from this point must be
     * re-entrant.
     * 
     */
    private void shutdownSession() {

        /*
         * Feature managers are now
         * all in a list, so they can be shutdown with this loop
         * instead of one by one.
         */
        for (final ITelemetryFeatureManager fm: allFeatureManagers) {
            log.debug("Stopping feature manager " + fm);
            fm.stopAllServices();
            log.debug("Shutting down ", fm.getClass().getName());
            if (summary != null) {
                fm.populateSummary(summary);
            }
            fm.clearAllServices();
        }

        // Close ancillary workers

        EmailCenter.closeAll();
    }

    /**
     * Sets up the telemetry input service for reading the downlink data stream.
     * 
     * @return true if the input service is successfully initialized
     */
    public boolean startRawInput() {

    	final TelemetryInputType inputType = appContext.getBean(IConnectionMap.class).getDownlinkConnection().getInputType();

        if (inputType.hasFrames()) {
            try {
            	appContext.getBean(ITransferFrameDefinitionProvider.class);
            } catch (final Exception e1) {
                log.error("Unable to start raw input because could not load transfer frame dicitonary", e1);
                return false;
            }
        }

        appContext.getBean(EnableRemoteDbContextFlag.class).setRemoteDbEnabled(featureSet.isOngoingDbMode());
        
        rawInputHandler = appContext.getBean(ITelemetryInputService.class);
        if (rawInputHandler.startService()) {
            rawInputHandler.setMeterInterval(meterInterval);
            log.debug("TelemetryInputService for ", inputType, " successfully created. Using handler ",
                      rawInputHandler.getClass().getName(), " with interval=", meterInterval);
            return true;
        } else {
            return false;
        }
         
    }

    /**
     * Starts the performance summary publisher.
     * 
     */
    private synchronized void startPerformancePublisher() {
    	performancePublisher = appContext.getBean(PerformanceSummaryPublisher.class);
    	
    	/**
    	 * Look up the configured intervals from the configuration.
    	 */
    	final int interval =  appContext.getBean(PerformanceProperties.class).getSummaryInterval();
    	performancePublisher.start(interval);
    }

    /**
     * Stops the performance summary publisher.
     * 
     */
    private synchronized void stopPerformancePublisher() {
        if (performancePublisher != null) {
        	performancePublisher.stop();
        	performancePublisher = null;
        }
    }

    /**
     * Processes telemetry until told to stop or the data runs out.
     * 
     * @return true if input processing is successfully started and terminates
     *         without error, false if there is an exception processing
     *         telemetry
     * @throws RawInputException could not process raw input
     * 
     */
    public synchronized boolean processInput() throws RawInputException
    {
        if (ended)
        {
            log.error("Begin reading raw input after end");
            return false;
        }

        if (rawInputHandler == null)
        {
            log.error("No raw input handler");
            return false;
        }

        try {
            log.debug("Begin reading telemetry input");
            if (rawInputHandler.connect()) {
                rawInputHandler.startReading();

                while (!this.endOfDataReceived.get()) {
                    synchronized(this.endOfDataReceived) {
                        this.endOfDataReceived.wait();
                    }
                }
                rawInputHandler.stopReading();
                // removed this rawInputHandler disconnect call.
                log.debug("Stopped reading telemetry input");
            }
        } catch (final RawInputException rie) {
            log.error("Could not read from raw input: " + rie.getMessage());
            log.error("There was a problem processing the input data or " +
					"accessing the input source");
			
			/* Throw raw input exception to
			 * capture that specific error code */
			throw rie;			
        } catch (final Exception e) {
        	/* Added general catch so we won't throw all the way out and freeze the app. */
        	log.error("Unexpected processing error (" + e.toString() + ")", e);
        	return false;
        }

        return true;
    }

    /**
     * Starts the database stores for the session. This creates the session in
     * the database.
     *
     * @throws DatabaseException could not initialize database
     *
     * Throw database exception to capture that specific error code.
     */
    public void startSessionDatabase() throws DatabaseException {
        try {
            startDatabase();
        } catch (final Exception e) {
            log.error("There was an error initializing the session database", e);
            throw new DatabaseException(e);
        }
    }

    /**
     * Starts a session. This means sending a start of session message and
     * starting the session heartbeat. This method also updates the session
     * configuration file on disk. This call does not start the database, which
     * must be accomplished by calling startDatabase() prior to invoking this
     * method.
     * 
     * @throws ApplicationException could not start session
     * 
     * Throw exception to capture error code later on.
     */
    public synchronized void startSession() throws ApplicationException {
        ended = false;
        try {

            // Initialize feature managers and meters
            final boolean ok = initSession();

            if (!ok) {
                shutdownSession();
                throw new ApplicationException();
            }

            // Prepare the object that will be populated with session statistics
            setupSessionSummary();

            // Send the session start message
            startTime = sendStartOfSessionMessage();

            // Add flag to call to control whether config is written
            IRestfulTelemetryApp.writeOutContextConfig(appContext, contextConfig, false);

            // Set the heartbeat interval from the config file
            final long heartbeatInterval = appContext.getBean(GeneralProperties.class).getContextHeartbeatInterval();
           
            // start the heartbeat message sender
            startHeartbeat(heartbeatInterval);

        } catch (final Exception e) {
            log.error("There was an error initializing the session", e);
            throw new ApplicationException(e);
        }
    }

    /**
     * Ends a session. This means stopping the session heartbeat, sending
     * an end of session message, displaying the session summary (command line
     * mode only), cleaning up after the downlink components, flushing out the
     * message service queue, and stopping the performance summary publisher.
     * 
	 * @param showSummary
	 *            true if the session summary should be displayed on the
     *        console, false if not
     * @return true if all session shutdown goes normally
     * 
	 *
     */
    public synchronized boolean endSession(final boolean showSummary) {
    	boolean ok = true;
    	if (ended) {
    	    log.debug("Session already ended in endSession");
    		return true;
    	}

    	final boolean enableMessaging = appContext.getBean(MessageServiceConfiguration.class).getUseMessaging();
        final boolean enableDb = appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class).getUseDatabase();
    	/*
    	 *  DO NOT UNDER ANY CIRCUMSTANCES CHANGE THE ORDER OF SHUTDOWN IN
    	 *  THIS METHOD WITHOUT PERMISSION OF THE COG-E. THIS ORDERING EXISTS FOR A REASON.
    	 */

        IAccurateDateTime endTime = null;
    	try {
            // Stop the raw input and send an end of test message
            // assuming we actually started a session
            // DO NOT stop heartbeat messages, it will break monitoring applications
            if (startTime != null) {
                stop();
            }

    		// Start sending performance summary messages more frequently
    		/*  Moved this BEFORE the service
    		 * shutdown begins, so that performance reporting is accelerated
    		 * during shutdown of all services.
    		 */
    		if (performancePublisher != null) {
	    	    /**
	    	     * Look up the configured intervals from the configuration.
	    	     */
    			final int shutdownInterval = appContext.getBean(PerformanceProperties.class).getShutdownSummaryInterval();
     			performancePublisher.setShutdownRate(shutdownInterval);
            }

    		// Shutdown feature managers and meters
    		shutdownSession();

    		// Send out summary log before the log database is closed. The
    		// summary has no end time but that's ok
    		getSummary();

            IPublishableLogMessage logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.INFO,
                    summary.toStringNoBlanks("SESSION SUMMARY: Session ID: "), LogMessageType.RAW_INPUT_SUMMARY);
            context.publish(logm);
            log.log(logm);

    		// Flush out any pending JMS messages
    		if (enableMessaging) {
                logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.INFO,
                        "Clearing message backlog", LogMessageType.PERFORMANCE);
    			context.publish(logm);
                log.log(logm);
    			jmsPortal.clearAllQueuedMessages();
    		}

            // Flush out and close the peripheral database connections
            if (enableDb) {
                logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.INFO, "Clearing database backlog",
                        LogMessageType.PERFORMANCE);
                context.publish(logm);
                log.log(logm);
                stopPeripheralDatabases();
            }

            // started a session
            if (startTime != null) {
                endTime = new AccurateDateTime();

                contextConfig.getContextId().setEndTime(endTime);

                // Populate summary including end time
                getSummary();

                // Flush out and close the non-session database connections
                if (enableDb) {
                    archiveController.updateSessionEndTime(contextConfig,
                            summary == null ? new DownlinkSummary() : summary);

                }
            }

            // Flush out and close the database connections
            if (enableDb) {
                logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.INFO,
                        "Shutting down remaining database stores.", LogMessageType.PERFORMANCE);
                context.publish(logm);
                log.log(logm);
                stopDatabase();
            }

    		/* Stop the performance publisher (replaces
    		 * backlog summary publisher).
    		 */
    		stopPerformancePublisher();

    		/* Display the long summary info to the console. This is different than the summary message,
    		 * which is a log message that goes to the database.
    		 */
    		if (showSummary) {
                log.info(Markers.INPUT_SUMMARY, summary);
    		}

    		/*
    		 * Send the end of test message and tell the message portal to flush it.
    		 */
    		if (startTime != null) {
                sendEndOfSessionMessage(startTime, endTime);
    			
    			if (enableMessaging) {
    				jmsPortal.clearAllQueuedMessages();
    				// R8 Refactor TODO - This seems unnecessary.  Everything should have been shutdown before the
    				// end of session message is sent. If there are log messages that come out afterwards,
    				// I don't see that as a huge issue.
    				//jmsPortal.flushEndOfSessionMessage();
    			}
    		}

    	} catch (final Exception e) {
    		log.warn("There was an anomaly shutting down the session: "
    				+ e.toString(), e);
    		ok = false;
    	}
    	ended = true;
    	return ok;
    }

    /**
     * Indicates whether the session has already ended.
     * 
     * @return true if the session has ended
     */
    public synchronized boolean isSessionEnded() {
        return ended;
    }

    /**
     * Pauses processing of telemetry input; the incoming data is thrown away while
     * paused.
     */
    public void pause() {
        if (this.rawInputHandler != null) {
            log.trace("Pausing Raw Input Handler");
            this.rawInputHandler.pause();
        }
    }

    /**
     * Resumes processing of telemetry input.
     */
    public void resume() {
        if (this.rawInputHandler != null) {
            log.trace("Resuming Raw Input Handler");
            this.rawInputHandler.resume();
        }
    }

    /**
     * Stops processing of telemetry input. The input stream is no longer read.
     * 
	 * Throws IllegalStateException if rawInputHandler is null
     */
    public void stop() {
        if (this.rawInputHandler != null) {
            log.trace("Stopping Raw Input Handler");
           this.rawInputHandler.stopService();
           
        }
        
        /**  R8 refactor TODO - The exception comes out too
         * often and leads someone doing a control-C to think the process did
         * not exit properly. Why is it necessary to so ANYTHING here?
         */
//        /*
//         * If stop is called early enough,
//         * rawInputHandler may not have been instantiated yet, but does not
//         * prevent it from being instantiated later and start ingesting data.
//         * Throw an error to show it was called at an inappropriate time
//         * Changed to log the error instead of
//         * throwing it
//         */
//        else {
//            log.error("DownlinkSessionManager - rawInputHandler does not exist");
//            throw new IllegalStateException("DownlinkSessionManager - rawInputHandler does not exist");
//        }
    }
    
	/**
	 * Clear the buffer within the telemetry input service InputStream
     *
	 * @throws IOException
	 *             if the <code>clearBufferCallable</code> threw an exception,
	 *             was interrupted while waiting, or could not be scheduled for
	 *             execution
	 */
    public void clearInputStreamBuffer() throws IOException {
    	if (this.rawInputHandler != null){
    		this.rawInputHandler.clearInputStreamBuffer();
    	}
    	else{
    		throw new IllegalStateException("DownlinkSessionManager: RawInputHandler is null at this time. No data to be cleared.");
    	}
        log.trace("Cleared Raw Input Handler buffer");
    }

    /**
     * Starts the session heartbeat timer. It is started for any standalone
     * downlink instance, or if running integrated and this is not the SSE
     * downlink instance.
     * 
     * @param heartbeatInterval the interval between heartbeats in milliseconds
     */
    private void startHeartbeat(final long heartbeatInterval) {
        if (!sseFlag.isApplicationSse() || standalone
                || (!((SessionConfiguration) contextConfig).getRunFsw().isFswDownlinkEnabled())) {
            log.info("Starting session heartbeat on a ", heartbeatInterval, " milisecond interval");
            /* Name the timer thread */
            heartbeatTimer = new Timer("Session Heartbeat Timer");
            heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    sendHeartbeatMessage();
                }
            }, heartbeatInterval, heartbeatInterval);
        }
    }

    /**
     * Stops the session heartbeat.
     * 
     */
    public void stopHeartbeat() {
        if (heartbeatTimer != null) {
            log.trace("Stopping session heartbeat");
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
    }


    /**
     * Publishes a start of session message.
     * 
     * @return the session start time
     */
    private IAccurateDateTime sendStartOfSessionMessage() {
        final IAccurateDateTime localStartTime = contextConfig.getContextId().getStartTime();
        final StartOfSessionMessage start = new StartOfSessionMessage(contextConfig);
              
        /*
         * Added service configuration to start of session message.
         */
        start.setServiceConfiguration(serviceConfiguration);


        final Integer vcid = contextConfig.getFilterInformation().getVcid();
        
        log.info(Markers.SESSION, "Start of Session [key=", contextConfig.getContextId().getNumber(), " javaVmPid=",
                 GdsSystemProperties.getJavaVmPid(), " dssId=", contextConfig.getFilterInformation().getDssId(),
                 " vcid=", ((vcid != null) ? vcid : "NOT APPLICABLE"), " start=",
                 TimeUtility.getFormatter().format(localStartTime), "] (", contextConfig.getContextId().getFullName(),
                 ")");

        context.publish(start);
        log.info(Markers.SESSION, start.getOneLineSummary());

        log.trace("Start of session message sent");
        return (localStartTime);
    }


    /**
     * Publishes a session heartbeat message.
     * 
     */
    private void sendHeartbeatMessage() {
        final SessionHeartbeatMessage heartbeat = new SessionHeartbeatMessage(contextConfig);
        /*
         * Added service configuration to heartbeat message.
         */
        heartbeat.setServiceConfiguration(serviceConfiguration);
        log.trace(heartbeat + ": (" + contextConfig.getContextId().getName() + ")");
        context.publish(heartbeat);
    }

    /**
     * Publishes an end of session message.
     * 
     * @param startTime the session start time, to include in the end message
     * @return the end of session message
     * 
     */
    private EndOfSessionMessage sendEndOfSessionMessage(final IAccurateDateTime startTime,
                                                        final IAccurateDateTime endTime) {

    	contextConfig.getContextId().setStartTime(startTime);
    	contextConfig.getContextId().setEndTime(endTime);
        final EndOfSessionMessage end = new EndOfSessionMessage(contextConfig.getContextId(), summary);
        context.publish(end);
        log.info(Markers.SESSION, end.getOneLineSummary());
        return end;
    }

    /**
     * Sets up the session summary object, which will eventually be populated
     * with statistics.
     * 
     */
    private void setupSessionSummary() {
        if (summary == null) {
            summary = new DownlinkSummary();
            summary.setFullName(contextConfig.getContextId().getFullName());
            summary.setOutputDirectory(contextConfig.getGeneralInfo().getOutputDir());

        }
    }

    /**
     * Prepares the output directory where session output will be written,
     * including log files, session reports, products, and debug information.
     * 
     */
    private void setupSessionOutputDirectory() {
        final String testPath = contextConfig.getGeneralInfo().getOutputDir();
        final File f = new File(testPath);
        if (!f.exists()) {
            f.mkdirs();
        }
    }

    /**
     * Populates the session summary object with data gathered during the
     * telemetry processing.
     * 
     * @param startTime the start time of the session
     * @param endTime the end time of the session
     */
    private void populateSummary(final IAccurateDateTime startTime, final IAccurateDateTime endTime) {
        if (summary == null) {
            summary = new DownlinkSummary();
        }
        summary.populateBasicSummary(startTime, endTime, contextConfig.getContextId().getFullName(),
                                     contextConfig.getGeneralInfo().getOutputDir(),
                                     contextConfig.getContextId().getNumber() != null
                                             ? contextConfig.getContextId().getNumber().longValue()
                                             : null);

    }

    /**
     * Retrieves the session summary object. This object is not complete until
     * an EndOfSession message has been published.
     * 
     * @return SessionSummary
     */
    public ITelemetrySummary getSummary() {
        populateSummary(contextConfig.getContextId().getStartTime(), contextConfig.getContextId().getEndTime());
        return summary;
    }

    /**
     * Starts up the database components and connections, and adds the session
     * configuration to the database.
     *
     * @throws DatabaseException
     *             could not start database

     * Throw database exception to capture that error code.
     */
    private synchronized void startDatabase() throws DatabaseException {
    	try {
            if (appContext.getBean(IMySqlAdaptationProperties.class)
                          .getUseDatabase()) {
                this.archiveController = appContext.getBean(IDbSqlArchiveController.class);
                this.archiveController.init();
                final boolean ok = this.archiveController.startAllStores();
		        if(!ok) {
		        	throw new DatabaseException();
		        }
            }
            else {
                log.debug(getClass().getName(), "Skipping database startup");
        	}
        } catch (final Exception e) {
        	throw new DatabaseException(e);
        }
    }

    /**
     * Stops the non-session database components and connections.
     */
    private void stopPeripheralDatabases() {
        if (this.archiveController != null) {
            this.archiveController.stopPeripheralStores();
        }
    }

    /**
     * Stops the database components and connections.
     * 
     */
    private void stopDatabase() {
        if (this.archiveController != null) {
            this.archiveController.shutDown();
        }
    }
    
    /**
     * Gets the SuspectChannelTable object from the EHA feature manager in this
     * downlink session manager object
     * 
     * @return SuspectChannelTable, or null if none has been initialized
     */
    public ISuspectChannelService getSuspectChannelService() {
        if (ehaManager != null) {
            return ehaManager.getSuspectChannelService();
        } else {
            return null;
        }
    }

    /* 1/30/14 - MPCS -5736: Need AlarmNotifierService access in chill_down
     * GUI */
    /**
     * Gets the AlarmNotifierService object from the EHA feature manager in this
     * downlink session manager object
     * 
     * @return AlarmNotifierService, or null if none has been initialized
     */
    public IAlarmNotifierService getAlarmNotifier() {
        if (ehaManager != null) {
            return ehaManager.getAlarmNotifier();
        } else {
            return null;
        }
    }

    @Override
    public void handleMessage(final IMessage message) {
        synchronized(this.endOfDataReceived) {
            this.endOfDataReceived.set(true);
            this.endOfDataReceived.notifyAll();
        }
    }

    @Autowired
    public void setServiceConfiguration(final ServiceConfiguration svcConfig) {
        this.serviceConfiguration = svcConfig;
    }
}
