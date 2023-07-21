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
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.cli.ParseException;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.PortOption;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.cli.options.numeric.UnsignedIntOption;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.sfdu.SfduLabel;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.shared.util.HostPortUtility;

/**
 * An application that queries TDS.
 *
 * Fixed parsing bug that was dropping first SFDU from TDS stream.
 * Incorporated into AMPCS
 */
public class TdsQueryApp extends AbstractCommandLineApp
{
	private static final Tracer trace = TraceManager.getDefaultTracer();
	
	private static final String SOCKET_ERROR = "Error reading information from socket and writing output: ";
	
	private static final int RETRY_TIME = 5000;
    private static final int BUFF_SIZE = 800;

    private int inputPort;
    private int outputPort;
    private int bufferSize;
    private boolean verbose;
    private String pvlFilename;
    private String outFilename;
    private String hostname;
    private boolean noEcho;
    
    private OutputStream outputStream;
    private InputStream pvlInStream;
	private OutputStream tdsOutStream;
	private DataInputStream tdsInStream;
	
    private StringOption hostOption;
    private FlagOption noEchoOption;
    private PortOption portOption;
    private FlagOption verboseOption;
    private FileOption outputFileOption;
    private UnsignedIntOption bufferSizeOption;
    
    
    /**
     * Constructor.
     */
    public TdsQueryApp() 
    {
    	this.inputPort = 0;
    	this.outputPort = 0;
    	this.bufferSize = BUFF_SIZE;
    	this.verbose = false;
    	this.pvlFilename = null;
    	this.outFilename = null;
    	this.hostname = "localhost";
    	this.noEcho = false;
    	
    	this.outputStream = null;
    	this.pvlInStream = null;
    	this.tdsOutStream = null;
    	this.tdsInStream = null;
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

		this.inputPort = GDR.parse_int(leftoverArgs[0]);
		this.pvlFilename = leftoverArgs[1];

		this.bufferSize = bufferSizeOption.parseWithDefault(commandLine, false, true).intValue();
		this.hostname = hostOption.parseWithDefault(commandLine, false, true);
	    this.outFilename = outputFileOption.parse(commandLine);
	    if (this.outFilename == null) {
	        if (!commandLine.hasOption(this.portOption.getLongOpt())) {
	            throw new ParseException("You must specify either the -f option or -p option.");
	        }
	        this.outputPort = portOption.parse(commandLine).intValue();
	    }

		this.verbose = verboseOption.parse(commandLine);
		this.noEcho = noEchoOption.parse(commandLine);
    }
    
