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

/**
 * A command line option class for a unique ID string.
 * 
 *
 * @since R8
 *
 */
public class UniqueIdOption extends StringOption {
    
    /** Long unique option */
    public static final String UNIQUE_LONG_OPT = "unique";

    /**
     * Constructor
     * 
     * @param required true if the option is required on the command line
     */
    public UniqueIdOption(final boolean required) {
        super(null, UNIQUE_LONG_OPT, "id", "A unique ID or disambiguating string", required);
    }

    
    
}
