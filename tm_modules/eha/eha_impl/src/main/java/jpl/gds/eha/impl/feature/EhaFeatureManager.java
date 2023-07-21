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
package jpl.gds.eha.impl.feature;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.notify.NotificationProperties;
import jpl.gds.common.service.telem.AbstractTelemetryFeatureManager;
import jpl.gds.dictionary.api.client.evr.IEvrUtilityDictionaryManager;
import jpl.gds.eha.api.channel.ChannelLadInitializedEvent;
import jpl.gds.eha.api.channel.IChannelLad;
import jpl.gds.eha.api.feature.IEhaFeatureManager;
import jpl.gds.eha.api.service.alarm.IAlarmNotifierService;
import jpl.gds.eha.api.service.alarm.IAlarmPublisherService;
import jpl.gds.eha.api.service.channel.IChannelLadService;
import jpl.gds.eha.api.service.channel.IGenericPacketDecomService;
import jpl.gds.eha.api.service.channel.IGroupedChannelAggregationService;
import jpl.gds.eha.api.service.channel.IHybridGenericPacketDecomService;
import jpl.gds.eha.api.service.channel.IPrechannelizedPublisherService;
import jpl.gds.eha.api.service.channel.ISuspectChannelService;
import jpl.gds.shared.interfaces.IService;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;

/**
 * EhaFeatureManager handles everything required to initialize and shutdown the
 * EHA packet decom capability in the downlink process. This feature manager
 * handles downlink serviced for pre-channelized channel packet decom, generic
 * decom, channel alarm processing, the channel LAD, and the alarm notification
 * service (moved from LadKeeper to here).
 * 
 *
 */
public class EhaFeatureManager extends AbstractTelemetryFeatureManager implements IEhaFeatureManager {
 
    private boolean genericDecomIsEnabled = true;	
    private boolean genericEvrDecomIsEnabled = true;
    private boolean alarmIsEnabled = true;
    private IAlarmNotifierService alarmNotifier;
    private IGroupedChannelAggregationService channelAggregationService;

	/**
     * {@inheritDoc}
     */
	@Override
    public void enableGenericDecom(final boolean isEnabled) {
		genericDecomIsEnabled = isEnabled;
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    public void enableGenericEvrDecom(final boolean isEnabled) {
		genericEvrDecomIsEnabled = isEnabled;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void enableAlarmProcessing(final boolean isEnabled) {
	    alarmIsEnabled = isEnabled;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public boolean init(final ApplicationContext springContext) {
        log = TraceManager.getTracer(springContext, Loggers.DOWNLINK);
        setValid(false);
		if (!isEnabled()) {
			return true;
		}
		
		setValid(true);
	        
        
        //Setup the prechannelized publisher, which listens for packet messages and publishes ChannelValueMessages
        try {
            addService(springContext.getBean(IPrechannelizedPublisherService.class));
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
            log.error("Unable to initialize prechannelized packet decom service");
            setValid(false);
            return false;
        }
        
        //Setup the generic EHA decom processor, which also serves as publisher for decommed channel
        if (genericDecomIsEnabled) {

            try {
                IService decomProcessor;
            	if (genericEvrDecomIsEnabled) {
            		decomProcessor = springContext.getBean(IHybridGenericPacketDecomService.class); 
            		/** This service requires the EVR dictionary if EVRs enabled for the mission */
            		final IEvrUtilityDictionaryManager evrDict = springContext.getBean(IEvrUtilityDictionaryManager.class);
            		if (evrDict != null) {
            		    evrDict.loadFsw();
            		}
            	} else {
            		decomProcessor = springContext.getBean(IGenericPacketDecomService.class);
            	}

                addService(decomProcessor);

            } catch (final Exception e) {
                log.error(e.getMessage(), e);
                log.error("Unable to initialize generic packet decom service");
                setValid(false);
                return false;
            }

			
        }
        
        IService channelLad = null;
        
        //Setup the LAD (Latest Available Data) table to listen for channel values
        try {
            channelLad = springContext.getBean(IChannelLadService.class);
        } catch (final Exception e1) {
            log.error(e1.getMessage(), e1);
            log.error("Unable to initialize channel LAD service");
            setValid(false);
            return false;
        }
        addService(channelLad);

        //Setup the alarm publisher, which listens for ChannelValueMessages and publishes EHA channel messages
        //(we always instantiate this...if alarm processing is turned off, it's up to the alarm publisher to
        //act as a pass-through for channel value messages)
        try {
            final IAlarmPublisherService alarmPub = springContext.getBean(IAlarmPublisherService.class);
            alarmPub.enableCalculation(alarmIsEnabled);
            addService(alarmPub);
        } catch (final Exception e1) {
            log.error(e1.getMessage(), e1);
            log.error("Unable to initialize alarm publisher service");
            setValid(false);
            return false;
        }

        // Setup the suspect channel publication service
        addService(springContext.getBean(ISuspectChannelService.class));

        final NotificationProperties notifyProps = springContext.getBean(NotificationProperties.class);
        // Start alarm notification
        if (notifyProps.isRealtimeAlarmNotificationEnabled()
                || notifyProps.isRecordedAlarmNotificationEnabled()) {

            try {
                alarmNotifier = springContext.getBean(IAlarmNotifierService.class);
                addService(alarmNotifier);

            } catch (final Exception e) {
                log.error("No alarm notification will be done.", e);
            }

        }


        /*
         * Start Grouped Channel Aggregation Service
         */
        try {
            channelAggregationService = springContext.getBean(IGroupedChannelAggregationService.class);
            addService(channelAggregationService);
        } catch (final Exception e) {
            log.error("No channel aggregation will be done.", e);
        }

        
        
        /*
         * Record validity of Eha Feature Manager as a logical AND of all related
         * services successfully starting.
         */
        setValid(startAllServices());
		
		if (isValid()) {
			log.debug("EHA/Channel Decom feature successfully initialized");
		}
		
        /**
         * Once the eha feature is ready to go, send out an event
         * so that it can be boot straped.
         * 
         * Move alarm history trigger to ChannelLad bootstrapper
         * to avoid triggering if no channels were bootstrapped
         */
		final IChannelLad lad = springContext.getBean(IChannelLad.class);
		springContext.publishEvent(new ChannelLadInitializedEvent(this, lad)); 


		return isValid();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.common.service.telem.AbstractTelemetryFeatureManager#shutdown()
	 */
	@Override
    public void shutdown() {
		if (!isEnabled()) {
			return;
		}
		super.shutdown();
			
		log.debug("EHA/Channel Decom feature has shutdown");
	}


	/**
     * {@inheritDoc}
     */
	@Override
    public ISuspectChannelService getSuspectChannelService() {
		return (ISuspectChannelService)getService(ISuspectChannelService.class);
	}
	
	
	/*  Need to access alarm notifier from AbstractDownShell */
	/**
     * {@inheritDoc}
     */
	@Override
    public IAlarmNotifierService getAlarmNotifier() 
	{
		return alarmNotifier;
	}
	
}
