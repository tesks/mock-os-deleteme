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
package jpl.gds.telem.input.impl.stream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import jpl.gds.shared.buffer.DiskBackedBufferedInputStream;
import jpl.gds.shared.buffer.DiskMappedByteBufferConfig;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.telem.input.impl.message.DiskBackedBufferedInputStreamMessage;

/**
 * LoggingDiskBackedBufferedInputStream handles interfacing a
 * DiskBackedBufferedInputStream with a message publishing service for reporting
 * statistics. By default, if a message type is not supplied, messages are
 * published as the default type.
 * 
 */
public class LoggingDiskBackedBufferedInputStream extends DiskBackedBufferedInputStream {

	private final IMessagePublicationBus msgContext;
	private Timer messageTimer;
	
	private DiskBackedBufferedInputStreamMessage msg;
	
	private final long updateMessageInterval;
	
	
	/**
	 * Constructor that takes a DiskMappedByteBufferConfig object. This is
	 * utilized to configure the DiskBackedBufferedInputStream
	 * 
	 * @param dis
	 *            the InputStream to be buffered
	 * @param configDMBB the DiskMappedByteBufferConfig object to be used for configuring the buffer
	 * @param msgContext
	 *            the message context that will be used to publish messages
	 * @param trace the trace logger to use for logging
	 * @param interval
	 *            the time, in milliseconds, between each message publication
	 * 
	 */
	public LoggingDiskBackedBufferedInputStream(final InputStream dis, final DiskMappedByteBufferConfig configDMBB, final IMessagePublicationBus msgContext, final Tracer trace, final long interval) {
		super(dis, configDMBB, trace);
		
		this.msgContext = msgContext;
		updateMessageInterval = interval;

		if(this.msgContext != null){
			startMessageTimer();
		}
		else{
			logger.warn("LoggingDiskBackedBufferedInputStream was not given a message context. Status information cannot be published.");
		}
	}

	private void startMessageTimer(){
		messageTimer = new Timer("LoggingDiskMappedByteBuffer Timer");
		messageTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				updateLogMessage();
			}
		}, updateMessageInterval, updateMessageInterval);
	}
	
	private void stopMessageTimer(){
		if(messageTimer != null){
			messageTimer.cancel();
		}
		
		messageTimer = null;
	}
	
	/**
	 * In addition to closing the DiskBackedBufferedInputStream, message
	 * publication is halted.
	 */
	@Override
	public void close() throws IOException{
		super.close();
		stopMessageTimer();
	}
	
	/**
	 * If a full shutdown is requested, message publication is halted. Otherwise
	 * message publication will continue.
	 */
	@Override
	public void close(final boolean shutdownFull) throws IOException{
		super.close(shutdownFull);
		if(shutdownFull){
			stopMessageTimer();
		}
	}

	//method used in timer to update and publish statistics of this buffer instance
	private void updateLogMessage(){
		final StringBuilder message = new StringBuilder();
		
		message.append("Buffer capacity: ");
		message.append(getBufferItemCount());
		message.append("/");
		message.append(getBufferSize());
		message.append(" (");
		message.append((int)(((getBufferItemCount()*1.0)/getBufferSize())*100));
		message.append("%)");
		
		
		message.append(" - ");
		message.append(isUseFiles() ? "Using disk files" : "Using memory only");
		
		if(isUseFiles()){

			if(!isBackupAllData()){
				message.append(" - Disk files are ");
				message.append(isDeleteFiles() ? "" : "NOT ");
				message.append("deleted after use");
			}
			else{
				message.append(" - All input data is saved in disk files");
			}
			
			message.append(" - File usage: " + getFileCount());
			if(!isBackupAllData()){
				message.append("/");
				message.append(getFileLimit());
				message.append(" (");
				message.append((int)(((getFileCount()*1.0)/getFileLimit())*100));
				message.append("%)");
			}
			
		}
		
		msg = new DiskBackedBufferedInputStreamMessage(message.toString());
		
		msgContext.publish(msg);
	}
	
}