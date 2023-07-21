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
package jpl.gds.app.tools.socket;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.PortOption;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.cli.options.numeric.UnsignedLongOption;
import jpl.gds.shared.io.MeteredFileInputStream;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.types.UnsignedLong;
import jpl.gds.shared.util.HostPortUtility;

/**
 * MeterToSocketApp pushes a file's contents down a socket at a specified data
 * rate.
 */
public class MeterFileToSocketApp extends AbstractCommandLineApp implements IQuitSignalHandler {

    private static final String HOST_NAME_SHORT = "o";
    static final String HOST_NAME_LONG = "hostName";
    private static final String INFINITE_SEND_SHORT = "i";
    static final String INFINITE_SEND_LONG = "infiniteSend";
    private static final String CLIENT_MODE_SHORT = "c";
    static final String CLIENT_MODE_LONG = "client";
    private static final String RATE_SHORT = "r";
    static final String RATE_LONG = "bitrate";
    private static final String PORT_SHORT = "p";
    static final String PORT_LONG = "port";
    private static final String FILE_SHORT = "f";
    static final String FILE_LONG = "inputFile";
    private static final String BUFFER_SHORT = "s";
    static final String BUFFER_LONG = "bufferSize";
    static final String VERBOSE_LONG = "verbose";

    private static final int SOCKET_TIMEOUT = 5000;
    private static final int SOCKET_RETRY_TIME = 5000;
    private static final int DEFAULT_BIT_RATE = 512000;
    private static final int STARTUP_DELAY = 3000;
    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_ERROR = 1;
    
    private static final int SENT_BUFFER_RESET_COUNTER = 500;
    private int sentBufferCounter = 0;
    private final Tracer tracer;

    private int port = 0;
    private boolean clientMode;
    private boolean verbose;
    private boolean infiniteSend;
    private long bufferSize = MeteredFileInputStream.DEFAULT_OVERFLOW_SIZE;
    private String filename = "";
    private String hostname = "localhost";

    private OutputStream outputStream;
    private Socket clientSocket;
    private ServerSocket serverSocket;
    private long inputBitrate = DEFAULT_BIT_RATE;
    private MeteredFileInputStream meteredInputStream;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final AtomicBoolean shutdown = new AtomicBoolean(true);
    private int exitCode = EXIT_SUCCESS;
    private Thread appThread;
    
    private UnsignedLongOption bufferSizeOption;
    private StringOption hostOption;
    private FlagOption infiniteOption;
    private FlagOption clientOption;
    private PortOption portOption;
    private UnsignedLongOption bitRateOption;
    private FlagOption verboseOption;
    private FileOption inputFileOption;
    
    /**
     * Constructor.
     */
    public MeterFileToSocketApp() {
        tracer = TraceManager.getDefaultTracer();
       }

    /*
     * {@inheritDoc}
     * 
     * @see jpl.gds.cli.legacy.app.AbstractCommandLineApp#createOptions()
     */
    @Override
    public BaseCommandOptions createOptions() {
        
        if (optionsCreated.get()) {
            return options;
        }     

        super.createOptions();

        bufferSizeOption = new UnsignedLongOption(BUFFER_SHORT, BUFFER_LONG, "size", 
                "Overflow buffer size, in bytes (default = " + MeteredFileInputStream.DEFAULT_OVERFLOW_SIZE + " bytes)", 
                false, UnsignedLong.valueOf(MeteredFileInputStream.MIN_OVERFLOW_BUFFER_SIZE), null);
        bufferSizeOption.setDefaultValue(UnsignedLong.valueOf(MeteredFileInputStream.DEFAULT_OVERFLOW_SIZE));
        options.addOption(bufferSizeOption);
        
        hostOption = new StringOption(HOST_NAME_SHORT, HOST_NAME_LONG, "host", 
                "The hostname of the machine sending data (Default = \"localhost\").  Only applicable in client mode.", false);
        hostOption.setDefaultValue(HostPortUtility.LOCALHOST);
        options.addOption(hostOption);
        
        verboseOption = new FlagOption(null, VERBOSE_LONG, "Display verbose output", false);
        options.addOption(verboseOption);
        
        clientOption = new FlagOption(CLIENT_MODE_SHORT,CLIENT_MODE_LONG,"Run the application in client mode (Default = Server Mode)", false);
        options.addOption(clientOption);
        
        infiniteOption = new FlagOption(INFINITE_SEND_SHORT, INFINITE_SEND_LONG, 
                "Loop and send the input file indefinitely.  Application will never terminate.", false);
        options.addOption(infiniteOption);
        
        bitRateOption = new UnsignedLongOption(RATE_SHORT, RATE_LONG, "rate",  "Bitrate (bps) at which to send data (Default = "
                        + DEFAULT_BIT_RATE + " bps)", false, UnsignedLong.valueOf(8), 
                        UnsignedLong.valueOf(MeteredFileInputStream.MAX_RATE));
        bitRateOption.setDefaultValue(UnsignedLong.valueOf(DEFAULT_BIT_RATE));
        options.addOption(bitRateOption);
        
        portOption = new PortOption(PORT_SHORT, PORT_LONG, "port", "Port for socket connection (required)", true);
        options.addOption(portOption);
        
        inputFileOption = new FileOption(FILE_SHORT, FILE_LONG, "file", "Path to data file to send (required)", true, true);
        options.addOption(inputFileOption);
        
        return (options);
    }

