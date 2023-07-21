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

public interface IPacketOrderByType extends IDbOrderByType {

	/** TEST_SESSION_ID_TYPE */
	int TEST_SESSION_ID_TYPE = 0;
	/** RCT_TYPE */
	int RCT_TYPE = 1;
	/** SCET_TYPE */
	int SCET_TYPE = 2;
	/** ERT_TYPE */
	int ERT_TYPE = 3;
	/** SCLK_TYPE */
	int SCLK_TYPE = 4;
	/** APID_TYPE */
	int APID_TYPE = 5;
	/** SPSC_TYPE */
	int SPSC_TYPE = 6;
	/** VCID_TYPE */
	int VCID_TYPE = 7;
	/** LST_TYPE */
	int LST_TYPE = 8;
	/** NONE_TYPE */
	int NONE_TYPE = 9;
	/** ID_TYPE */
	int ID_TYPE = 10;

}