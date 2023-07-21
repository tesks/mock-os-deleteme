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
package jpl.gds.eha.impl.service.channel;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.RealtimeRecordedConfiguration;
import jpl.gds.common.types.EhaBool;
import jpl.gds.common.types.RecordedBool;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.service.channel.IChannelPublisherUtility;
import jpl.gds.eha.api.service.channel.IPrechannelizedAdapter;
import jpl.gds.eha.api.service.channel.IPrechannelizedPublisherService;
import jpl.gds.eha.api.service.channel.PrechannelizedAdapterException;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * This class listens for PacketMessages and extracts EHA data from them, and
 * publishes EhaChannel messages containing the extracted values.
 * 
 */
public class PrechannelizedPublisherService implements IPrechannelizedPublisherService {

    private final Tracer log; 

    private final IMessagePublicationBus messageContext;
    private IPrechannelizedAdapter ehaAdapter;
    private long ehaPacketCount = 0L;
    private MessageSubscriber subscriber = null;
    private boolean started = false;

    /**
     * R/T or recorded configuration for EVR and EHA.
     */
    private final RealtimeRecordedConfiguration rtRec;
    
    /**
     * The mission-specific EHA APIDs.
     */
    private SortedSet<Integer> ehaApids;
    
    private final IChannelPublisherUtility pubUtil;
    private final ApplicationContext serviceContext;
    private final IContextConfiguration contextConfig;
    
    /**
     * Constructor.
     * 
     * @param appContext
     *            the current application context
     */
    public PrechannelizedPublisherService(final ApplicationContext appContext)
    {
        super();
        
        this.messageContext = appContext.getBean(IMessagePublicationBus.class);
        this.pubUtil = appContext.getBean(IChannelPublisherUtility.class);
        this.serviceContext = appContext;
        this.contextConfig = appContext.getBean(IContextConfiguration.class);
        this.log = TraceManager.getTracer(appContext, Loggers.TLM_EHA);
        
        setEhaApids(loadApids(appContext));

        RealtimeRecordedConfiguration temp = null;

        try
        {
            temp = appContext.getBean(RealtimeRecordedConfiguration.class);
        }
        catch (final Exception de)
        {
        	de.printStackTrace();
            log.error("Unable to create RT/Rec configuration, all will be R/T: " +
                      ExceptionTools.rollUpMessages(de));
        }

        rtRec = temp;

        if (rtRec != null)
        {
            log.debug("Eha Publisher has started with RtRec configured");
        }
        else
        {
            log.debug("Eha Publisher has started");
        }
    }
    
    /**
     * Sets the list of EHA packet APIDS.
     * @param apids the array of apids.
     */
    private void setEhaApids(final SortedSet<Integer> apids) {
        ehaApids = apids;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.interfaces.IService#startService()
     */
    @Override
    public boolean startService() {
        
        ehaAdapter = null;
        try
        {
            ehaAdapter = serviceContext.getBean(IPrechannelizedAdapter.class);
        }
        catch (final Exception e)
        {
        //    e.printStackTrace();
            log.error("EHA adapter configuration error: " + e.getMessage());
            return false;
        } 
        subscriber = new EhaMessageSubscriber();
        
        started = true;
        return started;
    }

    /**
     * Processes a PacketMessage by attempting to extract EHA from it and
     * sending out channel messages. If the packet is not an EHA packet, this
     * method will do nothing. The series of messages published for the packet
     * will include a start channel processing message, the eha channel messages
     * themselves, and an end of channel processing message. All will have the
     * same stream ID attached.
     * 
     * @param pm the PacketMessage containing the data to process
     */
    private void handlePacketMessage(final ITelemetryPacketMessage pm) {
    	if (!contextConfig.accept(pm) || pm.getPacketInfo().isFill()) {
    		return;
    	}
    	
        if (!isEhaApid(pm.getPacketInfo().getApid())) { 
            return; 
        }
        
        List<IServiceChannelValue> ehaList = null;
        
        try {
            ehaList = ehaAdapter.extractEha(pm);
        } catch (final PrechannelizedAdapterException e) {
            log.warn("Error extracting channels from pre-channelized packet: " + e.getMessage());
        }

        if (ehaList == null) {
            return;
        }

        ehaPacketCount++;

        if (ehaList.isEmpty()) {
            return;
        }

        pubUtil.assignPacketId(ehaList, pm);

        final String streamID = pubUtil.genStreamId("");

		/**
		 *  Using the new, wrapped publishing API.
		 */

        final ITelemetryPacketInfo  pi    = pm.getPacketInfo();
        final RecordedBool state =
            ((rtRec != null)
                 ? rtRec.getState(EhaBool.EHA, pi.getApid(), pi.getVcid(), pi.isFromSse())
                 : RecordedBool.REALTIME);

        pubUtil.publishFlightAndDerivedChannels(false,
                ehaList,
                pm.getRct(),
                pi.getErt(),
                pi.getScet(),
                pi.getSclk(),
                pi.getLst(),
                streamID,
                ! state.get(),
                pi.getDssId(),
                pi.getVcid(),
                null);
        ehaList.clear();
    }


    /**
     * Retrieves the number of processed EHA packets.
     * 
     * @return the packet count
     */
    public long getEhaPacketCount() {
        return ehaPacketCount;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.interfaces.IService#stopService()
     */
    @Override
	public void stopService() {
        if (subscriber != null) {
            messageContext.unsubscribeAll(subscriber);
        }

        log.debug("Eha Publisher has shut down");
        
        started = false;
    }
    
    /**
     * Returns a flag indicating whether the given apid is an Eha apid.
     * @param apid the apid to check
     * @return true if the apid is an Eha apid
     */
    private boolean isEhaApid(final int apid) {
        if (ehaApids == null) {
            return false;
        }
        return ehaApids.contains(Integer.valueOf(apid));
    }


    /**
     * Loads Eha apids from the ApidReference for the current mission.
     * @return a sorted set of apids, or null if the ApidReference could not
     * be created.
     */
    private SortedSet<Integer> loadApids(final ApplicationContext context) {
        SortedSet<Integer> results = null;
        try {
            final IApidDefinitionProvider apidRef = context.getBean(IApidDefinitionProvider.class);
            if (apidRef != null) {
                results = apidRef.getChannelApids();
            } else {
                // Failure to load apid reference will have already been logged
                results = new TreeSet<Integer>();
            }

        } catch (final Exception e) {
            e.printStackTrace();
            log.error("Unable to load mission adaptation or APID dictionary", e);
        }
        return results;
    }


    /**
     * EhaMessageSubscriber is the listener for internal packet messages.
     * 
     */
    private class EhaMessageSubscriber extends BaseMessageHandler {

        /**
         * Creates an instance of EhaMessageSubscriber.
         */
        public EhaMessageSubscriber() {
            super();

            messageContext.subscribe(TmServiceMessageType.TelemetryPacket, this);
        }

        /**
         * Process messages one by one.
         * 
         * @param m Next message to handle
         */
        @Override
        public void handleMessage(final IMessage m) {
            if (m instanceof ITelemetryPacketMessage) {
                handlePacketMessage((ITelemetryPacketMessage) m);
            }
        }
    }
    
    /**
     * Set if the publisher has started.
     * @param val is started
     */
    public void setIsStarted(final boolean val)
    {
    	started = val;
    }
    
    /**
     * Returns whether the eha publisher has started.
     * @return is started
     */
    public boolean isStarted()
    {
    	return started;
    }
}
