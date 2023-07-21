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

public interface IFrameOrderByType extends IDbOrderByType {

	/** TEST_SESSION_ID_TYPE */
	int TEST_SESSION_ID_TYPE = 0;
	/** FRAME_TYPE */
	int FRAME_TYPE = 1;
	/** ERT_TYPE */
	int ERT_TYPE = 2;
	/** RELAY_SCID_TYPE */
	int RELAY_SCID_TYPE = 3;
	/** VCID_TYPE */
	int VCID_TYPE = 4;
	/** VCFC_TYPE */
	int VCFC_TYPE = 5;
	/** DSS_TYPE */
	int DSS_TYPE = 6;
	/** NONE_TYPE */
	int NONE_TYPE = 7;
	/** ID_TYPE */
	int ID_TYPE = 8;
	/** RCT_TYPE */
	int RCT_TYPE = 9;

}