    /**
     * Main logic that runs the actual query.
     */
    @SuppressWarnings("DM_EXIT")
    public void run()
    {
    	Socket inputSocket = null;
		
		try
		{
			do
        	{
        		try
        		{
        			inputSocket = new Socket(this.hostname,this.inputPort);
        			break;
        		}
        		catch(final ConnectException ce)
        		{
        			trace.error("Can't connect to port number " + this.inputPort + ".  Retrying in " + RETRY_TIME + " ms...\n");

        			SleepUtilities.fullSleep(RETRY_TIME,
                                             trace,
                                             "TdsQueryApp.run Error waiting");
        		}
        	}while(true);
		}
		catch(final IOException ioe)
		{
			trace.error("Error connecting to server: " + ioe.getMessage());
			System.exit(1);
		}

		try
		{
			if(this.tdsInStream == null)
			{
				this.tdsInStream = new DataInputStream(inputSocket.getInputStream());
			}
			
			if(this.tdsOutStream == null)
			{
				this.tdsOutStream = new DataOutputStream(inputSocket.getOutputStream());
			}
			
			if(this.pvlInStream == null)
			{
				this.pvlInStream = new FileInputStream(new File(this.pvlFilename));
			}
			
			trace.info("Connected to server.");
			trace.info("Writing data from PVL file " + this.pvlFilename + " with a buffer size of " + bufferSize + " bytes.");
			trace.info("Writing TDS data to file " + this.outFilename + " with a buffer size of " + bufferSize + " bytes.");
		}
		catch(final IOException ioe)
		{
			trace.error("Could not make client/server connection: " + ioe.getMessage());
			System.exit(1);
		}

		try
		{
			//if there's no output file, we're writing data directly to MPCS
			//so let's first wait for MPCS to connect to us
			if(this.outFilename == null)
			{
				ServerSocket serverSocket = null;
				Socket clientSocket = null;
				
				try
				{
					serverSocket = new ServerSocket(this.outputPort);
				}
				catch(final IOException ioe)
				{
					trace.error("Error creating server socket: " + ioe.getMessage());
					System.exit(1);
				}
				
				try
				{
					trace.info("\nWaiting for a connection on port " + this.outputPort + "...");
					clientSocket = serverSocket.accept();
					
					if(this.outputStream == null)
					{
						this.outputStream = new DataOutputStream(clientSocket.getOutputStream());
					}
				}
				catch(final IOException ioe)
				{
					trace.error("Could not make client/server connection: " + ioe.getMessage());
					System.exit(1);
				}
			}
			else
			{
				if(this.outputStream == null)
				{
					this.outputStream = new FileOutputStream(new File(this.outFilename));
				}
			}
			
			doSend();
			if(this.noEcho)
			{
				doReceiveNoEcho();
			}
			else
			{
				doReceive();
			}

			if(inputSocket != null)
			{
				inputSocket.close();
			}
			
			if(this.outputStream != null)
			{
				this.outputStream.close();
			}
			
			if(this.pvlInStream != null)
			{
				this.pvlInStream.close();
			}
		}
		catch(final IOException e)
		{
			trace.error("Error transferring data: " + e.getMessage());
		}
    }
    
    private void doSend() throws IOException
    {
    	int totalBytes = 0;
        final byte[] buffer = new byte[this.bufferSize];

        final long startTime = System.currentTimeMillis();
        if(this.verbose)
        {
        	trace.info("Send start time = " + startTime + " (ms since epoch 1970)");
        }

        
        if(this.verbose)
        {
        	trace.info("Starting transfer of PVL file " + this.pvlFilename);
        }

        int readCount = this.pvlInStream.read(buffer);
        while (readCount != -1)
        {
            if (readCount > 0)
            {
                totalBytes += readCount;
                if(this.verbose)
                {
                    trace.info("Writing " + readCount + " bytes to client socket. Total: " + totalBytes);
                }
                this.tdsOutStream.write(buffer, 0, readCount);
            }
            readCount = this.pvlInStream.read(buffer);
        }
        
        this.tdsOutStream.flush();
		
        final long endTime = System.currentTimeMillis();
        if(this.verbose)
        {
        	trace.info("Send end time = " + endTime + " (ms since epoch 1970)");
        }

        final double bitRate = (totalBytes*8.0)/((endTime-startTime)/1000.0);
        trace.info("Wrote " + totalBytes + " bytes from the PVL file " + this.pvlFilename + " to socket at a bitrate of " + bitRate + " bits/second");
    }
    
    private void doReceive()
    {
    	long startTime = 0;
		long endTime = 0;
		int totalBytes = 0;

		try
		{
			final byte[] buffer = new byte[this.bufferSize];

			startTime = System.currentTimeMillis();
			if(this.verbose)
			{
				trace.info("Receive start time = " + startTime + "(ms since epoch 1970)");
			}
			int readCount = this.tdsInStream.read(buffer);
			while(readCount != -1)
			{
				if(readCount > 0)
				{
					totalBytes += readCount;
					if(this.verbose)
					{
						trace.info("Read " + readCount + " bytes from socket. Total: " + totalBytes);
					}
					this.outputStream.write(buffer, 0, readCount);
				}
				readCount = tdsInStream.read(buffer);
			}
			endTime = System.currentTimeMillis();
			if(this.verbose)
			{
				trace.info("Receive end time = " + endTime + "(ms since epoch 1970)");
			}

			this.outputStream.flush();
		}
		catch(final IOException e)
		{
			trace.error(SOCKET_ERROR + e.getMessage());
		}
			
		final double bitRate = (totalBytes*8.0)/((endTime - startTime)/1000.0);
		trace.info("Received " + totalBytes + " bytes at a bitrate of " + bitRate + " bits/second.");
    }
    
