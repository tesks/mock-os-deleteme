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

package jpl.gds.shared.sfdu;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;

/**
 * This class represents a CCSDS SFDU label.
 * 
 */
public class SfduLabel 
{	
	/**
	 * Byte length of the SFDU control authority ID (CAID)
	 */
	public static final int CONTROL_AUTHORITY_ID_LENGTH = 4;

	private static final String ASCII_CHARSET = "US-ASCII";
	private static final String RESTRICTED_ASCII_REGEXP = "[0-9A-Z]{1,}";
	private static final int VERSION_ID_MIN_VALUE = 0;
	private static final int VERSION_ID_MAX_VALUE = 9;


	/**
	 * Byte length of the label version ID.
	 */
	public static final int VERSION_ID_LENGTH = 1;
	/**
	 * Byte length of the label class ID.
	 */
	public static final int CLASS_ID_LENGTH = 1;
	/**
	 * Byte length of the label delimiter.
	 */
	public static final int DELIMITER_LENGTH = 1;
	/**
	 * Byte length of the label spare.
	 */
	public static final int SPARE_LENGTH = 1;	
	/**
	 * Byte length of the SFDU data description package ID (DDP ID)
	 */
	public static final int DATA_DESCRIPTION_PACKAGE_ID_LENGTH = 4;

	/**
	 * Byte length of the SFDU length or marker field.
	 */
	public static final int BLOCK_LENGTH_OR_MARKER_LENGTH = 8;
	
	/**
	 * Length of an SFDU Label.
	 */
	public static final int LABEL_LENGTH = CONTROL_AUTHORITY_ID_LENGTH +
										   VERSION_ID_LENGTH +
										   CLASS_ID_LENGTH +
										   DELIMITER_LENGTH +
										   SPARE_LENGTH +
										   DATA_DESCRIPTION_PACKAGE_ID_LENGTH +
										   BLOCK_LENGTH_OR_MARKER_LENGTH;
	
	/**
	 * Byte offset of the control authority ID.
	 */
	public static final int CONTROL_AUTHORITY_ID_OFFSET = 0;
	/**
	 * Byte offset of the version ID.
	 */ 
	public static final int VERSION_ID_OFFSET = CONTROL_AUTHORITY_ID_OFFSET + CONTROL_AUTHORITY_ID_LENGTH;
	/**
	 * Byte offset of the class ID.
	 */ 
	public static final int CLASS_ID_OFFSET = VERSION_ID_OFFSET + VERSION_ID_LENGTH;
	/**
	 * Byte offset of the delimiter.
	 */ 
	public static final int DELIMITER_OFFSET = CLASS_ID_OFFSET + CLASS_ID_LENGTH;
	/**
	 * Byte offset of the spare.
	 */ 
	public static final int SPARE_OFFSET = DELIMITER_OFFSET + DELIMITER_LENGTH;
	
	/**
	 * Byte offset of the DDP ID.
	 */
	public static final int DATA_DESCRIPTION_PACKAGE_ID_OFFSET = SPARE_OFFSET + SPARE_LENGTH;
	/**
	 * Byte offset of the block length or marker offset.
	 */
	public static final int BLOCK_LENGTH_OR_MARKER_OFFSET = DATA_DESCRIPTION_PACKAGE_ID_OFFSET +
															 DATA_DESCRIPTION_PACKAGE_ID_LENGTH;
	
	private String controlAuthorityId;
	private int versionId;
	private char classId;
	private char delimiter;
	private char spare;
	private String dataDescriptionPackageId;
	private String blockLengthOrMarker;
	
	/**
	 * Basic constructor.
	 */
	public SfduLabel()
	{
		this.controlAuthorityId = null;
		this.versionId = 0;
		this.classId = '\0';
		this.delimiter = '\0';
		this.spare = '\0';
		this.dataDescriptionPackageId = null;
		this.blockLengthOrMarker = null;
	}
	
	/**
	 * Constructs an SfduLabel object from a byte array.
	 * @param bytes the byte array containing label data
	 * @param offset the offset into the byte array to start at
	 */
	public SfduLabel(final byte[] bytes, final int offset)
	{
		parseFromBytes(bytes,offset);
	}	
	
