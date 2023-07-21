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

public interface ILogOrderByType extends IDbOrderByType {

	/** TEST_SESSION_ID_TYPE */
	int TEST_SESSION_ID_TYPE = 0;
	/** EVENT_TIME_TYPE */
	int EVENT_TIME_TYPE = 1;
	/** CLASSIFICATION_TYPE */
	int CLASSIFICATION_TYPE = 2;
	/** MESSAGE_TYPE_TYPE */
	int MESSAGE_TYPE_TYPE = 3;
	/** NONE_TYPE */
	int NONE_TYPE = 4;
	/** RCT_TYPE */
	int RCT_TYPE = 5;
	/** EVENT_TIME_DESC_TYPE */
	int EVENT_TIME_DESC_TYPE = 6;

}