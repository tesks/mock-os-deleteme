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

import java.util.zip.CRC32;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.ICommandFileLoad;
import jpl.gds.tc.api.config.CommandProperties;

/**
 * This class represents an uplink file load for the
 * "chill_send_file" tool.
 *
 * TODO: This may need to be made mission specific
 * (this class uses the MSL FGICD Volume II: Uplink representation)
 *
 *
 */
public class CommandFileLoad implements ICommandFileLoad {

    /** The max size of the file load header */
    protected static final int MAX_FILE_LOAD_HEADER_BYTE_SIZE = 132;
    /** The length in bytes of the file processing flag portion of the file load header */
    protected static final short FILE_PROCESSING_FLAG_BYTE_LENGTH = 1;
    /** The length in bytes of the file CRC field */
    protected static final short FILE_CRC_BYTE_LENGTH = 4;
    /** The length in bytes of the file length field */
    protected static final short FILE_LENGTH_BYTE_LENGTH = 4;
    /** The length in bytes of the file name length field */
    protected static final short FILE_NAME_LENGTH_BYTE_LENGTH = 1;

	/** True if this file load uses compression, false otherwise */
	private boolean overwriteFlag; //1 bit

	/** True if this file load contains a sequence file, false otherwise */
	private byte fileType; //7 bits

	/** The Cyclic Redundancy Check (CRC) value for the file being uplinked */
	private long crc;

	/** The length in bytes of the file data */
	private int fileByteLength; //32 bits

	/** The length in byte of the target file name */
	private byte fileNameByteLength; //8 bits

	/** The name/path of the target file */
	private String fileName; //8 to 976 bits

	/** The binary file data in this file load */
	private byte[] data; //1 to 260220 bytes

	/** True if this file load is a piece of a larger file that was split, false otherwise */
	private boolean partialFileLoad;

	/** The numbered part of the overall file that this file load constitutes */
	private int partNumber;

	/** The name of the input file used to generate this file load */
	private String inputFileName;
	
	private final ApplicationContext appContext;

	/**
	 * Creates an instance of CommandFileLoad.
	 * 
	 * @param appContext the ApplicationContext that in which this object is being used
	 */
	public CommandFileLoad(ApplicationContext appContext)
	{
		this.appContext = appContext;
		this.partNumber = 1;
		this.overwriteFlag = false;
		this.fileType = 0x00;
		this.fileByteLength = 0;
		this.fileNameByteLength = 0;
		this.fileName = null;
		this.data = null;
		this.partialFileLoad = false;
		this.inputFileName = "";
	}

