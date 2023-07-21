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
import jpl.gds.shared.types.UnsignedLong;


/**
 * A command line option parser for CommandLineOptions whose argument is an
 * unsigned long. By definition, this parser is a validating parser. Always
 * validates lower bound, which defaults to 0. Optionally, can be supplied with
 * an upper bound.
 * 
 */
public class UnsignedLongOptionParser extends AbstractRangeCheckingOptionParser<UnsignedLong> {
    
    /**
     * Constructor for an unsigned long parser with default range.
     */
    public UnsignedLongOptionParser() {
        super();
    }

    /**
     * Constructor for a sn unsigned long parser with
     * default lower bound (0) and specified upper bound.
     * 
     * @param highestValue the maximum allowed value (inclusive)
     */
    public UnsignedLongOptionParser(UnsignedLong highestValue) {
        super();
        setMaxValue(highestValue);
    }
    
    /**
     * Constructor for a validating unsigned long parser with restricted range.
     * 
     * @param lowestValue the maximum allowed value (inclusive)
     * @param highestValue the maximum allowed value (inclusive)
     */
    
    public UnsignedLongOptionParser(UnsignedLong lowestValue, UnsignedLong highestValue) {
        super(lowestValue, highestValue);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.cmdline.IOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine, jpl.gds.shared.cli.options.ICommandLineOption)
     */
    @Override
    public UnsignedLong parse(ICommandLine commandLine, ICommandLineOption<UnsignedLong> opt) throws ParseException {

        String value = getValue(commandLine, opt);
        if (value == null) {
            return null; 
          
        }
        UnsignedLong unsignedValue = null;
        try {
            unsignedValue = UnsignedLong.valueOf(value);
        } catch (final NumberFormatException e) {
            throw new ParseException(THE_VALUE + opt.getLongOpt() +
                    " must be an unsigned long integer");
        }
 
        if (getValidate()) {
            if(unsignedValue.compareTo(maxValue) > 0) {
                throwOutOfRange(opt); 
            }
            if(unsignedValue.compareTo(minValue) < 0) {
                throwOutOfRange(opt); 
            }
        }
        return unsignedValue;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.numeric.IntegerOptionParser#setMaxValue(java.lang.Integer)
     */
    public void setMaxValue(UnsignedLong highestValue) {
       
        if (highestValue == null) {
            super.setMaxValue(UnsignedLong.MAX_VALUE);
        } else {
            super.setMaxValue(highestValue);
        }
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.numeric.IntegerOptionParser#setMinValue(java.lang.Integer)
     */
    public void setMinValue(UnsignedLong lowestValue) {
        if (lowestValue == null) {
            super.setMinValue(UnsignedLong.MIN_VALUE);
        } else {
            super.setMinValue(lowestValue);
        }
    }
   
}
