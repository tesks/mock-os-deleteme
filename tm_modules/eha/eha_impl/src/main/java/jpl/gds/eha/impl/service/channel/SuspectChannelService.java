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

import java.util.Timer;
import java.util.TimerTask;

import org.springframework.context.ApplicationContext;

import jpl.gds.eha.api.config.EhaProperties;
import jpl.gds.eha.api.message.IEhaMessageFactory;
import jpl.gds.eha.api.message.ISuspectChannelTable;
import jpl.gds.eha.api.message.ISuspectChannelsMessage;
import jpl.gds.eha.api.service.channel.ISuspectChannelService;
import jpl.gds.shared.message.IMessagePublicationBus;

/**
 * SuspectChannelService manages a table information about channels with
 * suspicious DN/EU values and alarm calculations. It is a DownlinkService
 * implementation that will periodically send out SuspectChannelMessages, so
 * that clients can mark the channels as suspect on displays. The publication
 * interval is configurable.
 * 
 */
public class SuspectChannelService implements ISuspectChannelService {

	private Timer broadcastTimer;
	private final IMessagePublicationBus messageBus;
	private final ISuspectChannelTable suspectTable;

	private final IEhaMessageFactory ehaMessageFactory;

    private final ApplicationContext     appContext;
	
    /**
     * Constructor.
     * 
     * @param context
     *            the current application context
     */
	public SuspectChannelService(final ApplicationContext context) {
	    this.messageBus = context.getBean(IMessagePublicationBus.class);
		this.ehaMessageFactory = context.getBean(IEhaMessageFactory.class);
		this.suspectTable = ehaMessageFactory.createSuspectChannelsMessage().getSuspectTable();
        this.appContext = context;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.service.channel.ISuspectChannelService#getTable()
     */
	@Override
    public ISuspectChannelTable getTable() {
	    return this.suspectTable;
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.interfaces.IService#startService()
	 */
	@Override
	public boolean startService() {
	    
        if (!suspectTable.init(appContext)) {
	        return false;
	    }
	    
        final long time = appContext.getBean(EhaProperties.class).getSuspectChannelPublishInterval();
		if (broadcastTimer != null) {
			broadcastTimer.cancel();
			broadcastTimer = null;
		}

		/* Name the timer thread. */
		broadcastTimer = new Timer("Suspect Channel Timer");
		broadcastTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run()
			{
				sendUpdate();
			}
		}, time * 1000, time *1000);
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.interfaces.IService#stopService()
	 */
	@Override
	public void stopService() {
		if (broadcastTimer != null) {
			broadcastTimer.cancel();
			broadcastTimer = null;
		}
	}


	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.service.channel.ISuspectChannelService#sendUpdate()
     */
	@Override
    public void sendUpdate() {
		final ISuspectChannelsMessage scm = ehaMessageFactory.createSuspectChannelsMessage(this.suspectTable);
		messageBus.publish(scm);
	}

	
}

