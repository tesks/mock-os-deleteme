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
package jpl.gds.shared.cli.options.numeric;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.AbstractRangeCheckingOptionParser;
import jpl.gds.shared.cli.options.ICommandLineOption;

/**
 * A command line option parser for CommandLineOptions whose argument is an integer.
 * By definition, this parser is a validating parser. 
 * 
 */
public class IntegerOptionParser extends AbstractRangeCheckingOptionParser<Integer> {

    /**
     * Constructor for an integer option parser with default range.
     */
    public IntegerOptionParser() {
        super();
    }
    
    /**
     * Constructor for an integer option parser with restricted range.
     * 
     * @param lowestValue the lowest allowed value (inclusive); null to indicate no lower bound
     * @param highestValue the highest allowed value (inclusive); null to indicate no upper bound
     */
    public IntegerOptionParser(Integer lowestValue, Integer highestValue) {
        super(lowestValue, highestValue);

    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.IRangeOptionParser#setMinValue(java.lang.Object)
     */
    public void setMinValue(Integer lowestValue) {
        if (lowestValue == null) {
            super.setMinValue(Integer.MIN_VALUE);
        } else {
            super.setMinValue(lowestValue);
        }
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.IRangeOptionParser#setMaxValue(java.lang.Object)
     */
    public void setMaxValue(Integer highestValue) {
        if (highestValue == null) {
            super.setMaxValue(Integer.MAX_VALUE);
        } else {
            super.setMaxValue(highestValue);
        }
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.cmdline.IOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine, jpl.gds.shared.cli.options.ICommandLineOption)
     */
    @Override
    public Integer parse(ICommandLine commandLine, ICommandLineOption<Integer> opt) throws ParseException {

        String value = getValue(commandLine, opt);
        if (value == null) {
            return null; 
          
        }
        Integer intValue = Integer.valueOf(0);
        try {
            intValue = Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw new ParseException(THE_VALUE + opt.getLongOpt() +
                    " must be an integer");
        }
  
        if (getValidate()) {
            if(intValue > maxValue) {
                throwOutOfRange(opt); 
            }
            if(intValue < minValue) {
                throwOutOfRange(opt); 
            }
        }
        return intValue;
    }
}