    @Override
    public void exitCleanly() {

        tracer.debug("Exit signal is being processed");

        MeterFileToSocketApp.this.shuttingDown.set(true);
        /* Interrupt the app thread to stop any ongoing write to the client. */
        if (MeterFileToSocketApp.this.appThread != null) {
            MeterFileToSocketApp.this.appThread.interrupt();
        }
        cleanup();
        while (!MeterFileToSocketApp.this.shutdown.get()) {
            try {
                synchronized (MeterFileToSocketApp.this.shutdown) {
                    MeterFileToSocketApp.this.shutdown.wait();
                }
            } catch (final InterruptedException e) {
                SystemUtilities.doNothing();
            }
        }

    }

    /*
     * {@inheritDoc}
     * 
     * @see jpl.gds.cli.legacy.app.AbstractCommandLineApp#configure(org.apache.commons.cli.CommandLine)
     */
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        
        super.configure(commandLine);

        this.filename = inputFileOption.parse(commandLine, true);       
        this.port = portOption.parse(commandLine, true).intValue();
        this.hostname = hostOption.parseWithDefault(commandLine, false, true);
        this.bufferSize = bufferSizeOption.parseWithDefault(commandLine, false, true).longValue();
        this.inputBitrate = bitRateOption.parseWithDefault(commandLine, false, true).longValue();
        
        if (this.inputBitrate < 8 || this.inputBitrate % 8 != 0) {
            throw new ParseException(
                    "The -- "
                            + RATE_LONG
                            + " command line option requires an integer value between 8 and " + 
                            MeteredFileInputStream.MAX_RATE + " that is divisible by 8");

        }

