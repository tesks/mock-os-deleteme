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

import java.util.Set;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.IOptionParser;

/**
 * A generic interface to be implemented by command line option classes.
 * 
 *
 * @param <T> the data type of the option
 * 
 */
public interface ICommandLineOption<T extends Object> {

    /** VM exit code when there is a command line parsing error. */
    public static final int COMMAND_LINE_ERROR = 1;
    /** VM exit code when there is a user help or version request. */
    public static final int USER_HELP_REQUEST = 2;

    /**
     * Adds an alias for this command line option, for use by an aliasing
     * command line parser. There is no distinction between long and short
     * alias. It is just an alias, period.
     * 
     * @param aliasToAdd
     *            the alias to add; may not be null
     */
    public void addAlias(String aliasToAdd);

    /**
     * Gets the set of defined aliases.
     * 
     * @return Set of alias strings (non-modifiable); may be empty but never
     *         null
     */
    public Set<String> getAliases();

    /**
     * Indicates whether the supplied string is an alias for this command line
     * option. Compares only to the alias list, not the existing short or long
     * option name.
     * 
     * @param alias
     *            alias to check
     * @return true if this alias is on the alias list, false if not
     */
    public boolean isAlias(String alias);

    /**
     * Clears the alias list.
     */
    public void clearAliases();

    /**
     * Parsing method for use when the option is either required or not
     * required.
     * 
     * @param commandLine
     *            the ICommandLine object containing parsed command line options
     * @param required
     *            true if the option is required, false if not
     * @return the parsed object value, specific to the data type of this option
     * @throws ParseException
     *             if the option is required but not present, or if the option
     *             is not properly specified on the command line
     */
    public T parse(ICommandLine commandLine, boolean required)
            throws ParseException;

    /**
     * Parsing method for use when the option is not required.
     * 
     * @param commandLine
     *            the ICommandLine object containing parsed command line options
     * @return the parsed object value, specific to the data type of this option
     * @throws ParseException
     *             if the option is not properly specified on the command line
     */
    public T parse(ICommandLine commandLine) throws ParseException;
    
    /**
     * Parsing method for use when the option is either required or not
     * required, and may have a default.
     * 
     * @param commandLine
     *            the ICommandLine object containing parsed command line options
     * @param required
     *            true if the option is required, false if not
     * @param useDefault 
     *            true if the parse should return the option default value if the
     *            option is not present           
     * @return the parsed object value, specific to the data type of this option
     * @throws ParseException
     *             if the option is required but not present, or if the option
     *             is not properly specified on the command line
     */
    public T parseWithDefault(ICommandLine commandLine, boolean required, boolean useDefault)
            throws ParseException;

    /**
     * Gets the IOptionParser for this option
     * 
     * @return IOptionParser instance
     */
    public IOptionParser<T> getParser();

    /**
     * Sets the IOptionParser for this option; data type of the parser must
     * match or be a subclass of the option data type
     * 
     * @param parser
     *            the IOptionParser to set
     */
    public void setParser(IOptionParser<T> parser);

    /**
     * Gets the short option name (letter).
     * 
     * @return option letter; may be null
     */
    public String getOpt();
    
    /**
     * Gets the long option name.
     * 
     * @return long option name; may be null only if short option is null
     */
    public String getLongOpt();
    
    /**
     * Gets the long option name if defined, else the short option name.
     * 
     * @return long (preferred) or short option name
     */
    public String getLongOrShort();

    /**
     * Indicates whether this option takes an argument.
     * 
     * @return true if the option has an argument; false if not
     */
    public boolean hasArg();

    /**
     * Gets the name of the option's argument.
     * 
     * @return argument name; may be null if hasArg() is false
     */
    public String getArgName();

    /**
     * Gets the options description.
     * 
     * @return description text
     */
    public String getDescription();

    /**
     * Indicates if the option is always required on the command line.
     * 
     * @return true if the option is required; false if not
     */
    public boolean isRequired();

    /**
     * Sets the default value for the option. This is actually just passed
     * through to the IOptionParser. The default value is used only if the
     * IOptionParser parse() method invoked indicates that it is to be used.
     * 
     * @param defValue
     *            the default value
     *            
     */
    public void setDefaultValue(T defValue);

    /**
     * Gets the default value for the option. This is actually just passed
     * through to the IOptionParser.
     * 
     * @return default value; may be null
     * 
     */
    public T getDefaultValue();

    /**
     * Sets the flag indicating whether the option is hidden. A hidden option can
     * be used on the command line, but does not appear in help output.
     * 
     * @param isHidden true to hide the option, false to not
     * 
     */
    public void setHidden(boolean isHidden);

    /**
     * Gets the flag indicating whether the option is hidden. A hidden option can
     * be used on the command line, but does not appear in help output.
     * 
     * @return true if the option is hidden, false to not
     * 
     */
    public boolean isHidden();

}