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
package jpl.gds.db.api.types;

import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.InvalidFrameCode;


/**
 * MPCS-8322 - Moved to core_db_ip
 */
public interface IDbRawData extends IDbRecord
{
    /**
     * @return the ERT
     */
	public IAccurateDateTime getErt();
	
    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.api.types.IDbRecord#getRecordBytes()
     */
	@Override
    public byte[] getRecordBytes();

    /**
     * @return the packet's ID in a PacketIdHolder
     */
    public PacketIdHolder getPacketId();

    /**
     * @return the Frame's ID
     */
	public Long getFrameId();
	
    /**
     * @return the downlink bit rate
     */
	public Double getBitRate();
	
    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.api.types.IDbRecord#getRecordLength()
     */
	@Override
    public int getRecordLength();
	
    /**
     * @return the DSS ID of the frame as an Integer
     * 
     *         MPCS-6349 : DSS ID not set properly Removed
     *         dssId from subclasses of AbstractDatabaseItem.
     *         AbstractDatabaseItem class has been updated with protected fields
     *         sessionDssId and recordDssId with get/set methods for both.
     * 
     *         Updated this method to match the changes above
     */
	public Integer getRecordDssIdAsInt();


    /**
     * Get Frame bad reason or null.
     *
     * @return Bad reason or null
     */
    public InvalidFrameCode getBadReason();


    /**
     * Get header holder.
     *
     * @return Header holder
     */
	public HeaderHolder getRawHeader();


    /**
     * Get trailer holder.
     *
     * @return Trailer holder
     */
	public TrailerHolder getRawTrailer();
}
