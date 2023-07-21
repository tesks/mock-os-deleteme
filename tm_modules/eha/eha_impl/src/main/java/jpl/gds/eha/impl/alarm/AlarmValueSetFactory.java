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
package jpl.gds.eha.impl.alarm;

import jpl.gds.eha.api.channel.IAlarmValueSet;
import jpl.gds.eha.api.channel.alarm.IAlarmValueSetFactory;
/**
 * AlarmValueSetFactory is used to create IAlarmValueSet objects for use in
 * channel processing and derivation.
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * <p>
 * An IAlarmValueSet object is the multi-mission representation of a collection of 
 * alarm instances. Alarm instances are attached to channel values through the IInternalChannelValue
 * interface. In order to isolate the mission adaptation from changes in the
 * multi-mission core, IAlarmValueSet objects should always be created with
 * this factory. 
 * <p>
 * This class contains only static methods. There is one method per alarm type.
 * Once the IAlarmValueSet object is returned by this factory, its additional
 * members can be set through the methods in the IAlarmValueSet interface.
 * 
 *
 * @see IAlarmValueSet
 */
public class AlarmValueSetFactory implements IAlarmValueSetFactory {
    
    /**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.channel.alarm.IAlarmValueSetFactory#create()
	 */
    @Override
    public IAlarmValueSet create() {
        return new AlarmValueSet();
    }
}
