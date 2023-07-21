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
package jpl.gds.common.options.querycommand;

import jpl.gds.shared.cli.options.CommandLineOption;

/**
 * A command line option class representing the "--evrTypes" EVR type selector
 * option.
 * 
 *
 * @since R8
 */
@SuppressWarnings("serial")
public class EvrTypesOption extends CommandLineOption<EvrTypeSelect> {

    /** Long option name for EVR types */
    public static final String EVR_TYPES_LONG = "evrTypes";

    /**
     * Constructor.
     * 
     * @param required true if the option is required
     */
    public EvrTypesOption(final boolean required) {
        this(null, required);
    }
    
    /**
     * Constructor.
     * 
     * @param defValue default value (as string)
     * @param required true if the option is required
     */
    public EvrTypesOption(final String defValue, final boolean required) {
        super(null, EVR_TYPES_LONG, true, "string",
                "Allowed types: " +
                    "s=SSE "                +
                    "f=FSW-realtime "       +
                    "r=FSW-recorded", required,
                    new EvrTypesOptionParser());
        if (defValue != null) {
            setDefaultValue(new EvrTypeSelect(defValue));
        }
    }

}
