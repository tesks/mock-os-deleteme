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
package jpl.gds.cli.legacy.options;

import org.apache.commons.cli.Option;

/**
 * MPCS version of Apache Option class.
 * 
 */
public class MpcsOption extends Option {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * 
     * @param opt
     *            Option name
     * @param longOpt
     *            Long option name
     * @param hasArg
     *            True if takes argument
     * @param argName
     *            Argument name
     * @param description
     *            Option description
     */
    public MpcsOption(String opt, String longOpt, boolean hasArg,
            String argName, String description) {
        super(opt, longOpt, hasArg, description);

        setArgName(argName);
    }
}
