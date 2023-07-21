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
 * A command line option parser for CommandLineOptions whose argument is a float.
 * By definition, this parser is a validating parser.
 * 
 */
public class FloatOptionParser extends AbstractRangeCheckingOptionParser<Float> {
    
    /**
     * Constructor for a non-validating float option parser.
     */
    public FloatOptionParser() {
        super();
    }
    
    /**
     * Constructor for a validating float option parser.
     * 
     * @param lowestValue the lowest allowed value (inclusive); null to indicate no lower bound
     * @param highestValue the highest allowed value (inclusive); null to indicate no upper bound
     */
    public FloatOptionParser(Float lowestValue, Float highestValue) {
        super(lowestValue, highestValue);
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.IRangeOptionParser#setMinValue(java.lang.Object)
     */
    public void setMinValue(Float lowestValue) {
        if (lowestValue == null) {
            // The MIN_VALUE for float is NOT Float.MIN_VALUE, but
            // rather the negative of MAX_VALUE. Really.
            super.setMinValue(-Float.MAX_VALUE);
        } else {
            super.setMinValue(lowestValue);
        }
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.IRangeOptionParser#setMaxValue(java.lang.Object)
     */
    public void setMaxValue(Float highestValue) {
        if (highestValue == null) {
            super.setMaxValue(Float.MAX_VALUE);
        } else {
            super.setMaxValue(highestValue);
        }
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.cmdline.IOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine, jpl.gds.shared.cli.options.ICommandLineOption)
     */
    @Override
    public Float parse(ICommandLine commandLine, ICommandLineOption<Float> opt) throws ParseException {

        String value = getValue(commandLine, opt);
        
        if (value == null) {
            return null;         
        }
       
        float floatValue = 0;
        try {
            floatValue = Float.valueOf(value);
        } catch (NumberFormatException e) {
            throw new ParseException(THE_VALUE + opt.getLongOpt() +
                    " must be a float-precision floating point number");
        }
        
        if (getValidate()) {
            if(floatValue > maxValue.floatValue()) {
                throwOutOfRange(opt); 
            }
            if(floatValue < minValue.floatValue()) {
                throwOutOfRange(opt); 
            }
        }
        return floatValue;
    }

}
