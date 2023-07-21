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

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.EnumOptionParser;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.log.TraceSeverity;

/**
 * A command line option parser for the "--classification" option.
 * 
 *
 * @since R8
 */
public class ClassificationOptionParser extends EnumOptionParser<TraceSeverity> {
	
    /**
     * Constructor
     * @param restrictionValues list of allowed values for the options
     */
	public ClassificationOptionParser(final List<TraceSeverity> restrictionValues){
		super(TraceSeverity.class, restrictionValues);
	}
	
	@Override
    public TraceSeverity parse(final ICommandLine commandLine, final ICommandLineOption<TraceSeverity> opt) throws ParseException {
        final String value = getValue(commandLine, opt);
        TraceSeverity result;
        
        // Do not throw. Just return null.
        if (value == null) {
            return null;
        }        
        try
        {
            result = TraceSeverity.fromStringValue(value);
        }
        catch(final IllegalArgumentException iae)
        {
            throw new ParseException("The input classification value \"" + value + "\" is not a valid log message classification.");
        }
        
       
        return result;
    }
}
