package jpl.gds.tcapp.app.reverse;

/**
 * Common Interface for data writers
 *
 */
public interface IDataWriter {

    /**
     * Sets whether or not the writer should suppress console output (for test automation)
     *
     * @param quiet whether or not the output should be hidden from console
     */
    void setSuppressOutput(final boolean quiet);

    /**
     * Whether or not output should be suppressed
     * @return true if output should be suppressed, false otherwise
     */
    boolean getSuppressOutput();
}