	/**
	 * Parses an SFDU label string and stores the parsed attributes in this object.
	 * 
	 * NOTE: This will NOT work for version #2 SFDU labels (they'll end up with block length/marker field with a value of null)
	 * 
	 * @param inLabel the input SFDU label string
	 */
	public void parseFromString(final String inLabel)
	{
		if(inLabel == null)
		{
			throw new IllegalArgumentException("Null input SFDU label string");
		}
		
		final String label = inLabel.trim();
		
		setControlAuthorityId(label.substring(CONTROL_AUTHORITY_ID_OFFSET,
				CONTROL_AUTHORITY_ID_OFFSET+CONTROL_AUTHORITY_ID_LENGTH));
		setVersionId(Integer.parseInt(label.substring(VERSION_ID_OFFSET,
				VERSION_ID_OFFSET+VERSION_ID_LENGTH)));
		setClassId(label.charAt(CLASS_ID_OFFSET));
		setDelimiter(label.charAt(DELIMITER_OFFSET));
		setSpare(label.charAt(SPARE_OFFSET));
		setDataDescriptionPackageId(label.substring(DATA_DESCRIPTION_PACKAGE_ID_OFFSET,
				DATA_DESCRIPTION_PACKAGE_ID_OFFSET+DATA_DESCRIPTION_PACKAGE_ID_LENGTH));
		
		if(this.versionId != 2)
		{
			setBlockLengthOrMarker(label.substring(BLOCK_LENGTH_OR_MARKER_OFFSET,
						BLOCK_LENGTH_OR_MARKER_OFFSET+BLOCK_LENGTH_OR_MARKER_LENGTH));
		}
	}
	
	/**
	 * Gets the SFDU label contents as a byte array.
	 * @return an array of bytes containing the label data
	 */
	public byte[] getBytes()
	{
		final byte[] bytes = new byte[LABEL_LENGTH];
		Arrays.fill(bytes,(byte)0x00);
		
		try
		{
			final byte[] stringBytes = getBaseString().getBytes(ASCII_CHARSET);
			
			System.arraycopy(stringBytes,0,bytes,0,stringBytes.length);
			
			switch(this.versionId)
			{
				case 1:
				case 3:
				default:
					final byte[] lengthMarkerBytes = this.blockLengthOrMarker.getBytes(ASCII_CHARSET);
					System.arraycopy(lengthMarkerBytes,0,bytes,BLOCK_LENGTH_OR_MARKER_OFFSET,lengthMarkerBytes.length);
					break;
			
				case 2:
					
					final long length = Long.parseLong(this.blockLengthOrMarker);
					GDR.set_i64(bytes,BLOCK_LENGTH_OR_MARKER_OFFSET,length);
					break;
			}
		}
		catch(final UnsupportedEncodingException e)
		{
			TraceManager.getDefaultTracer().error("This system is not properly supporting the standard " + ASCII_CHARSET + " character set.", e);
		}
		
		return(bytes);
	}
	
	/**
	 * Parses an SFDU label from a byte array and stores the parsed attributes in this object.
	 * 
	 * @param bytes the array of bytes to parse data from
	 * @param offset starting byte offset in the array
	 */
	public void parseFromBytes(final byte[] bytes,final int offset)
	{
		if(bytes == null)
		{
			throw new IllegalArgumentException("Null input byte array");
		}
		else if(offset < 0 || offset >= bytes.length)
		{
			throw new IllegalArgumentException("Offset value " + offset + " is outside the bounds of the input " +
					" byte array which has indexes from 0 to " + (bytes.length-1) + ".");
		}
		else if(bytes.length < (offset+LABEL_LENGTH))
		{
			throw new IllegalArgumentException("The input byte array length of " + bytes.length + 
					" with an offset of " + offset + " does not leave enough space for an entire SFDU label" +
					" which has a length of " + LABEL_LENGTH + " bytes.");
		}
		
		try
		{
			final byte[] labelBytes = new byte[LABEL_LENGTH];
			System.arraycopy(bytes,offset,labelBytes,0,labelBytes.length);
			
			final String label = new String(labelBytes,ASCII_CHARSET);
			parseFromString(label);
			
			if(this.versionId == 2)
			{
				this.blockLengthOrMarker = String.valueOf(GDR.get_i64(bytes,BLOCK_LENGTH_OR_MARKER_OFFSET));
			}
		}
		catch(final UnsupportedEncodingException e)
		{
			TraceManager.getDefaultTracer().error("This system is not properly supporting the standard " + ASCII_CHARSET + " character set.", e);
		}
	}
	
