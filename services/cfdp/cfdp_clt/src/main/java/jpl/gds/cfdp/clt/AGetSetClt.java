/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */


package jpl.gds.cfdp.clt;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.StringOption;

/**
 * Class AGetSetClt
 *
 */
public abstract class AGetSetClt extends ACfdpClt {

	protected static final String GET_SHORT = "g";
	protected static final String GET_LONG = "get";
	protected static final String SET_SHORT = "s";
	protected static final String SET_LONG = "set";

	protected StringOption getOption;
	protected StringOption setOption;

	protected String getParam;
	protected String setParam;

	/*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.cfdp.clt.ACfdpClt#createOptions()
	 */
	@Override
	public BaseCommandOptions createOptions() {
		
	    if (optionsCreated.get()) {
            return options;
        }
	    
		final BaseCommandOptions options = super.createOptions();
		options.addOption(getOption);
		options.addOption(setOption);
		return options;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * jpl.gds.cfdp.clt.ACfdpClt#configure(jpl.gds.shared.cli.cmdline.ICommandLine)
	 */
	@Override
	public void configure(ICommandLine commandLine) throws ParseException {
		super.configure(commandLine);
		getParam = getOption.parse(commandLine);
		setParam = setOption.parse(commandLine);
	}

}
