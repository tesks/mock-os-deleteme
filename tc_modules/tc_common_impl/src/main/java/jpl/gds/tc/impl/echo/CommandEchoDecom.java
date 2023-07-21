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
package jpl.gds.tc.impl.echo;

import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.ICommandWriteUtility;
import jpl.gds.tc.api.cltu.ICltuParser;
import jpl.gds.tc.api.config.CltuProperties;
import jpl.gds.tc.api.echo.IEchoDecomService;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.exception.UnblockException;
import jpl.gds.tc.api.message.CommandMessageType;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * CommandEchoDecom listens for CommandEcho messages on the internal
 * bus, translates the raw command echo data back to command form,
 * and logs it to a file.
 * 
 * The default setup assumes synchronized CLTU data as input. In other words,
 * each command echo message is assumed to contain one CLTU, with start and
 * tail sequences.
 * 
 * If enableSynced(false) is invoked, this class supports syncing of CLTU data.
 * In this case the command echo message is assumed to contain just a buffer 
 * of data, each of which may contain all, part of, or no CLTU data.  The
 * data must be byte-aligned.
 *
 *
 * 09/19/17 - MPCS-9106 - Added tracer and some messages to
 *          the user, changed expectSyncedCltus to false
 *
 */
public class CommandEchoDecom implements MessageSubscriber, IEchoDecomService {
    private static final int STATE_NO_CLTU = 0;
    private static final int STATE_CLTU_START = 1;
    
    private String echoFile;
    private PrintWriter outputWriter;
    private byte[] cltuBytes;
    private int state = STATE_NO_CLTU;
    private final byte[] startSequence;
    private final byte[] tailSequence;
    private int cltuStartIndex = 0;
    private final EchoBufferManager manager;
    private int cltuCounter = 0;
    private boolean expectSyncedCltus = false;
	private final ApplicationContext appContext;
	private final ICommandWriteUtility writeUtil;
	

	private final Tracer trace;
    
    /**
     * Creates an instance of CommandEchoDecom.
     * 
     * @param appContext the ApplicationContext in which this object is being used
     * 
     * @param filename the name of the echo log file; if null, the 
     * configured default will be used, with a date-time stamp added 
     * and it will be placed into the test directory.
     */
    public CommandEchoDecom(final ApplicationContext appContext, final String filename) {
    	this.appContext = appContext;
        trace = TraceManager.getTracer(appContext, Loggers.CMD_ECHO);
    	manager = new EchoBufferManager(appContext);
        if (filename == null) {
            final String dir = appContext.getBean(IGeneralContextInformation.class).getOutputDir();
            String startTimeStr = TimeUtility.getFormatter().format(new AccurateDateTime());
            startTimeStr = startTimeStr.replaceAll("\\W", "_");
            this.echoFile = dir + File.separator + 
                "CommandEcho" + "_" + startTimeStr + ".log";
        } else {
            this.echoFile = filename;
        }
        
        trace.info("Decom output file: " + this.echoFile);

        final CltuProperties config = appContext.getBean(CltuProperties.class);
        this.writeUtil = appContext.getBean(ICommandWriteUtility.class);
        this.startSequence = config.getStartSequence();
        this.tailSequence = config.getTailSequence();
    }

