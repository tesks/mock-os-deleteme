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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.numeric.UnsignedIntOption;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.shared.util.HostPortUtility;

/**
 * GetFromSocketApp receives content from a socket and writes it to a file.
 */
public class GetFromSocketApp extends AbstractCommandLineApp implements Runnable
{

    /** bufferSize option */
    public static final String  BUFFER_LONG      = "bufferSize";
    /** hostname option */
    public static final String  HOSTNAME_LONG    = "hostname";
    /** append option */
    public static final String  APPEND_LONG   = "append";
    /** client option */
    public static final String  CLIENT_LONG   = "client";
    /** verbose option */
    public static final String  VERBOSE_LONG  = "verbose";

	private static final Tracer trace = TraceManager.getDefaultTracer();
	
	private static final int RETRY_TIME = 5000;
    private static final int BUFF_SIZE = 800;

    private int port;
    private int bufferSize;
    private boolean clientMode;
    private boolean verbose;
    private boolean continuousAppend;
    private String filename;
    private String hostname;
    
    private FileOutputStream os;
	private DataInputStream is;
	
	private UnsignedIntOption bufferSizeOption;
	private StringOption hostOption;
	private FlagOption verboseOption;
	private FlagOption clientOption;
	private FlagOption appendOption; 
    
    /**
     * Constructor.
     * 
     */
    public GetFromSocketApp() 
    {
    	port = 0;
    	bufferSize = BUFF_SIZE;
    	clientMode = false;
    	verbose = false;
    	continuousAppend = false;
    	filename = "";
    	hostname = "localhost";
    	
    	os = null;
    	is = null;

    }
    
    @Override
    @SuppressWarnings("DM_EXIT")
    public void configure(final ICommandLine commandLine) throws ParseException
    {
        super.configure(commandLine);
        
		final String[] leftoverArgs = commandLine.getTrailingArguments();
		if(leftoverArgs.length != 2)
		{
			showHelp();
			System.exit(1);
		}

		try {
		    port = Integer.parseInt(leftoverArgs[0]);
		    if (!HostPortUtility.isPortValid(port)) {
		        throw new ParseException("Port must be a non-negative integer between 0 and " + HostPortUtility.MAX_PORT_NUMBER);
		    }
		} catch (final NumberFormatException e) {
		    throw new ParseException("Port must be a non-negative integer between 0 and " + HostPortUtility.MAX_PORT_NUMBER);
		}
		
		filename = leftoverArgs[1];

		bufferSize = bufferSizeOption.parseWithDefault(commandLine, false, true).intValue();
		
	    hostname = hostOption.parseWithDefault(commandLine, false, true);
	  
		clientMode = clientOption.parse(commandLine);
		verbose = verboseOption.parse(commandLine);
		continuousAppend = appendOption.parse(commandLine);
    }
    
    @Override
    public void run()
    {
    	if(clientMode)
    	{
    		runInClientMode();
    	}
    	else
    	{
    		runInServerMode();
    	}
    }
    
    /**
     * Perform receipt of data as a socket client.
     */
    @SuppressWarnings("DM_EXIT")
    public void runInClientMode()
    {
    	Socket socket = null;
		
		try
		{
			do
        	{
        		try
        		{
        			socket = new Socket(hostname,port);
        			break;
        		}
        		catch(final ConnectException ce)
        		{
        			trace.error("Can't connect to port number " + port + ".  Retrying in " + RETRY_TIME + " ms...\n");

                    // Sleep a while; ignore interrupts
        			SleepUtilities.checkedSleep(RETRY_TIME);
        		} 
        	} while(true);
		}
		catch(final IOException ioe)
		{
			trace.error("Error connecting to server: " + ioe.getMessage());
			System.exit(1);
		}

		try
		{
			if(is == null)
			{
				is = new DataInputStream(socket.getInputStream());
			}
			
			if(os == null)
			{
				os = new FileOutputStream(new File(filename));
			}
			
			trace.info("Connected to server.");
			trace.info("Writing data to file " + filename + " with a buffer size of " + bufferSize + " bytes.");
		}
		catch(final IOException ioe)
		{
			trace.error("Could not make client/server connection: " + ioe.getMessage());
			System.exit(1);
		}

		try
		{
			doReceive();

		    socket.close();
		}
		catch(final IOException e)
		{
			trace.error("Error transferring data: " + e.getMessage());
		}
    }
    
