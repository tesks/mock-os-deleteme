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

public interface IEvrOrderByType extends IDbOrderByType {

	/** TEST_SESSION_ID_TYPE */
	int TEST_SESSION_ID_TYPE = 0;
	/** EVENT_ID_TYPE */
	int EVENT_ID_TYPE = 1;
	/** ERT_TYPE */
	int ERT_TYPE = 2;
	/** SCET_TYPE */
	int SCET_TYPE = 3;
	/** RCT_TYPE */
	int RCT_TYPE = 4;
	/** SCLK_TYPE */
	int SCLK_TYPE = 5;
	/** LEVEL_TYPE */
	int LEVEL_TYPE = 6;
	/** MODULE_TYPE */
	int MODULE_TYPE = 7;
	/** NAME_TYPE */
	int NAME_TYPE = 8;
	/** LST_TYPE */
	int LST_TYPE = 9;
	/** NONE_TYPE */
	int NONE_TYPE = 10;

}