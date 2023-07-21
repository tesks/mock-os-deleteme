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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.RealtimeRecordedConfiguration;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.types.EhaBool;
import jpl.gds.common.types.RecordedBool;
import jpl.gds.context.api.EnableLstContextFlag;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.eha.api.channel.ChannelCategoryEnum;
import jpl.gds.eha.api.channel.IChannelValueFactory;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.service.channel.IChannelPublisherUtility;
import jpl.gds.eha.api.service.channel.ISfduHeaderChannelizerService;
import jpl.gds.shared.annotation.ToDo;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.holders.FrameIdHolder;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.holders.VcidHolder;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.sfdu.SfduException;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.Sclk;
import jpl.gds.station.api.IStationHeaderFactory;
import jpl.gds.station.api.dsn.chdo.IChdoSfdu;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.ITelemetryFrameMessage;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * SfduHeaderChannelizerService processes ChdoSfdu objects from incoming DSN
 * monitor packet messages, extracting the necessary CHDO field values and
 * channelizing/publishing them. This class relies on a supplied Channel
 * Definition Provider to provide all of the necessary information on (1) which
 * CHDO fields to extract and (2) what sort of format they should be extracted
 * as.
 *
 * NB: For SSE header channels:
 *
 * The VCID might well be returned although we would normally expect NULL for
 * SSE. That is because there is no way to signal "no value" in the SFDU fields.
 * It can still be NULL if there is no CHDO for it.
 *
 * If VCID is not NULL, then a zero value is expected. We check for that and
 * warn if it's something else. In any case, VCID is forced to NULL.
 *
 * DSS id is similar, except that we force to UNSPECIFIED_DSSID (which is zero).
 *
 * Filtering makes no sense for SSE.
 *
 * @See jpl.gds.rawio.input.stream.processor.SfduPktStreamProcessor
 *
 *      NB: For non-SSE header channels:
 *
 *      dssId defaults to -1 instead of UNSPECIFIED_DSSID and it is left that
 *      way to avoid having to change downstream logic.
 *
 */
@ToDo("VCID should be replaced with a proper object that holds unsigned int")
public class SfduHeaderChannelizerService implements ISfduHeaderChannelizerService, MessageSubscriber {

    private final Tracer log; 

	private static final ISclk zeroSclk = new Sclk(0, 0);
	private static final IAccurateDateTime zeroScet = new AccurateDateTime(true); // dummy SCET
    private static final String SFDU_MARKER = "NJPL";

    private final boolean                         isSse;

	private final boolean setSolTimes;
	
    /**
     * R/T or recorded configuration for EVR and EHA.
     * Set USE_RTREC to true to allow header channels to inherit their
     * RT/REC state from the parent.
     *
     */
    private static final boolean USE_RTREC = false;

    private final RealtimeRecordedConfiguration rtRec;
    
    private final IMessagePublicationBus bus;

    private final Integer spacecraftId;
    private final IChannelPublisherUtility pubUtil;
    private Map<String, IChannelDefinition> fieldMap;
    private final IChannelValueFactory chanFactory;
    private final IStationHeaderFactory stationHeaderFactory;

    private final IContextConfiguration contextConfig;

    private AtomicBoolean              fieldMapInitialized = new AtomicBoolean(false);
    private IChannelDefinitionProvider chanTable;
    private HeaderChannelTable         ht;