	/**
	 * Sets the SFDU block length or marker. (Some SFDUs have a length, others have a marker).
	 * 
	 * @param blockLengthOrMarker the length of marker to set
	 */
	public void setBlockLengthOrMarker(final String blockLengthOrMarker)
	{
		this.blockLengthOrMarker = blockLengthOrMarker;
	}

	/**
	 * Sets the SFDU class identifier.
	 * 
	 * @param classId the class ID, which must be a character in the range A-Z or 0-9.
	 */
	public void setClassId(final char classId)
	{
		if(String.valueOf(classId).matches(RESTRICTED_ASCII_REGEXP) == false)
		{
			throw new IllegalArgumentException("The input class ID value \"" + classId + "\"" +
					" does not match the Restricted ASCII format regular expression \"" + RESTRICTED_ASCII_REGEXP + "\".");
		}
		
		this.classId = classId;
	}

	/**
	 * Sets the SFDU control authority ID (CAID).
	 * 
	 * @param cai the control authority identifier (e.g., NJPL, CCSD). Must be 4 characters in the range A-Z or 0-9.
	 */
	public void setControlAuthorityId(final String cai)
	{
		if(cai == null)
		{
			throw new IllegalArgumentException("Null input control authority ID.");
		}
		
		final String controlAuthorityId = cai.trim();
		
		if(controlAuthorityId.trim().length() != CONTROL_AUTHORITY_ID_LENGTH)
		{
			throw new IllegalArgumentException("The input control authority ID value \"" + controlAuthorityId + "\"" +
					" was not the proper length of " + CONTROL_AUTHORITY_ID_LENGTH + " characters.");
		}
		else if(controlAuthorityId.trim().matches(RESTRICTED_ASCII_REGEXP) == false)
		{
			throw new IllegalArgumentException("The input control authority ID value \"" + controlAuthorityId + "\"" +
					" does not match the Restricted ASCII format regular expression \"" + RESTRICTED_ASCII_REGEXP + "\".");
		}
		
		this.controlAuthorityId = controlAuthorityId;
	}

	/**
	 * Sets the SFDU data description package ID (DDP ID).
	 * 
	 * @param ddpId the DDP ID to set; must be 4 characters in the range A-Z or 0-9.
	 */
	public void setDataDescriptionPackageId(final String ddpId)
	{
		if(ddpId == null)
		{
			throw new IllegalArgumentException("Null input data description package ID.");
		}
		
		final String dataDescriptionPackageId = ddpId.trim();
		
		if(dataDescriptionPackageId.trim().length() != DATA_DESCRIPTION_PACKAGE_ID_LENGTH)
		{
			throw new IllegalArgumentException("The input data description package ID value \"" + dataDescriptionPackageId + "\"" +
					" was not the proper length of " + DATA_DESCRIPTION_PACKAGE_ID_LENGTH + " characters.");
		}
		else if(dataDescriptionPackageId.trim().matches(RESTRICTED_ASCII_REGEXP) == false)
		{
			throw new IllegalArgumentException("The input data description package ID value \"" + dataDescriptionPackageId + "\"" +
					" does not match the Restricted ASCII format regular expression \"" + RESTRICTED_ASCII_REGEXP + "\".");
		}
		
		this.dataDescriptionPackageId = dataDescriptionPackageId;
	}

	/**
	 * Sets the SFDU delimiter value.
	 * 
	 * @param delimiter the delimiter to set; must be a character in the range A-Z or 0-9.
	 */
	public void setDelimiter(final char delimiter)
	{
		if(String.valueOf(delimiter).matches(RESTRICTED_ASCII_REGEXP) == false)
		{
			throw new IllegalArgumentException("The input delimiter value \"" + delimiter + "\"" +
					" does not match the Restricted ASCII format regular expression \"" + RESTRICTED_ASCII_REGEXP + "\".");
		}
		
		this.delimiter = delimiter;
	}

