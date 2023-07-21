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
package jpl.gds.tcapp.app.gui.icmd.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;

import gov.nasa.jpl.icmd.schema.AggregationMethod;
import gov.nasa.jpl.icmd.schema.BitRateAndModIndexType;
import gov.nasa.jpl.icmd.schema.ExecutionMethod;
import gov.nasa.jpl.icmd.schema.ExecutionMode;
import gov.nasa.jpl.icmd.schema.ExecutionState;
import gov.nasa.jpl.icmd.schema.ListPreparationStateEnum;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.mission.StationMapper;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.tc.api.icmd.ICpdDmsBroadcastStatusMessagesPoller;
import jpl.gds.tc.api.icmd.ICpdDmsBroadcastStatusMessagesSubscriber;
import jpl.gds.tc.api.icmd.datastructures.CpdConfiguration;
import jpl.gds.tc.api.icmd.datastructures.CpdConnectionStatus;
import jpl.gds.tc.api.icmd.datastructures.CpdDmsBroadcastStatusMessages;

/**
 * This class is a data model that represents the CPD server configuration
 *
 * @since AMPCS R3
 */

/*
 * CpdParametersModel no longer uses the
 * CpdParametersPoller to keep its parameters up to date. By implementing
 * ICpdDmsBroadcastStatusMessagesSubscriber, it's able to receive parameter
 * updates directly. So class CpdParameterPoller and interface
 * ICpdParameterPoller are now gone.
 */
