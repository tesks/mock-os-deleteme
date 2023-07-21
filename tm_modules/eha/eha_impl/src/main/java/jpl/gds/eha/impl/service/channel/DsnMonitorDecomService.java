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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.eha.api.channel.IChannelValueFactory;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.service.channel.IChannelPublisherUtility;
import jpl.gds.eha.api.service.channel.IDsnMonitorDecomService;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.Sclk;
import jpl.gds.station.api.StationMessageType;
import jpl.gds.station.api.dsn.chdo.IChdo;
import jpl.gds.station.api.dsn.chdo.IChdoDefinition;
import jpl.gds.station.api.dsn.chdo.IChdoSfdu;
import jpl.gds.station.api.dsn.message.IDsnMonitorMessage;

/**
 * This is the downlink service that performs channelization of DSN monitor
 * SFDUs. It listens on the internal message context for Monitor SFDU messages
 * and publishes EHA Channel Messages.
 * 
 *
 * 12/6/13 Converted this to a DownlinkService.
 */
public class DsnMonitorDecomService implements IDsnMonitorDecomService {
    
    private static final String NUMBER_OF_CHANNELS_IN_SFDU_FIELD_NAME = "number_channels";
    private static final String SPACECRAFT_ID_FIELD_NAME = "scft_id";
    private static final String MONITOR_SAMPLE_TIME_FIELD_NAME = "mst";
    private static final String[] DATA_SOURCE_FIELD_NAMES = new String[] {"data_source", "source_code"};
    
    /** The logger for this class and all subclasses */
    protected final Tracer                  log;   


    private static final int CHANNELIZED_DATA_AREA_CHDO_NUMBER = 28;

    private static final IAccurateDateTime zeroScet = new AccurateDateTime(true); // dummy SCET
    private static final ISclk zeroSclk = new Sclk(0, 0);

    private final DsnMonitorChannelExtractor channelizer;
    private final IChannelDefinitionProvider chanTable;
    

    /*
     * Added a message subscriber and flag for service startup.
     */
    private MonSfduMessageSubscriber messageSubscriber;
    private final AtomicBoolean started = new AtomicBoolean(Boolean.FALSE);

    /*
     * This counter is used to post-fix the streamId string in case
     * many SFDUs arrive in a very short period time (but unlikely)
     */
    private int streamIdCounter = 0;
    
    private final IMessagePublicationBus messageBus;
    private final Integer filterDss;
    private final Integer filterVcid;
    private final Integer filterScid;
    private final IChannelPublisherUtility pubUtil;
    private final Map<Integer, IChannelDefinition> chanDefIndices                        = new HashMap<Integer, IChannelDefinition>();
    private final IStatusMessageFactory statusMessageFactory;
    
    /**
     * Constructor.
     * 
     * @param context
     *            the current application context
     */
    public DsnMonitorDecomService(final ApplicationContext context) {
        this.log = TraceManager.getTracer(context, Loggers.TLM_MONITOR);
        this.messageBus = context.getBean(IMessagePublicationBus.class);
        final IContextFilterInformation scFilter = context.getBean(IContextFilterInformation.class);
        this.filterScid = context.getBean(IContextIdentification.class).getSpacecraftId();
        this.filterDss = scFilter.getDssId();
        this.filterVcid = scFilter.getVcid();
		this.pubUtil = context.getBean(IChannelPublisherUtility.class);
        this.chanTable = context.getBean(IChannelDefinitionProvider.class);
        this.statusMessageFactory = context.getBean(IStatusMessageFactory.class);
        
        this.channelizer = new DsnMonitorChannelExtractor(this.chanTable, context.getBean(IChannelValueFactory.class));
        
        for (final IChannelDefinition def: chanTable.getChannelDefinitionMap().values()) {
            if (def.getIndex() != IChannelDefinition.NO_INDEX && def.getDefinitionType() == ChannelDefinitionType.M) {
                chanDefIndices.put(def.getIndex(), def);
            }
        }
        
    }

