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
package jpl.gds.evr.impl.service.notify;

import java.util.ArrayList;
import java.util.List;

import jpl.gds.common.notify.INotifier;
import jpl.gds.evr.api.service.IEvrNotificationTrigger;

/**
 * This class represents definitions of EVR notification. An object of this
 * class shall represent one notification definition. The definition is composed
 * of a set of triggers and a set of destinations. When a trigger is tripped,
 * the destinations will be sent notifications of the event.
 * 
 */
public class EvrNotificationDefinition {

	private final String name;
	private final List<IEvrNotificationTrigger> triggers;
	private final List<INotifier> destinations;

    private final boolean _isRealtime;
    private final boolean _isRecorded;


	/**
	 * Main constructor that takes in a name for the EVR notification.
	 * 
	 * @param name       Name of this definition
     * @param isRealtime True is notification is for real-time EVRs
     * @param isRecorded True is notification is for recorded EVRs
	 */
	public EvrNotificationDefinition(final String  name,
                                     final boolean isRealtime,
                                     final boolean isRecorded)
    {
		this.name = name;

        _isRealtime = isRealtime;
        _isRecorded = isRecorded;

		triggers = new ArrayList<IEvrNotificationTrigger>(3);
		destinations = new ArrayList<INotifier>(2);
	}


	/**
	 * Add a notification trigger to this definition's triggers list.
	 * 
	 * @param trigger
	 *            alarm trigger to add to the notification's list of triggers
	 */
	public void addAlarmTrigger(final IEvrNotificationTrigger trigger) {
		if (this.triggers.contains(trigger) == false) {
			this.triggers.add(trigger);
		}

	}

	/**
	 * Fetch method to retrieve the list of triggers of this notification
	 * object.
	 * 
	 * @return returns the alarm triggers
	 */
	public List<IEvrNotificationTrigger> getTriggers() {
		return (this.triggers);
	}

	/**
	 * Get method for retrieving the name of the notification.
	 * 
	 * @return returns the name of the notification
	 */
	public String getName() {
		return (name);
	}


    /**
     * Get method for retrieving the recorded state.
     * 
     * @return True if valid for a recorded EVR
     */
    public boolean isRecorded()
    {
        return _isRecorded;
    }


    /**
     * Get method for retrieving the real-time state.
     * 
     * @return True if valid for a real-time EVR
     */
    public boolean isRealtime()
    {
        return _isRealtime;
    }


	/**
	 * Add a new notification destination to the set.
	 * 
	 * @param destination
	 *            destination to notify when triggered
	 */
	public void addNotificationDestination(
			final INotifier destination) {

		if (destinations.contains(destination) == false) {
			destinations.add(destination);
		}

	}

	/**
	 * Returns the list of destinations defined for this notification.
	 * 
	 * @return list of destinations
	 */
	public List<INotifier> getNotificationDestinations() {
		return destinations;
	}

}
