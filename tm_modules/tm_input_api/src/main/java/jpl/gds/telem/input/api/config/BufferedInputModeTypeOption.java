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
package jpl.gds.telem.input.api.config;

import java.util.LinkedList;

import jpl.gds.shared.cli.options.EnumOption;

/**
 * A command line option class for input of Buffered Input Mode Type.
 * 
 *
 * @since R8
 *
 */
@SuppressWarnings("serial")
public class BufferedInputModeTypeOption extends EnumOption<BufferedInputModeType> {
    
    // MPCS-8120 - 1/15/18 - Added constant for long name
    /** Long option name */
    public static final String LONG_OPTION = "bufferedInput";

	/**
	 * Constructor. Note that both of the forFsw and forSse flags may be set if
	 * this command line option is used for an integrated (flight and SSE)
	 * application.
	 * 
	 * @param isRequired
	 *            true if the option is required, false if not
	 * @param config
	 *            the current TelemetryInputConfig object
	 * @param forFsw
	 *            true if the argument is for a flight application
	 * @param forSse
	 *            true if the argument is for an SSE application
	 */
	public BufferedInputModeTypeOption(final boolean isRequired,
			final TelemetryInputProperties config, final boolean forFsw, final boolean forSse) {
		super(BufferedInputModeType.class, null, LONG_OPTION, "enabledIn",
				"enable buffered input stream for downlink mode", isRequired,
				new LinkedList<BufferedInputModeType>(
						config.getAllowedBufferedInputModes()));
		setParser(new BufferedInputModeTypeOptionParser(config, forFsw, forSse));
	}    
   
}
