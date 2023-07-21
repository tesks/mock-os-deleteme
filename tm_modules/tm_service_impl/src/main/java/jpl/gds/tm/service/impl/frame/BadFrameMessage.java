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
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.IFrameEventMessage;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;

/**
 * BadFrameMessage is the message sent when an invalid transfer frame
 * is detected.
 * 
 */
public class BadFrameMessage extends AbstractFrameEventMessage {
	
    /**
     * Creates an instance of BadFrameMessage.
     *
     * @param dsnI the DSNInfo object associated with this message
     * @param tfI the IFrameInfo object associated with this message
      */
    protected BadFrameMessage(final IStationTelemInfo dsnI, final ITelemetryFrameInfo tfI) {
        super(TmServiceMessageType.BadTelemetryFrame,
                TraceSeverity.WARNING,
                LogMessageType.INVALID_TF,
                dsnI,
                tfI);
    }


    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.message.PublishableLogMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        return super.getOneLineSummary("BAD FRAME") +    
                ", Reason=" + (getFrameInfo() == null ? "Unknown" : getFrameInfo().getBadReason());
    }


    /**
     * XmlParseHandler is the message-specific SAX parse handler for creating this Message
     * from its XML representation.
     */
    public static class XmlParseHandler extends FrameEventMessageParseHandler {
        
        /** 
         * Constructor.
         * @param appContext the current applicationContext
         */
        protected XmlParseHandler(final ApplicationContext appContext) {
            super(appContext, TmServiceMessageType.BadTelemetryFrame);
        }
        

        /**
         * @{inheritDoc}
         * @see jpl.gds.tm.service.impl.frame.AbstractFrameEventMessage.FrameEventMessageParseHandler#createMessage(jpl.gds.station.api.IStationTelemInfo, jpl.gds.tm.service.api.frame.ITelemetryFrameInfo, java.util.Date)
         */
        @Override
        protected IFrameEventMessage createMessage(final IStationTelemInfo stationInfo,
                                                   final ITelemetryFrameInfo tfInfo, final IAccurateDateTime time) {
            final IFrameEventMessage msg = new BadFrameMessage(stationInfo, tfInfo);
            msg.setEventTime(time);
            return msg;
        }
        
    }
}