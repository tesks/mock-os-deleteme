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
import jpl.gds.tc.api.command.args.ICommandArgument;
import jpl.gds.tc.api.config.OutputFormatType;

/**
 * 
 * This is the multimission implementation of a boolean command argument. A
 * boolean argument is represented as a signed enumeration. The command argument
 * definition has an attached command enumeration object that defines the values
 * used for true and false.
 * 
 *
 * 1/8/14 - MPCS-5667. Renamed from ReferenceBooleanArgument.
 * 1/13/14 - MPCS-4802. Renamed from MslBooleanArgument.
 * 6/22/14 - MPCS-6304. Throughout: Removed dictionary-related
 *          members to CommandArgumentDefinition.
 */
public class BaseBooleanArgument extends BaseEnumeratedArgument
{

    /**
     * Creates an instance of BaseBooleanArgument.
     *
     * @param appContext App context
     * @param def
     *            the command argument definition object for this argument.
     * 
     *          definition.
     */
    public BaseBooleanArgument(ApplicationContext appContext, ICommandArgumentDefinition def)
    {
        super(appContext, def);

        /*
         * The output format for this argument is always the dictionary string
         * from the enumeration.
         */
        setFormat(OutputFormatType.STRING);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tc.impl.args.BaseEnumeratedArgument#copy()
     */
    @Override
    public ICommandArgument copy()
    {
        final BaseBooleanArgument ta = new BaseBooleanArgument(appContext, this.getDefinition());
        setSharedValues(ta);
        return(ta);
    }
}
