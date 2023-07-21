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

import jpl.gds.common.config.mission.RealtimeRecordedConfiguration;
import jpl.gds.common.types.EhaBool;
import jpl.gds.common.types.RecordedBool;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.decom.DecomEngine;
import jpl.gds.decom.IDecomDelegate;
import jpl.gds.decom.IDecomListener;
import jpl.gds.decom.exception.DecomException;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.dictionary.api.decom.IChannelDecomDefinitionProvider;
import jpl.gds.dictionary.api.decom.IDecomMapDefinition;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.service.channel.IChannelPublisherUtility;
import jpl.gds.eha.api.service.channel.IChannelizationListener;
import jpl.gds.eha.api.service.channel.IDecomListenerFactory;
import jpl.gds.eha.api.service.channel.IGenericPacketDecomService;
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
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.SortedSet;

/**
 * This class listens for incoming packets message that contain data to be
 * decommed, decoms non-pre-channelized EHA data from them according to a decom
 * map, and publishes EhaChannel messages containing the extracted values.
 * 
 *
 */
public class GenericPacketDecomService implements IGenericPacketDecomService {
	
	// 07/28/16: Made some members protected for use by subclasses

	/** Tracer instance is okay for use by subclasses. */
    protected final Tracer trace; 


    /**
     * The mission-specific decom apids.
     */
    private SortedSet<Integer> decomApids;

    /**
     * The internal message publication bus.
     */
    protected final IMessagePublicationBus messageContext; 

    /**
     * R/T or recorded configuration for EVR and EHA.
     * Ok for use by subclasses.
     */
    protected final RealtimeRecordedConfiguration rtRec;

    
    private MessageSubscriber subscriber = null;
    private boolean started = false;
    
    /** Cached decom dictionary is okay for use by subclasses */
    protected IChannelDecomDefinitionProvider decomMapTable;
    private final StringBuilder sb;
    

    protected DecomEngine decomEngine;

    protected IChannelizationListener decomListener;

    /**
     * Channel publisher utility reference.
     */
    protected final IChannelPublisherUtility pubUtil;

    /** Channel definition provider */
    protected final IChannelDefinitionProvider chanTable;
    
    /**
     * Current application context.
     */
    protected ApplicationContext appContext;


    private final IContextConfiguration contextConfig;
    
    
    /**
     * Creates an instance of GenericPacketDecomService.
     * 
     * @param context
     *            the current application context
     */
    public GenericPacketDecomService(final ApplicationContext context) {

        this(context, TraceManager.getTracer(context, Loggers.TLM_EHA), context.getBean(IMessagePublicationBus.class),
                context.getBean(IChannelDefinitionProvider.class),  context.getBean(IContextConfiguration.class),
                context.getBean(IChannelPublisherUtility.class));
    }

    GenericPacketDecomService(ApplicationContext appContext, Tracer tracer, IMessagePublicationBus messagePublicationBus,
                                     IChannelDefinitionProvider channelDefinitionProvider, IContextConfiguration contextConfig,
                                     IChannelPublisherUtility pubUtil) {
        this.appContext = appContext;
        this.trace = tracer;
        this.messageContext = messagePublicationBus;
        this.chanTable = channelDefinitionProvider;
        this.contextConfig = contextConfig;
        this.pubUtil = pubUtil;

        sb = new StringBuilder(1024);

        setDecomApids(loadDecomApidsFromDictionary(this.appContext));


        RealtimeRecordedConfiguration temp = null;

        try {
            temp = this.appContext.getBean(RealtimeRecordedConfiguration.class);
        } catch (final Exception de) {
            trace.error("Unable to create RT/Rec configuration, all will be R/T: "
                    + ExceptionTools.rollUpMessages(de), de);
        }

        this.rtRec = temp;
    }

    /**
     * Sets the internal decom APIDs list.
     * 
     * @param sortedSet
     *            sorted set of decom APIDs
     */
    private void setDecomApids(final SortedSet<Integer> sortedSet) {

        this.decomApids = sortedSet;
    }
    /**
     * Loads the decom dictionary.
     * 
     * @throws DictionaryException
     *             if a dictionary error is encountered
     */
    public void loadDictionary()
            throws DictionaryException {

        /* Must set channel map into the decom parser */
        /* Updated to use the channel decom factory. */
        decomMapTable = appContext.getBean(IChannelDecomDefinitionProvider.class);
    }

    @Override
    public boolean startService() {

        try {
            loadDictionary();
        } catch (final DictionaryException e) {
            trace.error("Error accessing decom dictionary provider: " + ExceptionTools.getMessage(e), e);
            return false;
        }

        this.subscriber = new PacketMessageSubscriber();
        initializeDecom();

        this.started = true;
        trace.debug("Packet Decom Processor has started");
        return this.started;
    }
    
