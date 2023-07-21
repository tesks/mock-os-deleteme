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

import edu.umd.cs.findbugs.annotations.SuppressWarnings;


public interface ISessionOrderByType extends IDbOrderByType {
    /** String value constants */
	@SuppressWarnings({"MS_MUTABLE_ARRAY","MS_PKGPROTECT"})
	public static final String orderByTypes[] =
	{ 
		"ID", 
	    "Name", 
		"FswVersion", 
		"SseVersion", 
		"StartTime", 
		"EndTime",
        "None",
        "ID_Desc"
	};
	
	/** ID_TYPE */
	int ID_TYPE = 0;
	/** NAME_TYPE */
	int NAME_TYPE = 1;
	/** FSW_VERSION_TYPE */
	int FSW_VERSION_TYPE = 2;
	/** SSE_VERSION_TYPE */
	int SSE_VERSION_TYPE = 3;
	/** START_TIME_TYPE */
	int START_TIME_TYPE = 4;
	/** END_TIME_TYPE */
	int END_TIME_TYPE = 5;
	/** NONE_TYPE */
	int NONE_TYPE = 6;
	/** ID_TYPE_DESC */
	int ID_DESC_TYPE = 7;
}