    private void doReceiveNoEcho()
    {
    	final String[] controlAuthorityIds = new String[] { "CCSD", "NJPL"};
    	final byte[] sfduLabelBuffer = new byte[SfduLabel.LABEL_LENGTH];
    	
		/* Pre-load SFDU Label Buffer */
		try {
			this.tdsInStream.readFully(sfduLabelBuffer, 0, SfduLabel.CONTROL_AUTHORITY_ID_LENGTH);
		}
		catch (final EOFException eofe) {
			return;
		}
		catch (final Exception e) {
			trace.error(SOCKET_ERROR + e.getMessage(), e);
			return;
		}
    	
    	while(true)
    	{
    		try
    		{
    			boolean inGoodSfduLabel = false;
    			boolean found = false;
	        	if(this.verbose)
				{
					trace.info("Searching for Control Authority ID...");
				}
	        	while(!found)
		        {
		        	//Look to see if the current bytes in the buffer match any of
		        	//the defined control authority IDs we're looking for
		        	//(this is done by a char-by-char comparison for each control
		        	//authority ID that we're looking for)
		        	for(int i=0; i < controlAuthorityIds.length; i++)
		        	{
		        		//loop through to see if the current control authority ID
		        		//matches what's in the buffer
		        		int j=0;
		        		for(; j < SfduLabel.CONTROL_AUTHORITY_ID_LENGTH; j++)
		        		{
		        			if(sfduLabelBuffer[j] != controlAuthorityIds[i].charAt(j))
		        			{
		        				break;
		        			}
		        		}
		        		
		        		//if the inner for loop above ran all the way through, then
		        		//it means we found a match on a control authority ID
		        		if(j == SfduLabel.CONTROL_AUTHORITY_ID_LENGTH)
		        		{
		        			if(this.verbose)
							{
								trace.info("Found Control Authority ID " + controlAuthorityIds[i]);
							}
		        			found = true;
		        			break;
		        		}
		        	}
		        	
					/*
					 * skip this step if we found a control authority ID in the current position
					 */
					if (!found) {
						if (inGoodSfduLabel) {
							this.outputStream.write(new byte[] { sfduLabelBuffer[0] });
						}
						shiftAndReadBuffer(sfduLabelBuffer, 1, SfduLabel.CONTROL_AUTHORITY_ID_LENGTH, this.tdsInStream);
		        	}
		        }
		        
		        //At this point, sfduBuffer[0] is pointing to the start of a control authority ID we recognize, so
		        //starting after that control authority ID, read in the rest of the SFDU label
		        this.tdsInStream.readFully(sfduLabelBuffer, SfduLabel.CONTROL_AUTHORITY_ID_LENGTH,SfduLabel.LABEL_LENGTH-SfduLabel.CONTROL_AUTHORITY_ID_LENGTH);
		        
		        //parse the SFDU label that we read in
		        final SfduLabel label = new SfduLabel(sfduLabelBuffer,0);
		        if(this.verbose)
				{
					trace.info("Found SFDU label " + label.toString());
				}
		        inGoodSfduLabel = label.getVersionId() == 1 || label.getVersionId() == 2;
		        if(!inGoodSfduLabel)
		        {
		        	shiftAndReadBuffer(sfduLabelBuffer, 1, SfduLabel.CONTROL_AUTHORITY_ID_LENGTH, this.tdsInStream);
		        	continue;
		        }
		        
		        final int sfduLength = label.getBlockLength().intValue();
				final byte[] sfduBuffer = new byte[SfduLabel.LABEL_LENGTH+sfduLength];
				System.arraycopy(label.getBytes(),0,sfduBuffer,0,SfduLabel.LABEL_LENGTH);
				this.tdsInStream.readFully(sfduBuffer,SfduLabel.LABEL_LENGTH,sfduLength);
				this.outputStream.write(sfduBuffer);
				if(this.verbose)
				{
					trace.info("Wrote " + sfduBuffer.length + " bytes to the output file.");
				}
				
				/* Refill the SFDU Label Buffer with bytes immediately following the SFDU just copied. */
				this.tdsInStream.readFully(sfduLabelBuffer,0,SfduLabel.CONTROL_AUTHORITY_ID_LENGTH);
    		}
    		catch(final EOFException eofe)
    		{
    			return;
    		}
    		catch(final Exception e)
    		{
    			trace.error(SOCKET_ERROR + e.getMessage(),e);
    			return;
    		}
    	}
    }

