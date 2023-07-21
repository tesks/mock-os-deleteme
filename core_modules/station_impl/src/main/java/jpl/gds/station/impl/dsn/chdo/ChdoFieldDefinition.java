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
package jpl.gds.station.impl.dsn.chdo;

import jpl.gds.station.api.dsn.chdo.ChdoFieldFormatEnum;
import jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition;

/**
 * This class represents the definition of a single field in a Compressed
 * Header Data Object (CHDO).
 * 
 */
class ChdoFieldDefinition extends Object implements IChdoFieldDefinition
{
	private String fieldId = null;
	private int bitLength = 0;
	private int byteOffset = 0;
	private int bitOffset = 0;
	private ChdoFieldFormatEnum fieldFormat = null;

    /** For integer types, min and max values.
     *  Range can have default, or field can have
     *  a fixed value.
     */
    private Long minValue     = null;
    private Long maxValue     = null;
    private Long defaultValue = null;
    private Long fixedValue   = null;


	/**
	 * Basic constructor.
	 */
	/* package */ ChdoFieldDefinition()
	{
        super();
	}
	
	/**
	 * Constructor for field attributes
	 * 
	 * @param fieldId fieldId
	 * @param bitL bitLength
	 * @param bitO bitOffset
	 * @param byteO byteOffset
	 */
	/* package */ ChdoFieldDefinition(String fieldId, int bitL, int bitO, int byteO) { 
		super();
		this.fieldId = fieldId;
		this.bitLength = bitL;
		this.bitOffset = bitO;
		this.byteOffset = byteO;
	}


	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition#getBitLength()
     */
	@Override
    public int getBitLength()
	{
		return this.bitLength;
	}


	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition#getByteLength()
     */
	@Override
    public int getByteLength()
	{
        return ((bitLength + Byte.SIZE - 1) / Byte.SIZE);
	}


	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition#getFlag()
     */
	@Override
    public boolean getFlag()
	{
        return (bitLength == 1);
	}


	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition#setBitLength(int)
     */
	@Override
    public void setBitLength(final int bitLength)
	{
		this.bitLength = bitLength;
	}
	
	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition#getBitOffset()
     */
	@Override
    public int getBitOffset()
	{
		return this.bitOffset;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition#setBitOffset(int)
     */
	@Override
    public void setBitOffset(final int bitOffset)
	{
		this.bitOffset = bitOffset;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition#getByteOffset()
     */
	@Override
    public int getByteOffset()
	{
		return this.byteOffset;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition#setByteOffset(int)
     */
	@Override
    public void setByteOffset(final int byteOffset)
	{
		this.byteOffset = byteOffset;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition#getFieldId()
     */
	@Override
    public String getFieldId()
	{
		return this.fieldId;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition#setFieldId(java.lang.String)
     */
	@Override
    public void setFieldId(final String fieldId)
	{
		this.fieldId = fieldId;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer(256);
		
		buffer.append("Field (Field ID=\"");
	    buffer.append(this.fieldId);
	    buffer.append("\", Bit Length=\"");
	    buffer.append(this.bitLength);
	    buffer.append("\", Byte Offset=\"");
	    buffer.append(this.byteOffset);
	    buffer.append("\", Bit Offset=\"");
	    buffer.append(this.bitOffset);
	    buffer.append("\")");
		
		return(buffer.toString());
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition#getFieldFormat()
     */
	@Override
    public ChdoFieldFormatEnum getFieldFormat()
	{
		return this.fieldFormat;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition#setFieldFormat(jpl.gds.station.api.dsn.chdo.ChdoFieldFormatEnum)
     */
	@Override
    public void setFieldFormat(final ChdoFieldFormatEnum fieldFormat)
	{
		this.fieldFormat = fieldFormat;
	}


    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition#getMinValue()
     */
    @Override
    public Long getMinValue()
    {
        return minValue;
    }


    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition#setMinValue(java.lang.Long)
     */
    @Override
    public void setMinValue(final Long min)
    {
        minValue = min;
    }


    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition#getMaxValue()
     */
    @Override
    public Long getMaxValue()
    {
        return maxValue;
    }


    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition#setMaxValue(java.lang.Long)
     */
    @Override
    public void setMaxValue(final Long max)
    {
        maxValue = max;
    }


    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition#getDefaultValue()
     */
    @Override
    public Long getDefaultValue()
    {
        return defaultValue;
    }


    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition#setDefaultValue(java.lang.Long)
     */
    @Override
    public void setDefaultValue(final Long value)
    {
        defaultValue = value;
    }


    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition#getFixedValue()
     */
    @Override
    public Long getFixedValue()
    {
        return fixedValue;
    }


    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoFieldDefinition#setFixedValue(java.lang.Long)
     */
    @Override
    public void setFixedValue(final Long value)
    {
        fixedValue = value;
    }
}
