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
package jpl.gds.dictionary.impl.command;

import java.util.ArrayList;
import java.util.List;

import jpl.gds.dictionary.api.command.CommandArgumentType;
import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.dictionary.api.command.IRepeatCommandArgumentDefinition;

/**
 * This is the implementation object for the command dictionary
 * IRepeatCommandArgumentDefinition interface. This class holds the dictionary
 * configuration for a repeating command argument.
 * 
 *
 */
public class RepeatCommandArgumentDefinition extends CommandArgumentDefinition
implements IRepeatCommandArgumentDefinition {

    /**
     * These are the definitions of the internal arguments of this repeat
     * argument straight from the dictionary
     */
    private final List<ICommandArgumentDefinition> dictionaryArguments = new ArrayList<ICommandArgumentDefinition>(
            8);

    /**
     * Constructor.
     * 
     */
    RepeatCommandArgumentDefinition() {

        super(CommandArgumentType.REPEAT);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.IRepeatCommandArgumentDefinition#getDictionaryArguments()
     */
    @Override
    public List<ICommandArgumentDefinition> getDictionaryArguments() {

        return (this.dictionaryArguments);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.IRepeatCommandArgumentDefinition#addDictionaryArgument(jpl.gds.dictionary.impl.impl.api.command.ICommandArgumentDefinition)
     */
    @Override
    public void addDictionaryArgument(final ICommandArgumentDefinition ca) {

        if (ca == null) {
            throw new IllegalArgumentException("Null input argument");
        } else if (ca.getType().equals(CommandArgumentType.REPEAT)) {
            throw new IllegalArgumentException(
                    "Cannot add other repeat arguments inside a repeat argument");
        }

        this.dictionaryArguments.add(ca);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.command.IRepeatCommandArgumentDefinition#getDictionaryArgumentCount(boolean)
     */
    @Override
    public int getDictionaryArgumentCount(final boolean ignoreFillArguments) {

        if (ignoreFillArguments == false) {
            return (this.dictionaryArguments.size());
        }

        int count = 0;
        for (int i = 0; i < this.dictionaryArguments.size(); i++) {
            if (!this.dictionaryArguments.get(i).getType()
                    .equals(CommandArgumentType.FILL)) {
                count++;
            }
        }

        return (count);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.impl.command.CommandArgumentDefinition#isVariableLength()
     */
    @Override
    public boolean isVariableLength()
    {
        return(true);
    }

}
