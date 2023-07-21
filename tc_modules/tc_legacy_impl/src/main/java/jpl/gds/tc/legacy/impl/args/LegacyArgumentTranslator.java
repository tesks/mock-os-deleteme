/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.tc.legacy.impl.args;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextIdentification;
import jpl.gds.dictionary.api.command.CommandArgumentType;
import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.shared.types.AmpcsStringBuffer;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.CommandProperties.BitValueFormat;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;

/**
 * The LegacyArgumentTranslator is the outward facing version of all of the argument translators. It handles
 * calling the appropriate argument translator necessary for any argument conversion
 *
 */
public class LegacyArgumentTranslator implements ICommandArgumentTranslator {

    private final ApplicationContext appContext;
    private final IContextIdentification contextId;
    private final CommandProperties cmdProps;

    /**
     * Constructor. Must be supplied the application context
     * @param appContext the current application context
     */
    public LegacyArgumentTranslator(final ApplicationContext appContext) {
        this.appContext = appContext;
        contextId = appContext.getBean(IContextIdentification.class);
        cmdProps = appContext.getBean(CommandProperties.class);
    }

    @Override
    public String parseFromBitString(final ICommandArgumentDefinition def, final AmpcsStringBuffer bitString) throws UnblockException {

        if(def == null) {
            throw new IllegalStateException("supplied definition must not be null");
        } else if (bitString == null) {
            throw new IllegalStateException("supplied bit string must not be null");
        }

        return getTranslator(def.getType()).parseFromBitString(def, bitString);
    }

    @Override
    public String toBitString(final ICommandArgumentDefinition def, final String argValue) throws BlockException {

        if(def == null) {
            throw new IllegalStateException("supplied definition must not be null");
        } else if (argValue == null) {
            throw new IllegalStateException("supplied argument value must not be null");
        }

        return getTranslator(def.getType()).toBitString(def, argValue);
    }

    // helper function that gets the appropriate translator
    private ICommandArgumentTranslator getTranslator(final CommandArgumentType type) {

        int scid = contextId.getSpacecraftId();

        switch(type) {

            case TIME:
                return new LegacyBaseTimeArgumentTranslator(scid);
            case FLOAT_TIME:
                return new LegacyFloatTimeArgumentTranslator(scid);
            case FLOAT:
                return new LegacyBaseFloatArgumentTranslator();
            case INTEGER:
                return new LegacyBaseIntegerArgumentTranslator();
            case UNSIGNED:
                return new LegacyBaseUnsignedArgumentTranslator();
            case SIGNED_ENUMERATION:
            case UNSIGNED_ENUMERATION:
            case BOOLEAN: //boolean argument extends enumerated, their to/from binary functionality was identical
                BitValueFormat format = cmdProps.getEnumBitValueFormat();
                return new LegacyBaseEnumeratedArgumentTranslator(format);
            case FIXED_STRING:
                return new LegacyBaseStringArgumentTranslator();
            case VAR_STRING:
                return new LegacyBaseVarStringArgumentTranslator();
            case FILL:
                return new LegacyBaseFillerArgumentTranslator();
            case REPEAT:
                return new LegacyBaseRepeatArgumentTranslator(appContext);
            case BITMASK:
                return new LegacyBaseBitmaskArgumentTranslator();
            case UNDEFINED:
            default :
                throw new IllegalArgumentException("The CommandArgumentType " + type.toString() + " currently has no translator");
        }
    }
}
