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

/**
 * Holder of SFDU identifier. By the CCSDS Standard the DDP ID should enough
 * to uniquely identify the SFDU,
 * but various missions have violated that standard so the major data type, 
 * minor data type, format ID, and mission ID must be added.
 * 
 */
public class SfduId
{
	private String ddpId;
	private int majorDataType;
	private int minorDataType;
	private int formatId;
	private int missionId;

    /**
     * 
     * Creates an instance of SfduId.
     */
	public SfduId()
	{
		this.ddpId = null;
		this.majorDataType = 0;
		this.minorDataType = 0;
		this.formatId = 0;
		this.missionId = 0;
	}
	
    /**
     * Creates an instance of SfduId with the given Data Description Package (DDP) ID.
     * @param ddpId the Data Description Package (DDP) ID
     */
	public SfduId(final String ddpId)
	{
		this();
		
		this.ddpId = ddpId;
	}

	/**
	 * Creates an instance of SfduId with the given attributes.
	 * @param ddpId Data Description Package (DDP) ID.
	 * @param major SFDU major number
	 * @param minor SFDU minor number
	 * @param format SFDU format specified
	 * @param missionId numeric mission ID
	 */
	public SfduId(final String ddpId, final int major, final int minor, final int format, final int missionId)
	{
		this();
		
		this.ddpId = ddpId;
		this.majorDataType = major;
		this.minorDataType = minor;
		this.formatId = format;
		this.missionId = missionId;
	}

	/**
	 * Gets the Data Description Package (DDP) ID.
	 * @return DDP ID string.
	 */
	public String getDdpId()
	{
		return this.ddpId;
	}

	/**
	 * Sets the Data Description Package (DDP) ID.
	 *
	 * @param ddpId The DDP ID to set.
	 */
	public void setDdpId(final String ddpId)
	{
		this.ddpId = ddpId;
	}

	/**
	 * Gets the format identifier.
	 * 
	 * @return the format ID.
	 */
	public int getFormatId()
	{
		return this.formatId;
	}

	/**
	 * Sets the format identifier.
	 *
	 * @param formatId The format ID to set.
	 */
	public void setFormatId(final int formatId)
	{
		this.formatId = formatId;
	}

	/**
	 * Gets the major data type.
	 * 
	 * @return the major data type indicator
	 */
	public int getMajorDataType()
	{
		return this.majorDataType;
	}

	/**
	 * Sets the major data type.
	 *
	 * @param majorDataType The major data type to set.
	 */
	public void setMajorDataType(final int majorDataType)
	{
		this.majorDataType = majorDataType;
	}

	/**
	 * Gets the minor data type.
	 * 
	 * @return the minor data type indicator
	 */
	public int getMinorDataType()
	{
		return this.minorDataType;
	}

	/**
	 * Sets the minor data type.
	 *
	 * @param minorDataType The minor data type to set.
	 */
	public void setMinorDataType(final int minorDataType)
	{
		this.minorDataType = minorDataType;
	}

	/**
	 * 
	 * Gets the mission identifier.
	 * 
	 * @return the mission ID
	 */
	public int getMissionId()
	{
		return this.missionId;
	}

	/**
	 * Sets the mission identifier.
	 *
	 * @param missionId The ID to set.
	 */
	public void setMissionId(final int missionId)
	{
		this.missionId = missionId;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		// get parts
		final int bval = (ddpId == null || ddpId.isEmpty()) ? 0 : (ddpId.charAt(0) - 'A');
		final int ival = (ddpId == null || ddpId.isEmpty()) ? 0 : Integer.parseInt(ddpId.substring(1));
		int hc = (1000 * bval) + ival; // ddpId
		// ignore majorDataType
		final int oc = (missionId << 16) | (minorDataType << 8) | formatId;
		hc = (hc << 12) + (oc % 0xfff);
		return hc;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object val)
	{
		if(val == null)
		{
			return false;
		}
		
		if((val instanceof SfduId) == false)
		{
			return(false);
		}
		
		final SfduId sid = (SfduId)val;
		return(this.missionId == sid.getMissionId() && 
			   this.majorDataType == sid.getMajorDataType() && 
			   this.minorDataType == sid.getMinorDataType() &&
			   this.formatId == sid.getFormatId() &&
			   this.ddpId.equals(sid.getDdpId()));
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		final StringBuffer buffer = new StringBuffer(256);
		
		buffer.append("DDP ID = ");
		buffer.append(this.ddpId);
		buffer.append(", Major Data Type = ");
		buffer.append(this.majorDataType);
		buffer.append(", Minor Data Type = ");
		buffer.append(this.minorDataType);
		buffer.append(", Format ID = ");
		buffer.append(this.formatId);
		buffer.append(", Mission ID = ");
		buffer.append(this.missionId);
		
		return(buffer.toString());
	}
}