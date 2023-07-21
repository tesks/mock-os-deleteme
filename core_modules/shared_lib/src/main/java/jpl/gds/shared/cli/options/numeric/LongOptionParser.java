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
 * A command line option parser for CommandLineOptions whose argument is a long.
 * By definition, this parser is a validating parser.
 * 
 */
public class LongOptionParser extends AbstractRangeCheckingOptionParser<Long> {

    /**
     * Constructor for a long option parser with default range.
     */
    public LongOptionParser() {
        super();
    }
    
    /**
     * Constructor for a long option parser with restricted range.
     * 
     * @param lowestValue the lowest allowed value (inclusive); null to indicate no lower bound
     * @param highestValue the highest allowed value (inclusive); null to indicate no upper bound
     */
    public LongOptionParser(Long lowestValue, Long highestValue) {
        super(lowestValue, highestValue);
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.IRangeOptionParser#setMinValue(java.lang.Object)
     */
    public void setMinValue(Long lowestValue) {
        if (lowestValue == null) {
            super.setMinValue(Long.MIN_VALUE);
        } else {
            super.setMinValue(lowestValue);
        }
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.IRangeOptionParser#setMaxValue(java.lang.Object)
     */
    public void setMaxValue(Long highestValue) {
        if (highestValue == null) {
            super.setMaxValue(Long.MAX_VALUE);
        } else {
            super.setMaxValue(highestValue);
        }
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.cmdline.IOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine, jpl.gds.shared.cli.options.ICommandLineOption)
     */
    @Override
    public Long parse(ICommandLine commandLine, ICommandLineOption<Long> opt) throws ParseException {

        String value = getValue(commandLine, opt);
        if (value == null) {
            return null; 
          
        }
        Long longValue = Long.valueOf(0);
        try {
            longValue = Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new ParseException(THE_VALUE + opt.getLongOpt() +
                    " must be a long integer");
        }
        
        if (getValidate()) {
            if(longValue > maxValue) {
                throwOutOfRange(opt); 
            }
            if(longValue < minValue) {
                throwOutOfRange(opt); 
            }
        }
        return longValue;
    }
    
}
