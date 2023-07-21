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
package jpl.gds.watcher.spring.bootstrap;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.reflect.ReflectionToolkit;
import jpl.gds.watcher.IMessageHandler;
import jpl.gds.watcher.IResponderAppHelper;
import jpl.gds.watcher.app.handler.CfdpPduCaptureHandler;
import jpl.gds.watcher.app.handler.FrameCaptureHandler;
import jpl.gds.watcher.app.handler.ICaptureHandler;
import jpl.gds.watcher.app.handler.ITelemetryIngestionCaptureHandler;
import jpl.gds.watcher.app.handler.PacketCaptureHandler;
import jpl.gds.watcher.responder.app.IResponderAppName;
import jpl.gds.watcher.responder.app.ResponderAppHelperMapper;
import jpl.gds.watcher.responder.app.ResponderAppName;
import jpl.gds.watcher.responder.app.ResponderMessageHandlerMapper;

/**
 * This is the spring bootstrap configuration class for the message responder (watcher)
 * applications.
 */
@Configuration
public class WatcherSpringBootstrap {
    
    /** Bean name for message responder (watcher) name enumerated value */
    public static final String      RESPONDER_NAME                     = "RESPONDER_NAME";
    /** Bean name for message responder (watcher) application helper */
    public static final String      RESPONDER_APP_HELPER               = "RESPONDER_APP_HELPER";
    /** Bean name for message responder (watcher) application helper map */
    public static final String      RESPONDER_APP_HELPER_MAPPER        = "RESPONDER_APP_HELPER_MAPPER";
    /** Bean name for message responder (watcher) message handlers */
    public static final String      RESPONDER_MESSAGE_HANDLER          = "RESPONDER_MESSAGE_HANDLER";
    /** Bean name for message responder (watcher) message handler helper map */
    public static final String      RESPONDER_MESSAGE_HANDLER_MAPPER   = "RESPONDER_MESSAGE_HANDLER_MAPPER";
    /** Bean name for frame capture message handlers */
    public static final String      FRAME_CAPTURE_HANDLER              = "FRAME_CAPTURE_HANDLER";
    /** Bean name for packet capture message handlers */
    public static final String      PACKET_CAPTURE_HANDLER             = "PACKET_CAPTURE_HANDLER";
    /** Bean name for CFDP PDU capture message handlers */
    public static final String      CFDP_PDU_CAPTURE_HANDLER           = "CFDP_PDU_CAPTURE_HANDLER";

    @Autowired
    private ApplicationContext       appContext;
    
    /* Removed auto-wiring of helper map for performance reasons. */
    
    private final Map<Class<? extends IMessageHandler>, IMessageHandler> handlerMap = new HashMap<>();

    /**
     * @return a singleton instance of a ResponderMesageHandlerMapper object
     */
    @Bean(name = RESPONDER_MESSAGE_HANDLER_MAPPER)
    @Scope("singleton")
    @Lazy(value = true)
    public ResponderMessageHandlerMapper getResponderMessageHandlerMapper() {
        return new ResponderMessageHandlerMapper();
    }
    
    /**
     * @return a singleton instance of a ResponderAppHelperMapper object
     */
    @Bean(name = RESPONDER_APP_HELPER_MAPPER)
    @Scope("singleton")
    @Lazy(value = true)
    public ResponderAppHelperMapper getResponderAppHelperMapper() {
        return new ResponderAppHelperMapper();
    }

    /**
     * @param helperMap ResponderAppHelperMapper bean, autowired
     * @return the ResponderAppHelper required for the current Watcher App.
     */
    @SuppressWarnings("unchecked")
    @Bean(name = RESPONDER_APP_HELPER)
    @Scope("singleton")
    @Lazy(value = true)
    public IResponderAppHelper getResponderAppHelper(final ResponderAppHelperMapper helperMap) {
        try {
            final String appName = ApplicationConfiguration.getApplicationName();
            final IResponderAppName appNameEnum = appContext.getBean(IResponderAppName.class, appName);
            final Class<? extends IResponderAppHelper> klass = helperMap.get(appNameEnum);
            if (null == klass) {
                throw new IllegalArgumentException(
                        "Could not find class for ResponderAppHelper implementation for \"" + appName + "\"");
            }
            final Constructor<? extends IResponderAppHelper> ctor = (Constructor<? extends IResponderAppHelper>) ReflectionToolkit
                    .getConstructor(klass, new Class<?>[] { ApplicationContext.class });
            return ctor.newInstance(appContext);
        }
        catch (final Exception e) {
            TraceManager.getDefaultTracer().error(
                    "Could not load ResponderAppHelper implementation for " + ApplicationConfiguration.getApplicationName(), e);
            return null;
        }
    }
    
    /**
     * @param appName Responder/watcher script name
     * @return Prototype responder name enumerated value.
     */
    @Bean(name = RESPONDER_NAME)
    @Scope("prototype")
    @Lazy(value = true)
    public IResponderAppName getResponderAppNameEnum(final String appName) {
          return ResponderAppName.valueOfAppName(appName);
    }
    
    /**
     * @param msgType the message type to create the handler for
     * @return the IMessageHandler required for the current Watcher App and message type.
     */
    @SuppressWarnings("unchecked")
    @Bean(name = RESPONDER_MESSAGE_HANDLER)
    @Scope("prototype")
    @Lazy(value = true)
    public IMessageHandler getResponderMessageHandler(final IMessageType msgType) {
        try {
            final String appName = ApplicationConfiguration.getApplicationName();
            final IResponderAppName appNameEnum = appContext.getBean(IResponderAppName.class, appName);
            final ResponderMessageHandlerMapper helperMap = appContext.getBean(ResponderMessageHandlerMapper.class);
            final Class<? extends IMessageHandler> klass = helperMap.get(appNameEnum, msgType);
            if (handlerMap.get(klass) != null) {
                return handlerMap.get(klass);
            }
            if (null == klass) {
                return null;
            }
            final Constructor<? extends IMessageHandler> ctor = (Constructor<? extends IMessageHandler>) ReflectionToolkit
                    .getConstructor(klass, new Class<?>[] { ApplicationContext.class });
            final IMessageHandler handler = ctor.newInstance(appContext);
            handlerMap.put(klass, handler);
            return handler;
        }
        catch (final Exception e) {
            TraceManager.getDefaultTracer().error(
                    "Could not load MessageHandler implementation for " + ApplicationConfiguration.getApplicationName() + 
                    ", message type " + msgType.toString(), e);
            return null;
        }
    }
    
    @Bean(name = FRAME_CAPTURE_HANDLER)
    @Scope("singleton")
    @Lazy(value = true)
    public ITelemetryIngestionCaptureHandler getFrameCaptureHandler() {
    	return new FrameCaptureHandler(appContext);
    }
    
    @Bean(name = PACKET_CAPTURE_HANDLER)
    @Scope("singleton")
    @Lazy(value = true)
    public ITelemetryIngestionCaptureHandler getPacketCaptureHandler() {
        return new PacketCaptureHandler(appContext);
    }

    @Bean(name = CFDP_PDU_CAPTURE_HANDLER)
    @Scope("singleton")
    @Lazy(value = true)
    public ICaptureHandler getCfdpPduCaptureHandler() {
        return new CfdpPduCaptureHandler(appContext);
    }
}
