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

import jpl.gds.tc.api.command.IDatabaseArchivableCommand;

public interface ICommandFileLoad extends IDatabaseArchivableCommand {

    /** The max size permitted for the file load data */
    public static final int MAX_FILE_LOAD_DATA_BYTE_SIZE = 260220;
    /** The max size of the target file name */
    public static final int MAX_FILE_NAME_BYTE_SIZE = 122;
    /** The length of the file type field in bits */
    public static final short FILE_TYPE_BIT_LENGTH = 7;

    /**
     * Get the binary representation of the header for a file load
     * that will be transmitted to the spacecraft.
     * 
     * @return The byte array representation of the file load header.
     */
    public byte[] getFileLoadHeaderBytes();

    /**
     * Get the byte representation of this command file load object (header and data)
     *
     * @return The byte array representing all the values in this class
     * (The format of this array can be found in the MSL FGICD Vol. II)
     */
    public byte[] getFileLoadBytes();

    /**
     * Given a byte array representation of a command file load, parse it and set all of its
     * contained values on this object.
     *
     * @param fileLoadBytes The byte array containing the entire command file load
     *
     * @param inOffset The offset into the input byte array where the command file
     * load begins
     */
    public void setFileLoadFromBytes(byte[] fileLoadBytes, int inOffset);

    /**
     * Set the file processing flag values
     *
     * @param input The byte representation of the file processing flag
     */
    public void setFileProcessingFlag(byte input);

    /**
     * Get the byte representation of the file processing flag
     * (this is the combination of the overwrite flag and file type).
     *
     * @return The byte representing the file processing flag
     */
    public byte getFileProcessingFlag();

    /**
     * Compute the CRC for this file load
     *
     */
    public void computeCrc();

    /**
     * Accessor for the byte length of the target file
     * 
     * @return Returns the fileLength.
     */
    public int getFileByteLength();

    /**
     * Accessor for the data portion of the file load
     * 
     * @return Returns the data.
     */
    public byte[] getData();

    /**
     * Mutator for the data portion of the file load
     *@param fileLoadData the file data
     *@param offset the offset into the data at which to start
     *@param length the number of bytes to pull from the input data
     */
    public void setFileLoadData(byte[] fileLoadData, int offset, int length);

    /**
     * Accessor for the target file name
     *
     * @return Returns the fileName.
     */
    public String getFileName();

    /**
     * Mutator for the target file name
     *
     * @param fileName The fileName to set.
     */
    public void setFileName(String fileName);

    /**
     * Accessor for the target file name byte length
     * 
     * @return Returns the fileNameLength.
     */
    public byte getFileNameByteLength();

    /**
     * Get a string representation of the file load header.
     * 
     * @return A String representation of the file load header for this file load
     */
    public String getHeaderString();

    /**
     * Accessor to determine whether or not this is a partial file load
     * (a partial is a piece of a file that had to be split because it
     * was too big).
     * 
     * @return Returns the partialFileLoad.
     */
    public boolean isPartialFileLoad();

    /**
     * Mutator to set whether or not this is a partial file load
     * (a partial is a piece of a file that had to be split because it
     * was too big).
     *
     * @param partialFileLoad The partialFileLoad value to set.
     */
    public void setPartialFileLoad(boolean partialFileLoad);

    /** 
     * Accessor for the source file name
     * 
     * @return Returns the inputFileName.
     */
    public String getInputFileName();

    /**
     * Mutator for the source file name
     *
     * @param inputFileName The inputFileName to set.
     */
    public void setInputFileName(String inputFileName);

    /**
     * Accessor for the part number.
     * (a part number is attached to each partial file load
     * identifying what part of the split larger file this
     * file load is).
     * 
     * @return Returns the partNumber.
     */
    public int getPartNumber();

    /**
     * Mutator for the part number.
     * (a part number is attached to each partial file load
     * identifying what part of the split larger file this
     * file load is).
     *
     * @param partNumber The partNumber to set.
     */
    public void setPartNumber(int partNumber);

    /**
     * Accessor for the numeric file type
     * 
     * @return Returns the fileType.
     */
    public byte getFileType();

    /**
     * Mutator for the numeric file type
     *
     * @param fileType The fileType to set.
     */
    public void setFileType(byte fileType);

    /**
     * Accessor for the overwrite flag
     * 
     * @return Returns the overwriteFlag.
     */
    public boolean isOverwriteFlag();

    /**
     * Mutator for the overwrite flag
     *
     * @param overwriteFlag The overwriteFlag to set.
     */
    public void setOverwriteFlag(boolean overwriteFlag);

    /**
     * Accessor for the CRC (no calculation done here...this value
     * must have been set already).
     * 
     * @return Returns the crc.
     */
    public long getCrc();

    /**
     * Mutator for the CRC (no calculation done here...just sets the
     * CRC to the input value).
     *
     * @param crc The crc to set.
     */
    public void setCrc(long crc);

}