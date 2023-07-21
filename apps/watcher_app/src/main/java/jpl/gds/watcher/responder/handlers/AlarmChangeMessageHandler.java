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
package jpl.gds.watcher.responder.handlers;

import jpl.gds.common.filtering.DssIdFilter;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.context.api.message.IContextHeartbeatMessage;
import jpl.gds.context.api.message.IContextMessage;
import jpl.gds.context.api.message.IEndOfContextMessage;
import jpl.gds.context.api.message.IStartOfContextMessage;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.client.channel.IChannelUtilityDictionaryManager;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.eha.api.message.aggregation.IAlarmChangeMessage;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.template.MessageTemplateManager;
import jpl.gds.shared.template.MissionConfiguredTemplateManagerFactory;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.template.TemplateManager;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.watcher.IResponderAppHelper;
import jpl.gds.watcher.responder.app.AlarmWatcherApp;
import jpl.gds.watcher.responder.app.MessageResponderApp;
import org.apache.velocity.Template;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * AlarmChangeMessageHandler is the MessageHandler implementation used by the message responder application to listen
 * for AlarmChange messages and put out alarm state change information to the console.
 */
public class AlarmChangeMessageHandler extends AbstractMessageHandler {

    /**
     * Default velocity template name for output from this handler.
     */
    public static final String DEFAULT_TEMPLATE = "onelinesummary";

    private static final String TEMPLATE_FOR_CHANNEL = " template for alarm messages";

    private       Template                         outputTemplate;
    private       MessageTemplateManager           templateManager;
    private       long                             alarmMessageCount;
    private       AlarmWatcherApp                  appHelper;
    private       Map<String, Boolean>             channelIds;
    private       SprintfFormat                    format;
    private       boolean                          recordedOnly         = false;
    private       boolean                          isShutdown           = false;
    /* Add set of DSS IDs */
    private       DssIdFilter                      dssIds               = null;
    private final IChannelUtilityDictionaryManager channelDictUtil;
    private final IContextIdentification           currentContextId;
    private final DictionaryProperties             currentDictConfig;
    private       boolean                          awaitingFirstContext = true;

    /**
     * Creates an instance of AlarmChangeMessageHandler.
     *
     * @param appContext the current application context
     */
    public AlarmChangeMessageHandler(final ApplicationContext appContext) {
        super(appContext);

        this.currentContextId = appContext.getBean(IContextIdentification.class);
        this.currentDictConfig = appContext.getBean(DictionaryProperties.class);
        this.channelDictUtil = appContext.getBean(IChannelUtilityDictionaryManager.class);

        try {
            templateManager = MissionConfiguredTemplateManagerFactory.getNewMessageTemplateManager(sseFlag);
            outputTemplate = templateManager.getTemplateForStyle(MessageRegistry.getMessageConfig(
                    EhaMessageType.AlarmChange), DEFAULT_TEMPLATE);
            if (outputTemplate == null) {
                writeError("Unable to locate " + DEFAULT_TEMPLATE
                        + TEMPLATE_FOR_CHANNEL);
            }
        } catch (final TemplateException e) {
            writeError("Unable to load " + DEFAULT_TEMPLATE
                    + TEMPLATE_FOR_CHANNEL, e);
        }

    }

