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
package jpl.gds.shared.cli.cmdline;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.options.ICommandLineOption;

/**
 * A generic interface to be implemented by command line option parsers. Data
 * type T is determined by the type of the command line option.
 * 
 *
 * @param <T>
 *            Data type is determined by the type of the command line option(s)
 *            this parser supports.
 * 
 */
public interface IOptionParser<T extends Object> {

    /**
     * Parsing method for use when the option is not required.
     * 
     * @param commandLine
     *            the ICommandLine object containing parsed command line options
     * @param opt
     *            the ICommandLineOption to be parsed from the ICommandLine
     * @return the parsed object value, specific to the data type of the option
     * @throws ParseException
     *             if the option is required but not present, or if the option
     *             is not properly specified on the command line
     */
    public T parse(ICommandLine commandLine, ICommandLineOption<T> opt)
            throws ParseException;

    /**
     * Parsing method for use when the option is either required or not
     * required.
     * 
     * @param commandLine
     *            the ICommandLine object containing parsed command line options
     * @param opt
     *            the ICommandLineOption to be parsed from the ICommandLine
     * @param required
     *            true if the option is required, false if not
     * @return the parsed object value, specific to the data type of this option
     * @throws ParseException
     *             if the option is required but not present, or if the option
     *             is not properly specified on the command line
     */
    public T parse(ICommandLine commandLine, ICommandLineOption<T> opt,
            boolean required) throws ParseException;
    
    /**
     * Parsing method for use when the option is either required or not
     * required, and does or does not have a default value
     * 
     * @param commandLine
     *            the ICommandLine object containing parsed command line options
     * @param opt
     *            the ICommandLineOption to be parsed from the ICommandLine
     * @param required
     *            true if the option is required, false if not
     * @param setDefault 
     *            true if the default value should be returned if the option
     *            is not present           
     * @return the parsed object value, specific to the data type of this option
     * @throws ParseException
     *             if the option is required but not present, or if the option
     *             is not properly specified on the command line
     *             
     */
    public T parseWithDefault(ICommandLine commandLine, ICommandLineOption<T> opt,
            boolean required, boolean setDefault) throws ParseException;

    /**
     * Enables custom validation during parsing. Data type validation is
     * standard. This is to enable any other validation steps defined by the
     * specific IOptionParser. 
     * 
     * @param doValidation
     *            true to enable validation; false if not
     */
    public void setValidate(boolean doValidation);

    /**
     * Indicates whether custom validation is enabled during parsing. Data type
     * validation is standard. This flag applies to any other validation steps
     * defined by the specific IOptionParser.
     * 
     * @return true if validation enabled; false if not
     */
    public boolean getValidate();
    
    /**
     * Sets the default value for the option's argument.  Note that the default
     * value will only be applied if the parse() method that takes the setDefault
     * flag as a parameter is used.
     * 
     * @param defValue the default value 
     * 
     */
    public void setDefaultValue(T defValue);
   
    
    /**
     * Gets the default value for the option's argument.  Note that the default
     * value will only be applied if the parse() method that takes the setDefault
     * flag as a parameter is used.
     * 
     * @return the default value; may be null
     * 
     */
    public T getDefaultValue();
    

}
