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
package jpl.gds.monitor.guiapp.channel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.widgets.Display;
import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.client.channel.IChannelUtilityDictionaryManager;
import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.message.api.external.IMessageServiceListener;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.monitor.message.InternalMonitorMessageType;
import jpl.gds.monitor.perspective.view.channel.LocalLadMessage;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelLad;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelSample;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;

/**
 * ChannelMessageDistributor receives all channel messages, converts the channel samples
 * in the messages to ChannelSample objects, and distributes these objects to all 
 * registered listeners, which include channel list views, alarm views, channel plot views,
 * and fixed views.
 * 
 * The primary entry points to this class are the onMessage() and sendLad() methods. Both are
 * invoked by the MonitorMessageController class.
 * 
 * When channel samples are received they are posted to a blocking send queue, which prevents 
 * messages from coming in faster than they can be processed. They are pulled off the
 * send queue by an SWT runnable that then notifies this object to send the data to registered
 * listeners.
 *
 * This is a Singleton class. There is only one instance of ChannelMessageDistributor 
 * per application; referring code should use the getInstance() method when 
 * interacting with it.
 *
 */
public final class ChannelMessageDistributor implements IMessageServiceListener, MessageSubscriber
{
    private final Tracer                                   trace;

    private final Tracer                                   jmsDebugLogger;


	/**
	 * The send queue of channel samples and the Runnable that will process them
	 */

	private final ArrayBlockingQueue<MonitorChannelSample> sendQueue = new ArrayBlockingQueue<MonitorChannelSample>(15000);
	private final SendDataRunnable sendData;

	/**
	 * Lists of listeners for channel samples.
	 */
	private final Map<String,List<ChannelSampleListener>> channelListenerMap = new HashMap<String,List<ChannelSampleListener>>();
	private final List<ChannelSampleListener> ladListenerMap = new ArrayList<ChannelSampleListener>();
	private final List<ChannelSampleListener> alarmListenerMap = new ArrayList<ChannelSampleListener>();
	private final Map<String, List<ChannelSampleListener>> plotListenerMap =  new HashMap<String,List<ChannelSampleListener>>();

	private long receiveCount = 0;
	private boolean isShutdown = false;
	private final MonitorChannelLad lad;
	private final AtomicBoolean sendLock = new AtomicBoolean(false);
	private final ApplicationContext appContext;
	private final IChannelUtilityDictionaryManager dictManager;
	private final IMessagePublicationBus bus;
    private final IExternalMessageUtility externalMessageUtil;
	

	/**
	 * Constructor. 
	 * @param appContext the current application context
	 *
	 */
	public ChannelMessageDistributor(final ApplicationContext appContext) {
		this.appContext = appContext;
        trace = TraceManager.getTracer(appContext, Loggers.UTIL);
        jmsDebugLogger = TraceManager.getTracer(appContext, Loggers.JMS);
		this.lad = this.appContext.getBean(MonitorChannelLad.class);
		this.bus = appContext.getBean(IMessagePublicationBus.class);
		this.dictManager = this.appContext.getBean(IChannelUtilityDictionaryManager.class);
		this.externalMessageUtil = this.appContext.getBean(IExternalMessageUtility.class);
		sendData = new SendDataRunnable();
	}

	/**
	 * This method is part of a proper singleton class. It prevents using
	 * cloning as a hack around the singleton.
	 * 
	 * @return It never returns
	 * @throws CloneNotSupportedException
	 *             This function always throws this exception
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	/**
	 * Gets the total message receive count up to the current time.
	 * 
	 * @return the receive count
	 */
	public long getReceiveCount()
	{
		return receiveCount;
	}

	/**
	 * Adds a global LAD listener for a specific channel ID. These listeners
	 * are delivered data points that come from the global lad.
	 * 
	 * @param channelId the ID of the channel the listener is interested in
	 * @param listener the ChannelSampleListener to add
	 */
	public void addGlobalLadListener(final String channelId, final ChannelSampleListener listener) {

		synchronized(channelListenerMap) {
			List<ChannelSampleListener> listenerList = channelListenerMap.get(channelId);

			if (listenerList == null) {
				listenerList = new LinkedList<ChannelSampleListener>();
				channelListenerMap.put(channelId, listenerList);
			}
			listenerList.add(listener);
		}
	}

	/**
	 * Removes a global LAD listener for a specific channel ID.
	 *
	 * @param channelId the ID of the channel the listener is registered for
	 * @param listener the ChannelSampleListener to remove
	 */
	public void removeGlobalLadListener(final String channelId, final ChannelSampleListener listener) {

		synchronized (channelListenerMap) {
			final List<ChannelSampleListener> listenerList = channelListenerMap.get(channelId);
			if (listenerList == null)
			{
				return;
			}
			listenerList.remove(listener);
		}
	}

	/**
	 * Adds a global LAD listener. These listeners receive all channels from the global LAD, 
	 * regardless of channel ID.
	 * 
	 * @param listener the ChannelSampleListener to add
	 */
	public void addGlobalLadListener(final ChannelSampleListener listener)
	{
		synchronized (ladListenerMap) {
			if (!ladListenerMap.contains(listener)) {
				ladListenerMap.add(listener);
			}
		}
	}

