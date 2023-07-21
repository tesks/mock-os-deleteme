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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.performance.BinaryStatePerformanceData;
import jpl.gds.shared.performance.IPerformanceData;
import jpl.gds.shared.performance.IPerformanceProvider;
import jpl.gds.shared.performance.PerformanceSummaryPublisher;
import jpl.gds.shared.sfdu.SfduVersionException;
import jpl.gds.station.api.IStationMessageFactory;

/**
 * AbstractSfduStreamProcessor handles all of the functionality that is
 * duplicated between SfduTfStreamProcessor and SfduPktStreamProcessor. This
 * primarily encompasses the heartbeat logic.
 * 
 *
 */
public abstract class AbstractSfduStreamProcessor extends AbstractRawStreamProcessor{
	
	/**
	 * Data input stream data is pulled from
	 */
	protected DataInputStream dis;
	
	/**
	 * Putting back together the heartbeat logic. Expected TDS Heartbeat period in milliseconds
	 */
	protected int		sfduHeartbeatPeriodMs			= 0;

	/**
	 * Boolean flag indicating whether TDS Heartbeat Reconnection logic is
	 * enabled.
	 */
	protected boolean	sfduHeartbeatReconnectEnabled	= false;

	/**
	 * Timer for keeping track of how long it's been without data.
	 *
	 * MPCS-10617 - 5/31/19 - Name the timer thread
	 */
	private final Timer	heartbeatTimer					= new Timer("SFDU Stream Processor");

	/**
	 * Timer Task used repeatedly to implement behavior when data TDS data flow/heartbeat times-out
	 */
	private TimerTask	heartBeatTask					= null;
	
	/* package */ BinaryStatePerformanceData heartbeatPerformance = null;
	private final PerformanceReporter perfReporter;
	
	/** List of received SFDU labels that cannot be handled, but have been reported to the user **/
	protected List<String> unhandledSFDULabels = new ArrayList<>();

    /** Station message factory */
    protected IStationMessageFactory stationMessageFactory;
	
	/**
	 * Initialize the AbstractSfduStreamProcessor. Pull configuration values and start up heartbeat performance reporting
	 * @param serviceContext the current application context
	 */
	protected AbstractSfduStreamProcessor(final ApplicationContext serviceContext) {
		super(serviceContext);
		this.sfduHeartbeatPeriodMs = rawConfig.getSfduHeartbeatPeriod();
		this.sfduHeartbeatReconnectEnabled = rawConfig.isSfduHeartbeatReconnectEnabled();
		logger.debug("Class: " + getClass().getSimpleName() + " Heartbeat Period: " + sfduHeartbeatPeriodMs + ", Heartbeat Reconnect: " + (sfduHeartbeatReconnectEnabled ? "EN" : "DIS") + "ABLED.");
		
		heartbeatPerformance = new BinaryStatePerformanceData(serviceContext.getBean(PerformanceProperties.class),"TDS Heartbeat", true);
		perfReporter = new PerformanceReporter();

	    this.stationMessageFactory = serviceContext.getBean(IStationMessageFactory.class);
		
	}
	
	/**
	 * Start the heart-beat timer task.
	 */
	protected synchronized void startHeartbeatTimer() {
		if (isStopped())
			return;
		if (isPaused())
			return;
		if (null == heartBeatTask) {
			heartBeatTask = new TimerTask() {
				@Override
                public void run() {
					heartbeatPerformance.setGood(false);
					logger.info(">>> SFDU Heartbeat has not been heard for one period, currently configured as " + (sfduHeartbeatPeriodMs / 1000.00) + " seconds.");
					if (sfduHeartbeatReconnectEnabled) {
						stopHeartbeatTimer();
						if (dis != null) {
							try {
								dis.close();
							}
							catch (final IOException ioe) {
								// ignore
							}
						}
						dis = null;
					}
				}
			};
			heartbeatTimer.schedule(heartBeatTask, sfduHeartbeatPeriodMs, sfduHeartbeatPeriodMs);
		}
	}
	
	/**
	 * Stop the heart-beat timer task.
	 */
	protected void stopHeartbeatTimer() {
		if (null != heartBeatTask) {
			heartBeatTask.cancel();
			heartBeatTask = null;
		}
	}

