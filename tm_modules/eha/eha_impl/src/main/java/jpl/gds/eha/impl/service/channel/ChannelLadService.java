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
package jpl.gds.eha.impl.service.channel;

import org.springframework.context.ApplicationContext;

import jpl.gds.eha.api.channel.IChannelLad;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;
import jpl.gds.eha.api.service.channel.IChannelLadService;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;

/**
 * 
 *         This class is responsible for keeping track of the most recent
 *         value for all the channels in the downlink. This class registers 
 *         for channel messages and automatically maintains the LAD as 
 *         channels are processed. For each channel, the most recent value for 
 *         the specified time type is stored. Note that channel are separated 
 *         by their realtime/recorded classification in this table. Monitor 
 *         channels are stored in the realtime map along with their station; 
 *         all other channels store station 0.<br>
 *         This class is also a singleton.
 *
 *
 */
public class ChannelLadService implements IChannelLadService
{ 
	/** 
	 * The message subscriber for the LAD. Listens for channel messages.
	 */
	private LadMessageSubscriber subscriber;

	private final IMessagePublicationBus messageBus;
	
	private final IChannelLad ladTable;

	    /**
     * Creates an instance of ChannelLadService.
     * 
     * @param serviceContext
     *            the current application context
     */
    public ChannelLadService(final ApplicationContext serviceContext)
	{
	    this.messageBus  = serviceContext.getBean(IMessagePublicationBus.class);
	    this.ladTable = serviceContext.getBean(IChannelLad.class);
	    
	}

	/**
	 * Starts the LAD listening for channel messages.
	 */
	@Override
    public boolean startService() {
		subscriber = new LadMessageSubscriber();
		return true;
	}

	/**
	 * Stops the LAD listening for channel messages.
	 */
	@Override
    public void stopService() {
		if (subscriber != null) {
			messageBus.unsubscribeAll(subscriber);
		}
	}


	/**
	 * LadMessageSubscriber is the LADs internal message subscriber to channel message.
	 *
	 */
	private class LadMessageSubscriber implements MessageSubscriber {
		public LadMessageSubscriber() {
			messageBus.subscribe(EhaMessageType.AlarmedEhaChannel, this);
		}

		@Override
		public synchronized void handleMessage(final IMessage message) {
		    final IAlarmedChannelValueMessage eha = (IAlarmedChannelValueMessage)message;
			ladTable.addNewValue((IServiceChannelValue) eha.getChannelValue());
			
			//  1/14/14: Revert last add time to RCT instead
			// of what the Global LAD's primary time system is, since we are no
			// longer clearing the Global LAD when the chill_down LAD is
			// cleared.
            //  12/11/13: Use Global LAD primary index's time
            // system as the time system of the last add time, so we can keep
            // the Global LAD in sync with this LAD
		}
	}

}
