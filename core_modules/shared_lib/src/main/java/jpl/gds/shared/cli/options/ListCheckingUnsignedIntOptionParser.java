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
package jpl.gds.shared.cli.options;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.types.UnsignedInteger;
  /**
     * A base option parser class for UnsignedInt options that have a list of
     * valid values.
     * 
     *
     */
    public class ListCheckingUnsignedIntOptionParser extends
            AbstractListCheckingOptionParser<UnsignedInteger> {

        /**
         * Constructor.
         */
        public ListCheckingUnsignedIntOptionParser() {
            super();
            setValidate(true);
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.numeric.UnsignedIntOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public UnsignedInteger parse(final ICommandLine commandLine,
                final ICommandLineOption<UnsignedInteger> opt) throws ParseException {

            final String value = getValue(commandLine, opt);
            if (value == null) {
                return null;

            }
            UnsignedInteger val = null;
            try {
                final Integer intValue = Integer.valueOf(value);
                val = UnsignedInteger.valueOf(intValue);
            } catch (final IllegalArgumentException e) {
                throw new ParseException(THE_VALUE + opt.getLongOpt()
                        + " must be an unsigned integer");
            }

            super.checkValueInList(opt, val, true);

            return val;

        }
    }
