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
import jpl.gds.shared.time.IAccurateDateTime;

public interface IDbCfdpIndicationUpdater extends IDbCfdpIndicationProvider {

	void setIndicationTime(IAccurateDateTime indicationTime);

	void setCfdpProcessorInstanceId(String cfdpProcessorInstanceId);

	void setType(ECfdpIndicationType type);

	void setCondition(ICfdpCondition condition);

	void setTransactionDirection(ECfdpTransactionDirection transactionDirection);

	void setSourceEntityId(long sourceEntityId);

	void setTransactionSequenceNumber(long transactionSequenceNumber);

	void setServiceClass(byte serviceClass);

	void setDestinationEntityId(long destinationEntityId);

	void setInvolvesFileTransfer(boolean involvesFileTransfer);

	void setTotalBytesSentOrReceived(long totalBytesSentOrReceived);

	void setTriggeringType(ECfdpTriggeredByType triggeringType);

	void setPduId(String pduId);

	void setTriggeringPduFixedHeader(FixedPduHeader triggeringPduFixedHeader);
	
}