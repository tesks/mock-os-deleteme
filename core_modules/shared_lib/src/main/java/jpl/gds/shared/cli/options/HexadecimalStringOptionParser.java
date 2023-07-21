package jpl.gds.shared.cli.options;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.cmdline.ICommandLine;

/**
 * An option parser for StringOption that validates the string to verify it is a
 * valid String representation of a hexadecimal value.
 * 
 * If a byte divisible value is requested, a zero (0) will be prepended to the
 * hex string.
 * 
 *
 *
 */
public class HexadecimalStringOptionParser extends StringRegexOptionParser {

    /** the regex for hex values */
    public static final String HEX_REGEX = "[A-F,0-9]+";
    
    /** If the parsed string will be byte divisible (an even number of characters) */
    protected boolean byteDivisible = false;
    
    /**
     * Constructor.
     */
    public HexadecimalStringOptionParser() {
        super(HEX_REGEX);
    }
    
    /**
     * Constructor used to optionally force the parsed hex string to be byte
     * divisible.
     * 
     * @param byteDivisible
     *            TRUE if the hex string is to be byte divisible, FALSE
     *            otherwise
     */
    public HexadecimalStringOptionParser(final boolean byteDivisible) {
        super(HEX_REGEX);
        this.byteDivisible = byteDivisible;
    }
    
 
    @Override
    public String parse(final ICommandLine commandLine, final ICommandLineOption<String> opt) throws ParseException {
        String value = getValue(commandLine, opt);

        if (value == null) {
            return value;
        }

        value = value.toUpperCase();

        if (getValidate() && !value.matches(allowedRegex)) {
            throw new ParseException(THE_VALUE + opt.getLongOpt() + " ( " + value
                    + " ) does not fit the required regular expression: " + allowedRegex);
        }

        if (byteDivisible && value.length() % 2 == 1) {
            value = "0" + value;
        }

        return value;
    }
}
