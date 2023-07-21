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
package jpl.gds.db.api.sql.order;

public interface IChannelValueOrderByType extends IDbOrderByType {

	/** TEST_SESSION_ID_TYPE */
	int TEST_SESSION_ID_TYPE = 0;
	/** CHANNEL_INDEX_TYPE */
	int CHANNEL_INDEX_TYPE = 1;
	/** CHANNEL_ID_TYPE */
	int CHANNEL_ID_TYPE = 2;
	/** CHANNEL_TYPE_TYPE */
	int CHANNEL_TYPE_TYPE = 3;
	/** MODULE_TYPE */
	int MODULE_TYPE = 4;
	/** SCLK_TYPE */
	int SCLK_TYPE = 5;
	/** ERT_TYPE */
	int ERT_TYPE = 6;
	/** SCET_TYPE */
	int SCET_TYPE = 7;
	/** LST_TYPE */
	int LST_TYPE = 8;
	/** NONE_TYPE */
	int NONE_TYPE = 9;
	/** HOST_ID_TYPE */
	int HOST_ID_TYPE = 10;
	/** VCID_TYPE */
	int VCID_TYPE = 11;
	/** STATION_TYPE */
	int STATION_TYPE = 12;
	/** APID_TYPE */
	int APID_TYPE = 13;
	/** SPSC_TYPE */
	int SPSC_TYPE = 14;
	/** PACKET_RCT_TYPE */
	int PACKET_RCT_TYPE = 15;
	/** SCLK_EXT_TYPE */
	int SCLK_EXT_TYPE = 16;
	/** ERT_EXT_TYPE */
	int ERT_EXT_TYPE = 17;
	/** SCET_EXT_TYPE */
	int SCET_EXT_TYPE = 18;
	/** LST_EXT_TYPE */
	int LST_EXT_TYPE = 19;
	/** RCT_TYPE */
	int RCT_TYPE = 20;

	/**
	 * Return True if Packet columns required.
	 *
	 * @return True if this order-by requires Packet columns.
	 */
	boolean getPacketRequired();

}