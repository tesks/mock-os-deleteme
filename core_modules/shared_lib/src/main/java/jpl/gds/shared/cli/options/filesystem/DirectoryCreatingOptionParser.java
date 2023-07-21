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
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.cli.options.StringOptionParser;

/**
 * A command line option parser for CommandLineOptions whose argument is a
 * directory name, for the cases in which the directory should be created if it
 * does not exist.
 * 
 *
 */
public class DirectoryCreatingOptionParser extends StringOptionParser {

    /**
     * Constructor.
     * 
     * @param validate true if the parser should check that the specified
     * directory really is a directory.
     * 
     */
    public DirectoryCreatingOptionParser(boolean validate) {
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
        if (!namedFile.exists()) {
            if (!namedFile.mkdirs()) {
                throw new ParseException("Could not create required directory: " + namedFile.getParent());
            }
        }
        if (getValidate() && !namedFile.isDirectory()) {
            throw new ParseException("The specified directory for the --"
                    + opt.getLongOpt() + " option (" + namedFile.getPath()
                    + ") does not exist or is not a directory.");
        }
        
        return name;
    }

}
