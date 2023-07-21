/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.shared.cli.options.numeric;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.AbstractOptionParser;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * A command line option parser for CommandLineOptions whose argument is an unsigned integer.
 * By definition, this parser is a validating parser.
 * Unlike UnsignedIntegerOptionParser, this parser does not require that the valid values
 * be a consecutive range.
 * 
 */
public class DiscreteUnsignedIntOptionParser extends AbstractOptionParser<UnsignedInteger> {

	private final List<UnsignedInteger> validValues;
	
	/**
	 * Constructor for a discrete UnsignedInteger option parser with a set of valid values
	 * 
	 * @param validValues
	 *        the list of UnsignedInteger valid values
	 */
	public DiscreteUnsignedIntOptionParser(final List <UnsignedInteger> validValues) {
		super();
		this.validValues = validValues;
	}
	
	/**
	 * Constructor for a discrete UnsignedInteger option parser with all UnsignedInteger values valid
	 */
	public DiscreteUnsignedIntOptionParser() {
		super();
		this.validValues = new ArrayList<>();
	}
	
	@Override
	public UnsignedInteger parse(final ICommandLine commandLine, final ICommandLineOption<UnsignedInteger> opt) throws ParseException {
		final String str = getValue(commandLine,opt);

		if (str == null) {
			return null;
		}
		
		UnsignedInteger intValue = UnsignedInteger.valueOf(0);
		
		try {
            intValue = UnsignedInteger.valueOf(str);
        } catch (final NumberFormatException e) {
            throw new ParseException(THE_VALUE + opt.getLongOpt() +
                    " must be an UnsignedInteger");
        }
  
        if (getValidate()) {
        	if(!validValues.contains(intValue)) {
        		throwNotInSet(opt);
        	}
        }
        return intValue;
	}
	
	protected void throwNotInSet(final ICommandLineOption<?> opt) throws ParseException {
		String valid = validValues.toString();
		valid = valid.substring(1, valid.length() - 1);
		throw new ParseException(THE_VALUE + opt.getLongOpt() + " is not a valid value for this set: " + valid);
	}
}
