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

import jpl.gds.shared.cli.options.PortOption;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * A command line option for GLAD socket port.
 */
public class GladSocketPortOption extends PortOption {

    /** Long option name */
    public static final String LONG_OPTION = "gladSocketPort";
    private static final String DESCRIPTION =
            "The socket port on which the global LAD server listens for incoming data.";

    /**
     * Constructor that accepts a default value.
     * @param required indicates if the option is required on the command line
     * @param defValue default value for the option; may be null
     */
    public GladSocketPortOption(final boolean required, final UnsignedInteger defValue) {
        super(null, LONG_OPTION, "port", DESCRIPTION, required);
        if (defValue != null) {
            setDefaultValue(defValue);
            this.setDescription(getDescription() + " Defaults to " + defValue.intValue());
        }
    }
    
    /**
     * Constructor.
     * @param required indicates if the option is required on the command line
     */
    public GladSocketPortOption(final boolean required) {
        this(required, null);
    }
}
