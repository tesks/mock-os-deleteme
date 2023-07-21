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
import jpl.gds.context.api.message.IContextHeartbeatMessage;
import jpl.gds.context.api.message.IEndOfContextMessage;
import jpl.gds.context.api.message.IStartOfContextMessage;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.product.api.message.IPartialProductMessage;
import jpl.gds.product.api.message.IProductAssembledMessage;
import jpl.gds.product.api.message.ProductMessageType;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.template.MessageTemplateManager;
import jpl.gds.shared.template.MissionConfiguredTemplateManagerFactory;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.watcher.IResponderAppHelper;
import jpl.gds.watcher.responder.app.MessageResponderApp;
import jpl.gds.watcher.responder.app.ProductWatcherApp;

/**
 * ProductWatcherMessageHandler is the MessageHandler implementation used by the 
 * message responder application to listen for Product messages and put them to 
 * the console
 */
public class ProductWatcherMessageHandler extends AbstractMessageHandler {
    private ProductWatcherApp appHelper;
    
	/** Current context configuration*/
	private ISimpleContextConfiguration currentContext;

	/** Default velocity template name for output from this handler */
    public static final String DEFAULT_TEMPLATE = "onelinesummary";
    /** Template Manager */
    private MessageTemplateManager templateManager;
    /** Template Formatter*/
    private final SprintfFormat    format;
    /** Product templates */
    private Template assembledTemplate;
	private Template partialTemplate;
    
    /** Product Message counters */
    private long receivedProductAssembledMessages = 0L;
    private long receivedPartialProductMessages = 0L;
    
	/**
	 * Creates an instance of ProductWatcherMessageHandler
	 * @param appContext the current application context
	 */
    public ProductWatcherMessageHandler(final ApplicationContext appContext) { 
    	super(appContext);
        format = new SprintfFormat();
    	try { 
            this.templateManager = MissionConfiguredTemplateManagerFactory.getNewMessageTemplateManager(sseFlag);
    	} catch(final TemplateException e) { 
    		writeError("Unable to create template manager", e);
    	}
    }
	
	/**
	 * Handles the PartialProduct message
	 * 
	 * @param message IPartialProductMessageProvider
	 */
	private void handlePartialMessage(final IPartialProductMessage message) {
    	this.receivedPartialProductMessages++;
    	writeLog("ProductWatcherMessageHandler got PartialProduct message: " + message.getOneLineSummary());
    	
    	if (partialTemplate != null) { 
    		printProduct(partialTemplate, message);
    	}
		
	}

	/**
	 * Handles the IProductAssembledMessageProvider 
	 * 
	 * @param message IProductAssembledMessageProvider
	 */
    private void handleProductMessage(final IProductAssembledMessage message) {
    	this.receivedProductAssembledMessages++;
        writeLog("ProductWatcherMessageHandler got ProductAssembled message: " + message.getOneLineSummary());
        
		if (assembledTemplate != null) { 
    		printProduct(assembledTemplate, message);
        }
    }
    
    /**
     * Prints the product message using its defined template 
     * 
     * @param template Output template
     * @param message Product message
     */
    private void printProduct(final Template template, final IMessage message) { 
    	final HashMap<String, Object> map = new HashMap<>();
		message.setTemplateContext(map);
		map.put("body", "true");
		map.put("formatter", this.format);
		final String text = MessageTemplateManager.createText(template, map);
		System.out.println(text);
    }
    

    /**
     * @{inheritDoc}
     * @see jpl.gds.watcher.IMessageHandler#setAppHelper(jpl.gds.watcher.IResponderAppHelper)
     */
    @Override
    public synchronized void setAppHelper(final IResponderAppHelper app)
    {
    	boolean error = false;
    	
        this.appHelper = (ProductWatcherApp)app;
    	final String templateName = this.appHelper.getTemplateName();

    	if (templateName != null) {
    		try {
    			this.assembledTemplate = templateManager.getTemplateForStyle(MessageRegistry.getMessageConfig(ProductMessageType.ProductAssembled), templateName);
    				
    			if (appHelper.isWatchingPartials()) { 
    				this.partialTemplate = templateManager.getTemplateForStyle(MessageRegistry.getMessageConfig(ProductMessageType.PartialProduct), templateName);
    			}
    		        
    		} catch (final TemplateException e) {
    			writeError("Unable to locate " + templateName  + " template for product messages, " + e.toString());
    			error = true;
    		}
    	}
    	
    	if (templateName == null || error) { 
    		try { 
				this.assembledTemplate = templateManager.getTemplateForStyle(MessageRegistry.getMessageConfig(ProductMessageType.ProductAssembled), DEFAULT_TEMPLATE);
				
				if (appHelper.isWatchingPartials()) { 
    				this.partialTemplate = templateManager.getTemplateForStyle(MessageRegistry.getMessageConfig(ProductMessageType.PartialProduct), DEFAULT_TEMPLATE);
    			}
				
				if (error) { 
					writeInfo("Unable to locate the specified template " + templateName + ". Using default template=" + DEFAULT_TEMPLATE);
					appHelper.setTemplateName(DEFAULT_TEMPLATE);
				}
				
			} catch(final TemplateException e2) { 
				writeError("Unable to locate " + DEFAULT_TEMPLATE + " template for product messages, " + e2.toString());
			}
    	}
    }
    
