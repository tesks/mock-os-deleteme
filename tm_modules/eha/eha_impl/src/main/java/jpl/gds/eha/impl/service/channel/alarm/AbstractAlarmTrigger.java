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
package jpl.gds.eha.impl.service.channel.alarm;

import java.util.ArrayList;
import java.util.List;

import jpl.gds.common.notify.INotifier;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.eha.api.channel.IAlarmValueSet;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;
import jpl.gds.eha.impl.alarm.AlarmValueSet;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * 
 * Class AbstractAlarmTrigger
 *
 */
public abstract class AbstractAlarmTrigger
{
    /** shared instance of logger */
	protected static final Tracer trace = TraceManager.getDefaultTracer();


    /** alarm notification state */
	protected AlarmNotificationState state;
    /** list of notifiers */
	protected List<INotifier> notifiers;

    /**
     * Constructor.
     */
	public AbstractAlarmTrigger()
	{
		state = null;
		notifiers = new ArrayList<INotifier>(16);
	}

	/**
	 * @return Returns the state.
	 */
	public AlarmNotificationState getState()
	{
		return state;
	}

	/**
	 * Sets the state
	 *
	 * @param state The state to set.
	 */
	public void setState(final AlarmNotificationState state)
	{
		if(state == null)
		{
			throw new IllegalArgumentException("Null alarm notification state.");
		}

		this.state = state;
	}

    /**
     * Determines if the given channel value matches this trigger.
     * 
     * @param value
     *            channel value
     * @return true if a match, false if not
     */
	public abstract boolean matches(final IServiceChannelValue value);

    /**
     * Invokes the notification method for all notifiers, supplying the given
     * channel value message.
     * 
     * @param iAlarmedChannelValueMessage
     *            the channel value messages
     */
	public void doNotify(final IAlarmedChannelValueMessage iAlarmedChannelValueMessage)
	{
		for(final INotifier notifier : notifiers)
		{
			notifier.notify(iAlarmedChannelValueMessage);
		}
	}

    /**
     * Adds a notifier to the list of notifiers.
     * 
     * @param notifier
     *            the notifier to add
     */
	public void addNotifier(final INotifier notifier)
	{
		if(notifiers.contains(notifier) == false)
		{
			notifiers.add(notifier);
		}
	}

    /**
     * Gets the list of notifiers for this trigger.
     * 
     * @return list of notifiers
     */
	public List<INotifier> getNotifiers()
	{
		return(notifiers);
	}

    /**
     * Indicates if this trigger is activated, given two channel values.
     * 
     * @param newValue
     *            the newer of the channel values
     * @param oldValue
     *            the older of the channel values
     * @return true if this trigger is activated by the change between the
     *         channel values
     */
	public boolean isTriggered(final IServiceChannelValue newValue, final IServiceChannelValue oldValue)
	{
		if(matches(newValue) == false)
		{
			return(false);
		}

		IAlarmValueSet oldAlarms = oldValue != null ? oldValue.getAlarms() : new AlarmValueSet();
		if(oldAlarms == null)
		{
			oldAlarms = new AlarmValueSet();
		}

		IAlarmValueSet newAlarms = newValue != null ? newValue.getAlarms() : new AlarmValueSet();
		if(newAlarms == null)
		{
			newAlarms = new AlarmValueSet();
		}

		//Currently we notify on every value for in YELLOW and in RED, though we may want to change to have
		//something like YELLOW_ALL and YELLOW_FIRST to distinguish between being notified on every alarmed
		//value versus only being notified when you first go into alarm
		switch(state)
		{
		case YELLOW:

			return(newAlarms.inAlarm(AlarmLevel.YELLOW));

		case YELLOW_FIRST:

			return(oldAlarms.inAlarm(AlarmLevel.YELLOW) == false && newAlarms.inAlarm(AlarmLevel.YELLOW) == true);

		case RED:

			return(newAlarms.inAlarm(AlarmLevel.RED));

		case RED_FIRST:

			return(oldAlarms.inAlarm(AlarmLevel.RED) == false && newAlarms.inAlarm(AlarmLevel.RED) == true);

		case CHANGE_ANY:

			return(oldAlarms.getWorstLevel() != newAlarms.getWorstLevel());

		case CHANGE_SET:

			return(oldAlarms.inAlarm() == false && newAlarms.inAlarm() == true);

		case CHANGE_CLEAR:

			return(oldAlarms.inAlarm() == true && newAlarms.inAlarm() == false);
		}

		return(false);
	}
}