	/**
	 * Removes a global lad listener
	 * 
	 * @param listener the ChannelSampleListener to remove
	 */
	public void removeGlobalLadListener(final ChannelSampleListener listener) {
		synchronized (ladListenerMap) {
			ladListenerMap.remove(listener);
		}
	}

	/**
	 * Adds a global alarm listener. These listeners receive all channels in alarm, 
	 * regardless of channel ID.
	 * 
	 * @param listener the ChannelSampleListener to add
	 */
	public void addGlobalAlarmListener(final ChannelSampleListener listener)
	{
		synchronized (alarmListenerMap) {
			if (!alarmListenerMap.contains(listener)) {
				alarmListenerMap.add(listener);
			}
		}
	}

	/**
	 * Removes a global alarm listener.
	 * 
	 * @param listener the ChannelSampleListener to remove
	 */
	public void removeGlobalAlarmListener(final ChannelSampleListener listener) {
		synchronized (alarmListenerMap) {
			alarmListenerMap.remove(listener);
		}
	}

	/**
	 * Adds a plot listener to the distributor for a specific channel ID. Plot listeners are
	 * delivered every channel sample that matches the listener's specified channel ID.
	 * 
	 * @param channelId the ID of the channel the listener is interested in
	 * @param listener the ChannelDataListener to add
	 */
	public void addPlotListener(final String channelId, final ChannelSampleListener listener) {

		synchronized (plotListenerMap) {
			List<ChannelSampleListener> listenerList = plotListenerMap.get(channelId);
			if (listenerList == null) {
				listenerList = new ArrayList<ChannelSampleListener>();
				plotListenerMap.put(channelId, listenerList);
			}
			listenerList.add(listener);
		}
	}

	/**
	 * Remove a plot listener for a specific channel ID. The listener will no longer be
	 * delivered data points.
	 *
	 * @param channelId the ID of the channel the listener is registered for
	 * @param listener the ChannelDataListener to remove
	 */
	public void removePlotListener(final String channelId, final ChannelSampleListener listener) {

		synchronized (plotListenerMap) {
			final List<ChannelSampleListener> listenerList = plotListenerMap.get(channelId);
			if (listenerList == null)
			{
				return;
			}
			listenerList.remove(listener);
		}
	}

    /**
     * Receives incoming channel messages from the message service, parses them,
     * and posts the data samples to the send queue.
     * 
     * @param m
     *            the received message
     */	
	@SuppressWarnings("PMD.CollapsibleIfStatements")
	@Override
	public synchronized void onMessage(final IExternalMessage m)
	{
		// Do nothing if we are shutting down
		if (isShutdown) {
			return;
		}

	
		// Keep track of message receive count
		receiveCount++;

		try {

			// Parse the incoming message
			IMessage[] newMessageList = null;
			if (trace.isEnabledFor(TraceSeverity.TRACE)) {			    
			    trace.trace(externalMessageUtil.getContentDump(m));
			}
			newMessageList = externalMessageUtil.instantiateMessages(m);

			// Convert the resulting channel samples to ChannelSample objects and
			// post them to the send queue
			for (int index = 0; index < newMessageList.length; index++) {
				postChannelMessage((IAlarmedChannelValueMessage)newMessageList[index]);
			}

			// If there are currently alarm or plot listeners, post an SWT Runnable to
			// pull the items off the send queue in the user interface thread. The lock
			// prevents multiple Runnables from stacking up in SWT
			if ((!ladListenerMap.isEmpty() || !plotListenerMap.isEmpty() || !alarmListenerMap.isEmpty()) && !sendQueue.isEmpty()) {
				triggerSend();
			}

		} catch (final Exception e) {
			e.printStackTrace();
			trace.error("Channel message distributor could not process channel message! " + e.toString());
			try {
                trace.error(externalMessageUtil.getContentDump(m));
            } catch (final MessageServiceException e1) {
                e1.printStackTrace();
            }
		}
	}

	/**
	 * Triggers a send of the current queue, which pushes queued channel values
	 * to the listeners that want them.
	 */
	public void triggerSend() {
		if (sendLock.compareAndSet(false, true)) {
			final Display display = Display.getDefault();
			display.asyncExec(sendData);
		}
	}

	/**
	 * Translates the given channel message to a ChannelSample object and posts
	 * it to the send queue.
	 * 
	 * @param message AlarmedChannelValueMessage containing the channel data to post

	 */
	public void postChannelMessage(final IAlarmedChannelValueMessage message) {

		// Convert the incoming channel value to a ChannelSample object
		final MonitorChannelSample data = MonitorChannelSample.create(dictManager, message.getChannelValue());
		if (data == null) {
			return;
		}

		// Channel messages do not some from the LAD so this sample did not either
		data.setFromLad(false);

		// Add the new value to the LAD
		lad.addNewValue(data);

		// We only need to put the item on the send queue if we have plot or alarm listeners for it.
		// Channel listeners will get all the latest samples from the LAD

		if (!plotListenerMap.isEmpty() || !alarmListenerMap.isEmpty()) {

			final String chanId = data.getChanId();

			final List<ChannelSampleListener> listenerList = plotListenerMap.get(chanId);
			if (listenerList != null || !alarmListenerMap.isEmpty()) {
				postToSendQueue(data);	
			}
		}
	}




