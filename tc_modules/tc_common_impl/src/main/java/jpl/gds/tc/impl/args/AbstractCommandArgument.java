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
package jpl.gds.tc.impl.args;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.tc.api.command.args.ICommandArgument;

/**
 * This is the superclass for all types of mission command arguments. The set of
 * classes that inherit from this one are used for runtime command processing.
 * Dictionary command argument information is kept in a separate definition
 * object, which is accessible via this one.
 * 
 *
 * 11/8/13 - MPCS-5521. Implemented ICommandArgumentDefinition.
 *          Added many common methods to this class for use by subclasses.
 *          General cleanup, javadoc, static analysis changes.
 * 1/8/14 - MPCS-5662. No longer implements
 *          ParsedDictionaryXmlElement interface
 * 6/22/14 - MPCS-6304. No longer implements
 *          ICommandArgumentDefinition. Dictionary-related members throughout
 *          removed to CommandArgumentDefinition class. This class has been
 *          stripped down to support runtime commanding capabilities only.
 */
abstract class AbstractCommandArgument implements ICommandArgument {

    private final ICommandArgumentDefinition definition;
    /** The current application context */
    protected ApplicationContext appContext;
    /** Shared Tracer to log with */
    protected static final Tracer            log = TraceManager.getDefaultTracer();

    /**
     * The actual user-supplied value for this argument as a String or null if
     * the argument does not currently have a value.
     */
    protected String argumentValue;

    /**
     * Constructor.
     * 
     * @param appContext
     *            The current application context
     * @param def
     *            the command argument definition object for this argument.
     *
     */
    public AbstractCommandArgument(final ApplicationContext appContext, final ICommandArgumentDefinition def) {

        if (def == null) {
            throw new IllegalArgumentException("Command definition may not be null");
        }

        this.appContext = appContext;
        this.definition = def;
        log.setAppContext(appContext);
    }


    @Override
    public ICommandArgumentDefinition getDefinition() {
        return this.definition;
    }

    @Override
    public boolean isUserEntered() {
        return (true);
    }

    @Override
    public String getArgumentValue() {
        return this.argumentValue;
    }

    @Override
    public void setArgumentValue(final String argumentValue) {
        this.argumentValue = argumentValue;
    }

    /**
     * Helper function used in making a deep copy of this argument. Set all the
     * fields in the input argument to have the same values as the fields on
     * this argument. Keep in mind the properties of particular Java objects
     * when you do this (e.g. Strings are immutable, so you don't need to copy
     * them). Omits setting of the definition object, which is always set via the
     * constructor.
     * 
     * @param arg
     *            The argument whose values should be set to those of this
     *            argument.
     */
    protected void setSharedValues(final AbstractCommandArgument arg) {
        arg.argumentValue = this.argumentValue;
    }

    @Override
    public String getDisplayName() {

        // check the dictionary name first
        String name = this.definition.getDictionaryName() == null ? "" : this.definition.getDictionaryName()
                .trim();
        if (name.isEmpty()) {
            // try FSW name next
            name = this.definition.getFswName() == null ? "" : this.definition.getFswName().trim();
            if (name.isEmpty()) {
                // couldn't find a valid name
                name = "Unknown";
            }
        }

        return (name);
    }

    @Override
    public void clearArgumentValue() {

        setArgumentValue(this.definition.getDefaultValue());
    }

    @Override
    public String toString()
    {
        if(this.argumentValue == null)
        {
            throw new IllegalStateException("No argument value exists (value is NULL).");
        }

        return this.argumentValue;
    }
}