    /**
     * This method is intentionally separate from startService to enable subclasses to override
     * what {@link IDecomListener} implementation and {@link IDecomDelegate} instance to use
     * If overridden, subclasses must either make a call to the parent class's method or
     * initialize decomListener and decomEngine.
     */
    protected void initializeDecom() {
        final IDecomListenerFactory listenFactory = appContext.getBean(IDecomListenerFactory.class);
        decomListener = listenFactory.createChannelizationListener(appContext);
        decomEngine = new DecomEngine(appContext, decomListener);
        decomEngine.addListener(decomListener);
    }

    /**
     * Processes a telemetry PacketMessage by attempting to decom EHA from it
     * and sending out channel messages. If the packet is not a generic decom
     * EHA packet, this method will do nothing. The series of messages published
     * for the packet will include a start channel processing message, the eha
     * channel messages themselves, and an end of channel processing message.
     * All will have the same stream ID attached.
     * 
     * @param pm
     *            the PacketMessage containing the data to process
     */
    protected void handlePacketMessage(final ITelemetryPacketMessage pm) {
        final IDecomMapDefinition map;
        
        if (!contextConfig.accept(pm)) {
            return;
        }
        
        // if this packet doesn't contain decommable channels, ignore it
        if (pm.getPacketInfo().isFill()
                || !isGenericDecomEhaApid(pm.getPacketInfo().getApid())) {
            return;
        }

        final int apid = pm.getPacketInfo().getApid();
        map = this.decomMapTable.getDecomMapByApid(apid);

        if (map == null) {
            this.sb.append("Could not find decom map to process packet APID ");
            this.sb.append(apid);
            trace.warn(this.sb.toString());
            this.sb.setLength(0);
            return;
        }

        // 07/08/2016 - Use new decom engine / visitor pattern
        decomListener.setPacketInfo(pm.getPacketInfo());
        try {
			decomEngine.decom(map, pm.getPacket(), 0, pm.getPacketInfo().getSize() * Byte.SIZE);
		} catch (final DecomException e) {
			trace.error(String.format("Generic decom failure occurred while processing packet %s; cause: %s",
					pm.getPacketInfo().getIdentifierString(),
					e.getMessage()));
			// Currently, publishing any channel values that come out before the exception was raised. Consistent
			// with pre-R7.4 behavior.
		}

        final List<IServiceChannelValue> ehaList = decomListener.collectChannelValues();

        if (ehaList == null || ehaList.isEmpty()) {
            return;
        }

        pubUtil.assignPacketId(ehaList, pm);

        final String streamID = pubUtil.genStreamId("");

        /*
         * Using the new, wrapped publishing API.
         */

        final ITelemetryPacketInfo pi = pm.getPacketInfo();
        	final RecordedBool state = ((this.rtRec != null) ? this.rtRec.getState(
                EhaBool.EHA,  pi.getApid(), pi.getVcid(), pi.isFromSse()) : RecordedBool.REALTIME);

        pubUtil.publishFlightAndDerivedChannels(false, ehaList,
        			pm.getRct(), pi.getErt(), pi.getScet(), pi.getSclk(),
        			pi.getLst(), streamID, !state.get(), pi.getDssId(),
        			pi.getVcid(), null);
        	ehaList.clear();
        }
        
    /**
     * Returns a flag indicating whether the given apid is an decom apid.
     * 
     * @param apid
     *            the apid to check
     * @return true if the apid is a decom apid
     */
    private boolean isGenericDecomEhaApid(final int apid) {

        if (this.decomApids == null) {
            return false;
        }
        return this.decomApids.contains(Integer.valueOf(apid));
    }

    @Override
    public void stopService() {

        if (this.subscriber != null) {
            this.messageContext.unsubscribeAll(this.subscriber);
        }

        trace.debug("Packet Decom Processor has shut down");

        this.started = false;
    }

    /**
     * PacketMessageSubscriber is the listener for internal packet messages, 
     * both for telemetry packets and NEN status packets.
     * 
     */
    private class PacketMessageSubscriber extends BaseMessageHandler {

        /**
         * Creates an instance of PacketMessageSubscriber.
         */
        public PacketMessageSubscriber() {

            super();

            GenericPacketDecomService.this.messageContext.subscribe(TmServiceMessageType.TelemetryPacket, this);
        }

        /**
         * Process messages one by one.
         * 
         * @param m
         *            Next message to handle
         */
        @Override
        public void handleMessage(final IMessage m) {

            
            if (m.isType(TmServiceMessageType.TelemetryPacket)) {
                handlePacketMessage((ITelemetryPacketMessage) m);
            } 
        }
    }

    /**
     * Loads decom apids from the ApidReference for the current mission.
     * 
     * @return and integer set of apids, or null if the ApidReference could not
     *         be created.
     */
    private SortedSet<Integer> loadDecomApidsFromDictionary(final ApplicationContext context) {

        SortedSet<Integer> results = null;
        try {
            final IApidDefinitionProvider apidRef = context.getBean(IApidDefinitionProvider.class);
            if (apidRef != null) {
                results = apidRef.getDecomApids();
            } else {
                // Failure to load apid reference will have already been logged
                results = new java.util.TreeSet<>();
            }

        } catch (final Exception e) {
            trace.error("Error loading decom APIDs: " + ExceptionTools.getMessage(e), e);
        }
        return results;
    }


}