	/**
	 * @{inheritDoc}
	 */
	@Override
	public void handleMessage(final IExternalMessage m) {
		try {
            final IMessage[] messages = externalMessageUtil.instantiateMessages(m);
            if (messages != null) {
            	for(final IMessage msg: messages) { 
            		
                    if (msg.isType(ProductMessageType.ProductAssembled)) {
                    	final IProductAssembledMessage pm = (IProductAssembledMessage) msg;
                    	
                    	if ( appHelper.getApids().isEmpty()
                    			|| appHelper.getApids().contains(String.valueOf(pm.getMetadata().getApid())) ) { 
                    		handleProductMessage(pm);
                    	}
                        // Not Watching ALL products OR
                    	// Not watching this product APID
                    	// So do nothing

                    } else if (msg.isType(ProductMessageType.PartialProduct) && appHelper.isWatchingPartials()) { 
                    	final IPartialProductMessage pm = (IPartialProductMessage) msg;
                    	
                    	if ( appHelper.getApids().isEmpty() 
                    			|| appHelper.getApids().contains(String.valueOf(pm.getMetadata().getApid())) ) { 
                    		handlePartialMessage(pm);
                    	}
                    	// Not Watching ALL products OR
                    	// Not watching this product APID
                    	// So do nothing
                    	
                    } else if (msg instanceof IStartOfContextMessage) {
                        startContext((IStartOfContextMessage) msg);

                    } else if (msg instanceof IContextHeartbeatMessage) {
                        startContext((IContextHeartbeatMessage)msg);

                    } else if (msg instanceof IEndOfContextMessage) {
                        handleEndOfContext((IEndOfContextMessage) msg);
                    } else {
                        writeError("ProductWatcherMessageHandler got an unrecognized message type: " + msg.getType());
                        continue;
                    }
            	}
            }
        } catch (final Exception e) {
            writeError("ProductWatcherMessageHandler could not process message: " + e.toString(), e);
        }
		
	}
	
    /**
     * Handles StartOfContextMessage.
     * 
     * @param message StartOfContextMessage
     */
	private void startContext(final IStartOfContextMessage message) {
        final ISimpleContextConfiguration newConfig = message.getContextConfiguration();
        writeLog("ProductWatcherMessageHandler got Start of Context message for context "
                + newConfig.getContextId().getNumber());

        this.currentContext = newConfig;
    }

	/**
	 * Handles the HeartbeatMessages
	 * 
	 * @param message HeartbeatMessage
	 */
    private void startContext(final IContextHeartbeatMessage message) {
        final ISimpleContextConfiguration newConfig = message.getContextConfiguration();
        if (this.currentContext == null
                || !this.currentContext.getContextId().getNumber().equals(
                        newConfig.getContextId().getNumber())) {
            writeLog("ProductWatcherMessageHandler got first Heartbeat message for context "
                    + newConfig.getContextId().getNumber());
            this.currentContext = newConfig;
        }
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.watcher.IMessageHandler#handleEndOfContext(IEndOfContextMessage)
     */
    @Override
    public synchronized void handleEndOfContext(final IEndOfContextMessage m) {
        final IContextKey tc = m.getContextKey();
        if (tc == null || tc.getNumber() == null) {
            writeLog("ProductWatcherMessageHandler received End of Context message for an unknown context; skipping");
            return;
        }
        writeLog("ProductWatcherMessageHandler received End of Context message for context "
                + tc.getNumber());
        this.currentContext = null;
        if (this.appHelper != null && this.appHelper.isExitContext()) {
            shutdown();
            MessageResponderApp.getInstance().markDone();
        }
    }

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.watcher.IMessageHandler#shutdown()
	 */
	@Override
	public void shutdown() {
        writeLog("ProductWatcherMessageHandler is shutting down\n"
        		+ "ProductAssembled messages received: " + this.receivedProductAssembledMessages + "\n"
        		+ "PartialProduct messages received: " + this.receivedPartialProductMessages);
	}
	
	/**
     * Gets the current context configuration known to this handler.
     * 
     * @return context configuration object
     */
    public ISimpleContextConfiguration getCurrentContextConfiguration() {
        return this.currentContext;
    }

}
