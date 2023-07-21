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

import java.util.List;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.cmdline.ICommandLine;

/**
 * A command line option parsing class for CommandLineOptions whose value is a String.
 * Optionally checks the string against a list of allowed values.
 * 
 */
public class StringOptionParser extends AbstractListCheckingOptionParser<String> {

    /**
     * Constructor for a non-validating string option parser.
     */
    public StringOptionParser() {
        super();
    }
    
    /**
     * Constructor for a validating string option parser.
     * 
     * @param restrictionValues the list of allowed values
     */
    public StringOptionParser(List<String> restrictionValues) {
        super(restrictionValues);
    }
    
    /**
     * Constructor for a validating or non-validating string option parser.
     * Used only by subclasses. Basic string options have no validation.
     * 
     * @param validate true to enable validation; false to disable
     */
    protected StringOptionParser(boolean validate) {
        super();
        setValidate(true);
    }

    /**
     * @see jpl.gds.shared.cli.cmdline.IOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine, jpl.gds.shared.cli.options.ICommandLineOption)
     */
    @Override
    public String parse(ICommandLine commandLine, ICommandLineOption<String> opt) throws ParseException {
        String value = getValue(commandLine, opt);
        if (value != null && getValidate()) {
            super.checkValueInList(opt, value, true);
        }
        return value;
    }
}