	/**
	 *
	 * Creates an instance of CommandFileLoad from a byte array
	 *
	 * @param appContext the ApplicationContext that in which this object is being used
	 *
	 * @param fileLoadBytes The byte array containing the entire command file load
	 *
	 * @param offset The offset into the input byte array where the command file
	 * load begins
	 *
	 * the calculated CRC over the same data.
	 */
	public CommandFileLoad(ApplicationContext appContext, final byte[] fileLoadBytes, final int offset)
	{
		this(appContext);

		if(fileLoadBytes == null)
		{
			throw new IllegalArgumentException("Null input byte array");
		}
		else if(offset < 0)
		{
			throw new IllegalArgumentException("Negative input offset");
		}
		else if(offset >= fileLoadBytes.length)
		{
			throw new IllegalArgumentException("Offset is past the end of the input byte array");
		}

		setFileLoadFromBytes(fileLoadBytes,offset);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#getFileLoadHeaderBytes()
     */
	@Override
    public byte[] getFileLoadHeaderBytes()
	{
		computeCrc();

		//Get the byte representation of the target file name
		final byte[] fileNameBytes = getFileName().getBytes();

		//make an array to hold the entire command file load header
		final byte[] headerBytes = new byte[FILE_PROCESSING_FLAG_BYTE_LENGTH +
		                                FILE_CRC_BYTE_LENGTH +
		                                FILE_LENGTH_BYTE_LENGTH +
		                                FILE_NAME_LENGTH_BYTE_LENGTH +
		                                fileNameBytes.length];

		int offset = 0;

		//set the file processing flag
		headerBytes[offset] = getFileProcessingFlag();
		offset += FILE_PROCESSING_FLAG_BYTE_LENGTH;

		//set the file CRC (this has already been calculated by this point)
		GDR.set_u32(headerBytes, offset, this.crc);
		offset += FILE_CRC_BYTE_LENGTH;

		//set the file byte length
		GDR.set_i32(headerBytes,offset,getFileByteLength());
		offset += FILE_LENGTH_BYTE_LENGTH;

		//set the file name byte length
		headerBytes[offset] = getFileNameByteLength();
		offset += FILE_NAME_LENGTH_BYTE_LENGTH;
		System.arraycopy(fileNameBytes,0,headerBytes,offset,fileNameBytes.length);
		offset += fileNameBytes.length;

		if(offset != headerBytes.length)
		{
			//TODO: log this exception at the very least
		}

		return(headerBytes);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#getFileLoadBytes()
     */
	@Override
    public byte[] getFileLoadBytes()
	{
		final byte[] headerBytes = getFileLoadHeaderBytes();
		final byte[] dataBytes = getData();

		final byte[] fileLoadBytes = new byte[headerBytes.length + dataBytes.length];

		System.arraycopy(headerBytes,0,fileLoadBytes,0,headerBytes.length);
		System.arraycopy(dataBytes,0,fileLoadBytes,headerBytes.length,dataBytes.length);

		return(fileLoadBytes);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#setFileLoadFromBytes(byte[], int)
     */
	@Override
    public void setFileLoadFromBytes(final byte[] fileLoadBytes, int inOffset)
	{
		int offset = inOffset;
		
		if(fileLoadBytes == null)
		{
			throw new IllegalArgumentException("Null input byte array");
		}
		else if(offset < 0)
		{
			throw new IllegalArgumentException("Negative input offset");
		}
		else if(offset >= fileLoadBytes.length)
		{
			throw new IllegalArgumentException("Offset is past the end of the input byte array");
		}

		//read the file processing flag
		setFileProcessingFlag(fileLoadBytes[offset]);
		offset += FILE_PROCESSING_FLAG_BYTE_LENGTH;

		//read in the CRC
		//long tempCrc = 
		setCrc(GDR.get_u32(fileLoadBytes,offset));
		offset += FILE_CRC_BYTE_LENGTH;

		//read in the file length
		setFileByteLength(GDR.get_i32(fileLoadBytes,offset));
		offset += FILE_LENGTH_BYTE_LENGTH;

		//read in the file name length
		setFileNameByteLength(fileLoadBytes[offset]);
		offset += FILE_NAME_LENGTH_BYTE_LENGTH;

		//read in the file name
		final byte[] fileNameBytes = new byte[getFileNameByteLength()];
		System.arraycopy(fileLoadBytes,offset,fileNameBytes,0,getFileNameByteLength());
		setFileName(new String(fileNameBytes));
		offset += getFileNameByteLength();

		//read in the file load data (or only part of it if we don't have everything)
		this.data = new byte[getFileByteLength()];
		if(this.data.length > (fileLoadBytes.length-offset))
		{
			this.data = new byte[(fileLoadBytes.length-offset)];
		}
		System.arraycopy(fileLoadBytes,offset,this.data,0,this.data.length);

		// MPCS-7006 - 09/24/19 - removed compute CRC - if we're loading a previously generated file load we want
		// to KEEP the old CRC.
		//check the CRC to make sure the file load data has no errors
		//computeCrc();
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#setFileProcessingFlag(byte)
     */
	@Override
    public void setFileProcessingFlag(final byte input)
	{
		final byte overwrite = (byte)((0x80 & input) >>> 7);
		setOverwriteFlag(overwrite == 1);
		setFileType((byte)(0x7f & input));
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#getFileProcessingFlag()
     */
	@Override
    public byte getFileProcessingFlag()
	{
		byte flag = 0x00;

		if(this.overwriteFlag == true)
		{
			flag = (byte)(flag | 0x80);
		}

		flag = (byte)(flag | (this.fileType & 0x7f));

		return(flag);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#computeCrc()
     */
	@Override
    public void computeCrc()
	{
		final byte[] data = getData();
		
		//luckily Java has a built-in CRC-32
		final CRC32 checksumAlgorithm = new CRC32();
		checksumAlgorithm.update(data);
		this.crc = checksumAlgorithm.getValue();

//The commented out code below pads out the file load data to a number of bytes
//that is divisible by 4.  Not sure if that's what we're supposed to be doing, so I'm
//just letting the Java-defined CRC-32 algorithm take care of it
//
//		int mod = data.length % 4;
//		byte[] paddedData = null;
//
//		if(mod == 1)
//		{
//			paddedData = new byte[data.length + 3];
//			paddedData[data.length] = 0x00;
//			paddedData[data.length+1] = 0x00;
//			paddedData[data.length+2] = 0x00;
//		}
//		else if(mod == 2)
//		{
//			paddedData = new byte[data.length + 2];
//			paddedData[data.length] = 0x00;
//			paddedData[data.length+1] = 0x00;
//		}
//		else if(mod == 3)
//		{
//			paddedData = new byte[data.length + 1];
//			paddedData[data.length] = 0x00;
//		}
//		else
//		{
//			paddedData = new byte[data.length];
//		}
//		System.arraycopy(data,0,paddedData,0,data.length);
//
//		//this.crc = RotatedXorChecksum.calculate32BitChecksum(paddedData);
//		CRC32 checksumAlgorithm = new CRC32();
//		checksumAlgorithm.update(paddedData);
//		this.crc = checksumAlgorithm.getValue();
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#getFileByteLength()
     */
	@Override
    public int getFileByteLength()
	{
		return this.fileByteLength;
	}

	/**
	 * Mutator for the byte length of the target file
	 *
	 * @param fileLength The fileLength to set.
	 */
	private void setFileByteLength(final int fileLength)
	{
		if(fileLength < 0)
		{
			throw new IllegalArgumentException("Negative input file length");
		}
		else if(fileLength > appContext.getBean(CommandProperties.class).getChunkSize())
		{
			throw new IllegalArgumentException("Input file length of " + fileLength + 
					" exceeds max allowable file length of " + appContext.getBean(CommandProperties.class).getChunkSize());
		}

		this.fileByteLength = fileLength;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#getData()
     */
	@Override
    public byte[] getData()
	{
		return this.data;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#setFileLoadData(byte[], int, int)
     */
	@Override
    public void setFileLoadData(final byte[] fileLoadData, final int offset,final int length)
	{
		if(fileLoadData == null)
		{
			throw new IllegalArgumentException("Null input byte array");
		}

		this.data = new byte[length];
		System.arraycopy(fileLoadData,offset,this.data,0,length);

		setFileByteLength(length);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#getFileName()
     */
	@Override
    public String getFileName()
	{
		return this.fileName;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#setFileName(java.lang.String)
     */
	@Override
    public void setFileName(final String fileName)
	{
		if(fileName == null)
		{
			throw new IllegalArgumentException("Null input filename");
		}
		else if(fileName.getBytes().length > MAX_FILE_NAME_BYTE_SIZE)
		{
			throw new IllegalArgumentException("The file name must not exceed " + MAX_FILE_NAME_BYTE_SIZE + " bytes");
		}

		this.fileName = fileName;
		setFileNameByteLength((byte)this.fileName.getBytes().length);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#getFileNameByteLength()
     */
	@Override
    public byte getFileNameByteLength()
	{
		return this.fileNameByteLength;
	}

	/**
	 * Mutator for the target file name byte length
	 *
	 * @param fileNameLength The fileNameLength to set.
	 */
	private void setFileNameByteLength(final byte fileNameLength)
	{
		this.fileNameByteLength = fileNameLength;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#getHeaderString()
     */
	@Override
    public String getHeaderString()
	{
		final StringBuilder buf = new StringBuilder(512);

		final byte flag = getFileProcessingFlag();
		final byte overwriteFlag = (byte)((flag & 0x80) >>> 7);
		final byte fileTypeFlag = (byte)(flag & 0x7f);

		buf.append("File Overwrite Flag = " + GDR.getBooleanFromInt(overwriteFlag) + "\n"
		+ "File Type = 0x" + BinOctHexUtility.toHexFromByte(fileTypeFlag) + "\n"
		+ "File CRC = 0x" + Long.toHexString(getCrc()) + "\n"
		+ "File Length = " + getFileByteLength() + "\n"
		+ "File Name = " + getFileName() + "\n"
	    + "File Name Length = " + getFileNameByteLength() + "\n");

		return(buf.toString());
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#isPartialFileLoad()
     */
	@Override
    public boolean isPartialFileLoad()
	{
		return this.partialFileLoad;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#setPartialFileLoad(boolean)
     */
	@Override
    public void setPartialFileLoad(final boolean partialFileLoad)
	{
		this.partialFileLoad = partialFileLoad;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#getInputFileName()
     */
	@Override
    public String getInputFileName()
	{
		return this.inputFileName;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#setInputFileName(java.lang.String)
     */
	@Override
    public void setInputFileName(final String inputFileName)
	{
		this.inputFileName = inputFileName;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#getPartNumber()
     */
	@Override
    public int getPartNumber()
	{
		return this.partNumber;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#setPartNumber(int)
     */
	@Override
    public void setPartNumber(final int partNumber)
	{
		this.partNumber = partNumber;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#getDatabaseString()
     */
    @Override
	public String getDatabaseString()
	{
		final StringBuilder dbString = new StringBuilder();

		dbString.append(this.fileType 
		+ "," + this.inputFileName
		+ "," + this.fileName);

		return(dbString.toString());
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return(getDatabaseString());
	}

	/**
	 * Given a database string representation of a file load, convert it back into
	 * a file load object.
	 * 
	 * @param appContext the ApplicationContext in which this object is being used
	 * 
	 * @param dbString The string from the MPCS database for a file load
	 * 
	 * @return A file load object corresponding to the input database string
	 */
	public static ICommandFileLoad parseDatabaseString(ApplicationContext appContext, final String dbString)
	{
		final ICommandFileLoad cfl = new CommandFileLoad(appContext);
		final String[] parts = dbString.split(",");
		cfl.setFileType(Byte.parseByte(parts[0]));
		cfl.setInputFileName(parts[1]);
		cfl.setFileName(parts[2]);
		return(cfl);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#getFileType()
     */
	@Override
    public byte getFileType()
	{
		return this.fileType;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#setFileType(byte)
     */
	@Override
    public void setFileType(final byte fileType)
	{
		this.fileType = fileType;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#isOverwriteFlag()
     */
	@Override
    public boolean isOverwriteFlag()
	{
		return this.overwriteFlag;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#setOverwriteFlag(boolean)
     */
	@Override
    public void setOverwriteFlag(final boolean overwriteFlag)
	{
		this.overwriteFlag = overwriteFlag;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#getCrc()
     */
	@Override
    public long getCrc()
	{
		return this.crc;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.ICommandFileLoad#setCrc(long)
     */
	@Override
    public void setCrc(final long crc)
	{
		this.crc = crc;
	}
}