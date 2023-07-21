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
package jpl.gds.perspective.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.ContextTopicNameFactory;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.message.api.BaseMessageHeader;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.external.CheckMessageService;
import jpl.gds.message.api.external.ExternalDeliveryMode;
import jpl.gds.message.api.external.IClientHeartbeatListener;
import jpl.gds.message.api.external.IClientHeartbeatPublisher;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.message.api.external.IMessageClientFactory;
import jpl.gds.message.api.external.IMessageServiceListener;
import jpl.gds.message.api.external.ITopicPublisher;
import jpl.gds.message.api.external.ITopicSubscriber;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.message.api.util.MessageFilterMaker;
import jpl.gds.perspective.ApplicationConfiguration;
import jpl.gds.perspective.PerspectiveConfiguration;
import jpl.gds.perspective.PromptSettings;
import jpl.gds.perspective.message.ChangePerspectiveMessage;
import jpl.gds.perspective.message.ExitPerspectiveMessage;
import jpl.gds.perspective.message.PerspectiveMessageType;
import jpl.gds.perspective.message.SavePerspectiveMessage;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.metadata.MetadataKey;
import jpl.gds.shared.swt.ConfirmationShell;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.time.AccurateDateTime;

/**
 * PerspectiveActor is an implementation of PerspectiveListener that performs
 * the actions necessary to save or update the user's perspective. It sends and
 * receives the perspective-related messages and invokes the proper methods
 * on the PerspectiveShell.
 * Implements IClientHeartbeatListener interface so this class can detect when JMS is down.
 * 
 *
 */
public class PerspectiveActor implements PerspectiveListener, IMessageServiceListener, IClientHeartbeatListener {
	
	// 5/23/13 - Adding constant to fix PMD violation
	private static final String SAVE_ERROR =   "Error saving perspective";
	
    protected static Tracer tracer;
    private static Tracer jmsTracer;

    private ITopicPublisher publisher;
    private ITopicSubscriber subscriber;
    protected final ApplicationConfiguration appConfig;
    protected final PerspectiveConfiguration persConfig;
    protected PerspectiveShell perspectiveShell;
    private final PromptSettings settings = new PromptSettings();
    private final List<String> loadedAppUids = new ArrayList<>();
    protected final SWTUtilities swt = new SWTUtilities();
    private boolean jmsActive = false;

    private final AtomicBoolean perspectiveExit = new AtomicBoolean(false);

    protected final ApplicationContext appContext;

    private final IExternalMessageUtility externalMessageUtil;

    /**
     * Creates an instance of PerspectiveActor.
     * 
     * @param appContext
     *            The current application context
     * @param appConfig
     *            the ApplicationConfiguration object for the current
     *            application
     * @param shell
     *            the perspective shell for the current object
     * @param useOwnHeartbeat
     *            true if this PerspectiveActor should start its own client
     *            heartbeat in order to detect JMS outages; false if not
     * 
     */
    public PerspectiveActor(final ApplicationContext appContext, 
    		final ApplicationConfiguration appConfig,
            final PerspectiveShell shell, final boolean useOwnHeartbeat) {
        if (appConfig == null) {
            throw new IllegalArgumentException("appConfig cannot be null");
        }
        if (shell == null) {
            throw new IllegalArgumentException("shell cannot be null");
        }
        this.appContext = appContext;
        this.appConfig = appConfig;
        this.loadedAppUids.add(this.appConfig.getUid());

        this.perspectiveShell = shell;
        this.persConfig = appContext.getBean(PerspectiveConfiguration.class);
        this.externalMessageUtil = appContext.getBean(IExternalMessageUtility.class);

        jmsTracer = TraceManager.getTracer(appContext, Loggers.JMS);
        tracer = TraceManager.getDefaultTracer(appContext);
        
        if (useOwnHeartbeat) {
            IClientHeartbeatPublisher hbPublisher = appContext.getBean(IClientHeartbeatPublisher.class);
            hbPublisher = appContext.getBean(IClientHeartbeatPublisher.class);
            hbPublisher.addListener(this);
            if (!hbPublisher.startPublishing()) {
                jmsTracer.error("Unable to start client heartbeat");
            } else {
                jmsTracer.info("Client heartbeat started for " + hbPublisher.getClientId());
            }
        }
        
        this.connectToMessageService();
    }

