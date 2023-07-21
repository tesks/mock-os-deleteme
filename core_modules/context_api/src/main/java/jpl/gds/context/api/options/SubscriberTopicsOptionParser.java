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

import java.util.Collection;

import org.apache.commons.cli.ParseException;

import jpl.gds.context.api.ContextTopicNameFactory;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.CsvStringOptionParser;
import jpl.gds.shared.cli.options.ICommandLineOption;

/**
 * 
 * An option parser class for the subscriber topics option. Topics are always sorted and
 * duplicates removed. Topic names are checked to ensure they conform to expectations.
 * 
 *
 * @since R8
 */
public class SubscriberTopicsOptionParser extends CsvStringOptionParser {

    /**
     * Constructor.
     */
    public SubscriberTopicsOptionParser() {
        super(true, true);
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.cmdline.IOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine, jpl.gds.shared.cli.options.ICommandLineOption)
     */
    @Override
    public Collection<String> parse(final ICommandLine commandLine, final ICommandLineOption<Collection<String>> opt) throws ParseException {
        final Collection<String> values = super.parse(commandLine, opt);
        if (values == null || values.isEmpty()) {
            return values;
        }
        for (final String val: values) { 
            ContextTopicNameFactory.checkTopicCommandOption(val, opt.getLongOpt());           
        }
        
        return values;
    }

}
