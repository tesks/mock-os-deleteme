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
package jpl.gds.tcapp.app.gui;

import java.io.PrintWriter;

import org.apache.commons.cli.ParseException;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.Display;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.perspective.ApplicationConfiguration;
import jpl.gds.perspective.ApplicationType;
import jpl.gds.perspective.DisplayConfiguration;
import jpl.gds.perspective.DisplayType;
import jpl.gds.perspective.PerspectiveConfiguration;
import jpl.gds.perspective.gui.PerspectiveActor;
import jpl.gds.perspective.gui.PerspectiveListener;
import jpl.gds.perspective.options.PerspectiveCommandOptions;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.metadata.InvalidMetadataException;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.sys.Shutdown;
import jpl.gds.tc.api.icmd.exception.AuthenticationException;
import jpl.gds.tcapp.app.AbstractUplinkApp;

/**
 * chill_up
 * 
 * This application is used to uplink data to FSW or SSE
 * 
 *
 *         Note: Get rid of System.exit in main. We now do the shutdown in a
 *         shutdown hook, because doing it in showGui doesn't shut down all
 *         topics. But the shutdown hooks are not called on a normal exit, so we
 *         force it by calling System.exit.
 * 
 *         That's not really satisfactory, because it doesn't guarantee that
 *         everything is shut down properly.
 *
 * NB: Business with passing the listener up to the superclass
 * is so that AccessControl can detect an ExitPerspectiveMessage and quit
 * trying to authenticate. That's important in the case where the user has
 * brought up chill_up but does not want to bother to authenticate. So when
 * the perspective is exited the CSS login window remains. With these changes
 * it will go away with a single close and forbear to issue warnings or errors.
 *
 * Also allows registration of the ExitPerspectiveMessage early on
 * so it can be detected whilst the CSS login window is active.
 */
public class UplinkApp extends AbstractUplinkApp implements IQuitSignalHandler {

    private PerspectiveCommandOptions perspectiveOpts;
    private ApplicationConfiguration  appConfig;

    @Override
    public void exitCleanly() {
        try {
            stopHeartbeat();

            stopTestConfigDatabase();

            stopExternalInterfaces();

        } catch (final Exception e) {
            log.error("Exception encountered while shutting down " + jpl.gds.shared.cli.app.ApplicationConfiguration.getApplicationName()
                    + ": " + ExceptionTools.getMessage(e), e);
        }
    }

