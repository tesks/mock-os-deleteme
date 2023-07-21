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
 * A command option class for the downlink "auto start" option. This is a no-argument option
 * with no short option letter, and is not required on the command line.
 * 
 *
 * @since R8
 *
 */
public class DownlinkAutostartOption extends FlagOption {
	private static final long serialVersionUID = 1L;
	
	/**
	 * Long option name for the autostart option.
	 */
    public static final String AUTOSTART_OPTION_LONG = "autoStart";

    /**
     * Constructor. 
     */
    public DownlinkAutostartOption() {
       super(null, AUTOSTART_OPTION_LONG, 
            "automatically press the green 'start' arrow in the downlink GUI", false);
    }

}
