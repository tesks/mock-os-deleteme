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

import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import jpl.gds.shared.cli.options.ICommandLineOption;

/**
 * A class that is a container for ICommandLineOption objects. It contains a set
 * of CommandLineOptions for parsing from a command line. It extends the Apache
 * commons-cli Options object and disables methods we do not want used. This
 * class exists to ensure we use the AMPCS class in our interfaces, and not the
 * Apache classes directly, and that we add only ICommandLineOption and not
 * Option to the set. Someday we might need to replace the Apache classes.
 * 
 *
 */
public class OptionSet {

    private Options innerOptions = new Options();

    /**
     * Adds a ICommandLineOption object to this OptionSet. Overrides
     * the Apache implementation to use ICommandLineOption instead of
     * Option.
     * 
     * @param opt
     *            the ICommandLineOption to add
     * @return this
     */
    public OptionSet addOption(ICommandLineOption<?> opt) {
        innerOptions.addOption((Option)opt);
        return this;
    }

    /**
     * Gets the ICommandLineOption object with the given option
     * name (long or short)
     * @param opt the option name
     * @return the matching ICommandLineOption, or null if no match found
     */
    public ICommandLineOption<?> getOption(String opt) {
        return (ICommandLineOption<?>) innerOptions.getOption(opt);
    }
   

    /**
     * Gets a list of all the defined ICommandLineOption objects.
     * 
     * @return List of ICommandLineOption
     */
    public List<ICommandLineOption<?>> getAllOptions() {
        Collection<Option> apacheOptions = innerOptions.getOptions();
        List<ICommandLineOption<?>> ourOptions = new LinkedList<ICommandLineOption<?>>();
        for (Option o : apacheOptions) {
            ourOptions.add((ICommandLineOption<?>) o);

        }
        return ourOptions;
    }
    
    /**
     * Prints formatted option help to the specified PrintWriter.
     * 
     * @param pw PrintWriter for output
     * 
     */
    public void printOptions(PrintWriter pw) {
        HelpFormatter formatter = new HelpFormatter();
        Collection<ICommandLineOption<?>> allOptions = getAllOptions();
       
        
        // OptionSet that will not contain the hidden options
        OptionSet newOptionsList = new OptionSet();
        
        for(ICommandLineOption<?> opt: allOptions) {
            if (!opt.isHidden()) {
                newOptionsList.addOption(opt);
            }
       
        }
        formatter.printOptions(pw, 80, newOptionsList.getInnerOptions(), 3, 2);
    }

    /**
     * Gets the inner Apache Options object. Deliberately package-protected.
     * 
     * @return Apache Options object
     */
    Options getInnerOptions() {
        return innerOptions;
    }
}
