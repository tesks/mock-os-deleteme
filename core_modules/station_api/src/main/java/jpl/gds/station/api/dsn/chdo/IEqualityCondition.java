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
package jpl.gds.station.api.dsn.chdo;

/**
 * An interface to be implemented by classes that perform CHDO Property
 * equality checks.
 * 
 *
 * @since R8
 */
public interface IEqualityCondition {

    /**
     * Retrieves the flag indicating whether this is an "equals" or "not equals"
     * condition.
     * 
     * @return true if equals condition, false if not equals
     */
    public boolean getEqualityValue();

    /**
     * Sets the flag indicating whether this is an "equals" or "not equals"
     * condition.
     * 
     * @param equalityValue true if equals condition, false if not equals
     */
    public void setEqualityValue(boolean equalityValue);

    /**
     * Retrieves the name of this condition.
     * 
     * @return the name
     */
    public String getName();

    /**
     * Sets the name of this condition.
     *
     * @param name The name to set.
     */
    public void setName(String name);

    /**
     * Returns the comparison value for this condition.
     * 
     * @return comparison value
     */
    public String getValue();

    /**
     * Sets the comparison value for this condition.
     *
     * @param value The value to set.
     */
    public void setValue(String value);

}