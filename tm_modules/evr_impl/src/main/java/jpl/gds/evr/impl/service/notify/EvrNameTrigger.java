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
package jpl.gds.evr.impl.service.notify;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.service.IEvrNotificationTrigger;

/**
 * This class defines the EVR trigger that is based on the name value of the
 * event. The matching of the name string is purely done through regular
 * expressions.
 * 
 */
public class EvrNameTrigger implements IEvrNotificationTrigger {
	private static final int NAME_PATTERN_LIST_SIZE = 1;
	private final List<Pattern> namePatterns;

	/**
	 * Default constructor.
	 */
	public EvrNameTrigger() {
		namePatterns = new ArrayList<Pattern>(NAME_PATTERN_LIST_SIZE);
	}

	/**
	 * Add a regular expression to the set of patterns to look for in the names
	 * of incoming EVRs.
	 * 
	 * @param pattern
	 *            regular expression pattern to monitor for
	 * @throws PatternSyntaxException
	 *             thrown if regular expression is incorrectly formed
	 */
	public void addName(final String pattern) throws PatternSyntaxException {
		Pattern p = Pattern.compile(pattern);
		namePatterns.add(p);
	}

	/**
	 * Get the list of name patterns defined for this trigger.
	 * 
	 * @return list of regular expression <code>Pattern</code> objects
	 */
	public List<Pattern> getNamePatterns() {
		return namePatterns;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.evr.api.service.IEvrNotificationTrigger#evrTriggersNotification(jpl.gds.evr.api.IEvr)
	 */
	@Override
	public boolean evrTriggersNotification(final IEvr evr) {
		String evrName = evr.getName();

		if (evrName == null) {
			/*
			 * This happens when EVR doesn't have a definition in the dicionary.
			 */
			return false;
		}

		for (Pattern pattern : namePatterns) {
			Matcher m = pattern.matcher(evrName);

			if (m.matches()) {
				return true;
			}

		}

		return false;
	}

}