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
package jpl.gds.monitor.guiapp;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.widgets.Display;

import jpl.gds.monitor.config.GlobalPerspectiveParameter;
import jpl.gds.monitor.config.IMonitorConfigChangeListener;
import jpl.gds.monitor.config.MonitorConfigValues;
import jpl.gds.monitor.guiapp.common.ChannelFlushListener;
import jpl.gds.monitor.guiapp.common.ChartUpdateListener;
import jpl.gds.monitor.guiapp.common.GeneralFlushListener;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.SWTUtilities;

/**
 * 
 *         MonitorTimers manages all the central timers in the monitor. These
 *         include the display flush timers, which fire periodically in order to
 *         update various types of displays. Most of the timer intervals are
 *         established from the GDS configuration file or the user perspective. <br>
 *         Listeners register with this object to receive notification when a
 *         timer fires. <br>
 *         Display timers are fired on the user interface thread, so that the
 *         listening displays do not have to post their own jobs to that thread.
 */
public final class MonitorTimers implements IMonitorConfigChangeListener {

	// Configuration property defaults
	private static final long DEFAULT_GENERAL_UPDATE_INTERVAL = 3000;	
	
	private static final Tracer tracer = TraceManager.getDefaultTracer();


	private Timer displayTimer;
	private Timer channelTimer;
	private Timer alarmTimer;
	private Timer plotTimer;
	
	/**
	 * Interval at which the general display flush timer will fire.
	 */
	private static final long flushInterval = DEFAULT_GENERAL_UPDATE_INTERVAL;
	
	/**
	 * Interval at which the channel display flush timer will fire.
	 */
	private long channelUpdateInterval;
	
	/**
	 * Interval at which the alarm display flush timer will fire.
	 */
	private long alarmUpdateInterval;
	
	/**
	 * Interval at which the plot update timer will fire.
	 */
	private long plotUpdateInterval;

	// Access to these listener lists must be synchronized
	private final List<GeneralFlushListener> displayFlushListeners = new ArrayList<>();
	private final List<ChannelFlushListener> channelFlushListeners = new ArrayList<>();
	private final List<ChannelFlushListener> alarmFlushListeners = new ArrayList<>();
	private final List<ChartUpdateListener> plotUpdateListeners = new ArrayList<>();

	
	/**
	 * Creates an instance of MonitorTimers. 
	 * @param configVals the current monitor configuration values object
	 * 
	 */
	public MonitorTimers(final MonitorConfigValues configVals) {
		channelUpdateInterval = (Long)configVals.getValue(GlobalPerspectiveParameter.CHANNEL_LIST_UPDATE_RATE) * 1000;
        alarmUpdateInterval = (Long)configVals.getValue(GlobalPerspectiveParameter.CHANNEL_ALARM_UPDATE_RATE) * 1000;
        plotUpdateInterval = (Long)configVals.getValue(GlobalPerspectiveParameter.CHANNEL_PLOT_UPDATE_RATE) * 1000;
	}
	
	/**
	 * Starts all timers.
	 */
	public void start() {
		startDisplayTimer();
		startChannelTimer();
		startAlarmTimer();
		startPlotTimer();
	}
	
	/**
	 * Stops all timers.
	 */
	public void stop() {
		if (alarmTimer != null) {
			alarmTimer.cancel();
			alarmTimer.purge();
			alarmTimer = null;
		}
		if (channelTimer != null) {
			channelTimer.cancel();
			channelTimer.purge();
			channelTimer = null;
		}
		if (displayTimer != null) {
			displayTimer.cancel();
			displayTimer.purge();
			displayTimer = null;
		}
		if (plotTimer != null) {
			plotTimer.cancel();
			plotTimer.purge();
			plotTimer = null;
		}		
	}

