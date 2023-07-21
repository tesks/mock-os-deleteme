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

import java.io.UnsupportedEncodingException;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.command.args.ICommandArgument;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;

/**
 * This is the multimission representation of a variable length string command
 * argument.
 * <p>
 * Unlike a fixed string argument, a variable length string has a maximum length
 * defined in the dictionary, but can be any length up to that maximum. The
 * first value transmitted to the spacecraft is an unsigned integer prefix
 * indicating the actual length of the string. The length of this prefix must
 * also be defined in the dictionary.
 * 
 *
 * 11/5/13 - MPCS-5521. Correct static analysis and javadoc issues.
 * 1/8/14 - MPCS-5622. No longer implements ReferenceVarCountParsable
 * 1/8/14 - MPCS-5667. Renamed from ReferenceVarStringArgument
 * 6/22/14 - MPCS-6304. Dictionary-related members throughout
 *          removed to CommandArgumentDefinition class. This class has been
 *          stripped down to support runtime commanding capabilities only.
 * 
 */
public class BaseVarStringArgument extends BaseStringArgument {
    /**
     * Creates an instance of BaseVarStringArgument.
     *
     * @param appContext App context
     * @param def
     *            the command argument definition object for this argument.
     * 
     */
    public BaseVarStringArgument(ApplicationContext appContext, ICommandArgumentDefinition def) {

        super(appContext, def);
    }

    @Override
    public ICommandArgument copy() {

        final BaseVarStringArgument arg = new BaseVarStringArgument(
                appContext, this.getDefinition());
        setSharedValues(arg);
        return (arg);
    }

    @Override
    public String toString() {

        return (getArgumentValue());
    }

    @Override
    public boolean isValueValid() {

        if (!isValueTransmittable()) {
            return (false);
        }

        /* 6/23/14 - MPCS-6304. Get rid of max chars. Just check against bit length. */
        if (this.argumentValue.length() > this.getDefinition().getBitLength() / 8) {
            return (false);
        }

        if (this.getDefinition().getValueRegexp() == null) {
            return (true);
        }

        return (this.argumentValue.matches(this.getDefinition().getValueRegexp()));
    }

    @Override
    public boolean isValueTransmittable() {

        return (this.argumentValue != null);
    }

}
