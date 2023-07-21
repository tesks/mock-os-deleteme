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

import java.util.ArrayList;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader;
import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.mission.RealtimeRecordedConfiguration;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.types.RecordedBool;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.dictionary.api.channel.FrameHeaderFieldName;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.eha.api.channel.ChannelCategoryEnum;
import jpl.gds.eha.api.channel.IChannelValueFactory;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.config.EhaProperties;
import jpl.gds.eha.api.service.channel.IChannelPublisherUtility;
import jpl.gds.eha.api.service.channel.IFrameHeaderChannelizerService;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.Sclk;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.ITelemetryFrameMessage;

/**
 * FrameHeaderChannelizer is a message subscriber that listens for
 * TransferFrameMessages, and parses the frame metadata (or "header" data)
 * to be channelized. It then publishes the channelized header data,
 * and publishes them using the typical API provided by the
 * EhaPublisherUtility. FrameHeaderChannlizer is a
 * DownlinkService, and thus gets instantiated and set up by
 * the downlink initialization. FrameHeaderChannelizer relies on
 * a supplied Channel Definition Provider for the information on which header data
 * needs to be channelized and into what format (e.g. ASCII, float,
 * etc.).
 *
 * This is a feature being provided for MPCS 5.3.
 *
 *
 */
public class FrameHeaderChannelizerService extends BaseMessageHandler implements IFrameHeaderChannelizerService
{

    private final boolean                                       processIdle;


	private final Map<FrameHeaderFieldName, IChannelDefinition> fieldMap;


	private static final ISclk zeroSclk = new Sclk(0);

    /**
     * R/T or recorded configuration for EVR and EHA.
     * Set USE_RTREC to true to allow header channels to inherit their
     * RT/REC state from the parent.
     *
     */
    private static final boolean USE_RTREC = false;

    private final RealtimeRecordedConfiguration rtRec;

    private final IMessagePublicationBus messageBus;
    private final IChannelPublisherUtility pubUtil;
    private final IChannelValueFactory chanFactory;
    private final IContextConfiguration contextConfig;


    /**
     * Constructor.
     *
     * @param context
     *            the current application context
     */
    public FrameHeaderChannelizerService(final ApplicationContext context)
    {

        super();

        this.messageBus = context.getBean(IMessagePublicationBus.class);
        this.chanFactory = context.getBean(IChannelValueFactory.class);
        this.contextConfig = context.getBean(IContextConfiguration.class);

        this.pubUtil = context.getBean(IChannelPublisherUtility.class);

        TelemetryInputType inputType = context.getBean(IConnectionMap.class).getDownlinkConnection().getInputType();

        final IChannelDefinitionProvider chanTable = context.getBean(IChannelDefinitionProvider.class);

        final HeaderChannelTable ht = new HeaderChannelTable();

        ht.addDefinitions(chanTable.getChannelDefinitionMap().values());
        if (inputType.hasSfdus()) {
            ht.disableOtherChannelizersOfOverlaps(inputType);
        }
        fieldMap = ht.getFrmHdrFieldMapping();

        RealtimeRecordedConfiguration temp = null;

        try
        {
            if (USE_RTREC)
            {
                temp = context.getBean(RealtimeRecordedConfiguration.class);
            }
        }
        catch (final Exception de)
        {
            TraceManager.getDefaultTracer(context).error(
                "Unable to create RT/Rec configuration, all will be R/T: " +
                ExceptionTools.rollUpMessages(de), de);
        }

        rtRec = temp;
        processIdle = context.getBean(EhaProperties.class).enableIdleFrameHeaderChannels();
    }


	/**
	 * handleMessage is an overridden method to process the incoming
	 * TransferFrameMessage objects, and to subsequently publish the
	 * channelized data from the header part of the TransferFrameMessages.
	 *
	 * @param message Message object to be processed. In this
	 * 				  overridden method, assumed to be TransferFrameMessage.
	 */
    @Override
    public void handleMessage(final IMessage message)
    {

    	if (fieldMap.isEmpty()) {
            return;
        }

        final ITelemetryFrameMessage tfm = (ITelemetryFrameMessage) message;

        /** Filter out frames without proper station, vcid, or scid */
        if (!this.contextConfig.accept(tfm)) {
            return;
        }

        /* R8 Refactor - We should not be producing channels from idle frames unless
         * asked to.
         */
        if (tfm.getFrameInfo().isIdle() && !processIdle) {
            return;
        }

        final ITelemetryFrameHeader header = tfm.getFrameInfo().getHeader();

        final ArrayList<IServiceChannelValue> chanVals = new ArrayList<>();

        /*
         * fieldMap contains the entire set of frame
         * header fields that need to be channelized. If it's not in
         * there, we don't need to channelize the data.
         */
        for (final FrameHeaderFieldName field : fieldMap.keySet()) {
            final Object val = header.getFieldValue(field);

            if (val != null) {
                final IChannelDefinition def = fieldMap.get(field);
                final IServiceChannelValue chanVal = chanFactory.createServiceChannelValue(def);
                chanVal.setDn(val);
                chanVals.add(chanVal);

                chanVal.setPacketId(PacketIdHolder.UNSUPPORTED);

                chanVal.setChannelCategory(ChannelCategoryEnum.FRAME_HEADER);
            }

        }

        if (! chanVals.isEmpty())
        {
        	/*
        	 * Because we have some valid, channelized data, must
        	 * publish them. Because we're processing frame header
        	 * data, only ERT information is available, so use that
        	 * value. RCT will simply be the current time. SCLK time
        	 * values will simply be "zeros." SCET is set from ERT.
        	 */
        	/*  Use faster date formatter here */
        	final String streamId = pubUtil.genStreamId(tfm.getStationInfo()
        			.getErt().getFormattedErtFast(true));
            final IAccurateDateTime rct = new AccurateDateTime();
            final IAccurateDateTime ert = tfm.getStationInfo().getErt();
    		/*
    		 * Using the new, wrapped publishing API.
    		 */
            final int vcid  = tfm.getFrameInfo().getVcid();
            final RecordedBool state =
                ((rtRec != null)
                     ? rtRec.getState(Long.valueOf(vcid), false)
                     : RecordedBool.REALTIME);


            /**
             * Frame header channels get a reasonable SCET
             * so they can be like other header channels.
             */
            pubUtil.publishFlightAndDerivedChannels(
                false,
                chanVals,
                rct,
                ert,
                new AccurateDateTime(ert.getTime()), // SCET from ERT
                zeroSclk,
                null,
                streamId,
                ! state.get(),
                tfm.getStationInfo().getDssId(),
                vcid,
                null);
        }

    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.interfaces.IService#startService()
     */
    @Override
    public boolean startService()
    {

        messageBus.subscribe(TmServiceMessageType.TelemetryFrame, this);

        return true;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.interfaces.IService#stopService()
     */
    @Override
    public void stopService() {
        messageBus.unsubscribeAll(this);
    }
}
