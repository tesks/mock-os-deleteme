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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jpl.gds.tc.api.icmd.CpdStatusChange;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.tc.api.ICpdUplinkStatus;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.RadiationListOrder;
import jpl.gds.tc.api.icmd.CpdDmsBroadcastStatusMessagesUtil;
import jpl.gds.tc.api.icmd.ICpdDmsBroadcastStatusMessagesPoller;
import jpl.gds.tc.api.icmd.ICpdObjectFactory;
import jpl.gds.tc.api.icmd.datastructures.CpdDmsBroadcastStatusMessages;

/**
 * A data model representing the state of the CPD radiation list
 *
 * @since AMPCS R3
 */
public class CpdRadiationListModel extends AbstractCpdRequestTableModel {
	/**
	 * The order with which to arrange the radiation list items. This only
	 * affects how they should be displayed, not the real radiation order
	 */
	private final RadiationListOrder radListOrder;

	/*
	 * logStale flag is no longer used to
	 * determine if CPD data staleness should be logged or not. Instead, we use
	 * a separate logger
	 * (jpl.gds.tcapp.icmd.config.IntegratedCommandConfiguration
	 * .STALE_DATA_LOGGER_NAME) to turn on and off these warnings.
	 */
	/**
	 * Default constructor
	 * 
	 * @param appContext the ApplicationContext in which this object is being used
	 */
	public CpdRadiationListModel(final ApplicationContext appContext) {
		super(appContext);
		this.radListOrder = appContext.getBean(CommandProperties.class)
				.getRadiationListOrder();

		// Notify the subscription model that this model is now subscribed
		// (subscription done by the superclass constructor)
		CpdDmsBroadcastStatusMessagesSubscriptionModel.INSTANCE
				.subscribed(appContext.getBean(ICpdDmsBroadcastStatusMessagesPoller.class), 
						CpdRadiationListModel.class);
	}

	/*
	 * Overriding this method because this
	 * model needs to filter each request status based on its
	 * INCLUDED_IN_EXE_LIST flag.
	 */
	/**
	 * {@inheritDoc}
	 *
	 * @see jpl.gds.tcapp.app.gui.icmd.model.AbstractCpdRequestTableModel#handleNewMessages(jpl.gds.tcapp.icmd.datastructures.CpdDmsBroadcastStatusMessages)
	 */
	@Override
	public synchronized void handleNewMessages(
			final CpdDmsBroadcastStatusMessages msgs) {

		final List<CpdStatusChange> statusChanges = new ArrayList<>();
		/*
		 * Changed the logic here to fit the
		 * new CPD's long-poll response data structure (which separates list and
		 * incremental updates, and we need to handle both).
		 */
		if (msgs.getRadiationList() != null || msgs.getIncrementalRequestStatusList() != null) {

			if (msgs.getRadiationList() != null) {
				this.requests = new ArrayList<ICpdUplinkStatus>(msgs.getRadiationList().size());

				for (final ICpdUplinkStatus status : msgs.getRadiationList()) {

					if ("TRUE".equalsIgnoreCase(status.getIncludedInExeList())) {
						this.requests.add(status);
						statusChanges.add(CpdStatusChange.newAddedStatus(status));
					}

				}

			}

			// Now see if there have been incremental updates since the last list.
			/*
			 * actored out the merging of
			 * incremental request status updates with a radiation list to a new
			 * utility class, because it was getting repeated by other classes as
			 * well.
			 */
			statusChanges.addAll(CpdDmsBroadcastStatusMessagesUtil
					.applyIncrementalStatusUpdates(appContext.getBean(MissionProperties.class).getStationMapper(),
					        appContext.getBean(ICpdObjectFactory.class),
					        this.requests,
							msgs.getIncrementalRequestStatusList(), true));

			if (!statusChanges.isEmpty()) {
				refreshTableViewer(statusChanges, false);
			}

		}

		this.stale = false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * jpl.gds.tcapp.icmd.AbstractCpdRequestTableModel#getElements(java.lang
	 * .Object)
	 */
	@Override
	public Object[] getElements(final Object arg0) {
		if (this.requests == null) {
			return new Object[0];
		}

		if (this.radListOrder.equals(RadiationListOrder.BOTTOM_UP)) {
			final List<ICpdUplinkStatus> reverseList = requests.subList(0,
					requests.size());
			Collections.reverse(reverseList);

			return reverseList.toArray();
		}

		return requests.toArray();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.tcapp.icmd.AbstractCpdRequestTableModel#getName()
	 */
	@Override
	protected String getName() {
		return "Radiation List";
	}

	/*
	 * poll method removed. CPD long polling
	 * mechanism will now push the list to this object.
	 */


	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

}
