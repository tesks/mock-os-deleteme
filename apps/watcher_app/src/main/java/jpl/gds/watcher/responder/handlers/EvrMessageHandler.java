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

import java.util.HashMap;

import jpl.gds.context.api.ISimpleContextConfiguration;
import org.apache.velocity.Template;
import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.message.IContextHeartbeatMessage;
import jpl.gds.context.api.message.IContextMessage;
import jpl.gds.context.api.message.IEndOfContextMessage;
import jpl.gds.context.api.message.IStartOfContextMessage;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.message.EvrMessageType;
import jpl.gds.evr.api.message.IEvrMessage;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.template.MessageTemplateManager;
import jpl.gds.shared.template.MissionConfiguredTemplateManagerFactory;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.template.TemplateManager;
import jpl.gds.watcher.IResponderAppHelper;
import jpl.gds.watcher.responder.app.EvrWatcherApp;
import jpl.gds.watcher.responder.app.MessageResponderApp;


/**
 * EvrMessageHandler is the MessageHandler implementation used by the
 * message responder application to listen for EVR messages and put them to the
 * console.
 */
public class EvrMessageHandler extends AbstractMessageHandler {
	/** Default velocity template name for output from this handler. */
    public static final String DEFAULT_TEMPLATE = "onelinesummary";

    private final EvrFilter evrFilter = new EvrFilter();
    private Template outputTemplate;
    private MessageTemplateManager templateManager;
    private long evrMessageCount;
    private EvrWatcherApp appHelper;
    private SprintfFormat format;
    private boolean awaitingFirstContext = true;
    private final IContextIdentification currentContextId;
	private final IContextFilterInformation currentScFilterInfo;

    /**
     * Creates an instance of EvrMessageHandler.
     * @param appContext the current application context
     */
    public EvrMessageHandler(final ApplicationContext appContext) {
    	super(appContext);
    	
    	this.currentContextId = appContext.getBean(IContextIdentification.class);
    	this.currentScFilterInfo = appContext.getBean(IContextFilterInformation.class);
    	
        try {
            this.templateManager = MissionConfiguredTemplateManagerFactory.getNewMessageTemplateManager(sseFlag);
            this.outputTemplate = this.templateManager.getTemplateForStyle(MessageRegistry.getMessageConfig(
                    EvrMessageType.Evr), DEFAULT_TEMPLATE);
            if (this.outputTemplate == null) {
                writeError("Unable to locate " + DEFAULT_TEMPLATE
                        + " template for EVR messages");
            }
        } catch (final TemplateException e) {
            writeError("Unable to load template " + DEFAULT_TEMPLATE, e);
        }

    }

    /**
     * Gets the number of EVR messages received by this handler.
     * 
     * @return the EVR message count
     */
    public long getHandledEvrCount() {
        return this.evrMessageCount;
    }

    /**
     * {@inheritDoc}
     * 
     */
    @Override
    public synchronized void handleMessage(final IExternalMessage m) {
        try {
            final jpl.gds.shared.message.IMessage[] messages = externalMessageUtil.instantiateMessages(m);
            if (messages != null) {
                for (int i = 0; i < messages.length; i++) {
                    final IMessage currentMsg = messages[i];
                    if (currentMsg.isType(EvrMessageType.Evr)) {
                        if (this.evrMessageCount == 0) {
                            if (this.outputTemplate != null) {
                                final HashMap<String, Object> map = new HashMap<>();
                                map.put("header", true);
                                map.put("formatter", this.format);
                                final String text = TemplateManager.createText(
                                        this.outputTemplate, map);
                                System.out.print(text);
                            }
                        }
                        handleEvrMessage((IEvrMessage) messages[i]);

                    } else if (currentMsg instanceof IStartOfContextMessage) {
                        startContext((IStartOfContextMessage) messages[i]);

                    } else if (currentMsg instanceof IContextHeartbeatMessage) {
                        startContext((IContextHeartbeatMessage)messages[i]);

                    } else if (currentMsg instanceof IEndOfContextMessage) {
                        handleEndOfContext((IEndOfContextMessage) messages[i]);

                    } else {
                        writeError("EvrSummaryMessageHandler got an unrecognized message type: "
                                + currentMsg.getType());
                        continue;
                    }
                }
            }
        } catch (final Exception e) {
            writeError("EvrSummaryMessageHandler could not process message: "
                    + e.toString(), e);
        }
    }

