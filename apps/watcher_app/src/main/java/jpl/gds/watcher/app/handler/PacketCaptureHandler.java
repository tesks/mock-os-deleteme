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
package jpl.gds.watcher.app.handler;

import java.util.HashMap;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * The PacketCaptureHandler verifies each of the queued messages are packet
 * messages before appropriately packing up the packet metadata and binary data
 * for storage or transmission
 */
public class PacketCaptureHandler extends TelemetryIngestionCaptureHandler<ITelemetryPacketMessage> {
    

    /**
     * Creates an instance of PacketCaptureHandler.
     * 
     * @param appContext
     *            the current application context
     */
    public PacketCaptureHandler(final ApplicationContext appContext) {
        super(appContext, "PacketQuery");
    }
    
    @Override
    protected byte[] prepDataBytes(final ITelemetryPacketMessage msg) {
        byte[] packetBytes = new byte[0];
        if (isCaptureMessages()) {
            packetBytes = msg.toBinary();
        } else {
            if (isRestoreBodies()) {
                packetBytes = msg.getHeader().getValue();
            }
            packetBytes = ArrayUtils.addAll(packetBytes, msg.getPacket());
            if (isRestoreBodies()) {
                packetBytes = ArrayUtils.addAll(packetBytes, msg.getTrailer().getValue());
            }
        }
        
        return packetBytes;
    }

    @Override
    protected HashMap<String, Object> messageToMap(final ITelemetryPacketMessage msg) {
        // Use template context.

        final HashMap<String, Object> retMap = super.messageToMap(msg);

        if (isDataOutput()) {
            int bytes = -1;
            if (isRestoreBodies()) {
                bytes = (int) retMap.get("length") + (int) retMap.get("HeaderLength")
                        + (int) retMap.get("TrailerLength");
            }
            if (isCaptureMessages()) {
                bytes = msg.toBinary().length;
            }
            if (bytes > 0) {
                retMap.put("length", bytes);
            }
        }


        // transformed vcid
        String tvcid = "";
        final Integer vcid = msg.getVcid();
        if (vcid != null && appContext.getBean(MissionProperties.class).shouldMapQueryOutputVcid()) {
            tvcid = StringUtil.safeTrim(appContext.getBean(MissionProperties.class).mapDownlinkVcidToName(vcid));
        }

        if (!tvcid.isEmpty()) {
            retMap.put(appContext.getBean(MissionProperties.class).getVcidColumnName(), tvcid);
        }

        return retMap;
    }

    @Override
    protected String constructCsvMetadataEntry(final ITelemetryPacketMessage msg) {

        final HashMap<String, Object> msgMap = messageToMap(msg);

        final StringBuilder sb = new StringBuilder(QUOTE + "Packet" + QUOTE);
        for (final String val : getCsvColumns()) {

            sb.append(COMMA + QUOTE);
            final String col = val.toUpperCase();
            switch (col) {
                case "SESSIONID":
                    sb.append(msgMap.get("sessionId"));
                    break;

                case "SESSIONHOST":
                    sb.append(msgMap.get("sessionHost"));
                    break;

                case "RCT":
                    sb.append(msgMap.get("rct"));
                    break;

                case "SCET":
                    sb.append(msgMap.get("scet"));
                    break;

                case "LST":
                    sb.append(msgMap.get("lst"));
                    break;

                case "ERT":
                    sb.append(msgMap.get("ert"));
                    break;

                case "SCLK":
                    sb.append(msgMap.get("sclk"));
                    break;

                case "VCID":
                    sb.append(msgMap.get("vcid"));
                    break;

                case "DSSID":
                    sb.append(msgMap.get("dssId"));
                    break;

                case "APID":
                    sb.append(msgMap.get("apid"));
                    break;

                case "APIDNAME":
                    sb.append(msgMap.get("apidName"));
                    break;

                case "FROMSSE":
                    sb.append(msgMap.get("fromSse"));
                    break;

                case "SPSC":
                    sb.append(msgMap.get("spsc"));
                    break;

                case "LENGTH":
                    sb.append(msgMap.get("length"));
                    break;

                case "SOURCEVCFCS":
                    final Object vcfcs = msgMap.get("sourceVcfcs");

                    if (vcfcs != null) {
                        sb.append(vcfcs);
                    }
                    else {
                        sb.append("");
                    }

                    break;

                case "FILEBYTEOFFSET":
                    sb.append(msgMap.get("fileByteOffset"));
                    break;

                case "VCIDNAME":
                    String mappedVcid = (String) msgMap
                            .get(appContext.getBean(MissionProperties.class).getVcidColumnName());
                    sb.append(mappedVcid != null ? mappedVcid : "");
                    break;

                default:

                    if (appContext.getBean(MissionProperties.class).getVcidColumnName().toUpperCase()
                            .equalsIgnoreCase(col)) {
                        mappedVcid = (String) msgMap
                                .get(appContext.getBean(MissionProperties.class).getVcidColumnName());
                        sb.append(mappedVcid != null ? mappedVcid : "");
                    } else if (!metaSkip.contains(col)) {
                        log.warn("Column " + val + " is not supported, skipped");

                        metaSkip.add(col);
                    }

                    break;
            }
            sb.append(QUOTE);
        }
        sb.append(NEWLINE);
        return sb.toString();
    }
    
    @Override
    public ITelemetryPacketMessage castMessage(final IMessage msg) {
        return (ITelemetryPacketMessage) msg;
    }
    
    @Override
    public boolean messageFilterCheck(final ITelemetryPacketMessage msg) {
        if (super.messageFilterCheck(msg)) {
            // If we have a message, but no filter, accept all messages (not filtering messages)
            return true;
        }
        // then we have a message AND a filter. check the filter
        if (msg != null) {
            boolean goodVcid = true;
            boolean goodDss = true;
            // check versus the vcid filter
            if (getVcidFilter() != null) {
                final UnsignedInteger vcid = msg.getVcid() == null ? null
                        : UnsignedInteger.valueOfIntegerAsUnsigned(msg.getVcid());
                goodVcid = getVcidFilter().accept(vcid);
            }
            // check versus the DssId filter
            if (getDssIdFilter() != null) {
                final UnsignedInteger dssId = msg.getDssId() == null ? null
                        : UnsignedInteger.valueOfIntegerAsUnsigned(msg.getDssId());
                goodDss = getDssIdFilter().accept(dssId);
            }

            return goodVcid && goodDss;
        }

        // otherwise we have a filter, but no message. Obviously that "message" doesn't pass.
        return false;
    }

    @Override
    protected String getMessageClassName(){
        return ITelemetryPacketMessage.class.toString();
    }
}