	/**
	 * Starts the general display flush timer. Any existing timer is canceled. This timer fires at
	 * a rate defined in the GDS configuration file.
	 */
	@SuppressWarnings("unchecked")
	private void startDisplayTimer() {
		displayTimer = new Timer();
		displayTimer.scheduleAtFixedRate(new TimerTask() {

		    @Override
		    public void run() {

                tracer.trace("Monitor Timers: general timer fired");
		        ArrayList<GeneralFlushListener> cloneListeners = null;
		        synchronized (displayFlushListeners) {
					cloneListeners = (ArrayList<GeneralFlushListener>) ((ArrayList<GeneralFlushListener>) displayFlushListeners)
					.clone();
				}
				for (final GeneralFlushListener l : cloneListeners) {
					l.flushTimerFired();
				}

			}
		}, flushInterval, flushInterval);
	}


	/**
	 * Starts the channel flush timer. Any existing timer is canceled. This timer fires at
	 * the rate defined by the channel update interval, which is a global perspective parameter.
	 */
	@SuppressWarnings("unchecked")
	private void startChannelTimer() {
		
		if (channelTimer != null) {
			channelTimer.cancel();
			channelTimer = null;
		}
		
		channelTimer = new Timer();
		channelTimer.scheduleAtFixedRate(new TimerTask() {

			/**
			 * @see java.util.TimerTask#run()
			 */
			@Override
			public void run() {
				
				// Execute the notification to the listeners on the user interface thread			
				SWTUtilities.safeAsyncExec(Display.getDefault(), "Monitor Channel Timer", 
						new Runnable() {
					/**
					 * @see java.lang.Object#toString()
					 */
					@Override
					public String toString() {
						return "MonitorTimers.startChannelTimer.Runnable";
					}
					
					/**
					 * @see java.lang.Runnable#run()
					 */
					@Override
					public void run() {
                                                   tracer.trace("Monitor Timers: channel timer fired");
					    ArrayList<ChannelFlushListener> cloneListeners = null;
					    synchronized (channelFlushListeners) {
							cloneListeners = (ArrayList<ChannelFlushListener>) ((ArrayList<ChannelFlushListener>) channelFlushListeners)
							.clone();
						}
						for (final ChannelFlushListener l : cloneListeners) {
							l.flushTimerFired();
						}
					}
				});
			}
		}, channelUpdateInterval, channelUpdateInterval);
	}

