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

import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.IOptionParser;

/**
 * An abstract class for extension by specific ICommandLineOption classes.
 * Provides utility methods. The data type of the option is unknown by the
 * methods here.
 * 
 * @param <T> the type of the option's value
 * 
 *
 */
public abstract class AbstractOptionParser<T extends Object> implements IOptionParser<T> {

    /** Constant for display in error messages */
    protected static final String THE_REQUIRED_OPTION = "The required command line option --";

    /** Constant for display in error messages */
    protected static final String THE_OPTION = "The command line option --";
    
    /** Constant for display in error messages */
    protected static final String THE_VALUE = "The value of command line option --";

    /** Constant for display in error messages */
    protected static final String IS_MISSING = " is missing.";

    /** Constant for display in error messages */
    protected static final String REQUIRES_A_VALUE = " requires a value.";

    /** Flag indicating whether additional validation of the option is enabled */
    private boolean validate;
    
    /** Default value for the option's argument */
    private T defaultValue;

    /**
     * Constructor for a non-validating parser.
     */
    protected AbstractOptionParser() {
        // do nothing
    }
    
    /**
     * Constructor for a non-validating parser with
     * a default value.
     * 
     * @param defValue the default value for the option
     */
    protected AbstractOptionParser(T defValue) {
        defaultValue = defValue;
    }
    
    /**
     * Constructor for a validating or non-validating parser.
     * 
     * @param validate true to enable validation; false to disable
     */
    protected AbstractOptionParser(boolean validate) {
        this.validate = validate;
    }
    
    /**
     * Constructor for a validating or non-validating parser with a 
     * default value.
     * 
     * @param validate true to enable validation; false to disable
     * @param defValue the default value for the option
     */
    protected AbstractOptionParser(boolean validate, T defValue) {
        this.validate = validate;
        this.defaultValue = defValue;
    }
    
    /**
     * Method to throw a ParseException indicating a missing option is required.
     * 
     * @param opt
     *            the ICommandLineOption in error
     * @throws ParseException
     *             always
     */
    protected void throwRequired(ICommandLineOption<?> opt)
            throws ParseException {
        throw new ParseException(THE_REQUIRED_OPTION + opt.getLongOpt()
                + IS_MISSING);
    }

    /**
     * Method to throw a ParseException indicating an option is missing its
     * corresponding value.
     * 
     * @param opt
     *            the ICommandLineOption in error
     * @throws ParseException
     *             always
     */
    protected void throwNeedsValue(ICommandLineOption<?> opt)
            throws ParseException {
        throw new ParseException(THE_OPTION + opt.getLongOpt()
                + REQUIRES_A_VALUE);
    }

    /**
     * Sets the flag indicating extra validation is enabled. Data type
     * validation is standard. This is for additional validation, which may not
     * be supported by all implementations of IOptionParser.
     * 
     * @param doValidation
     *            true to enable extra validation; false to disable
     */
    public void setValidate(boolean doValidation) {
        validate = doValidation;
    }

    /**
     * Gets the flag indicating extra validation is enabled. Data type
     * validation is standard. This is for additional validation, which may not
     * be supported by all implementations of IOptionParser.
     * 
     * @return true if extra validation is enabled; false if disabled
     */
    public boolean getValidate() {
        return validate;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.cmdline.IOptionParser#setDefaultValue(java.lang.Object)
     */
    public void setDefaultValue(T defValue) {
        this.defaultValue = defValue;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.cmdline.IOptionParser#getDefaultValue()
     */
    public T getDefaultValue() {
        return defaultValue;
    }

    /**
     * Convenience method for detecting if an option is present on the command
     * line. The only real reason to have this is to hide the Apache-specific
     * call.
     * 
     * @param commandLine
     *            parsed ICommandLine object
     * @param opt
     *            the ICommandLineOption to look for
     * @return true if the option is present in the parsed ICommandLine, false if
     *         not
     */
    protected boolean isPresent(ICommandLine commandLine,
            ICommandLineOption<?> opt) {

        return commandLine.hasOption(opt.getLongOrShort());
    }
   
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.cmdline.IOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine, jpl.gds.shared.cli.options.ICommandLineOption, boolean)
     */
    @Override
    public T parse(ICommandLine commandLine, ICommandLineOption<T> opt, boolean required) throws ParseException {
        T value = parse(commandLine, opt);
        
        if (value == null && required) {
            throwRequired(opt);
        } 

        return value;

    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.cmdline.IOptionParser#parseWithDefault(jpl.gds.shared.cli.cmdline.ICommandLine, jpl.gds.shared.cli.options.ICommandLineOption, boolean, boolean)
     */
    @Override
    public T parseWithDefault(ICommandLine commandLine, ICommandLineOption<T> opt, boolean required, boolean doSetDefault) throws ParseException {
        T value = parse(commandLine, opt);
      
        if (value == null && required) {
            throwRequired(opt);
        } else if (value == null && doSetDefault) {
            value = defaultValue;
        }

        return value;
    }
    
    /**
     * Convenience method for getting the String value of an option from the
     * parsed ICommandLine and throwing if it has no value. 
     * 
     * @param commandLine
     *            parsed ICommandLine object
     * @param opt
     *            the ICommandLineOption to look for
     * @return String value of the option, or null if the option was not present
     * 
     * @throws ParseException if the option was present but its value was not
     */
    protected String getValue(ICommandLine commandLine, ICommandLineOption<T> opt) throws ParseException {
        if (!isPresent(commandLine, opt)) {
            return null;
        }
        String value = commandLine.getOptionValue(opt.getLongOrShort());
        if (value == null) {
            throwNeedsValue(opt);           
        } else {
            value = value.trim();
        }
        return value;
    }
}
