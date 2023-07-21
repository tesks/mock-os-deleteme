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
package jpl.gds.db.api.types.cfdp;

import jpl.gds.cfdp.data.api.ECfdpIndicationType;
import jpl.gds.cfdp.data.api.ECfdpTransactionDirection;
import jpl.gds.cfdp.data.api.ECfdpTriggeredByType;
import jpl.gds.cfdp.data.api.FixedPduHeader;
import jpl.gds.cfdp.data.api.ICfdpCondition;
import jpl.gds.db.api.types.IDbQueryable;
import jpl.gds.db.api.types.IDbRawData;
import jpl.gds.shared.time.IAccurateDateTime;

public interface IDbCfdpIndicationProvider extends IDbQueryable {

	IAccurateDateTime getIndicationTime();

	String getCfdpProcessorInstanceId();

	ECfdpIndicationType getType();

	ICfdpCondition getCondition();

	ECfdpTransactionDirection getTransactionDirection();

	long getSourceEntityId();

	long getTransactionSequenceNumber();

	byte getServiceClass();

	long getDestinationEntityId();

	boolean getInvolvesFileTransfer();

	long getTotalBytesSentOrReceived();

	ECfdpTriggeredByType getTriggeringType();

	String getPduId();

	FixedPduHeader getTriggeringPduFixedHeader();

}