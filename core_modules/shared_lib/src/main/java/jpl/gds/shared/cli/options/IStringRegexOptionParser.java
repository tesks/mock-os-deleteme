package jpl.gds.shared.cli.options;

/**
 * An interface for the parser that verifies a String will match a supplied
 * regular expression
 * 
 *
 */
public interface IStringRegexOptionParser {

    /**
     * Sets the regular expression if the supplied value is a valid regular
     * expression
     * 
     * @param allowedRegex
     *            the new regular expression to be used for validating
     */
    public void setAllowedRegex(String allowedRegex);
    
    /**
     * Get the current valid regular expression used for validating, or null if
     * no expression
     * 
     * @return the current regular expression
     */
    public String getAllowedRegex();
}
