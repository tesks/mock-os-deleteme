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
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.IOptionParser;

/**
 * A generic extended version of Apache Option class that adds ability to create
 * required options using a constructor, aliasing capability, and self parsing
 * capability using an IOptionParser.
 * 
 *
 * @param <T> the type of the option value
 * 
 */
public class CommandLineOption<T extends Object> extends Option implements ICommandLineOption<T>, 
    Comparable<CommandLineOption<T>> {

    private static final long serialVersionUID = 1L;

    /**
     * The option parser instance.
     */
    protected IOptionParser<T> parser;
    private final Set<String> aliases = new TreeSet<String>();
    private boolean hidden;

    /**
     * Constructor for a command line option, with ability to set the option as
     * required. Note that setting the required flag here means the option is
     * required by the command line parser every time and will never reach the
     * parse() methods defined here. If the option is sometimes required, use
     * the required flag on the parse method instead.
     * 
     * @param opt
     *            Short option name (letter, may be null)
     * @param longOpt
     *            Long option name; may not be null
     * @param hasArg
     *            true if the option takes an argument
     * @param argName
     *            Argument name (may be null if hasArg is false)
     * @param description
     *            Option description
     * @param required
     *            true if option is required, false if not
     * @param parser
     *            the IOptionParser for this option; may be null to support
     *            later override by subclasses, but the parse() methods will
     *            throw if still null at parse time
     */
    public CommandLineOption(final String opt, final String longOpt,
            final boolean hasArg, final String argName,
            final String description, final boolean required,
            final IOptionParser<T> parser) {
        this(opt, longOpt, hasArg, argName, description, required,
                parser, null);
    }
    
    /**
     * Constructor for a command line option, with ability to set the option as
     * required and to specify a default value. Note that setting the required 
     * flag here means the option is required by the command line parser every 
     * time and will never reach the parse() methods defined here. If the option 
     * is sometimes required, use the required flag on the parse method instead.
     * Note also that the default value is ONLY applied if the IOptionParser method
     * that includes the setDefault argument is invoked on the option. It will not
     * be automatically applied just because the parse() call indicates it is
     * required but the option is not provided.
     * 
     * @param opt
     *            Short option name (letter, may be null)
     * @param longOpt
     *            Long option name; may not be null
     * @param hasArg
     *            true if the option takes an argument
     * @param argName
     *            Argument name (may be null if hasArg is false)
     * @param description
     *            Option description
     * @param required
     *            true if option is required, false if not
     * @param defValue 
     *            defaultValue for the option           
     * @param parser
     *            the IOptionParser for this option; may be null to support
     *            later override by subclasses, but the parse() methods will
     *            throw if still null at parse time
     */
    public CommandLineOption(final String opt, final String longOpt,
            final boolean hasArg, final String argName,
            final String description, final boolean required,
            final IOptionParser<T> parser, final T defValue) {

        super(opt, longOpt, hasArg, description);
        this.parser = parser;

        if (longOpt == null) {
            throw new IllegalArgumentException(
                    "long option name may not be null");
        }

        if (hasArg && argName == null || !hasArg && argName != null) {
            throw new IllegalArgumentException(
                    "boolean value of hasArg and null status of argName disagree");
        }
        setArgName(argName);
        setRequired(required);
        if (parser != null) {
            parser.setDefaultValue(defValue);
        } else if (defValue != null) {
            throw new IllegalArgumentException("Default value may not be supplied unless a parser is also supplied");
        }
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.ICommandLineOption#addAlias(java.lang.String)
     */
    @Override
    public void addAlias(final String aliasToAdd) {
        if (aliasToAdd == null) {
            throw new IllegalArgumentException("alias may not be null");
        }
        aliases.add(aliasToAdd);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.ICommandLineOption#getAliases()
     */
    @Override
    public Set<String> getAliases() {
        return Collections.unmodifiableSet(this.aliases);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.ICommandLineOption#isAlias(java.lang.String)
     */
    @Override
    public boolean isAlias(final String alias) {
        return this.aliases.contains(alias);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.ICommandLineOption#clearAliases()
     */
    @Override
    public void clearAliases() {
        this.aliases.clear();
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.ICommandLineOption#parse(jpl.gds.shared.cli.cmdline.ICommandLine, boolean)
     */
    @Override
    public T parse(final ICommandLine commandLine, final boolean required)
            throws ParseException {
        if (parser == null) {
            throw new IllegalStateException("Command line option --"
                    + this.getLongOpt() + " has no defined parser");
        } else {
            return parser.parse(commandLine, this, required);
        }
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.ICommandLineOption#parse(jpl.gds.shared.cli.cmdline.ICommandLine)
     */
    @Override
    public T parse(final ICommandLine commandLine) throws ParseException {
        if (parser == null) {
            throw new IllegalStateException("Command line option --"
                    + this.getLongOpt() + " has no defined parser");
        } else {
            return parser.parse(commandLine, this, this.isRequired());
        }
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.ICommandLineOption#parseWithDefault(jpl.gds.shared.cli.cmdline.ICommandLine, boolean, boolean)
     */
    @Override
    public T parseWithDefault(final ICommandLine commandLine, final boolean required, final boolean setDefault)
            throws ParseException {
        if (parser == null) {
            throw new IllegalStateException("Command line option --"
                    + this.getLongOpt() + " has no defined parser");
        } else {
            return parser.parseWithDefault(commandLine, this, required, setDefault);
        }
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.ICommandLineOption#getParser()
     */
    @Override
    public IOptionParser<T> getParser() {
        return parser;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.ICommandLineOption#setParser(jpl.gds.shared.cli.cmdline.IOptionParser)
     */
    @Override
    public void setParser(final IOptionParser<T> parser) {
        this.parser = parser;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.ICommandLineOption#getLongOrShort()
     */
    @Override
    public String getLongOrShort() {
        if (getLongOpt() != null) {
            return getLongOpt();

        }
        return getOpt();
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.ICommandLineOption#setHidden(boolean)
     */
    @Override
    public void setHidden(final boolean isHidden) {
        this.hidden = isHidden;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.ICommandLineOption#isHidden()
     */
    @Override
    public boolean isHidden() {
        return this.hidden;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.ICommandLineOption#setDefaultValue(java.lang.Object)
     */
    @Override
    public void setDefaultValue(final T defValue) {
        if (this.parser == null) {
            throw new IllegalStateException("Cannot set default value when option parser is null");
        }
        this.parser.setDefaultValue(defValue);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.ICommandLineOption#getDefaultValue()
     */
    @Override
    public T getDefaultValue() {
        return (this.parser == null) ? null : this.parser.getDefaultValue();
    }

    @Override
    public int compareTo(final CommandLineOption<T> o) {
        return getLongOpt().compareTo(o.getLongOpt());
    }

}