    /**
     * Enables or disables synced CLTU operation.
     * @param enable true if CLTU data coming in CommandEcho messages is
     * already synced
     */
    public void enableSyncedCltus(final boolean enable) {
    	this.expectSyncedCltus = enable;
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.MessageSubscriber#handleMessage(jpl.gds.shared.message.IMessage)
     */
    @Override
	public void handleMessage(final IMessage message) {
        try {
            if (this.outputWriter == null) {
                this.outputWriter = new PrintWriter(this.echoFile);
            } 
            final CommandEchoMessage cem = (CommandEchoMessage)message;
    		this.outputWriter.write("CLTU data received at " + message.getEventTimeString() + "\n");
            if (this.expectSyncedCltus) {
                this.cltuBytes = cem.getData();
                writeCltu();
            } else {
                this.manager.consume(cem.getData());
                writeAllCltus();
            }
        } catch (final FileNotFoundException e) {
            // irrelevant whether the file already exists
        } 
    }
   

    @Override
	public boolean startService() {
        appContext.getBean(IMessagePublicationBus.class).subscribe(CommandMessageType.CommandEcho, this);
        return true;
    }
    
    /**
     * Shuts down this command echo handler and closes the command
     * echo file. Unsubscribes and ceases message processing.
     */
    @Override
	public void stopService() {
        if (this.outputWriter != null) {
            this.outputWriter.close();
        }
        appContext.getBean(IMessagePublicationBus.class).unsubscribeAll(this);
        trace.info("Decom terminated.\n");
        trace.info("Output file: " + this.echoFile + "\n");
        trace.info("Processed CLTU count: " + this.cltuCounter);
    }

    /**
     * Writes the single, complete CLTU in the CLTU data buffer to the log file.
     */
    private void writeCltu()
    {
    	try
    	{
    		final ICltu c = appContext.getBean(ICltuParser.class).parse(this.cltuBytes);
    		writeUtil.writeCltu(this.outputWriter, c,cltuCounter++);
    		trace.info("CLTU #" + cltuCounter + " - successfully received");
    	}
    	catch (final UnblockException | BlockException | CltuEndecException e)
    	{
    		writeUtil.writeBadCltu(this.outputWriter,this.cltuBytes,cltuCounter++);
    		trace.warn("CLTU #" + cltuCounter + " - error");
    	} 
    	finally
    	{
    		this.outputWriter.flush();
    	}
    }
    
    /**
     * Writes all the CLTUs that can be found by the buffer manager.
     */
    private void writeAllCltus() {
        while (findNextCltu()) {
        	writeCltu();
        }
    }
    
    // TODO - If it turns out we only get synchronized CLTUs, this can be removed.
    private boolean findNextCltu() {

    	// If we are starting without any CLTU start location, we must look for
    	// a start sequence
    	if (this.state == STATE_NO_CLTU) {
    		final int index = manager.findByteSequence(this.startSequence, 0);
    		if (index != -1) {
    			// Found start sequence.  State changes to STARTED
    			//  and we record the start index
    			this.cltuStartIndex = index;
    			this.state = STATE_CLTU_START;
    		} else {
    		    /* JFWagner - 9/12/2019 - if a start sequence was not found, it's possible that there wasn't enough
    		    data in the buffer. If that is the case, we need to load a new buffer in, so we must exit this method.
    		     */
                if(this.manager.getNumBuffers() <= 1) {
                    return false;
                }

    			if (this.manager.getNumBytes() > this.manager.getFirstBufferLen() + this.startSequence.length) {
    				this.manager.dropFirstBuffer();
    			}
    		}
    	}
    	// Now if we have found the start sequence, we need to look for the tail sequence
		if (this.state == STATE_CLTU_START) {
			final int index = manager.findByteSequence(this.tailSequence, 
					this.cltuStartIndex + this.startSequence.length);
			// No CLTU end sequence yet found.  Remain in the started state
			if (index == -1) {
				return false;
			} 
			// Found end sequence. Copy the cltu bytes into the CLTU buffer
			this.cltuBytes = manager.getBuffer(this.cltuStartIndex, index - this.cltuStartIndex + this.tailSequence.length);
			// Set the state back to "no cltu" so we will start again.
			// Drop data buffers no longer needed
			this.state = STATE_NO_CLTU;
			this.manager.dropBuffersToByte(this.cltuStartIndex + this.cltuBytes.length);
			this.cltuStartIndex = 0;
			return true;
		}
		return false;
    }
    
    /**
     * @return the number of CLTUs processed so far
     */
    public int getCltuCounter() {
    	return this.cltuCounter;
    }
}
