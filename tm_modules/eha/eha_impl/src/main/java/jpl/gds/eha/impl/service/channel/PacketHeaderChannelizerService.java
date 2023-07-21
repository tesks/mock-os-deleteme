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
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.mission.RealtimeRecordedConfiguration;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.types.EhaBool;
import jpl.gds.common.types.RecordedBool;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.dictionary.api.channel.PacketHeaderFieldName;
import jpl.gds.eha.api.channel.ChannelCategoryEnum;
import jpl.gds.eha.api.channel.IChannelValueFactory;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.config.EhaProperties;
import jpl.gds.eha.api.service.channel.IChannelPublisherUtility;
import jpl.gds.eha.api.service.channel.IPacketHeaderChannelizerService;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * PacketHeaderChannelizerService is a message subscriber that listens for
 * PacketMessages, and parses the packet metadata (or "header" data) to be
 * channelized. It then publishes the channelized header data, and publishes
 * them using the typical API provided by the EhaPublisherUtility.
 * PacketHeaderChannelizer is a DownlinkService, and thus gets instantiated and
 * set up by the downlink initialization. PacketHeaderChannelizer relies on a
 * supplied Channel Definition Provider for the information on which header data
 * needs to be channelized and into what format (e.g. ASCII, float, etc.).
 *
 * This is a feature being provided for MPCS 5.3.
 *
 *
 * 4/29/15. Changes throughout to decouple from the
 *          packet header and frame info classes and go through the
 *          IPacketMessage and IPacketInfo interfaces for everything.
 *
 */
public class PacketHeaderChannelizerService extends BaseMessageHandler implements IPacketHeaderChannelizerService
{
    /* Moved Channel Processing Properties to EhaProperties */
    private final boolean                                        processFill;

    private final boolean                                        isSse;


	private final Map<PacketHeaderFieldName, IChannelDefinition> fieldMap;

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

    private final Tracer                                         log;

    private final IContextConfiguration contextConfig;

    /**
     * Constructor.
     *
     * @param context
     *            the current application context
     */
    public PacketHeaderChannelizerService(final ApplicationContext context)
    {

        super();

        this.messageBus = context.getBean(IMessagePublicationBus.class);
        this.pubUtil = context.getBean(IChannelPublisherUtility.class);
        this.chanFactory = context.getBean(IChannelValueFactory.class);
        this.log = TraceManager.getTracer(context, Loggers.TLM_HEADER);
        this.contextConfig = context.getBean(IContextConfiguration.class);
        isSse = context.getBean(SseContextFlag.class).isApplicationSse();

        TelemetryInputType inputType = context.getBean(IConnectionMap.class).getDownlinkConnection().getInputType();

        final IChannelDefinitionProvider chanTable = context.getBean(IChannelDefinitionProvider.class);

        final HeaderChannelTable ht = new HeaderChannelTable();
        if (inputType.hasSfdus()) {
            ht.disableOtherChannelizersOfOverlaps(inputType);
        }
        ht.addDefinitions(chanTable.getChannelDefinitionMap().values());
        fieldMap = ht.getPktHdrFieldMapping();

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
            TraceManager.getDefaultTracer().error(
                "Unable to create RT/Rec configuration, all will be R/T: " +
                ExceptionTools.rollUpMessages(de), de);
        }

        rtRec = temp;
        processFill = context.getBean(EhaProperties.class).enableFillPacketHeaderChannels();
    }

	/**
	 * handleMessage is an overridden method to process the incoming
	 * PacketMessage objects, and to subsequently publish the
	 * channelized data from the header part of the PacketMessages.
	 *
	 * @param message Message object to be processed. In this
	 * 				  overridden method, assumed to be PacketMessage.
	 */
	@Override
	public void handleMessage(final IMessage message)
	{

		if (fieldMap.isEmpty()) {
			return;
		}

		final ITelemetryPacketMessage pm = (ITelemetryPacketMessage) message;

		// Do not process fill packets unless configured to do so
		if (!contextConfig.accept(pm) || !processFill && pm.getPacketInfo().isFill()) {
		    return;
		}

		final PacketIdHolder packetId = pm.getPacketId();

		final List<IServiceChannelValue> chanVals = new ArrayList<>(9);

		/*
		 * fieldMap contains the entire set of packet
		 * header fields that need to be channelized. If it's not in
		 * there, we don't need to channelize the data.
		 */
		for (final PacketHeaderFieldName field : fieldMap.keySet()) {

			final Object val = pm.getPacketInfo().getFieldValue(field);

			if (val != null) {
				final IChannelDefinition def = fieldMap.get(field);
				final IServiceChannelValue chanVal = chanFactory.createServiceChannelValue(def);
				chanVal.setDn(val);
				chanVals.add(chanVal);

                chanVal.setPacketId(packetId);

                chanVal.setChannelCategory(
                    isSse ? ChannelCategoryEnum.SSEPACKET_HEADER : ChannelCategoryEnum.PACKET_HEADER);
			}

		}

        if (! chanVals.isEmpty())
        {
			/*
			 * Because we have some valid, channelized data, must
			 * publish them. Because we're processing packet header
			 * data, ERT, SCLK, and SCET are all available, so use
			 * those values. But RCT will simply be the current time.
			 */

			/* Use faster date formatter here */
			final String streamId = pubUtil.genStreamId(pm
					.getPacketInfo().getErt().getFormattedErtFast(true));
            final IAccurateDateTime rct = new AccurateDateTime();

			/*
			 * Using the new, wrapped publishing API.
			 */

            final ITelemetryPacketInfo  pi    = pm.getPacketInfo();
            final RecordedBool state =
                ((rtRec != null)
                     ? rtRec.getState(EhaBool.EHA,  pi.getApid(), pi.getVcid(), pi.isFromSse())
                     : RecordedBool.REALTIME);

            pubUtil.publishFlightAndDerivedChannels(false,
                    chanVals,
                    rct,
                    pi.getErt(),
                    pi.getScet(),
                    pi.getSclk(),
                    pi.getLst(),
                    streamId,
                    ! state.get(),
                    pi.getDssId(),
                    pi.getVcid(),
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

        messageBus.subscribe(TmServiceMessageType.TelemetryPacket, this);

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
