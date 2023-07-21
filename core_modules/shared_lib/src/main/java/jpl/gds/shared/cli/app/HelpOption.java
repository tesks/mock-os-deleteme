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
package jpl.gds.shared.cli.app;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.IExitableOption;

/**
 * A class that represents an application help option (-h or --help).  
 * 
 */
public class HelpOption extends FlagOption implements IExitableOption<Boolean> {
    
    private static final long serialVersionUID = 1L;
    private ICommandLineApp theApp;

    /**
     * Constructs a help option for the specified instance of ICommandLineApp
     * @param app the ICommandLineApp object this help option applies to
     */
    public HelpOption(ICommandLineApp app) {
        super("h", "help", "display help information", false);
        theApp = app;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.CommandLineOption#parse(jpl.gds.shared.cli.cmdline.ICommandLine, boolean)
     */
    @Override
    public Boolean parse(ICommandLine commandLine, boolean required)
            throws ParseException {
        return parseWithExit(commandLine, required, false);
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.IExitableOption#parseWithExit(jpl.gds.shared.cli.cmdline.ICommandLine, boolean, boolean)
     */
    @Override
    public Boolean parseWithExit(ICommandLine commandLine, boolean required, boolean exit)
            throws ParseException {
        
        Boolean found = super.parse(commandLine, required);
        if (found) {
            theApp.showHelp();
            if (exit) {
                System.exit(USER_HELP_REQUEST);
            }
        }

        return found;
    }
}
