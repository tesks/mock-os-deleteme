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
package jpl.gds.monitor.guiapp.common;


/**
 * Performs checks to determine if messages have been dropped and stores the 
 * number of dropped messages.
 *
 */
public class DroppedMessageCounter {
	
	/** Number of dropped messages */
	private int droppedCounter;
	
	/** Number of the latest received message */
	private int lastMessageReceivedCount;
	
	/**
	 * Constructor: Initialized the number of dropped messages (if any) and 
	 * the number of the last received message.
	 * 
	 * @param messageCounter number that this counter should start at
	 */
	public DroppedMessageCounter(int messageCounter) {
		droppedCounter = 0;
		this.lastMessageReceivedCount = messageCounter;
	}
	
	/**
	 * Sets the number of dropped messages.
	 * 
	 * @param droppedCounter number of dropped messages
	 */
	public void setCount(int droppedCounter) {
		this.droppedCounter = droppedCounter;
	}
	
	/**
	 * Gets the number of dropped messages since the last reset.
	 * 
	 * @return number of dropped messages
	 */
	public int getCount() {
		return this.droppedCounter;
	}
	
	/**
	 * Increments the dropped message counter if messages have been dropped 
	 * since the last time this calculation was performed.
	 * 
	 * @param messageNumber number of the last received message
	 */
	public void calculateDroppedMessages(int messageNumber) {
		
		int difference = messageNumber - this.lastMessageReceivedCount;
		
		if(difference >= 2) {
			droppedCounter += (difference - 1);
		}
		
		this.lastMessageReceivedCount = messageNumber;
	}
	
	/**
	 * Sets the number of dropped messages to zero.
	 */
	public void resetCount() {
		this.droppedCounter = 0;
	}
}
