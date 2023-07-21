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

import jpl.gds.station.api.dsn.chdo.IChdo;
import jpl.gds.station.api.dsn.chdo.IChdoDefinition;


/**
 * This class represents a single Compressed Header Data Object (CHDO) within
 * an SFDU CHDO structure. It essentially consists of the CHDO definition and
 * the associated data bytes.
 * 
 */
class Chdo implements IChdo
{
	/**
	 * Maximum length of a Chdo object.
	 */
	private static final int MAX_CHDO_SIZE = 65534;
	
	private int length;
	
	private IChdoDefinition definition;
	private byte[] rawValue;
	
	/**
	 * Constructs a new Chdo object.
	 * @param def the definition object for the CHDO.
	 * @param chdoBytes the actual data bytes for the CHDO.
	 */
	public Chdo(IChdoDefinition def, byte[] chdoBytes)
	{
		setDefinition(def);
		setRawValue(chdoBytes);
	}
	
	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdo#getDefinition()
     */
	@Override
    public IChdoDefinition getDefinition()
	{
		return this.definition;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdo#setDefinition(jpl.gds.station.api.dsn.chdo.IChdoDefinition)
     */
	@Override
    public void setDefinition(final IChdoDefinition definition)
	{
		this.definition = definition;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdo#getRawValue()
     */
	@Override
    public byte[] getRawValue()
	{
		return this.rawValue;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdo#setRawValue(byte[])
     */
	@Override
    public void setRawValue(final byte[] rawValue)
	{
		if(rawValue.length > MAX_CHDO_SIZE)
		{
			throw new IllegalArgumentException("Input CHDO is larger than the allowable CHDO size of " + MAX_CHDO_SIZE + " bytes.");
		}
		this.rawValue = rawValue;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdo#getBytesWithoutChdoHeader()
     */
	@Override
    public byte[] getBytesWithoutChdoHeader()
	{
		byte[] bytes = new byte[this.length];
		System.arraycopy(this.rawValue,this.rawValue.length-bytes.length,bytes,0,bytes.length);
		return(bytes);
	}
	
	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdo#getLength()
     */
	@Override
    public int getLength()
	{
		return this.length;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdo#setLength(int)
     */
	@Override
    public void setLength(int length)
	{
		this.length = length;
	}


    /**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdo#getHeaderLength()
     */
    @Override
    public int getHeaderLength()
    {
        return rawValue.length - getLength();
    }
}
