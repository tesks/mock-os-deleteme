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
 * A command line option class for an "exit with session" flag option.
 * 
 *
 * @since R8
 * 
 */
public class ExitWithSessionOption extends FlagOption {
    
    private static final long serialVersionUID = -6418217655856932162L;
    
    /**
     * Long command line option name for exit session
     */
    public static final String EXIT_SESSION_LONG = "exitWithSession";

    /**
     * Constructor
     */
    public ExitWithSessionOption() {
        super(null, EXIT_SESSION_LONG, 
             "exit when an End Of Session message is received", false);
    }

}
