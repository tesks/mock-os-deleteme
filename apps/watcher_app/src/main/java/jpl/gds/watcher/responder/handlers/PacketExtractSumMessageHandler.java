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
import java.util.Iterator;
import java.util.Map;

import jpl.gds.context.api.ISimpleContextConfiguration;
import org.apache.velocity.Template;
import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.message.IContextHeartbeatMessage;
import jpl.gds.context.api.message.IContextMessage;
import jpl.gds.context.api.message.IEndOfContextMessage;
import jpl.gds.context.api.message.IStartOfContextMessage;
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
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.packet.IPacketSummaryMessage;
import jpl.gds.tm.service.api.packet.PacketSummaryRecord;
import jpl.gds.watcher.IResponderAppHelper;
import jpl.gds.watcher.responder.app.MessageResponderApp;
import jpl.gds.watcher.responder.app.PacketWatcherApp;

/**
 * This is the application helper class for a message responder that displays
 * Packet Extract Summaries from the message bus.
 */
public class PacketExtractSumMessageHandler extends AbstractMessageHandler {
    
	/**
	 * Xml configuration tag for default template.
	 */
	public static final String DEFAULT_TEMPLATE = "table";

	private Template outputTemplate;
	private MessageTemplateManager templateManager;
	private long packeExtractSumMessageCount;
	private PacketWatcherApp appHelper;
	private SprintfFormat format;
	private String[] apids;
	private String[] apidNames;
	private UnsignedInteger vcid;
	private Map<String, PacketSummaryRecord> prevSumMap = null;
	private boolean headerShown = false;
	private boolean minify = false;
	private boolean csvHeader = false;
	private final PesFilter pesFilter = new PesFilter();
	private boolean awaitingFirstContext = true;
	private IContextIdentification currentContextId;
	private final IContextFilterInformation currentScFilterInfo;
	/**
	 * Creates an instance of PacketExtractSumMessageHandler.
	 * @param appContext the current application context
	 */
	public PacketExtractSumMessageHandler(final ApplicationContext appContext) {
		super(appContext);
		this.currentContextId = appContext.getBean(IContextIdentification.class);
		this.currentScFilterInfo = appContext.getBean(IContextFilterInformation.class);
		
		try {
            templateManager = MissionConfiguredTemplateManagerFactory.getNewMessageTemplateManager(sseFlag);
			outputTemplate = templateManager.getTemplateForStyle(MessageRegistry.getMessageConfig(
					TmServiceMessageType.TelemetryPacketSummary), DEFAULT_TEMPLATE);
			if (outputTemplate == null) {
				writeError("Unable to locate " + DEFAULT_TEMPLATE
						+ " template for Packet Extract Summary messages");
			}
		} catch (final TemplateException e) {
			writeError("Error creating default template for packet summary output: " + e.toString(), e);
		}

	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.watcher.IMessageHandler#handleEndOfContext(IEndOfContextMessage)
	 */
	@Override
	public void handleEndOfContext(final IEndOfContextMessage m) {
		final IContextKey tc = m.getContextKey();
		if (tc == null || tc.getNumber() == null) {
			writeLog("PacketExtractSumMessageHandler received End of Context message for an unknown context; skipping");
			return;
		}
		writeLog("PacketExtractSumMessageHandler received End of Context message for context "
				+ tc.getNumber());
		currentContextId = null;
		if (appHelper != null && appHelper.isExitContext()) {
			shutdown();
			MessageResponderApp.getInstance().markDone();
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public synchronized void handleMessage(final IExternalMessage m) {
		try {
			final jpl.gds.shared.message.IMessage[] messages = externalMessageUtil.instantiateMessages(m);
			if (messages != null) {
				for (int i = 0; i < messages.length; i++) {
				    final IMessage currentMsg = messages[i];

					if (currentMsg.isType(TmServiceMessageType.TelemetryPacketSummary)) {
						final IPacketSummaryMessage pesMsg = (IPacketSummaryMessage) messages[i];
						pesFilter.setApids(apids);
						pesFilter.setApidNames(apidNames);
						if (vcid != null) {
						    pesFilter.setVcid(vcid.toString());
						}
						
						final Map<String, PacketSummaryRecord> newRecords = runFilter(pesMsg.getPacketSummaryMap(), prevSumMap);
						
						if(newRecords == null) {
                            continue;
                        }
						
						if (packeExtractSumMessageCount == 0 && !(minify && headerShown)) {
							if (outputTemplate != null) {
								final HashMap<String, Object> map = new HashMap<>();
								map.put("header", true);
								map.put("csvHeader", csvHeader);
								if(csvHeader) {
                                    csvHeader = false;
                                }
								map.put("formatter", format);
								final String text = TemplateManager.createText(
										outputTemplate, map);
								System.out.print(text);
								headerShown = true;
							}
						}
						handlePacketMessage(pesMsg, newRecords);

                    } else if (currentMsg instanceof IStartOfContextMessage) {
						startContext((IStartOfContextMessage) messages[i]);

                    } else if (currentMsg instanceof IContextHeartbeatMessage) {
						startContext((IContextHeartbeatMessage) messages[i]);

                    } else if (currentMsg instanceof IEndOfContextMessage) {
						handleEndOfContext((IEndOfContextMessage) messages[i]);

					} else {
						writeError("PacketExtractSumMessageHandler got an unrecognized message type: "
								+ currentMsg.getType());
					}
				}
			}
		} catch (final Exception e) {
			writeError("PacketExtractSumMessageHandler could not process message: "
					+ e.toString(), e);
		}
	}

	private synchronized void handlePacketMessage(final IPacketSummaryMessage m, final Map<String, PacketSummaryRecord> newRecords) {
		try {
			if (outputTemplate != null) {
				final Map<String, Object> map = new HashMap<>();
				map.put("body", true);
				map.put("formatter", format);
				m.setTemplateContext(map);
				// TODO _ this overwrites the summary records in the
				// map with only those that have changed, but requires that this
				// module know the velocity tag for the map, which is bad.
				// Previous implementation, however, was modifying the
				// actual source message. Clients should just not do that.
				map.put("summaryList", newRecords);
				final String text = TemplateManager.createText(outputTemplate,
						map);

				System.out.println(text);
			}
			
			prevSumMap = newRecords;
		} catch (final Exception e) {
			writeError("Unable to format PacketExtractSummary message for output", e);
		}
	}

	private void startContext(final IContextMessage message) {
		final ISimpleContextConfiguration newConfig = message.getContextConfiguration();
		writeLog("PacketExtractSumMessageHandler got Start of Context or first Heartbeat message for context "
				+ newConfig.getContextId().getNumber());

		// NPE after downlink completes
		if(currentContextId != null) {
			currentContextId.copyValuesFrom(newConfig.getContextId());
		}
		if (awaitingFirstContext) {
			currentScFilterInfo.copyValuesFrom(newConfig.getFilterInformation());
			format = new SprintfFormat(currentContextId.getSpacecraftId());
			awaitingFirstContext = false;
		}

	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.watcher.IMessageHandler#setAppHelper(jpl.gds.watcher.IResponderAppHelper)
	 */
	@Override
	public void setAppHelper(final IResponderAppHelper app) {
		appHelper = (PacketWatcherApp) app;
		apids = appHelper.getApids();
		apidNames = appHelper.getApidNames();
		vcid = appHelper.getVcid();
		minify = appHelper.isMinified();
		csvHeader = appHelper.showCSVHeaders();

		final String templateName = appHelper.getTemplateName();
		try {
			outputTemplate = templateManager.getTemplateForStyle(
			        MessageRegistry.getMessageConfig(TmServiceMessageType.TelemetryPacketSummary), templateName);
			if (outputTemplate == null) {
				writeError("Unable to locate " + templateName
						+ " template for PacketExtractSummary messages");
			}
		} catch (final TemplateException e) {
			writeError("Unable to load " + templateName
					+ " template for PacketExtractSummary messages", e);
			outputTemplate = null;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.watcher.IMessageHandler#shutdown()
	 */
    @Override
	public void shutdown() {
		if (!awaitingFirstContext && packeExtractSumMessageCount > 0) {
			if (outputTemplate != null) {
				final HashMap<String, Object> map = new HashMap<>();
				map.put("trailer", true);
				map.put("formatter", format);
				final String text = TemplateManager.createText(outputTemplate,
						map);
				System.out.print(text);
			}
		}
		writeLog("PacketExtractSumMessageHandler is shutting down");

	}
    
    /**
     * Performs filtering on PacketSummaryObjects based on specified filters.
     *
     * @param prevMap map of previous PacketSummaryObjects
     * 
     * @return true if new data is available
     */
    private Map<String, PacketSummaryRecord> runFilter(final Map<String, PacketSummaryRecord> newMap,
            final Map<String, PacketSummaryRecord> prevMap)
    {
        if (newMap == null)
        {
            return null;
        }
    
        final Map<String, PacketSummaryRecord> result = new HashMap<>(newMap);
    
        for (final Iterator<Map.Entry<String, PacketSummaryRecord>> it =
                result.entrySet().iterator();
                it.hasNext();)
        {
            final Map.Entry<String, PacketSummaryRecord> entry = it.next();
            final String                                 k     = entry.getKey();
            final PacketSummaryRecord                    psr   = entry.getValue();
    
            boolean newData = false;
    
            if (pesFilter.matches(psr))
            {
                final PacketSummaryRecord oldPsr =
                        (prevMap != null) ? prevMap.get(k) : null;
    
                        if ((oldPsr == null) || ! psr.equals(oldPsr))
                        {
                            newData = true;
                        }
            }
    
            if (!newData)
            {
                // Get rid of because it does not match filter or is old
                it.remove();
            }
        }
    
        return result.isEmpty() ? null : result;
    }

    /**
     * Private class used for filtering between one packet extract 
     * summary message and the next.
     */
    public static class PesFilter {
        private String[] apids;
        private String[] apidNames;
        private String vcid;
    
        /**
         * Gets the list of filter APIDs.
         * 
         * @return array of APID numbers as strings
         */
        @SuppressWarnings("EI_EXPOSE_REP")
        public String[] getApids() {
            return apids;
        }
    
        /**
         * Gets the list of filter APID names.
         * 
         * @return array of APID names
         */
        @SuppressWarnings("EI_EXPOSE_REP")
        public String[] getApidNames() {
            return apidNames;
        }
    
        /**
         * Gets the filter virtual channel ID.
         *  
         * @return filter VCID
         */
        public String getVcid() {
            return vcid;
        }
    
    
        /**
         * Sets the list of filter APIDs.
         * 
         * @param apids array of APID number strings to set
         */
        @SuppressWarnings("EI_EXPOSE_REP2")
        public void setApids(final String[] apids) {
    
            this.apids = apids;
        }
    
    
        /**
         * Sets the list of filter APID names.
         * 
         * @param apidNames array of APID names to set
         */
        @SuppressWarnings("EI_EXPOSE_REP2")
        public void setApidNames(final String[] apidNames) {
    
            this.apidNames = apidNames;
        }
    
    
        /**
         * Sets the filter virtual channel ID.
         * 
         * @param vcid filter VCID to set
         */
        public void setVcid(final String vcid) {
    
            this.vcid = vcid;
        }
    
    
        /**
         * Determines is a new PacketSummaryObject matches the filter.
         * 
         * @param pso new PacketSummaryOBject to check
         * @return true if something in the summary object has changed, false if it has not
         */
        public boolean matches(final PacketSummaryRecord pso) {
    
            boolean matchVcid = false;
    
            if (vcid != null) {
                if (pso.getVcid() == Long.parseLong(vcid)) {
                    matchVcid = true;
                }
            } else {
                matchVcid = true;
            }
    
            if (!matchVcid) {
                return false;
            }
    
            boolean matchApid = false;
    
            /* Check for empty apid array */
            if (apids != null && apids.length > 0) {
                for (final String a : apids) {
                    if (pso.getApid() == Long.parseLong(a)) {
                        matchApid = true;
                        break;
                    }
                }
            } else {
                matchApid = true;
            }
    
            if (!matchApid) {
                return false;
            }
    
            boolean matchApidName = false;
    
            /* Check for empty apid names array */
            if (apidNames != null && apidNames.length > 0) {
                for (final String a : apidNames) {
                    if (pso.getApidName().equalsIgnoreCase(a)) {
                        matchApidName = true;
                        break;
                    }
                }
            } else {
                matchApidName = true;
            }
    
            return matchApidName;
        }
    }

}
