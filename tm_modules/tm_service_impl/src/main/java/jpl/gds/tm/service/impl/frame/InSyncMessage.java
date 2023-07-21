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
/* */
package jpl.gds.tm.service.impl.frame;
import org.springframework.context.ApplicationContext;

import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.IFrameEventMessage;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;

/**
 * InSyncMessage is the message sent when an in-sync state of the frame
 * synchronization stage of telemetry processing is entered or assumed.
 * 
 */
public class InSyncMessage extends AbstractFrameEventMessage implements Templatable {
	
    /**
     * This is a String to store the Type as "InSync"
     */
    public static final String TYPE = "InSync";
    /**
     * This is a DSNInfo object to store the DSN info
     */
    protected IStationTelemInfo dsnInfo;
    /**
     * This is an IFrameInfo object to store the transfer frame info.
     */
    protected ITelemetryFrameInfo tfInfo;
    
    /**
     * Constructor.
     * 
     * @param dsnI the IStationTelemInfo object associated with the frame event.
     * @param tfI the ITelemetryFrameInfo object for the latest in-sync frame
     */
    protected InSyncMessage(final IStationTelemInfo dsnI, final ITelemetryFrameInfo tfI) {
        super(TmServiceMessageType.InSync, LogMessageType.IN_SYNC, dsnI, tfI);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.message.PublishableLogMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        return super.getOneLineSummary("IN SYNC");      
    }
 
    /**
     * XML parser class for this message.
     * 
     */
    public static class XmlParseHandler extends FrameEventMessageParseHandler {
        
        /**
         * Constructor.
         * @param appContext the current application context
         */
        public XmlParseHandler(final ApplicationContext appContext) {
            super(appContext, TmServiceMessageType.InSync);
        }

        /**
         * @{inheritDoc}
         * @see jpl.gds.tm.service.impl.frame.AbstractFrameEventMessage.FrameEventMessageParseHandler#createMessage(jpl.gds.station.api.IStationTelemInfo, jpl.gds.tm.service.api.frame.ITelemetryFrameInfo, java.util.Date)
         */
        @Override
        protected IFrameEventMessage createMessage(
                                                   final IStationTelemInfo stationInfo,
                                                   final ITelemetryFrameInfo tfInfo, final IAccurateDateTime time) {
            final IFrameEventMessage msg = new InSyncMessage(stationInfo, tfInfo);
            msg.setEventTime(time);
            
            return msg;
        }
    }

    
}