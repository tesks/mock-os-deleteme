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

package jpl.gds.common.options;

import jpl.gds.shared.cli.options.FlagOption;

/**
 * A command option class for the SSE option. This is a no-argument option
 * with no short option letter, and is not required on the command line.
 *
 */
public class SseOption extends FlagOption {
    private static final long serialVersionUID = 1L;

    /**
     * Short option name for the SSE option.
     */
    public static final String SSE_OPTION_SHORT = "s";

    /**
     * Long option name for the SSE option.
     */
    public static final String SSE_OPTION_LONG  = "sse";

    /**
     * Constructor.
     */
    public SseOption() {
        super(SSE_OPTION_SHORT, SSE_OPTION_LONG,
              "whether SSE is enabled", false);
    }
}
