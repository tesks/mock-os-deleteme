/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.cfdp.message.api;

import jpl.gds.cfdp.data.api.ECfdpIndicationType;
import jpl.gds.cfdp.data.api.ECfdpTransactionDirection;
import jpl.gds.cfdp.data.api.ECfdpTriggeredByType;
import jpl.gds.cfdp.data.api.FixedPduHeader;
import jpl.gds.cfdp.data.api.ICfdpCondition;
import jpl.gds.shared.interfaces.EscapedCsvSupport;
import jpl.gds.shared.message.IMessage;

/**
 * Interface ICfdpIndicationMessage
 * 
 * @since R8
 */
public interface ICfdpIndicationMessage extends ICfdpMessage, EscapedCsvSupport {

	public ECfdpIndicationType getIndicationType();

	public ICfdpCondition getCondition();
	
	public ECfdpTransactionDirection getTransactionDirection();

	public long getSourceEntityId();

	public long getTransactionSequenceNumber();

	public byte getServiceClass();

	public long getDestinationEntityId();

	public boolean getInvolvesFileTransfer();

	public long getTotalBytesSentOrReceived();

	public ECfdpTriggeredByType getTriggeringType();

	public String getPduId();

	public FixedPduHeader getTriggeringPduFixedHeader();

	public ICfdpIndicationMessage setIndicationType(ECfdpIndicationType type);
	
	public ICfdpIndicationMessage setCondition(ICfdpCondition Condition);

	public ICfdpIndicationMessage setTransactionDirection(ECfdpTransactionDirection transactionDirection);

	public ICfdpIndicationMessage setSourceEntityId(long sourceEntityId);

	public ICfdpIndicationMessage setTransactionSequenceNumber(long transactionSequenceNumber);

	public ICfdpIndicationMessage setServiceClass(byte serviceClass);

	public ICfdpIndicationMessage setDestinationEntityId(long destinationEntityId);

	public ICfdpIndicationMessage setInvolvesFileTransfer(boolean involvesFileTransfer);

	public ICfdpIndicationMessage setTotalBytesSentOrReceived(long totalBytesSentOrReceived);

	public ICfdpIndicationMessage setTriggeringType(ECfdpTriggeredByType triggeringType);

	public ICfdpIndicationMessage setPduId(String pduId);

	public ICfdpIndicationMessage setTriggeringPduFixedHeader(FixedPduHeader triggeringPduFixedHeader);
	
}