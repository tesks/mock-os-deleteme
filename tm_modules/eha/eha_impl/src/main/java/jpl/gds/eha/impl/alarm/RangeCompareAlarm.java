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
/*
 * Title: RangeCompareAlarm.java
 * 
 * Author: dan
 * Created: Jan 12, 2006
 * 
 */
package jpl.gds.eha.impl.alarm;

import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;

/**
 * This class is used as a base class for alarm classes that use upper and lower
 * limit values.
 * 
 */
public abstract class RangeCompareAlarm extends AbstractAlarm
{

	/**
	 * The upper bound of the range.
	 */
	protected double upperLimit;
	/**
	 * The lower bound of the range.
	 */
	protected double lowerLimit; 

	    /**
     * Constructor.
     * 
     * @param def
     *            the alarm definition object for this alarm
     * @param timeStrategy
     *            the current time comparison strategy
     */
	public RangeCompareAlarm(final IAlarmDefinition def, final TimeComparisonStrategyContextFlag timeStrategy) {
		super(def, timeStrategy);
	}

	/**
	 * Retrieves the lower bound on the range.
	 * 
	 * @return the lower limit
	 */
	public double getLowerLimit()
	{
		return lowerLimit;
	}

	/**
	 * Sets the lower bound on the range.
	 * 
	 * @param lowerLimit The limit to set.
	 */
	public void setLowerLimit(final double lowerLimit)
	{
		this.lowerLimit = lowerLimit;
	}

	/**
	 * Retrieves the upper bound on the range.
	 * 
	 * @return the upper limit
	 */
	public double getUpperLimit()
	{
		return upperLimit;
	}

	/**
	 * Sets the upper bound on the range.
	 * 
	 * @param upperLimit The upperLimit to set.
	 */
	public void setUpperLimit(final double upperLimit)
	{
		this.upperLimit = upperLimit;
	}

	/**
	 * Appends the range limits to the given StringBuilder for construction of a unique key.
	 * @param sb the StringBuilder to append to
	 * @return a reference to the same StringBuilder
	 */
	protected StringBuilder appendRangeToUniqueKey(final StringBuilder sb) {
		sb.append(Double.toString(upperLimit));
		sb.append("_");
		sb.append(Double.toString(lowerLimit));
		return sb;
	}
}
