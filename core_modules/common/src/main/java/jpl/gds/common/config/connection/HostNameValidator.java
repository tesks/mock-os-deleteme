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
package jpl.gds.common.config.connection;

import java.util.regex.Pattern;

import jpl.gds.shared.string.StringUtil;

/**
 * Validation class for a host name entered by the user.
 * 
 */
public class HostNameValidator {
    
	/** Maximum length */
	private static final int MAXIMUM = 63;

	/** Must be letters or digits or dashes or underscores */
	private static final Pattern pattern = Pattern.compile("^\\s*"
			+ "[A-Za-z0-9_-]+" + "\\s*$");

	/** But cannot begin with a dash or underscore */
	private static final Pattern notPattern0 = Pattern.compile("^\\s*"
			+ "[_-]" + ".*" + "$");

	/** But cannot end with a dash or underscore */
	private static final Pattern notPattern1 = Pattern.compile("^" + ".*"
			+ "[_-]" + "\\s*$");

	/** But cannot be all digits */
	private static final Pattern notPattern2 = Pattern.compile("^\\s*"
			+ "[0-9]+" + "\\s*$");

	/**
	 * Validate that the string is a valid session host.
	 * 
	 * @param rawS
	 *            String to validate
	 * 
	 * @return Error message or null if valid
	 */
	public String isValid(final String rawS) {

		final String s = StringUtil.safeTrim(rawS);

		if (s.isEmpty()) {
			return "Host name cannot be zero-length";
		}

		if (s.length() > MAXIMUM) {
			return ("Host name cannot be longer than " + MAXIMUM + " characters");
		}

		if (!pattern.matcher(s).matches()) {
			return "Host name must consist of letters or digits "
					+ "or dashes or underscores";
		}

		if (notPattern0.matcher(s).matches()) {
			return "Host name must not begin with a dash or underscore";
		}

		if (notPattern1.matcher(s).matches()) {
			return "Host name must not end with a dash or underscore";
		}

		if (notPattern2.matcher(s).matches()) {
			return "Host name must not consist of all digits";
		}

		return null;
	}
}