    /**
     * Processes the DSN Monitor Data Channels in the provided SFDU.
     * Publishing the channel data and doing derivations is this
     * method's culmination.
     * 
     * @param sfdu	ChdoSfdu that potentially carries DSN Monitor
     * 				Data Channels to be processed.
     * @return		number of channels processed from the provided
     * 				ChdoSfdu. Includes those channels that could
     * 				not be matched to a channel definition. If it was
     * 				looked at, then it's counted.
     */
    public long process(final IChdoSfdu sfdu) {
        final StringBuilder sb = new StringBuilder(1024);

        if (sfdu == null) {
            log.debug("Received null SFDU object; do nothing");
            return 0;
        }

        /*
         * Check for spacecraft ID
         * 
         */
        final Long scId = sfdu.getFieldValueAsUnsignedInt(SPACECRAFT_ID_FIELD_NAME);

        if (scId == null) {
            sb.append("Cannot obtain ");
            sb.append(SPACECRAFT_ID_FIELD_NAME);
            sb.append(" (SFDU id: ");
            sb.append(sfdu.getSfduId());
            sb.append(", label: ");
            sb.append(sfdu.getSfduLabel());
            sb.append("); abandoning DSN monitors from SFDU");
            final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.ERROR,
                    sb.toString());
            messageBus.publish(lm);
            log.log(lm);
            return 0;
        }
           
        /*
         * Deleted an equality check for the session SCID and SFDU SCID that
         * would cause the method to exit out if not equal.  Monitor data is 
         * multimission and should be processed regardless of SCID.
         */


        /*
         * Fetch the antenna/facility identifier (a.k.a. DSS ID)
         */
        Long dssId = null;

        for (int i = 0; i < DATA_SOURCE_FIELD_NAMES.length && dssId == null; i++) {
            dssId = sfdu.getFieldValueAsUnsignedInt(DATA_SOURCE_FIELD_NAMES[i]);
        }

