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

import jpl.gds.common.config.connection.ConnectionKey;
import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.connection.IDownlinkConnection;
import jpl.gds.common.config.connection.NetworkConnection;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.context.api.EnableFswDownlinkContextFlag;
import jpl.gds.context.api.EnableSseDownlinkContextFlag;
import jpl.gds.context.api.IVenueConfiguration;
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
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.telem.down.gui.AbstractDownShell;
import jpl.gds.telem.down.gui.SSEDownShell;
import jpl.gds.telem.input.api.config.BufferedInputModeTypeOption;
import jpl.gds.telem.input.api.config.TelemetryInputProperties;
import org.apache.commons.cli.ParseException;
import org.eclipse.swt.widgets.Display;
import org.springframework.context.ApplicationContext;
import sun.nio.ch.Net;

import javax.annotation.PostConstruct;

/**
 * SseDownlinkApp is the SSE-specific main chill_down application class. 
 * The DownlinkApp class should be used in scripts that want to execute chill_down.
 * It will create an instance of this class if appropriate. This classes performs
 * argument parsing that is specific only to the SSE instance of chill_down.
 * 
 */
public class SseDownlinkApp extends AbstractDownlinkApp {

    /**
     * Creates an instance of SseDownlinkApp.
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public SseDownlinkApp(final ApplicationContext appContext) {
        super(appContext, "sse_chill_down");
        appContext.getBean(SseContextFlag.class).setApplicationIsSse(true);
	}

    /**
     * @throws ParseException
     *             if command line cannot be parsed
     */
    @PostConstruct
    public void configure() throws ParseException {
        configure(springContext.getBean(ICommandLine.class));
    }


	@Override
	public void configure(final ICommandLine commandLine) throws ParseException {
		super.configure(commandLine);
		
		final ConnectionProperties connectionProps = springContext.getBean(ConnectionProperties.class);
		final IVenueConfiguration venueConfig = springContext.getBean(IVenueConfiguration.class);

		if (GdsSystemProperties.isIntegratedGui()) {
			final VenueType venueType = springContext.getBean(IVenueConfiguration.class).getVenueType();
			final TelemetryConnectionType originalDct            = contextConfig.getConnectionConfiguration().getSseDownlinkConnection()
					.getDownlinkConnectionType();
			final TelemetryConnectionType newDct = connectionProps.getSseOverrideTelemetryConnectionType(venueType, originalDct);

			// Save for later
			final IDownlinkConnection originalDownlinkConn = contextConfig.getConnectionConfiguration().getSseDownlinkConnection();

			final TelemetryInputType originalInputFormat = contextConfig.getConnectionConfiguration().getSseDownlinkConnection().getInputType();
			final TelemetryInputType newTelemetryInputFormat =connectionProps.getSseOverrideTelemetryInputType(venueType, newDct, originalInputFormat);
			contextConfig.getConnectionConfiguration().createSseDownlinkConnection(newDct);

			IDownlinkConnection newDc = contextConfig.getConnectionConfiguration().getSseDownlinkConnection();
			if ((originalDownlinkConn instanceof NetworkConnection) && (newDc instanceof NetworkConnection)) {
				int port = ((NetworkConnection) originalDownlinkConn).getPort();
				String host  = ((NetworkConnection) originalDownlinkConn).getHost();
				((NetworkConnection) newDc).setHost(host);
				((NetworkConnection) newDc).setPort(port);
			}
			newDc.setInputType(newTelemetryInputFormat);
			if (log != null) {
                log.debug("SSE downlink connection type will be " + newDct + " and input format will be " + newDct);
            }


		}

		this.connectionOpts.SSE_DOWNLINK_HOST.parse(commandLine);
		this.connectionOpts.SSE_DOWNLINK_PORT.parse(commandLine);

		// clearout chill_up and Fsw config entries from being set into the DB
		springContext.getBean(EnableSseDownlinkContextFlag.class).setSseDownlinkEnabled(true);

		// isSse is getting set in the constructor, but overwritten in super.configure
		// setting isSse back to true
		SseContextFlag flag = contextConfig.getGeneralInfo().getSseContextFlag();
		flag.setApplicationIsSse(true);

		if (!GdsSystemProperties.isIntegratedGui()) {
		   springContext.getBean(EnableFswDownlinkContextFlag.class).setFswDownlinkEnabled(false);
		   contextConfig.getConnectionConfiguration().remove(ConnectionKey.FSW_UPLINK);
		   contextConfig.getConnectionConfiguration().remove(ConnectionKey.FSW_DOWNLINK);
		   contextConfig.getConnectionConfiguration().remove(ConnectionKey.SSE_UPLINK);
		}


        /* Validate the final session configuration */
        final SessionConfigurationValidator scv = new SessionConfigurationValidator((SessionConfiguration)contextConfig);
        
        if (!scv.validate(false, autoRun)) {
           throw new ParseException(scv.getErrorsAsMultilineString());
        }

	}

	@Override
	public BaseCommandOptions createOptions() {
	    
	    if (options != null) {
            return options;
        }
	    
	    super.createOptions();

		perspectiveOpts = new PerspectiveCommandOptions(
				getAppContext().getBean(PerspectiveConfiguration.class),
				ApplicationType.DOWNLINK, 
				false, 
				true);

	    options.addOption(perspectiveOpts.APPLICATION_CONFIGURATION);
		
		bufferOption = new BufferedInputModeTypeOption(false, 
				springContext.getBean(TelemetryInputProperties.class), false, true);
        options.addOption(bufferOption);
        options.addOptions(dictOpts.getSseOptions());
        options.addOptions(sessionOpts.getAllSseDownlinkOptions());
        options.addOptions(connectionOpts.getAllSseDownlinkOptions());
   
		return options;
	}

	@Override
	public void launchGuiApp() 
	{
		super.launchGuiApp(false, true);
	}

	@Override
	protected AbstractDownShell createGuiShell(final Display mainDisplay) {
		final AbstractDownShell ds = new SSEDownShell(springContext, mainDisplay, this, contextConfig, 
				appConfig == null ? null : appConfig.getDisplayConfig(DisplayType.SSE_DOWN));
		if (appConfig != null) {
			/*
			 * Pass true as last argument when
			 * creating perspective actor because we want it to start a client
			 * heartbeat in order to detect the message service is down.
			 */
			final PerspectiveListener listener = new PerspectiveActor(springContext, appConfig, ds, true);
			ds.setPerspectiveListener(listener);
		}
		return ds;
	}
}
