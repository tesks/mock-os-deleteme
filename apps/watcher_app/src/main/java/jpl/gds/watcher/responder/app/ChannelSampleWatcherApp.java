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
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.watcher.responder.handlers.ChannelSampleMessageHandler;
import org.springframework.context.ApplicationContext;

import java.util.List;

/**
 * This is the application helper class for a message responder that displays channel values from the message bus.
 */
public class ChannelSampleWatcherApp extends AbstractEhaWatcherApp {
    private static final String APP_NAME = ApplicationConfiguration
            .getApplicationName(ResponderAppName.CHANNEL_SAMPLE_WATCHER_APP_NAME.getAppName());

    /**
     * Constructor
     *
     * @param appContext the current application context
     */
    public ChannelSampleWatcherApp(final ApplicationContext appContext) {
        super(appContext, APP_NAME, EhaMessageType.AlarmedEhaChannel, ChannelSampleMessageHandler.DEFAULT_TEMPLATE);
    }

    @Override
    protected List<String> getOverrideTypesList() {
        List<String> overrideTypes = super.getOverrideTypesList();
        overrideTypes.add(0, EhaMessageType.AlarmedEhaChannel.getSubscriptionTag());
        return overrideTypes;
    }

}
