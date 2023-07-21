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
package jpl.gds.db.api.types;

import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.tc.api.CommandStatusType;

public interface IDbCommandProvider extends IDbQueryable {

    /**
     * Get type.
     *
     * @return Type
     */
    CommandType getType();

    /**
     * Get command string.
     *
     * @return Command string
     */
    String getCommandString();

    /**
     * Get event time.
     *
     * @return Event time
     */
    IAccurateDateTime getEventTime();

    /**
     * Get RCT.
     *
     * @return RCT
     */
    IAccurateDateTime getRct();

    /**
     * Get original file.
     *
     * @return original file
     */
    String getOriginalFile();

    /**
     * Get SCMF file.
     *
     * @return SCMF file
     */
    String getScmfFile();

    /**
     * Get fail reason.
     *
     * @return Fail reason
     */
    String getFailReason();

    /**
     * Getter for commanded side.
     *
     * @return Commanded side
     */
    String getCommandedSide();

    /**
     * Get requestId.
     *
     * @return RequestId
     */
    String getRequestId();

    /**
     * Get status.
     *
     * @return Status
     */
    CommandStatusType getStatus();

    /**
     * Get finalized.
     *
     * @return Finalized state
     */
    boolean getFinalized();

    /**
     * Get final.
     *
     * @return Final state
     */
    boolean getFinal();

    /**
     * Get checksum.
     *
     * @return Value
     */
    Long getChecksum();

    /**
     * Get totalCltus.
     *
     * @return Value
     */
    Long getTotalCltus();

    /**
     * Get bit1RadTime.
     *
     * @return Value
     */
    IAccurateDateTime getBit1RadTime();

    /**
     * Get lastBitRadTime.
     *
     * @return Value
     */
    IAccurateDateTime getLastBitRadTime();
}