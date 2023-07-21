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
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.command.args.ICommandArgument;
import jpl.gds.tc.api.command.args.IFillCommandArgument;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;


/**
 * This is the base class for the implementation of fill command arguments.
 * 
 * A fill argument is an argument whose value is not actually input by the user.
 * Fill arguments are generally used to pad out a particular command so that it
 * falls on a byte boundary. Fill argument values are unique in that their
 * entire value is defined in the command dictionary; the user has no control
 * over the value of a fill argument.
 * 
 *
 * 6/22/14 - MPCS-6304. Dictionary-related members throughout
 *          removed to CommandArgumentDefinition class. This class has been
 *          stripped down to support runtime commanding capabilities only.
 * 7/1/19 - MPCS-10745 - removed toBitString, parseFromBitString
 */
class BaseFillerArgument extends AbstractCommandArgument implements IFillCommandArgument
{
    /**
     * Creates a new fill argument.
     * 
     * @param def the command argument definition object for this argument
     *
     */
    public BaseFillerArgument(final ApplicationContext appContext, final ICommandArgumentDefinition def)
    {

        /*
         * 11/5/13 - MPCS-5521. Must now pass argument type to the superclass.
         */
        super(appContext, def);

        /*
         * 1/13/14 - MPCS-4802. Set default fill value to all '0' bits.
         */
        if(def.getDefaultValue() == null ) {
            def.setDefaultValue("0b0");
        }
    }
    
    //MPCS-9726 06/19/18 - removed override of isUserEntered - fill argument value can be specified by the user

    @Override
    public void setArgumentValue(final String val)
    {
        if(val == null)
        {
            throw new IllegalArgumentException("Null input fill value");
        }

        this.argumentValue = BinOctHexUtility.stripBinaryPrefix(val);
    }

    @Override
    public String toString()
    {
        return(getArgumentValue());
    }

    @Override
    public String getArgumentValue()
    {
        if (this.argumentValue == null) {
            setArgumentValue(getDefinition().getDefaultValue());
        }
        if(this.argumentValue == null)
        {
            throw new IllegalStateException("Argument value is currently null.");
        }

        return this.argumentValue;
    }

    @Override
    public ICommandArgument copy()
    {
        final BaseFillerArgument arg = new BaseFillerArgument(appContext, this.getDefinition());
        setSharedValues(arg);
        return(arg);
    }

    @Override
    public boolean isValueValid()
    {
        try {
            //if the argument value is bad, an IllegalStateExeption is thrown.
            getArgumentValue();
        } catch (IllegalStateException e) {
            return false;
        }

        if(!isValueTransmittable()) {
            return false;
        }
        if(this.argumentValue.length() == 0) {
            return false;
        }

        return ((getDefinition().getBitLength() % this.argumentValue.length()) == 0);
    }

    @Override
    public boolean isValueTransmittable()
    {
        if(this.argumentValue == null)
        {
            return(false);
        }

        /*
         * MPCS-5652 - Removed unreachable length < 0 condition
         */
        if(this.argumentValue.length() > this.getDefinition().getBitLength())
        {
            return(false);
        }

        /*
         * MPCS-5607. This was checking that value had valid hex format. The entire rest
         * of the class assumes binary format of the value and nothing works with a hex value. 
         * Changed this to check for binary format instead.
         */
        return(BinOctHexUtility.isValidBin(this.argumentValue));
    }
}