        this.clientMode = clientOption.parse(commandLine);
        this.verbose = verboseOption.parse(commandLine);
        this.infiniteSend = infiniteOption.parse(commandLine);
        
    }

    /*
     * {@inheritDoc}
     * 
     * @see jpl.gds.cli.legacy.app.AbstractCommandLineApp#showHelp()
     */
    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final PrintWriter pw = new PrintWriter(System.out);
        
        pw.println("Usage: " + ApplicationConfiguration.getApplicationName()  + " --" + PORT_LONG + " <port> --" + 
        FILE_LONG
                + " <file> [options]\n");
        
        createOptions().getOptions().printOptions(pw);
        
        pw.flush(); 
    }

    /**
     * Gets the application exit status.
     * 
     * @return exit code
     */
    public int getExitCode() {

        return this.exitCode;
    }

    /**
     * Executes the main application logic.
     */
    public void run() {
    	
    	/* Set thread member. */
    	this.appThread = Thread.currentThread();

    	if (this.clientMode) {
            runInClientMode();
        } else {
            runInServerMode();
        }

        cleanup();
    }

    /**
     * Runs the data transfer with this application acting as socket client.
     */
    private void runInClientMode() {

        try {
            /*
             * Loop attempting to connect to the socket server until this
             * application is requested to shutdown.
             */
            do {
                try {
                    this.clientSocket = new Socket(this.hostname, this.port);

                    this.clientSocket.setSoTimeout(SOCKET_TIMEOUT);
                    
                    tracer.info("Connected to server " + this.hostname + ":" + this.port);
                    break;
                } catch (final ConnectException ce) {
                    tracer.warn("Can't connect to port number " + this.port
                            + " on host " + this.hostname + ". Retrying in " + SOCKET_RETRY_TIME
                            + " ms...\n");

                    // Sleep a while; ignore interrupts
                    SleepUtilities.checkedSleep(SOCKET_RETRY_TIME);
                }
            } while (!this.shuttingDown.get());

            /*
             * If shutting down, do nothing else.
             */
            if (this.shuttingDown.get()) {
                return;
            }

            /*
             * We have a connection to the socket server. Open the socket 
             * output stream.
             */
            this.outputStream = new DataOutputStream(
                    this.clientSocket.getOutputStream());
            /*
             * Wait a few seconds to allow the receiver to spin up.
             */
            SleepUtilities.checkedSleep(STARTUP_DELAY);

            
            /*
             * Start the metered input stream. It will begin reading the input file
             * into the input buffer as soon as open is called on it.
             */
            this.meteredInputStream = new MeteredFileInputStream(
                    this.filename, this.inputBitrate, this.infiniteSend, this.verbose, this.bufferSize, tracer);
            this.meteredInputStream.open();

            /*
             * Send the data to the server.
             */
            doSend();
        } catch (final SocketTimeoutException se) {
            tracer.info("Socket timeout.");
            this.exitCode = EXIT_ERROR;
        } catch (final SocketException se) {
            tracer.info("Socket connection closed.");
            this.exitCode = EXIT_SUCCESS;
        } catch (final IOException e) {
            tracer.error("Error transferring data: " + e.getMessage(), e);
            this.exitCode = EXIT_ERROR;
        } finally {
            if (this.meteredInputStream != null) {
            	this.meteredInputStream.close();
            }
        }
        /*
         * Let the application know we have a clean state for shutdown.
         */
        this.shutdown.set(true);
        synchronized(this.shutdown) {
        	this.shutdown.notifyAll();
        }
    }

    /**
     * Runs the data transfer with this application acting as socket client.
     */
    private void runInServerMode() {

        /*
         * Create the server socket. Any error or port in use
         * results in immediate return.
         */
        try {
            this.serverSocket = new ServerSocket(this.port);
            this.serverSocket.setSoTimeout(SOCKET_TIMEOUT);
        }
        catch (final BindException e) {
            tracer.error("Port is already in use by another application");
            this.exitCode = EXIT_ERROR;
            return;
        } catch (final IOException e) {
            tracer.error("Error creating server socket: " + e.getMessage(), e);
            this.exitCode = EXIT_ERROR;
            return;
        }

        /*
         * Loop and wait for client connection on the socket until
         * this application is asked to shutdown.
         */
        boolean timedOut = false;
        do {
            try {
            	/* Check for socket timeout. */
            	if (!timedOut) {
            		tracer.info("Waiting for client connection on port "
            				+ this.port + "...");
            	}
            	timedOut = false;
                this.clientSocket = this.serverSocket.accept();
                tracer.info("Accepted client connection.");

                /*
                 * Got a connection. Open the socket output stream to the client.
                 */
                this.outputStream = new DataOutputStream(
                        this.clientSocket.getOutputStream());
                
                /* Wait a few seconds to allow the receiver to spin up. */
                SleepUtilities.checkedSleep(STARTUP_DELAY);
                
                /*
                 * Start the metered input stream. It will begin reading the input file
                 * into the input buffer as soon as start is called on it.
                 */
                this.meteredInputStream = new MeteredFileInputStream(
                        this.filename, this.inputBitrate, this.infiniteSend, this.verbose, this.bufferSize, tracer);
                this.meteredInputStream.open();

                /*
                 * Send the metered data to the client.
                 */
                doSend();

            } catch (final SocketTimeoutException ste) {
            	tracer.debug("Socket accept timed out.");
            	timedOut = true;
            	
            } catch (final SocketException se) {
                tracer.info("Socket connection closed.");
                this.exitCode = EXIT_SUCCESS;
                break;
            } catch (final IOException e) {
                tracer.error("Error transferring data: " + e.getMessage(), e);
                this.exitCode = EXIT_ERROR;
                break;
            } finally {
            	if (this.meteredInputStream != null) {
            		this.meteredInputStream.close();
            	}
            }
            
        } while (!this.shuttingDown.get());
        
        /* Let the application know we have a clean state for shutdown. */
        this.shutdown.set(true);
        synchronized(this.shutdown) {
            this.shutdown.notifyAll();
        }
    }

    /**
     * Performs the actual data transfer over the socket connection.
     * 
     * @throws IOException
     *             if there is an error performing file or socket I/O.
     */
    private void doSend() throws IOException{

        /*
         * Tell the application we do not have a clean state for shutdown.
         */
        this.shutdown.set(false); 
        
        tracer.info("Writing data from file " + this.filename
                + " with block size " + this.meteredInputStream.getReadBufferSize()
                + " at bit rate of " + this.inputBitrate
                + " bps and clock tick interval of "
                + this.meteredInputStream.getClockInterval() + " ms");

        long totalBytes = 0;
        final long startTime = System.currentTimeMillis();
        if (this.verbose) {
            tracer.info("Start time = " + startTime + " (ms since epoch 1970)");
        }

        /*
         * Loop and read from the metered input stream, writing each buffer to the
         * socket output stream. Stop if the application is instructed to shutdown
         * or no more data is available.
         */
        byte[] buffer = this.meteredInputStream.readNoBuffer();

        while (buffer.length != 0 && !this.shuttingDown.get()) {
            final int readCount = buffer.length;
            if (readCount > 0) {
                totalBytes += readCount;
                
                if (this.infiniteSend && this.sentBufferCounter > SENT_BUFFER_RESET_COUNTER) { 
                	tracer.info("Writing " + readCount 
                    		+ " bytes to client socket. Total: " + totalBytes);
                	this.sentBufferCounter = 0;
                }
                tracer.debug("Writing " + readCount 
                		+ " bytes to client socket. Total: " + totalBytes);
                            
                if (this.outputStream != null) {
                    this.outputStream.write(buffer, 0, readCount);
                    this.sentBufferCounter++;
                }
            }
            if (!this.shuttingDown.get()) {
                buffer = this.meteredInputStream.readNoBuffer();
            }
        }

        reportStats(totalBytes, startTime);

        // If we are in server mode and not in infinite send mode, throw 
        // a socket exception to make it look like we need to exit.
        if (!(clientMode || infiniteSend)) {
        		throw new SocketException("Just want to shut down cleanly.");
        }
    }

    private void reportStats(final long totalBytes, final long startTime) {
        /*
         * Report statistics.
         */
        final long endTime = System.currentTimeMillis();
        if (this.verbose) {
            tracer.info("End time = " + endTime + " (ms since epoch 1970)");
        }

        final double bitRate = (totalBytes * 8.0)
                / ((endTime - startTime) / 1000.0);
        tracer.info("Wrote " + totalBytes + " bytes from the file "
                + this.filename + " to socket at a bitrate of "
                + String.format("%.0f", bitRate) + " bits/second");
    }

    /**
     * Cleans up things prior to shutdown of the application.
     */
    private void cleanup() {

        if (this.meteredInputStream != null) {
            this.meteredInputStream.close();
            this.meteredInputStream = null;
        }

        try {
            if (this.outputStream != null) {
                this.outputStream.close();
                this.outputStream = null;
            }
        } catch (final IOException e) {
            SystemUtilities.doNothing();
        }

        try {
            if (this.clientSocket != null) {
                this.clientSocket.close();
                this.clientSocket = null;
            }
        } catch (final IOException e) {
            SystemUtilities.doNothing();
        }

        try {
            if (this.serverSocket != null) {
                this.serverSocket.close();
                this.serverSocket = null;
            }
        } catch (final IOException e) {
            SystemUtilities.doNothing();
        }
    }

    /**
     * Main application entry point.
     * 
     * @param args
     *            command line arguments.
     */
    public static void main(final String[] args) {

        final MeterFileToSocketApp app = new MeterFileToSocketApp();

        try {
            final ICommandLine cmdline = app.createOptions().parseCommandLine(args,
                    true);
            app.configure(cmdline);
            app.run();
            
        } catch (final ParseException pe) {
            TraceManager.getDefaultTracer().error(pe.getMessage());
            System.exit(EXIT_ERROR);
            
        } catch (final Exception e) {
            TraceManager.getDefaultTracer().error(e.getMessage(), e);
            System.exit(EXIT_ERROR);
        }

        System.exit(app.getExitCode());
    }

    // package private getters to use for tests

    boolean isClientMode() {
        return clientMode;
    }

    boolean isVerbose() {
        return verbose;
    }

    long getBufferSize() {
        return bufferSize;
    }

    String getHostname() {
        return hostname;
    }

    boolean isInfiniteSend() {
        return infiniteSend;
    }

    String getFilename() {
        return filename;
    }

    long getInputBitrate() {
        return inputBitrate;
    }
    
    
}