	/**
	 * Starts the alarm flush timer. Any existing timer is canceled. This timer fires at the rate
	 * defined by the alarm update interval, which is a global perspective parameter.
	 */
	@SuppressWarnings("unchecked")
	private void startAlarmTimer() {
		
		if (alarmTimer != null) {
			alarmTimer.cancel();
			alarmTimer = null;
		}
		
		alarmTimer = new Timer();
		alarmTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {

				// Execute the update notification on the user interface thread
				SWTUtilities.safeAsyncExec(Display.getDefault(), "Monitor Alarm Timer", 
						new Runnable() {
					@Override
					public String toString() {
						return "MonitorTimers.startAlarmTimer.Runnable";
					}
					@Override
					public void run() {

                                                   tracer.trace("Monitor Timers: alarm timer fired");
					    ArrayList<ChannelFlushListener> cloneListeners = null;
					    synchronized (alarmFlushListeners) {
					        cloneListeners = (ArrayList<ChannelFlushListener>) ((ArrayList<ChannelFlushListener>) alarmFlushListeners)
							.clone();
						}
						for (final ChannelFlushListener l: cloneListeners) {
							l.flushTimerFired();
						}
					}
				});
			}
		}, alarmUpdateInterval, alarmUpdateInterval);
	}
	
	/**
	 * Starts the plot update timer. Any existing timer is canceled. This timer fires at the rate
	 * defined by the plot update interval, which is a global perspective parameter.
	 */
	@SuppressWarnings("unchecked")
	private void startPlotTimer() {
		
		if (plotTimer != null) {
			plotTimer.cancel();
			plotTimer = null;
		}
		
		plotTimer = new Timer();
		plotTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {

				// Execute the notification on the user interface thread
				SWTUtilities.safeAsyncExec(Display.getDefault(), "Monitor Plot Timer", 
						new Runnable() {
					@Override
					public String toString() {
						return "MonitorTimers.startplotTimer.Runnable";
					}
					@Override
					public void run() {

					    tracer.trace("Monitor Timers: plot timer fired");				    
					    ArrayList<ChartUpdateListener> cloneListeners = null;
					    synchronized (plotUpdateListeners) {
							cloneListeners = (ArrayList<ChartUpdateListener>) ((ArrayList<ChartUpdateListener>) plotUpdateListeners)
							.clone();
						}
						for (final ChartUpdateListener l: cloneListeners) {
							l.update();
						}
					}
				});
			}
		}, plotUpdateInterval, plotUpdateInterval);
	}


	/**
	 * Adds a general display flush listener.
	 * 
	 * @param fl the GeneralFlushListener to add
	 */
	public void addGeneralFlushListener(final GeneralFlushListener fl) {
		synchronized (displayFlushListeners) {
			displayFlushListeners.add(fl);
		}
	}
	
	/**
	 * Removes a general display flush listener.
	 * 
	 * @param fl the GeneralFlushListener to remove
	 */
	public void removeGeneralFlushListener(final GeneralFlushListener fl) {
		synchronized (displayFlushListeners) {
			displayFlushListeners.remove(fl);
		}
	}
	
	/**
	 * Removes a channel display flush listener.
	 * 
	 * @param fl the ChannelFlushListener to remove
	 */
	public void removeChannelFlushListener(final ChannelFlushListener fl) {
		synchronized (channelFlushListeners) {
			channelFlushListeners.remove(fl);
		}
	}

	/**
	 * Adds a channel display flush listener.
	 * 
	 * @param fl the ChannelFlushListener to add
	 */
	public void addChannelFlushListener(final ChannelFlushListener fl) {
		synchronized (channelFlushListeners) {
			channelFlushListeners.add(fl);
		}
	}
	
	/**
	 * Removes an alarm display flush listener.
	 * 
	 * @param fl the ChannelFlushListener to remove
	 */
	public void removeAlarmFlushListener(final ChannelFlushListener fl) {
		synchronized (alarmFlushListeners) {
			alarmFlushListeners.remove(fl);
		}
	}

	/**
	 * Adds an alarm display flush listener.
	 * 
	 * @param fl the ChannelFlushListener to add
	 */
	public void addAlarmFlushListener(final ChannelFlushListener fl) {
		synchronized (alarmFlushListeners) {
			alarmFlushListeners.add(fl);
		}
	}
	
	/**
     * Adds an plot update listener to the list of listeners to be triggered on each update pass.
     * 
     * @param listener the listener to add
     */
    public synchronized void addPlotListener(final ChartUpdateListener listener) {
        synchronized (plotUpdateListeners) {
        	plotUpdateListeners.add(listener);
        }
    }
    
    /**
     * Removes a ChartUpdateListener from the list of listeners to be triggered on each update pass.
     * 
     * @param listener the listener to add
     */
    public synchronized void removePlotListener(final ChartUpdateListener listener) {
    	synchronized (plotUpdateListeners) {
    		plotUpdateListeners.remove(listener);
    	}
    }

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.config.IMonitorConfigChangeListener#globalConfigurationChange(jpl.gds.monitor.config.GlobalPerspectiveParameter, java.lang.Object)
	 */
	@Override
    public void globalConfigurationChange(final GlobalPerspectiveParameter param,
			final Object newValue) {
		
		// A global parameter controlling the timers may have changed.
		// Make necessary updates to timers.
		if (param.equals(GlobalPerspectiveParameter.CHANNEL_LIST_UPDATE_RATE)) {
			channelUpdateInterval = ((Long)newValue).longValue() * 1000;
			startChannelTimer();
		} else if (param.equals(GlobalPerspectiveParameter.CHANNEL_ALARM_UPDATE_RATE)) {
			alarmUpdateInterval = ((Long)newValue).longValue() * 1000;
			startAlarmTimer();
		}  else if (param.equals(GlobalPerspectiveParameter.CHANNEL_PLOT_UPDATE_RATE)) {
			plotUpdateInterval = ((Long)newValue).longValue() * 1000;
			startPlotTimer();
		}
	}
}
