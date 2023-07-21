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

import java.util.HashMap;
import java.util.Map;

import jpl.gds.watcher.IResponderAppHelper;

/**
 * A class that maps Message Responder App Names to their respective IResponderAppHelper instantiation.
 */
public class ResponderAppHelperMapper extends HashMap<IResponderAppName, Class<? extends IResponderAppHelper>>
        implements Map<IResponderAppName, Class<? extends IResponderAppHelper>> {
    private static final long  serialVersionUID                  = -3310274443418703448L;
    
    /**
     * Constructor. Adds the multimission responder app helpers to the map.
     */
    public ResponderAppHelperMapper() {
        super();
        this.put(ResponderAppName.CHANNEL_CHANGE_WATCHER_APP_NAME, ChannelChangeWatcherApp.class);
        this.put(ResponderAppName.CHANNEL_SAMPLE_WATCHER_APP_NAME, ChannelSampleWatcherApp.class);
        this.put(ResponderAppName.EVR_WATCHER_APP_NAME, EvrWatcherApp.class);
        this.put(ResponderAppName.PACKET_WATCHER_APP_NAME, PacketWatcherApp.class);
        this.put(ResponderAppName.RECORDED_ENG_WATCHER_APP_NAME, RecordedProductHandlerApp.class);
        this.put(ResponderAppName.PRODUCT_WATCHER_APP_NAME, ProductWatcherApp.class);
        this.put(ResponderAppName.TIME_CORRELATION_WATCHER_APP_NAME, TimeCorrelationWatcherApp.class);
        this.put(ResponderAppName.TRIGGER_SCRIPT_APP_NAME, TriggerScriptApp.class);
        this.put(ResponderAppName.ALARM_WATCHER_APP_NAME, AlarmWatcherApp.class);
    }
}