public class CpdParametersModel implements IContentProvider,
		ICpdDmsBroadcastStatusMessagesSubscriber {

	/** Types of CPD parameters that viewers can subscribe to */
	public enum CpdParameterType {
		CONFIGURATION, CONNECTION, BITRATE, ROLE
	}

	/** The object representing CPD's configuration */
	private CpdConfiguration cpdConfig;

	/** The object representing CPD's connection status */
	private CpdConnectionStatus connStatus;

	/** The object representing the set bit rate on CPD */
	private BitRateAndModIndexType bitrateModindex;

	/** A map of all the viewers by subscription */
	private final Map<CpdParameterType, List<Viewer>> viewers;

	/** Flag indicating if data is stale */
	private boolean isStale;

	/** The singleton instance */
//	private static CpdParametersModel instance;

	/*
	 * Added station mapper so log messages for
	 * station connection can include numeric ID.
	 */
	private final StationMapper stationMapper;
	
	private final Shell parentShell;

	private final IMessagePublicationBus bus;

    private final IStatusMessageFactory statusMsgFactory;

    private final Tracer                              trace;

//	/**
//	 * Get the singleton instance
//	 *
//	 * @return the singleton instance
//	 */
//	public synchronized static CpdParametersModel getInstance() {
//		if (instance == null) {
//			instance = new CpdParametersModel();
//		}
//
//		return instance;
//	}

	public CpdParametersModel(final ApplicationContext appContext, final Shell parentShell) {
		
		this.stationMapper = appContext.getBean(MissionProperties.class).getStationMapper();
	    this.parentShell = parentShell;
		this.viewers = new HashMap<CpdParameterType, List<Viewer>>();
		this.isStale = false;
		
		this.bus = appContext.getBean(IMessagePublicationBus.class);
		this.statusMsgFactory = appContext.getBean(IStatusMessageFactory.class);
        this.trace = TraceManager.getTracer(appContext, Loggers.CPD_UPLINK);

		/*
		 * Since we're not using pollers
		 * anymore, can remove much of the logic here that was setting up
		 * periodic poller and updater. We now register this object as a handler
		 * of incoming CPD long poll messages.
		 */
		appContext.getBean(ICpdDmsBroadcastStatusMessagesPoller.class).subscribe(this);

		// Notify the subscription model that this model is now subscribed
		CpdDmsBroadcastStatusMessagesSubscriptionModel.INSTANCE.subscribed(appContext.getBean(ICpdDmsBroadcastStatusMessagesPoller.class), 
				CpdParametersModel.class);
	}

	/**
	 * Updates all listeners of this model.
	 */
	public void refresh() {

		/*
		 * No longer need to force polls to update parameters.
		 */

		this.updateAllListeners();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface
	 * .viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(final Viewer viewer, final Object oldInput,
			final Object newInput) {
		CpdParameterType[] toRemove = null;
		CpdParameterType[] toAdd = null;

		if (oldInput instanceof CpdParameterType) {
			toRemove = new CpdParameterType[] { (CpdParameterType) oldInput };
		} else if (oldInput instanceof CpdParameterType[]) {
			toRemove = (CpdParameterType[]) oldInput;
		}

		if (newInput instanceof CpdParameterType) {
			toAdd = new CpdParameterType[] { (CpdParameterType) newInput };
		} else if (newInput instanceof CpdParameterType[]) {
			toAdd = (CpdParameterType[]) newInput;
		}

		if (toRemove != null) {
			for (final CpdParameterType oldParam : toRemove) {
				if (this.viewers.containsKey(oldParam)) {
					this.viewers.get(oldParam).remove(viewer);
				}
			}
		}

		if (toAdd != null) {
			for (final CpdParameterType newParam : toAdd) {
				if (!this.viewers.containsKey(newParam)) {
					this.viewers.put(newParam, new LinkedList<Viewer>());
				}

				this.viewers.get(newParam).add(viewer);
			}
		}

		viewer.refresh();
	}

	/**
	 * Retrieve the CPD server's execution mode.
	 *
	 * @return the ExecutionMode of the CPD server
	 */
	public ExecutionMode getExecutionMode() {
		if (this.cpdConfig == null) {
			return null;
		}

		return this.cpdConfig.getExecutionMode();
	}

	/**
	 * Retrieve the CPD server's execution state.
	 *
	 * @return the ExecutionMode of the CPD server
	 */
	public ExecutionState getExecutionState() {
		if (this.cpdConfig == null) {
			return null;
		}

		return this.cpdConfig.getExecutionState();
	}

	/**
	 * Retrieve the CPD server's execution mode.
	 *
	 * @return the ExecutionMode of the CPD server
	 */
	public ExecutionMethod getExecutionMethod() {
		if (this.cpdConfig == null) {
			return null;
		}

		return this.cpdConfig.getExecutionMethod();
	}

	/**
	 * Get the CPD server's aggregation metohd
	 *
	 * @return the AggregationMethod of the CPD server
	 */
	public AggregationMethod getAggregationMethod() {
		if (this.cpdConfig == null) {
			return null;
		}

		return this.cpdConfig.getAggregationMethod();
	}

	/**
	 * Retrieve the CPD server's connection status.
	 *
	 * @return the connection status of the CPD server
	 */
	public CpdConnectionStatus getConnectionStatus() {
		if (this.connStatus == null) {
			return null;
		}

		return this.connStatus;
	}

	/**
	 * Retrieve the connected station ID, or the connection status if connection
	 * status is not CONNECTED
	 *
	 * @return the connected station ID, or the connection status if connection
	 *         status is not CONNECTED
	 */
	public String getConnectedStation() {
		if (this.connStatus == null) {
			return "";
		}

		if (this.connStatus.isConnected()) {
			return this.connStatus.getConnectedStationId();
		} else {
			return this.connStatus.toString();
		}
	}

	/**
	 * Retrieve the bit rate that the station will be radiating at
	 *
	 * @return the bit rate that the station will be radiating at
	 */
	public double getBitRate() {
		if (this.bitrateModindex == null) {
			return -1;
		}

		return this.bitrateModindex.getBITRATE();
	}

	/**
	 * Retrieve the list preparation state
	 *
	 * @return the list preparation state
	 */
	public ListPreparationStateEnum getPreparationState() {
		if (this.cpdConfig == null) {
			return null;
		}

		return this.cpdConfig.getPreparationState();
	}


	private boolean isEqual(final BitRateAndModIndexType object1,
			final BitRateAndModIndexType object2) {
		if ((object1 == null) || (object2 == null)) {
			return false;
		}

		if ((int) object1.getBITRATE() != (int) object2.getBITRATE()) {
			return false;
		}

		if ((int) object1.getMODINDEX() != (int) object2.getMODINDEX()) {
			return false;
		}

		return true;
	}

	private void updateAllListeners() {
		if (!parentShell.isDisposed()) {
			SWTUtilities.safeAsyncExec(parentShell
					.getDisplay(), "CpdParametersModel", new Runnable() {
				@Override
				public void run() {
					for (final List<Viewer> vs : CpdParametersModel.this.viewers
							.values()) {
						for (final Viewer v : vs) {
							v.refresh();
						}
					}
				}
			});
		}
	}

	/**
	 * Indicates if the data in this model is stale
	 *
	 * @return true if data is stale, false otherwise
	 */
	public boolean isStale() {
		return this.isStale;
	}

	/*
	 * Replaces what Updater class used to
	 * do. The Updater class ran as a Runnable, taking from a BlockingQueue to
	 * process each parameter polled. Now, the parameters get pushed to this
	 * class directly, as they get updated at the CPD server, so we just need to
	 * handle it.
	 */
	/**
	 * {@inheritDoc}
	 *
	 * @see jpl.gds.tcapp.icmd.ICpdDmsBroadcastStatusMessagesSubscriber#handleNewMessages(jpl.gds.tcapp.icmd.datastructures.CpdDmsBroadcastStatusMessages)
	 */
	@Override
	public void handleNewMessages(final CpdDmsBroadcastStatusMessages msgs) {

		final boolean prevIsStale = this.isStale;

		// parameters are not stale anymore
		this.isStale = false;

		if (prevIsStale) {
			// update everyone
			updateAllListeners();
		}

		final Set<Viewer> viewersToUpdate = new HashSet<Viewer>();
		final CpdConnectionStatus newConnStatus = msgs.getConnectionState();

		if ((newConnStatus != null) && !newConnStatus.equals(this.connStatus)) {

			/*
			 * Log change to connection status, if we
			 * had a previous status.
			 */
			if (connStatus != null) {
				logConnectionChange(connStatus, newConnStatus);
			}

			this.connStatus = newConnStatus;

			final List<Viewer> connStatusViewers = this.viewers
					.get(CpdParameterType.CONNECTION);

			if (connStatusViewers != null) {
				viewersToUpdate.addAll(connStatusViewers);
			}

		}

		final CpdConfiguration newCpdConfig = msgs.getConfiguration();

		if ((newCpdConfig != null) && !newCpdConfig.equals(this.cpdConfig)) {

			/*
			 * Log change to configuration, if we had
			 * a previous configuration.
			 */
			if (cpdConfig != null) {
				final StringBuilder message = new StringBuilder("Execution Mode="
						+ newCpdConfig.getExecutionMode().toString());
				message.append(", Execution State="
						+ newCpdConfig.getExecutionState().toString());
				message.append(", Execution Method="
						+ newCpdConfig.getExecutionMethod().toString());
				message.append(", Aggregation Method="
						+ newCpdConfig.getAggregationMethod().toString());
				message.append(", Preparation State="
						+ newCpdConfig.getPreparationState().toString());

				/*
				 * Added Station Name to all messages
				 */
				final String stationName = connStatus.getConnectedStationId();
				if (null != stationName) {
					message.append(", " + stationName + " ("
							+ stationMapper.getStationId(stationName) + ")");
				}

				final IPublishableLogMessage lm = statusMsgFactory.createPublishableLogMessage(
                        TraceSeverity.INFO, "CPD configuration changed to "
								+ message.toString(), LogMessageType.UPLINK);
				bus.publish(lm);
                trace.log(lm);
				
			}

			this.cpdConfig = newCpdConfig;

			final List<Viewer> cpdConfigViewers = this.viewers
					.get(CpdParameterType.CONFIGURATION);

			if (cpdConfigViewers != null) {
				viewersToUpdate.addAll(cpdConfigViewers);
			}
		}

		final BitRateAndModIndexType newBitRate = msgs.getBitRateModIndex();

		if ((newBitRate != null) && !isEqual(this.bitrateModindex, newBitRate)) {
			this.bitrateModindex = newBitRate;
			final List<Viewer> bitRateViewers = this.viewers
					.get(CpdParameterType.BITRATE);

			if (bitRateViewers != null) {
				viewersToUpdate.addAll(bitRateViewers);
			}
		}

		if ((parentShell != null)
				&& !parentShell.isDisposed()) {
			SWTUtilities.safeAsyncExec(parentShell
					.getDisplay(), "CpdParametersModel", new Runnable() {
				@Override
				public void run() {
					for (final Viewer v : viewersToUpdate) {
						if (!v.getControl().isDisposed()) {
							v.refresh();
						}
					}
				}
			});
		}

	}

	/*
	 * Added Station Name to all messages
	 *
	 * This method used to exist in the
	 * Updater inner class, but moved it out because the Updater class is now
	 * gone.
	 */
	private void logConnectionChange(final CpdConnectionStatus connStatus,
			final CpdConnectionStatus newConnStatus) {
		String stationName = connStatus.getConnectedStationId();
		if (newConnStatus.isPending()) {
			final String message = stationName == null ? "CPD is pending connection to station"
					: "CPD is pending connection to station " + stationName
							+ " (" + stationMapper.getStationId(stationName)
							+ ")";
			final IPublishableLogMessage lm = statusMsgFactory.createPublishableLogMessage(TraceSeverity.INFO,
                    message, LogMessageType.UPLINK);
			bus.publish(lm);
            trace.log(lm);

		} else if (newConnStatus.isTerminating()) {
			final String message = stationName == null ? "CPD is terminating connection from station"
					: "CPD is terminating connection from station "
							+ stationName + " ("
							+ stationMapper.getStationId(stationName) + ")";
			final IPublishableLogMessage lm = statusMsgFactory.createPublishableLogMessage(TraceSeverity.INFO,
                    message, LogMessageType.UPLINK);
			bus.publish(lm);
            trace.log(lm);

		} else if (!newConnStatus.isConnected()) {
			final String message = stationName == null ? "CPD has disconnected from station"
					: "CPD has disconnected from station " + stationName + " ("
							+ stationMapper.getStationId(stationName) + ")";
			final IPublishableLogMessage lm = statusMsgFactory.createPublishableLogMessage(TraceSeverity.INFO,
                    message, LogMessageType.UPLINK);
			bus.publish(lm);
            trace.log(lm);

		} else if (newConnStatus.isConnected()) {
			stationName = newConnStatus.getConnectedStationId();
			final String message = stationName == null ? "CPD has connected to station"
					: "CPD has connected to station " + stationName + " ("
							+ stationMapper.getStationId(stationName) + ")";
			final IPublishableLogMessage lm = statusMsgFactory.createPublishableLogMessage(TraceSeverity.INFO,
                    message, LogMessageType.UPLINK);
			bus.publish(lm);
            trace.log(lm);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see jpl.gds.tcapp.icmd.ICpdDmsBroadcastStatusMessagesSubscriber#dataNowStale()
	 */
	@Override
	public void dataNowStale() {
		this.isStale = true;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}


}
