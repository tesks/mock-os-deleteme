/*
 * Copyright 2006-2021. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.mds.server;

import jpl.gds.mds.server.config.MdsProperties;
import jpl.gds.mds.server.tcp.TcpSocketServer;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.PortOption;
import jpl.gds.shared.cli.options.numeric.UnsignedIntOptionParser;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.types.UnsignedInteger;

import javax.annotation.PostConstruct;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Monitor data service
 */
public class MonitorDataService extends AbstractCommandLineApp implements Runnable{

    static final String TCP_PORT_LONG = "tcpPort";
    static final String UDP_PORT_LONG  = "udpPort";

    private final ApplicationContext appContext;

    /** Command-line arguments */
    @Autowired
    protected ApplicationArguments arguments;

    @Autowired
    private MdsProperties   mdsProperties;

    @Autowired
    private TcpSocketServer tcpSocketServer;

    /** Tracer */
    protected Tracer tracer;

    /** ICommandLine object */
    protected ICommandLine commandLine;

    /** Exiting Atomic Boolean */
    public static final AtomicBoolean exiting = new AtomicBoolean(false);

    private final AtomicBoolean springInitialized = new AtomicBoolean(false);

    private PortOption tcpPortOption;
    private PortOption udpPortOption;

    /**
     * Constructor
     * @param appContext Application context
     */
    public MonitorDataService(final ApplicationContext appContext) {
        this.appContext = appContext;
        tracer = TraceManager.getTracer(Loggers.MDS);
    }

    /**
     * Init method
     */
    @PostConstruct
    public void init() {
        createOptions();

        try {
            this.commandLine = options.parseCommandLine(arguments.getSourceArgs(), true);
            configure(commandLine);
        }
        catch (final ParseException e) {
            tracer.error(ExceptionTools.getMessage(e));
            System.exit(1);
        }

        springInitialized.getAndSet(true);
    }

    @Override
    public void exitCleanly() {
        if (helpDisplayed.get() || versionDisplayed.get()) {
            return;
        }

        if (exiting.getAndSet(true)) {
            return;
        }

        // Don't run shutdown hook stuff if the application did not start OK
        if (!springInitialized.get()) {
            return;
        }

        tracer.info(this, " has received a shutdown request...");

        // Closing spring context should be last
        final ConfigurableApplicationContext context = (ConfigurableApplicationContext) appContext;
        if (context.isActive()) {
            context.close();
        }
    }

    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        if (options == null) {
            createOptions();
        }

        final PrintWriter pw = new PrintWriter(System.out);
        pw.print("Usage: " + jpl.gds.shared.cli.app.ApplicationConfiguration.getApplicationName());
        pw.println("\n");
        options.getOptions().printOptions(pw);

        pw.flush();
    }

    @Override
    public void run() {
        Thread.currentThread().setName(ApplicationConfiguration.getApplicationName());
        tracer.info("Starting up MDS Service ...");
        if(!mdsProperties.getUdpForwardHost().isEmpty()) {
            tracer.info("Will forward UDP to ", mdsProperties.getUdpForwardHost());
        }
        tcpSocketServer.start(mdsProperties.getSocketPort());

    }

    @Override
    public BaseCommandOptions createOptions() {
        if (options != null) {
            return options;
        }

        super.createOptions();

        //TCP port potion
        tcpPortOption = new PortOption("t", TCP_PORT_LONG, "port",
                                       "network port to use for TCP server", false);
        tcpPortOption.setParser(new UnsignedIntOptionParser());
        options.addOption(tcpPortOption);

        //UDP port option
        udpPortOption = new PortOption("u", UDP_PORT_LONG, "port",
                                       "network port to use for UDP client", false);
        udpPortOption.setParser(new UnsignedIntOptionParser());
        options.addOption(udpPortOption);

        return options;
    }

    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        super.configure(commandLine);

        if(commandLine.hasOption(TCP_PORT_LONG)) {
            final UnsignedInteger tcpPort = tcpPortOption.parse(commandLine);
            if(tcpPort != null) {
                mdsProperties.setSocketPort(tcpPort.intValue());
            }
        }

        if(commandLine.hasOption(UDP_PORT_LONG)) {
            final UnsignedInteger udpPort = udpPortOption.parse(commandLine);
            if (udpPort != null) {
                mdsProperties.setClientPort(udpPort.intValue());
            }
        }
    }
}
