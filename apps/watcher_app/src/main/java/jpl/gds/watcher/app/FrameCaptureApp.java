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
package jpl.gds.watcher.app;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.watcher.app.handler.ITelemetryIngestionCaptureHandler;
import jpl.gds.watcher.spring.bootstrap.WatcherSpringBootstrap;

/**
 * The frame capture app subscribes to the JMS and listens for frame messages
 * with user specified filters on specific topics. When one is received, the
 * frame metadata and data are pushed into the queue and handled by the
 * FrameCaptureHandler in the specified manner
 */
public class FrameCaptureApp extends TelemetryIngestionCaptureApp {

    /**
     * Constructor
     */
    public FrameCaptureApp(){
        super(TmServiceMessageType.TelemetryFrame, "Frame", true);
    }
    
    /**
     * Main application entry point.
     * 
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        final FrameCaptureApp app = new FrameCaptureApp();
        try {
            final ICommandLine cmdline = app.createOptions().parseCommandLine(args, true);
            app.configure(cmdline);
        } catch (final ParseException e) {
            TraceManager.getDefaultTracer().error(ExceptionTools.getMessage(e));
            System.exit(FAILURE);
        }
        
        if (app.init()) {
            app.run();
        }
        
        System.exit(app.getExitStatus());
    }

    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        capture = appContext.getBean(WatcherSpringBootstrap.FRAME_CAPTURE_HANDLER,
                                     ITelemetryIngestionCaptureHandler.class);

        super.configure(commandLine);
    }

}
