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

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;

import org.apache.velocity.Template;
import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.message.IEndOfContextMessage;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.process.DoNothingLineHandler;
import jpl.gds.shared.process.LineHandler;
import jpl.gds.shared.process.ProcessLauncher;
import jpl.gds.shared.process.StderrLineHandler;
import jpl.gds.shared.process.StdoutLineHandler;
import jpl.gds.shared.template.MessageTemplateManager;
import jpl.gds.shared.template.MissionConfiguredTemplateManagerFactory;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.template.TemplateManager;
import jpl.gds.watcher.IResponderAppHelper;
import jpl.gds.watcher.WatcherProperties;
import jpl.gds.watcher.responder.app.ResponderAppName;
import jpl.gds.watcher.responder.app.TriggerScriptApp;

/**
 * ExecuteScriptMessageHandler is the MessageHandler implementation used by
 * the message responder application to listen for requested messages and 
 * execute a script.
 */
public class ExecuteScriptMessageHandler extends AbstractMessageHandler {
    /**
     * Default output template for messages.
     */
    public static final String DEFAULT_TEMPLATE = "Xml";
    
    private MessageTemplateManager templateManager;
    private long handledMessageCount;
    private String outputFormat;
    private TriggerScriptApp appHelper;
    private SprintfFormat format;
    private final HashMap<IMessageType, Template> templates = new HashMap<>();
    private String script;
    private ProcessLauncher processLauncher;
    private boolean waitForProcess = true;
    private boolean useFileExchange = true;
    
    /**
     * Creates an instance of ExecuteScriptMessageHandler.
     * @param appContext the current application context
     */
    public ExecuteScriptMessageHandler(final ApplicationContext appContext) {
    	super(appContext);
    	try {
            final WatcherProperties responderProps = new WatcherProperties(ResponderAppName.TRIGGER_SCRIPT_APP_NAME.getAppName(),
                                                                           sseFlag);
            this.templateManager = MissionConfiguredTemplateManagerFactory.getNewMessageTemplateManager(sseFlag);
            this.waitForProcess = responderProps.waitForProcess();
            this.useFileExchange = responderProps.useFileExchange();
            this.processLauncher = new ProcessLauncher();
            this.format = new SprintfFormat(0);
    	} catch (final TemplateException e) {
    		writeError("Problem creating message template manager: " + e.toString(), e);
    	}
    }

    /**
     * Gets the number of messages received by this handler.
     * @return the message count
     */
    public long getHandledMessageCount() {
    	return this.handledMessageCount;
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
                    final IMessageType type = messages[i].getType();
                    Template t = this.templates.get(type);
                    if (t == null) {
                        t = this.templateManager.getTemplateForStyle(MessageRegistry.getMessageConfig(type), this.outputFormat);  
                        if (t == null) {
                            writeError("Cannot find template " + this.outputFormat + " for message type " + type);
                            t = this.templateManager.getTemplateForStyle(MessageRegistry.getMessageConfig(type), DEFAULT_TEMPLATE);  
                        }
                        this.templates.put(type, t);
                    }
                    final HashMap<String, Object> map = new HashMap<>();
                    messages[i].setTemplateContext(map);
                    map.put("body", true);
                    map.put("formatter", this.format);
                    final String text = TemplateManager.createText(t, map);
                    final boolean ok = fireScript(text);
                    if (ok) {
                        this.handledMessageCount++;
                    }
                } 
            }
        } catch (final Exception e) {
            writeError("ExecuteScriptMessageHandler could not process message: " + e.toString(), e);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IMessageHandler#shutdown()
     */
    @Override
    public synchronized void shutdown() {
        writeLog("ExecuteScriptMessageHandler is shutting down");
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IMessageHandler#handleEndOfContext(IEndOfContextMessage)
     */
    @Override
    public synchronized void handleEndOfContext(final IEndOfContextMessage m) {
       // do nothing
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IMessageHandler#setAppHelper(jpl.gds.watcher.IResponderAppHelper)
     */
    @Override
    public void setAppHelper(final IResponderAppHelper app) {
       this.appHelper = (TriggerScriptApp)app;
 
       this.outputFormat = this.appHelper.getTemplateName();
       this.script = this.appHelper.getScriptName();
       
    }
    
    private boolean fireScript(final String text) {
        if (this.appHelper.isConsoleOutput()) {
            final StdoutLineHandler out = new StdoutLineHandler();
            final StderrLineHandler err = new StderrLineHandler();
            this.processLauncher.setOutputHandler(out);
            this.processLauncher.setErrorHandler(err);
        } else {
            // We want to do nothing with console output. But we cannot really do
            // nothing, because if the process has a lot of console output, Java will
            // eventually hang if nothing is reading from the streams
            final LineHandler out = new DoNothingLineHandler();
            final LineHandler err = new DoNothingLineHandler();
            this.processLauncher.setOutputHandler(out);
            this.processLauncher.setErrorHandler(err);
        }
        try {
            File tempFile = null;

            if (this.useFileExchange) {
                tempFile = File.createTempFile("mpcs_message", ".txt", new File("/tmp"));
                final FileWriter fw = new FileWriter(tempFile);
                fw.write(text);
                fw.close();
                this.processLauncher.launch(new String[] { this.script, tempFile.getAbsolutePath() });
            } else {
                this.processLauncher.launch(new String[] { this.script, text });
            }
            if (this.waitForProcess) {
                if (tempFile != null) {
                    tempFile.deleteOnExit();
                }
                final int status = this.processLauncher.waitForExit();
                if (status != 0) {
                    writeLog("Process " + this.script + " exited with non-zero status " + status);
                }
            }
            if (this.waitForProcess && this.useFileExchange) {
                if (tempFile != null) {
                    final boolean ok = tempFile.delete();
                    if (!ok) {
                        writeLog("Process " + this.script + " failed to delete file " + tempFile.getAbsolutePath());
                    }
                }
            }
            return true;
        } catch (final Exception e) {
            writeError("Error executing script: " + e.toString());
            return false;
        }
    }
}
