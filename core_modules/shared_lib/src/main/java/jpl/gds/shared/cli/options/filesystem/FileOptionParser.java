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
package jpl.gds.shared.cli.options.filesystem;

import java.io.File;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.AbstractOptionParser;
import jpl.gds.shared.cli.options.ICommandLineOption;

/**
 * A parser for command line options whose value is a file path. Optionally validates
 * the existence of the file.
 * 
 *
 */
public class FileOptionParser extends AbstractOptionParser<String>  {
    
    /**
     * Creates a validating or non-validating file option parser.  
     * 
     * @param validate true if the existence of the file should be validated
     */
    public FileOptionParser(boolean validate) {
        super(validate);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine, jpl.gds.shared.cli.options.ICommandLineOption)
     */
    @Override
    public String parse(ICommandLine commandLine, ICommandLineOption<String> opt) throws ParseException {
        final String name = getValue(commandLine,opt);

        if (name == null) {
            return null;
        }

        File namedFile = new File(name);
        if (getValidate() && (!namedFile.exists() || !namedFile.isFile())) {
            throw new ParseException("The specified file for the --"
                    + opt.getLongOpt() + " option (" + name
                    + ") does not exist or is not a file");
        }
        
        return name;
    }

}
