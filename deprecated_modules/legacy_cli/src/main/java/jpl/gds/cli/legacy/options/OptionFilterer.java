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
package jpl.gds.cli.legacy.options;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import jpl.gds.shared.sys.SystemUtilities;

/**
 * Provides ability to have "hidden" command line options.  These options are 
 * filtered out so that they will not be shown in the help menu.
 * 
 *
 */
public class OptionFilterer extends HelpFormatter {
  private String[] filteredOpts;
  
  public OptionFilterer(String[] hiddenOptions) {
	  filteredOpts = hiddenOptions;
  }
 
  	public void printHelp( int width, String cmdLineSyntax, String header, Options options, String footer )
    {
  		// Set up iterator to traverse through all the given options
  		Collection<Option> opts =
            SystemUtilities.<Collection<Option>>castNoWarning(
                options.getOptions());

  		Iterator<Option> it = opts.iterator();
  		
  		// List that will not contain the filtered options
  		Options newOptionsList = new Options();
  		
  		for(String toBeFiltered : filteredOpts) {
  			while (it.hasNext()) {
			Option option = it.next();
			
			//only add options that aren't on the filtered list
	        if(!option.getLongOpt().equals(toBeFiltered)) {
	        	newOptionsList.addOption(option);
	        }
	    }
	}

    super.printHelp(80,"\n","\n",newOptionsList,"\n");
  }
}