	/**
	 * Sets the SFDU spare character.
	 * 
	 * @param spare character to set
	 */
	public void setSpare(final char spare)
	{
		this.spare = spare;
	}

	/**
	 * Sets the SFDU version ID.
	 * 
	 * @param versionId version to set: 0-3.
	 */
	public void setVersionId(final int versionId)
	{
		if(versionId < VERSION_ID_MIN_VALUE || versionId > VERSION_ID_MAX_VALUE)
		{
			throw new IllegalArgumentException("Input version ID value of \"" + versionId + "\" is outside" +
					" the allowable value range of " + VERSION_ID_MIN_VALUE + "-" + VERSION_ID_MAX_VALUE + ".");
		}
		
		this.versionId = versionId;
	}
	
	/**
	 * Gets the SFDU block length. 
	 * 
	 * @return SFDU block length, or null for version 3 SFDUs.
	 */
	public Integer getBlockLength()
	{
		int length = 0;
		try
		{
			switch(this.versionId)
			{
				case 1:
				case 2:
				default:
					length = Integer.parseInt(this.blockLengthOrMarker);	
					break;
				
				case 3:
					return(null);
			}
		}
		catch(final NumberFormatException nfe)
		{
			return(null);
		}
		
		if((length%2) != 0)
		{
			throw new IllegalStateException("The block length value must be an even number, but was set to " + length);
		}
		
		return(Integer.valueOf(length));
	}
	
	/**
	 * Gets the SFDU block length or marker. (Some SFDUs have a length, others have a marker).
	 * 
	 * @return the block length or marker, as a string.
	 */
	public String getBlockLengthOrMarker()
	{
		return this.blockLengthOrMarker;
	}

	/**
	 * Gets the SFDU class identifier.
	 * 
	 * @return the class ID, which will be a character in the range A-Z or 0-9.
	 */
	public char getClassId()
	{
		return this.classId;
	}

	/**
	 * Gets the SFDU control authority ID (CAID).
	 * 
	 * @return the control authority identifier (e.g., NJPL, CCSD); will be 4 characters in the range A-Z or 0-9.
	 */
	public String getControlAuthorityId()
	{
		return this.controlAuthorityId;
	}

	/**
	 * Gets the SFDU data description package ID (DDP ID).
	 * 
	 * @return the DDP ID; will be 4 characters in the range A-Z or 0-9.
	 */
	public String getDataDescriptionPackageId()
	{
		return this.dataDescriptionPackageId;
	}

	/**
	 * Gets the SFDU delimiter.
	 * 
	 * @return delimiter character
	 */
	public char getDelimiter()
	{
		return this.delimiter;
	}

	/**
	 * Gets the SFDU spare value.
	 * 
	 * @return spare character.
	 */
	public char getSpare()
	{
		return this.spare;
	}

	/**
	 * Gets the SFDU version.
	 * 
	 * @return version ID: 0-3.
	 */
	public int getVersionId()
	{
		return this.versionId;
	}

	/**
	 * Gets a string representation of this SFDU label minus the block length or marker.
	 * 
	 * @return SFDU label string
	 */
	private String getBaseString()
	{
		final StringBuffer buffer = new StringBuffer(12);
		

		buffer.append(this.controlAuthorityId == null ? "0000" : this.controlAuthorityId );
		buffer.append(String.valueOf(this.versionId));
		buffer.append(this.classId == '\0' ? "0" : this.classId);
		buffer.append(this.delimiter == '\0' ? "0" : this.delimiter);
		buffer.append(this.spare  == '\0' ? "0" : this.spare);
		buffer.append(this.dataDescriptionPackageId == null? "0000" : this.dataDescriptionPackageId);
		
		return(buffer.toString());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		final StringBuffer buffer = new StringBuffer(20);
		
		buffer.append(getBaseString());
		// check for null block length marker
		buffer.append(this.blockLengthOrMarker == null ? "00000000" : getBlockLengthOrMarker());
		
		return(buffer.toString());
	}
}