    private void startContext(final IContextMessage message) {
    	final ISimpleContextConfiguration newConfig = message.getContextConfiguration();
    	writeLog("EvrMessageHandler got Start of Context or first Heartbeat message for context " + newConfig.getContextId().getNumber());
		/*
		 * We always tolerate changes in session/context
		 * identification. We will no longer tolerate changes in venue or
		 * dictionary information. The MessageRouter should be checking for
		 * session changes we cannot handle and we should never get here as a
		 * result.
		 */
    	currentContextId.copyValuesFrom(newConfig.getContextId());
    	if (awaitingFirstContext && newConfig instanceof IContextConfiguration) {
    		currentScFilterInfo.copyValuesFrom(newConfig.getFilterInformation());
            format = new SprintfFormat(currentContextId.getSpacecraftId());
            awaitingFirstContext = false;
        }
    }

    private synchronized void handleEvrMessage(final IEvrMessage message) {
        this.evrMessageCount++;
        writeLog("EvrSummaryMessageHandler got EVR message: "
                + message.getOneLineSummary());

        final IEvr val = message.getEvr();

        try {
            if (this.evrFilter.matches(val) && this.outputTemplate != null) {
                final HashMap<String, Object> map = new HashMap<>();
                map.put("body", true);
                map.put("formatter", this.format);
                message.setTemplateContext(map);
                final String text = TemplateManager.createText(this.outputTemplate,
                        map);
                System.out.println(text);
            }
        } catch (final Exception e) {
            writeError("Unable to format EVR message for output", e);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IMessageHandler#shutdown()
     */
    @Override
    public synchronized void shutdown() {
        if (!awaitingFirstContext && this.evrMessageCount > 0) {
            if (this.outputTemplate != null) {
                final HashMap<String, Object> map = new HashMap<>();
                map.put("trailer", true);
                map.put("formatter", this.format);
                final String text = TemplateManager.createText(this.outputTemplate,
                        map);
                System.out.print(text);
            }
        }
        writeLog("EvrSummaryMessageHandler is shutting down");
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IMessageHandler#handleEndOfContext(IEndOfContextMessage)
     */
    @Override
    public synchronized void handleEndOfContext(final IEndOfContextMessage m) {
        final IContextKey tc = m.getContextKey();
        if (tc == null || tc.getNumber() == null) {
            writeLog("EvrSummaryMessageHandler received End of Context message for an unknown context; skipping");
            return;
        }
        writeLog("EvrSummaryMessageHandler received End of Context message for context "
                + tc.getNumber());
        if (this.appHelper != null && this.appHelper.isExitSession()) {
            shutdown();
            MessageResponderApp.getInstance().markDone();
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IMessageHandler#setAppHelper(jpl.gds.watcher.IResponderAppHelper)
     */
    @Override
    public void setAppHelper(final IResponderAppHelper app) {
        this.appHelper = (EvrWatcherApp) app;
        final String[] levels = this.appHelper.getLevels();
        final String[] ids = this.appHelper.getIds();
        final String[] modules = this.appHelper.getModules();
        final String name = this.appHelper.getEvrNamePattern();
        final boolean realtimeOnly = this.appHelper.isRealtime();
        final boolean recordedOnly = this.appHelper.isRecorded();
        final boolean fswOnly = this.appHelper.isFswOnly();
        final boolean sseOnly = this.appHelper.isSseOnly();

        final String templateName = this.appHelper.getTemplateName();
        try {
            this.outputTemplate = this.templateManager.getTemplateForStyle(
                    MessageRegistry.getMessageConfig(EvrMessageType.Evr), templateName);
            if (this.outputTemplate == null) {
                writeError("Unable to locate " + templateName
                        + " template for EVR messages");
            }
        } catch (final TemplateException e) {
            writeError("Unable to load " + templateName
                    + " template for EVR messages");
            this.outputTemplate = null;
        }
        this.evrFilter.setLevels(levels);
        this.evrFilter.setIds(ids);
        this.evrFilter.setRecordedOnly(recordedOnly);
        this.evrFilter.setRealtimeOnly(realtimeOnly);
        this.evrFilter.setFswOnly(fswOnly);
        this.evrFilter.setSseOnly(sseOnly);
        this.evrFilter.setEvrName(name);
        this.evrFilter.setModules(modules);
    }

    /**
     * Evr filtering
     */
    private static class EvrFilter {
        private String[] levels;
        private String[] ids;
        private String[] modules;
        private String evrName;
        private boolean sseOnly;
        private boolean fswOnly;
        private boolean realtimeOnly;
        private boolean recordedOnly;

        /**
         * @param levels evr levels
         */
        public void setLevels(final String[] levels) {
            this.levels = levels;
        }

        /**
         * @param ids evr ids
         */
        public void setIds(final String[] ids) {
            this.ids = ids;
        }

        /**
         * @param sseOnly whether the filter is sse only
         */
        public void setSseOnly(final boolean sseOnly) {
            this.sseOnly = sseOnly;
        }

 
        /**
         * @param fswOnly fsw only
         */
        public void setFswOnly(final boolean fswOnly) {
            this.fswOnly = fswOnly;
        }

        /**
         * @param recordedOnly recorded only toggle
         */
        public void setRecordedOnly(final boolean recordedOnly) {
            this.recordedOnly = recordedOnly;
        }

        /**
         * @param realtimeOnly recorded only toggle
         */
        public void setRealtimeOnly(final boolean realtimeOnly) {
            this.realtimeOnly = realtimeOnly;
        }

        /**
         * @param modules array of modules to set
         */
        public void setModules(final String[] modules) {
            this.modules = modules;
        }

        /**
         * @param evrName evr name
         */
        public void setEvrName(final String evrName) {
            this.evrName = evrName;
        }

        /**
         * Tests if the passed evr matches the filter.
         * @param evr evr to test for matching
         * @return whether the evr matches the filter
         */
        public boolean matches(final IEvr evr) {

            if (this.fswOnly && evr.isFromSse()) {
                return false;
            }

            if (this.sseOnly && !evr.isFromSse()) {
                return false;
            }

            // Realtime/recorded meaningless unless FSW
            if (! evr.isFromSse())
            {
                if (this.realtimeOnly && ! evr.isRealtime())
                {
                    return false;
                }

                if (this.recordedOnly && evr.isRealtime())
                {
                    return false;
                }
            }

            boolean levelMatched = false;

            if (levels != null && evr.getLevel() != null) {
                for (final String level : levels) {
                    if (evr.getLevel().equalsIgnoreCase(level)) {
                        levelMatched = true;
                        break;
                    }
                }
            } else {
                levelMatched = true;
            }

            if (!levelMatched) {
                return false;
            }

            boolean moduleMatched = false;
            /* New call to categories. */
            if (modules != null && evr.getCategory(IEvrDefinition.MODULE) != null) {
                for (final String module : modules) {
                    if (evr.getCategory(IEvrDefinition.MODULE).equalsIgnoreCase(module)) {
                        moduleMatched = true;
                        break;
                    }
                }
            } else {
                moduleMatched = true;
            }

            if (!moduleMatched) {
                return false;
            }

            boolean idMatched = false;

            if (ids != null) {
                for (final String id : ids) {
                    long numId = 0;
                    try {
                        numId = Long.valueOf(id);
                    } catch (final NumberFormatException e) {
                        // do nothing
                    }
                    if (evr.getEventId() == numId) {
                        idMatched = true;
                        break;
                    }
                }
            } else {
                idMatched = true;
            }

            if (!idMatched) {
                return false;
            }

            boolean nameMatched = false;

            if (this.evrName != null && evr.getName() != null) {
                if (evr.getName().matches(this.evrName)) {
                    nameMatched = true;
                }
            } else {
                nameMatched = true;
            }

            return nameMatched;
        
        }
    }
}