    /**
	 *
     * Shift the specified byte array left by bytes, and append the buffer with bytes read from the specified stream
     * 
     * @param buffer buffer to shift
     * @param shift number of bytes to shift/read from stream
     * @param stream the stream from which to read.
     * @throws IOException
     */
    private void shiftAndReadBuffer(final byte[] buffer, final int shift, final int width, final DataInputStream stream) throws IOException {
    	if (width > buffer.length) {
    	    throw new IOException("Specified shift width is larger than buffer size.");
    	}
    	if (shift > width) {
    	    throw new IOException("Specified shift is larger than area to be shifted.");
    	}
    	
    	/*
    	 * drop the first byte in the buffer, shift everything left by 1 spot,
    	 * read in one more byte, and then loop around to compare again
    	 */
		System.arraycopy(buffer, shift, buffer, 0, width - shift);
		stream.readFully(buffer, width - shift, shift);
    }
    
    /**
     * Main application entry point.
     * 
     * @param args command line arguments
     */
	public static void main(final String[] args) 
	{

        try {
            final TdsQueryApp app = new TdsQueryApp();
            
            final ICommandLine cmdline = app.createOptions().parseCommandLine(args,
                    true);
            app.configure(cmdline);
            app.run();
            
        } catch (final ParseException pe) {
            TraceManager.getDefaultTracer().error(pe.getMessage());

            System.exit(2);
            
        } catch (final Exception e) {
            TraceManager.getDefaultTracer().error(e.getMessage(), e);
            System.exit(2);
        }

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

	    bufferSizeOption = new UnsignedIntOption("b", "bufferSize", "size", 
                "The size in bytes of the output buffer. (Default = " + BUFF_SIZE + " bytes)", false);
        bufferSizeOption.setDefaultValue(UnsignedInteger.valueOf(BUFF_SIZE));
        options.addOption(bufferSizeOption);
        
        hostOption = new StringOption("o", "hostname", "host", 
                "The hostname of the machine sending data (Default = \"localhost\").", false);
        hostOption.setDefaultValue(HostPortUtility.LOCALHOST);
        options.addOption(hostOption);
        
        verboseOption = new FlagOption("r", "verbose", "Display verbose output (Note: this may slow down sending rate)", false);
        options.addOption(verboseOption);
        
        portOption = new PortOption("p", "outputPort", "port", "The output port to which data will be written if no output file is specified.", false);
        options.addOption(portOption);
        
        outputFileOption = new FileOption("f", "writeToFile", "file", "Write the output from TDS to the specified file.",false, false);
        options.addOption(outputFileOption);
        
	    noEchoOption = new FlagOption("n", "noEcho", 
	            "Ignore the PVL echo portion of the response from TDS. (Only applicable in conjunction with the \"writeToFile\" options).", false);
	    options.addOption(noEchoOption);
		
		return options;
	}

	/**
	 * Display the usage for this application
	 */
	@Override
    @java.lang.SuppressWarnings("unchecked")
	public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

	    final PrintWriter pw = new PrintWriter(System.out);
	   
		pw.print("Usage: ");
		pw.println(ApplicationConfiguration.getApplicationName() + " [options] <tds_port> <pvl_filename>\n");

		createOptions().getOptions().printOptions(pw);
        
        pw.flush(); 
	}

}
