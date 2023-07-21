package jpl.gds.tc.mps.impl.ctt.repo;

import jpl.gds.tc.api.exception.CommandFileParseException;

/**
 * Interface for CTS cxml command dictionary compiler
 *
 */
public interface ICtsCxml {
    /**
     * Get the version of CTS cxml
     *
     * @return Version
     */
    String getCtsVersion();

    /**
     * Compile the command dictionary, return the paths
     *
     * @param cmdDictPath      path to command dictionary
     * @param validateSchema   validate schema option passed to cxml
     * @param compiledBasePath path to directory where compiled dictionaries will be written
     * @param basename         desired base file name of compiled command dictionary
     * @return compiled dictionary paths
     * @throws CommandFileParseException When exception occurs
     */
    CommandTranslationTablePaths compileCommandDictionary(
            String cmdDictPath,
            boolean validateSchema,
            String compiledBasePath,
            String basename) throws
                             CommandFileParseException;
}
