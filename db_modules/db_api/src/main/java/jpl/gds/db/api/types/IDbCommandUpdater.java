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


public interface IDbCommandUpdater extends IDbCommandProvider {

    /**
     * Set type.
     *
     * @param type
     *            Type
     */
    void setType(CommandType type);

    /**
     * Set command string.
     *
     * @param commandString
     *            Command string
     */
    void setCommandString(String commandString);

    /**
     * Set event time.
     *
     * @param eventTime
     *            Event time
     */
    void setEventTime(IAccurateDateTime eventTime);

    /**
     * Set RCT.
     *
     * @param rct
     *            RCT
     */
    void setRct(IAccurateDateTime rct);

    /**
     * Set original file.
     *
     * @param originalFile
     *            Original file
     */
    void setOriginalFile(String originalFile);

    /**
     * Set SCMF file.
     *
     * @param scmfFile
     *            SCMF file
     */
    void setScmfFile(String scmfFile);

    /**
     * Set fail reason.
     *
     * @param failReason
     *            Fail reason
     */
    void setFailReason(String failReason);

    /**
     * Setter for commanded side.
     *
     * @param cs
     *            Command side
     */
    void setCommandedSide(String cs);

    /**
     * Set requestId.
     *
     * @param id
     *            RequestId type
     */
    void setRequestId(String id);

    /**
     * Set status.
     *
     * @param cst
     *            Status type
     */
    void setStatus(CommandStatusType cst);

    /**
     * Set finalized.
     *
     * @param fin
     *            True if finalized
     */
    void setFinalized(boolean fin);

    /**
     * Set final.
     *
     * @param fin
     *            True if final
     */
    void setFinal(boolean fin);

    /**
     * Set checksum.
     *
     * @param value
     *            New value
     */
    void setChecksum(Long value);

    /**
     * Set totalCltus.
     *
     * @param value
     *            New value
     */
    void setTotalCltus(Long value);

    /**
     * Set bit1RadTime.
     *
     * @param value
     *            New value
     */
    void setBit1RadTime(IAccurateDateTime value);

    /**
     * Set lastBitRadTime.
     *
     * @param value
     *            New value
     */
    void setLastBitRadTime(IAccurateDateTime value);

}