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
 * A command line option class for the --printLog flag option in
 * watchers.
 * 
 *
 * @since R8
 *
 */
public class PrintLogOption extends FlagOption {
    
    /** Short print option */
    public static final String              PRINT_SHORT_OPT            = "p";
    
    /** Long print option */
    public static final String              PRINT_LONG_OPT             = "printLog";
    
    /**
     * Constructor
     */
    public PrintLogOption() {
        super(PRINT_SHORT_OPT, PRINT_LONG_OPT, 
        "display status and message receipt info to standard output", false);

    }

}