	    /**
     * Default constructor.
     * 
     * @param serveContext
     *            the current application context
     */
    public SfduHeaderChannelizerService(final ApplicationContext serveContext)
    {
        this.log = TraceManager.getTracer(serveContext, Loggers.TLM_HEADER);
        this.bus = serveContext.getBean(IMessagePublicationBus.class);
        this.pubUtil = serveContext.getBean(IChannelPublisherUtility.class);
        this.chanFactory = serveContext.getBean(IChannelValueFactory.class);
        this.stationHeaderFactory = serveContext.getBean(IStationHeaderFactory.class);
        this.contextConfig = serveContext.getBean(IContextConfiguration.class);
        isSse = serveContext.getBean(SseContextFlag.class).isApplicationSse();

        this.spacecraftId = serveContext.getBean(IContextIdentification.class).getSpacecraftId();
        
		this.setSolTimes = serveContext.getBean(EnableLstContextFlag.class).isLstEnabled();
		
		chanTable = serveContext.getBean(IChannelDefinitionProvider.class);

		ht = new HeaderChannelTable();
        ht.addDefinitions(chanTable.getChannelDefinitionMap().values());
        fieldMap = ht.getSfduHdrFieldMapping();


        RealtimeRecordedConfiguration temp = null;

        try
        {
            if (USE_RTREC)
            {
                temp =  serveContext.getBean(RealtimeRecordedConfiguration.class);
            }
        }
        catch (final Exception de)
        {
            log.error(
                "Unable to create RT/Rec dictionary, all will be R/T: " +
                ExceptionTools.rollUpMessages(de), de);
        }

        rtRec = temp;
	}

