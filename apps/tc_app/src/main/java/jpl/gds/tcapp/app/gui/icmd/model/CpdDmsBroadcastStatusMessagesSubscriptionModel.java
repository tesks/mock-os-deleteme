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

import java.util.HashSet;
import java.util.Set;

import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.tc.api.icmd.ICpdDmsBroadcastStatusMessagesPoller;
import jpl.gds.tcapp.app.gui.icmd.CpdControlPanel;

/*
 * 4/16/2015: Before, each parameter or table model
 * had its own poller which it had to start, manage, and stop. We're
 * simplifying the interface between AMPCS and CPD by centralizing the long
 * polling into a singleton instance. However, this requires that the CPD
 * long poller has to start after all the subscriptions are made by the
 * parameter or table models. We use this internal subscription model object
 * to start the CPD long poller only when all of chill_up's subscribers have
 * made the callback registrations.
 */
/**
 * This class tracks if the necessary CPD broadcast status message
 * subscribers used in {@link CpdControlPanel} have finished their
 * subscriptions to {@link ICpdDmsBroadcastStatusMessagesPoller} or not. Then
 * once all the subscriptions have been made, prompts the
 * {@link ICpdDmsBroadcastStatusMessagesPoller} to start.
 *
 * @since AMPCS R7.1
 */
public enum CpdDmsBroadcastStatusMessagesSubscriptionModel {

	INSTANCE;

    private final Set<Class<?>> requiredSubscribers;
    private final Tracer        logger = TraceManager.getTracer(Loggers.CPD_UPLINK);


	private CpdDmsBroadcastStatusMessagesSubscriptionModel() {
		requiredSubscribers = new HashSet<>(3);
		requiredSubscribers.add(CpdParametersModel.class);
		requiredSubscribers.add(CpdRadiationListModel.class);
		requiredSubscribers.add(CpdRequestPoolModel.class);
	}

	/**
	 * Notify the subscription model that a subscriber has subscribed. If
	 * all required subscribers have subscribed, then this will start the
	 * poller.
	 *
	 * @param cl class that subscribed to the poller
	 */
	public void subscribed(final ICpdDmsBroadcastStatusMessagesPoller poller, final Class<?> cl) {
		requiredSubscribers.remove(cl);

		if (requiredSubscribers.isEmpty()) {
			logger.debug(this.getClass().getSimpleName()
					+ ": All subscribers finished subscribing; starting CpdDmsBroadcastStatusMessagesPoller");
			poller.start();
		}

	}

}