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
package jpl.gds.context.api.options;

import java.util.Arrays;

import jpl.gds.context.api.ContextTopicNameFactory;
import jpl.gds.shared.cli.options.CsvStringOption;

/**
 * A command line option class for subscriber messaging topics, which can be entered as a
 * CSV string.
 * 
 *
 * @since R8
 *
 */
@SuppressWarnings("serial")
public class SubscriberTopicsOption extends CsvStringOption {
    
    /**
     * Long option name.
     */
    public static final String LONG_OPTION = "topics";
    

	/**
	 * Constructor.
	 * 
	 * @param required
	 *            true if the option is required, false if not
	 */
	public SubscriberTopicsOption(final boolean required) {
		super(
				null,
				LONG_OPTION,
				"topic[,topic...]",
				"comma-separated list of topics to subscribe to; " +
				        "used INSTEAD of session information for establishing topics",
				false, true, required);
		setDefaultValue(Arrays.asList(ContextTopicNameFactory
				.getGeneralTopic()));
		setParser(new SubscriberTopicsOptionParser());
	}

}