	/**
	 * The main method that gets called by the downlink process. channelize is
	 * handed the ChdoSfdu object to be processed, and the method will extract
	 * the necessary field values from it. It will then channelize them and
	 * finally publish the values.
	 *
     * 6/04/13 Add support for frame headers
     *
	 * @param sfdu     ChdoSfdu to process
     * @param pi       Packet id of source packet
     * @param fi       Frame id of source frame
     * @param ph       True if a (fsw or sse) packet header, else frame
     * @param apid     APID
     * @param haveApid True if APID available
	 */
    private void channelize(IChdoSfdu sfdu,
                            final Integer dssIdObj,
                            final Integer vcid,
                            final HeaderHolder   sfduData,
                            final PacketIdHolder pi,
                            final FrameIdHolder  fi,
                            final boolean        ph,
                            final int            apid,
                            final boolean        haveApid)
    {
        if (isSse && ! ph)
        {
            log.error("SFDU H-filtering: Marked as frame " +
                                  "under SSE, so skipped");
            return;
        }

		/*
		 * According to Marti, we need to filter out H-channels from SFDU that
		 * have DSS ID and VCID that don't match the session's. Below code is
		 * meant to do that filtering.
		 */
		boolean dssIdVcidFilterPassed = false;

        if (isSse)
        {
            dssIdVcidFilterPassed = true;
            log.trace("SFDU H-filtering: SSE so processing this SFDU without filtering");
        }


        if (sfdu == null) {
            try {
                sfdu = stationHeaderFactory.createChdoSfdu();
                final byte[] sfduBytes = sfduData.getValue();
                sfdu.loadSdfuHeaderOnly(sfduBytes, 0, sfduBytes.length);
            } catch (IOException | SfduException e1) {
                log.error("Error creating CHDO SFDU in SFDU header channelizer. SFDU header will not be processed: " +
                        e1.toString(), e1);
            }
        }
        
        
	    final int  dssId    = (dssIdObj != null) ? dssIdObj.intValue() : -1;

	    /*
	     * If filter's still on, compare DSS ID and VCID with values from SFDU.
	     */
	    if (!dssIdVcidFilterPassed) {
	        dssIdVcidFilterPassed = contextConfig.accept(sfdu);	    	
	    }
	    
	    if (dssIdVcidFilterPassed) {
	        log.trace("SFDU H-filtering: SCID, DSS ID and VCID filters passed, so process this SFDU");
	    } else {
	        log.trace("SFDU H-filtering: DSS ID, VCID, or SCID filter not passed, so do not process this SFDU");
	        return;
	    }
        
	    
		final ArrayList<IServiceChannelValue> chanVals = new ArrayList<>();
	
		final Set<Entry<String, IChannelDefinition>> fieldSet = fieldMap.entrySet();
		final StringBuilder sb = new StringBuilder(1024);

		/*
		 * fieldSet is assumed to have the entire set of channels to be
		 * extracted from the ChdoSfdu. So loop through the set and extract the
		 * field values.
		 */
		for (final Entry<String, IChannelDefinition> entry : fieldSet) {
			final String fieldId = entry.getKey();
			final IChannelDefinition def = entry.getValue();

			try {
				final ChannelType type = def.getChannelType();
				/*
				 * ChdoSfdu provides different methods for different types of
				 * values. We need to call the correct method for each
				 * individual channel value. Use the internal method for that
				 * very mapping.
				 */

				Object val = null;

				switch (type) {
				case SIGNED_INT:
				case STATUS:
					val = sfdu.getFieldValueAsSignedInt(fieldId);
					break;
				case DIGITAL:
				case UNSIGNED_INT:
				case TIME:
					val = sfdu.getFieldValueAsUnsignedInt(fieldId);
					break;
				case ASCII:
					val = sfdu.getFieldValueAsString(fieldId);
					break;
				case FLOAT:
					val = sfdu.getFieldValueAsFloatingPoint(fieldId);
					break;
				default:
					throw new IllegalArgumentException("Unexpected ChannelType for SFDU header channels");
				}

				if (val != null) {
					final IServiceChannelValue chanVal = chanFactory.createServiceChannelValue(def);
					chanVal.setDn(val);


                    chanVal.setPacketId(pi);

                    /*
                     * Just a reminder. Currently a long is expected.
                     *
                     * chanVal.setFrameId(fi);
                     */

                    if (! ph)
                    {
                        chanVal.setChannelCategory(ChannelCategoryEnum.FRAME_HEADER);
                    }
                    else if (isSse)
                    {
                        chanVal.setChannelCategory(ChannelCategoryEnum.SSEPACKET_HEADER);
                    }
                    else
                    {
                        chanVal.setChannelCategory(ChannelCategoryEnum.PACKET_HEADER);
                    }


					chanVals.add(chanVal);
				}

			} catch (final Exception e) {
				sb.setLength(0);
				sb.append("Exception during SFDU H-channelization: fieldId=");
				sb.append(fieldId);
				log.error(sb, e);

				/*
				 * Exception was encountered while trying extract a field value,
				 * but it doesn't invalidate the entire ChdoSfdu. Just skip this
				 * one and continue on from the next one.
				 */
				continue;
			}

		}

		if (! chanVals.isEmpty()) {
            final IAccurateDateTime rct = new AccurateDateTime();

			/*
			 * Try to retrieve as much time-data from the ChdoSfdu as possible.
			 * If any are missing, either use zero-values or current time (for
			 * ERT, at least).
			 */
			IAccurateDateTime ert = sfdu.getFieldValueAsDate("ert");

			if (ert == null) {

				ert = sfdu.getFieldValueAsDate("mst");

				if (ert == null) {
					ert = new AccurateDateTime();
					sb.setLength(0);
					sb.append("Could not obtain neither ERT nor MST from ChdoSfdu; using current time: ");
					sb.append(ert.toString());
					log.debug(sb);
				} else {
					sb.setLength(0);
					sb.append("Could not obtain ERT from ChdoSfdu; using MST: ");
					sb.append(ert.toString());
					log.debug(sb);
				}

			}

            ISclk sclk = sfdu.getFieldValueAsSclk("sclk");
			ILocalSolarTime sol = null;

			if (sclk == null) {
				sclk = zeroSclk;
			}

			IAccurateDateTime scet = sfdu.getFieldValueAsDate("scet");

            /**
             * Frame headers make up SCET from ERT if we have it,
             * but do not use for LST.
             */

            if (scet == null)
            {
                if (! ph && (ert != null))
                {
                    scet = new AccurateDateTime(ert.getTime());
                }
                else
                {
                    scet = zeroScet;
                }
            }
            else if (setSolTimes)
            {
                sol = LocalSolarTimeFactory.getNewLst(scet, this.spacecraftId);
            }
            
            /* Use faster date formatter here */
            final String streamId = pubUtil.genStreamId(ert.getFormattedErtFast(true));

            /** The db can store dssId and vcid these days. Keep that comment for now. */

            /** VCID should be Long, not Integer */
            Integer restrictedVcid = Integer.valueOf(0);

            try
            {
                // NULL is OK because EhaPublisherUtility methods accept it
                restrictedVcid = VcidHolder.restrictSfduVcid(vcid.longValue(), true);
            }
            catch (final IllegalArgumentException iae)
            {
                log.warn("SFDU H-filtering: VCID of " +
                                     vcid                         +
                                     " forced to 0");
            }

    		/*
    		 * Using the new, wrapped publishing API.
             *
             */
            pubUtil.publishFlightAndDerivedChannels(
                false,
                chanVals,
                rct,
                ert,
                scet,
                sclk,
                sol,
                streamId,
                ! getRtRecState(apid, haveApid, restrictedVcid).get(),
                dssId,
                restrictedVcid,
                null);
		}

	}


