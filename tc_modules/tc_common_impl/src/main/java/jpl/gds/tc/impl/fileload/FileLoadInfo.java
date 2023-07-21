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

package jpl.gds.tc.impl.fileload;

import jpl.gds.tc.api.IFileLoadInfo;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.FileLoadParseException;

/**
 * This class is used to represent a one triple of input information that contains the file type, input file, and output file. The input
 * triple is the format used by the user either via the chill_send_file command line or via the File Load Window to input
 * information about a file to be uplinked.
 * 
 *
 */
public class FileLoadInfo implements IFileLoadInfo
{
	/** The type of file...sequence or non-sequence */
	private byte fileType;

	/** The location of the input file */
	private String inputFilePath;

	/** The target (output) file location */
	private String targetFilePath;
	
	/** 
	 * True if the file load should overwrite an existing onboard file of the same name,
	 * false otherwise.
	 */
	private boolean overwrite;

	private final CommandProperties cmdConfig;
	
	/** 
	 * Creates an instance of FileLoadInfo.
	 */
	public FileLoadInfo(final CommandProperties cmdConfig)
	{
		this.cmdConfig = cmdConfig;
		this.fileType = DEFAULT_FILE_TYPE;
		this.inputFilePath = null;
		this.targetFilePath = null;
		this.overwrite = false;
	}

	/**
	 * 
	 * Creates an instance of FileLoadInfo.
	 * 
	 * @param fileType The numeric type of the file (mission dependent).
	 * @param inputFilePath The path to the file on local disk that should be uplinked
	 * @param targetFilePath The path to where the file should be placed on the flight filesystem
	 * @param overwrite True if the file load should overwrite an existing onboard file of the same name,
	 * false otherwise.
	 * @throws FileLoadParseException If there's an error parsing the user input information 
	 */
	public FileLoadInfo(final CommandProperties cmdConfig, final byte fileType, final String inputFilePath, final String targetFilePath, final boolean overwrite) throws FileLoadParseException
	{
		this(cmdConfig);

		setFileType(fileType);
		this.inputFilePath = inputFilePath;
		this.targetFilePath = targetFilePath;
		this.overwrite = overwrite;
	}
	
	/**
	 * 
	 * Creates an instance of FileLoadInfo.
	 * 
	 * @param fileType The human-readable name for the type of the file (mission dependent).
	 * @param inputFilePath The path to the file on local disk that should be uplinked
	 * @param targetFilePath The path to where the file should be placed on the flight filesystem
	 * @param overwrite True if the file load should overwrite an existing onboard file of the same name,
	 * false otherwise.
	 * @throws FileLoadParseException If there's an error parsing the user input information 
	 */
	public FileLoadInfo(final CommandProperties cmdConfig, final String fileType, final String inputFilePath, final String targetFilePath, final boolean overwrite) throws FileLoadParseException
	{
		this(cmdConfig);

		setFileType(fileType);
		this.inputFilePath = inputFilePath;
		this.targetFilePath = targetFilePath;
		this.overwrite = overwrite;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public byte getFileType()
	{
		return this.fileType;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setFileType(final byte fileType) throws FileLoadParseException
	{
		setFileType(Byte.toString(fileType));
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    public void setFileType(final String fileType) throws FileLoadParseException
	{
		this.fileType = cmdConfig.mapFileLoadTypeToInteger(fileType);
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    public String getFileTypeString()
	{
		return(cmdConfig.mapFileLoadTypeToString(this.fileType));
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String getInputFilePath()
	{
		return this.inputFilePath;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setInputFilePath(final String inputFilePath)
	{
		this.inputFilePath = inputFilePath;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String getTargetFilePath()
	{
		return this.targetFilePath;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setTargetFilePath(final String targetFilePath)
	{
		this.targetFilePath = targetFilePath;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public boolean isOverwrite()
	{
		return overwrite;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setOverwrite(final boolean overwrite)
	{
		this.overwrite = overwrite;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder(1024);
		
		sb.append("FileLoad(localFile=\"");
		sb.append(this.inputFilePath);
		sb.append("\", targetFile=\"");
		sb.append(this.targetFilePath);
		sb.append("\", fileType=\"");
		sb.append(this.fileType);
		sb.append("\", overwrite=\"");
		sb.append(this.overwrite);
		sb.append("\")");
		
		return(sb.toString());
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTransmitSummary() {
	    return toString();
    }

}