    private synchronized boolean connectToMessageService() {

        final String perspectiveTopic = ContextTopicNameFactory
                .getPerspectiveTopic(appContext);

        try {


            final String filter = MessageFilterMaker.createFilter(MetadataKey.PERSPECTIVE_ID, this.appConfig.getApplicationId());
            final IMessageClientFactory clientFactory = appContext.getBean(IMessageClientFactory.class);
            jmsTracer.info("Subscribing to topic " + perspectiveTopic + " with filter " + filter);
            jmsTracer.info("Publishing to topic " + perspectiveTopic);
            this.publisher = clientFactory.getTopicPublisher(perspectiveTopic, false);
            this.subscriber = clientFactory.getTopicSubscriber(perspectiveTopic, filter, true);

            this.subscriber.setMessageListener(this);
            this.subscriber.start();
            jmsActive = true;
            return true;
        } catch (final MessageServiceException e) {
            tracer.error("Perspective Actor cannot connect to the message service", e);
        } 

        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.perspective.gui.PerspectiveListener#setPerspectiveShell(jpl.gds.perspective.gui.PerspectiveShell)
     */
    @Override
    public void setPerspectiveShell(final PerspectiveShell pShell) {
        this.perspectiveShell = pShell;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.perspective.gui.PerspectiveListener#getPerspectiveShell()
     */
    @Override
    public PerspectiveShell getPerspectiveShell() {
        return this.perspectiveShell;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.perspective.gui.PerspectiveListener#saveCalled()
     */
    @Override
    public void saveCalled() {
        final SavePerspectiveMessage message = new SavePerspectiveMessage();
        message.setLocation(this.appConfig.getConfigPath());
        message.setApplicationId(this.appConfig.getApplicationId());
        message.setLocked(this.persConfig.isLocked());
        try {
            this.persConfig.backupPerspective();
        } catch (final IOException e) {
            SWTUtilities
                    .showErrorDialog(this.perspectiveShell.getShell(),
                            "Error backup up perspective",
                            "The user interface configuration could not be properly saved.");
            return;
        }
        try {
            publishMessage(message);
        } catch (final MessageServiceException e) {
            SWTUtilities
                    .showErrorDialog(
                            this.perspectiveShell.getShell(),
                           SAVE_ERROR,
                            "The user interface configuration could not be properly saved due to a message service problem.");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.perspective.gui.PerspectiveListener#saveAsCalled(org.eclipse
     *      .swt.widgets.Shell)
     */
    @Override
    public void saveAsCalled(final Shell parent) {
        // Popup save dialog with root at ~user/CHILL
        String dir = this.appConfig.getConfigPath();
        if (parent != null) {
            dir = this.swt.displayStickyDirSaver(parent, "PerspectiveActor",
                    GdsSystemProperties.getUserConfigDir());
        }
        // Create a new save manager
        if (dir != null) {
            SavePerspectiveMessage message = null;
            try {

                message = new SavePerspectiveMessage();
                message.setLocation(dir);
                message.setApplicationId(this.appConfig.getApplicationId());
                message.setLocked(false);
                this.persConfig.setConfigPath(dir);
                setPerspectiveLock(false);
                try {
                    this.persConfig.backupPerspective();
                } catch (final IOException e) {
                    SWTUtilities
                            .showErrorDialog(this.perspectiveShell.getShell(),
                                    "Error backup up perspective",
                                    "The user interface configuration could not be properly saved.");
                    return;
                }
                // By saving the entire perspective all necessary files are
                // created
                this.persConfig.save();
            } catch (final IOException j) {
                SWTUtilities
                        .showErrorDialog(
                                parent,
                               SAVE_ERROR,
                                "The user interface configuration could not be properly saved due to a file I/O problem.");
            }
            try {
                publishMessage(message);
            } catch (final MessageServiceException e) {
                SWTUtilities
                        .showErrorDialog(
                                parent,
                               SAVE_ERROR,
                                "The user interface configuration could not be properly saved due to a message service problem.");
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.perspective.gui.PerspectiveListener#exitCalled()
     */
    @Override
    public void exitCalled() {
        if (this.settings.showPerspectiveExitConfirmation()) {
            final ConfirmationShell cs = new ConfirmationShell(
                    this.perspectiveShell.getShell().getDisplay(),
                    "This will exit all the applications in your perspective. Are you sure?",
                                                               false);
            cs.open();
            while (!cs.getShell().isDisposed()) {
                if (!cs.getShell().getDisplay().readAndDispatch()) {
                    cs.getShell().getDisplay().sleep();
                }
            }
            final boolean exit = !cs.wasCanceled();
            if (!exit) {
                return;
            }
            final boolean promptAgain = cs.getPromptAgain();
            if (!promptAgain) {
                this.settings.setPerspectiveExitConfirmation(false);
                this.settings.save();
            }
        }

        final ExitPerspectiveMessage m = new ExitPerspectiveMessage();
        m.setApplicationId(this.appConfig.getApplicationId());
        try {
            publishMessage(m);
        } catch (final MessageServiceException e) {
            SWTUtilities
                    .showErrorDialog(this.perspectiveShell.getShell(),
                            "Error exiting perspective",
                            "The user interface could not be properly exited due to a JMS problem.");
        }
    }

    // R8 Refactor - Commenting out everything related to session restart
//    /**
//     * {@inheritDoc}
//     * 
//     * @see jpl.gds.perspective.gui.PerspectiveListener#newSessionStarted()
//     */
//    @Override
//    public void newSessionStarted() {
//
//        final ChangeSessionMessage m = new ChangeSessionMessage();
//        m.setApplicationId(this.appConfig.getApplicationId());
//        m.setSessionConfiguration(SessionConfiguration.getGlobalInstance());
//        try {
//        	publishToJms(m);
//        } catch (final JMSException e) {
//            SWTUtilities
//                    .showErrorDialog(this.perspectiveShell.getShell(),
//                            "Error exiting perspective",
//                            "The new session notification could not be sent due to a JMS problem.");
//        }
//    }

    protected synchronized void publishMessage(final IMessage m) throws MessageServiceException {
    	
    	boolean done = false;
    	
    	// The loop is needed to handle message service disconnects
    	while (!done) {
    		
    		// If the service is known to be dead, attempt to reconnect.
	    	if (!jmsActive) {
	    		final boolean ok = connectToMessageService();
	    		if (ok) {
	    			tracer.info("Message service publisher and subscriber connections on the perspective topic were recreated");
	    			jmsActive = true;
	    		} else {
	    			throw new MessageServiceException("No message service connection");
	    		}
	    	}
	    	
	    	// Now try to publish
	        final BaseMessageHeader header = new BaseMessageHeader(appContext.getBean(MissionProperties.class), m.getType(),
	                appContext.getBean(IContextConfiguration.class).getMetadataHeader(), new AccurateDateTime());
	        header.setHeaderProperty(MetadataKey.PERSPECTIVE_ID,
	                appConfig.getApplicationId());
	        final Map<String, Object> headerProps = header.getPropertiesWithStringKeys();
	        final String body = header.wrapContent(m.getType(), m.toXml());
	        
	        try {
	           publisher.publishTextMessage(body, headerProps, 0,
	                ExternalDeliveryMode.NON_PERSISTENT); 
	           done = true;
	           
	        } catch (final MessageServiceException e) {

	        	// This may occur because the message service is currently down, or because it was
	        	// down and came back up.  Close the message service objects. Check if the service is
	        	// running. If not, exit the loop. Otherwise, loop and retry the connection.
	        	tracer.error("Message service publisher and subscriber connections on the perspective topic were lost");
	        	jmsActive = false;
	        	publisher.close();
	        	subscriber.closeNoDisconnect();
	        	if (!CheckMessageService.checkMessageServiceRunning(appContext.getBean(MessageServiceConfiguration.class), null, 0, tracer, false)) {
	        		done = true;
	        	}
	        }
    	}
    }

    /**
     * {@inheritDoc}
     * 
     */
    @Override
    public void onMessage(final IExternalMessage theMessage) {
        try {
            final IMessage[] messages = externalMessageUtil.instantiateMessages(theMessage);
            if (messages.length == 0) {
                if (perspectiveShell != null) {
                    SWTUtilities
                            .showErrorDialog(perspectiveShell.getShell(),
                                   SAVE_ERROR,
                                    "Unable to save user perspective: message was not valid.");
                } else {
                    tracer.error("Perspective message was not valid");
                }
            }
            final IMessage m = messages[0];
            if (m.isType(PerspectiveMessageType.SavePerspective)) {
                tracer.debug(perspectiveShell.getTitle()
                        + " received save perspective message");
                if (perspectiveShell.getShell().isDisposed()) {
                    tracer.debug("Shell has already been disposed in PerspectiveActor");
                    return;
                }
                final String dir = ((SavePerspectiveMessage) m).getLocation();

                SWTUtilities.safeAsyncExec(perspectiveShell.getShell()
                        .getDisplay(), tracer, "Saving perspective",
                        new Runnable() {
                            @Override
                            public String toString() {
                                return "perspectiveActor.onMessage.Runnable";
                            }

                            @Override
                            public void run() {
                                try {
                                    perspectiveShell.updateConfiguration();
                                    appConfig.setConfigPath(dir);
                                    persConfig
                                            .setLocked(((SavePerspectiveMessage) m)
                                                    .isLocked());
                                    tracer.debug(perspectiveShell.getTitle()
                                            + " saving perspective");
                                    appConfig.save();
                                    perspectiveShell.perspectiveChanged();
                                } catch (final Exception e) {
                                    tracer.error(perspectiveShell.getTitle()
                                            + " error saving perspective");
                                    SWTUtilities.showErrorDialog(
                                            perspectiveShell.getShell(),
                                           SAVE_ERROR,
                                            "Unable to save user perspective: "
                                                    + e.toString());
                                }
                            }
                        });
            } else if (m.isType(PerspectiveMessageType.ChangePerspective)) {
                tracer.debug(perspectiveShell.getTitle()
                        + " received change perspective message");
                if (perspectiveShell.getShell().isDisposed()) {
                    tracer.debug("Shell has already been disposed in PerspectiveActor");
                    return;
                }
                final String dir = ((ChangePerspectiveMessage) m).getLocation();

                SWTUtilities.safeAsyncExec(perspectiveShell.getShell()
                        .getDisplay(), tracer, "Changing perspective",
                        new Runnable() {

                            @Override
                            public String toString() {
                                return "perspectiveActor.onMessage.Runnable";
                            }

                            @Override
                            public void run() {
                                perspectiveShell.updateConfiguration();
                                appConfig.setConfigPath(dir);
                                try {
                                    tracer.debug(perspectiveShell.getTitle()
                                            + " changing perspective");
                                    persConfig.setConfigPath(dir);
                                    appConfig.setConfigPath(dir);
                                    perspectiveShell.perspectiveChanged();
                                } catch (final Exception e) {
                                    tracer.error(perspectiveShell.getTitle()
                                            + " error changing perspective");
                                    SWTUtilities.showErrorDialog(
                                            perspectiveShell.getShell(),
                                            "Error changing perspective",
                                            "Unable to change user perspective: "
                                                    + e.toString());
                                }
                            }
                        });
            } else if (m.isType(PerspectiveMessageType.MergePerspective)) {
                tracer.debug(perspectiveShell.getTitle()
                        + " received merge perspective message");

            } else if (m.isType(PerspectiveMessageType.ExitPerspective)) {
                perspectiveExit.set(true);

                tracer.debug(perspectiveShell.getTitle()
                        + " received exit perspective message");
                if (perspectiveShell.getShell().isDisposed()) {
                    tracer.debug("Shell has already been disposed in PerspectiveActor");
                    return;
                }

                SWTUtilities.safeAsyncExec(perspectiveShell.getShell()
                        .getDisplay(), tracer, "Exiting perspective",
                        new Runnable() {

                            @Override
                            public String toString() {
                                return "perspectiveActor.onMessage.Runnable";
                            }

                            @Override
                            public void run() {
                                try {
                                    if (perspectiveShell != null
                                            && !perspectiveShell.getShell()
                                                    .isDisposed()) {
                                        tracer.info(perspectiveShell.getTitle()
                                                + " exiting perspective");
                                        perspectiveShell.exitShell();
                                    }
                                } catch (final Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
            } 
         // R8 Refactor - Commenting out everything related to session restart
//            else if (m.isType(RegisteredMessageType.ChangeSession)) {
//
//                if (perspectiveShell.getShell().isDisposed()) {
//                    tracer.debug("Shell has already been disposed in PerspectiveActor");
//                    return;
//                }
//                SWTUtilities.safeAsyncExec(perspectiveShell.getShell()
//                        .getDisplay(), tracer, "Changing persepctive session",
//                        new Runnable() {
//                            @Override
//                            public String toString() {
//								return "perspectiveActor.onMessage.Runnable";
//							}
//							
//							@Override
//							public void run()
//							{
//								try {
//									if (perspectiveShell != null && !perspectiveShell.getShell().isDisposed())
//									{
//										perspectiveShell.sessionChanged(((ChangeSessionMessage)m).getSessionConfiguration());
//									}
//								} catch (final Exception e) {
//									e.printStackTrace();
//								}
//							}
//						});
//			}
		} catch (final Exception e) {
			e.printStackTrace();
			tracer.error("Error processing perspective message: " + e.toString());
		}
	}

   

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.perspective.gui.PerspectiveListener#setPerspectiveLock(boolean)
     */
    @Override
    public void setPerspectiveLock(final boolean lock) {
        this.persConfig.setLocked(lock);
        try {
            this.persConfig.save();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
    

    @Override
    public void publicationFailed() {

    	tracer.error("Message service publisher and subscriber connections on the perspective topic were lost");
    	jmsActive = false;
    	publisher.close();
    	subscriber.closeNoDisconnect();

    }


    @Override
    public void publicationRegained() {

    	final boolean ok = connectToMessageService();
    	if (ok) {
    		tracer.info("Message service publisher and subscriber connections on the perspective topic were recreated");
    	}

    }


    /**
     * Get exit perspective status.
     *
     * @return True if perspective is exiting
     */
    @Override
    public boolean getPerspectiveExit()
    {

        return perspectiveExit.get();
    }
}