    /**
     * Do we want R/T or recorded?
     *
     * @param apid     APID (or ignore)
     * @param haveApid True if we use APID
     * @param vcid     VCID (may be null)
     *
     * @return R/T or Rec state
     */
    private RecordedBool getRtRecState(final int     apid,
                                       final boolean haveApid,
                                       final Integer vcid)
    {

        if (rtRec == null)
        {
            return RecordedBool.REALTIME;
        }

        final Long useVcid = ((vcid != null)
                                  ? Long.valueOf(vcid.longValue())
                                  : null);
        if (haveApid)
        {
            return rtRec.getState(EhaBool.EHA,
                                  Long.valueOf(apid),
                                  useVcid,
                                  isSse);
        }

        return rtRec.getState(useVcid, isSse);
    }

    @Override
    public boolean startService() {
        // Subscribe to both types
        bus.subscribe(TmServiceMessageType.TelemetryFrame, this);
        bus.subscribe(TmServiceMessageType.TelemetryPacket, this);
        return true;
    }

    @Override
    public void stopService() {
        bus.unsubscribeAll(this);
        
    }

    @Override
    public void handleMessage(final IMessage message) {
        if (message.isType(TmServiceMessageType.TelemetryFrame)) {
            final ITelemetryFrameMessage m = (ITelemetryFrameMessage)message;
            if (!m.getFrameInfo().isDeadCode() && !m.getFrameInfo().isIdle()
                    && isSfduHeader(m.getRawHeader(), false)) {
                channelize(m.getChdoObject(), m.getDssId(), m.getVcid(), m.getRawHeader(), 
                        PacketIdHolder.UNSUPPORTED, FrameIdHolder.UNSUPPORTED, false, 0, false);
            }
        } else if (message.isType(TmServiceMessageType.TelemetryPacket)) {
            final ITelemetryPacketMessage m = (ITelemetryPacketMessage)message;
            if (!m.getPacketInfo().isFill() && isSfduHeader(m.getHeader(), true)) {
                channelize(m.getChdoObject(), m.getDssId(), m.getVcid(), m.getHeader(),
                        (m.getPacketId() != null) ? m.getPacketId() : PacketIdHolder.UNSUPPORTED,
                        FrameIdHolder.UNSUPPORTED, true, m.getPacketInfo().getApid(), true);
            }
        }
        
    }
    // Removed dependency on inputType
    // check header to see if it's SFDU - if it has SFDU header (starts with NJPL)
    // also moved call to disableOtherChannelizersOfOverlaps here
    private boolean isSfduHeader(final HeaderHolder header, boolean isPacket) {
        final byte[] bytes = header.getValue();
        if(bytes != null){
            boolean isSfdu = new String(bytes).startsWith(SFDU_MARKER);
            if(!fieldMapInitialized.get() && isSfdu){
                ht.disableOtherChannelizersOfOverlaps(isPacket ? TelemetryInputType.SFDU_PKT : TelemetryInputType.SFDU_TF);
                fieldMapInitialized.set(true);
            }
            return isSfdu;
        }
        return false;
    }
}
