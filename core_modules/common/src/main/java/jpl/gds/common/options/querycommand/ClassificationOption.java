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

import java.util.ArrayList;
import java.util.List;

import jpl.gds.common.options.DownlinkStreamTypeOptionParser;
import jpl.gds.shared.cli.options.EnumOption;
import jpl.gds.shared.log.TraceSeverity;

/**
 * A command line option class for a TraceSeverity enumeration value, with the --classification flag.
 * 
 *
 * @since R8
 *
 */
@SuppressWarnings("serial")
public class ClassificationOption extends EnumOption<TraceSeverity> {
	
	/**
	 * Long option name.
	 */
	public static final String LONG_OPTION = "classification";
	/**
	 * Short option name.
	 */
	public static final String SHORT_OPTION = "c";
	
	/**
	 * Constructor.
	 * 
	 * @param isRequired
	 *            true if the option is required, false if not
	 * @param restrictionValues
     *            List of enum values the argument value should be restricted to
	 */
	public ClassificationOption(boolean isRequired, List<TraceSeverity> restrictionValues) {
		this(TraceSeverity.INFO, isRequired, restrictionValues);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param defValue default value (as TraceSeverity enum)
	 * @param isRequired
	 *            true if the option is required, false if not
	 * @param restrictionValues
     *            List of enum values the argument value should be restricted to  
	 */
	public ClassificationOption(TraceSeverity defValue, boolean isRequired, List<TraceSeverity> restrictionValues) {
		super(TraceSeverity.class,
				SHORT_OPTION,
				LONG_OPTION,
				"string",
				"The severity of the log message.Valid values are",
				isRequired,
				restrictionValues);
		
		setParser(new ClassificationOptionParser(restrictionValues));
		if (defValue != null) {
            setDefaultValue(defValue);
        }
	}
	
}
