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
package jpl.gds.common.options.querycommand;

import java.util.List;

import jpl.gds.shared.cli.options.numeric.CsvUnsignedIntOption;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * A command option class for session key identifier values.
 * 
 *
 * @since R8
 * 
 */
@SuppressWarnings("serial")
public class SessionKeyListOption extends CsvUnsignedIntOption{

	/**
	 * Long option name.
	 */
	public static final String LONG_OPTION = "sessionKey";
	/**
	 * Short option name.
	 */
	public static final String SHORT_OPTION = "K";	
	/**
	 * Option argument name.
	 */
	public static final String ARG_NAME = "sessionId";
	
	/**
	 *  The description of the option for help text.
	 */
	public static final String DESCRIPTION = "The unique numeric identifier for a session. Multiple values "
            + "may be supplied in a comma-separated value (CSV) "
            + "format. (A range separated by \"..\" also accepted.)";
	
	/**
	 * @param sort
	 * 		true to sort the resulting collection based on the natural ordering.
	 * @param removeDuplicates
	 * 		true to remove any duplicates from the resulting collection.
     * @param required
     *            true if the option is always required on the command line
	 */
	public SessionKeyListOption(final boolean sort, final boolean removeDuplicates, final boolean required) {
		super(SHORT_OPTION, LONG_OPTION, ARG_NAME, DESCRIPTION, sort, removeDuplicates, required);
		addAlias("testKey");
		
	}
	
	
	/**
	 * @param sort
	 * 		true to sort the resulting collection based on the natural ordering.
	 * @param removeDuplicates
	 * 		true to remove any duplicates from the resulting collection.
     * @param required
     *            true if the option is always required on the command line
     * @param valid list of valid values 
	 */
	public SessionKeyListOption(final boolean sort, final boolean removeDuplicates, final boolean required, final List<UnsignedInteger> valid) {
		super(SHORT_OPTION, LONG_OPTION, ARG_NAME, DESCRIPTION, sort, removeDuplicates, required, valid);
		addAlias("testKey");
	}
}
