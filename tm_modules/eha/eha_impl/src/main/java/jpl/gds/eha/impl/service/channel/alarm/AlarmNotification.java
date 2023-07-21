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


/**
 * Holds the status of an alarm notification.
 *
 */
public class AlarmNotification
{
    private String name;
    private final List<AbstractAlarmTrigger> triggers;

    private final boolean _isRealtime;
    private final boolean _isRecorded;


    /**
     * Constructor.
     *
     * @param isRealtime True if to be used for R/T
     * @param isRecorded True if to be used for recorded
     */
    public AlarmNotification(final boolean isRealtime,
                             final boolean isRecorded)
    {
        this("", isRealtime, isRecorded);
    }


    /**
     * Constructor.
     *
     * @param name       Name of this notification
     * @param isRealtime True if to be used for R/T
     * @param isRecorded True if to be used for recorded
     */
    public AlarmNotification(final String  name,
                             final boolean isRealtime,
                             final boolean isRecorded)
    {
        this.name = name;
        this.triggers = new ArrayList<AbstractAlarmTrigger>(16);

        _isRealtime = isRealtime;
        _isRecorded = isRecorded;
    }


    /**
     * Add a trigger to this notification.
     *
     * @param trigger New alarm trigger
     */
    public void addAlarmTrigger(final AbstractAlarmTrigger trigger)
    {
        if(this.triggers.contains(trigger) == false)
        {
            this.triggers.add(trigger);
        }
    }


    /**
     * Get list of triggers.
     *
     * @return Triggers
     */
    public List<AbstractAlarmTrigger> getTriggers()
    {
        return(this.triggers);
    }


    /**
     * Get name.
     *
     * @return Name
     */
    public String getName()
    {
        return(this.name);
    }


    /**
     * Get R/T state.
     *
     * @return True if configured for R/T
     */
    public boolean isRealtime()
    {
        return _isRealtime;
    }


    /**
     * Get recorded state.
     *
     * @return True if configured for recorded
     */
    public boolean isRecorded()
    {
        return _isRecorded;
    }
}