        if (dssId == null) {
            sb.append("Cannot obtain antenna/facility ID (SFDU id: ");
            sb.append(sfdu.getSfduId());
            sb.append(", label: ");
            sb.append(sfdu.getSfduLabel());
            sb.append("); abandoning DSN monitors from SFDU");
            final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.ERROR,
                    sb.toString());
            log.log(lm);
            messageBus.publish(lm);
            return 0;
        }

        /*
         * Discard MON SFDUs that do not match the session DSS ID.
         */
        if (filterDss != null 
                && filterDss.intValue() != dssId.intValue()) {
            final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.INFO,
                    "Encountered MON-0158 SFDU for station " + dssId
                            + ", but session is configured for station "
                            + filterDss
                            + "; This MON SFDU will be discarded");
            log.log(lm);
            messageBus.publish(lm);
            return 0;
        }

        /*
         * Fetch number of channels contained in this SFDU for
         * verification purposes
         */
        final Long numberOfChannels = sfdu.getFieldValueAsUnsignedInt(NUMBER_OF_CHANNELS_IN_SFDU_FIELD_NAME);

        if (numberOfChannels == null) {
            sb.append("Cannot obtain ");
            sb.append(NUMBER_OF_CHANNELS_IN_SFDU_FIELD_NAME);
            sb.append(" (SFDU id: ");
            sb.append(sfdu.getSfduId());
            sb.append(", label: ");
            sb.append(sfdu.getSfduLabel());
            sb.append("); ignoring");
            final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.WARNING,
                    sb.toString());
            log.log(lm);
            messageBus.publish(lm);
        }

        final IChdo chdo = sfdu.getDataChdo();

        if (chdo == null) {
            sb.delete(0, sb.length());
            sb.append("SFDU object (id: ");
            sb.append(sfdu.getSfduId());
            sb.append(", label: ");
            sb.append(sfdu.getSfduLabel());
            sb.append(") returned null data CHDO; can't process DSN monitor from this SFDU");
            final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.ERROR,
                    sb.toString());
            log.log(lm);
            messageBus.publish(lm);
            return 0;
        }

        final IChdoDefinition chdoDef = chdo.getDefinition();

        if (chdoDef == null) {
            sb.delete(0, sb.length());
            sb.append("CHDO for SFDU (id: ");
            sb.append(sfdu.getSfduId());
            sb.append(", label: ");
            sb.append(sfdu.getSfduLabel());
            sb.append(") is missing its definition; can't process DSN monitor from this SFDU");
            final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.ERROR,
                    sb.toString());
            messageBus.publish(lm);
            return 0;
        }

        if (chdoDef.getType() != CHANNELIZED_DATA_AREA_CHDO_NUMBER) {
            sb.delete(0, sb.length());
            sb.append("Data is CHDO ");
            sb.append(chdoDef.getType());
            sb.append("; can't process DSN monitor from this SFDU");
            final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.INFO,
                    sb.toString());
            messageBus.publish(lm);
            log.log(lm);
            return 0;
        }

        /*
         * We have a Channelized Data Area CHDO, so extract monitor
         * data. This is how this CHDO should look like:
         * 
         * Bits-> | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10| 11| 12| 13| 14| 15|
         *        |---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---|
         *      0 |							  chdo_type							  |
         *        |---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---|
         *      2 | 						 chdo_length 						  |
         *        |---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---|
         *      4 | 	 source		  | 5 | 6 | 7 | 		length_value		  |
         *        |---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---|
         *      6 | filler_length | 			   channel_number 				  |
         *        |---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---|
         *      8 | 															  |
         *        |																  |
         *        /							 lc_value							  /
         *        |			 (this field may not exist for a channel) 			  |
         *      n |																  |
         *        |---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---|
         * 
         * (Copied from 820-013 0172-Telecomm-028 Rev. A)
         * 
         * Bytes 4 - lc_value are repeated once for each channel.
         * (i,e., "number_channels" times, value from quaternary CHDO
         * type 27). Bytes 0-3 only occur once per CHDO.
         * 
         * First four bytes (chdo_type and chdo_length) should be
         * omitted from chdoBody.
         */
        final byte[] chdoBody = chdo.getBytesWithoutChdoHeader();

        /*
         * Before iterating through the channels, first obtain the
         * timestamp(s)
         */
        final IAccurateDateTime monitorSampleTime = sfdu.getFieldValueAsDate(MONITOR_SAMPLE_TIME_FIELD_NAME);
        /*
         * Ignore RCT from SFDU. Use current time as RCT.  
         */
        //		 IAccurateDateTime recordCreationTime = sfdu.getFieldValueAsDate(RECORD_CREATION_TIME_FIELD_NAME);

        if (monitorSampleTime == null) {
            sb.delete(0, sb.length());
            sb.append("Cannot obtain monitor sample time (SFDU id: ");
            sb.append(sfdu.getSfduId());
            sb.append(", label: ");
            sb.append(sfdu.getSfduLabel());
            sb.append("); can't process DSN monitor from this SFDU");
            final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.ERROR,
                    sb.toString());
            log.log(lm);
            messageBus.publish(lm);
            return 0;
        }

        //		if (recordCreationTime == null) {
        //			sb.delete(0, sb.length());
        //			sb.append("Cannot obtain record creation time (SFDU id: ");
        //			sb.append(sfdu.getSfduId());
        //			sb.append(", label: ");
        //			sb.append(sfdu.getSfduLabel());
        //			sb.append("); will use monitor sample time as RCT");
        //			IPublishableLogMessage lm = new PublishableLogMessage(TraceSeverity.WARNING, sb.toString());
        //            MessageContext.getInstance().publish(lm);
        //            recordCreationTime = monitorSampleTime;
        //		}

        /*
         * channelOffset will be incremented with size (bytes) of one
         * entire channel data, per loop.
         */
        int channelOffset = 0;

        /*
         * channelValues will carry all of the extracted monitor data
         * from this SFDU in a List.
         */
        final List<IServiceChannelValue> channelValues = new ArrayList<IServiceChannelValue>(64);

        /*
         * loopCounter will be useful when there's a problem and we
         * need to track down where it occurred.
         */
        long loopCounter = 0;

        while (channelOffset < chdoBody.length) {
            IServiceChannelValue channelValue = null;
            int lcValueLength = 0;

            boolean skipThisChannel = false;


            try {

                /* source offset is 0. */
                // int source = GDR.get_u8(chdoBody, channelOffset, 0, 5);	// not needed, assumed to be 'M'

                /* channel_number offset is 2 bytes + 4 bits. */
                final int channelNum = GDR.get_u16(chdoBody, channelOffset + 2, 4, 12);

                /* 
                 * Look up the entry for this channel in the dictionary.
                 */
                final IChannelDefinition channelDef = chanDefIndices.get(channelNum);
                if (channelDef == null) {
                    sb.delete(0, sb.length());
                    /* M. DeMore 3/13/2012 
                     * Given that it seems legacy mission CPTs vary as to which monitor channels
                     * are delivered by TIS/TDS, it seems that this should just not generate a
                     * error. Individual monitor channels are not required. It might be better to modify
                     * the monitor dictionary schema to allow these channels to be enabled/disabled.
                     */
                    //					sb.append("Data CHDO channel ('M',num=");
                    //					sb.append(channelNum);
                    //					sb.append(") [ix:");
                    //					sb.append(loopCounter);
                    //					sb.append("] (SFDU id: ");
                    //					sb.append(sfdu.getSfduId());
                    //					sb.append(", label: ");
                    //					sb.append(sfdu.getSfduLabel());
                    //					sb.append(") not found in dictionary; skipping this channel value");

                    skipThisChannel = true;
                }

                /* lv_flag offset is 5 bits. */
                final boolean shortValue =
                        GDR.getBooleanFromInt(GDR.get_u8(chdoBody,
                                channelOffset,
                                5,
                                1));

                /* filler_length offset is 2 bytes. */
                final int fillerLen = GDR.get_u8(chdoBody, channelOffset + 2, 0, 4);

                /* Consistency check. */
                if (shortValue && fillerLen > 7) {
                    sb.delete(0, sb.length());
                    sb.append("Data CHDO channel ");
                    sb.append(channelDef.getId().toString());
                    sb.append(" [ix:");
                    sb.append(loopCounter);
                    sb.append("] (SFDU id: ");
                    sb.append(sfdu.getSfduId());
                    sb.append(", label: ");
                    sb.append(sfdu.getSfduLabel());
                    sb.append(") indicates filler_length=");
                    sb.append(fillerLen);
                    sb.append(", but value is only 8 bits long ");
                    sb.append("(lv_flag=1); skipping this channel value");
                    final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(
                            TraceSeverity.ERROR, sb.toString());
                    log.log(lm);
                    messageBus.publish(lm);
                    channelOffset += 4;	// Increment for next loop.
                    loopCounter++;
                    continue;
                }

                /*
                 * Extract the channel value.
                 */
                if (shortValue) {
                    /*
                     * lv_flag indicates that length_value (offset:
                     * 1 byte) is the actual data value.
                     */

                    /*
                     * Before we even read the data, if we need to
                     * skip, do it now.
                     */
                    if (skipThisChannel) {
                        /* M. DeMore 3/13/2012 
                         * Given that it seems legacy mission CPTs vary as to which monitor channels
                         * are delivered by TIS/TDS, it seems that this should just not generate a
                         * error. Individual monitor channels are not required. It might be better to modify
                         * the monitor dictionary schema to allow these channels to be enabled/disabled.
                         */
                        //		            	IPublishableLogMessage lm = new PublishableLogMessage(TraceSeverity.ERROR, skipReason);
                        //		                messageBus.publish(lm);
                        channelOffset += 4;	// Increment for next loop.
                        loopCounter++;
                        continue;
                    }

                    channelValue = channelizer.getChannel(channelDef.getId().toString(), chdoBody, channelOffset + 1, fillerLen, 8 - fillerLen);
                    
                    /*
                     * No longer compute EU here. EU computation is now done
                     * by the EhaPublisherUtility.
                     */

                    lcValueLength = 0;
                } else {

                    /* length_value offset is 1 byte. */
                    final int valueWordsLength = GDR.get_u8(chdoBody, channelOffset + 1, 0, 8);

                    /*
                     * Validity check.
                     */
                    if (valueWordsLength < 1) {
                        sb.delete(0, sb.length());
                        sb.append("Data CHDO channel ");
                        sb.append(channelDef.getId().toString());
                        sb.append(" [ix:");
                        sb.append(loopCounter);
                        sb.append("] (SFDU id: ");
                        sb.append(sfdu.getSfduId());
                        sb.append(", label: ");
                        sb.append(sfdu.getSfduLabel());
                        sb.append(") has lv_flag=0 but length_value < 1; skipping this channel value");
                        final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(
                                TraceSeverity.ERROR, sb.toString());
                        log.log(lm);
                        messageBus.publish(lm);
                        channelOffset += 4;	// Increment for next loop.
                        loopCounter++;
                        continue;
                    } else if (valueWordsLength > 255) {
                        sb.delete(0, sb.length());
                        sb.append("Data CHDO channel ");
                        sb.append(channelDef.getId().toString());
                        sb.append(" [ix:");
                        sb.append(loopCounter);
                        sb.append("] (SFDU id: ");
                        sb.append(sfdu.getSfduId());
                        sb.append(", label: ");
                        sb.append(sfdu.getSfduLabel());
                        sb.append(") has lv_flag=0 with length_value > 255 (illegal); abort processing DSN monitor from this SFDU");
                        final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(
                                TraceSeverity.ERROR, sb.toString());
                        log.log(lm);
                        messageBus.publish(lm);
                        return loopCounter;
                    }

                    lcValueLength = valueWordsLength * 2;

                    /*
                     * Now that we know exactly how long the
                     * lc_value is, skip if we need to.
                     */
                    if (skipThisChannel) {
                        /* M. DeMore 3/13/2012 
                         * Given that it seems legacy mission CPTs vary as to which monitor channels
                         * are delivered by TIS/TDS, it seems that this should just not generate a
                         * error. Individual monitor channels are not required. It might be better to modify
                         * the monitor dictionary schema to allow these channels to be enabled/disabled.
                         */
                        //		            	IPublishableLogMessage lm = new PublishableLogMessage(TraceSeverity.ERROR, skipReason);
                        //		                messageBus.publish(lm);
                        channelOffset += 4 + lcValueLength;	// Increment for next loop.
                        loopCounter++;
                        continue;
                    }

                    channelValue = channelizer.getChannel(channelDef.getId(), chdoBody, channelOffset + 4 + fillerLen / 8, fillerLen % 8, 16 * valueWordsLength - fillerLen);
                    /*
                     * No longer compute EU here. EU computation is now done
                     * by the EhaPublisherUtility.
                     */
                }

            } catch (final ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                sb.delete(0, sb.length());
                sb.append("Unable to extract DSN monitor channel value [ix:");
                sb.append(loopCounter);
                sb.append("] (SFDU id: ");
                sb.append(sfdu.getSfduId());
                sb.append(", label: ");
                sb.append(sfdu.getSfduLabel());
                sb.append(") due to array out-of-bounds indexing error: ");
                sb.append(e.getMessage());
                final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(
                        TraceSeverity.ERROR, sb.toString());
                log.log(lm);
                messageBus.publish(lm);
            } catch (final Exception e) {
                e.printStackTrace();
                sb.delete(0, sb.length());
                sb.append("Unable to extract DSN monitor channel value [ix:");
                sb.append(loopCounter);
                sb.append("] (SFDU id: ");
                sb.append(sfdu.getSfduId());
                sb.append(", label: ");
                sb.append(sfdu.getSfduLabel());
                sb.append("): ");
                sb.append(e.getMessage());
                final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(
                        TraceSeverity.ERROR, sb.toString());
                log.log(lm);
                messageBus.publish(lm);
            }

            if (channelValue != null) {
                channelValues.add(channelValue);
            }

            channelOffset += 4 + lcValueLength;
            loopCounter++;
        }

        sb.delete(0, sb.length());
        if (numberOfChannels != null) {

            if (numberOfChannels.longValue() != loopCounter) {
                sb.append("Channels count didn't match [number_channels:");
                sb.append(numberOfChannels);
                sb.append(", counted:");
                sb.append(loopCounter);
                sb.append("] (SFDU id: ");
                sb.append(sfdu.getSfduId());
                sb.append(", label: ");
                sb.append(sfdu.getSfduLabel());
                sb.append(")");
                final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(
                        TraceSeverity.WARNING, sb.toString());
                log.log(lm);
                messageBus.publish(lm);
            }
            else {
                sb.append("Channels count match [number_channels:");
                sb.append(numberOfChannels);
                sb.append(", counted:");
                sb.append(loopCounter);
                sb.append("] (SFDU id: ");
                sb.append(sfdu.getSfduId());
                sb.append(", label: ");
                sb.append(sfdu.getSfduLabel());
                sb.append(")");
                log.debug(sb.toString());
            }

        }
        else {
            sb.append("channels counted: ");
            sb.append(loopCounter);
            sb.append(" (SFDU id: ");
            sb.append(sfdu.getSfduId());
            sb.append(", label: ");
            sb.append(sfdu.getSfduLabel());
            sb.append("); no number_channels to compare to");
            log.debug(sb.toString());
        }

        final IAccurateDateTime currentDate = new AccurateDateTime();
        sb.delete(0, sb.length());
        sb.append(currentDate.toString());
        sb.append("-");
        sb.append(streamIdCounter);
        /*
         * Arbitrarily bound the counter because we won't be getting
         * too many SFDUs in a single millisecond.
         */
        streamIdCounter = ++streamIdCounter % 100000;

        /*
         * Using the new, wrapped publishing API.
         */
        pubUtil.publishFlightAndDerivedChannels(false,
                channelValues, currentDate, monitorSampleTime, zeroScet,
                zeroSclk, null, pubUtil.genStreamId(sb.toString()),
                true, dssId.byteValue(), null, new Boolean(false));

        return loopCounter;
    }

    /* 
     * Removed local method computeAndSetEu. There is now
     * a method in ChannelUtility to do it.
     */
 
    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.interfaces.IService#startService()
     */
    @Override
    public boolean startService() {

        if (!this.started.get()) {
            this.messageSubscriber = new MonSfduMessageSubscriber();
            this.started.set(Boolean.TRUE);
            return true;    
        } else {           
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.interfaces.IService#stopService()
     */
    @Override
    public void stopService() {
        if (!this.started.get()) {
            return;
        }
        if (this.messageSubscriber != null) {
            messageBus.unsubscribeAll(this.messageSubscriber);
        }
        this.messageSubscriber = null;  
        this.started.set(Boolean.FALSE);
    }

    /**
     * Internal message subscriber for MonitorSfduMessage.
     * 
     */
    private class MonSfduMessageSubscriber implements MessageSubscriber {

        /**
         * Constructor.
         */
        public MonSfduMessageSubscriber() {
            messageBus.subscribe(StationMessageType.DsnStationMonitor, this);
        }

        @Override
        public void handleMessage(final IMessage message) {

            if (message.isType(StationMessageType.DsnStationMonitor)) {
                process(((IDsnMonitorMessage)message).getMonitorSfdu());
            }

        }

    }

}
