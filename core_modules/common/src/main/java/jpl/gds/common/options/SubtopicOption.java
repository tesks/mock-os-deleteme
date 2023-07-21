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

import java.util.List;

import jpl.gds.shared.cli.options.StringOption;

/**
 * A command line option for messaging subtopic.
 * 
 * @since R8
 *
 */
@SuppressWarnings("serial")
public class SubtopicOption extends StringOption {

	/**
	 * Long option name.
	 */
	public static final String LONG_OPTION = "subtopic";
	/**
	 * Description.
	 */
	public static final String DESCRIPTION = "name of the session realtime publication subtopic for OPS venues";
	
	/**
	 * Constructor.
	 * 
	 * @param restrictTo
	 *            list of subtopics to restrict the argument value to
	 * @param required
	 *            true if the option is required, false if not
	 */
	public SubtopicOption(final List<String> restrictTo, final boolean required) {
		super(null, LONG_OPTION, "subtopic", DESCRIPTION, required, restrictTo);
		addAlias("jmsSubtopic");
	}
    
    /**
     * Constructor.
     * 
     * @param required true if the option is required, false if not
     */
    public SubtopicOption(final boolean required) {
        this(null, required);
    }


}
