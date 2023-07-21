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
package jpl.gds.tc.api;

import jpl.gds.tc.api.config.FileLoadParseException;

public interface IFileLoadInfo extends ISendCompositeState {

    /** The default file type if not specified. */
    byte DEFAULT_FILE_TYPE = 0;

    /**
     * Accessor for the numeric file type
     * 
     * @return Returns the fileType.
     */
    byte getFileType();

    /**
     * Mutator for the numeric file type
     * 
     * @param fileType
     *            The fileType to set.
     * @throws FileLoadParseException If the file type can't be interpreted
     */
    void setFileType(byte fileType) throws FileLoadParseException;

    /**
     * Mutator for the file type
     * 
     * @param fileType The human-readable representation of the file type
     * 
     * @throws FileLoadParseException If the file type can't be interpreted
     */
    void setFileType(String fileType) throws FileLoadParseException;

    /**
     * Accessor for the file type as a string
     * 
     * @return The human-readable name of the current file type
     */
    String getFileTypeString();

    /**
     * Accessor for the input file
     * 
     * @return Returns the inputFilePath.
     */
    String getInputFilePath();

    /**
     * Mutator for the input file
     * 
     * @param inputFilePath
     *            The inputFilePath to set.
     */
    void setInputFilePath(String inputFilePath);

    /**
     * Accessor for the target file
     * 
     * @return Returns the targetFilePath.
     */
    String getTargetFilePath();

    /**
     * Mutator for the target file
     * 
     * @param targetFilePath
     *            The targetFilePath to set.
     */
    void setTargetFilePath(String targetFilePath);

    /**
     * Accessor for the overwrite flag
     * 
     * @return True if this file load should overwrite an existing one on the flight system, false otherwise
     */
    boolean isOverwrite();

    /**
     * Mutator for the overwrite flag
     * 
     * @param overwrite True if this file load should overwrite an existing one on the flight system, false otherwise
     */
    void setOverwrite(boolean overwrite);

}