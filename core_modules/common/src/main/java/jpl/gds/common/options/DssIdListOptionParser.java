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
package jpl.gds.common.options;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.ParseException;

import jpl.gds.common.filtering.DssIdFilter;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.AbstractOptionParser;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * An option parser for a list of DSS IDs to filter for. Splits a string by a comma. 
 * Allows a NONE value for matching things with no station, if so configured. Also
 * allows checking against a list of valid stations IDs. Returns a DssIdFilter object,
 * or null if the option was not provided on the command line. 
 * 
 *
 */
public class DssIdListOptionParser extends AbstractOptionParser<DssIdFilter> {
    
    /** Value that represents undefined DSS ID */
    public static final String NONE_VALUE = "NONE";

	private final boolean allowNone;
    private final List<UnsignedInteger> validValues;
	
	/**
	 * Constructor
	 * @param allowNone if the NONE station is allowed
	 * @param valid list of valid values; may be null
	 */
	public DssIdListOptionParser(final boolean allowNone, final List<UnsignedInteger> valid) {
		super();
		this.allowNone = allowNone;
		this.validValues = valid;
	}

	@Override
	public DssIdFilter parse(final ICommandLine commandLine, final ICommandLineOption<DssIdFilter> opt)
			throws ParseException {
        final String str = getValue(commandLine,opt);

        if (str == null) {
           return null;
           
        } else {

            boolean inputNone = false;
            
            final Collection<UnsignedInteger> csv = new ArrayList<>();
        	 
            for (final String s : str.trim().split(",")) {
                if (!s.isEmpty()) {
                    try {
                        final UnsignedInteger temp = UnsignedInteger.valueOf(s);
                        if (validValues != null && !validValues.contains(temp)) {
                            throw new ParseException(THE_VALUE + opt.getLongOpt() + " includes " + s + 
                                    " which is not a valid value in the current configuration");
                        }
                        csv.add(temp);
                    } catch (final NumberFormatException e) {
                        if (s.equalsIgnoreCase(NONE_VALUE)) {
                            if (!this.allowNone) {
                                throw new ParseException(THE_VALUE + opt.getLongOpt() + " does now allow " + NONE_VALUE + " in this context");
                            }
                            inputNone = true;
                        } else {
                            final StringBuilder errString = new StringBuilder(THE_VALUE + opt.getLongOpt()
                                    + " does not allow " + s + ". supplied values must include only unsigned integers");
                            if (this.allowNone) {
                                errString.append(" or " + NONE_VALUE);
                            }
                            throw new ParseException(errString.toString());
                        }
                    }
                }
            }
            
            return new DssIdFilter(csv, inputNone);
	   
        }
	}

	/**
	 * Gets the list of valid values for the option.
	 * 
	 * @return list of valid values; may be null
	 * 
	 */
    public List<UnsignedInteger> getValidValues() {
        return this.validValues;
    }
}