    /**
     * Perform receipt of data as a socket server.
     */
    @SuppressWarnings("DM_EXIT")
    public void runInServerMode()
    {
    	ServerSocket serverSocket = null;
		Socket clientSocket = null;
		try {
    		try
    		{
    			serverSocket = new ServerSocket(port);
    		}
    		catch(final IOException ioe)
    		{
    			trace.error("Error creating server socket: " + ioe.getMessage());
    			System.exit(1);
    		}
    
    		do
    		{
    			try
    			{
    				trace.info("\nWaiting for a connection on port " + port + "...");
    				clientSocket = serverSocket.accept();
    				
    				if(is == null)
    				{
    					is = new DataInputStream(clientSocket.getInputStream());
    				}
    				
    				if(os == null)
    				{
    					os = new FileOutputStream(new File(filename));
    				}
    				
    				trace.info("Connection succeeded.");
    				trace.info("Writing input data to file " + filename + " with a buffer size of " + bufferSize + " bytes.");
    			}
    			catch(final IOException ioe)
    			{
    				trace.error("Could not make client/server connection: " + ioe.getMessage());
    				System.exit(1);
    			}
    
    			try
    			{
    				doReceive();
    
    				if(clientSocket != null)
    				{
    					clientSocket.close();
    				}
    			}
    			catch(final IOException e)
    			{
    				trace.error("Error transferring data: " + e.getMessage());
    			}
    		}while(true);
		}
		finally {
		    if (null != serverSocket) {
		        try {
                    serverSocket.close();
                }
                catch (final IOException e) {
                    // don't care
                }
		    }
		}
    }
    
    /**
     * Performs the actual socket reads.
     */
    public void doReceive()
    {
    	long startTime = 0;
		long endTime = 0;
		int totalBytes = 0;

		try
		{
			final byte[] buffer = new byte[bufferSize];

		
			int readCount = is.read(buffer);
			while(readCount != -1)
			{
			    if (startTime == 0) {
    			    startTime = System.currentTimeMillis();
    	            if(verbose)
    	            {
    	                trace.info("Start time = " + startTime + "(ms since epoch 1970)");
    	            }
			    }
				if(readCount > 0)
				{
					totalBytes += readCount;
					if(verbose)
					{
						trace.info("Read " + readCount + " bytes from socket. Total: " + totalBytes);
					}
					os.write(buffer, 0, readCount);
				}
				readCount = is.read(buffer);
			}
			endTime = System.currentTimeMillis();
			if(verbose)
			{
				trace.info("End time = " + endTime + "(ms since epoch 1970)");
			}

			is.close();
			is = null;
			os.flush();
			if(!continuousAppend)
			{
			    os.close();
			    os = null;
			}
		}
		catch(final IOException e)
		{
			trace.error("Error reading information from socket and writing to file: " + e.getMessage());
		}
		
		double bitRate = 0.0;
		if (endTime - startTime != 0) {
		   
		    bitRate = (totalBytes*8.0)/((endTime - startTime)/1000.0);
		}
		trace.info("Received " + totalBytes + " bytes at a bitrate of " + bitRate + " bits/second.");
		trace.info("These bytes were written to the file " + filename);
    }
    
    /**
     * Main application entry point.
     * 
     * @param args command line arguments.
     */
	public static void main(final String[] args)
	{
		GetFromSocketApp app = null;
		
		try {
			app = new GetFromSocketApp();
			app.configure(app.createOptions().parseCommandLine(args, true));
		} catch (final ParseException pe) {
		    TraceManager.getTracer(Loggers.DEFAULT).error(pe.getMessage());
    		System.exit(1);
		}
		
		app.run();
	}

	/**
	 * Creates an Options object containing possible command line arguments/options.
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

        bufferSizeOption = new UnsignedIntOption("b", BUFFER_LONG, "size",
	            "The size in bytes of the output buffer. (Default = 800 bytes)", false);
		bufferSizeOption.setDefaultValue(UnsignedInteger.valueOf(BUFF_SIZE));
		options.addOption(bufferSizeOption);
		
        hostOption = new StringOption("o", HOSTNAME_LONG, "host",
	            "The hostname of the machine sending data (Default = \"localhost\").  Only applicable in client mode.", false);
	    hostOption.setDefaultValue(HostPortUtility.LOCALHOST);
	    options.addOption(hostOption);
	    
        verboseOption = new FlagOption(null, VERBOSE_LONG,
                                       "Display verbose output (Note: this may slow down sending rate)", false);
	    options.addOption(verboseOption);
	    
        clientOption = new FlagOption("c", CLIENT_LONG, "Run the application in client mode (Default = Server Mode)",
                                      false);
	    options.addOption(clientOption);
	    
        appendOption = new FlagOption("a", APPEND_LONG,
	            "Keep appending data to the end of the same output file.  By default the output file is overwritten every time a new transmission occurs.",
	            false);
	    options.addOption(appendOption);
		
	    return options;
	}

	/**
	 * Displays the usage for this application
	 * 
	 */
	@Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

	    final PrintWriter pw = new PrintWriter(System.out);

	    pw.println("Usage: " + ApplicationConfiguration.getApplicationName() + " [options] <port> <output_filename>\n");

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

    boolean isContinuousAppend() {
        return continuousAppend;
    }

    int getBufferSize() {
        return bufferSize;
    }

    String getHostname() {
        return hostname;
    }
}
