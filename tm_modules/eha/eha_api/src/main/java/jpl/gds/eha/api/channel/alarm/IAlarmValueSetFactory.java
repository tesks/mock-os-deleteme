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
package jpl.gds.eha.api.channel.alarm;

import jpl.gds.eha.api.channel.IAlarmValueSet;

/**
 * An interface to be implemented by factories that create alarm value sets.
 * 
 * @since R8
 */
public interface IAlarmValueSetFactory {

	/**
	 * Creates an empty IAlarmValueSet.
	 * 
	 * @return newIAlarmValueSet
	 */
	IAlarmValueSet create();

}