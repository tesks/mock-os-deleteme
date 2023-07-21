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
package jpl.gds.shared.cli.options;

/**
 * A generic interface to be implemented by command line option parsers that
 * perform range validation by verifying that the option value is in a specified
 * range. Data type T is determined by the type of the command line option.
 * 
 *
 * @param <T>
 *            Data type is determined by the type of the command line option(s)
 *            this parser supports.
 * 
 */
public interface IRangeOptionParser<T extends Object> {

    /**
     * Sets the maximum value (inclusive) for the command line option. Setting
     * a null value implies there is no upper bound for the option; it will be
     * checked only to ensure it fits within its data type.
     * 
     * @param highestValue
     *            maximum value; data type must match the type of the option
     */
    public void setMaxValue(T highestValue);

    /**
     * Sets the minimum value (inclusive) for the command line option. Setting
     * a null value implies there is no upper bound for the option; it will be
     * checked only to ensure it fits within its data type.
     * 
     * @param lowestValue
     *            minimum value; data type must match the type of the option
     */
    public void setMinValue(T lowestValue);

    /**
     * Gets the maximum value (inclusive) for the command line option.
     * 
     * @return maximum value; data type matches the type of the option
     */
    public T getMaxValue();

    /**
     * Gets the minimum value (inclusive) for the command line option.
     * 
     * @return minimum value; data type matches the type of the option
     */
    public T getMinValue();

}
