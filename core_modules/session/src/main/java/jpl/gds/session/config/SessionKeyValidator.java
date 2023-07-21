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
package jpl.gds.session.config;

import org.eclipse.jface.dialogs.IInputValidator;

import jpl.gds.shared.string.StringUtil;

/**
 * Validation class for session keys.
 * 
 */
public class SessionKeyValidator extends Object implements
		IInputValidator {
	private static final String message = "Session key must be an integer > 0 and <= "
			+ Long.MAX_VALUE;

	/**
	 * Validate that the string is a valid session key.
	 * 
	 * @param s
	 *            String to validate
	 * 
	 * @return Error message or null if valid
	 */
	@Override
	public String isValid(final String s) {

		String result = null;

		try {
			final long value = Long.parseLong(StringUtil.safeTrim(s));

			if (value <= 0L) {
				result = message;
			}
		} catch (final NumberFormatException nfe) {
			result = message;
		}

		return result;
	}
}