	/**
	 * Stops processing of raw input. Also sends a stop message. Does nothing if input is already stopped.
	 */
	@Override
    public synchronized void stop() {
		stopHeartbeatTimer();
		this.perfReporter.deregister();
		super.stop();
		
		// triviski 1/2018 - Caused NSYT not to exit.  Stopping the timer.
		heartbeatTimer.cancel();
	}

	/**
	 * Pauses processing of input. All input received while pause is on will be thrown away. Also sends a pause message. Does
	 * nothing if input is already paused.
	 */
	@Override
    public synchronized void pause() {
		stopHeartbeatTimer();
		super.pause();
	}

	/**
	 * Resumes processing of paused input. Also sends a resume message. Does nothing if input was not paused.
	 */
	@Override
    public synchronized void resume() {
		startHeartbeatTimer();
		super.resume();
	}
	
	/**
	 * Set the heartbeat period
	 * 
	 * @param heartbeatPeriodMs
	 *            the current acceptable time between heartbeat messages, in
	 *            milliseconds
	 * 06/05/2012 - MPCS-3831 Putting back together the heartbeat logic.
	 * 
	 * 11/10/2011 - MPCS-2900 Setter for the SFDU Heart-beat
	 */
	protected void setHeartbeatPeriodMs(final int heartbeatPeriodMs) {
		sfduHeartbeatPeriodMs = heartbeatPeriodMs;
	}

	/**
	 * Get the heartbeat period
	 * 
	 * @return the current acceptable time between heartbeat messages, in
	 *         milliseconds
	 * 11/10/2011 - MPCS-2900 Getter for the SFDU Heart-beat
	 */
	protected int getHeartbeatPeriodMs() {
		return sfduHeartbeatPeriodMs;
	}
	
	/**
	 * Reports to the user if a label has been received, but cannot be handled.
	 * 
	 * @param sfduExc
	 *            the SfduVersionException to examine
	 * MPCS-7751 - 02/28/17 - updated, check for heartbeat has
	 *          been moved.
	 */
	protected void registerUnhandledSFDUVersion(final SfduVersionException sfduExc) {

		if (!this.unhandledSFDULabels.contains(sfduExc.getSfduLabel())){
			logger.warn("Ignoring unrecognized version #" + sfduExc.getVersionNumber() + " SFDU label: " + sfduExc.getSfduLabel());
			this.unhandledSFDULabels.add(sfduExc.getSfduLabel());
		}
	}
	
	/**
	 * Report the performance data regarding the TDS heartbeat
	 * 
	 * @return a list containing the heartbeat performance
	 */
	public List<IPerformanceData> getHeartbeatPerformanceData(){
		if (heartbeatPerformance != null){
			return Arrays.asList((IPerformanceData)heartbeatPerformance);
		}
		
		return Collections.emptyList();
	}
	
	
	/**
	 * Performance reporter class, to allow TDS heartbeat performance data to be
	 * reported
	 * 
	 *
	 */
	public class PerformanceReporter implements IPerformanceProvider {
		private static final String THIS_PROVIDER = "TDS Heartbeat";
		
		/**
		 * Constructor. Register this reporter with the
		 * SessionBasedPerformanceSummaryPublisher
		 */
		public PerformanceReporter(){
			appContext.getBean(PerformanceSummaryPublisher.class).registerProvider(this);
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see jpl.gds.shared.performance.IPerformanceProvider#getProviderName()
		 */
		@Override
		public String getProviderName() {
			return THIS_PROVIDER;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see jpl.gds.shared.performance.IPerformanceProvider#getPerformanceData()
		 */
		@Override
		public List<IPerformanceData> getPerformanceData() {
			return new LinkedList<>(AbstractSfduStreamProcessor.this.getHeartbeatPerformanceData());
		}
		
		/**
		 * De-registers with the performance summary publisher for performance data requests.
		 */
		public void deregister() {
			appContext.getBean(PerformanceSummaryPublisher.class).deregisterProvider(this);
		}
		
		
	}
	
	
	
}