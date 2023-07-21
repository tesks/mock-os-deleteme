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

import org.apache.commons.cli.ParseException;

/**
 * A abstract command line option parser for CommandLineOptions whose argument is a Number
 * that can be range checked. To be extended by specific range-checking parsers. All range-
 * checking option parsers are validating parsers by definition.
 * 
 *
 * @param <T> The option value class, any that extends Number
 */
public abstract class AbstractRangeCheckingOptionParser<T extends Number> extends AbstractOptionParser<T> implements
        IRangeOptionParser<T> {
    
    /**
     * The minimum value (inclusive) of the option
     */
    protected T maxValue;
    /**
     * The maximum value (inclusive) of the option
     */
    protected T minValue;

    /**
     * Constructor for a range-checking option parser with default range.
     * (Range determined by data type).
     */
    protected AbstractRangeCheckingOptionParser() {
        super(true);
        setMinValue(null);
        setMaxValue(null);
    }
    
    /**
     * Constructor for a range-checking option parser with specified range.
     * @param lowestValue The minimum value (inclusive) of the option
     * @param highestValue  The maximum value (inclusive) of the option
     */
    protected AbstractRangeCheckingOptionParser(T lowestValue, T highestValue) {
        super(true);
        setMinValue(lowestValue);
        setMaxValue(highestValue);
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.IRangeOptionParser#setMinValue(java.lang.Object)
     */
    @Override
    public void setMinValue(T lowestValue) {
        minValue = lowestValue;     
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.IRangeOptionParser#setMaxValue(java.lang.Object)
     */
    @Override
    public void setMaxValue(T highestValue) {
        maxValue = highestValue;
    }
    

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.IRangeOptionParser#getMaxValue()
     */
    @Override
    public T getMaxValue() {
        return maxValue;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.IRangeOptionParser#getMinValue()
     */
    @Override
    public T getMinValue() {
        return minValue;
    }
    
    /**
     * Throws a ParseException for an out of range condition.
     * 
     * @param opt the ICommandLineOption being parsed
     * 
     * @throws ParseException always
     */
    protected void throwOutOfRange(ICommandLineOption<?> opt) throws ParseException {
            throw new ParseException(THE_VALUE + opt.getLongOpt() +
                    " must be between " + minValue.toString() + " and " + maxValue.toString());
    }
}
