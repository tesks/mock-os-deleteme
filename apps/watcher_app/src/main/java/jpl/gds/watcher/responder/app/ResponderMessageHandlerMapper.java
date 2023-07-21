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
package jpl.gds.watcher.responder.app;

import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.evr.api.message.EvrMessageType;
import jpl.gds.product.api.message.ProductMessageType;
import jpl.gds.session.message.SessionMessageType;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.time.api.message.TimeCorrelationMessageType;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.watcher.IMessageHandler;
import jpl.gds.watcher.ResponderMessageType;
import jpl.gds.watcher.responder.handlers.*;

import java.util.HashMap;
import java.util.Map;

/**
 * A class that maps message responder name/message type combination to a message responder
 * handler class.
 */
public class ResponderMessageHandlerMapper extends HashMap<IResponderAppName, Map<IMessageType, Class<? extends IMessageHandler>>>
        implements Map<IResponderAppName, Map<IMessageType, Class<? extends IMessageHandler>>> {
    private static final long  serialVersionUID                  = -3310274443418703448L;

    
    /**
     * Constructor.
     */
    public ResponderMessageHandlerMapper() {
        super();
        for (final ResponderAppName appName : ResponderAppName.values()) {
            this.put(appName, new HashMap<>());
            final Map<IMessageType, Class<? extends IMessageHandler>> appMap = this.get(appName);
            switch(appName) {
                case CHANNEL_CHANGE_WATCHER_APP_NAME:
                    appMap.put(EhaMessageType.AlarmedEhaChannel, ChannelChangeMessageHandler.class);
                    appMap.put(SessionMessageType.StartOfSession, ChannelChangeMessageHandler.class);
                    appMap.put(SessionMessageType.SessionHeartbeat, ChannelChangeMessageHandler.class);
                    appMap.put(SessionMessageType.EndOfSession, ChannelChangeMessageHandler.class);
                    break;
                case CHANNEL_SAMPLE_WATCHER_APP_NAME:
                    appMap.put(EhaMessageType.AlarmedEhaChannel, ChannelSampleMessageHandler.class);
                    appMap.put(SessionMessageType.StartOfSession, ChannelSampleMessageHandler.class);
                    appMap.put(SessionMessageType.SessionHeartbeat, ChannelSampleMessageHandler.class);
                    appMap.put(SessionMessageType.EndOfSession, ChannelSampleMessageHandler.class);
                    break;
                case EVR_WATCHER_APP_NAME:
                    appMap.put(EvrMessageType.Evr, EvrMessageHandler.class);
                    appMap.put(SessionMessageType.StartOfSession, EvrMessageHandler.class);
                    appMap.put(SessionMessageType.SessionHeartbeat, EvrMessageHandler.class);
                    appMap.put(SessionMessageType.EndOfSession, EvrMessageHandler.class);
                    break;
                case PACKET_WATCHER_APP_NAME:
                    appMap.put(TmServiceMessageType.TelemetryPacketSummary, PacketExtractSumMessageHandler.class);
                    appMap.put(SessionMessageType.StartOfSession, PacketExtractSumMessageHandler.class);
                    appMap.put(SessionMessageType.SessionHeartbeat, PacketExtractSumMessageHandler.class);
                    appMap.put(SessionMessageType.EndOfSession, PacketExtractSumMessageHandler.class);
                    break;
                case RECORDED_ENG_WATCHER_APP_NAME:
                    appMap.put(ProductMessageType.ProductAssembled, ProductMessageHandler.class);
                    appMap.put(SessionMessageType.StartOfSession, ProductMessageHandler.class);
                    appMap.put(SessionMessageType.SessionHeartbeat, ProductMessageHandler.class);
                    appMap.put(SessionMessageType.EndOfSession, ProductMessageHandler.class);
                    break;
                case PRODUCT_WATCHER_APP_NAME:
                    appMap.put(ProductMessageType.ProductAssembled, ProductWatcherMessageHandler.class);
                    appMap.put(ProductMessageType.PartialProduct, ProductWatcherMessageHandler.class);
                    appMap.put(SessionMessageType.StartOfSession, ProductWatcherMessageHandler.class);
                    appMap.put(SessionMessageType.SessionHeartbeat, ProductWatcherMessageHandler.class);
                    appMap.put(SessionMessageType.EndOfSession, ProductWatcherMessageHandler.class);
                    break;
                case TIME_CORRELATION_WATCHER_APP_NAME:
                    appMap.put(TimeCorrelationMessageType.FswTimeCorrelation, TimeCorrelationMessageHandler.class);
                    appMap.put(TimeCorrelationMessageType.SseTimeCorrelation, TimeCorrelationMessageHandler.class);
                    appMap.put(SessionMessageType.StartOfSession, TimeCorrelationMessageHandler.class);
                    appMap.put(SessionMessageType.EndOfSession, TimeCorrelationMessageHandler.class);
                    appMap.put(SessionMessageType.SessionHeartbeat, TimeCorrelationMessageHandler.class);
                    break;
                case TRIGGER_SCRIPT_APP_NAME:
                    appMap.put(ResponderMessageType.Any, ExecuteScriptMessageHandler.class);
                    break;
                case ALARM_WATCHER_APP_NAME:
                    appMap.put(EhaMessageType.AlarmChange, AlarmChangeMessageHandler.class);
                    appMap.put(SessionMessageType.StartOfSession, AlarmChangeMessageHandler.class);
                    appMap.put(SessionMessageType.SessionHeartbeat, AlarmChangeMessageHandler.class);
                    appMap.put(SessionMessageType.EndOfSession, AlarmChangeMessageHandler.class);
                    break;
                default:
                    break;
                
            }
        }

    }


    /**
     * Gets the IMessageHandler class for the supplied responder application and message type.
     * 
     * @param appNameEnum enum value corresponding to the responder application
     * @param msgType message type
     * @return message handler, or null if no mapping
     */
    public Class<? extends IMessageHandler> get(final IResponderAppName appNameEnum, final IMessageType msgType) {
        final Map<IMessageType, Class<? extends IMessageHandler>> map = get(appNameEnum);
        if (map == null)  {
            return null;
        }
        return map.get(msgType);
    }
}
