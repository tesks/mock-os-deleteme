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
package jpl.gds.globallad.options;

import jpl.gds.shared.cli.options.StringOption;

/**
 * A command line option for GLAD server host.
 */
public class GladServerHostOption extends StringOption {
    
    /** Long option name */
    public static final String LONG_OPTION = "gladServerHost";
    private static final String DESCRIPTION =
            "The host on which the global LAD server is running.";

    /**
     * Constructor that accepts a default value.
     * @param required indicates if the option is required on the command line
     * @param defValue default value for the option; may be null
     */
    public GladServerHostOption(final boolean required, final String defValue) {
        super(null, LONG_OPTION, "host", DESCRIPTION, required);
        if (defValue != null) {
            setDefaultValue(defValue);
            this.setDescription(getDescription() + " Defaults to " + defValue);
        }
    }
    
    /**
     * Constructor.
     * @param required indicates if the option is required on the command line
     */
    public GladServerHostOption(final boolean required) {
        this(required, null);
    }

}
