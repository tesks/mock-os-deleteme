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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.numeric.UnsignedIntOption;
import jpl.gds.shared.exceptions.ExcessiveInterruptException;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.shared.util.HostPortUtility;


/**
 * SendToSocketApp pushes a file's contents down a socket in order to test
 * the downlink process reading raw data from a socket.
 *
 * Changes throughout to check shutting down
 * flag for more graceful shutdown following control-c. PMD corrections.
 */
public class SendToSocketApp extends AbstractCommandLineApp implements Runnable, IQuitSignalHandler
{
    private static final String BUFFER_SIZE_SHORT = "b";
    static final String BUFFER_SIZE_LONG = "bufferSize";
    private static final String HOST_NAME_SHORT = "o";
    static final String HOST_NAME_LONG = "hostName";
    private static final String METER_INTERVAL_SHORT = "m";
    static final String METER_INTERVAL_LONG = "meterInterval";
    private static final String INFINITE_SEND_SHORT = "i";
    static final String INFINITE_SEND_LONG = "infiniteSend";
    private static final String CLIENT_MODE_SHORT = "c";
    static final String CLIENT_MODE_LONG = "client";

    private static final int RETRY_TIME = 5000;
    private static final int BUFF_SIZE = 800;

    private int port;
    private int bufferSize;
    private long meterInterval;
    private boolean clientMode;
    private boolean verbose;
    private boolean infiniteSend;
    private String filename;
    private String hostname;

    private OutputStream outputStream;
    private InputStream inputStream;
    private Socket clientSocket;
    private ServerSocket serverSocket;
    /* Flag for graceful shutdown. */
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    
    private UnsignedIntOption bufferSizeOption;
    private UnsignedIntOption meterIntervalOption;
    private StringOption hostOption;
    private FlagOption infiniteOption;
    private FlagOption clientOption;
    
    private final Tracer trace = TraceManager.getDefaultTracer();

    /**
     * Constructor.
     */
    public SendToSocketApp() 
    {
        this.port = 0;
        this.bufferSize = BUFF_SIZE;
        this.meterInterval = 0;
        this.clientMode = false;
        this.verbose = false;
        this.infiniteSend = false;
        this.filename = "";
        this.hostname = "localhost";

        this.outputStream = null;
        this.inputStream = null;
        this.clientSocket = null;
        this.serverSocket = null;
    }

    private void cleanup()
    {
        try
        {
            if(this.inputStream != null)
            {
                this.inputStream.close();
                this.inputStream = null;
            }
        }
        catch(final IOException e)
        {
            //ignore
        }

        try
        {
            if(this.outputStream != null)
            {
                this.outputStream.close();
                this.outputStream = null;
            }
        }
        catch(final IOException e)
        {
            //ignore
        }

        try
        {
            if(this.clientSocket != null)
            {
                this.clientSocket.close();
                this.clientSocket = null;
            }
        }
        catch(final IOException e)
        {
            //ignore
        }

        try
        {
            if(this.serverSocket != null)
            {
                this.serverSocket.close();
                this.serverSocket = null;
            }
        }
        catch(final IOException e)
        {
            //ignore
        }
    }

    @Override
    public void exitCleanly() {
        shuttingDown.set(true);
        cleanup();
    }

    @Override
    @SuppressWarnings("DM_EXIT")
    public void configure(final ICommandLine commandLine) throws ParseException
    {
        super.configure(commandLine);
        
  
        final String[] leftoverArgs = commandLine.getTrailingArguments();
        
        if(leftoverArgs.length != 2)
        {
            throw new ParseException("Required value(s) missing on the command line.  Please supply both the port number and the input file.");
        }

        try
        {
            this.port = Integer.parseInt(leftoverArgs[0]);
        }
        catch(final NumberFormatException nfe)
        {
            throw new ParseException("The input value " + leftoverArgs[0] + " is not a valid port number.");
        }

        if (!HostPortUtility.isPortValid(port))
        {
            throw new ParseException("The input value " + leftoverArgs[0] + " is not a valid port number.");
        }

        this.filename = leftoverArgs[1];

        this.bufferSize = bufferSizeOption.parseWithDefault(commandLine, false, true).intValue();
        
        this.meterInterval = meterIntervalOption.parseWithDefault(commandLine, false, true).intValue();

        this.hostname = hostOption.parseWithDefault(commandLine, false, true);

        this.clientMode = clientOption.parse(commandLine);
        this.verbose = BaseCommandOptions.DEBUG.parse(commandLine);
        this.infiniteSend = infiniteOption.parse(commandLine);
    }

