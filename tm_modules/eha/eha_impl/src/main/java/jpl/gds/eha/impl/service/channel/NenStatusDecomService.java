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

import org.springframework.context.ApplicationContext;

import jpl.gds.decom.DecomEngine;
import jpl.gds.decom.exception.DecomException;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.decom.IChannelDecomDefinitionProvider;
import jpl.gds.dictionary.api.decom.IDecomMapDefinition;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.service.channel.IChannelPublisherUtility;
import jpl.gds.eha.api.service.channel.IChannelizationListener;
import jpl.gds.eha.api.service.channel.IDecomListenerFactory;
import jpl.gds.eha.api.service.channel.INenStatusDecomService;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.Sclk;
import jpl.gds.station.api.StationMessageType;
import jpl.gds.station.api.earth.message.INenMonitorMessage;

/**
 * This class listens for incoming NEN Status Packet Messages, decoms station
 * data from them according to a decom map, and publishes EhaChannel messages
 * (for M-channels) containing the extracted values.
 * 
 */
public class NenStatusDecomService implements INenStatusDecomService {

    private final Tracer trace;

    
    /**
     * Dummy SCLK/SCET for channels decommed from status packets.
     */
    private static final IAccurateDateTime zeroScet = new AccurateDateTime(true); 
    private static final ISclk zeroSclk = new Sclk(0, 0); 

    private final IMessagePublicationBus messageContext;
    

    private MessageSubscriber subscriber = null;
    private boolean started = false;
    private IChannelDecomDefinitionProvider decomMapTable;
    private final StringBuilder sb;

    private DecomEngine decomEngine;
    
    private IChannelizationListener decomVisitor;
    
    private final IChannelPublisherUtility pubUtil;
    
    private final ApplicationContext appContext;


    /**
     * Creates an instance of NenStatusDecomService.
     * 
     * @param context
     *            the current application context
     */
    public NenStatusDecomService(final ApplicationContext context) {
        
    	this.appContext = context;
        this.trace = TraceManager.getTracer(context, Loggers.TLM_MONITOR);
        this.messageContext = context.getBean(IMessagePublicationBus.class);
        this.pubUtil = context.getBean(IChannelPublisherUtility.class);

        sb = new StringBuilder(1024);

        trace.debug("Nen Status Decom Processor has started");
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
            e.printStackTrace();
            return false;
        }
        this.subscriber = new PacketMessageSubscriber();
        final IDecomListenerFactory listenFactory = appContext.getBean(IDecomListenerFactory.class);
        decomVisitor = listenFactory.createChannelizationListener(appContext);
        decomEngine = new DecomEngine(appContext, decomVisitor);
        decomEngine.addListener(decomVisitor);
        this.started = true;
        return this.started;
    }

    /**
     * Processes a RawNenStatusPacketMessage by attempting to decom EHA from it
     * and sending out monitor channel messages. The series of messages
     * published for the packet will include a start channel processing message,
     * the eha channel messages themselves, and an end of channel processing
     * message. All will have the same stream ID attached.
     * 
     * @param pm
     *            the RawNenStatusPacketMessage containing the data to process
     */
    private void handleNenStatusMessage(final INenMonitorMessage pm) {

        final IDecomMapDefinition map = this.decomMapTable.getGeneralDecomMap();

        if (map == null) {
            this.sb.append("Could not find decom map to process NEN status packet");
            trace.warn(this.sb.toString());
            this.sb.setLength(0);
            return;
        }
        
        final List<IServiceChannelValue> ehaList = decomPacket(map, pm);


        if (ehaList == null || ehaList.isEmpty()) {
            return;
        }

        final String streamID = pubUtil.genStreamId("");
        
        pubUtil.publishFlightAndDerivedChannels(false, ehaList,
                new AccurateDateTime(), pm.getStationInfo().getErt(), zeroScet, zeroSclk,
                null, streamID, true, pm.getStationInfo().getDssId(),
                null, Boolean.FALSE);

        ehaList.clear();
    }

    /**
     * Decoms a RawNenStatusPacketMessage and extracts monitor channel values.
     * 
     * @param map the DecomMap for the NEN status packet
     * @param pm
     *            NEN status packet message to decom
     * @return list of channel values
     */
    private List<IServiceChannelValue> decomPacket(final IDecomMapDefinition map,
            final INenMonitorMessage pm) {

        final byte[] packetBytes = pm.getData();

        /*
         * 12/11/13   Add offset to skip LEOT header.
         * 07/08/2016 Use new decom engine / listener pattern
         */
        decomVisitor.setCurrentTimes(null, null, null, 0);
        try {
			decomEngine.decom(map, pm.getData(), 0,
					packetBytes.length * Byte.SIZE);
		} catch (final DecomException e) {
			sb.append("Generic decom failure occurred while processing nen status message packet: ")
			.append("Packet ERT=" + pm.getStationInfo().getErt())
			.append(", DSSID=")
			.append(pm.getStationInfo().getDssId())
			.append("cause: ");
			trace.error(sb.toString(), e);
			sb.setLength(0);
			// Currently publishing channels created before exception was thrown. Consistent
			// with pre-R7.4 behavior
		}
        return decomVisitor.collectChannelValues(); 
    }

    
    @Override
    public void stopService() {

        if (this.subscriber != null) {
            this.messageContext.unsubscribeAll(this.subscriber);
        }

        trace.debug("NEN Status Decom Processor has shut down");

        this.started = false;
    }

    /**
     * PacketMessageSubscriber is the listener for internal packet messages, 
     * both for raw NEN status packet messages.
     * 
     */
    private class PacketMessageSubscriber extends BaseMessageHandler {

        /**
         * Creates an instance of PacketMessageSubscriber.
         */
        public PacketMessageSubscriber() {

            super();

            NenStatusDecomService.this.messageContext.subscribe(StationMessageType.NenStationMonitor, this);
        }

        /**
         * Process messages one by one.
         * 
         * @param m
         *            Next message to handle
         */
        @Override
        public void handleMessage(final IMessage m) {

            // Reset the decom object to handle a new run
            
           if (m instanceof INenMonitorMessage) {
                handleNenStatusMessage((INenMonitorMessage) m);
            }
        }
    }
}
