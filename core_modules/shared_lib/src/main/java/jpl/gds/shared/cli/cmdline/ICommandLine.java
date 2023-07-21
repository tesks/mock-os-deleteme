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
package jpl.gds.shared.cli.cmdline;

import java.util.Map;

/**
 * An interface to be implemented by command line classes.
 * 
 *
 */
public interface ICommandLine {
    
    /**
     * Indicates if the specified command line option is present in this command line.
     * 
     * @param opt option name, long or short
     * @return true if the option is present; false if not
     */
    public boolean hasOption(String opt);
    
    /**
     * Returns the string value of the specified option in this command line.
     * 
     * @param opt option name, long or short
     * @return String value; may be null
     */
    public String getOptionValue(String opt);
    
    /** 
     * Retrieve any left-over non-recognized options and arguments
     *
     * @return remaining items passed in but not parsed as an array
     * @see org.apache.commons.cli.CommandLine#getArgs()
     */
    public String[] getTrailingArguments();

    /**
     * Constructs a key value map of all present command line options and their values
     * 
     * @return Map<String, String> of all command line options
     */
    public Map<String, String> getAllOptions();
}