    @Override
    public void run()
    {
        if(this.clientMode)
        {
            runInClientMode();
        }
        else
        {
            runInServerMode();
        }

        cleanup();
    }

    /**
     * Performs sending of data as a socket client.
     */
    @SuppressWarnings("DM_EXIT")
    protected void runInClientMode()
    {
        try
        {
            do
            {
                try
                {
                    this.clientSocket = new Socket(this.hostname,this.port);
                    System.out.println("Connected to server on " + this.hostname);
                    System.out.println("Writing data from file " + this.filename + " with block size " + this.bufferSize + " and meter interval of " + this.meterInterval + "ms");
                    break;
                }
                catch(final ConnectException ce)
                {
                    trace.error("Can't connect to port number " + this.port + " on host " + this.hostname + ". Retrying in " + RETRY_TIME + " ms...\n");

                    // Sleep a while; ignore interrupts
                    SleepUtilities.checkedSleep(RETRY_TIME);
                }
            } while (!shuttingDown.get());

            if (shuttingDown.get()) {
                return;
            }

            this.outputStream = new DataOutputStream(this.clientSocket.getOutputStream());

            doSend();
        }
        catch (final SocketException se) {
            trace.error("Socket connection closed.");
            System.exit(1);
        }
        catch (final IOException e)
        {
            trace.error("Error transferring data: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Performs sending of data as a socket server.
     */
    @SuppressWarnings("DM_EXIT")
    protected void runInServerMode()
    {
        try
        {
            this.serverSocket = new ServerSocket(this.port);
        }
        /*
         * Catch bind exception and report address in use.
         */
        catch (final BindException e)
        {
            trace.error("Port is already in use by another application");
            System.exit(1);
        }
        catch (final IOException e)
        {
            trace.error("Error creating server socket: " + e.getMessage(), e);
            System.exit(1);
        }

        do
        {
            try
            {
                System.out.println("Waiting for client connection on port " + this.port + "...");
                this.clientSocket = this.serverSocket.accept();
                System.out.println("Accepted client connection.");
                System.out.println("Writing data from file " + this.filename + " with block size " + this.bufferSize + " and meter interval of " + this.meterInterval + "ms");

                this.outputStream = new DataOutputStream(this.clientSocket.getOutputStream());

                doSend();
            }
            catch (final SocketException se) {
                System.out.println("Socket connection closed.");
                System.exit(1);
            }
            catch (final IOException e)
            {
                trace.error("Error transferring data: " + e.getMessage(), e);
                System.exit(1);
            }
        } while (!shuttingDown.get());
    }

    @SuppressWarnings({"DM_EXIT"})
    private void doSend() throws IOException
    {
        int totalBytes = 0;
        final byte[] buffer = new byte[this.bufferSize];

        final long startTime = System.currentTimeMillis();
        if(this.verbose)
        {
            System.out.println("Start time = " + startTime + " (ms since epoch 1970)");
        }
        /*
         * when an interrupt e.g. ctrl-c happens in the loop- the shutdown handler closes
         * up sockets and streams causing exceptions to be thrown if the thread is still
         * reading and writing
         */
        try {
            do
            {
                if(this.verbose)
                {
                    System.out.println("(Re)starting transfer of file " + this.filename);
                }

                this.inputStream = new FileInputStream(new File(this.filename));

                totalBytes = doSendLoop(totalBytes, buffer);	 
                
                if (this.inputStream != null) {
                    this.inputStream.close();
                }

            } while (!this.shuttingDown.get() && this.infiniteSend);

        } catch (final Exception e) {
            System.out.println("Send interrupted");

        }
        final long endTime = System.currentTimeMillis();
        if(this.verbose)
        {
            System.out.println("End time = " + endTime + " (ms since epoch 1970)");
        }

        final double bitRate = (totalBytes*8.0)/((endTime-startTime)/1000.0);
        System.out.println("Wrote " + totalBytes + " bytes from the file " + this.filename + " to socket at a bitrate of " + bitRate + " bits/second");
    }

    private int doSendLoop(final int totalBytes, final byte[] buffer) throws IOException {
        int localBytes = totalBytes;
        
        int readCount = this.inputStream.read(buffer);
       
        while (readCount != -1 && !shuttingDown.get())
        {
            if (readCount > 0)
            {
                localBytes += readCount;
                if(this.verbose)
                {
                    System.out.println("Writing " + readCount + " bytes to client socket. Total: " + totalBytes);
                }
                if (this.outputStream != null) {
                    this.outputStream.write(buffer, 0, readCount);
                }

                try
                {
                    SleepUtilities.fullSleep(this.meterInterval);
                }
                catch (final ExcessiveInterruptException eie)
                {
                    TraceManager.getDefaultTracer().error("Error sleeping: " + eie.getMessage(), eie);
                    System.exit(1);
                }
            }
            readCount = this.inputStream.read(buffer);
            
        }
        return localBytes;
    }

    /**
     * Main application entry point.
     * @param args command line arguments
     */
    public static void main(final String[] args)
    {
        SendToSocketApp app = null;

        try {
            app = new SendToSocketApp();
            app.configure(app.createOptions().parseCommandLine(args, true));
        } catch (final ParseException pe) {
            TraceManager.getDefaultTracer().error(pe.getMessage());
            System.exit(1);
        }

        app.run();
    }

    /**
     * Creates an Options object containing possible command line
     * arguments/options.
     *
     * @return the Options object
     */
    @Override
    public BaseCommandOptions createOptions()
    {
        
        if (optionsCreated.get()) {
            return options;
        }
        
        super.createOptions();
        options.addOption(BaseCommandOptions.DEBUG);

        bufferSizeOption = new UnsignedIntOption(BUFFER_SIZE_SHORT, BUFFER_SIZE_LONG, "size", 
                "The size in bytes of the output buffer. (Default = " + BUFF_SIZE + " bytes)", false);
        bufferSizeOption.setDefaultValue(UnsignedInteger.valueOf(BUFF_SIZE));
        options.addOption(bufferSizeOption);
        
        hostOption = new StringOption(HOST_NAME_SHORT, HOST_NAME_LONG, "host", 
                "The hostname of the machine sending data (Default = \"localhost\").  Only applicable in client mode.", false);
        hostOption.setDefaultValue(HostPortUtility.LOCALHOST);
        options.addOption(hostOption);
        
        infiniteOption = new FlagOption(INFINITE_SEND_SHORT, INFINITE_SEND_LONG, 
                "Loop and send the input file indefinitely.  Application will never terminate.", false);
        options.addOption(infiniteOption);
        
        clientOption = new FlagOption(CLIENT_MODE_SHORT, CLIENT_MODE_LONG,
                "Run the application in client mode (Default = Server Mode)", false);
        options.addOption(clientOption);
        
        meterIntervalOption = new UnsignedIntOption(METER_INTERVAL_SHORT, METER_INTERVAL_LONG, 
                "interval", "The time in milliseconds to wait between buffer sends. (Default = 0)", false);
        meterIntervalOption.setDefaultValue(UnsignedInteger.MIN_VALUE);
        options.addOption(meterIntervalOption);
        
        return options;
    }

    /**
     * Get the usage for this application
     */
    @Override
    public void showHelp()
    {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final PrintWriter pw = new PrintWriter(System.out);

        pw.println("Usage: " + ApplicationConfiguration.getApplicationName() + " [options] <port> <input_filename>\n");

        createOptions().getOptions().printOptions(pw);
        
        pw.flush(); 
      
    }

    // package private getters to use for tests

    boolean isClientMode() {
        return clientMode;
    }

    boolean isVerbose() {
        return verbose;
    }

    int getBufferSize() {
        return bufferSize;
    }

    String getHostname() {
        return hostname;
    }

    boolean isInfiniteSend() {
        return infiniteSend;
    }

    long getMeterInterval() {
        return meterInterval;
    }
}
