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
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.command.args.ICommandArgument;
import jpl.gds.tc.api.exception.ArgumentParseException;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;

/**
 * 
 * This class is the base implementation of a signed integer command argument.
 * 
 * An integer argument represents a single signed integer value such as 25 or
 * -2. If an integer argument is defined to be 8 bits long, its allowable values
 * fall in the range -128 to 127 (unless otherwise restricted by the mission).
 * Integer arguments use 2's complement representation for signed numbers.
 * 
 *
 * 11/8/13 - MPCS-5521. Some methods and members moved to
 *          superclass. General cleanup, javadoc, static analysis changes.
 * 6/22/14 - MPCS-6304. Dictionary-related members throughout
 *          removed to CommandArgumentDefinition class. This class has been
 *          stripped down to support runtime commanding capabilities only.
 * 7/1/19 - MPCS-10745  - removed toBitString, parseFromBitString
 */
public class BaseIntegerArgument extends AbstractNumericArgument
{
    /**
     * Creates an instance of BaseIntegerArgument.
     *
     * @param appContext App context
     * @param def
     *            the command argument definition object for this argument.
     * 
     */
    public BaseIntegerArgument(ApplicationContext appContext, ICommandArgumentDefinition def)
    {
        super(appContext, def);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tc.impl.args.AbstractCommandArgument#copy()
     */
    @Override
    public ICommandArgument copy()
    {
        final BaseIntegerArgument ia = new BaseIntegerArgument(appContext, this.getDefinition());
        setSharedValues(ia);
        return(ia);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tc.impl.args.AbstractNumericArgument#formatValueAsComparable(java.lang.String)
     */
    @Override
    protected Number formatValueAsComparable(final String inputVal) throws ArgumentParseException
    {
        final int bitLength = this.getDefinition().getBitLength();
        try
        {
            if(bitLength <= Byte.SIZE)
            {
                return(Byte.valueOf(GDR.parse_byte(inputVal)));
            }
            else if(bitLength <= Short.SIZE)
            {
                return(Short.valueOf(GDR.parse_short(inputVal)));
            }
            else if(bitLength <= Integer.SIZE)
            {
                return(Integer.valueOf(GDR.parse_int(inputVal)));
            }

            return(Long.valueOf(GDR.parse_long(inputVal)));
        }
        catch(final Exception e)
        {
            throw new ArgumentParseException("Could not interpret input value "
                    + inputVal,e);
        }
    }
}