    /**
     * {@inheritDoc}
     */
	@Override
	@SuppressWarnings("DM_EXIT")
    public void configure(final ICommandLine commandLine) throws ParseException {
		cmdConfig.setShowGui(true);

		super.configure(commandLine);
		
        appConfig = perspectiveOpts.APPLICATION_CONFIGURATION.parse(commandLine);

        uplinkOptions.UPLINK_RELEASE_PARAM.parse(commandLine);

		if (autorun) {
			writeOutSessionConfig();
		}
		
	     // Set up uplink security
        loginMethod = securityOptions.LOGIN_METHOD_GUI.parseWithDefault(commandLine, false, true);
        keytabFile = securityOptions.KEYTAB_FILE.parse(commandLine);
        
        checkLoginOptions();

	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final PrintWriter pw = new PrintWriter(System.out);
        pw.print("Usage: " + jpl.gds.shared.cli.app.ApplicationConfiguration.getApplicationName() + " <options>\n\n"
                + "This application is used to uplink data to FSW or SSE.\n\n");

        options.getOptions().printOptions(pw);

        pw.flush();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
    public BaseCommandOptions createOptions() {
        if (options != null) {
            return options;
        }
        options = super.createOptions();

        options.addOptions(dictOpts.getAllOptions());
        
        options.addOptions(securityOptions.getAllGuiOptions());

        options.addOption(uplinkOptions.UPLINK_RELEASE_PARAM);
        
        perspectiveOpts = new PerspectiveCommandOptions(appContext.getBean(PerspectiveConfiguration.class),
                ApplicationType.UPLINK);
        options.addOption(perspectiveOpts.APPLICATION_CONFIGURATION);
        perspectiveOpts.APPLICATION_CONFIGURATION.setHidden(true);
        
        /* only chill_up GUI needs this option */
        options.addOption(BaseCommandOptions.AUTORUN);
        
        /* Add SSE options */
        options.addOptions(connectionOpts.getAllSseUplinkOptionsNoConnectionType());

		return (options);
	}


	/**
	 * Show the GUI. Note that appConfig is set if it's integrated GUI.
	 * 
	 * @throws AuthenticationException if Access Control is enabled and user
	 *             authentication fails
	 * @throws InvalidMetadataException 
	 * @throws DictionaryException 
	 */
    protected void showGui() throws AuthenticationException, InvalidMetadataException, DictionaryException {
		// launch the SWT display
		final String app = jpl.gds.shared.cli.app.ApplicationConfiguration.getApplicationName("chill_up");
		Display.setAppName(app);
		Display display = null;
		try {
			display = Display.getDefault();
		} catch (final SWTError e) {
			if (e.getMessage().indexOf("No more handles") != -1) {
				throw new IllegalStateException(
						"Unable to initialize user interface.  If you are using X-Windows, make sure your DISPLAY variable is set.",
						e);
			} else {
				throw (e);
			}
		}

		// Do not show config GUI if attaching to an existing session.
        // Check if the user cancelled the GUI

		if (! this.testKeySpecified && showSessionConfigGui(display))
        {
            return;
		}
 

        DisplayConfiguration dispConfig = null;
        PerspectiveListener  listener   = null;

        if (appConfig != null)
        {
            dispConfig = appConfig.getDisplayConfig(DisplayType.UPLINK);
        }

        /**
         * The order is important here. The uplink shell is created
         * but the GUI is not created because it will need AccessControl
         * to be available.
         *
         * Ths listener is created and passed up to both the superclass
         * and the uplink shell.
         *
         * The external interfaces (including AccessControl) are started
         * and the listener is there for AccessControl so he can get
         * the exit perspective status.
         *
         * The uplink shell GUI is started. He then has both AccessControl
         * and the listener available.
         */

        /** Added parameter to not start GUI*/
        final UplinkShell us = new UplinkShell(appContext,
        		                               dispConfig,
                                               display,
                                               sessionConfig,
                                               this,
                                               false);

        if (appConfig != null)
        {
            /*
             * Pass true as last argument to the
             * PerspectiveActor so it will create a client heartbeat that
             * will allow it to detect when the message service goes down.
             */
            listener = new PerspectiveActor(appContext, appConfig, us, true);

            // Pass to superclass and thence to AccessControl
            setPerspectiveListener(listener);

            us.initPerspectiveListener(listener);
        }

        // Listener must be set before we start external interfaces
        // so it gets to AccessControl. Also we want to be able to detect
        // perspective exit early on.

        startExternalInterfaces();

        /**
         * Enabling and loading the dictionary.  Calling this early so that errors
         * happen right away.
         */
        loadConfiguredCommandDictionary();

    	if (! GdsSystemProperties.isIntegratedGui()) {
    		us.updateTitle();
    	}
        // This guy is the main user of the listener
        us.startGui();
        us.setPerspectiveListener(listener);

        // Bail if we are exiting already

        if ((listener != null) && listener.getPerspectiveExit())
        {
            if (! display.isDisposed())
            {
                display.dispose();
            }

            return;
        }

        if (sessionConfig != null)
        {
            writeOutSessionConfig();
        }

        us.open();

        while (! us.getShell().isDisposed())
        {
            if (! display.readAndDispatch())
            {
                display.sleep();
            }
        }

        display.dispose();

    }


	/**
	 * The command line interface.
	 *
	 * @param args The command line arguments
	 */
	@SuppressWarnings("DM_EXIT")
    public static void main(final String[] args) {

		final String appName = jpl.gds.shared.cli.app.ApplicationConfiguration.getApplicationName();

        Shutdown<ShutdownFunctorsEnum> shutdown = null;

        int status = 0;

		try
        {
			final UplinkApp app = new UplinkApp();

            shutdown = app.getShutdown();

            final ICommandLine commandLine = app.createOptions().parseCommandLine(args, true);
            app.configure(commandLine);

			app.showGui();
        }
        catch (final IllegalArgumentException iae)
        {
            status = 1;
            TraceManager.getDefaultTracer().error("Exception was encountered while running " + appName
					+ ": " + ExceptionTools.rollUpMessages(iae));
        }
        catch (final ParseException e)
        {
            status = 1;
            TraceManager.getDefaultTracer().error(ExceptionTools.getMessage(e));
        }
        catch (final Exception e)
        {
            status = 1;
            TraceManager.getDefaultTracer().error("Exception was encountered while running ", appName, ": ",
                                                  ExceptionTools.rollUpMessages(e));

            // Log exception stack trace to file and suppress from console
            // Note this is meant to replace the stack trace below, but I'm currently too scared to remove it
            TraceManager.getDefaultTracer().error(Markers.SUPPRESS, "Exception was encountered while running ", appName,
                                                  ": ", ExceptionTools.rollUpMessages(e), e);

			// I do not care WHO complains about this stack trace. Do not remove it.
			// Exceptions that can be anticipated must be caught and nicely logged rather 
			// than throwing to this point.  This is for exceptions that are NOT expected.
			// Having no stack trace removes all possibility of us diagnosing the error
			// without being able to both reproduce it AND recompile this, which can be
			// impossible.
            e.printStackTrace();
            
		}
        finally
        {
            if (shutdown != null) {
                shutdown.shutdown();
            }
            System.exit(status);
        }
	}

}
