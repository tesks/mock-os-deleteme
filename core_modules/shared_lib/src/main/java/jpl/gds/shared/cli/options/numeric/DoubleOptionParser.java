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
 * A command line option parser for CommandLineOptions whose argument is a double.
 * By definition, this parser is a validating parser.
 * 
 */
public class DoubleOptionParser extends AbstractRangeCheckingOptionParser<Double> {
    
    /**
     * Constructor for a non-validating double option parser.
     */
    public DoubleOptionParser() {
        super();
    }
    
    /**
     * Constructor for a validating double option parser.
     * 
     * @param lowestValue the lowest allowed value (inclusive); null to indicate no lower bound
     * @param highestValue the highest allowed value (inclusive); null to indicate no upper bound
     */
    public DoubleOptionParser(Double lowestValue, Double highestValue) {
        super(lowestValue, highestValue);
    }
    
    /**
     * @see jpl.gds.shared.cli.options.IRangeOptionParser#setMinValue(java.lang.Object)
     */
    public void setMinValue(Double lowestValue) {
        if (lowestValue == null) {
            // : The MIN_VALUE for double is NOT Double.MIN_VALUE, but
            // rather the negative of MAX_VALUE. Really.
            super.setMinValue(-Double.MAX_VALUE);
        } else {
            super.setMinValue(lowestValue);
        }
    }
    
    /**
     * @see jpl.gds.shared.cli.options.IRangeOptionParser#setMaxValue(java.lang.Object)
     */
    public void setMaxValue(Double highestValue) {
        if (highestValue == null) {
            super.setMaxValue(Double.MAX_VALUE);
        } else {
            super.setMaxValue(highestValue);
        }
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.cmdline.IOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine, jpl.gds.shared.cli.options.ICommandLineOption)
     */
    @Override
    public Double parse(ICommandLine commandLine, ICommandLineOption<Double> opt) throws ParseException {

        String value = getValue(commandLine, opt);
        
        if (value == null) {
            return null;         
        }
       
        double doubleValue = 0;
        try {
            doubleValue = Double.valueOf(value);
        } catch (NumberFormatException e) {
            throw new ParseException(THE_VALUE + opt.getLongOpt() +
                    " must be a double-precision floating point number");
        }
        
        if (getValidate()) {
            if(doubleValue > maxValue.doubleValue()) {
                throwOutOfRange(opt); 
            }
            if(doubleValue < minValue.doubleValue()) {
                throwOutOfRange(opt); 
            }
        }
        return doubleValue;
    }

}