    /**
     * Gets the number of EHA messages received by this handler.
     *
     * @return the EHA message count
     */
    public long getHandledEhaCount() {
        return alarmMessageCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void handleMessage(final IExternalMessage m) {
        try {
            final IMessage[] messages = externalMessageUtil.instantiateMessages(m);
            if (messages != null) {
                for (int i = 0; i < messages.length && !isShutdown; i++) {
                    final IMessage currentMsg = messages[i];
                    if (currentMsg.isType(EhaMessageType.AlarmChange)) {
                        if (alarmMessageCount == 0 && outputTemplate != null) {
                            final HashMap<String, Object> map = new HashMap<>();
                            map.put("header", true);
                            map.put("formatter", format);
                            final String text = TemplateManager.createText(
                                    outputTemplate, map);
                            System.out.print(text);
                        }
                        handleAlarmChangeMessage((IAlarmChangeMessage) messages[i]);

                    } else if (currentMsg instanceof IStartOfContextMessage) {
                        startContext((IStartOfContextMessage) messages[i]);

                    } else if (currentMsg instanceof IContextHeartbeatMessage) {
                        startContext((IContextHeartbeatMessage) messages[i]);

                    } else if (currentMsg instanceof IEndOfContextMessage) {
                        handleEndOfContext((IEndOfContextMessage) messages[i]);

                    } else {
                        writeError("AlarmChangeMessageHandler got an unrecognized message type: "
                                + currentMsg.getType());
                        continue;
                    }
                }
            }
        } catch (final Exception e) {
            writeError("AlarmChangeMessageHandler could not process message: "
                    + e.toString(), e);
        }
    }

    private void startContext(final IContextMessage message) {
        final ISimpleContextConfiguration newConfig = message.getContextConfiguration();
        writeLog("AlarmChangeMessageHandler got Start of Context or first Heartbeat message for context " + newConfig
                .getContextId().getNumber());

        /*
         * We always tolerate changes in session/context identification. We will
         * no longer tolerate changes in venue or dictionary information. We load
         * the dictionaries only once. The MessageRouter should be checking for
         * session changes we cannot handle and we should never get here as a result.
         */
        currentContextId.copyValuesFrom(newConfig.getContextId());
        if (awaitingFirstContext && newConfig instanceof IContextConfiguration) {
            currentDictConfig.copyValuesFrom(((IContextConfiguration) newConfig).getDictionaryConfig());
            format = new SprintfFormat(currentContextId.getSpacecraftId());
            try {
                if (currentDictConfig.useChannelFormatters()) {
                    this.channelDictUtil.loadAll(currentDictConfig, false);
                }

            } catch (final DictionaryException e) {
                writeLog("AlarmChangeMessageHandler could not load context channel dictionary");
            }
            awaitingFirstContext = false;
        }

    }

    private synchronized void handleAlarmChangeMessage(
            final IAlarmChangeMessage message) {
        alarmMessageCount++;
        writeLog("AlarmChangeMessageHandler got Alarm Change message: "
                + message.getOneLineSummary());
        if (recordedOnly) {
            if (message.isRealtime()) {
                return;
            }
        } else {
            if (!message.isRealtime()) {
                return;
            }
        }

        /* Logic for filtering M-channels by station */
        final IClientChannelValue val = message.getChannelValue();
        if (val.getDefinitionType().equals(ChannelDefinitionType.M)) {
            if (dssIds != null && !dssIds.accept(UnsignedInteger.valueOf(message.getChannelValue().getDssId()))) {
                return;
            }
        }

        if (channelIds != null
                && channelIds.get(val.getChanId()) == null) {
            return;
        }
        try {
            if (outputTemplate != null) {
                final HashMap<String, Object> map = new HashMap<>();
                map.put("body", true);
                map.put("formatter", format);
                message.setTemplateContext(map);
                final String text = TemplateManager.createText(outputTemplate, map);
                System.out.println(text);
            }
        } catch (final Exception e) {
            writeError("Unable to format channel message for output", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.watcher.IMessageHandler#shutdown()
     */
    @Override
    public synchronized void shutdown() {
        isShutdown = true;
        if (!awaitingFirstContext && alarmMessageCount > 0 && outputTemplate != null) {
            final HashMap<String, Object> map = new HashMap<>();
            map.put("trailer", true);
            map.put("formatter", format);
            final String text = TemplateManager.createText(outputTemplate, map);
            System.out.println(text);
        }
        writeLog("AlarmChangeMessageHandler is shutting down");
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.watcher.IMessageHandler#handleEndOfContext(IEndOfContextMessage)
     */
    @Override
    public synchronized void handleEndOfContext(final IEndOfContextMessage m) {
        final IContextKey tc = m.getContextKey();
        if (tc == null || tc.getNumber() == null) {
            writeLog("AlarmChangeMessageHandler received End of Context message for an unknown context; skipping");
            return;
        }
        writeLog("AlarmChangeMessageHandler received End of Context message for context "
                + tc.getNumber());
        currentContextId.clearFieldsForNewConfiguration();
        if (appHelper != null && appHelper.isExitSession()) {
            shutdown();
            MessageResponderApp.getInstance().markDone();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.watcher.IMessageHandler#setAppHelper(IResponderAppHelper)
     */
    @Override
    public synchronized void setAppHelper(final IResponderAppHelper app) {
        appHelper = (AlarmWatcherApp) app;
        final String[] chans = appHelper.getChannelIds();
        if (chans != null) {
            channelIds = new HashMap<>();
            for (int i = 0; i < chans.length; i++) {
                channelIds.put(chans[i], true);
            }
        }
        final String templateName = appHelper.getTemplateName();
        try {
            outputTemplate = templateManager.getTemplateForStyle(
                    MessageRegistry.getMessageConfig(EhaMessageType.AlarmChange), templateName);
            if (outputTemplate == null) {
                writeError("Unable to locate " + templateName
                        + TEMPLATE_FOR_CHANNEL);
            }
        } catch (final TemplateException e) {
            writeError("Unable to load " + templateName
                    + TEMPLATE_FOR_CHANNEL);
            outputTemplate = null;
        }
        recordedOnly = appHelper.isRecorded();

        /* Set list of desired stations */
        dssIds = appHelper.getDssIdFilter();
    }

    /**
     * Gets the current context key.
     *
     * @return IContextKey
     */
    public IContextKey getContextId() {
        return this.currentContextId == null ? null : this.currentContextId.getContextKey();
    }

}
