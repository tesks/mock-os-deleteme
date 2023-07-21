package jpl.gds.shared.cli.options;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.cmdline.ICommandLine;

/**
 * Class StringRegexOptionParser
 *
 */
public class StringRegexOptionParser extends AbstractOptionParser<String> implements IStringRegexOptionParser {

    /** the string representation of the regular expression to be used by this parser for comparison*/
    protected String allowedRegex;
    
    /**
     * Constructor. Does not need to validate and has no value for the regex.
     */
    public StringRegexOptionParser(){
        super(false);
    }
    
    /**
     * Constructor that sets the regular expression
     * @param allowedRegex the desired regular expression to be used by this parser.
     */
    public StringRegexOptionParser(final String allowedRegex) {
        super(true);
        setAllowedRegex(allowedRegex);
    }
    
    @Override
    public void setAllowedRegex(final String allowedRegex) {
        if(allowedRegex == null){
            this.allowedRegex = null;
            setValidate(false);
            return;
        }
        
        // Made the throw rather than failing silently
        try{
            Pattern.compile(allowedRegex);
        } catch (final PatternSyntaxException e){
            throw new IllegalArgumentException("Invalid regex pattern:" + allowedRegex);
        }
        
        this.allowedRegex = allowedRegex;
        setValidate(true);
    }
    
    @Override
    public String getAllowedRegex() {
        return this.allowedRegex.toString();
    }

    @Override
    public String parse(final ICommandLine commandLine, final ICommandLineOption<String> opt) throws ParseException {
        final String value = getValue(commandLine, opt);

        if(getValidate() && value != null && !value.matches(allowedRegex)) {
            throw new ParseException(THE_VALUE + value +
                  " does not fit the required regular expression: " + allowedRegex);
        }

        return value;
    }

}
