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

import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.ParseException;

/**
 * A abstract command line option parser for CommandLineOptions whose argument
 * can checked against a list of allowed values. To be extended by specific
 * list-checking parsers.
 * 
 * @param <T> the data type of the option's value
 * 
 */
public abstract class AbstractListCheckingOptionParser<T> extends
        AbstractOptionParser<T> implements IListCheckingOptionParser<T> {

    /** The list of allowed values */
    protected List<T> allowedValues;

    /**
     * Constructor for a non-validating list-checking option parser.
     */
    protected AbstractListCheckingOptionParser() {
        super(false);
    }

    /**
     * Constructor for a range-checking option parser with specified list
     * of allowed values.
     * 
     * @param restrictionValues the list of valid values for the option
     */
    protected AbstractListCheckingOptionParser(final List<T> restrictionValues) {
        super(true);
        setRestrictionList(restrictionValues);
    }

    /**
     * @{inheritDoc
     * @see jpl.gds.shared.cli.options.IListCheckingOptionParser#setRestrictionList(java.util.List)
     */
    @Override
    public void setRestrictionList(final List<T> restrictionValues) {
        allowedValues = restrictionValues;
        setValidate(allowedValues != null);
    }

    /**
     * @{inheritDoc
     * @see jpl.gds.shared.cli.options.IListCheckingOptionParser#getRestrictionList()
     */
    @Override
    public List<T> getRestrictionList() {
        if (allowedValues == null) {
            return null;
        }
        return Collections.unmodifiableList(allowedValues);
    }
    
    /**
     * A convenience method to verify that the supplied option value is
     * in the list of restricted values and throw if not.
     * 
     * @param opt the ICommandLineOption being parsed
     * @param value the value of the option from the command line
     * @param printValues true to include the allowed values in the error message
     * @throws ParseException if the value is not in the restriction list
     */
    protected void checkValueInList(final ICommandLineOption<T> opt, final T value, final boolean printValues)
            throws ParseException {
        if (getValidate() && (allowedValues != null)) {
            if (allowedValues.contains(value)) {
                return;
            }
            if (value != null) {
                for (final T allowedValue : allowedValues) {
                    if ((allowedValue != null) && allowedValue.toString().equals(value.toString())) {
                        return;
                    }
                }
            }
            throwNotListValue(opt, printValues);
        }
    }
    
    /**
     * A convenience method to throw a ParseException when the option's value is
     * not in the required subset of values.
     * 
     * @param opt
     *            the ICommandLineOption being parsed
     * @param printValues true to include the allowed values in the error message
     *            
     * @throws ParseException
     *             always
     */
    protected void throwNotListValue(final ICommandLineOption<T> opt, final boolean printValues)
            throws ParseException {
        final StringBuilder extraMessage = new StringBuilder("");
        
        if (printValues && allowedValues != null) {
            extraMessage.append(": ");
            for (int i = 0; i < allowedValues.size(); i++) {
                extraMessage.append(allowedValues.get(i));
                if (i != allowedValues.size() -1)
                    extraMessage.append(", ");
            }
        }
        throw new ParseException(THE_VALUE
                + opt.getLongOpt() + " is not one of the accepted values: " +
                extraMessage.toString());
    }

}
