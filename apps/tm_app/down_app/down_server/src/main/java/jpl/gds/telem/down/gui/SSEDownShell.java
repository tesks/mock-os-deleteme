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
package jpl.gds.telem.down.gui;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.perspective.DisplayConfiguration;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.telem.down.IDownlinkApp;

/**
 * This is the downlink controller window specifically for SSE telemetry processing.
 *
 */
public class SSEDownShell extends AbstractDownShell {
    /**
     * Creates an instance of SEEDownShell with the given parent Display,
     * associated application class, and test configuration.
     * @param context the current application context
     * @param display the parent Display
     * @param app the associated instance of IDownlinkApp
     * @param tc the IContextConfiguration object
     * @param config the DisplayConfiguration object for the window
     */
    public SSEDownShell(final ApplicationContext context, final Display display, final IDownlinkApp app, final IContextConfiguration tc, 
            final DisplayConfiguration config) {
        super(context, display, app, tc, config);
    }
    
    /**
     * Creates an instance of SSEDownShell with the given parent Shell,
     * associated application class, and test configuration.
     * @param context the current application context
     * @param parent the parent Shell
     * @param app the associated instance of AbstractDownlinkApp
     * @param config the IContextConfiguration object
     */
    public SSEDownShell(final ApplicationContext context, final Shell parent, final IDownlinkApp app, final IContextConfiguration config) {
        super(context, parent, app, config);
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#getTitle()
     */
    @Override
	public String getTitle() {
        return ReleaseProperties.getProductLine() + " SSE " + TITLE;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.telem.down.gui.AbstractDownShell#getBrandingImage()
     */
    @Override
    protected Image getBrandingImage() {
        return SWTUtilities.createImage(this.mainDisplay, "jpl/gds/down/gui/SSELogo.gif");
    }  
	
    // R8 Refactor - Commenting out everything related to session restart
//	/**
//	 * {@inheritDoc}
//	 * @see jpl.gds.telem.down.gui.AbstractDownShell#canRestartSession()
//	 */
//	@Override
//	public boolean canRestartSession() {
//		return !GdsSystemProperties.isIntegratedGui();
//	}
//	
//	/**
//	 * {@inheritDoc}
//	 * @see jpl.gds.telem.down.gui.AbstractDownShell#restartWithNewConfig(jpl.gds.config.SessionConfiguration)
//	 * 
//	 */
//	@Override
//	protected void restartWithNewConfig(final SessionConfiguration config) {
//		SessionConfiguration.setGlobalInstance(config);
//		SWTUtilities.safeAsyncExec(
//				getShell().getDisplay(),
//				Log4jTracer.getDefaultTracer(),

//				"Session Restart",
//				new Runnable ()
//				{
//					@Override
//					public void run()
//					{
//
//						try {
//							final IPublishableLogMessage lm = StatusMessageFactory.createPublishableLogMessage(
//                                    TraceSeverity.INFO, "Automatically starting session " +  config.getContextId().getNumber() + ".");
//							bus.publish(lm);
//
//							downApp.setSessionConfiguration(config, false);
//							// Write out session configuration.
//							if (!GdsSystemProperties.isIntegratedGui()) {
//								// Add flag to call to control whether config is written
//								AbstractDownlinkApp.writeOutSessionConfig(appContext, config, true);
//							} else {
//								// Raw input type in the session configuration
//								// when running integrated comes from FSW chill_down. We need to restore
//								// the SSE input type.
//								// Restore connection type as well.
//								final TelemetryInputType inputType = testConfig.getConnectionConfiguration()
//										.getSseDownlinkConnection().getInputType();
//								testConfig.getConnectionConfiguration()
//								.getSseDownlinkConnection().setInputType(inputType);
//								
//								/* R8 Refactor TODO - Not sure what to do with this.  We must now
//								 * recreate the connection object to change the connection type, but that
//								 * will lose other information.
//								 */
////								final TelemetryConnectionType connType = testConfig.getConnectionConfiguration()
////										.getSseDownlinkConnection().getDownlinkConnectionType();
////								config.setConnectionType(connType);
//							}
//							configurationChanged(config);
//
//							if (!neverStarted) {
//								startApp();
//							}
//
//						} catch (final Exception e) {
//							e.printStackTrace();
//						}
//					}
//				});
//	}

}
