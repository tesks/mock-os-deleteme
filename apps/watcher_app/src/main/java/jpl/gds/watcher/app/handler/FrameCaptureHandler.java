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
import jpl.gds.tm.service.api.frame.ITelemetryFrameMessage;

/**
 * The FrameCaptureHandler verifies each of the queued messages are frame
 * messages before appropriately packing up the frame metadata and binary data
 * for storage or transmission
 */
public class FrameCaptureHandler extends TelemetryIngestionCaptureHandler<ITelemetryFrameMessage> {

    /**
     * Creates an instance of FrameCaptureHandler. 
     * @param appContext the current application context
     */
    public FrameCaptureHandler(final ApplicationContext appContext) {
        super(appContext, "FrameQuery");
    }
    
    @Override
    protected byte[] prepDataBytes(final ITelemetryFrameMessage msg) {
        byte[] frameBytes = new byte[0];
        if(isCaptureMessages()){
            frameBytes = msg.toBinary();
        }
        else{
            if(isRestoreBodies()){
                frameBytes = msg.getRawHeader().getValue();
            }
            frameBytes = ArrayUtils.addAll(frameBytes, msg.getFrame());
            if(isRestoreBodies()){
                frameBytes = ArrayUtils.addAll(frameBytes, msg.getRawTrailer().getValue());
            }
        }
        return frameBytes;
    }

   	@Override
    protected HashMap<String, Object> messageToMap(final ITelemetryFrameMessage msg){

        final HashMap<String, Object> retMap = super.messageToMap(msg);
    	
        msg.setTemplateContext(retMap);
        
        if(isDataOutput()){
            int bytes = -1;
        	if(isRestoreBodies()){
                bytes = (int) retMap.get("length") + (int) retMap.get("rawHeaderLength")
                        + (int) retMap.get("rawTrailerLength");
        	}
        	if(isCaptureMessages()){
        		bytes = msg.toBinary().length;
        	}
            if (bytes > 0) {
                retMap.put("length", bytes);
            }
        }
        
        retMap.put("sessionDssId", retMap.get("dssId"));

        //transformed vcid
        String tvcid = "";
        final Integer vcid = msg.getVcid();
        if (vcid != null && appContext.getBean(MissionProperties.class).shouldMapQueryOutputVcid())
        {
            tvcid = StringUtil.safeTrim(appContext.getBean(MissionProperties.class).mapDownlinkVcidToName(vcid));
        }

        if (! tvcid.isEmpty())
        {
            retMap.put(appContext.getBean(MissionProperties.class).getVcidColumnName(), tvcid);
        }
    	
    	return retMap;
    }
    
	@Override
	protected String constructCsvMetadataEntry(final ITelemetryFrameMessage msg){
        /*
         * Use the template context variable names.
         */

    	final HashMap<String, Object> msgMap = messageToMap(msg);

    	final StringBuilder sb = new StringBuilder(QUOTE + "Frame" + QUOTE);
    	for(final String val : getCsvColumns()){

    		sb.append(COMMA + QUOTE);
    		final String col = val.toUpperCase();
    		switch (col){
    			case "SESSIONID":
    				sb.append(msgMap.get("sessionId"));
    				break;

    			case "SESSIONHOST":
    				sb.append(msgMap.get("sessionHost"));
    				break;

    			case "TYPE":
                    sb.append(msgMap.get("name"));
    				break;

    			case "ERT":
    				sb.append(msgMap.get("ert"));
    				break;

    			case "RELAYSPACECRAFTID":
    				sb.append(msgMap.get("relayScid"));
    				break;

    			case "VCID":
    				sb.append(msgMap.get("vcid"));
    				break;

    			case "SOURCEVCFC":
                    sb.append(msgMap.get("seqCount"));
    				break;

    			case "DSSID":
    				sb.append(msgMap.get("dssId"));
    				break;

    			case "BITRATE":
    				sb.append(msgMap.get("bitRate"));
    				break;

    			case "ISBAD":
    				sb.append(msgMap.get("bad"));
    				break;

    			case "BADREASON":
    				final Object badReason = msgMap.get("badReason");
    				sb.append(badReason != null ? badReason.toString() : "");
    				break;

    			case "LENGTH":
    				sb.append(msgMap.get("length"));
    				break;

    			case "FILEBYTEOFFSET":
    				sb.append(msgMap.get("fileByteOffset"));
    				break;

    			case "RCT":
    				sb.append(msgMap.get("rct"));
    				break;

    			case "VCIDNAME":
    				String mappedVcid = (String) msgMap.get(appContext.getBean(MissionProperties.class).getVcidColumnName());
    				sb.append(mappedVcid != null ? mappedVcid : "");
    				break;

    			default:

    				if (appContext.getBean(MissionProperties.class).getVcidColumnName().toUpperCase().equalsIgnoreCase(col)) {
    					mappedVcid = (String) msgMap.get(appContext.getBean(MissionProperties.class).getVcidColumnName());
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
    public ITelemetryFrameMessage castMessage(final IMessage msg) {
        return (ITelemetryFrameMessage) msg;
    }
    
    @Override
    public boolean messageFilterCheck(final ITelemetryFrameMessage msg) {
        if (super.messageFilterCheck(msg)) {
            // if message isn't null, but the filter is (not filtering messages)
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
        return ITelemetryFrameMessage.class.toString();
    }
}
