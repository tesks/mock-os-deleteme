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
package jpl.gds.db.api.sql.store.ldi;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.tc.api.message.IUplinkMessage;

public interface ICommandMessageLDIStore extends ILDIStore {
    public static final StoreIdentifier STORE_IDENTIFIER = StoreIdentifier.CommandMessage;
    
	/** Common database table fields */
	String REQUEST_ID = "requestId";

    /** Common database table fields as CSV. */
    static final String COMMON = SESSION_ID  + "," +
                                         HOST_ID     + "," +
                                         FRAGMENT_ID + "," +
                                         REQUEST_ID;
	
	/**
	 * Database table fields as CSV.
	 */
	static final String DB_COMMAND_MESSAGE_FIELDS = COMMON + "," + "message" + "," + "type" + "," + "originalFile" + "," + "scmfFile" + ","
			+ "commandedSide" + "," + "finalized" + "," + "checksum" + "," + "totalCltus";
	
	/**
	 * Database status table fields as CSV.
	 */
	static final String DB_COMMAND_STATUS_FIELDS = COMMON + "," + "rctCoarse" + "," + "rctFine" + "," + "eventTimeCoarse" + ","
			+ "eventTimeFine" + "," + "status" + "," + "failReason" + "," + "bit1RadTimeCoarse" + ","
			+ "bit1RadTimeFine" + "," + "lastBitRadTimeCoarse" + "," + "lastBitRadTimeFine" + "," + "dssId" + ","
			+ "final";

	/**
	 * Insert a command message into the database
	 *
	 * @param m The external command message
	 *
	 * @throws DatabaseException Throws SQLException
	 */
	void insertMessage(IUplinkMessage m) throws DatabaseException;
}