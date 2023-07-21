package jpl.gds.shared.cli.options;

/**
 * An interface for the option parser that validates that a StringOption is a
 * valid hexadecimal string
 * 
 *
 */
public interface IHexadecimalStringOptionParser {

    /**
     * Set if the parsed hex string will be verified to be byte divisible.
     * @param byteDivisible TRUE if the hex string is to be byte divisible, FALSE if not
     */
    public void setByteDivisible(boolean byteDivisible);
    
    /**
     * Get if the parsed hex string will be byte divisible or not
     * 
     * @return TRUE if the hex string will be byte divisible, FALSE otherwise
     */
    public boolean isByteDivisible();
    
}