	/**
	 * Posts a channel sample to the blocking send queue. This method will not return
	 * until the sample is posted.
	 * 
	 * @param data ChannelSample the channel sample to post
	 */
	void postToSendQueue(final MonitorChannelSample data) {
		// Visibility of this method set to default because
		// MonitorChannelLad now needs to access it to distribute LAD messages.

		boolean done = false;
		long checkCounter = 0;

		while (!done) {


			try {
				done = sendQueue.offer(data, 10, TimeUnit.MILLISECONDS);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			} 

			// We didn't succeed
			if (!done) {

				try {
					// Do we seem stuck here? Log it
					checkCounter++;
					if (checkCounter > 5000) {
						trace.warn("It appears that channel message distributor is seriously stalled");
					}

					// Submit a Runnable to pull data off the send queue
					triggerSend();

					// Sleep for a little while before we try again
					Thread.sleep(100);
					trace.debug("ChannelMessageDistributor Sleeping done, queue size is " + sendQueue.size());

				} catch (final InterruptedException e) {
					// do nothing
				}
			}
		}
	}

	/**
	 * SendDataRunnable is a runnable class that sends all the data items on the queue to
	 * the receive method in this class. This class must always be executed on the user interface
	 * thread.
	 */
	class SendDataRunnable implements Runnable {

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "ChannelMessageDistributor.SendData.Runnable";
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			// Poll all the items off the blocking send queue and send them to listeners.
			// It doesn't matter if more is added to the queue meanwhile. The blocking queue
			// object handles that
			final int size = sendQueue.size();

			for (int index = 0; index < size && !sendQueue.isEmpty(); index++) {
				final MonitorChannelSample data = sendQueue.poll();
				receive(data);
			}

			// Reset the lock so that the next Runnable can be posted
			sendLock.set(false);
		}

		/**
		 * Receives channel samples from the incoming queue at intervals and distributes them to 
		 * registered listeners.
		 * 
		 * @param data the received channel sample
		 * 
		 */
		public void receive(final MonitorChannelSample data)
		{
			final String chanId = data.getChanId();

			// Channel and alarm listeners only get updated here if the data is from the 
			// global LAD. The rest of the time they get their data from the internal 
			// LAD. Plot listeners do not get global LAD values.

			if (data.isFromLad()) {

				// Send global LAD data samples to the channel listeners for specific channels
				synchronized (channelListenerMap) {
					final List<ChannelSampleListener> listenerList = channelListenerMap.get(chanId);
					if (listenerList != null) {

						for (final ChannelSampleListener listener: listenerList) {
							listener.receive(data);
						}
					}
				}

				// Send all global LAD values to global LAD listeners
				synchronized (ladListenerMap) {
					for (final ChannelSampleListener al: ladListenerMap) {
						al.receive(data);
					}
				}
			}

			// Now send all data to the plot listeners for specific channels. Plots must receive all
			// data samples via this route.
			synchronized (plotListenerMap) {
				final List<ChannelSampleListener> listenerList = plotListenerMap.get(chanId);
				if (listenerList != null) {

					for (final ChannelSampleListener listener: listenerList) {
						listener.receive(data);
					}
				}
			}

			// Now send all samples in alarm to the alarm listeners for specific channels. Alarm views receive 
			// channels in alarm via this mechanism or via the LAD.  
			synchronized (alarmListenerMap) {
				final boolean newValueInAlarm = !(data.getDnAlarmLevel().equals(AlarmLevel.NONE) && data.getEuAlarmLevel().equals(AlarmLevel.NONE));
				if (newValueInAlarm) {
					for (final ChannelSampleListener listener: alarmListenerMap) {
						listener.receive(data);
					}
				}
			}
		}
	}

	/**
	 * Starts the distributor. Primarily exists to start the channel simulator.
	 * Should be called after dictionaries are loaded.
	 */
	public void start() {
		bus.subscribe(InternalMonitorMessageType.MonitorLocalLad, this);
	}

	/**
	 * Shuts down the distributor.
	 */
	public void shutdown() {
		if (isShutdown) {
			return;
		}
	
		bus.unsubscribeAll( this);
		
		jmsDebugLogger.debug("ChannelMessageDistributor has a receive count of " + receiveCount + " messages at time of shutdown");
		isShutdown = true;
	}

    @Override
    public void handleMessage(final IMessage message) {
        final LocalLadMessage lm = (LocalLadMessage)message;
        for (final MonitorChannelSample s: lm.getAllSamples()) {
            postToSendQueue(s);
        }        
    }
    
}
