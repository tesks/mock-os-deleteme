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

import java.util.Collection;

/**
 * Command line option to handle channel ID inputs.
 * 
 */
public class ChannelOption extends CommandLineOption<Collection<String>> {
	private static final long serialVersionUID = 6093475777000651845L;

	/**
	 * Creates a channel ID option.  Handles csv channel ID inputs and will 
	 * validate values are in the correct format.  Expands channel value ranges and substitutes wild card 
	 * symbols if values are passed in.
	 *  
     * @param opt
     *            the short option name (letter); may be null
     * @param longOpt
     *            the long option name; may be null only if opt is not
     * @param argName
     *            the name of the option's argument for help text
     * @param description
     *            the description of the option for help text
	 * @param wildCardSymbol
	 * 		Wild card symbol for the command line input.  Can be null.
	 * @param wildCardReplaceSymbol
	 * 		Wild card symbol to replace the command line wild card symbol with before expanding ranges.  Can be null.
     * @param required
     *            true if the option is always required on the command line
	 * 
	 */
	public ChannelOption(final String opt, final String longOpt, final String argName, final String description, 
			final String wildCardSymbol, final String wildCardReplaceSymbol, final boolean required) {
		super(opt, longOpt, true, argName, description, required, new ChannelOptionParser(wildCardSymbol, wildCardReplaceSymbol));

	}
	/**
	 * Creates a channel value option.  Handles csv channel value inputs and will 
	 * validate values are in the correct format.  Expands channel value ranges.  No wild card substitutions 
	 * will be made.
	 *  
     * @param opt
     *            the short option name (letter); may be null
     * @param longOpt
     *            the long option name; may be null only if opt is not
     * @param argName
     *            the name of the option's argument for help text
     * @param description
     *            the description of the option for help text
     * @param required
     *            true if the option is always required on the command line
	 */
	public ChannelOption(final String opt, final String longOpt, final String argName, final String description,
			final boolean required) {
		this(opt, longOpt, argName, description, null, null, required);
	}
}
