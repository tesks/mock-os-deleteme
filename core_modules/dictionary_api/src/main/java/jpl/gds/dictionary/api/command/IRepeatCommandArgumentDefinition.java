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
package jpl.gds.dictionary.api.command;

import java.util.List;

/**
 * The ICommandRepeatArgumentDefinition interface is to be implemented by all
 * command argument classes that implement repeating blocks of arguments. 
* <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 *
 *
 * @see ICommandDefinition
 */
public interface IRepeatCommandArgumentDefinition extends ICommandArgumentDefinition {

    /**
     * Accessor for the list of dictionary arguments in this repeat
     * argument.
     *
     * @return The list of current dictionary arguments
     */
    List<ICommandArgumentDefinition> getDictionaryArguments();

    /**
     * Adds a new dictionary argument to the list of arguments in 
     * this repeat argument.
     *
     * @param ca
     *            The new dictionary argument to add
     *
     */
    void addDictionaryArgument(final ICommandArgumentDefinition ca);

    /**
     * Return the number of dictionary arguments defined for this repeat argument.
     * 
     * @param ignoreFillArguments False if fill arguments should be included in
     * the count, true otherwise
     * 
     * @return The integer number of dictionary arguments
     */
    int getDictionaryArgumentCount(
            final boolean ignoreFillArguments);

}