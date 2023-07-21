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
package jpl.gds.telem.down.app;

import javax.annotation.PostConstruct;

import org.apache.commons.cli.ParseException;
import org.eclipse.swt.widgets.Display;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.ConnectionKey;
import jpl.gds.context.api.EnableFswDownlinkContextFlag;
import jpl.gds.context.api.EnableSseDownlinkContextFlag;
import jpl.gds.perspective.ApplicationType;
import jpl.gds.perspective.DisplayType;
import jpl.gds.perspective.PerspectiveConfiguration;
import jpl.gds.perspective.gui.PerspectiveActor;
import jpl.gds.perspective.gui.PerspectiveListener;
import jpl.gds.perspective.options.PerspectiveCommandOptions;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.session.config.SessionConfigurationValidator;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.telem.down.gui.AbstractDownShell;
import jpl.gds.telem.down.gui.FSWDownShell;
import jpl.gds.telem.input.api.config.BufferedInputModeTypeOption;
import jpl.gds.telem.input.api.config.TelemetryInputProperties;

/**
 * FswDownlinkApp is the flight-specific main chill_down application class. 
 * The DownlinkApp class should be used in scripts that want to execute chill_down.
 * It will create an instance of this class if appropriate. This classes performs
 * argument parsing that is specific only to the FSW instance of chill_down.
 * 
 *
 */
public class FswDownlinkApp extends AbstractDownlinkApp {
    /**
     * Creates an instance of FswDownlinkApp.
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public FswDownlinkApp(final ApplicationContext appContext) {
        super(appContext, "chill_down");
	}

    /**
     * @throws ParseException
     *             if an error occurs parsing the command line
     */
    @PostConstruct
    public void configure() throws ParseException {
        configure(springContext.getBean(ICommandLine.class));
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void configure(final ICommandLine commandLine) throws ParseException {
		super.configure(commandLine);
		
    
		// clearout chill_up and Sse config entries from being set into the DB
	    springContext.getBean(EnableFswDownlinkContextFlag.class).setFswDownlinkEnabled(true);
	
		if (!GdsSystemProperties.isIntegratedGui()) {
		   springContext.getBean(EnableSseDownlinkContextFlag.class).setSseDownlinkEnabled(false);
		   contextConfig.getConnectionConfiguration().remove(ConnectionKey.FSW_UPLINK);
		   contextConfig.getConnectionConfiguration().remove(ConnectionKey.SSE_DOWNLINK);
		   contextConfig.getConnectionConfiguration().remove(ConnectionKey.SSE_UPLINK);
		}
				
	     
        /*  Validate the final session configuration */
        final SessionConfigurationValidator scv = new SessionConfigurationValidator((SessionConfiguration)contextConfig);
        
        if (!scv.validate(false, autoRun)) {
            throw new ParseException(scv.getErrorsAsMultilineString());
        }

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BaseCommandOptions createOptions() {
	    
	    if (options != null) {
            return options;
        }
	    
        super.createOptions();
		
		perspectiveOpts = new PerspectiveCommandOptions(
				getAppContext().getBean(PerspectiveConfiguration.class),
				ApplicationType.DOWNLINK, 
				true, 
				false);
	    options.addOption(perspectiveOpts.APPLICATION_CONFIGURATION);
		
		bufferOption = new BufferedInputModeTypeOption(false, springContext.getBean(TelemetryInputProperties.class), true, false);
        options.addOption(bufferOption);
        
        options.addOptions(dictOpts.getFswOptions());
        options.addOptions(sessionOpts.getAllFswDownlinkOptions());
        options.addOptions(connectionOpts.getAllFswDownlinkOptions());
   
		return options;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void launchGuiApp() {
		super.launchGuiApp(true, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected AbstractDownShell createGuiShell(final Display mainDisplay) {
		final AbstractDownShell ds = new FSWDownShell(springContext, mainDisplay, this, contextConfig,
				appConfig == null ? null : appConfig
						.getDisplayConfig(DisplayType.FSW_DOWN));
		if (appConfig != null) {
			/*
			 * Pass true as last argument when
			 * creating perspective actor because we want it to start a client
			 * heartbeat in order to detect the message service is down.
			 */
			final PerspectiveListener listener = new PerspectiveActor(springContext, appConfig,
					ds, true);
			ds.setPerspectiveListener(listener);
		}
		return ds;
	}

}
